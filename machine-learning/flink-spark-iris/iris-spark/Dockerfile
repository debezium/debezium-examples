FROM apache/spark:3.3.3
COPY target/debezium-spark-1.0-SNAPSHOT-jar-with-dependencies.jar /opt/spark/work-dir
CMD ["/opt/spark/bin/spark-submit", "debezium-spark-1.0-SNAPSHOT-jar-with-dependencies.jar"]