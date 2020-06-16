ARG DEBEZIUM_VERSION
FROM debezium/connect:$DEBEZIUM_VERSION
ENV KAFKA_CONNECT_DEBEZIUM_DIR=$KAFKA_CONNECT_PLUGINS_DIR/debezium-connector-mysql
ENV APICURIO_VERSION=1.2.2.Final

RUN cd $KAFKA_CONNECT_DEBEZIUM_DIR &&\
    curl https://repo1.maven.org/maven2/io/apicurio/apicurio-registry-distro-connect-converter/$APICURIO_VERSION/apicurio-registry-distro-connect-converter-$APICURIO_VERSION-converter.tar.gz | tar xzv
