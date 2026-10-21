package io.debezium.examples.kstreams.liveupdate.eventsource;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="products")
public class Product {
    @Id
    public long id;

    public String name;
    public String description;
    public Float weight;

    public Product() {

    }
}
