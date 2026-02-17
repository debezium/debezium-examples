# Podman Postgres Example

In this example you will find how to run a Debezium example running podman commands. 

## Requirements

In order to run this example you properly you only need two things:

- Internet connection to download Podman and the container images.
- Podamn installed in your device. You can look at the installation instructions [here](https://podman.io/docs/installation). 

## Running the example

If you work on Windows or MacOs, you need to perform the step zero. This step consist on starting a podman machine in order to run the containers in there. You can skip this step if your OS is any Linux distribution.

``` 
podman machine init --cpus 8 --memory 16384  
podman machine start
```

Look at the [podman machine instructions](https://docs.podman.io/en/latest/markdown/podman-machine.1.html) to tweak your command.

If we want all the different networks to communicate over the same network, we will run them all using the same network:

``` 
podman network create debezium-net
```

Then we are going to start with Kafka container. In order to achieve it, this is the command we need to run:

```
podman run -d \
    --name kafka \
    --network debezium-net \
    -p 9092:9092 \
    -p 9093:9093 \
    -e CLUSTER_ID=oh-sxaDRTcyAr6pFRbXyzA \
    -e NODE_ID=1 \
    -e NODE_ROLE=combined \
    -e KAFKA_LISTENERS=PLAINTEXT://kafka:9092,CONTROLLER://kafka:9093 \
    -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka:9092 \
    quay.io/debezium/kafka:3.4.1.Final
```  

Once we have Kafka running, we need to run the database. As this example says, we are going for Postgres:

``` 
podman run -d \
  --name postgres \
  --network debezium-net \
  -p 5432:5432 \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  quay.io/debezium/example-postgres:3.4.1.Final
```

With Kafka and the database running, we next go for Debezium, in this case working with Kafka Connect:

``` 
podman run -d \
  --name connect \
  --network debezium-net \
  -p 8083:8083 \
  -e BOOTSTRAP_SERVERS=kafka:9092 \
  -e GROUP_ID=1 \
  -e CONFIG_STORAGE_TOPIC=my_connect_configs \
  -e OFFSET_STORAGE_TOPIC=my_connect_offsets \
  -e STATUS_STORAGE_TOPIC=my_connect_statuses \
  quay.io/debezium/connect:3.4.1.Final
```

And with this, all the containers involved should be working fine. But if we want Debezium start snapshotting and/or streaming changes, we need to run the following command to create the connector:

``` 
curl -X POST -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/ -d @postgres-connector.json 
```

And now, we can start creating changes into the database and the changes are going to be streamed into Kafka. 