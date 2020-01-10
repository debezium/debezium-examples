FROM fabric8/java-jboss-openjdk8-jdk
ENV JAVA_OPTIONS='-Dquarkus.http.host=0.0.0.0 -Dquarkus.http.port=${PORT}'
COPY target/lib/* /deployments/lib/
COPY target/*-runner.jar /deployments/app.jar
ENTRYPOINT [ "/deployments/run-java.sh" ]
