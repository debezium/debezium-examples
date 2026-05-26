package io.debezium.demos.auditing.admin.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieRuntimeBuilder;

import io.debezium.demos.auditing.admin.TransactionEvent;
import io.debezium.demos.auditing.admin.VegetableEvent;

@ApplicationScoped
public class AdminService {

    @Inject
    KieRuntimeBuilder kruntimeBuilder;
    
    private KieSession workinMemory;
    
    @PostConstruct
    public void setup() {
        
        this.workinMemory = kruntimeBuilder.newKieSession();
    }
    
    @PreDestroy
    public void cleanup() {
        this.workinMemory.dispose();
    }
    
    public TransactionEvent processTransaction(TransactionEvent event) {
        if (event == null) {
            throw new RuntimeException("Missing transaction event");
        }
        
        workinMemory.insert(event);
        
        workinMemory.fireAllRules();
        return event;
    }

    public VegetableEvent processVegetable(VegetableEvent event) {
        if (event == null) {
            throw new RuntimeException("Missing transaction event");
        }
        
        workinMemory.insert(event);
        
        workinMemory.fireAllRules();
        return event;
    }
}
