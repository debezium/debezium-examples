/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.example.saga.order.rest;

import io.debezium.example.saga.framework.internal.SagaStepStatus;

public enum CreditApprovalStatus {
    SUCCEEDED, FAILED, ABORTED;

    public SagaStepStatus toStepStatus() {
        return this == ABORTED ? SagaStepStatus.ABORTED : this == FAILED ? SagaStepStatus.FAILED : SagaStepStatus.SUCCEEDED;
    }
}
