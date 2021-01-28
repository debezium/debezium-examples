/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.saga.order.event;

import io.debezium.examples.saga.framework.internal.SagaStepStatus;

public enum CreditApprovalStatus {
    APPROVED, REJECTED, CANCELLED;

    public SagaStepStatus toStepStatus() {
        return this == CANCELLED ? SagaStepStatus.COMPENSATED : this == REJECTED ? SagaStepStatus.FAILED : SagaStepStatus.SUCCEEDED;
    }
}
