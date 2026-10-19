# 1. Prepare JDK
FROM eclipse-temurin:21-jdk AS jdk

# Run this to get list of needed modules;
# Not running it as part of the Dockerfile as it needs the app JAR as input, so it'd have to be executed each time
# jdeps --ignore-missing-deps --multi-release 21 --print-module-deps poc-ddd-aggregates/target/poc-ddd-aggregates-0.1-SNAPSHOT.jar poc-ddd-aggregates/target/dependencies/*.jar

RUN jlink \
    --add-modules java.base,java.desktop,java.management,java.security.jgss,java.security.sasl,java.sql,jdk.unsupported \
    --verbose --strip-debug --compress zip-6 --no-header-files --no-man-pages \
    --output /opt/jre-minimal

# 2. Build
# The build context is the repository root so that the shared parent POM is available;
# Dockerfile.dockerignore limits the context to the files this build needs.
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /tmp/build
COPY pom.xml .
COPY kstreams/poc-ddd-aggregates/pom.xml kstreams/poc-ddd-aggregates/

# Download all Maven dependencies; unless the POM changes, this step will be cached for future builds
RUN mvn dependency:go-offline -f kstreams/poc-ddd-aggregates/pom.xml

RUN mvn dependency:copy-dependencies -f kstreams/poc-ddd-aggregates/pom.xml -DoutputDirectory=target/dependencies -DincludeScope=runtime
COPY kstreams/poc-ddd-aggregates/src kstreams/poc-ddd-aggregates/src
RUN mvn package -DskipTests -o -f kstreams/poc-ddd-aggregates/pom.xml

# 3. Create actual image (jlink-ed JDK, dependencies, JAR and launcher)
FROM registry.fedoraproject.org/fedora-minimal:44

COPY --from=jdk /opt/jre-minimal /opt/poc-ddd-aggregates/jdk
RUN cd /opt/poc-ddd-aggregates \
    && curl -sO https://raw.githubusercontent.com/fabric8io-images/run-java-sh/master/fish-pepper/run-java-sh/fp-files/run-java.sh \
    && chmod u+x run-java.sh
COPY --from=build /tmp/build/kstreams/poc-ddd-aggregates/target/dependencies/* /opt/poc-ddd-aggregates/lib/
COPY kstreams/poc-ddd-aggregates/run-aggregator.sh /opt/poc-ddd-aggregates
COPY --from=build /tmp/build/kstreams/poc-ddd-aggregates/target/poc-ddd-aggregates-0.1-SNAPSHOT.jar /opt/poc-ddd-aggregates/lib/poc-ddd-aggregates-0.1-SNAPSHOT.jar

# ENTRYPOINT /opt/poc-ddd-aggregates/run-aggregator.sh
