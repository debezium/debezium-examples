/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.saga.framework;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.debezium.examples.saga.framework.internal.ConsumedMessage;
import io.debezium.examples.saga.framework.internal.SagaState;
import io.debezium.examples.saga.framework.internal.SagaStepMessageState;
import io.debezium.examples.saga.framework.internal.SagaStepStatus;
import io.quarkus.hibernate.orm.panache.runtime.JpaOperations;

public abstract class SagaBase {

    private static final Logger LOG = LoggerFactory.getLogger(SagaBase.class);

    private static ObjectMapper objectMapper = new ObjectMapper();

    private final UUID id;
    private final JsonNode payload;

    protected SagaBase(UUID id, JsonNode payload) {
        this.id = id;
        this.payload = payload;
    }

    public final UUID getId() {
        return id;
    }

    public final JsonNode getPayload() {
        return payload;
    }

    public final String getType() {
        return getClass().getAnnotation(Saga.class).type();
    }

    public final Set<String> getStepIds() {
        return new HashSet<>(Arrays.asList(getClass().getAnnotation(Saga.class).stepIds()));
    }

    public SagaStatus getStatus() {
        EntityManager em = JpaOperations.getEntityManager(SagaState.class);
        return em.find(SagaState.class, getId()).getStatus();
    }

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
                            compensationStepState.setId(UUID.randomUUID());
                            compensationStepState.setSagaId(state.getId());
                            compensationStepState.setType(compensation.type);
                            compensationStepState.setPayload(objectMapper.writeValueAsString(compensation.payload));
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

    protected abstract SagaStepMessage getStepMessage(String id);

    protected abstract SagaStepMessage getCompensatingStepMessage(String id);

    protected void processed(UUID eventId) {
        EntityManager em = JpaOperations.getEntityManager(ConsumedMessage.class);
        em.persist(new ConsumedMessage(eventId, Instant.now()));
    }

    protected boolean alreadyProcessed(UUID eventId) {
        LOG.debug("Looking for event with id {} in message log", eventId);
        EntityManager em = JpaOperations.getEntityManager(ConsumedMessage.class);
        return em.find(ConsumedMessage.class, eventId) != null;
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
