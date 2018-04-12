/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.example.endtoend.vertx;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.ServerWebSocket;
import io.vertx.reactivex.core.net.SocketAddress;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumer;

public class Verticle extends AbstractVerticle {

    private static final String CONF_BOOTSTRAP_SERVERS = "bootstrap.servers";
    private static final String CONF_TOPIC = "demo.topic";
    private static final String CONF_PORT = "demo.port";

    private static final Logger LOG = LoggerFactory.getLogger(Verticle.class);

    @Override
    public void start() throws Exception {
        final Map<SocketAddress, ServerWebSocket> sockets = new ConcurrentHashMap<>();

        config().put(CONF_BOOTSTRAP_SERVERS, "kafka:9092");
        config().put(CONF_TOPIC, "dbserver1_inventory_Hike_json");
        config().put(CONF_PORT, 5000);

        final ConfigRetriever retriever = ConfigRetriever.create(vertx);
        final Buffer indexPage = vertx.fileSystem().readFileBlocking("html/index.html");

        retriever.rxGetConfig().subscribe(
            config -> {
                vertx.createHttpServer()
                    .requestHandler(request -> {
                        if ("/".equals(request.path())) {
                            request.response()
                                .setChunked(true)
                                .write(indexPage)
                                .end();
                        }
                        else {
                            request.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
                        }
                    })
                    .websocketHandler(socket -> {
                        if ("/changes".equals(socket.path())) {
                            LOG.info("New connection from {}", socket.remoteAddress());
                            sockets.put(socket.remoteAddress(), socket);
                            socket.endHandler(closeHandler -> {
                                LOG.info("Connection from {} closed", socket.remoteAddress());
                                sockets.remove(socket.remoteAddress());
                            });
                            socket.accept();
                        }
                        else {
                            socket.reject();
                        }
                    }).rxListen(config.getInteger(resolveConfName(config, CONF_PORT))).subscribe();

                final Map<String, String> kafkaConfig = new HashMap<>();
                kafkaConfig.put(CONF_BOOTSTRAP_SERVERS, config.getString(resolveConfName(config, CONF_BOOTSTRAP_SERVERS)));
                kafkaConfig.put("key.deserializer", io.vertx.kafka.client.serialization.JsonObjectDeserializer.class.getName());
                kafkaConfig.put("value.deserializer", io.vertx.kafka.client.serialization.JsonObjectDeserializer.class.getName());
                kafkaConfig.put("group.id", "vertx-client");
                kafkaConfig.put("auto.offset.reset", "earliest");
                kafkaConfig.put("enable.auto.commit", "false");

                // use consumer for interacting with Apache Kafka
                KafkaConsumer<JsonObject, JsonObject> consumer = KafkaConsumer.create(vertx, kafkaConfig);
                consumer.subscribe(config.getString(resolveConfName(config, CONF_TOPIC))).toFlowable().subscribe(record -> {
                    final String message = message(record.value());
                    sockets.forEach((address, socket) -> {
                        LOG.debug("Writing message to a client {}", socket.remoteAddress());;
                        socket.write(Buffer.buffer(message));
                    });
                   LOG.info(message);
                });
            },
            t -> {
                throw new IllegalStateException("Could not initialize configration", t);
            }
        );
    }

    private String resolveConfName(JsonObject config, String name) {
        final String envVar = name.toUpperCase().replace('.', '_');
        return config.containsKey(envVar) ? envVar : name;
    }

    private String message(JsonObject message) {
        final JsonObject payload = message != null ? message.getJsonObject("payload") : new JsonObject();
        return String.format("Change: before = %s, after = %s",
                payload.getJsonObject("before"),
                payload.getJsonObject("after"));
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new Verticle());
    }
}
