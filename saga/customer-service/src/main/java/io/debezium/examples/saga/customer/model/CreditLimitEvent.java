/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.saga.customer.model;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreditLimitEvent {

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

    public CreditLimitEvent() {
    }
}
