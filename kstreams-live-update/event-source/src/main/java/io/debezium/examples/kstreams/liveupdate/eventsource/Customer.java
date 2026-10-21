package io.debezium.examples.kstreams.liveupdate.eventsource;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="customers")
public class Customer {
    @Id
    public long id;
    public String firstName;
    public String lastName;
    public String email;

    public Customer() {

    }
}
