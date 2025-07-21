ARG DEBEZIUM_VERSION
FROM quay.io/debezium/connect:nightly

ENV GROOVY_VERSION=4.0.20

USER root
RUN microdnf -y install which curl java-17-openjdk-devel && microdnf clean all

USER kafka

# Deploy database init script
RUN curl https://groovy.jfrog.io/artifactory/dist-release-local/groovy-zips/apache-groovy-binary-$GROOVY_VERSION.zip > groovy.zip &&\
    unzip groovy.zip &&\
    rm groovy.zip &&\
    mv groovy-$GROOVY_VERSION groovy

COPY initdb.groovy inventory.sql .
