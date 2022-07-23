# Debezium Unwrap MongoDB SMT Demo

This example shows how to capture events from a MongoDB database and stream them to a relational database (Postgres in this case).
In order to convert the CDC events ebitted by Debezium's MongoDB connector into a "flat" structure consumable by the JDBC sink connector, [Debezium MongoDB Event Flattening SMT](https://debezium.io/docs/configuration/mongodb-event-flattening/) is used.

We are using Docker Compose to deploy the following components:

* MongoDB
* Kafka
  * ZooKeeper
  * Kafka Broker
  * Kafka Connect with the [Debezium CDC](https://debezium.io/) and [JDBC sink](https://github.com/confluentinc/kafka-connect-jdbc) connectors as well as the Postgres JDBC driver
* PostgreSQL

## Preparations

```shell
# Start the application
export DEBEZIUM_VERSION=1.9
docker-compose up --build -d

# Initialize MongoDB replica set and insert some test data
docker-compose exec mongodb bash -c '/usr/local/bin/init-inventory.sh'

# Current host
# if using docker-machine:
export CURRENT_HOST=$(docker-machine ip $(docker-machine active));
# or any other host
# export CURRENT_HOST='localhost' //or your host name 

# Start JDBC sink connector
curl -i -X POST -H "Accept:application/json" -H  "Content-Type:application/json" http://$CURRENT_HOST:8083/connectors/ -d @jdbc-sink.json

# Start Debezium MongoDB CDC connector
curl -i -X POST -H "Accept:application/json" -H  "Content-Type:application/json" http://$CURRENT_HOST:8083/connectors/ -d @mongodb-source.json
```

## Verify initial sync

Check contents of the MongoDB database:

```shell
docker-compose exec mongodb bash -c 'mongo -u $MONGODB_USER -p $MONGODB_PASSWORD --authenticationDatabase admin inventory --eval "db.customers.find()"'

{ "_id" : NumberLong(1001), "first_name" : "Sally", "last_name" : "Thomas", "email" : "sally.thomas@acme.com" }
{ "_id" : NumberLong(1002), "first_name" : "George", "last_name" : "Bailey", "email" : "gbailey@foobar.com" }
{ "_id" : NumberLong(1003), "first_name" : "Edward", "last_name" : "Walker", "email" : "ed@walker.com" }
{ "_id" : NumberLong(1004), "first_name" : "Anne", "last_name" : "Kretchmar", "email" : "annek@noanswer.org" }
```

Verify that the PostgreSQL database has the same content:

```shell
docker-compose exec postgres bash -c 'psql -U $POSTGRES_USER $POSTGRES_DB -c "select * from customers"'
 last_name |  id  | first_name |         email
-----------+------+------------+-----------------------
 Thomas    | 1001 | Sally      | sally.thomas@acme.com
 Bailey    | 1002 | George     | gbailey@foobar.com
 Walker    | 1003 | Edward     | ed@walker.com
 Kretchmar | 1004 | Anne       | annek@noanswer.org
(4 rows)
```

## Adding a new record

Insert a new record into MongoDB:

```shell
docker-compose exec mongodb bash -c 'mongo -u $MONGODB_USER -p $MONGODB_PASSWORD --authenticationDatabase admin inventory'

MongoDB server version: 3.4.10
rs0:PRIMARY>

db.customers.insert([
    { _id : NumberLong("1005"), first_name : 'Bob', last_name : 'Hopper', email : 'bob@example.com' }
]);

...
"nInserted" : 1
...
```

Verify that PostgreSQL contains the new record:

```shell
docker-compose exec postgres bash -c 'psql -U $POSTGRES_USER $POSTGRES_DB -c "select * from customers"'

 last_name |  id  | first_name |         email
-----------+------+------------+-----------------------
...
Hopper        | 1005 | Bob       | bob@example.com
(5 rows)
```

## Updating a record

Update a record in MongoDB:

```shell
docker-compose exec mongodb bash -c 'mongo -u $MONGODB_USER -p $MONGODB_PASSWORD --authenticationDatabase admin inventory'

MongoDB server version: 3.4.10
rs0:PRIMARY>

db.customers.update(
   {
    _id : NumberLong("1005")
   },
   {
     $set : {
       first_name: "Billy-Bob"
     }
   }
);
```

Verify that record in PostgreSQL is updated:

```shell
docker-compose exec postgres bash -c 'psql -U $POSTGRES_USER $POSTGRES_DB -c "select * from customers"'

 last_name |  id  | first_name |         email
-----------+------+------------+-----------------------
...
 Hopper    | 1005 | Billy-Bob  | bob@example.com
(5 rows)
```

```shell
docker-compose exec mongodb bash -c 'mongo -u $MONGODB_USER -p $MONGODB_PASSWORD --authenticationDatabase admin inventory'

MongoDB server version: 3.4.10
rs0:PRIMARY>
db.customers.remove(
   {
    _id: NumberLong("1005")
   }
);   
```

Verify that record in PostgreSQL is deleted:

```shell
docker-compose exec postgres bash -c 'psql -U $POSTGRES_USER $POSTGRES_DB -c "select * from customers"'

 last_name |  id  | first_name |         email
-----------+------+------------+-----------------------
...
(4 rows)
```

There should be no record of `Billy Bob Hopper`.

End application:

```shell
docker-compose down
```
