FROM fabric8/java-centos-openjdk11-jdk

COPY target/dependency/* /deployments/
COPY target/*.jar /deployments/app.jar

ENTRYPOINT [ "/deployments/run-java.sh" ]
