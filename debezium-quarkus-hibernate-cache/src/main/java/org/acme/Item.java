package org.acme;

import java.math.BigDecimal;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Cacheable
@Table(name = "item", schema = "inventory")
public class Item {

    @Id
    public Long id;

    public String name;

    public BigDecimal price;

    public Item() {
    }

    public Item(Long id, String name, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }
}
