/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.cacheinvalidation.persistence;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;

import jakarta.persistence.PersistenceUnit;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.ConfigValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.debezium.config.Configuration;
import io.debezium.data.Envelope.Operation;
import io.debezium.embedded.Connect;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.RecordChangeEvent;
import io.debezium.engine.format.ChangeEventFormat;
import io.debezium.examples.cacheinvalidation.model.Item;

/**
 * Listens to database changes using Debezium's embedded engine. If a change
 * event for an {@link Item} arrives that has not been caused by this
 * application itself, that {@code Item} will be removed from the JPA 2nd-level
 * cache.
 *
 * @author Gunnar Morling
 */
@ApplicationScoped
public class DatabaseChangeEventListener {

    private static final String CONFIG_PREFIX = "quarkus.debezium-cdc.";
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseChangeEventListener.class);

    @PersistenceUnit
    private EntityManagerFactory emf;

    @PersistenceContext
    private EntityManager em;

    @Inject
    private KnownTransactions knownTransactions;

    private DebeziumEngine<?> engine;
    private ExecutorService executorService;

    @Priority(2)
    public void startEmbeddedEngine(@Observes @Initialized(ApplicationScoped.class) Object init) {
        LOG.info("Launching Debezium embedded engine");

        final Properties properties = new Properties();
        for (String propertyName : ConfigProvider.getConfig().getPropertyNames()) {
            if (propertyName.startsWith(CONFIG_PREFIX)) {
                final String key = propertyName.replace(CONFIG_PREFIX, "");
                final ConfigValue value = ConfigProvider.getConfig().getConfigValue(propertyName);
                properties.put(key, value.getRawValue());
                LOG.info("\t{}: {}", key, value.getRawValue());
            }
        }

        final Configuration config = Configuration.empty()
                .withSystemProperties(Function.identity())
                .edit()
                .with(Configuration.from(properties))
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

        executorService = Executors.newFixedThreadPool(1);
        executorService.execute(engine);
    }

    @PreDestroy
    public void shutdownEngine() throws Exception {
        LOG.info("Stopping Debezium embedded engine");
        engine.close();
        executorService.shutdown();
    }

    private void handleDbChangeEvent(SourceRecord record) {
        LOG.info("Handling DB change event " + record);

        if (record.topic().equals("dbserver1.public.item")) {
            Long itemId = ((Struct) record.key()).getInt64("id");
            Struct payload = (Struct) record.value();
            Operation op = Operation.forCode(payload.getString("op"));
            Long txId = ((Struct) payload.get("source")).getInt64("txId");

            if (knownTransactions.isKnown(txId)) {
                LOG.info("Not evicting item {} from 2nd-level cache as TX {} was started by this application", itemId, txId);
            }
            else if (op != Operation.UPDATE && op != Operation.DELETE) {
                LOG.info("Not evicting item {} from 2nd-level cache as the change is neither an UPDATE nor a DELETE", itemId);
            }
            else {
                LOG.info("Evicting item {} from 2nd-level cache as TX {} was not started by this application", itemId, txId);
                emf.getCache().evict(Item.class, itemId);
            }
        }
    }
}
