package io.debezium.examples.kinesis;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import io.debezium.embedded.Connect;
import io.debezium.embedded.async.AsyncEmbeddedEngine;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.RecordChangeEvent;
import io.debezium.engine.format.ChangeEventFormat;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.json.JsonConverter;
import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.model.PutRecordRequest;

import io.debezium.config.Configuration;
import io.debezium.connector.mysql.MySqlConnectorConfig;
import io.debezium.relational.history.MemorySchemaHistory;

/**
 * Demo for using the Debezium Embedded API to send change events to Amazon Kinesis.
 */
public class ChangeDataSender implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeDataSender.class);

    private static final String APP_NAME = "kinesis";
    private static final String KINESIS_REGION_CONF_NAME = "kinesis.region";

    private final Configuration config;
    private final JsonConverter valueConverter;
    private final AmazonKinesis kinesisClient;
    private DebeziumEngine engine;

    public ChangeDataSender() {
        config = Configuration.empty().withSystemProperties(Function.identity()).edit()
                .with(AsyncEmbeddedEngine.CONNECTOR_CLASS, "io.debezium.connector.mysql.MySqlConnector")
                .with(AsyncEmbeddedEngine.ENGINE_NAME, APP_NAME)
                .with(MySqlConnectorConfig.TOPIC_PREFIX,APP_NAME)
                .with(MySqlConnectorConfig.SERVER_ID, 8192)

                // for demo purposes let's store offsets and history only in memory
                .with(AsyncEmbeddedEngine.OFFSET_STORAGE, "org.apache.kafka.connect.storage.MemoryOffsetBackingStore")
                .with(MySqlConnectorConfig.SCHEMA_HISTORY, MemorySchemaHistory.class.getName())

                // Send JSON without schema
                .with("schemas.enable", false)
                .build();

        valueConverter = new JsonConverter();
        valueConverter.configure(config.asMap(), false);

        final String regionName = config.getString(KINESIS_REGION_CONF_NAME);

        final AWSCredentialsProvider credentialsProvider = new ProfileCredentialsProvider("default");

        kinesisClient = AmazonKinesisClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(regionName)
                .build();
    }

    @Override
    public void run() {
        engine = DebeziumEngine.create(ChangeEventFormat.of(Connect.class))
                .using(config.asProperties())
                .using(this.getClass().getClassLoader())
                .using(Clock.systemUTC())
                .notifying(this::sendRecord)
                .build();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(engine);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Requesting embedded engine to shut down");
            try {
                engine.close();
            }
            catch (IOException e) {
                LOGGER.error("Error terminating embedded engine", e);
                throw new RuntimeException(e);
            }
        }));

        // the submitted task keeps running, only no more new ones can be added
        executor.shutdown();

        awaitTermination(executor);

        cleanUp();

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

    private void cleanUp() {
        kinesisClient.shutdown();
    }

    private void sendRecord(RecordChangeEvent<SourceRecord> record) {
        // We are interested only in data events not schema change events
        if (record.record().topic().equals(APP_NAME)) {
            return;
        }

        Schema schema = null;

        if ( null == record.record().keySchema() ) {
            LOGGER.error("The keySchema is missing. Something is wrong.");
            return;
        }

        // For deletes, the value node is null
        if ( null != record.record().valueSchema() ) {
            schema = SchemaBuilder.struct()
                    .field("key", record.record().keySchema())
                    .field("value", record.record().valueSchema())
                    .build();
        }
        else {
            schema = SchemaBuilder.struct()
                    .field("key", record.record().keySchema())
                    .build();
        }

        Struct message = new Struct(schema);
        message.put("key", record.record().key());

        if ( null != record.record().value() )
            message.put("value", record.record().value());

        String partitionKey = String.valueOf(record.record().key() != null ? record.record().key().hashCode() : -1);
        final byte[] payload = valueConverter.fromConnectData("dummy", schema, message);

        PutRecordRequest putRecord = new PutRecordRequest();

        putRecord.setStreamName(streamNameMapper(record.record().topic()));
        putRecord.setPartitionKey(partitionKey);
        putRecord.setData(ByteBuffer.wrap(payload));

        kinesisClient.putRecord(putRecord);
    }

    private String streamNameMapper(String topic) {
        return topic;
    }

    public static void main(String[] args) {
        new ChangeDataSender().run();
    }
}
