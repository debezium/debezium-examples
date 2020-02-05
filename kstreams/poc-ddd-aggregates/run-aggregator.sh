#!/bin/bash

export PATH="/opt/poc-ddd-aggregates/jdk/bin:${PATH}"
export JAVA_APP_DIR=/opt/poc-ddd-aggregates/lib
export JAVA_MAIN_CLASS=io.debezium.examples.aggregation.StreamingAggregatesDDD

exec /opt/poc-ddd-aggregates/run-java.sh "$PARENT_TOPIC" "$CHILDREN_TOPIC" "$BOOTSTRAP_SERVERS"
