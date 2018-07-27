
Reproduction for defect ARTEMIS-1982.  Defect reproduced against 2.5.0, 2.6.2 and master (f0c13622ac7e821a81a354b0242ef5235b6e82df)

1. Create a fresh Artemis Broker

```
artemis create /some/where --user admin --password admin --allow-anonymous
```

2. Start the Broker

```
"/some/where/bin/artemis" run
```

3. Create an anycast queue 'queue'

```
curl --user admin:admin 'http://localhost:8161/console/jolokia/exec/org.apache.activemq.artemis:broker=%220.0.0.0%22/createQueue(java.lang.String,java.lang.String,boolean,java.lang.String)/queue/queue/false/ANYCAST/'
```

4. Run this reproduction

```
mvn clean package install  exec:java
```

5. Once the program consumes the message for the last time, ^C it and use observe the queue

```
curl --user admin:admin -k http://localhost:8161/console/jolokia/read/org.apache.activemq.artemis:broker=%220.0.0.0%22,component=addresses,address=%22queue%22,subcomponent=queues,routing-type=%22anycast%22,queue=%22queue%22
```

6. Observe that MessageCount, DeliveringCount, and DeliveringSize are all negative




