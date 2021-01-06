package io.debezium.examples.saga.order.facade;

import io.debezium.examples.saga.order.event.PaymentEvent;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class PaymentDeserializer extends ObjectMapperDeserializer<PaymentEvent> {

    public PaymentDeserializer() {
        super(PaymentEvent.class);
    }
}
