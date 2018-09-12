/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.aggregation.hibernate;

import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;

import com.example.domain.Customer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;

import io.debezium.aggregation.connect.KafkaConnectSchemaFactoryWrapper;
import io.debezium.aggregation.connect.KafkaConnectSchemaFactoryWrapper.KafkaConnectSchemaAdapterFactory;
import io.debezium.aggregation.hibernate.Aggregate.AggregateKey;

/**
 * Hibernate event listener that materializes aggregates for given entities via JSON into a separate table.
 *
 * @author Gunnar Morling
 */
class AggregationBuilderListener implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

        private final ObjectMapper mapper;

        public AggregationBuilderListener() {
            mapper = new ObjectMapperFactory().buildObjectMapper();
        }

        @Override
        public boolean requiresPostCommitHanding(EntityPersister persister) {
            return false;
        }

        @Override
        public void onPostInsert(PostInsertEvent event) {
            String aggregateName = getAggregateName((event.getPersister().getMappedClass()));
            if (aggregateName != null) {
                insertOrUpdateAggregate(
                        event.getEntity(),
                        event.getId(),
                        event.getPersister().getIdentifierPropertyName(),
                        aggregateName,
                        event.getSession()
                );
            }
        }

        @Override
        public void onPostUpdate(PostUpdateEvent event) {
            String aggregateName = getAggregateName((event.getPersister().getMappedClass()));
            if (aggregateName != null) {
                insertOrUpdateAggregate(
                        event.getEntity(),
                        event.getId(),
                        event.getPersister().getIdentifierPropertyName(),
                        aggregateName,
                        event.getSession()
                );
            }
        }

        @Override
        public void onPostDelete(PostDeleteEvent event) {
            String aggregateName = getAggregateName((event.getPersister().getMappedClass()));
            if (aggregateName != null) {
                deleteAggregate(
                        event.getEntity(),
                        event.getId(),
                        event.getPersister().getIdentifierPropertyName(),
                        aggregateName,
                        event.getSession()
                );
            }
        }

        private String getAggregateName(Class<?> entityType) {
            MaterializeAggregate materializeAggregate = entityType.getAnnotation(MaterializeAggregate.class);
            return materializeAggregate != null ? materializeAggregate.aggregateName() : null;
        }

        private void insertOrUpdateAggregate(Object entity, Object id, String idProperty, String aggregateName, EventSource session) {
            session.getActionQueue().registerProcess( new BeforeTransactionCompletionProcess() {

                @Override
                public void doBeforeTransactionCompletion(SessionImplementor session) {
                    if ( !session.getTransactionCoordinator().isActive() ) {
                        return;
                    }

                    try {
                        KafkaConnectSchemaFactoryWrapper visitor = new KafkaConnectSchemaFactoryWrapper();
                        mapper.acceptJsonFormatVisitor(Customer.class, visitor);
                        String materialization = mapper.writeValueAsString(entity);
                        JsonSchema valueSchema = visitor.finalSchema();

                        String idString = "{ \"" + idProperty + "\" : " + mapper.writeValueAsString(id) + " }";
                        ObjectSchema keySchema = new KafkaConnectSchemaAdapterFactory().objectSchema();
                        keySchema.putProperty(idProperty, valueSchema.asObjectSchema().getProperties().get(idProperty));
                        keySchema.setId(aggregateName + ".Key");
                        keySchema.setRequired(true);

                        Aggregate aggregate = new Aggregate();
                        aggregate.pk = new AggregateKey(idString, aggregateName);
                        aggregate.keySchema = mapper.writeValueAsString(keySchema);
                        aggregate.materialization = materialization;
                        aggregate.valueSchema = mapper.writeValueAsString(valueSchema);

//                        Session temporarySession = null;
//                        try {
//                            temporarySession = session.sessionWithOptions()
//                                    .connection()
//                                    .autoClose( false )
//                                    .connectionHandlingMode( PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION )
//                                    .openSession();
//                            temporarySession.saveOrUpdate(aggregate);
//                            temporarySession.flush();
//                        }
//                        finally {
//                            if ( temporarySession != null ) {
//                                temporarySession.close();
//                            }
//                        }

                        session.saveOrUpdate(aggregate);
                        session.flush();
                    }
                    catch(RuntimeException e) {
                        throw e;
                    }
                    catch(Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            } );
        }

        private void deleteAggregate(Object entity, Object id, String idProperty, String aggregateName, EventSource session) {
            session.getActionQueue().registerProcess( new BeforeTransactionCompletionProcess() {

                @Override
                public void doBeforeTransactionCompletion(SessionImplementor session) {
                    if ( !session.getTransactionCoordinator().isActive() ) {
                        return;
                    }

                    try {
//                        Session temporarySession = null;
//                        try {
//                            temporarySession = session.sessionWithOptions()
//                                    .connection()
//                                    .autoClose( false )
//                                    .connectionHandlingMode( PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION )
//                                    .openSession();
//                            temporarySession.saveOrUpdate(aggregate);
//                            temporarySession.flush();
//                        }
//                        finally {
//                            if ( temporarySession != null ) {
//                                temporarySession.close();
//                            }
//                        }

                        String idString = "{ \"" + idProperty + "\" : " + mapper.writeValueAsString(id) + " }";
                        Aggregate aggregate = session.getReference(Aggregate.class, new AggregateKey(idString, aggregateName));
                        session.delete(aggregate);
                        session.flush();
                    }
                    catch(RuntimeException e) {
                        throw e;
                    }
                    catch(Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            } );
        }
    }
