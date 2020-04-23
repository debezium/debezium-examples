FROM debezium/connect:1.1

RUN curl https://repo1.maven.org/maven2/net/logstash/log4j/jsonevent-layout/1.7/jsonevent-layout-1.7.jar > /kafka/libs/jsonevent-layout.jar &&\
    curl https://repo1.maven.org/maven2/commons-lang/commons-lang/2.6/commons-lang-2.6.jar > /kafka/libs/commons-lang.jar &&\
    curl https://repo1.maven.org/maven2/net/minidev/json-smart/2.3/json-smart-2.3.jar > /kafka/libs/json-smart.jar &&\
    curl https://repo1.maven.org/maven2/net/minidev/accessors-smart/1.2/accessors-smart-1.2.jar > /kafka/libs/accessors-smart.jar &&\
    curl https://repo1.maven.org/maven2/org/ow2/asm/asm/8.0.1/asm-8.0.1.jar > /kafka/libs/asm.jar

COPY log4j.properties /kafka/config/log4j.properties
