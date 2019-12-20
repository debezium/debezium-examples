package io.debezium.examples.kstreams.liveupdate.eventsource;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

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
