FROM debezium/connect:0.8
ENV KAFKA_CONNECT_MONGODB_DIR=$KAFKA_CONNECT_PLUGINS_DIR/kafka-connect-mongodb

USER root
RUN yum -y install git maven && yum clean all

USER kafka

# Deploy MongoDB Sink Connector
RUN mkdir $KAFKA_CONNECT_MONGODB_DIR && cd $KAFKA_CONNECT_MONGODB_DIR && \
  git clone https://github.com/hpgrahsl/kafka-connect-mongodb.git && \
  cd kafka-connect-mongodb && \
  mvn clean package -DskipTests=true -DskipITs=true && \
  mv target/kafka-connect-mongodb-1.1.0-SNAPSHOT-jar-with-dependencies.jar $KAFKA_CONNECT_MONGODB_DIR && \
  cd .. && rm -rf $KAFKA_CONNECT_MONGODB_DIR/kafka-connect-mongodb
