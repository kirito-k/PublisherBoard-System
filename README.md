# Publisher Board System(RPC)


## Prerequisites:
- Java
- Maven


## Implementation
- Clone this repository.
- Run the following commands in terminal.
- For running docker-compose if running this repository first time. Replace “number” by 
total number of pubsubs(either publishers or subscribers) required in system,
```
docker-compose up -d --scale pubsubagent=”number”
```
OR to rebuild(if you performed any changes later, use below command instead of the above),
```
docker-compose up --build -d --scale pubsubagent=”number”
```
- Determine the container IDs by following command,
```
docker ps
```
- Check the “Names” section from above output and remember which new terminal will be for 
what program (i.e. container IDs for eventmanager and the rest of pubsubs).
- Open new terminals, one for each peer and run individual containers 
with following command. Replace “containerID” with container ID from above output,
```
docker exec -it “containerID” sh
```
- Above command will open terminals of individual docker containers. Run the following command for specified containers,
- Firstly, run following command in eventmanager container(terminal opened using eventmanager ContainerID)
```
java -cp target/pubsub-1.0-SNAPSHOT.jar edu.rit.cs.EventManager
```
- Once the eventmanager container is up, run following command in any PubSubAgent containers remaining,
```
java -cp target/pubsub-1.0-SNAPSHOT.jar edu.rit.cs.PubSubAgent 172.16.1.2:10000
```

Very clean and easy to understand GUI menu will show up. Let me know if you have any issues.

Enjoy!