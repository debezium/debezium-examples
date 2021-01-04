/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.example.saga.framework;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.debezium.example.saga.framework.internal.SagaState;
import io.debezium.example.saga.framework.internal.SagaStepMessageState;
import io.debezium.example.saga.framework.internal.SagaStepStatus;

@ApplicationScoped
public class SagaManager {

    private static ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    private EntityManager entityManager;

    public void begin(Saga saga) {
        SagaState state = new SagaState();
        state.setId(UUID.randomUUID());
        state.setType(saga.getType());
        state.setPayload(saga.getPayload());
        state.setStatus(SagaStatus.STARTED);

        Map<String, String> stepStates = new HashMap<>();

        entityManager.persist(state);

        for (String stepId : saga.stepIds()) {
            SagaStepMessage stepEvent = saga.getStepMessage(stepId);

            SagaStepMessageState stepState = new SagaStepMessageState();
            stepState.sagaId = state.getId();
            stepState.type = stepEvent.type;
            stepState.payload = stepEvent.payload;
            stepStates.put(stepId, SagaStepStatus.STARTED.name());
            entityManager.persist(stepState);
        }

        try {
            state.setStepState(objectMapper.writeValueAsString(stepStates));
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public <S extends Saga> S find(Class<S> sagaType, UUID sagaId) {
        SagaState state = entityManager.find(SagaState.class, sagaId);

        try {
            return sagaType.getConstructor(UUID.class, String.class).newInstance(state.getId(), state.getPayload());

        }
        catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }
}
