/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.saga.customer.model;

import javax.persistence.Entity;
import javax.persistence.Version;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Customer extends PanacheEntity {

    public long openLimit;

    @Version
    private int version;

    public Customer() {
    }

    public boolean fitsCreditLimit(long paymentDue) {
        return openLimit - paymentDue > 0;
    }

    public void allocateCreditLimit(long paymentDue) {
        openLimit = openLimit - paymentDue;
    }

    public void releaseCreditLimit(long paymentDue) {
        openLimit = openLimit + paymentDue;
    }
}
