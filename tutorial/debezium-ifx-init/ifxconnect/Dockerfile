ARG DEBEZIUM_VERSION
FROM quay.io/debezium/connect:$DEBEZIUM_VERSION

ADD --chown=kafka:kafka --chmod=775 https://repo1.maven.org/maven2/com/ibm/informix/jdbc/4.50.10/jdbc-4.50.10.jar /kafka/connect/debezium-connector-informix/
ADD --chown=kafka:kafka --chmod=775 https://repo1.maven.org/maven2/com/ibm/informix/ifx-changestream-client/1.1.3/ifx-changestream-client-1.1.3.jar /kafka/connect/debezium-connector-informix/

