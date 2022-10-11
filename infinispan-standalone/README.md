# Oracle with a standalone Infinispan cluster

This demo deploys an Apache Kafka Connect cluster and a standalone Infinispan cluster that will be used for buffering in-progress transactions by the Debezium Oracle connector.

## Setup

This demo assumes that Oracle is running on `localhost` or by means of running within a VM or Docker container with appropriate configurations described in the [documentation](https://debezium.io/documentation/reference/stable/connectors/oracle.adoc) using LogMiner.

You must also download the [Oracle instant client for Linux](http://www.oracle.technetwork/topics/linuxx86-64soft-092277.html) and install it under the directory _debezium-with-oracle-jdbc/oracle_instantclient_.

Now lets set up this demo by specifying the Debezium version we wish to use.
Once that variable is set, lets start the services using `docker-compose`.

```shell
export DEBEZIUM_VERSION=2.0
docker-compose up --build
```

Insert the test data into the Oracle database.
The following assumes a Docker container running on `localhost` named `dbz_oracle` with a pluggable database named `ORCLPDB1`.
If your installation differs, adjust accordingly.

```shell
cat debezium-with-oracle-jdbc/init/inventory.sql | docker exec -i dbz_oracle sqlplus debezium/dbz@//localhost:1521/ORCLPDB1
```

## Example

Open the `register-oracle.json` file and adjust the name of the database server according to your environment.
Once adjusted, register the Debezium Oracle connector:

```shell
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" http://localhost:8083/connectors/ -d @register-oracle.json
```

The connector should have taken a snapshot of the existing records.
The snapshot can be inspected by using the `kafka-console-consumer` script.
In a new shell, run the following:

```shell
docker-compose exec kafka /kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server kafka:9092 \
  --from-beginning \
  --property print.key=true \
  --topic server1.DEBEZIUM.CUSTOMERS
```

Now lets test and verify how streaming with an Infinispan cluster works.
In a new shell, open up SQL*Plus, again adjusting the command accordingly to your environment:

```shell
docker exec -it dbz_oracle sqlplus debezium/dbz@//localhost:1521/ORCLPDB1
```

To make sure that `AUTOCOMMIT` is disabled, execute the following:

```shell
SQL> set autocommit off;
```

Now insert a new customer record, but do not commit the change.
We want this change to be written to the transaction log; however, without issuing a commit the transaction will be retained in the buffer until a commit is detected.
In the SQL*Plus terminal execute the following:

```shell
SQL> INSERT INTO debezium.customers values (1005, 'Daffy', 'Duck', 'daffy@loonytoons.com');
```

Now lets check the state of the buffered record in Infinispan.
Open a new shell and start the Infinispan CLI tool.
In the CLI tool, we want to inspect the contents of the `transactions` and `events` caches.  

The `transactions` cache describes the in-progress transaction's metadata such as the transaction identifier, the SCN that started the transaction and the time the transaction began.

The `events` cache describes the individual events that participate in a given transaction.
Each event consists of a type, the SQL command and its data, the time the event occurred and the SCN.

```shell
docker exec -it infinispan-standalone_infinispan_1 ./bin/cli.sh
```

Inside the CLI run the following command to connect to the Infinispan cluster.
If your docker environment is configured differently, the IP address the server bound to could be different and you can easily resolve the IP address by tailing the end of the Infinispan container's logs.

```shell
connect -u admin -p admin 172.26.0.2:11222
```

To inspect the contents of the `transactions` cache, execute the following commands in the Infinispan CLI:
```shell
cache transactions
ls
```

The output of the above will be the specific `transaction-id` for the open transaction we've started.
To inspect the contents of the `events` cache, execute the following commands in the Infinispan CLI:

```shell
cache events
ls
```

We can also see an entry gets returned.
This entry has the `tranasction-id` with a `-0` suffix which represents the `INSERT` we've done as a part of the current transaction.
So the customer `Daffy Duck` is cached.
Additionally, if you look at the `kafka-console-consumer` shell window where the topic is being tailed, no new record has arrived yet; this is expected.

Now lets commit the transaction and see what happens.
In SQL*Plus, commit the transaction:

```shell
SQL> commit;
```

Now if you check the `transactions` and `events` caches you'll notice they no longer contain any data.
Additionally, you'll notice that the new record now has appeared in the `kafka-console-consumer` shell window as well.

You can also follow the same steps but rather than committing the change, you can issue a rollback and see that the entries in Infinispan are removed but no event gets emitted to Kafka.

