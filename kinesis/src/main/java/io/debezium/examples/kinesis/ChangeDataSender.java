package io.debezium.examples.kinesis;

import java.nio.ByteBuffer;
import java.util.function.Function;

import org.apache.kafka.connect.json.JsonConverter;
import org.apache.kafka.connect.source.SourceRecord;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.model.PutRecordRequest;

import io.debezium.config.Configuration;
import io.debezium.connector.mysql.MySqlConnectorConfig;
import io.debezium.embedded.EmbeddedEngine;
import io.debezium.relational.history.MemoryDatabaseHistory;
import io.debezium.util.Clock;

public class ChangeDataSender implements Runnable {
    private static final String APP_NAME = "kinesis";
    private static final String KINESIS_REGION_CONF_NAME = "kinesis.region";

    private final Configuration config;
    private final JsonConverter valueConverter;
    private final JsonConverter keyConverter;
    private final AmazonKinesis kinesisClient;

    public ChangeDataSender() {
        config = Configuration.empty().withSystemProperties(Function.identity()).edit()
                .with(EmbeddedEngine.CONNECTOR_CLASS, "io.debezium.connector.mysql.MySqlConnector")
                .with(EmbeddedEngine.ENGINE_NAME, APP_NAME)
                .with(MySqlConnectorConfig.SERVER_NAME,APP_NAME)
                .with(MySqlConnectorConfig.SERVER_ID, 8192)

                // for demo purpose let' store offsets and history only in memory
                .with(EmbeddedEngine.OFFSET_STORAGE, "org.apache.kafka.connect.storage.MemoryOffsetBackingStore")
                .with(MySqlConnectorConfig.DATABASE_HISTORY, MemoryDatabaseHistory.class.getName())

                // Send JSON without schema
                .with("schemas.enable", false)
                .build();

        keyConverter = new JsonConverter();
        keyConverter.configure(config.asMap(), true);
        valueConverter = new JsonConverter();
        valueConverter.configure(config.asMap(), false);

        final String regionName = config.getString(KINESIS_REGION_CONF_NAME);

        final AWSCredentialsProvider credentialsProvider = new ProfileCredentialsProvider("default");

        kinesisClient = AmazonKinesisClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(regionName)
                .build();
    }

    public void run() {
        final EmbeddedEngine engine = EmbeddedEngine.create()
                .using(config)
                .using(this.getClass().getClassLoader())
                .using(Clock.SYSTEM)
                .notifying(this::sendRecord)
                .build();
        final Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            engine.stop();
            try {
                mainThread.join();
            }
            catch (InterruptedException e) {
            }
        }));
        engine.run();
    }

    private void sendRecord(SourceRecord record) {
        // We are interested only in data events not schema change events
        if (record.valueSchema().name().endsWith(".Envelope")) {
            final byte[] payload = valueConverter.fromConnectData("dummy", record.valueSchema(), record.value());
            final byte[] key = keyConverter.fromConnectData("dummy", record.keySchema(), record.key());

            PutRecordRequest putRecord = new PutRecordRequest();
            putRecord.setStreamName(streamNameMapper(record.topic()));
            putRecord.setPartitionKey(new String(key));
            putRecord.setData(ByteBuffer.wrap(payload));
            kinesisClient.putRecord(putRecord);
        }
    }

    private String streamNameMapper(String topic) {
        return topic;
    }

    public static void main(String[] args) {
        new ChangeDataSender().run();
    }

}
