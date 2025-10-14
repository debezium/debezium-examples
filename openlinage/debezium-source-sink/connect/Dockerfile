FROM quay.io/debezium/connect:3.3.0.Final

ENV MAVEN_REPO="https://repo1.maven.org/maven2"
ENV GROUP_ID="io/debezium"
ENV DEBEZIUM_VERSION="3.3.0.Final"
ENV ARTIFACT_ID="debezium-openlineage-core"
ENV CLASSIFIER="-libs"

# This is just a workaround for https://issues.apache.org/jira/browse/KAFKA-19758
RUN rm -rf /kafka/connect/debezium-connector-jdbc && \
    rm -rf /kafka/connect/debezium-connector-cockroachdb && \
    rm -rf /kafka/connect/debezium-connector-db2 && \
    rm -rf /kafka/connect/debezium-connector-ibmi && \
    rm -rf /kafka/connect/debezium-connector-informix && \
    rm -rf /kafka/connect/debezium-connector-mariadb && \
    rm -rf /kafka/connect/debezium-connector-mysql && \
    rm -rf /kafka/connect/debezium-connector-oracle && \
    rm -rf /kafka/connect/debezium-connector-spanner && \
    rm -rf /kafka/connect/debezium-connector-sqlserver && \
    rm -rf /kafka/connect/debezium-connector-vitess


# Add OpenLineage
RUN mkdir -p /tmp/openlineage-libs && \
    curl "$MAVEN_REPO/$GROUP_ID/$ARTIFACT_ID/$DEBEZIUM_VERSION/$ARTIFACT_ID-${DEBEZIUM_VERSION}${CLASSIFIER}.tar.gz" -o /tmp/debezium-openlineage-core-libs.tar.gz && \
    tar -xzvf /tmp/debezium-openlineage-core-libs.tar.gz -C /tmp/openlineage-libs --strip-components=1


RUN cp -r /tmp/openlineage-libs /kafka/external_libs/

RUN for file in /kafka/external_libs/openlineage-libs/*; do \
      ln -s "$file" /kafka/connect/debezium-connector-postgres/; \
      ln -s "$file" /kafka/connect/debezium-connector-mongodb/; \
    done

ADD openlineage.yml /kafka/