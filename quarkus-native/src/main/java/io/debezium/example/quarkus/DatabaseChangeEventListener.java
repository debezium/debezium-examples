/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.example.quarkus;

import io.debezium.config.Configuration;
import io.debezium.data.Envelope.Operation;
import io.debezium.embedded.Connect;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.RecordChangeEvent;
import io.debezium.engine.format.ChangeEventFormat;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;


@ApplicationScoped
@Startup
public class DatabaseChangeEventListener {

    private static final String CONFIG_PREFIX = "quarkus.debezium-cdc.";
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseChangeEventListener.class);

    private DebeziumEngine<?> engine;
    @Inject
    private ExecutorService executor;

    @Inject
    private DebeziumConfiguration debeziumConfiguration;

    @PostConstruct
    public void startEmbeddedEngine() {
        LOG.info("Launching Debezium embedded engine");

        final Configuration config = Configuration.empty()
                .withSystemProperties(Function.identity())
                .edit()
                .with(Configuration.from(debeziumConfiguration.configuration()))
                .build();

        this.engine = DebeziumEngine.create(ChangeEventFormat.of(Connect.class))
                .using(config.asProperties())
                .notifying((list, recordCommitter) -> {
                    for (RecordChangeEvent<SourceRecord> record : list) {
                        handleDbChangeEvent(record.record());
                        recordCommitter.markProcessed(record);
                    }
                    recordCommitter.markBatchFinished();
                })
                .build();

        executor.execute(engine);
    }

    @PreDestroy
    public void shutdownEngine() throws Exception {
        LOG.info("Stopping Debezium embedded engine");
        engine.close();
        executor.shutdown();
    }

    private void handleDbChangeEvent(SourceRecord record) {
        LOG.info("f DB change event {}", record);

        if (record.topic().equals("dbserver1.public.products")) {
            Long itemId = ((Struct) record.key()).getInt64("id");
            Struct payload = (Struct) record.value();
            Operation op = Operation.forCode(payload.getString("op"));
            Long txId = ((Struct) payload.get("source")).getInt64("txId");

            LOG.info("received event with itemId: {} op: {} txId: {}", itemId, op, txId);
        }
    }
}
