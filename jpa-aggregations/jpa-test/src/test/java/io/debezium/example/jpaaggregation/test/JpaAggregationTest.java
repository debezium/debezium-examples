/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.example.jpaaggregation.test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.example.domain.Category;
import com.example.domain.Customer;

/**
 * Simple test for creating a customer aggregate.
 */
public class JpaAggregationTest {

    private EntityManagerFactory entityManagerFactory;

    @Before
    public void init() {
        entityManagerFactory = Persistence.createEntityManagerFactory("templatePU");
    }

    @After
    public void destroy() {
        entityManagerFactory.close();
    }

    @Test
    public void createCustomerAggregate() throws Exception {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        entityManager.createQuery("update Customer c set c.active = false where c.id = 1004").executeUpdate();
        Customer sally = entityManager.find(Customer.class, 1004L);
//
        sally.active = true;
        sally.firstName = "Sally Deluxe 123";
        Category category = new Category();
        category.id = 100001L;
        category.name = "Retail";
        entityManager.persist(category);

        sally.category = category;
//        sally.someBlob = "some text".getBytes();
//
//
//
//        entityManager.merge(sally);

        entityManager.getTransaction().commit();
        entityManager.close();
    }
}
