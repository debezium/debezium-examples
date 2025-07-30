package org.acme;

import io.quarkus.debezium.engine.deserializer.ObjectMapperDeserializer;

public class ProductDeserializer extends ObjectMapperDeserializer<Product> {
    public ProductDeserializer() {
        super(Product.class);
    }
}
