package io.debezium.example.saga.order.rest;

import io.debezium.example.saga.order.model.PurchaseOrder;

public class PlaceOrderResponse {

    public long orderId;
    public PurchaseOrderStatus status;

    public static PlaceOrderResponse fromPurchaseOrder(PurchaseOrder order) {
        PlaceOrderResponse response = new PlaceOrderResponse();
        response.orderId = order.id;
        response.status = order.status;

        return response;
    }
}
