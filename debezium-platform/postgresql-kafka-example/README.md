Using Debezium-platfrom to manage and stream changes
===
This example  will walk you through on how to use the Debezium Management Platform to manage and stream changes from a PostgreSQL database into Apache Kafka.


Preparing the Environment
---
As the first step we will provision a local Kubernetes cluster using [minikube](https://minikube.sigs.k8s.io/docs/) and will install an ingress controller. For this example, considering a local setup, we will use the `/etc/hosts` to resolve the domain.
The following script, when executed, will use minikube to provision a local k8s cluster named `debezium` and will add the required ingress controllers. It will also update the `/etc/hosts` to add the domain url.

```sh
./create-environment.sh
```
> **_NOTE:_**
If you are using minikube on Mac, you need also to run the `minikube tunnel -p debezium` command. For more details see [this](https://minikube.sigs.k8s.io/docs/drivers/docker/#known-issues) and [this](https://stackoverflow.com/questions/70961901/ingress-with-minikube-working-differently-on-mac-vs-ubuntu-when-to-set-etc-host).

Now that you have the required k8s environment setup, its time to fire the required infra for this example. As we will be using PostgreSQL database and the Apache Kafka broker as ssource and the destination for our pipeline. The following script will create a dedicated namespace `debezium-platform` and use it going forward for further installations of our example. It will alos provision the both PostgreSQL database and the Apache Kafka broke

```shell
./setup-infra.sh
```

Deploying Debezium-platform Operator
---
We will install debezium-platfrom platform through helm 

```shell
cd helm && 
helm dependency build &&
helm install debezium-platform . -f ./example.yaml &&
cd ..
```

after all pods are running you should access the Debezium-platform-stage(UI) from `http://platform.debezium.io/`, now you have completed the installing and running the debezium-platform part.

Using the debezium-platfrom-stage(UI) for seting up our data pipeline 
---
Now once you have running platfrom-stage(UI), we will create a data pipeline and all its resources i.e source, destination and transform(as per need) thru it. You will see different side navigation option to configure them.

For this demo, see the connection properties you can use for each connector type as illustrated below:

### Source
 #### PostgreSQL

 ```shell
  {
  "name": "test-source",
  "description": "postgreSQL database",
  "type": "io.debezium.connector.postgresql.PostgresConnector",
  "schema": "schema123",
  "vaults": [],
  "config": {
    "topic.prefix": "inventory",
    "database.port": 5432,
    "database.user": "debezium",
    "database.dbname": "debezium",
    "database.hostname": "postgresql",
    "database.password": "debezium",
    "schema.include.list": "inventory"
  }
}

 ```

![PostgreSQL Connnector](./resources/source.png)


### Destination
 ```shell
 {
  "name": "test-destination",
  "description": "Kafka destination",
  "type": "kafka",
  "schema": "schema123",
  "vaults": [],
  "config": {
    "producer.key.serializer": "org.apache.kafka.common.serialization.StringSerializer",
    "producer.value.serializer": "org.apache.kafka.common.serialization.StringSerializer",
    "producer.bootstrap.servers": "dbz-kafka-kafka-bootstrap.debezium-platform:9092"
  }
}

 ```

 ![Kafka Connnector](./resources/destination.png)

### Transform

**Transform class**: o.debezium.transforms.ExtractNewRecordState       
**Transform name**: Debezium marker      
**Description**: Extract Debezium payload  
**Adds the specified fields to the header if they exist**: db,table  
**Adds the specified field(s) to the message if they exist.**: op  
**Predicate type**: org.apache.kafka.connect.transforms.predicates.TopicNameMatches    
**Pattern**: inventory.inventory.products

 ```shell
 {
  "config": {
    "add.fields": "op",
    "add.headers": "db,table"
  },
  "description": "Extract Debezium payload",
  "name": "Debezium marker",
  "predicate": {
    "config": {
      "pattern": "inventory.inventory.products"
    },
    "negate": false,
    "type": "org.apache.kafka.connect.transforms.predicates.TopicNameMatches"
  },
  "schema": "string",
  "type": "io.debezium.transforms.ExtractNewRecordState",
  "vaults": []
}

 ```

 ![ExtractNewRecordState](./resources/transform.png)

### Pipeline
The use of [Operator Lifecycle Manager](https://olm.operatorframework.io/) allows you to configure the scope of namespaces watched by the operator from a single namespace to the entire cluster. The process below will install the operator into the `operators` namespace -- which is by default intended for cluster-wide operators. 






Verifying Change Events
---
You can verify that the _Debezium Server_ instance deployed in the previous section consumed all initial data from the database with the following command:

```sh
kubectl exec dbz-kafka-kafka-0 -n debezium -- /opt/kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server localhost:9092 \
    --from-beginning \
    --property print.key=true \
    --topic inventory.inventory.orders
```

Cleanup
---
To remove the Kubernetes environment used in this tutorial, execute the cleanup script:

```sh
./destroy-environment.sh
```