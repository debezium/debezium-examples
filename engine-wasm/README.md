# Streaming Database Changes to a TinyGo Application

[Debezium](https://debezium.io/) allows to capture and stream change events from multiple databases such as MySQL and PostgreSQL and is mostly used with Apache Kafka as the underlying messaging infrastructure.

Using [Debezium Engine](https://debezium.io/documentation/reference/3.0/development/engine.html) it is possible though to stream database changes as in-process solution and execute an arbitrary Java code.
It is also possible to execute a code written in a different language.
This can be for example achieved with [Java Scripting API](https://docs.oracle.com/javase/8/docs/technotes/guides/scripting/prog_guide/api.html) for languages that run on JVM.

[WebAssembly](https://webassembly.org/) is a different approach to handle the task.
Project [Chicory](https://github.com/dylibso/chicory) is a JVM native WebAssembly runtime.
It allows you to run WebAssembly programs with zero native dependencies or JNI.
This example combines Debezium Engine and Chicory to execute a code written in [TinyGo](https://tinygo.org/) for changes done on a MySQL database.

## Prerequisites

* Java 21 development environment
* (Optional) TinyGo compiler

## Running the Demo

### Build TinyGo module (optional)

The example comes with the pre-compiled TinyGo module with the executed code.
The TinyGo source is located in `src/main/resources/go/cdc.go`file and its compiled representation in `src/main/resources/compiled/cdc.wasm` file.

If you want to make any changes to the code then you need to [install TinyGo](https://tinygo.org/getting-started/install/) on your local machine and you can use script `build-wasm.sh` to (re-)build the module.

### Starting the MySQL Source Database

We will start a pre-populated MySQL database that is the same as used by the Debezium [tutorial](https://debezium.io/docs/tutorial/):

```
docker run -it --rm --name mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=debezium -e MYSQL_USER=mysqluser -e MYSQL_PASSWORD=mysqlpw quay.io/debezium/example-mysql:3.0
```

### Connecting to the Database

Start the application that uses Debezium Engine to get change events from database to execute TinyGo code.

```
mvn clean install exec:java
```

The terminal window will contain lines like:

```
Received message for destination 'wasm.inventory.customers', with id = '1004' and content {"before":null,"after":{"id":1004,"first_name":"Anne","last_name":"Kretchmar","email":"annek@noanswer.org"},"source":{"version":"3.0.1.Final","connector":"mysql","name":"wasm","ts_ms":1731327511000,"snapshot":"true","db":"inventory","sequence":null,"ts_us":1731327511000000,"ts_ns":1731327511000000000,"table":"customers","server_id":0,"gtid":null,"file":"mysql-bin.000003","pos":797,"row":0,"thread":null,"query":null},"transaction":null,"op":"r","ts_ms":1731327512006,"ts_us":1731327512006319,"ts_ns":1731327512006319782}
...
```

that are emitted by the module.


### Updating Records in the Database

Now update a record in the database:

```
docker exec -it mysql bash -c 'mysql -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" inventory'

update customers set first_name = 'Sarah' where id = 1001;
```

The application terminal will contain the new line with update record message:

```
Received message for destination 'wasm.inventory.customers', with id = '1001' and content {"before":{"id":1001,"first_name":"Sally","last_name":"Thomas","email":"sally.thomas@acme.com"},"after":{"id":1001,"first_name":"Sarah","last_name":"Thomas","email":"sally.thomas@acme.com"},"source":{"version":"3.0.1.Final","connector":"mysql","name":"wasm","ts_ms":1731327659000,"snapshot":"false","db":"inventory","sequence":null,"ts_us":1731327659000000,"ts_ns":1731327659000000000,"table":"customers","server_id":223344,"gtid":null,"file":"mysql-bin.000003","pos":1041,"row":0,"thread":102,"query":null},"transaction":null,"op":"u","ts_ms":1731327659957,"ts_us":1731327659957757,"ts_ns":1731327659957757335}
```
