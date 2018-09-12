/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package com.example.domain;

import java.time.LocalDate;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

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
