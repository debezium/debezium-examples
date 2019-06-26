package io.debezium.examples.camel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.TypeConversionException;
import org.apache.camel.TypeConverter;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.main.Main;
import org.apache.camel.main.MainListener;
import org.apache.camel.main.MainListenerSupport;
import org.apache.camel.main.MainSupport;
import org.apache.camel.support.TypeConverterSupport;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.json.JsonConverter;
import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.debezium.config.Configuration;
import io.debezium.connector.mysql.MySqlConnectorConfig;
import io.debezium.embedded.EmbeddedEngine;
import io.debezium.relational.history.MemoryDatabaseHistory;
import io.debezium.util.Clock;

/**
 * Demo for using the Debezium Embedded API to send change events to Apache Camel.
 */
public class ChangeDataSender extends Main {

    private static final String DEBEZIUM_URI = "direct:debezium";
    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeDataSender.class);
    private static final String APP_NAME = "camel";

    private final Configuration config;
    private final JsonConverter valueConverter;

    private EmbeddedEngine engine;
    private ExecutorService executor;

    public ChangeDataSender() {
        config = Configuration.empty().withSystemProperties(Function.identity()).edit()
                .with(EmbeddedEngine.CONNECTOR_CLASS, "io.debezium.connector.mysql.MySqlConnector")
                .with(EmbeddedEngine.ENGINE_NAME, APP_NAME)
                .with(MySqlConnectorConfig.SERVER_NAME,APP_NAME)
                .with(MySqlConnectorConfig.SERVER_ID, 8192)

                // for demo purposes let's store offsets and history only in memory
                .with(EmbeddedEngine.OFFSET_STORAGE, "org.apache.kafka.connect.storage.MemoryOffsetBackingStore")
                .with(MySqlConnectorConfig.DATABASE_HISTORY, MemoryDatabaseHistory.class.getName())

                // Send JSON without schema
                .with("schemas.enable", false)
                .build();

        valueConverter = new JsonConverter();
        valueConverter.configure(config.asMap(), false);
    }

    private void startEngine() {
        engine = EmbeddedEngine.create()
                .using(config)
                .using(this.getClass().getClassLoader())
                .using(Clock.SYSTEM)
                .notifying(ChangeDataSender.this::sendRecord)
                .build();

        executor = Executors.newSingleThreadExecutor();
        executor.execute(engine);
    }

    private void stopEngine() {
        LOGGER.info("Requesting embedded engine to shut down");
        engine.stop();
        try {
            while (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                LOGGER.info("Waiting another 10 seconds for the embedded engine to shut down");
            }
        }
        catch (InterruptedException e) {
            Thread.interrupted();
        }
    }

    private void sendRecord(SourceRecord record) {
        // We are interested only in data events not schema change events
        if (record.topic().equals(APP_NAME)) {
            return;
        }

        if (null == record.keySchema()) {
            LOGGER.error("The keySchema is missing. Something is wrong.");
            return;
        }

        String partitionKey = String.valueOf(record.key() != null ? record.key().hashCode() : -1);

        try {
            getCamelTemplate().sendBodyAndHeader(DEBEZIUM_URI, record.value(), "key", partitionKey);
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private MainListener getCamelListener() {
        return new MainListenerSupport() {

            @Override
            public void afterStart(MainSupport main) {
                startEngine();
            }

            @Override
            public void beforeStop(MainSupport main) {
                stopEngine();
            }

            @Override
            public void configure(CamelContext context) {
                context.getTypeConverterRegistry().addTypeConverter(byte[].class, Struct.class, getConverter());
            }
        };
    }

    private TypeConverter getConverter() {
        return new TypeConverterSupport() {

            @SuppressWarnings("unchecked")
            @Override
            public <T> T convertTo(Class<T> type, Exchange exchange, Object value) throws TypeConversionException {
                final Struct struct = (Struct) value;
                return (T) valueConverter.fromConnectData("dummy", struct.schema(), struct);
            }
        };
    }

    public static void main(String[] args) throws Exception {
        ChangeDataSender main = new ChangeDataSender();
        main.addMainListener(main.getCamelListener());
        main.addRouteBuilder(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from(DEBEZIUM_URI)
                    .convertBodyTo(byte[].class)
                    .log("Arrived ${body}")
                    .choice()
                        .when(body().isNull()).log("Tombstone is ignored")
                        .otherwise().to("file:out")
                     .endChoice();
            }
        });
        main.run(args);
    }
}
