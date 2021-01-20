package io.debezium.examples.caching.cacheupdater.streams.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OrderWithLines {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderWithLines.class);

    public Long id;

    @JsonProperty("customer_id")
    public long customerId;

    @JsonProperty("order_date")
//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
//    public ZonedDateTime orderDate;
    public long orderDate;

    public List<OrderLine> lines = new ArrayList<>();

    public OrderWithLines addOrderLine(OrderLineAndPurchaseOrder lineAndPurchaseOrder) {
        LOGGER.info("Adding: " + lineAndPurchaseOrder);

        id = lineAndPurchaseOrder.purchaseOrder.id;
        customerId = lineAndPurchaseOrder.purchaseOrder.customerId;
        orderDate = lineAndPurchaseOrder.purchaseOrder.orderDate / 1_000;
        lines.add(lineAndPurchaseOrder.orderLine);

        return this;
    }

    public OrderWithLines removeOrderLine(OrderLineAndPurchaseOrder lineAndPurchaseOrder) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        OrderWithLines that = (OrderWithLines) o;
        return customerId == that.customerId && Objects.equals(id, that.id) && Objects.equals(orderDate, that.orderDate)
              && Objects.equals(lines, that.lines);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, customerId, orderDate, lines);
    }
}
