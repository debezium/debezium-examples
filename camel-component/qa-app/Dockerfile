FROM fabric8/java-centos-openjdk8-jdk

COPY target/lib/* /deployments/lib/
COPY target/*-runner.jar /deployments/app.jar

ENTRYPOINT [ "/deployments/run-java.sh" ]
