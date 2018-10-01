/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.kstreams.liveupdate.eventsource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Random;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

class EventSource {

    private boolean running = true;
    private Thread thread;
    private final Random random = new Random();

    public void run() {
        thread = new Thread(() -> {
            EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("templatePU");
            EntityManager entityManager = entityManagerFactory.createEntityManager();

            entityManager.getTransaction().begin();
            List<Station> stations = entityManager.createQuery("from Station s", Station.class).getResultList();
            entityManager.getTransaction().commit();

            int i = 0;
            while (running) {
                if (i % 50 == 0) {
                    System.out.println("Inserted " + i + " measurements");
                }

                entityManager.getTransaction().begin();

                entityManager.persist(getRandomMeasurement(entityManager, stations));
                entityManager.getTransaction().commit();

                i++;
                try {
                    Thread.sleep(100);
                }
                catch (InterruptedException e) {
                    System.out.println("Interrupted");
                    running = false;
                }
            }

            System.out.println("Clean-up");

            entityManager.close();
            entityManagerFactory.close();
        });

        thread.start();
    }

    private TemperatureMeasurement getRandomMeasurement(EntityManager entityManager, List<Station> stations) {
        Station station = stations.get(random.nextInt(stations.size()));

        return new TemperatureMeasurement(
                entityManager.getReference(Station.class, station.id),
                getRandomTemperature(),
                ZonedDateTime.now()
        );
    }

    private double getRandomTemperature() {
        BigDecimal bigDecimal = BigDecimal.valueOf(40 * random.nextDouble());
        bigDecimal = bigDecimal.setScale(1, RoundingMode.HALF_UP);
        return bigDecimal.doubleValue();
    }

    public void stop() {
        try {
            thread.interrupt();
            thread.join();
        }
        catch (InterruptedException e) {
        }
    }
}