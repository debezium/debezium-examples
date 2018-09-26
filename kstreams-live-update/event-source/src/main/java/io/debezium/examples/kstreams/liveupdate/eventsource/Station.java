package io.debezium.examples.kstreams.liveupdate.eventsource;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="stations")
public class Station {

    @Id
    public long id;

    public String name;

    Station() {
    }

    @Override
    public String toString() {
        return "Station [id=" + id + ", name=" + name + "]";
    }
}
