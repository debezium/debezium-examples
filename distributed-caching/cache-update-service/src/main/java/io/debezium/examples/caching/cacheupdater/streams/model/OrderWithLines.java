package io.debezium.examples.caching.cacheupdater.streams.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OrderWithLines {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderWithLines.class);

    public Long id;

    @JsonProperty("customer_id")
    public long customerId;

    @JsonProperty("order_date")
    public Instant orderDate;

    public List<OrderLine> lines = new ArrayList<>();

    public OrderWithLines addAddress(OrderLineAndPurchaseOrder lineAndPurchaseOrder) {
        LOGGER.info("Adding: " + lineAndPurchaseOrder);

        id = lineAndPurchaseOrder.purchaseOrder.id;
        customerId = lineAndPurchaseOrder.purchaseOrder.customerId;
        orderDate = lineAndPurchaseOrder.purchaseOrder.orderDate;
        lines.add(lineAndPurchaseOrder.orderLine);

        return this;
    }

    public OrderWithLines removeAddress(OrderLineAndPurchaseOrder lineAndPurchaseOrder) {
        LOGGER.info("Removing: " + lineAndPurchaseOrder);

        Iterator<OrderLine> it = lines.iterator();
        while (it.hasNext()) {
            OrderLine a = it.next();
            if (a.id == lineAndPurchaseOrder.orderLine.id) {
                it.remove();
                break;
            }
        }

        return this;
    }

    @Override
    public String toString() {
        return "OrderWithLines [id=" + id + ", customerId=" + customerId + ", orderDate=" + orderDate + ", lines="
                + lines + "]";
    }
}
