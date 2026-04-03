package io.debezium.demos.auditing.admin.wih;

import java.util.Collections;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.debezium.demos.auditing.admin.AuditData;
import io.debezium.demos.auditing.admin.TransactionData;
import io.debezium.demos.auditing.admin.TransactionEvent;
import io.debezium.demos.auditing.admin.VegetableEvent;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SendTransactionEventWIHandler implements WorkItemHandler {

    @Inject
    ObjectMapper json;

    @Channel("missingtransactions")
    Emitter<KafkaRecord<String, String>> emitter;

    @Override
    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        try {
            VegetableEvent vegetable = (VegetableEvent) workItem.getParameter("vegetable");
            AuditData audit = (AuditData) workItem.getParameter("audit");

            if (vegetable == null || audit == null) {
                throw new IllegalArgumentException("Missing required parameters: vegetable or audit");
            }

            TransactionEvent event = buildEvent(vegetable, audit);

            String key = json.writeValueAsString(
                    Collections.singletonMap("transaction_id", vegetable.getSource().getTransactionId()));

            String value = json.writeValueAsString(event);

            emitter.send(KafkaRecord.of(key, value))
                    .toCompletableFuture()
                    .exceptionally(ex -> {
                        throw new RuntimeException("Failed to send Kafka message", ex);
                    });

            manager.completeWorkItem(workItem.getId(), null);

        } catch (Exception e) {
            manager.abortWorkItem(workItem.getId());
            throw new RuntimeException(e);
        }
    }

    private TransactionEvent buildEvent(VegetableEvent vegetable, AuditData audit) {

        TransactionEvent event = new TransactionEvent();

        event.setSource(vegetable.getSource());
        event.setOperation(vegetable.getOperation());
        event.setTimestamp(new java.util.Date());

        TransactionData data = new TransactionData();
        data.setTransactionId(vegetable.getSource().getTransactionId());
        data.setUseCase(audit.getUseCase());
        data.setClientDate(new java.util.Date());
        data.setUsername(audit.getUsername());

        event.setAfter(data);

        return event;
    }

    @Override
    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
        manager.abortWorkItem(workItem.getId());
    }
}