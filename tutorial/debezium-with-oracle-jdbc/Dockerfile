ARG DEBEZIUM_VERSION
FROM quay.io/debezium/connect:$DEBEZIUM_VERSION
ENV KAFKA_CONNECT_JDBC_DIR=$KAFKA_CONNECT_PLUGINS_DIR/kafka-connect-jdbc

# These define where we will automatically fetch the Oracle Instant Client from
# This uses version 21.1.0.0, and all these should remain in-sync version wise with one another.
ENV INSTANT_CLIENT_RPM_BASE_URL=https://yum.oracle.com/repo/OracleLinux/OL8/oracle/instantclient21/x86_64/getPackage/
ENV INSTANT_CLIENT_RPM_NAME=oracle-instantclient-basic-21.1.0.0.0-1.x86_64.rpm
ENV INSTANT_CLIENT_LIB=/usr/lib/oracle/21/client64/lib

USER root

# Install wget and use it to pull down the RPM from Oracle's repository
RUN microdnf -y install wget
RUN wget --quiet $INSTANT_CLIENT_RPM_BASE_URL$INSTANT_CLIENT_RPM_NAME && \
  rpm -i $INSTANT_CLIENT_RPM_NAME && \
  rm $INSTANT_CLIENT_RPM_NAME

RUN microdnf -y install libaio && microdnf clean all

# Copies the xstreams.jar and ojdbc8.jar from the installed instant client to /kafka/libs
RUN cp $INSTANT_CLIENT_LIB/xstreams.jar /kafka/libs && \
    cp $INSTANT_CLIENT_LIB/ojdbc8.jar /kafka/libs && \
    chown kafka:kafka /kafka/libs/xstreams.jar && \
    chown kafka:kafka /kafka/libs/ojdbc8.jar

USER kafka
