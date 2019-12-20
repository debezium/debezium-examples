package io.debezium.examples.kstreams.liveupdate.eventsource;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

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
