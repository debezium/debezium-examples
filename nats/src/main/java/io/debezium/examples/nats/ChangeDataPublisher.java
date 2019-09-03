package io.debezium.examples.nats;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.json.JsonConverter;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.debezium.config.Configuration;
import io.debezium.embedded.EmbeddedEngine;
import io.debezium.util.Clock;
import io.nats.client.Connection;
import io.nats.client.Nats;

/**
 * Demo for using the Debezium Embedded API to send change events to NATS.
 */
public class ChangeDataPublisher implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeDataPublisher.class);

    private static final String APP_NAME = "nats";
    private static final String serverUrl = "nats://localhost:4222";
    private static final String propFileName = "config.properties";
    private static final String subject = "NATSChannel";

    private final Configuration engineConfig;
    private final JsonConverter valueConverter;

    public ChangeDataPublisher() {
        // Read the configurations from the config file
        Properties prop = new Properties();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
        Configuration.Builder builder = Configuration.create();

        try {
            if (inputStream != null) {
                prop.load(inputStream);
            }
            else {
                throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
            }
        }
        catch (IOException ie) {
            ie.printStackTrace();
        }

        prop.forEach((k, v) -> builder.with(k.toString(), v.toString()));

        engineConfig = builder.build();

        valueConverter = new JsonConverter();
        valueConverter.configure(engineConfig.asMap(), false);
    }

    @Override
    public void run() {
        final EmbeddedEngine engine = EmbeddedEngine.create()
                .using(engineConfig)
                .using(Clock.SYSTEM)
                .notifying(this::sendRecord)
                .build();

        // Boot a publisher to send change data
        ExecutorService publisherExecutor = Executors.newSingleThreadExecutor();
        publisherExecutor.execute(engine);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Requesting embedded engine to shut down");
            engine.stop();
        }));


        // the submitted task keeps running, only no more new ones can be added
        publisherExecutor.shutdown();

        awaitTermination(publisherExecutor);

        LOGGER.info("Engine terminated");
    }

    private void awaitTermination(ExecutorService executor) {
        try {
            while (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                LOGGER.info("Waiting another 10 seconds for the embedded engine to complete");
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void sendRecord(SourceRecord record) {
        // We are interested only in data events not schema change events
        if (record.topic().equals(APP_NAME)) {
            return;
        }

        Schema schema;

        if (record.keySchema() == null) {
            LOGGER.error("The keySchema is missing. Something is wrong.");
            return;
        }

        // For deletes, the value node is null
        if (record.valueSchema() != null) {
            schema = SchemaBuilder.struct()
                    .field("key", record.keySchema())
                    .field("value", record.valueSchema())
                    .build();
        }
        else {
            schema = SchemaBuilder.struct()
                    .field("key", record.keySchema())
                    .build();
        }

        Struct message = new Struct(schema);
        message.put("key", record.key());

        if (record.value() != null) {
            message.put("value", record.value());
        }

        final byte[] payload = valueConverter.fromConnectData("dummy", schema, message);

        try {
            // Connect to the default server
            Connection nc = Nats.connect(serverUrl);

            nc.publish(subject, payload);

            // Make sure the message goes through before we close
            nc.flush(Duration.ZERO);
            nc.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        BasicConfigurator.configure();

        new ChangeDataPublisher().run();
    }
}
