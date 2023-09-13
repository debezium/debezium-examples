FROM flink:1.17-scala_2.12-java11
RUN curl -sfSL https://dlcdn.apache.org/flink/flink-ml-2.3.0/apache-flink-ml-2.3.0.tar.gz -o apache-flink-ml-2.3.0.tar.gz && \
  tar -zvxf apache-flink-ml-2.3.0.tar.gz && \
  cp apache-flink-ml-2.3.0/deps/lib/* /opt/flink/lib && \
  rm -rf apache-flink-ml-2.3.0 && \
  rm -f apache-flink-ml-2.3.0.tar.gz
COPY target/debezium-flink-0.1.jar /opt/flink
