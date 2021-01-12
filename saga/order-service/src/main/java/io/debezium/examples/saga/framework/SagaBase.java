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
import java.util.List;
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

    private final SagaState state;

    protected SagaBase(SagaState state) {
        this.state = state;
    }

    public final UUID getId() {
        return state.getId();
    }

    public final JsonNode getPayload() {
        try {
            return objectMapper.readTree(state.getPayload());
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public final String getType() {
        return getClass().getAnnotation(Saga.class).type();
    }

    public final List<String> getStepIds() {
        return Arrays.asList(getClass().getAnnotation(Saga.class).stepIds());
    }

    public SagaStatus getStatus() {
        EntityManager em = JpaOperations.getEntityManager(SagaState.class);
        return em.find(SagaState.class, getId()).getStatus();
    }

    protected void onStepEvent(String type, SagaStepStatus status) {
        TypeReference<HashMap<String, SagaStepStatus>> typeRef = new TypeReference<>() {};
        try {
            HashMap<String, SagaStepStatus> stepStates = objectMapper.readValue(state.getStepState(), typeRef);
            stepStates.put(type, status);
            state.setStepState(objectMapper.writeValueAsString(stepStates));

            if (status == SagaStepStatus.SUCCEEDED) {
                advance();
            }
            else if (status == SagaStepStatus.FAILED) {
                compensate();
            }
            else if (status == SagaStepStatus.ABORTED) {
                goBack();
            }

            state.setStatus(getSagaStatus(objectMapper.readValue(state.getStepState(), typeRef).values()));
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
        if (containsOnly(stepStates, SagaStepStatus.STARTED, SagaStepStatus.SUCCEEDED)) {
            return SagaStatus.STARTED;
        }
        else if (containsOnly(stepStates, SagaStepStatus.SUCCEEDED)) {
            return SagaStatus.COMPLETED;
        }
        else if (containsOnly(stepStates, SagaStepStatus.FAILED, SagaStepStatus.ABORTED)) {
            return SagaStatus.ABORTED;
        }
        else {
            return SagaStatus.ABORTING;
        }
    }

    private boolean containsOnly(Collection<SagaStepStatus> stepStates, SagaStepStatus status) {
        for (SagaStepStatus sagaStepStatus : stepStates) {
            if (sagaStepStatus != status) {
                return false;
            }
        }

        return true;
    }

    private boolean containsOnly(Collection<SagaStepStatus> stepStates, SagaStepStatus status1, SagaStepStatus status2) {
        for (SagaStepStatus sagaStepStatus : stepStates) {
            if (sagaStepStatus != status1 && sagaStepStatus != status2) {
                return false;
            }
        }

        return true;
    }

    protected final void advance() throws JsonProcessingException {
        String nextStep = getNextStep();

        if (nextStep == null) {
            return;
        }

        SagaStepMessage stepEvent = getStepMessage(nextStep);
        persistStepMessage(stepEvent);

        TypeReference<HashMap<String, SagaStepStatus>> typeRef = new TypeReference<>() {};
        HashMap<String, SagaStepStatus> stepStates = objectMapper.readValue(state.getStepState(), typeRef);
        stepStates.put(nextStep, SagaStepStatus.STARTED);

        state.setCurrentStep(nextStep);
        state.setStepState(objectMapper.writeValueAsString(stepStates));
    }

    private void compensate() throws JsonProcessingException {
        SagaStepMessage stepEvent = getCompensatingStepMessage(getCurrentStep());
        persistStepMessage(stepEvent);

        TypeReference<HashMap<String, SagaStepStatus>> typeRef = new TypeReference<>() {};
        HashMap<String, SagaStepStatus> stepStates = objectMapper.readValue(state.getStepState(), typeRef);
        stepStates.put(getCurrentStep(), SagaStepStatus.ABORTING);

        state.setStepState(objectMapper.writeValueAsString(stepStates));
    }

    protected final void goBack() throws JsonProcessingException {
        String previousStep = getPreviousStep();

        if (previousStep == null) {
            return;
        }

        SagaStepMessage stepEvent = getCompensatingStepMessage(previousStep);
        persistStepMessage(stepEvent);

        TypeReference<HashMap<String, SagaStepStatus>> typeRef = new TypeReference<>() {};
        HashMap<String, SagaStepStatus> stepStates = objectMapper.readValue(state.getStepState(), typeRef);
        stepStates.put(previousStep, SagaStepStatus.ABORTING);

        state.setCurrentStep(previousStep);
        state.setStepState(objectMapper.writeValueAsString(stepStates));
    }

    private void persistStepMessage(SagaStepMessage stepEvent) throws JsonProcessingException {
        SagaStepMessageState stepState = new SagaStepMessageState();
        stepState.setId(UUID.randomUUID());
        stepState.setSagaId(getId());
        stepState.setType(stepEvent.type);
        stepState.setPayload(objectMapper.writeValueAsString(stepEvent.payload));

        EntityManager em = JpaOperations.getEntityManager(SagaState.class);
        em.persist(stepState);
    }

    protected String getCurrentStep() {
        return state.getCurrentStep();
    }

    private String getNextStep() {
        if (getCurrentStep() == null) {
            return getStepIds().get(0);
        }

        int idx = getStepIds().indexOf(getCurrentStep());

        if (idx == getStepIds().size() - 1) {
            return null;
        }

        return getStepIds().get(idx + 1);
    }

    private String getPreviousStep() {
        int idx = getStepIds().indexOf(getCurrentStep());

        if (idx == 0) {
            return null;
        }

        return getStepIds().get(idx - 1);
    }
}
