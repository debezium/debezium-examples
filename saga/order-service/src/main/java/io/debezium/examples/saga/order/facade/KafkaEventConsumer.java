/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.saga.order.facade;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.OptimisticLockException;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.debezium.examples.saga.order.event.CreditApprovalEvent;
import io.debezium.examples.saga.order.event.PaymentEvent;
import io.debezium.examples.saga.order.saga.OrderPlacementEventHandler;
import io.opentracing.Scope;
import io.opentracing.Tracer;
import io.opentracing.contrib.kafka.TracingKafkaUtils;

@ApplicationScoped
public class KafkaEventConsumer {

    private static final int RETRIES = 5;

    @Inject
    private OrderPlacementEventHandler eventHandler;

    @Inject
    Tracer tracer;

    @Incoming("paymentresponse")
    public CompletionStage<Void> onPaymentMessage(PaymentEvent event) throws IOException {
        return CompletableFuture.runAsync(() -> {
            try (final Scope span = tracer.buildSpan("orders").asChildOf(TracingKafkaUtils.extractSpanContext(event.headers, tracer)).startActive(true)) {
                retrying(() -> eventHandler.onPaymentEvent(event));
            }
        });
    }

    @Incoming("creditresponse")
    public CompletionStage<Void> onCreditMessage(CreditApprovalEvent event) throws IOException {
        return CompletableFuture.runAsync(() -> {
            try (final Scope span = tracer.buildSpan("orders").asChildOf(TracingKafkaUtils.extractSpanContext(event.headers, tracer)).startActive(true)) {
                retrying(() -> eventHandler.onCreditApprovalEvent(event));
            }
        });
    }

    private void retrying(Runnable runnable) {
        int tries = 0;

        while (tries < RETRIES) {
            try {
                tries++;
                runnable.run();
                return;
            }
            catch(OptimisticLockException ole) {
                if (tries == RETRIES) {
                    throw ole;
                }
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
}
