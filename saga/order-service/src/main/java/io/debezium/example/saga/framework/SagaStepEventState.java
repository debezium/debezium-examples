package io.debezium.example.saga.framework;

import java.util.UUID;

import javax.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class SagaStepEventState extends PanacheEntity {

    public UUID sagaId;
    public String type;
    public String payload;
}
