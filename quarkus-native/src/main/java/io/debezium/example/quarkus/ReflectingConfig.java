/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.example.quarkus;

import io.debezium.connector.postgresql.PostgresConnector;
import io.debezium.connector.postgresql.PostgresConnectorTask;
import io.debezium.connector.postgresql.PostgresSourceInfoStructMaker;
import io.debezium.connector.postgresql.snapshot.lock.NoSnapshotLock;
import io.debezium.connector.postgresql.snapshot.lock.SharedSnapshotLock;
import io.debezium.connector.postgresql.snapshot.query.SelectAllSnapshotQuery;
import io.debezium.embedded.ConvertingEngineBuilderFactory;
import io.debezium.embedded.async.ConvertingAsyncEngineBuilderFactory;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.spi.OffsetCommitPolicy;
import io.debezium.pipeline.notification.channels.LogNotificationChannel;
import io.debezium.pipeline.notification.channels.SinkNotificationChannel;
import io.debezium.pipeline.notification.channels.jmx.JmxNotificationChannel;
import io.debezium.pipeline.signal.actions.StandardActionProvider;
import io.debezium.pipeline.signal.channels.FileSignalChannel;
import io.debezium.pipeline.signal.channels.KafkaSignalChannel;
import io.debezium.pipeline.signal.channels.SourceSignalChannel;
import io.debezium.pipeline.signal.channels.jmx.JmxSignalChannel;
import io.debezium.pipeline.signal.channels.process.InProcessSignalChannel;
import io.debezium.pipeline.txmetadata.DefaultTransactionMetadataFactory;
import io.debezium.schema.SchemaTopicNamingStrategy;
import io.debezium.snapshot.lock.NoLockingSupport;
import io.debezium.snapshot.mode.*;
import io.debezium.snapshot.spi.SnapshotLock;
import io.debezium.transforms.ExtractNewRecordState;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.kafka.common.security.authenticator.SaslClientAuthenticator;
import org.apache.kafka.connect.json.JsonConverter;
import org.apache.kafka.connect.transforms.predicates.Predicate;
import org.apache.kafka.connect.transforms.predicates.TopicNameMatches;

@RegisterForReflection(targets = {
        DebeziumEngine.BuilderFactory.class,
        ConvertingEngineBuilderFactory.class,
        ConvertingAsyncEngineBuilderFactory.class,
        SaslClientAuthenticator.class,
        JsonConverter.class,
        PostgresConnector.class,
        PostgresSourceInfoStructMaker.class,
        DefaultTransactionMetadataFactory.class,
        SchemaTopicNamingStrategy.class,
        OffsetCommitPolicy.class,
        PostgresConnectorTask.class,
        SinkNotificationChannel.class,
        LogNotificationChannel.class,
        JmxNotificationChannel.class,
        SnapshotLock.class,
        NoLockingSupport.class,
        NoSnapshotLock.class,
        SharedSnapshotLock.class,
        SelectAllSnapshotQuery.class,
        AlwaysSnapshotter.class,
        InitialSnapshotter.class,
        InitialOnlySnapshotter.class,
        NoDataSnapshotter.class,
        RecoverySnapshotter.class,
        WhenNeededSnapshotter.class,
        NeverSnapshotter.class,
        SchemaOnlySnapshotter.class,
        SchemaOnlyRecoverySnapshotter.class,
        ConfigurationBasedSnapshotter.class,
        SourceSignalChannel.class,
        KafkaSignalChannel.class,
        FileSignalChannel.class,
        JmxSignalChannel.class,
        InProcessSignalChannel.class,
        StandardActionProvider.class,
        Predicate.class,
        TopicNameMatches.class,
        ExtractNewRecordState.class
})
public class ReflectingConfig { }
