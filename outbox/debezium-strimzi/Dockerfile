ARG STRIMZI_VERSION=latest-kafka-3.4.0
FROM quay.io/strimzi/kafka:${STRIMZI_VERSION}

ARG DEBEZIUM_CONNECTOR_VERSION=2.4.0.Alpha1
ENV KAFKA_CONNECT_PLUGIN_PATH=/tmp/connect-plugins/
ENV KAFKA_CONNECT_LIBS=/opt/kafka/libs

#RUN mkdir $KAFKA_CONNECT_PLUGIN_PATH &&\
#    cd $KAFKA_CONNECT_PLUGIN_PATH &&\
#    curl -sfSL https://repo1.maven.org/maven2/io/debezium/debezium-connector-postgres/${DEBEZIUM_CONNECTOR_VERSION}/debezium-connector-postgres-${DEBEZIUM_CONNECTOR_VERSION}-plugin.tar.gz | tar xz &&\
#    cd $KAFKA_CONNECT_LIBS &&\
#    curl -sfSL https://repo1.maven.org/maven2/io/debezium/debezium-interceptor/${DEBEZIUM_CONNECTOR_VERSION}/debezium-interceptor-${DEBEZIUM_CONNECTOR_VERSION}.jar

#TODO Uncomment the code above and delete the code below when the PR DBZ-2862 is integrated into a connector version and the debezium-interceptor package is available.
COPY "debezium-connector-postgres-2.4.0-SNAPSHOT-plugin.tar.gz" /
COPY "debezium-interceptor-2.4.0-SNAPSHOT.jar" $KAFKA_CONNECT_LIBS

RUN mkdir $KAFKA_CONNECT_PLUGIN_PATH &&\
    cd $KAFKA_CONNECT_PLUGIN_PATH &&\
    tar xzvf /debezium-connector-postgres-2.4.0-SNAPSHOT-plugin.tar.gz
