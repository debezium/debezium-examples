package io.debezium.examples.aggregation.model;

import com.fasterxml.jackson.annotation.*;

public class Customer {

    private final EventType _eventType;

    private final Integer id;
    private final String first_name;
    private final String last_name;
    private final String email;

    @JsonCreator
    public Customer(
            @JsonProperty("_eventType") EventType _eventType,
            @JsonProperty("id") Integer id,
            @JsonProperty("first_name") String first_name,
            @JsonProperty("last_name") String last_name,
            @JsonProperty("email") String email) {
        this._eventType = _eventType == null ? EventType.UPSERT : _eventType;
        this.id = id;
        this.first_name = first_name;
        this.last_name = last_name;
        this.email = email;
    }

    public EventType get_eventType() {
        return _eventType;
    }

    public Integer getId() {
        return id;
    }

    public String getFirst_name() {
        return first_name;
    }

    public String getLast_name() {
        return last_name;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "_eventType='" + _eventType + '\'' +
                ", id=" + id +
                ", first_name='" + first_name + '\'' +
                ", last_name='" + last_name + '\'' +
                ", email='" + email + '\'' +
                '}';
    }

}
