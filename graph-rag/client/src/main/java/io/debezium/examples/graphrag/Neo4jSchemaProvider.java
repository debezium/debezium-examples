/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.graphrag;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Value;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

// Describes the graph schema from Neo4j itself so the Text2Cypher prompt never drifts from reality.
@ApplicationScoped
public class Neo4jSchemaProvider {

    // Hidden from the prompt: the embedding vector is a retrieval detail the LLM should never query.
    private static final String HIDDEN_PROPERTY = "embedding";

    @Inject
    Driver driver;

    private volatile String cached;

    public String text() {
        if (cached == null) {
            synchronized (this) {
                if (cached == null) {
                    cached = build();
                    Log.debugf("Graph schema for Text2Cypher:%n%s", cached);
                }
            }
        }
        return cached;
    }

    private String build() {
        try (final var session = driver.session()) {
            final var nodeProps = new LinkedHashMap<String, LinkedHashSet<String>>();
            session.run("CALL db.schema.nodeTypeProperties()").list().forEach(record -> {
                final var labels = record.get("nodeLabels").asList(Value::asString);
                if (labels.isEmpty()) {
                    return;
                }
                final var props = nodeProps.computeIfAbsent(labels.get(0), key -> new LinkedHashSet<>());
                final var name = record.get("propertyName");
                if (!name.isNull() && !HIDDEN_PROPERTY.equals(name.asString())) {
                    props.add(name.asString());
                }
            });

            final var rels = new LinkedHashSet<String>();
            session.run("""
                    MATCH (a)-[r]->(b)
                    WITH labels(a)[0] AS from, type(r) AS type, keys(r) AS props, labels(b)[0] AS to
                    RETURN DISTINCT from, type, props, to ORDER BY from, type, to
                    """).list().forEach(record -> {
                final var props = record.get("props").asList(Value::asString);
                final var propText = props.isEmpty() ? "" : " {" + String.join(", ", props) + "}";
                rels.add("  (:%s)-[:%s%s]->(:%s)".formatted(
                        record.get("from").asString(), record.get("type").asString(), propText, record.get("to").asString()));
            });

            final var schema = new StringBuilder("Nodes:\n");
            nodeProps.forEach((label, props) -> schema.append("  (:").append(label)
                    .append(" {").append(String.join(", ", props)).append("})\n"));
            schema.append("Relationships:\n");
            rels.forEach(line -> schema.append(line).append("\n"));
            return schema.toString();
        }
    }
}
