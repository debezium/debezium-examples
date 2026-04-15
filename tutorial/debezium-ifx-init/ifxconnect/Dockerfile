ARG DEBEZIUM_VERSION
FROM quay.io/debezium/connect:$DEBEZIUM_VERSION

ADD --chown=kafka:kafka --chmod=775 https://repo1.maven.org/maven2/com/ibm/informix/jdbc/15.0.1.1/jdbc-15.0.1.1.jar /kafka/connect/debezium-connector-informix/

