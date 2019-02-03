/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.outbox.order.outbox;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Describes an event which should exported via the "outbox" table.
 *
 * @author Gunnar Morling
 */
public interface ExportedEvent {

    /**
     * The id of the aggregate affected by a given event. E.g. the order id in case of events relating to a purchase
     * order, or order lines of that order. Used to ensure order of events belonging to one order, customer etc.
     */
    String getAggregateId();

    /**
     * The type of the aggregate affected by a given event, E.g. "order" in case of events relating to a purchase order,
     * or order lines of that order. Used as a topic name.
     */
    String getAggregateType();

    /**
     * The actual event payload.
     */
    JsonNode getPayload();

    /**
     * The type of an event, e.g. "Order Created", "Order Line Canceled" for events of the "order" aggregate type.
     */
    String getType();
}
