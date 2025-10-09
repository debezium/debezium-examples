FROM quay.io/debezium/connect:3.3.0.Final

ENV MAVEN_REPO="https://repo1.maven.org/maven2"
ENV GROUP_ID="io/debezium"
ENV DEBEZIUM_VERSION="3.3.0.Final"
ENV ARTIFACT_ID="debezium-openlineage-core"
ENV CLASSIFIER="-libs"

RUN rm -rf /kafka/connect/debezium-connector-jdbc
RUN rm -rf /kafka/connect/debezium-connector-cockroachdb
RUN rm -rf /kafka/connect/debezium-connector-db2
RUN rm -rf /kafka/connect/debezium-connector-ibmi
RUN rm -rf /kafka/connect/debezium-connector-informix
RUN rm -rf /kafka/connect/debezium-connector-mariadb
RUN rm -rf /kafka/connect/debezium-connector-mysql
RUN rm -rf /kafka/connect/debezium-connector-oracle
RUN rm -rf /kafka/connect/debezium-connector-spanner
RUN rm -rf /kafka/connect/debezium-connector-sqlserver
RUN rm -rf /kafka/connect/debezium-connector-vitess


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