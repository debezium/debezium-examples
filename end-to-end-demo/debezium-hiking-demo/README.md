# Simple CRUD Demo

A simple CRUD application using Java EE (JPA, JAX-RS, CDI etc.), based on top of WildFly and MySQL.
Used as an example for streaming changes out of a database using Debezium.

To run the app, follow these steps:

    mvn clean package
    docker build --no-cache -t debezium-examples/hike-manager:latest -f Dockerfile .

    docker run -it --rm --name mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=debezium -e MYSQL_USER=mysqluser -e MYSQL_PASSWORD=mysqlpw debezium/example-mysql:0.7

    docker run -it --rm -p 8080:8080 --link mysql debezium-examples/hike-manager:latest

Then visit the application in a browser at http://localhost:8080/hibernate-ogm-hiking-demo-1.0-SNAPSHOT/hikes.html.
