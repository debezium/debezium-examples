FROM quay.io/debezium/connect:1.9
ENV KAFKA_CONNECT_MONGODB_DIR=$KAFKA_CONNECT_PLUGINS_DIR/kafka-connect-mongodb

USER root
RUN microdnf -y install git maven && microdnf clean all

USER kafka

# Deploy MongoDB Sink Connector
RUN mkdir -p $KAFKA_CONNECT_MONGODB_DIR && cd $KAFKA_CONNECT_MONGODB_DIR && \
  git clone https://github.com/hpgrahsl/kafka-connect-mongodb.git && \
  cd kafka-connect-mongodb && \
  git fetch --tags && \
  git checkout tags/v1.2.0 && \
  mvn clean package -DskipTests=true -DskipITs=true && \
  mv target/kafka-connect-mongodb/kafka-connect-mongodb-1.2.0-jar-with-dependencies.jar $KAFKA_CONNECT_MONGODB_DIR && \
  cd .. && rm -rf $KAFKA_CONNECT_MONGODB_DIR/kafka-connect-mongodb
