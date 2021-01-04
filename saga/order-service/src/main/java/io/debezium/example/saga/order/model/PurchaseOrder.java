/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.example.saga.order.model;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class PurchaseOrder extends PanacheEntity {

    public long itemId;
    public int quantity;
    public long customerId;
    public long paymentDue;
    public String creditCardNo;

    @Enumerated(EnumType.STRING)
    public PurchaseOrderStatus status;
}
