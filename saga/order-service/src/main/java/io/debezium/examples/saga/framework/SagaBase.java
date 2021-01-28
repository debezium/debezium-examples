/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.saga.framework;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.enterprise.event.Event;
import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.debezium.examples.saga.framework.internal.ConsumedMessage;
import io.debezium.examples.saga.framework.internal.SagaState;
import io.debezium.examples.saga.framework.internal.SagaStepStatus;
import io.debezium.examples.saga.order.saga.SagaEvent;
import io.debezium.outbox.quarkus.ExportedEvent;
import io.quarkus.hibernate.orm.panache.runtime.JpaOperations;

public abstract class SagaBase {

    private static final Logger LOG = LoggerFactory.getLogger(SagaBase.class);

    private final Event<ExportedEvent<?, ?>> event;

    private final SagaState state;


    protected SagaBase(Event<ExportedEvent<?, ?>> event, SagaState state) {
        this.event = event;
        this.state = state;
    }

    public final UUID getId() {
        return state.getId();
    }

    public final JsonNode getPayload() {
        return state.getPayload();
    }

    public final String getType() {
        return getClass().getAnnotation(Saga.class).type();
    }

    public final List<String> getStepIds() {
        return Arrays.asList(getClass().getAnnotation(Saga.class).stepIds());
    }

    public SagaStatus getStatus() {
        EntityManager em = JpaOperations.getEntityManager(SagaState.class);
        return em.find(SagaState.class, getId()).getSagaStatus();
    }

    protected void onStepEvent(String type, SagaStepStatus status) {
        try {
            ObjectNode stepStatus = (ObjectNode) state.getStepStatus();
            stepStatus.put(type, status.name());

            if (status == SagaStepStatus.SUCCEEDED) {
                advance();
            }
            else if (status == SagaStepStatus.FAILED || status == SagaStepStatus.COMPENSATED) {
                goBack();
            }

            EnumSet<SagaStepStatus> allStatus = EnumSet.noneOf(SagaStepStatus.class);
            Iterator<String> fieldNames = stepStatus.fieldNames();
            while (fieldNames.hasNext()) {
                allStatus.add(SagaStepStatus.valueOf(stepStatus.get(fieldNames.next()).asText()));
            }

            state.setSagaStatus(getSagaStatus(allStatus));
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

    private SagaStatus getSagaStatus(EnumSet<SagaStepStatus> stepStates) {
        if (containsOnly(stepStates, SagaStepStatus.SUCCEEDED)) {
            return SagaStatus.COMPLETED;
        }
        else if (containsOnly(stepStates, SagaStepStatus.STARTED, SagaStepStatus.SUCCEEDED)) {
            return SagaStatus.STARTED;
        }
        else if (containsOnly(stepStates, SagaStepStatus.FAILED, SagaStepStatus.COMPENSATED)) {
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
            state.setCurrentStep(null);
            return;
        }

        SagaStepMessage stepEvent = getStepMessage(nextStep);
        event.fire(new SagaEvent(getId(), stepEvent.type, stepEvent.eventType, stepEvent.payload));

        ObjectNode stepStatus = (ObjectNode) state.getStepStatus();
        stepStatus.put(nextStep, SagaStepStatus.STARTED.name());

        state.setCurrentStep(nextStep);
    }

    protected final void goBack() throws JsonProcessingException {
        String previousStep = getPreviousStep();

        if (previousStep == null) {
            state.setCurrentStep(null);
            return;
        }

        SagaStepMessage stepEvent = getCompensatingStepMessage(previousStep);
        event.fire(new SagaEvent(getId(), stepEvent.type, stepEvent.eventType, stepEvent.payload));

        ObjectNode stepStatus = (ObjectNode) state.getStepStatus();
        stepStatus.put(previousStep, SagaStepStatus.COMPENSATING.name());

        state.setCurrentStep(previousStep);
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
