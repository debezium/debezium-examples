/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.saga.framework;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import io.debezium.examples.saga.framework.internal.SagaState;
import io.debezium.outbox.quarkus.ExportedEvent;

@ApplicationScoped
public class SagaManager {

    @Inject
    private Event<ExportedEvent<?, ?>> event;

    @Inject
    private EntityManager entityManager;

    public <S extends SagaBase> S begin(Class<S> sagaType, JsonNode payload) {
        try {
            UUID sagaId = UUID.randomUUID();

            SagaState state = new SagaState();
            state.setId(sagaId);
            state.setType(sagaType.getAnnotation(Saga.class).type());
            state.setPayload(payload);
            state.setSagaStatus(SagaStatus.STARTED);
            state.setStepStatus(JsonNodeFactory.instance.objectNode());
            entityManager.persist(state);


            S saga = sagaType.getConstructor(Event.class, SagaState.class).newInstance(event, state);
            saga.advance();
            return saga;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <S extends SagaBase> S find(Class<S> sagaType, UUID sagaId) {
        SagaState state = entityManager.find(SagaState.class, sagaId);

        if (state == null) {
            return null;
        }

        try {
            return sagaType.getConstructor(Event.class, SagaState.class).newInstance(event, state);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
