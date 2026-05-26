package io.debezium.demos.auditing.admin.wih;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.kie.kogito.process.impl.DefaultWorkItemHandlerConfig;

/**
 * Registers the custom work item handlers used by the BPMN processes. In Kogito the handler name
 * must match the work item type generated for the BPMN task ("Send Task" for the {@code <sendTask>}).
 */
@ApplicationScoped
public class AuditWorkItemHandlerConfig extends DefaultWorkItemHandlerConfig {

    @Inject
    SendTransactionEventWIHandler sendTransactionEventWIHandler;

    @PostConstruct
    void init() {
        register("Send Task", sendTransactionEventWIHandler);
    }
}
