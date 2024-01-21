Deploying Debezium Server with Google Pub/Sub
===
This tutorial demonstrates how to stream changes from a PostgreSQL database into [Google Cloud Pub/Sub](https://cloud.google.com/pubsub/docs) with Debezium Server deployed in a Kubernetes cluster.

Prerequisities
---
The following prerequisities are required 

- Installed [Kind](https://kind.sigs.k8s.io/) to provision a local k8s cluster
- Installed [kubectl](https://kubernetes.io/docs/reference/kubectl/) client to manage k8s cluster
- Installed [gcloud](https://cloud.google.com/sdk/gcloud) CLI 
- Google cloud service account [json key](https://console.cloud.google.com/apis/credentials)
- Update `env.sh` (at minimum set the `PUBSUB_PROJECT_ID` and `PUBSUB_CREDENTIALS_FILE` variables)
- Installed [jq]https://jqlang.github.io/jq/ too format messages pulled from Pub/Sub

Creating Local Kubernetes Cluster
---
Simply run the following script

```sh
./create-environment.sh
```

Deploying Debezium Operator
---
To deploy Debezium operator create an OLM subscription 

```sh
kubectl create -f k8s/operator/001_subscription.yml 
```

Configure Pub/Sub Connection
---

Run the following script to initialize your Pub/Sub environment

```sh
./pubsub.sh
```
The script performs several steps

- Generates k8s secret to `k8s/debezium/001_pubsub/yml` used by Debezium in the next step
- Deletes Pub/Sub topic and subscription if any of them already exists
- Creates the Pub/Sub topic and subscription anew

Deploying Debezium Server with PostgreSQL Source and Pub/Sub Sink
---
With Debezium Operator up and running an instance of Debezium Server can be deployed into the kubernetes cluster by simply creating a `DebeziumServer` resource.

```sh
source env.sh
kubectl create -f k8s/debezium/ -n $NAMESPACE
```

You can check that the deployed Debezium Server instance in running with the following command:

```sh
$ kubectl get deployments -n $NAMESPACE

NAME                        READY   UP-TO-DATE   AVAILABLE 
my-debezium                 1/1     1            1 
```

Verify Messages in Pub/Sub
---
To verify the messaged sent to Pub/Sub topic, pull your subscription by running the following script

```shell
./pull.sh 4
```

_Note: The number passed to the `pull.sh` script is the number of expected messages. In this case the connector emitted 4 messages for the initial data in the PostgreSQL database_

Cleanup
---
To remove the Kubernetes environment used in this tutorial, execute the cleanup script:

```sh
./destroy-environment.sh
```