package io.debezium.demos.auditing.vegetables.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

@Entity
public class Vegetable {

    private Long id;
    private String name;
    private String description;

    @Id
    @SequenceGenerator(
            name = "vegetablesSequence",
            sequenceName = "vegetables_id_seq",
            allocationSize = 10,
            initialValue = 10)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vegetablesSequence")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
