package kwall.artemis1982;/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.concurrent.CountDownLatch;

import javax.xml.bind.DatatypeConverter;

import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonSender;

import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Modified;
import org.apache.qpid.proton.message.Message;

public class Artemis1982
{
    private final String _host;
    private final int _port;
    private final String _address;

    public static void main(String[] args) throws Exception
    {
        final String host = args[0];
        final int port = Integer.parseInt(args[1]);
        final String address = "queue";

        Artemis1982 artemis1982 = new Artemis1982(host, port, address);

        artemis1982.putMessageOnQueue();
        artemis1982.consumeFailDeliveryForever();

        new CountDownLatch(1).await();
    }

    private Artemis1982(final String host, final int port, final String address)
    {
        _host = host;
        _port = port;
        _address = address;
    }

    private void putMessageOnQueue() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(1);
        Vertx vertx = Vertx.vertx();

        try
        {
            ProtonClient client = ProtonClient.create(vertx);

            client.connect(_host, _port, res -> {
                if (res.succeeded())
                {

                    ProtonConnection connection = res.result();
                    connection.open();

                    ProtonSender sender = connection.createSender(_address);

                    sender.sendQueueDrainHandler(event -> {

                        Message message = Proton.message();
                        message.setBody(new AmqpValue(1));
                        sender.send(message, delivery -> {
                            latch.countDown();
                        });
                    });

                    sender.open();
                }
                else
                {
                    throw new RuntimeException(res.cause());
                }
            });

            latch.await();
            vertx.runOnContext(event -> {
                System.out.format("Successfully put message on queue%n");
            });
        }
        finally
        {
            vertx.close();
        }
    }

    private void consumeFailDeliveryForever()
    {
        Vertx vertx = Vertx.vertx();

        ProtonClient client = ProtonClient.create(vertx);

        client.connect(_host, _port, (AsyncResult<ProtonConnection> res) -> {
            if (res.succeeded())
            {
                ProtonConnection connection = res.result();
                connection.open();
                ProtonReceiver protonReceiver = connection
                        .createReceiver(_address)
                        .setAutoAccept(false)
                        .setPrefetch(100)

                        .handler((delivery, msg) -> {
                            Modified deliveryFailed = new Modified();
                            deliveryFailed.setDeliveryFailed(true);
                            deliveryFailed.setUndeliverableHere(false);

                            System.out.format("Failing delivery for tag %s with delivery count %d%n",
                                              DatatypeConverter.printBase64Binary(delivery.getTag()),
                                              msg.getDeliveryCount());

                            delivery.disposition(deliveryFailed, true);
                        });

                protonReceiver.open();
            }
            else
            {
                throw new RuntimeException(res.cause());
            }
        });


        System.out.format("Once delivery count for the message exceeds the DLQ threshold, check the console %n"
                          + "and observe that the PersistentSize, DeliveringCount and MessageCount have all gone negative.");
    }
}