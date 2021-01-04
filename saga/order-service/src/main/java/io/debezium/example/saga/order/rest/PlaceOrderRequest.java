package io.debezium.example.saga.order.rest;

import io.debezium.example.saga.order.model.PurchaseOrder;

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
