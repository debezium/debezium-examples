package io.debezium.demos.auditing.admin.service;

import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import io.debezium.demos.auditing.admin.TransactionEvent;
import io.debezium.demos.auditing.admin.VegetableEvent;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AdminService {

    private KieContainer kContainer;

    @PostConstruct
    public void setup() {
        KieServices kieServices = KieServices.Factory.get();
        this.kContainer = kieServices.getKieClasspathContainer();
    }

    public void processEvents(TransactionEvent tx, VegetableEvent veg) {
        if (tx == null || veg == null) {
            throw new IllegalArgumentException("Both TransactionEvent and VegetableEvent are required");
        }

        KieSession session = null;

        try {
            session = kContainer.newKieSession();

            session.insert(tx);
            session.insert(veg);

            session.fireAllRules();

        } finally {
            if (session != null) {
                session.dispose();
            }
        }
    }
}