FROM debezium/connect:0.8
ENV KAFKA_CONNECT_MONGODB_DIR=$KAFKA_CONNECT_PLUGINS_DIR/kafka-connect-mongodb

USER root
RUN yum -y install git maven curl && yum clean all

RUN cd /kafka/libs && \
  curl -s https://oss.sonatype.org/content/repositories/releases/org/mongodb/mongodb-driver/3.8.2/mongodb-driver-3.8.2.jar --remote-name && \
  curl -s https://oss.sonatype.org/content/repositories/releases/org/mongodb/mongodb-driver-core/3.8.2/mongodb-driver-core-3.8.2.jar --remote-name && \
  curl -s https://oss.sonatype.org/content/repositories/releases/org/mongodb/bson/3.8.2/bson-3.8.2.jar --remote-name

USER kafka

# Deploy MongoDB Sink Connector
RUN mkdir -p $KAFKA_CONNECT_MONGODB_DIR && cd $KAFKA_CONNECT_MONGODB_DIR && \
  git clone https://github.com/hpgrahsl/kafka-connect-mongodb.git && \
  cd kafka-connect-mongodb && \
  mvn clean package -DskipTests=true -DskipITs=true && \
  mv target/kafka-connect-mongodb-1.2.0.jar $KAFKA_CONNECT_MONGODB_DIR && \
  cd .. && rm -rf $KAFKA_CONNECT_MONGODB_DIR/kafka-connect-mongodb
