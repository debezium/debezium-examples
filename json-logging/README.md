# JSON Logging for Debezium

Many production deployments use a log aggregator to gather logs from all systems in the environment and make them available for extended analysis and monitoring.
Such tools are usually able to work with text-based logs and convert them into structured events.
They can have a problem with multi-line log messages that are typical for Java applications providing stacktraces in the log.

In this case, it might be better to log messages as structured JSON messages containing all the log message information.
This example uses [Logstash json_event pattern for log4j](https://github.com/logstash/log4j-jsonevent-layout).
The Debezium `connect` image is enriched with the required JAR files for that purpose.

## Demonstration

Start the components and register Debezium to stream changes from the database:

```
$ export DEBEZIUM_VERSION=1.8
$ docker-compose up --build
$ curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" http://localhost:8083/connectors/ -d @register-mysql.json
```

Log messages for `connect` container looks like:

```json
{"@timestamp":"2020-04-23T16:30:02.080Z","source_host":"ef2f9638f931","file":"SnapshotReader.java","method":"enqueueSchemaChanges","level":"INFO","line_number":"1040","thread_name":"debezium-mysqlconnector-dbserver1-snapshot","@version":1,"logger_name":"io.debezium.connector.mysql.SnapshotReader","message":"\tCREATE TABLE `products` (\n  `id` int(11) NOT NULL AUTO_INCREMENT,\n  `name` varchar(255) NOT NULL,\n  `description` varchar(512) DEFAULT NULL,\n  `weight` float DEFAULT NULL,\n  PRIMARY KEY (`id`)\n) ENGINE=InnoDB AUTO_INCREMENT=110 DEFAULT CHARSET=latin1","class":"io.debezium.connector.mysql.SnapshotReader","mdc":{"dbz.connectorContext":"snapshot","dbz.connectorType":"MySQL","connector.context":"[inventory-connector|task-0] ","dbz.connectorName":"dbserver1"}}
```

The formatted version of this message is:

```json
{
  "@timestamp": "2020-04-23T16:30:02.080Z",
  "source_host": "ef2f9638f931",
  "file": "SnapshotReader.java",
  "method": "enqueueSchemaChanges",
  "level": "INFO",
  "line_number": "1040",
  "thread_name": "debezium-mysqlconnector-dbserver1-snapshot",
  "@version": 1,
  "logger_name": "io.debezium.connector.mysql.SnapshotReader",
  "message": "\tCREATE TABLE `products` (\n  `id` int(11) NOT NULL AUTO_INCREMENT,\n  `name` varchar(255) NOT NULL,\n  `description` varchar(512) DEFAULT NULL,\n  `weight` float DEFAULT NULL,\n  PRIMARY KEY (`id`)\n) ENGINE=InnoDB AUTO_INCREMENT=110 DEFAULT CHARSET=latin1",
  "class": "io.debezium.connector.mysql.SnapshotReader",
  "mdc": {
    "dbz.connectorContext": "snapshot",
    "dbz.connectorType": "MySQL",
    "connector.context": "[inventory-connector|task-0] ",
    "dbz.connectorName": "dbserver1"
  }
}
```

When we try to generate a stacktrace using:

```
$ curl http://localhost:8083/connectors/error
```

then we get a log line:

```json
{"exception":{"stacktrace":"javax.ws.rs.NotFoundException: HTTP 404 Not Found\n\tat org.glassfish.jersey.server.ServerRuntime$1.run(ServerRuntime.java:250)\n\tat org.glassfish.jersey.internal.Errors$1.call(Errors.java:248)\n\tat org.glassfish.jersey.internal.Errors$1.call(Errors.java:244)\n\tat org.glassfish.jersey.internal.Errors.process(Errors.java:292)\n\tat org.glassfish.jersey.internal.Errors.process(Errors.java:274)\n\tat org.glassfish.jersey.internal.Errors.process(Errors.java:244)\n\tat org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:265)\n\tat org.glassfish.jersey.server.ServerRuntime.process(ServerRuntime.java:232)\n\tat org.glassfish.jersey.server.ApplicationHandler.handle(ApplicationHandler.java:679)\n\tat org.glassfish.jersey.servlet.WebComponent.serviceImpl(WebComponent.java:392)\n\tat org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:346)\n\tat org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:365)\n\tat org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:318)\n\tat org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:205)\n\tat org.eclipse.jetty.servlet.ServletHolder.handle(ServletHolder.java:852)\n\tat org.eclipse.jetty.servlet.ServletHandler.doHandle(ServletHandler.java:544)\n\tat org.eclipse.jetty.server.handler.ScopedHandler.nextHandle(ScopedHandler.java:233)\n\tat org.eclipse.jetty.server.session.SessionHandler.doHandle(SessionHandler.java:1581)\n\tat org.eclipse.jetty.server.handler.ScopedHandler.nextHandle(ScopedHandler.java:233)\n\tat org.eclipse.jetty.server.handler.ContextHandler.doHandle(ContextHandler.java:1307)\n\tat org.eclipse.jetty.server.handler.ScopedHandler.nextScope(ScopedHandler.java:188)\n\tat org.eclipse.jetty.servlet.ServletHandler.doScope(ServletHandler.java:482)\n\tat org.eclipse.jetty.server.session.SessionHandler.doScope(SessionHandler.java:1549)\n\tat org.eclipse.jetty.server.handler.ScopedHandler.nextScope(ScopedHandler.java:186)\n\tat org.eclipse.jetty.server.handler.ContextHandler.doScope(ContextHandler.java:1204)\n\tat org.eclipse.jetty.server.handler.ScopedHandler.handle(ScopedHandler.java:141)\n\tat org.eclipse.jetty.server.handler.ContextHandlerCollection.handle(ContextHandlerCollection.java:221)\n\tat org.eclipse.jetty.server.handler.StatisticsHandler.handle(StatisticsHandler.java:173)\n\tat org.eclipse.jetty.server.handler.HandlerWrapper.handle(HandlerWrapper.java:127)\n\tat org.eclipse.jetty.server.Server.handle(Server.java:494)\n\tat org.eclipse.jetty.server.HttpChannel.handle(HttpChannel.java:374)\n\tat org.eclipse.jetty.server.HttpConnection.onFillable(HttpConnection.java:268)\n\tat org.eclipse.jetty.io.AbstractConnection$ReadCallback.succeeded(AbstractConnection.java:311)\n\tat org.eclipse.jetty.io.FillInterest.fillable(FillInterest.java:103)\n\tat org.eclipse.jetty.io.ChannelEndPoint$2.run(ChannelEndPoint.java:117)\n\tat org.eclipse.jetty.util.thread.strategy.EatWhatYouKill.runTask(EatWhatYouKill.java:336)\n\tat org.eclipse.jetty.util.thread.strategy.EatWhatYouKill.doProduce(EatWhatYouKill.java:313)\n\tat org.eclipse.jetty.util.thread.strategy.EatWhatYouKill.tryProduce(EatWhatYouKill.java:171)\n\tat org.eclipse.jetty.util.thread.strategy.EatWhatYouKill.produce(EatWhatYouKill.java:135)\n\tat org.eclipse.jetty.util.thread.QueuedThreadPool.runJob(QueuedThreadPool.java:782)\n\tat org.eclipse.jetty.util.thread.QueuedThreadPool$Runner.run(QueuedThreadPool.java:918)\n\tat java.base\/java.lang.Thread.run(Thread.java:834)","exception_class":"javax.ws.rs.NotFoundException","exception_message":"HTTP 404 Not Found"},"source_host":"ef2f9638f931","method":"toResponse","level":"ERROR","message":"Uncaught exception in REST call to \/error","mdc":{},"@timestamp":"2020-04-23T16:34:08.936Z","file":"ConnectExceptionMapper.java","line_number":"61","thread_name":"qtp70788844-57","@version":1,"logger_name":"org.apache.kafka.connect.runtime.rest.errors.ConnectExceptionMapper","class":"org.apache.kafka.connect.runtime.rest.errors.ConnectExceptionMapper"}
```

which after formatting looks like:

```
{
  "exception": {
    "stacktrace": "javax.ws.rs.NotFoundException: HTTP 404 Not Found\n\tat org.glassfish.jersey.server.ServerRuntime$1.run(ServerRuntime.java:250)\n\tat org.glassfish.jersey.internal.Errors$1.call(Errors.java:248)\n\tat org.glassfish.jersey.internal.Errors$1.call(Errors.java:244)\n\tat org.glassfish.jersey.internal.Errors.process(Errors.java:292)\n\tat org.glassfish.jersey.internal.Errors.process(Errors.java:274)\n\tat org.glassfish.jersey.internal.Errors.process(Errors.java:244)\n\tat org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:265)\n\tat org.glassfish.jersey.server.ServerRuntime.process(ServerRuntime.java:232)\n\tat org.glassfish.jersey.server.ApplicationHandler.handle(ApplicationHandler.java:679)\n\tat org.glassfish.jersey.servlet.WebComponent.serviceImpl(WebComponent.java:392)\n\tat org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:346)\n\tat org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:365)\n\tat org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:318)\n\tat org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:205)\n\tat org.eclipse.jetty.servlet.ServletHolder.handle(ServletHolder.java:852)\n\tat org.eclipse.jetty.servlet.ServletHandler.doHandle(ServletHandler.java:544)\n\tat org.eclipse.jetty.server.handler.ScopedHandler.nextHandle(ScopedHandler.java:233)\n\tat org.eclipse.jetty.server.session.SessionHandler.doHandle(SessionHandler.java:1581)\n\tat org.eclipse.jetty.server.handler.ScopedHandler.nextHandle(ScopedHandler.java:233)\n\tat org.eclipse.jetty.server.handler.ContextHandler.doHandle(ContextHandler.java:1307)\n\tat org.eclipse.jetty.server.handler.ScopedHandler.nextScope(ScopedHandler.java:188)\n\tat org.eclipse.jetty.servlet.ServletHandler.doScope(ServletHandler.java:482)\n\tat org.eclipse.jetty.server.session.SessionHandler.doScope(SessionHandler.java:1549)\n\tat org.eclipse.jetty.server.handler.ScopedHandler.nextScope(ScopedHandler.java:186)\n\tat org.eclipse.jetty.server.handler.ContextHandler.doScope(ContextHandler.java:1204)\n\tat org.eclipse.jetty.server.handler.ScopedHandler.handle(ScopedHandler.java:141)\n\tat org.eclipse.jetty.server.handler.ContextHandlerCollection.handle(ContextHandlerCollection.java:221)\n\tat org.eclipse.jetty.server.handler.StatisticsHandler.handle(StatisticsHandler.java:173)\n\tat org.eclipse.jetty.server.handler.HandlerWrapper.handle(HandlerWrapper.java:127)\n\tat org.eclipse.jetty.server.Server.handle(Server.java:494)\n\tat org.eclipse.jetty.server.HttpChannel.handle(HttpChannel.java:374)\n\tat org.eclipse.jetty.server.HttpConnection.onFillable(HttpConnection.java:268)\n\tat org.eclipse.jetty.io.AbstractConnection$ReadCallback.succeeded(AbstractConnection.java:311)\n\tat org.eclipse.jetty.io.FillInterest.fillable(FillInterest.java:103)\n\tat org.eclipse.jetty.io.ChannelEndPoint$2.run(ChannelEndPoint.java:117)\n\tat org.eclipse.jetty.util.thread.strategy.EatWhatYouKill.runTask(EatWhatYouKill.java:336)\n\tat org.eclipse.jetty.util.thread.strategy.EatWhatYouKill.doProduce(EatWhatYouKill.java:313)\n\tat org.eclipse.jetty.util.thread.strategy.EatWhatYouKill.tryProduce(EatWhatYouKill.java:171)\n\tat org.eclipse.jetty.util.thread.strategy.EatWhatYouKill.produce(EatWhatYouKill.java:135)\n\tat org.eclipse.jetty.util.thread.QueuedThreadPool.runJob(QueuedThreadPool.java:782)\n\tat org.eclipse.jetty.util.thread.QueuedThreadPool$Runner.run(QueuedThreadPool.java:918)\n\tat java.base/java.lang.Thread.run(Thread.java:834)",
    "exception_class": "javax.ws.rs.NotFoundException",
    "exception_message": "HTTP 404 Not Found"
  },
  "source_host": "ef2f9638f931",
  "method": "toResponse",
  "level": "ERROR",
  "message": "Uncaught exception in REST call to /error",
  "mdc": {},
  "@timestamp": "2020-04-23T16:34:08.936Z",
  "file": "ConnectExceptionMapper.java",
  "line_number": "61",
  "thread_name": "qtp70788844-57",
  "@version": 1,
  "logger_name": "org.apache.kafka.connect.runtime.rest.errors.ConnectExceptionMapper",
  "class": "org.apache.kafka.connect.runtime.rest.errors.ConnectExceptionMapper"
}
```

Even a complex multiline log message is thus formatted as a single event.

Stop the demo:

```
$ docker-compose down
```
