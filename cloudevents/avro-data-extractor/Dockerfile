FROM fabric8/java-centos-openjdk11-jdk
ENV JAVA_OPTIONS='-Dquarkus.http.host=0.0.0.0 -Dquarkus.http.port=${PORT}'
COPY target/quarkus-app/lib/ /deployments/lib/
COPY target/quarkus-app/*.jar /deployments/
COPY target/quarkus-app/app/ /deployments/app/
COPY target/quarkus-app/quarkus/ /deployments/quarkus/
ENTRYPOINT [ "/deployments/run-java.sh" ]
