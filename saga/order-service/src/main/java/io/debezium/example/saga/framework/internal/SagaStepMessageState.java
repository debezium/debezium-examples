/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.example.saga.framework.internal;

import java.util.UUID;

import javax.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class SagaStepMessageState extends PanacheEntity {

    public UUID sagaId;
    public String type;
    public String payload;
}
