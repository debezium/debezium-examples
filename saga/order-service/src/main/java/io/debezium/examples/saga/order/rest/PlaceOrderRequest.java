/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.saga.order.rest;

import io.debezium.examples.saga.order.model.PurchaseOrder;
import io.debezium.examples.saga.order.model.PurchaseOrderStatus;

public class PlaceOrderRequest {

    public long itemId;
    public int quantity;
    public long customerId;
    public long paymentDue;
    public String creditCardNo;

    public PurchaseOrder toPurchaseOrder() {
        PurchaseOrder order = new PurchaseOrder();
        order.itemId = itemId;
        order.quantity = quantity;
        order.customerId = customerId;
        order.paymentDue = paymentDue;
        order.creditCardNo = creditCardNo;
        order.status = PurchaseOrderStatus.CREATED;

        return order;
    }
}
