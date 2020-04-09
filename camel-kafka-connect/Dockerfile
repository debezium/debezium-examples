FROM debezium/connect:1.1

# Copy all sink dependencies to a destination connector directory
COPY coap-connector-dist/target/dependency/ $KAFKA_CONNECT_PLUGINS_DIR/camel-kafka-connector/
