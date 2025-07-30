package org.acme;

import io.debezium.runtime.Capturing;
import io.debezium.runtime.CapturingEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ProductListener {

    private final ProductsService productsService;
    private final Logger logger = LoggerFactory.getLogger(ProductListener.class);

    @Inject
    public ProductListener(ProductsService productsService) {
        this.productsService = productsService;
    }

    @Capturing(destination = "dbz.inventory.products")
    public void products(CapturingEvent<Product> event) {
        logger.info("capturing filtered event from {} with data {}", event.destination(), event.record());
        productsService.save(event.record());
    }

    @Capturing
    public void capture(CapturingEvent<SourceRecord> event) {
        logger.info("capturing event from {} with data {}", event.destination(), event.record());
    }
}
