/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package com.example.domain;

import java.time.LocalDate;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import io.debezium.aggregation.hibernate.MaterializeAggregate;

@Entity
@Table(name = "customers")
@MaterializeAggregate(aggregateName="customers-complete")
public class Customer {

    @Id
    public long id;

    @Column(name = "first_name")
    public String firstName;

    @Column(name = "last_name")
    public String lastName;

    public String email;

    @Column(name = "some_blob")
    public byte[] someBlob;

    @Column(name = "isactive")
    public boolean active;

    @ElementCollection
    @CollectionTable(
          name="customer_scores",
          joinColumns=@JoinColumn(name="customer_id")
    )
    @Column(name="score")
    @OrderColumn(name="idx")
    public double[] scores;

    @ElementCollection
    @CollectionTable(
          name="customer_tags",
          joinColumns=@JoinColumn(name="customer_id")
    )
    @Column(name="tag")
    @OrderColumn(name="idx")
    public String[] tags;

    public LocalDate birthday;

    @ManyToOne
    public Category category;

    @OneToMany(mappedBy = "customer", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    public Set<Address> addresses;

    @Override
    public String toString() {
        return "Customer [id=" + id + ", firstName=" + firstName + ", lastName=" + lastName + ", email=" + email
                + ", addresses=" + addresses + "]";
    }
}
