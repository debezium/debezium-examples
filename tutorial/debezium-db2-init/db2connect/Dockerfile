ARG DEBEZIUM_VERSION
FROM debezium/connect:$DEBEZIUM_VERSION

USER root
RUN yum -y install libaio curl && yum clean all

USER kafka

# Deploy db2 client and drivers
RUN curl https://repo1.maven.org/maven2/com/ibm/db2/jcc/11.5.0.0/jcc-11.5.0.0.jar --output /kafka/connect/debezium-connector-db2/jcc-11.5.0.0.jar
