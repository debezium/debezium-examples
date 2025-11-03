package io.debezium.examples.cqrs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Entity representing poll question.
 *
 * @author vjuranek
 */
@Entity
public class PollEntity {
    @Id
    public Long id;

    @Column(length = 128)
    public String question;
}
