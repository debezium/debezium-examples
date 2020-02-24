FROM debezium/connect:1.0
ENV CAMEL_CONNECTOR_TAG=e507ea76de231c371b9a9f7ce90f1f4b90024914

USER root
RUN yum -y install git

RUN curl https://archive.apache.org/dist/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz > maven.tar.gz &&\
    mkdir maven &&\
    tar -xvz -C maven --strip-components 1 -f maven.tar.gz &&\
    cd ..

# Obtain Camel CoAP component and its dependencies
COPY pom.xml src/coap/
COPY coap-server/ src/coap/coap-server
RUN cd src/coap/coap-server && ../../../maven/bin/mvn dependency:copy-dependencies

# Build Camel Kafka Connect connector
RUN cd src &&\
    git clone --single-branch https://github.com/apache/camel-kafka-connector.git &&\
    cd camel-kafka-connector &&\
    git checkout $CAMEL_CONNECTOR_TAG &&\
    ../../maven/bin/mvn clean install -DskipTests

# Copy all JARs to a destination connector directory
RUN cd /kafka/src &&\
    find . &&\
    cp -R camel-kafka-connector/core/target/camel-kafka-connector-*-package/share/java/camel-kafka-connector $KAFKA_CONNECT_PLUGINS_DIR &&\
    cp -R coap/coap-server/target/dependency/*.jar $KAFKA_CONNECT_PLUGINS_DIR/camel-kafka-connector

USER jboss
