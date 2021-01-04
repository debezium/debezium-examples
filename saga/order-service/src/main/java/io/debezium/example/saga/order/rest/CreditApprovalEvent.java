/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.example.saga.order.rest;

import java.util.UUID;

public class CreditApprovalEvent {

    public UUID sagaId;
    public CreditApprovalStatus status;
}
