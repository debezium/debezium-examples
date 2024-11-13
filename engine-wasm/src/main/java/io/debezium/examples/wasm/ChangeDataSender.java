package io.debezium.examples.wasm;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executors;

import com.dylibso.chicory.runtime.ImportMemory;
import com.dylibso.chicory.runtime.ImportValue;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wasm.Parser;

import io.debezium.DebeziumException;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import io.debezium.relational.history.MemorySchemaHistory;

/**
 * Demo for using the Debezium Embedded API to send change events to Amazon Kinesis.
 */
public class ChangeDataSender implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeDataSender.class);

    private static final String APP_NAME = "wasm";

    private final Properties config;
    private final DebeziumEngine<ChangeEvent<String, String>> engine;

    private final ExportFunction processFunction;
    private final ExportFunction allocFunction;
    private final ExportFunction deallocFunction;
    private final Memory memory;

    public ChangeDataSender() {
        config = new Properties();
        config.putAll(System.getProperties());
        config.setProperty("name", APP_NAME);
        config.setProperty("connector.class", "io.debezium.connector.mysql.MySqlConnector");
        config.setProperty("converter.schemas.enable", "false");
        // for demo purposes let's store offsets and history only in memory
        config.setProperty("offset.storage", "org.apache.kafka.connect.storage.MemoryOffsetBackingStore");
        config.setProperty("schema.history.internal", MemorySchemaHistory.class.getName());
        config.setProperty("offset.flush.interval.ms", "60000");
        /* begin connector properties */
        config.setProperty("database.server.id", "85744");
        config.setProperty("topic.prefix", "wasm");
        config.setProperty("include.schema.changes", "false");

        // Create the engine with this configuration ...
        engine = DebeziumEngine.create(Json.class)
            .using(config)
            .notifying(this::sendRecord)
            .build();

        memory = new Memory(new MemoryLimits(2, MemoryLimits.MAX_PAGES));
        final var module = Parser.parse(getClass().getResourceAsStream("/compiled/cdc.wasm"));
        Instance instance = Instance.builder(module)
                .withImportValues(
                        ImportValues.builder()
                                .addMemory(new ImportMemory("env", "memory", memory))
                                .build()
                )
                .build();
        processFunction = instance.export("change");
        allocFunction = instance.export("malloc");
        deallocFunction = instance.export("free");
    }

    @Override
    public void run() {
        final var executor = Executors.newSingleThreadExecutor();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Requesting embedded engine to shut down");
            try {
                engine.close();
                LOGGER.info("Engine terminated");
                executor.shutdown();
            }
            catch (IOException e) {
                throw new DebeziumException(e);
            }
        }));

        executor.execute(engine);
    }

    private void sendRecord(ChangeEvent<String, String> record) {
        LOGGER.debug("Passing change event key = '{}', value = '{}' to WASM module", record.key(), record.value());

        final var destinationLen = record.destination().getBytes().length;
        final var destinationPtr = allocFunction.apply(destinationLen)[0];
        memory.writeString((int) destinationPtr, record.destination());

        final var keyLen = record.key().getBytes().length;
        final var keyPtr = allocFunction.apply(keyLen)[0];
        memory.writeString((int) keyPtr, record.key());

        final var valueLen = record.value().getBytes().length;
        final var valuePtr = allocFunction.apply(valueLen)[0];
        memory.writeString((int) valuePtr, record.value());

        processFunction.apply(destinationPtr, destinationLen, keyPtr, keyLen, valuePtr, valueLen);

        deallocFunction.apply(valuePtr);
        deallocFunction.apply(keyPtr);
        deallocFunction.apply(destinationPtr);
    }

    public static void main(String[] args) {
        new ChangeDataSender().run();
    }
}
