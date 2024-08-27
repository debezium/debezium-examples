package io.debezium.examples;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ApplicationScoped
public class AppLifecycleBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppLifecycleBean.class);

    private final MeterRegistry registry;

    @ConfigProperty(name = "app.version")
    String appVersion;

    public AppLifecycleBean(MeterRegistry registry) {
        this.registry = registry;
    }

    void onStart(@Observes StartupEvent ev) {
        LOGGER.info("The application with version {} is starting...", appVersion);

        registry.gauge("application.info",
                Tags.of(Tag.of("version", appVersion),
                        Tag.of("name", "Order Service")),
                Integer.parseInt("1"));

    }

    void onStop(@Observes ShutdownEvent ev) {
        LOGGER.info("The application is stopping...");
    }

}