FROM debezium/connect:0.9

RUN curl https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.3.1/jmx_prometheus_javaagent-0.3.1.jar > /kafka/jmx_prometheus_javaagent.jar
COPY config.yml /kafka/config.yml
