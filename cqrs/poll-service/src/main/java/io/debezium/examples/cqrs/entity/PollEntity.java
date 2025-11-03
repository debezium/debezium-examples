package io.debezium.examples.cqrs.entity;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

/**
 * Entity representing poll. Besides options user can vote for, it contains also records of the votes.
 *
 * @author vjuranek
 */
@Entity
public class PollEntity extends PanacheEntity {
    @Column(length = 128)
    public String question;

    public Timestamp created;

    @OneToMany(cascade = CascadeType.ALL)
    public List<OptionEntity> options;

    @OneToMany(cascade = CascadeType.ALL)
    public List<VoteEntity> votes;

    @Override
    public String toString() {
        return String.format("Poll '%s' with options '%s', current votes '%s'",
                question,
                options.stream().map(Object::toString).collect(Collectors.joining(",")),
                votes.stream().map(Object::toString).collect(Collectors.joining(",")));
    }
}
