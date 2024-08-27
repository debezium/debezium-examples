/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class LoadSimulator {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadSimulator.class);

    private final OrderRepository orderRepository;

    @ConfigProperty(name = "app.version")
    String appVersion;

    private final Random random = new Random();

    LoadSimulator(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Scheduled(every="10s", delay = 5, delayUnit = TimeUnit.SECONDS)
    @Transactional
    void insertOrders(){

        LOGGER.info("Storing orders");
        List<Order> orders = Stream.generate(this::getOrder).limit(100).collect(Collectors.toList());

        if (appVersion.equals("1.0")) {
            orderRepository.persist(orders);
        } else {
            if (random.nextBoolean()) { //Just to simulate a drop of 50% of standard insert rate
                orderRepository.persist(orders);
            }
        }

    }

    private Order getOrder() {

        Order order = new Order();
        order.setOrderDate(LocalDate.now());
        order.setProductId(101);
        order.setPurchaser(1001);
        order.setQuantity(100);

        return order;
    }
}
