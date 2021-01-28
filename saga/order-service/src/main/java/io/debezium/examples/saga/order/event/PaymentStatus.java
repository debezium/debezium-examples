/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.saga.order.event;

import io.debezium.examples.saga.framework.internal.SagaStepStatus;

public enum PaymentStatus {
    REQUESTED, CANCELLED, FAILED, COMPLETED;

    public SagaStepStatus toStepStatus() {
        switch(this) {
        case CANCELLED:
            return SagaStepStatus.COMPENSATED;
        case COMPLETED:
        case REQUESTED:
            return SagaStepStatus.SUCCEEDED;
        case FAILED:
            return SagaStepStatus.FAILED;
        default:
            throw new IllegalArgumentException("Unexpected state: " + this);
        }
    }
}
