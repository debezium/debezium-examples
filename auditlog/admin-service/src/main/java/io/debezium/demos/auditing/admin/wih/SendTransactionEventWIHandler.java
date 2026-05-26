package io.debezium.demos.auditing.admin.wih;

import java.util.Collections;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.kie.kogito.internal.process.workitem.KogitoWorkItem;
import org.kie.kogito.internal.process.workitem.KogitoWorkItemHandler;
import org.kie.kogito.internal.process.workitem.KogitoWorkItemManager;
import org.kie.kogito.internal.process.workitem.WorkItemTransition;
import org.kie.kogito.process.workitems.impl.DefaultKogitoWorkItemHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.debezium.demos.auditing.admin.AuditData;
import io.debezium.demos.auditing.admin.TransactionData;
import io.debezium.demos.auditing.admin.TransactionEvent;
import io.debezium.demos.auditing.admin.VegetableEvent;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;

@ApplicationScoped
public class SendTransactionEventWIHandler extends DefaultKogitoWorkItemHandler {

    private ObjectMapper json = new ObjectMapper();

    @Channel("missingtransactions")
    Emitter<String> emitter;

    @Override
    public Optional<WorkItemTransition> activateWorkItemHandler(KogitoWorkItemManager manager, KogitoWorkItemHandler handler,
            KogitoWorkItem workItem, WorkItemTransition transition) {
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

            return Optional.of(handler.completeTransition(workItem.getPhaseStatus(), Collections.emptyMap()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<WorkItemTransition> abortWorkItemHandler(KogitoWorkItemManager manager, KogitoWorkItemHandler handler,
            KogitoWorkItem workItem, WorkItemTransition transition) {
        return Optional.empty();
    }
}
