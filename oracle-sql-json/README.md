# Oracle SQL to JSON Converter

Debezium user can use `schema-changes` signal to informa Oracle Connector that is it is necessary to alter the table represenation in Debezium database schema.
This tool allow the user to convert a `CREATE TABLE` statement describing the table structure into the JSON message that can be used in signal message.

## Usage

```
$ mvn clean install
$ mvn exec:java -Dsql.catalog=orapdb1 -Dsql.schema=debezium -Dsql.file=example.sql
```

The JSON is printed to stdout.
