ARG DEBEZIUM_VERSION
FROM quay.io/debezium/connect:$DEBEZIUM_VERSION

ARG DEBEZIUM_CONNECTOR_VERSION

# The SQLite connector is not part of the Connect image, so install its plugin archive
RUN curl -sSfL https://repo1.maven.org/maven2/io/debezium/debezium-connector-sqlite/$DEBEZIUM_CONNECTOR_VERSION/debezium-connector-sqlite-$DEBEZIUM_CONNECTOR_VERSION-plugin.tar.gz \
    | tar -xz -C $KAFKA_CONNECT_PLUGINS_DIR

# SQLite runs inside the connector's process, so the database lives in this container.
# The sqlite3 client is only needed to create and modify the sample data.
USER root
RUN microdnf -y install sqlite && microdnf clean all
RUN mkdir /data && chown kafka:kafka /data
USER kafka
