#Project 2 (RPC)

Implementation instruction -

Run docker-compose located in root directory with following command.
Replace “number” by total number of pubsubs(either publishers or subscribers) required,
```
docker-compose up -d --scale pubsubagent=”number”
```
OR to rebuild,
```
docker-compose up --build -d --scale pubsubagent=”number”
```

Determine the container IDs by following command,
```
docker ps
```
Check the “Names” section from above output and remember which new terminal will be for what program( i.e. where to run EventManager and the rest).

Run individual containers with replacing “containerID” with container ID from above output,
```
docker exec -it “containerID” sh
```

After entering terminal of containers run following for specified containers,

Run EventManager in its specific container ONLY,
```
java -cp target/pubsub-1.0-SNAPSHOT.jar edu.rit.cs.EventManager
```

Run PubSubAgent in any of remaining containers,
```
java -cp target/pubsub-1.0-SNAPSHOT.jar edu.rit.cs.PubSubAgent 172.16.1.2:10000
```
