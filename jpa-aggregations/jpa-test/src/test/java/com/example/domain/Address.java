/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package com.example.domain;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "addresses")
public class Address {

    @Id
    public long id;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    @JsonIgnore
    public Customer customer;

    public String street;

    public String city;

    public String state;

    public String zip;

    @Enumerated(EnumType.STRING)
    public AddressType type;

    @Override
    public String toString() {
        return "Address [id=" + id + ", street=" + street + ", city=" + city + ", state=" + state + ", zip=" + zip
                + ", type=" + type + "]";
    }
}