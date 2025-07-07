FROM flink:2.0.0-java21
COPY target/flink-order-processor-1.0-SNAPSHOT.jar /opt/flink
COPY openlineage.yml /opt/flink/conf
COPY target/lib/* /opt/flink/lib
