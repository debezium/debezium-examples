/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.example.saga.order.rest;

import io.debezium.example.saga.order.model.PurchaseOrder;
import io.debezium.example.saga.order.model.PurchaseOrderStatus;

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
