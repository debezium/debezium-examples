/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.camel.pipeline;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;

public class AggregateStore {

    static String PROP_AGGREGATE = "aggregate";

    public AggregateStore() {
    }

    public void readFromStoreAndUpdateIfNeeded(Exchange exchange) {
        final Question question = exchange.getMessage().getBody(Question.class);
        final ProducerTemplate send = exchange.getContext().createProducerTemplate();

        Question aggregate = send.requestBody(QaDatabaseUserNotifier.ROUTE_GET_AGGREGATE, question.getId(), Question.class);
        if (aggregate == null) {
            aggregate = question;
        }
        updateAggregate(exchange, aggregate, send);
        exchange.getMessage().setBody(question);
    }

    public void readFromStoreAndAddAnswer(Exchange exchange) {
        final Answer answer = exchange.getMessage().getBody(Answer.class);

        final ProducerTemplate send = exchange.getContext().createProducerTemplate();

        Question aggregate = send.requestBody(QaDatabaseUserNotifier.ROUTE_GET_AGGREGATE, answer.getQuestionId(), Question.class);
        aggregate.addOrUpdateAnswer(answer);

        updateAggregate(exchange, aggregate, send);
        exchange.getMessage().setBody(answer);
    }

    private void updateAggregate(Exchange exchange, final Question aggregate, final ProducerTemplate send) {
        send.sendBody(QaDatabaseUserNotifier.ROUTE_WRITE_AGGREGATE, aggregate);
        exchange.setProperty(PROP_AGGREGATE, aggregate);
    }
}
