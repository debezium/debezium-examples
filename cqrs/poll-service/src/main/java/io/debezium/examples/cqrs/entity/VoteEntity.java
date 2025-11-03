package io.debezium.examples.cqrs.entity;

import java.sql.Timestamp;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

/**
 * Entity representing single vote in a poll.
 *
 * @author vjuranek
 */
@Entity
public class VoteEntity extends PanacheEntity {
    public Long pollId;
    public Long votedOption;
    public Timestamp votedOn;

    @Override
    public String toString() {
        return String.format("Vote for '%s', voted on %s", votedOption, votedOn);
    }
}
