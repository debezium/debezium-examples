FROM apache/spark:4.2.0-scala2.13-java21-ubuntu
COPY target/debezium-spark-1.0-SNAPSHOT-jar-with-dependencies.jar /opt/spark/work-dir
CMD ["/opt/spark/bin/spark-submit", "debezium-spark-1.0-SNAPSHOT-jar-with-dependencies.jar"]