FROM eclipse-temurin:21-jre

ENV PORT=8079
ENV JAVA_OPTIONS="-Dquarkus.http.host=0.0.0.0"

COPY target/quarkus-app/lib/ /deployments/lib/
COPY target/quarkus-app/*.jar /deployments/
COPY target/quarkus-app/app/ /deployments/app/
COPY target/quarkus-app/quarkus/ /deployments/quarkus/

EXPOSE 8079

ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTIONS -Dquarkus.http.port=${PORT} -jar /deployments/quarkus-run.jar" ]
