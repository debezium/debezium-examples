package io.debezium.examples.cqrs.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Entity representing poll results, summing all the votes for given option.
 *
 * @author vjuranek
 */
@Entity
public class OptionVotesEntity {
    @Id
    public String option;
    public long votes;

    public OptionVotesEntity() {
    }
}
