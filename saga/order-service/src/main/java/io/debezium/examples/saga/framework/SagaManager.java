/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.saga.framework;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.debezium.examples.saga.framework.internal.SagaState;

@ApplicationScoped
public class SagaManager {

    private static ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    private EntityManager entityManager;

    public <S extends SagaBase> S begin(Class<S> sagaType, JsonNode payload) {
        try {
            UUID sagaId = UUID.randomUUID();

            Map<String, String> stepStates = new HashMap<>();

            SagaState state = new SagaState();
            state.setId(sagaId);
            state.setType(sagaType.getAnnotation(Saga.class).type());
            state.setPayload(objectMapper.writeValueAsString(payload));
            state.setStatus(SagaStatus.STARTED);
            state.setStepState(objectMapper.writeValueAsString(stepStates));
            entityManager.persist(state);


            S saga = sagaType.getConstructor(SagaState.class).newInstance(state);
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
            return sagaType.getConstructor(SagaState.class).newInstance(state);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
