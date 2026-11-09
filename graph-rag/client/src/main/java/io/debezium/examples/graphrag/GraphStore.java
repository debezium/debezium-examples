/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.graphrag;

import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.neo4j.driver.Driver;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GraphStore {

    static final String VECTOR_INDEX = "product_embeddings";

    @ConfigProperty(name = "graph.rag.embedding.dimensions", defaultValue = "768")
    int dimensions;

    @Inject
    Driver driver;

    public void createIndex() {
        try (final var session = driver.session()) {
            session.run("""
                    CREATE VECTOR INDEX %s IF NOT EXISTS
                    FOR (p:Product) ON (p.embedding)
                    OPTIONS { indexConfig: {
                        `vector.dimensions`: $dimensions,
                        `vector.similarity_function`: 'cosine'
                    } }
                    """.formatted(VECTOR_INDEX), Map.of("dimensions", dimensions));

            // Vector indexes populate asynchronously; wait so an immediate query sees every product.
            session.run("CALL db.awaitIndexes(300)");
            Log.infof("Vector index '%s' is ready", VECTOR_INDEX);
        }
    }

    public void list() {
        try (final var session = driver.session()) {
            final var counts = session.run("""
                    RETURN
                        COUNT { (c:Customer) } AS customers,
                        COUNT { (p:Product) } AS products,
                        COUNT { (e:Product) WHERE e.embedding IS NOT NULL } AS embedded,
                        COUNT { (o:Order) } AS orders
                    """).single();
            Log.infof("Graph contents: %d Customer(s), %d Product(s) (%d embedded), %d Order(s)",
                    counts.get("customers").asLong(),
                    counts.get("products").asLong(),
                    counts.get("embedded").asLong(),
                    counts.get("orders").asLong());
        }
    }
}
