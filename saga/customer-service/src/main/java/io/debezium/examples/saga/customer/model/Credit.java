/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.saga.customer.model;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Credit extends PanacheEntity {

    @JsonProperty("order-id")
    public long orderId;

    @JsonProperty("customer-id")
    public long customerId;

    @JsonProperty("payment-due")
    public long paymentDue;

    @JsonProperty("credit-card-no")
    public String creditCardNo;

    @Enumerated(EnumType.STRING)
    public CreditRequestType type;

    public Credit() {
    }

    public Credit(long orderId, long customerId, long paymentDue, String creditCardNo, CreditRequestType type) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.paymentDue = paymentDue;
        this.creditCardNo = creditCardNo;
        this.type = type;
    }
}
