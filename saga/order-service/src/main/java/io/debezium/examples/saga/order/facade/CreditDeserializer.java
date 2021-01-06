package io.debezium.examples.saga.order.facade;

import io.debezium.examples.saga.order.event.CreditApprovalEvent;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class CreditDeserializer extends ObjectMapperDeserializer<CreditApprovalEvent> {

    public CreditDeserializer() {
        super(CreditApprovalEvent.class);
    }
}
