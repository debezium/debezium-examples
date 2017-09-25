# Debezium Unwrap SMT Demo

This setup is going to demonstrate how to receive events from MySQL database and stream them down to PostreSQL database using [Debezium Event Flattening SMT](http://debezium.io/docs/configuration/event-flattening/).


## Topology

```
                   +-------------+
                   |             |
                   |    MySQL    |
                   |             |
                   +------+------+
                          |
                          |
                          |
          +---------------v------------------+
          |                                  |
          |           Kafka Connect          |
          |  (Debezium, JDBC, ElasticSearch  |
          |           connectors)            |
          |                                  |
          +---+-----------+------------------+
                          |
                          |
                          |
                          |
                  +-------v--------+
                  |                |
                  |   PostgreSQL   |
                  |                |
                  +----------------+


```
We are using a Docker Compose to deploy following components
* MySQL
* Kafka
  * ZooKeeper
  * Kafka Broker
  * Kafka Connect with [Debezium](http://debezium.io/) and  [JDBC](https://github.com/confluentinc/kafka-connect-jdbc) Connectors
* PostgreSQL

## Usage
How to run:

```shell
# Start the application
DEBEZIUM_VERSION=0.6
docker-compose up

# Start PostgreSQL connector
curl -i -X POST -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/ -d @jdbc-sink.json

# Start MySQL connector
curl -i -X POST -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/ -d @source.json
```

Check content of MySQL database
```shell
docker-compose exec mysql bash -c 'mysql -u $MYSQL_USER  -p$MYSQL_PASSWORD inventory -e "select * from customers"'
+------+------------+-----------+-----------------------+
| id   | first_name | last_name | email                 |
+------+------------+-----------+-----------------------+
| 1001 | Sally      | Thomas    | sally.thomas@acme.com |
| 1002 | George     | Bailey    | gbailey@foobar.com    |
| 1003 | Edward     | Walker    | ed@walker.com         |
| 1004 | Anne       | Kretchmar | annek@noanswer.org    |
+------+------------+-----------+-----------------------+
```

Verify that PostgreSQL database has the same content
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
### New record
Insert a new record into MySQL
```shell
docker-compose exec mysql bash -c 'mysql -u $MYSQL_USER  -p$MYSQL_PASSWORD inventory'
mysql> insert into customers values(default, 'Reinhard', 'Scheer', 'cic@hsf.de');
Query OK, 1 row affected (0.02 sec)
```

PostgreSQL contains a new record
```shell
docker-compose exec -postgres bash -c 'psql -U $POSTGRES_USER $POSTGRES_DB -c "select * from customers"'
 last_name |  id  | first_name |         email         
-----------+------+------------+-----------------------
...
Scheer    | 1005 | Reinhard   | cic@hsf.de
(5 rows)
```

### Record update
Update a record in MySQL
```shell
mysql> update customers set first_name='Franz', last_name='von Hipper' where last_name='Scheer';
Query OK, 1 row affected (0.02 sec)
Rows matched: 1  Changed: 1  Warnings: 0
```

Verify that record in PostgreSQL is updated
```shell
docker-compose exec postgres bash -c 'psql -U $POSTGRES_USER $POSTGRES_DB -c "select * from customers"'
 last_name |  id  | first_name |         email         
-----------+------+------------+-----------------------
...
von Hipper | 1005 | Franz      | cic@hsf.de
(5 rows)
```

End application
```shell
# Shut down the cluster
docker-compose down
```
