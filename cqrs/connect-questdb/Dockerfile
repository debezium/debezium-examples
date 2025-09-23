ARG DEBEZIUM_VERSION
FROM quay.io/debezium/connect:$DEBEZIUM_VERSION

USER kafka
RUN curl -L -o /kafka/connect/kafka-questdb-connector-0.16-bin.zip https://github.com/questdb/kafka-questdb-connector/releases/download/v0.16/kafka-questdb-connector-0.16-bin.zip  && cd /kafka/connect && unzip kafka-questdb-connector-0.16-bin.zip
