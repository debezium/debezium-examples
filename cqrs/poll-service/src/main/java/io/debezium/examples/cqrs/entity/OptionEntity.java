package io.debezium.examples.cqrs.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

/**
 * Entity representing a single option in a poll.
 *
 * @author vjuranek
 */
@Entity
public class OptionEntity extends PanacheEntity {
    public String pollOption;
}
