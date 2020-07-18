package io.debezium.examples.kstreams.fkjoin.model;

public class Address {

    public long id;
    public int customer_id;
    public String street;
    public String city;
    public String zipcode;
    public String country;

    @Override
    public String toString() {
        return "Address [id=" + id + ", customer_id=" + customer_id + ", street=" + street + ", city=" + city
                + ", zipcode=" + zipcode + ", country=" + country + "]";
    }
}
