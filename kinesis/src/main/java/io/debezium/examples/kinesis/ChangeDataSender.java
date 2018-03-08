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
    private static final String KINESIS_STREAM_CONF_NAME = "kinesis.stream";
    private static final String KINESIS_REGION_CONF_NAME = "kinesis.region";

    private final Configuration config;
    private final JsonConverter converter;
    private final AmazonKinesis kinesisClient;
    private final String streamName;

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

        converter = new JsonConverter();
        converter.configure(config.asMap(), false);

        streamName = config.getString(KINESIS_STREAM_CONF_NAME);
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
                .using(this.getClass().getClassLoader())
                .using(Clock.SYSTEM)
                .notifying(this::sendRecord)
                .build();
        engine.run();
    }

    private void sendRecord(SourceRecord record) {
        // We are interested only in data events not schema change events
        if (record.valueSchema().name().endsWith(".Envelope")) {
            final byte[] payload = converter.fromConnectData("dummy", record.valueSchema(), record.value());

            PutRecordRequest putRecord = new PutRecordRequest();
            putRecord.setStreamName(streamName);
            putRecord.setPartitionKey(APP_NAME); // We want events to keep total ordering
            putRecord.setData(ByteBuffer.wrap(payload));
            kinesisClient.putRecord(putRecord);
        }
    }

    public static void main(String[] args) {
        new ChangeDataSender().run();
    }

}
