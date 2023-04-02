FROM quay.io/debezium/connect:2.0
ENV KAFKA_CONNECT_MONGODB_DIR=$KAFKA_CONNECT_PLUGINS_DIR/kafka-connect-mongodb

USER root
RUN microdnf -y install git maven java-11-openjdk-devel && microdnf clean all

USER kafka

# Deploy MongoDB Sink Connector
RUN mkdir -p $KAFKA_CONNECT_MONGODB_DIR && cd $KAFKA_CONNECT_MONGODB_DIR && \
  git clone https://github.com/hpgrahsl/kafka-connect-mongodb.git && \
  cd kafka-connect-mongodb && \
  git fetch --tags && \
  git checkout tags/v1.2.0 && \
  sed -i 's/http:\/\/packages.confluent.io\/maven\//https:\/\/packages.confluent.io\/maven\//g' pom.xml && \
  mvn clean package -DskipTests=true -DskipITs=true && \
  mv target/kafka-connect-mongodb/kafka-connect-mongodb-1.2.0-jar-with-dependencies.jar $KAFKA_CONNECT_MONGODB_DIR && \
  cd .. && rm -rf $KAFKA_CONNECT_MONGODB_DIR/kafka-connect-mongodb
