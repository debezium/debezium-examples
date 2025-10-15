ARG DEBEZIUM_VERSION
FROM quay.io/debezium/server:${DEBEZIUM_VERSION}

ENV MAVEN_REPO="https://repo1.maven.org/maven2"
ENV GROUP_ID="io/debezium"
ENV DEBEZIUM_VERSION=${DEBEZIUM_VERSION}
ENV ARTIFACT_ID="debezium-openlineage-core"
ENV CLASSIFIER="-libs"

USER root
RUN microdnf -y install gzip jq && \
    microdnf clean all

RUN mkdir -p /debezium/logs && \
    chown -R jboss:jboss /debezium/logs && \
    chmod 755 /debezium/logs


USER jboss

# Add OpenLineage
RUN mkdir -p /tmp/openlineage-libs && \
    curl "$MAVEN_REPO/$GROUP_ID/$ARTIFACT_ID/$DEBEZIUM_VERSION/$ARTIFACT_ID-${DEBEZIUM_VERSION}${CLASSIFIER}.tar.gz" -o /tmp/debezium-openlineage-core-libs.tar.gz && \
    tar -xzvf /tmp/debezium-openlineage-core-libs.tar.gz -C /tmp/openlineage-libs --strip-components=1

RUN cp -r /tmp/openlineage-libs/* /debezium/lib