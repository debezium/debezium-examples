/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.cacheinvalidation.persistence;

import java.util.function.Function;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.storage.MemoryOffsetBackingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.debezium.config.Configuration;
import io.debezium.connector.postgresql.PostgresConnector;
import io.debezium.connector.postgresql.PostgresConnectorConfig;
import io.debezium.connector.postgresql.PostgresConnectorConfig.SnapshotMode;
import io.debezium.embedded.EmbeddedEngine;
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

    private static final Logger LOG = LoggerFactory.getLogger( DatabaseChangeEventListener.class );

    @Resource
    private ManagedExecutorService executorService;

    @PersistenceUnit
    private EntityManagerFactory emf;

    @PersistenceContext
    private EntityManager em;

    private EmbeddedEngine engine;

    @Inject
    private KnownTransactions knownTransactions;

    public void startEmbeddedEngine(@Observes @Initialized(ApplicationScoped.class) Object init) {
        LOG.info("Launching Debezium embedded engine");

        Configuration config = Configuration.empty()
                .withSystemProperties(Function.identity()).edit()
                .with(EmbeddedEngine.CONNECTOR_CLASS, PostgresConnector.class)
                .with(EmbeddedEngine.ENGINE_NAME, "cache-invalidation-engine")
                .with(EmbeddedEngine.OFFSET_STORAGE, MemoryOffsetBackingStore.class)
                .with("name", "cache-invalidation-connector")
                .with("database.hostname", "postgres")
                .with("database.port", 5432)
                .with("database.user", "postgresuser")
                .with("database.password", "postgrespw")
                .with("database.server.name", "dbserver1")
                .with("database.dbname", "inventory")
                .with("database.whitelist", "public")
                .with(PostgresConnectorConfig.SNAPSHOT_MODE, SnapshotMode.NEVER)
                .build();

        this.engine = EmbeddedEngine.create()
                .using(config)
                .notifying(this::handleDbChangeEvent)
                .build();

        executorService.execute(engine);
    }

    @PreDestroy
    public void shutdownEngine() {
        LOG.info("Stopping Debezium embedded engine");
        engine.stop();
    }

    private void handleDbChangeEvent(SourceRecord record) {
        LOG.info("Handling DB change event " + record);

        if (record.topic().equals("dbserver1.public.item")) {
            Long txId = ((Struct) ((Struct) record.value()).get("source")).getInt64("txId");
            Long itemId = ((Struct) record.key()).getInt64("id");

            if (knownTransactions.isKnown(txId)) {
                LOG.info("Not evicting item {} from 2nd-level cache as TX {} was started by this application", itemId, txId);
            }
            else {
                LOG.info("Evicting item {} from 2nd-level cache as TX {} was not started by this application", itemId, txId);
                emf.getCache().evict(Item.class, itemId);
            }
        }
    }
}
