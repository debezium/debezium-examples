package org.acme;

import io.debezium.runtime.Capturing;
import io.debezium.runtime.CapturingEvent;
import io.debezium.runtime.CapturingEvent.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class InventoryListener {

    private final ProductsService productsService;
    private final Logger logger = LoggerFactory.getLogger(InventoryListener.class);

    @Inject
    public InventoryListener(ProductsService productsService) {
        this.productsService = productsService;
    }

    @Capturing(destination = "dbz.inventory.products")
    public void products(CapturingEvent<Product> product) {
        switch (product) {
            case Create<Product> event ->
                    logger.info("capturing product creation from {} with data {}", event.destination(), event.record());
            case Delete<Product> event ->
                    logger.info("capturing product deletion from {} with data {}", event.destination(), event.record());
            case Message<Product> event ->
                    logger.info("capturing product message from {} with data {}", event.destination(), event.record());
            case Read<Product> event ->
                    logger.info("capturing product read (snapshot) from {} with data {}", event.destination(), event.record());
            case Truncate<Product> event ->
                    logger.info("capturing product truncate from {} with data {}", event.destination(), event.record());
            case Update<Product> event ->
                    logger.info("capturing product update from {} with data {}", event.destination(), event.record());
        }

        productsService.save(product.record());
    }

    @Capturing(destination = "dbz.inventory.orders")
    public void orders(CapturingEvent<SourceRecord> event) {
        logger.info("capturing orders from {} with data {}", event.destination(), event.record());
    }

    @Capturing()
    public void any(CapturingEvent<SourceRecord> event) {
        logger.info("capturing any other event from {} with data {}", event.destination(), event.record());
    }
}
