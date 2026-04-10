package io.debezium.demos.auditing.vegetables.service;

import io.debezium.demos.auditing.vegetables.model.Vegetable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class VegetableService {

    @Inject
    EntityManager entityManager;

    public Vegetable createVegetable(Vegetable vegetable) {
        entityManager.persist(vegetable);
        return vegetable;
    }

    public Vegetable updateVegetable(Vegetable vegetable) {
        Vegetable existing = entityManager.getReference(Vegetable.class, vegetable.getId());

        existing.setName(vegetable.getName());
        existing.setDescription(vegetable.getDescription());

        return existing;
    }

    public void deleteVegetable(long id) {
        Vegetable existing = entityManager.getReference(Vegetable.class, id);
        entityManager.remove(existing);
    }
}
