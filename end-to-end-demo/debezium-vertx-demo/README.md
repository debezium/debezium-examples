# Vert.x Debezium event client

This is a simple application that listens on Kafka topic for events that are created by a Debezium connector.
The events are read, partially transformed and then emitted as WebSocket messages.

## Configuration
The application can be configured either via system properties or environment variables or using `conf/config.json` configuration file as defined by [Vert.x Config](https://vertx.io/docs/vertx-config/java/).

| Property            | Environment variable | Description                                               | Default value                   |
| ------------------- | -------------------- | --------------------------------------------------------- | --------------------------------|
| `bootstrap.servers` | `BOOTSTRAP_SERVERS`  | An initial list of Kafka servers to consumes message from | `kafka:9092`                    |
| `demo.topic`        | `DEMO_TOPIC`         | The name of the topic                                     | `dbserver1_inventory_Hike_json` |
| `demo.port`         | `DEMO PORT`          | The port on which HTTP server is started                  | `5000`                          |

## How to run the application
There are multiple ways how to start the consumer

### Vert.x plugin
This should be used if you are modifying the demo application.
Vert.x has a support of monitoring and hot-redeployment of the source code so whenever you make any change it is immediately reflected in the application.
```
mvn vertx:run
```

### Plain Java application
The result of the build process is a fat JAR that can be started as an oridnary Java application.
```
mvn clean package
java -jar target/debezium-vertx-demo-fat.jar
```

### Docker container
You can also deploy the application as Docker container.
To create the image just use the provided `Dockerfile`.
```
docker build -t debezium/demo-vertx .
docker run -it debezi/demo-vertx
```

Then visit the application in a browser at `http://<host>:<demo.port>/`.
The web page will create a WebSocket connection to the application and whenever a new message arrives to the topic it is automatically added to the page.
