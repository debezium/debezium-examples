Deploying Debezium Server with Debezium Operator
===
This tutorial demonstrates how to stream changes from a PostgreSQL database into Apache Kafka with Debezium Server deployed in a Kubernetes cluster.

More detailed walktrough of this tutorial is available as an [article](https://debezium.io/blog/2023/10/19/Debezium-Operator-Takes-off-to-the-Clouds/) at our blog site. 


Preparing the Environment
---
As the first step we will provision a local Kubernetes cluster with a running PostgreSQL database and the Apache Kafka broker. The following script, when executed, will use [Kind](https://kind.sigs.k8s.io/) to provision a local k8s cluster and deploy the required infrastructure. 

```sh
./create-environment.sh
```

Deploying Debezium Operator
---
There are currently two ways to deploy the operator to your Kubernetes cluster. You can either apply a set of kubernetes manifests to your cluster, or directly from the [OperatorHub](https://operatorhub.io/) operator catalog.


### A: Deploying Debezium Operator from Operator Catalog
The use of [Operator Lifecycle Manager](https://olm.operatorframework.io/) allows you to configure the scope of namespaces watched by the operator from a single namespace to the entire cluster. The process below will install the operator into the `operators` namespace -- which is by default intended for cluster-wide operators.  

#### 1. Adding OLM to K8s Cluster
First we need to install OLM itself by running the following shell commands:

```sh
curl -L https://github.com/operator-framework/operator-lifecycle-manager/releases/download/v0.25.0/install.sh -o install.sh
chmod +x install.sh
./install.sh v0.25.0
```
_Note: this is a one-time process and any production k8s cluster which provides access to operator catalogs would already have OLM installed_

#### 2. Subscribing to Debezium Operator
When OLM is installed in your K8s cluster, all you need to do is subscribing to an operator channel by creating a subscription object in the namespace where you wish to deploy the operator of your choice. The following command will create a subscription instructing OLM to deploy _Debezium Operator_ into the `operators` namespace which is by default intended for operators monitoring the entire cluster.

```sh
kubectl create -f infra/010_debezium-subscription.yml 
```


### B: Deploying Debezium Operator without OLM
Debezium Operator deployed this way will be limited to managing the Debezium Server instances **only in the same namespace as the operator**. 

To deploy _Debezium Operator_ into the `debezium` namespace we need to execute the following commands:

```sh
kubectl create -f https://raw.githubusercontent.com/debezium/debezium-operator/2.4/k8/debeziumservers.debezium.io-v1.yml
kubectl create -f https://raw.githubusercontent.com/debezium/debezium-operator/2.4/k8/kubernetes.yml -n debezium
```

Deploying Debezium Server
---
With Debezium Operator up and running an instance of Debezium Server can be deployed into the kubernetes cluster simply by creating a `DebeziumServer` resource.

```sh
kubectl create -f https://raw.githubusercontent.com/debezium/debezium-operator/2.4/examples/postgres/010_debezium-server-ephemeral.yml -n debezium
```

You can check that the deployed Debezium Server instance in running with the following command:

```sh
$ kubectl get deployments -n debezium

NAME                        READY   UP-TO-DATE   AVAILABLE 
my-debezium                 1/1     1            1 
```

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