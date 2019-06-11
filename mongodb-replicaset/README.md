# Using Debezium with Multi-Node MongoDB Replica Set

This demo shows how the Debezium connector for MongoDB supports failover to secondary nodes elected as new primaries in a MongoDB replica set.
It's based on the services as defined in the [Debezium Tutorial](http://debezium.io/docs/tutorial/).

Start the topology as defined in http://debezium.io/docs/tutorial/ (using a replica set with three nodes):

    $ export DEBEZIUM_VERSION=0.9
    $ docker-compose up

Initialize MongoDB replica set and insert some test data:

    $ docker-compose exec mongodb-1 bash -c '/usr/local/bin/init-inventory-replicaset.sh "mongodb-1:27017 mongodb-2:27017 mongodb-3:27017"'

Start MongoDB connector:

    $ curl -i -X POST -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/ -d @register-mongodb-replicaset.json

Consume messages from the topic created for the "customers" collection:

    $ docker-compose exec kafka /kafka/bin/kafka-console-consumer.sh \
        --bootstrap-server kafka:9092 \
        --from-beginning \
        --property print.key=true \
        --topic dbserver1.inventory.customers

Shut down the current primary node:

    $ docker-compose stop mongodb-1

Find out which MongoDB node is the new primary one:

    $ docker-compose exec mongodb-2 bash -c 'mongo inventory --eval "rs.status()"'

Modify records in the database via MongoDB client, connecting to new primary
(assuming this is _mongodb-2_ in the following):

    $ docker-compose exec mongodb-2 bash -c 'mongo inventory'

    rs0:PRIMARY> db.customers.insert([
        { \_id : NumberLong("1005"), first_name : 'Bob', last_name : 'Hopper', email : 'thebob@example.com' }
    ]);

The connector should have automatically failed over to the new primary, so the new record should show up in the topic inspected above.

Step down as primary, which will make the third node the new primary:

    rs0:PRIMARY> rs.stepDown(120);
    rs0:PRIMARY> exit;

Modify records in the database via MongoDB client, connecting to new primary
(assuming this is _mongodb-3_ in the following):

    $ docker-compose exec mongodb-3 bash -c 'mongo inventory'

    rs0:PRIMARY> db.customers.insert([
        { \_id : NumberLong("1006"), first_name : 'Bob', last_name : 'Hopper', email : 'thebob@example.com' }
    ]);

Again the connector should have automatically failed over to the new primary, so the new record should show up in the topic inspected above.

Shut down the cluster:

    docker-compose down
