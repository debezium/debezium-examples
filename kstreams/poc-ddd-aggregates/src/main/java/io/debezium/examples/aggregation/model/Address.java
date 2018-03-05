package io.debezium.examples.aggregation.model;

import com.fasterxml.jackson.annotation.*;

public class Address {

    private final EventType _eventType;

    private final Integer id;
    private final Integer customer_id;
    private final String street;
    private final String city;
    private final String state;
    private final String zip;
    private final String type;

    @JsonCreator
    public Address(
            @JsonProperty("_eventType") EventType _eventType,
            @JsonProperty("id") Integer id,
            @JsonProperty("customer_id") Integer customer_id,
            @JsonProperty("street") String street,
            @JsonProperty("city") String city,
            @JsonProperty("state") String state,
            @JsonProperty("zip") String zip,
            @JsonProperty("type") String type) {
        this._eventType = _eventType == null ? EventType.UPSERT : _eventType;
        this.id = id;
        this.customer_id = customer_id;
        this.street = street;
        this.city = city;
        this.state = state;
        this.zip = zip;
        this.type = type;
    }

    public EventType get_eventType() {
        return _eventType;
    }

    public Integer getId() {
        return id;
    }

    public Integer getCustomer_id() {
        return customer_id;
    }

    public String getStreet() {
        return street;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getZip() {
        return zip;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Address{" +
                "_eventType='" + _eventType + '\'' +
                ", id=" + id +
                ", customer_id=" + customer_id +
                ", street='" + street + '\'' +
                ", city='" + city + '\'' +
                ", state='" + state + '\'' +
                ", zip='" + zip + '\'' +
                ", type='" + type + '\'' +
                '}';
    }

}
