# Deploying Debezium on OpenShift

This demo shows how to deploy Debezium on a developer OpenShift instance using [CodeReady Containers](https://developers.redhat.com/products/codeready-containers/overview).
The deployment leverages the [Strimzi](https://strimzi.io/) project, which aims to simplify the deployment of Apache Kafka on Kubernetes by means of _custom resources_.

## Prerequisites:

* [Installed and running](https://console.redhat.com/openshift/create/local) CodeReady Containers.
* To keep containers separated from other workloads on the cluster, create a dedicated project for this example. In the remainder of this document, the `debezium-example` namespace will be used:

```
oc new-project debezium-example
```

## Deploying Strimzi operator and Kafka

[Strimzi operator](https://operatorhub.io/operator/strimzi-kafka-operator) has to be deployed into the OpenShift cluster as the first step.
In the OperatorHub tab select Strimzi and click on install button.
Alternatively, you can install it from the command line:

```
oc create -f strimzi-kafka-operator.yaml
```

Once the Strimzi operator is deployed, we can deploy Kafka cluster into our project:

```
oc create -f kafka.yaml
```

## Creating a secret for the database credentials

To allow Debezium Kafka connector to connect to the database, we will have to provide the database credentials to the connector.
For security reasons, it's a good practice not to provide the credentials directly, but keep them in a separate secured place.
OpenShift provides `Secret` object for this purpose.
Let's create one with database credentials:

```
oc create -f secret.yaml
```

Besides that, we have to also create a role which is allowed to read this secret:

```
oc create -f role.yaml
```

In the next step, we will create Kafka Connect cluster.
Besides creating the cluster itself, Strimzi does couple of other things.
It also creates a service account for this cluster.
To allow Kafka Connect to access our secret, we have to bind Kafka Connect service account to the role created in previous step, which is allowed read the secret:

```
oc create -f role-binding.yam
```

Now, we have all reparation steps done and can deploy source database and Kafka Connect cluster.

## Deploying data source and Debezium connector

As a data source, MySQL will be used in this example.
Besides running a pod with MySQL database, an appropriate service which will point to the pod with DB itself is needed.
It can be created e.g. as follows:

```
oc create -f mysql.yaml
```

To be able to deploy Kafka Connect into the cluster, we have to first create its image with the appropriate Debezium plugin (MySQL in this case).
However, Strimzi allows to merge image build with deployment itself, so we can do it at once:


```
oc create -f kafka-connect.yaml
```

Kafka Connect image with Debezium plugin is built and pushed into the internal OpenShift repository.
Afterwards, Kafka Connect is deployed from this image into the cluster.

As the last step we have to create Debezium connector for MySQL database:

```
oc apply -n debezium-example -f kafka-connector.yaml
```

As you can notice, we are not providing credential to the database directly in the connector object specification, but refer to our `Secret` object.

## Verify Deployment
By now, Debezium should stream the changes from MySQL database into Kafka.
To verify it, start watching `mysql.inventory.customers` Kafka topic:

```
oc run -n debezium-example -it --rm --image=quay.io/debezium/tooling:1.2  --restart=Never watcher -- kcat -b debezium-cluster-kafka-bootstrap:9092 -C -o beginning -t mysql.inventory.customers
```

Next, connect to MySQL database:

```
oc run -n debezium-example -it --rm --image=mysql:8.0 --restart=Never --env MYSQL_ROOT_PASSWORD=debezium mysqlterm -- mysql -hmysql -P3306 -uroot -pdebezium
```

and do some changes, e.g.:

```
> update customers set first_name="Sally Marie" where id=1001;
```


## Clean up

To clean up, just remove the project:

```
oc delete project debezium-example
```
