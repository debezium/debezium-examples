/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.saga.order.facade.serdes;

import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.debezium.examples.saga.order.event.CreditApprovalEvent;
import io.debezium.examples.saga.order.event.CreditApprovalEventPayload;
import io.smallrye.reactive.messaging.MessageConverter;
import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecordMetadata;

@ApplicationScoped
public class CreditEventConverter implements MessageConverter {

    @Override
    public boolean canConvert(Message<?> in, Type target) {
        return in.getMetadata(IncomingKafkaRecordMetadata.class).isPresent()
                && target.equals(CreditApprovalEvent.class);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Message<?> convert(Message<?> in, Type target) {
        IncomingKafkaRecordMetadata metadata = in.getMetadata(IncomingKafkaRecordMetadata.class)
                .orElseThrow(() -> new IllegalStateException("No Kafka metadata"));

        UUID messageId = UUID.fromString(getHeaderAsString(metadata.getHeaders(), "id"));
        UUID sagaId = UUID.fromString((String) metadata.getKey());

        return in.withPayload(new CreditApprovalEvent(sagaId, messageId, ((CreditApprovalEventPayload) in.getPayload()).status));
    }

    private String getHeaderAsString(Headers headers, String name) {
        Header header = headers.lastHeader(name);
        if (header == null) {
            throw new IllegalArgumentException("Expected record header '" + name + "' not present");
        }

        return new String(header.value(), Charset.forName("UTF-8"));
    }
}