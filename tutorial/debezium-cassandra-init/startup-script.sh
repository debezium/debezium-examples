#!/bin/sh

sh /opt/cassandra/bin/cassandra -f &

while ! grep -q "Created default superuser role 'cassandra" /opt/cassandra/logs/system.log
do
  sleep 1
done;

cqlsh -f $DEBEZIUM_HOME/inventory.cql

java -Dlog4j.debug -Dlog4j.configuration=file:$DEBEZIUM_HOME/log4j.properties -jar $DEBEZIUM_HOME/debezium-connector-cassandra.jar $DEBEZIUM_HOME/config.properties
