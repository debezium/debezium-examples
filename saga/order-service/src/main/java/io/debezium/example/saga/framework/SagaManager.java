package io.debezium.example.saga.framework;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.debezium.example.saga.order.saga.SagaStatus;
import io.debezium.example.saga.order.saga.SagaStepStatus;

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
            SagaStepEvent stepEvent = saga.getStepEvent(stepId);

            SagaStepEventState stepState = new SagaStepEventState();
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

    public <S extends Saga> void process(S saga, SagaStepState stepState) {
        SagaState state = entityManager.find(SagaState.class, saga.getId());
        TypeReference<HashMap<String, SagaStepStatus>> typeRef = new TypeReference<>() {};
        try {
            HashMap<String, SagaStepStatus> stepStates = objectMapper.readValue(state.getStepState(), typeRef);

            if (stepState.status == SagaStepStatus.FAILED) {
                for (Entry<String, SagaStepStatus> oneState : stepStates.entrySet()) {
                    if (!oneState.getKey().equals(stepState.type)) {
                        if (oneState.getValue() == SagaStepStatus.STARTED || oneState.getValue() == SagaStepStatus.SUCCEEDED) {
                            SagaStepEvent compensation = saga.getCompensatingStep(oneState.getKey());

                            SagaStepEventState compensationStepState = new SagaStepEventState();
                            compensationStepState.sagaId = state.getId();
                            compensationStepState.type = compensation.type;
                            compensationStepState.payload = compensation.payload;
                            oneState.setValue(SagaStepStatus.ABORTING);
                            entityManager.persist(compensationStepState);
                        }
                    }
                }
            }

            stepStates.put(stepState.type, stepState.status);
            state.setStepState(objectMapper.writeValueAsString(stepStates));
            state.setStatus(getSagaStatus(stepStates.values()));
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private SagaStatus getSagaStatus(Collection<SagaStepStatus> stepStates) {
        if (stepStates.equals(Collections.singleton(SagaStepStatus.SUCCEEDED))) {
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
