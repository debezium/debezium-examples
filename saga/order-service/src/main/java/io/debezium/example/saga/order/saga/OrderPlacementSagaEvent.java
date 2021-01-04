package io.debezium.example.saga.order.saga;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class OrderPlacementSagaEvent extends PanacheEntity {

    @ManyToOne
    public OrderPlacementSaga saga;

    public String type;

    public String payload;
}
