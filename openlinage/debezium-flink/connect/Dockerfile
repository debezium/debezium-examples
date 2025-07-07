FROM quay.io/debezium/connect:3.2.0.Final

ENV MAVEN_REPO="https://repo1.maven.org/maven2"
ENV GROUP_ID="io/debezium"
ENV DEBEZIUM_VERSION="3.2.0.Final"
ENV ARTIFACT_ID="debezium-openlineage-core"
ENV CLASSIFIER="-libs"

# Add OpenLineage
RUN mkdir -p /tmp/openlineage-libs && \
    curl "$MAVEN_REPO/$GROUP_ID/$ARTIFACT_ID/$DEBEZIUM_VERSION/$ARTIFACT_ID-${DEBEZIUM_VERSION}${CLASSIFIER}.tar.gz" -o /tmp/debezium-openlineage-core-libs.tar.gz && \
    tar -xzvf /tmp/debezium-openlineage-core-libs.tar.gz -C /tmp/openlineage-libs --strip-components=1

RUN cp -r /tmp/openlineage-libs/* /kafka/connect/debezium-connector-postgres/
ADD openlineage.yml /kafka/