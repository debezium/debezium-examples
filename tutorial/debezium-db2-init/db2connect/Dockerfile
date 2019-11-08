ARG DEBEZIUM_VERSION
FROM debezium/connect:$DEBEZIUM_VERSION
ENV KAFKA_CONNECT_JDBC_DIR=$KAFKA_CONNECT_PLUGINS_DIR/kafka-connect-jdbc
ENV INSTANT_CLIENT_DIR=/instant_client/

USER root
RUN yum -y install libaio curl && yum clean all

USER kafka
# Deploy db2 client and drivers

run curl https://repo1.maven.org/maven2/com/ibm/db2/jcc/11.5.0.0/jcc-11.5.0.0.jar --output /kafka/libs/jcc-11.5.0.0.jar
