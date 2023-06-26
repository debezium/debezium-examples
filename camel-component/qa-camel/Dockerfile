FROM registry.access.redhat.com/ubi8/openjdk-17-runtime

COPY target/dependency/* /deployments/
COPY target/*.jar /deployments/app.jar
COPY target/docker-extra/run-java/run-java.sh /deployments

ENTRYPOINT [ "/deployments/run-java.sh" ]
