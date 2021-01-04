/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.example.saga.framework;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import javax.persistence.EntityManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.debezium.example.saga.framework.internal.SagaState;
import io.debezium.example.saga.framework.internal.SagaStepMessageState;
import io.debezium.example.saga.framework.internal.SagaStepStatus;
import io.quarkus.hibernate.orm.panache.runtime.JpaOperations;

public abstract class SagaBase implements Saga {

    private static ObjectMapper objectMapper = new ObjectMapper();

    protected void updateStepStatus(String type, SagaStepStatus status) {
        EntityManager em = JpaOperations.getEntityManager(SagaState.class);
        SagaState state = em.find(SagaState.class, getId());

        TypeReference<HashMap<String, SagaStepStatus>> typeRef = new TypeReference<>() {};
        try {
            HashMap<String, SagaStepStatus> stepStates = objectMapper.readValue(state.getStepState(), typeRef);

            if (status == SagaStepStatus.FAILED) {
                for (Entry<String, SagaStepStatus> oneState : stepStates.entrySet()) {
                    if (!oneState.getKey().equals(type)) {
                        if (oneState.getValue() == SagaStepStatus.STARTED || oneState.getValue() == SagaStepStatus.SUCCEEDED) {
                            SagaStepMessage compensation = getCompensatingStepMessage(oneState.getKey());

                            SagaStepMessageState compensationStepState = new SagaStepMessageState();
                            compensationStepState.sagaId = state.getId();
                            compensationStepState.type = compensation.type;
                            compensationStepState.payload = compensation.payload;
                            oneState.setValue(SagaStepStatus.ABORTING);
                            em.persist(compensationStepState);
                        }
                    }
                }
            }

            stepStates.put(type, status);
            state.setStepState(objectMapper.writeValueAsString(stepStates));
            state.setStatus(getSagaStatus(stepStates.values()));
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SagaStatus getStatus() {
        EntityManager em = JpaOperations.getEntityManager(SagaState.class);
        return em.find(SagaState.class, getId()).getStatus();
    }

    private SagaStatus getSagaStatus(Collection<SagaStepStatus> stepStates) {
        stepStates = new HashSet<>(stepStates);

        if (stepStates.size() == 1 && stepStates.contains(SagaStepStatus.SUCCEEDED)) {
            return SagaStatus.COMPLETED;
        }
        else if (stepStates.size() == 2 && stepStates.contains(SagaStepStatus.FAILED) && stepStates.contains(SagaStepStatus.ABORTED)) {
            return SagaStatus.ABORTED;
        }
        else if (stepStates.contains(SagaStepStatus.ABORTING)) {
            return SagaStatus.ABORTING;
        }
        else {
            return SagaStatus.STARTED;
        }
    }
}
