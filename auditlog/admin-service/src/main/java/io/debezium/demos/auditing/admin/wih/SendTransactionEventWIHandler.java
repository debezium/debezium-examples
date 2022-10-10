package io.debezium.demos.auditing.admin.wih;

import java.util.Collections;

import javax.enterprise.context.ApplicationScoped;

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

@ApplicationScoped
public class SendTransactionEventWIHandler implements WorkItemHandler {

    private ObjectMapper json = new ObjectMapper();

    @Channel("missingtransactions")
    Emitter<String> emitter;

    @Override
    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        try {
            VegetableEvent vegetable = (VegetableEvent) workItem.getParameter("vegetable");
            AuditData audit = (AuditData) workItem.getParameter("audit");

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

            String key = json.writeValueAsString(Collections.singletonMap("transaction_id", vegetable.getSource().getTransactionId()));
            String value = json.writeValueAsString(event);

            emitter.send(KafkaRecord.of(key, value));

            manager.completeWorkItem(workItem.getId(), null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
    }

    @Override
    public String getName() {
        return "Send Task";
    }
}
