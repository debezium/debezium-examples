/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.kstreams.liveupdate.eventsource;

import java.util.Random;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name="categories")
public class Category {

    @Id
    public long id;

    public String name;

    @Column(name="average_price")
    public long averagePrice;

    @Transient
    private final Random random = new Random();

    Category() {
    }

    public long getRandomPrice() {
        int spread = (int) (averagePrice / 2);
        boolean add = random.nextBoolean();
        return add ? averagePrice + random.nextInt(spread) : averagePrice - random.nextInt(spread);
    }

    @Override
    public String toString() {
        return "Category [id=" + id + ", name=" + name + ", averagePrice=" + averagePrice + "]";
    }
}
