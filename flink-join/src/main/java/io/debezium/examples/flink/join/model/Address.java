package io.debezium.examples.flink.join.model;

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

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Address)) {
            return false;
        }
        Address other = (Address)obj;
        return other.id == id;
    }

}
