ARG DEBEZIUM_VERSION
FROM quay.io/debezium/connect:${DEBEZIUM_VERSION}

ARG ORACLE_JDBC_VERSION=23.5.0.24.07

USER root

# The Debezium JDBC sink connector is already part of the Debezium Connect
# image, so only the Oracle JDBC driver has to be added here - it is not
# bundled with the image. The driver is published to Maven Central under the
# Oracle Free Use Terms and Conditions licence.
#
# Note: the Confluent JDBC sink connector is NOT interchangeable with the
# Debezium one used by this example - it is distributed under the Confluent
# Community License, which restricts production use.
RUN curl -fsSL -o $KAFKA_CONNECT_PLUGINS_DIR/debezium-connector-jdbc/ojdbc11-${ORACLE_JDBC_VERSION}.jar \
    https://repo1.maven.org/maven2/com/oracle/database/jdbc/ojdbc11/${ORACLE_JDBC_VERSION}/ojdbc11-${ORACLE_JDBC_VERSION}.jar

USER kafka
