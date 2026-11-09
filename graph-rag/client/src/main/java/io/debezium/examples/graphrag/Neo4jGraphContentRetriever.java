/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.graphrag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

// Semantic path: vector-search Product nodes, then traverse to their orders and customers.
@ApplicationScoped
public class Neo4jGraphContentRetriever implements ContentRetriever {

    private static final String RETRIEVAL_QUERY = """
            CALL db.index.vector.queryNodes($index, $k, $vector) YIELD node AS p, score
            WHERE score >= $minScore
            OPTIONAL MATCH (p)<-[ci:CONTAINS]-(o:Order)-[:PLACED_BY]->(c:Customer)
            RETURN p.name AS name, p.description AS description, p.price AS price, score,
                   collect(CASE WHEN o IS NULL THEN null ELSE {
                       order: o.id, status: o.status, quantity: ci.quantity,
                       customer: c.first_name + ' ' + c.last_name
                   } END) AS orders
            ORDER BY score DESC
            """;

    @ConfigProperty(name = "graph.rag.retriever.max-results", defaultValue = "5")
    int maxResults;

    // Drop matches below this cosine score so off-topic input retrieves nothing. Model-dependent
    // (nomic-embed-text floors ~0.72 for unrelated text, genuine matches ~0.89+)
    @ConfigProperty(name = "graph.rag.retriever.min-score", defaultValue = "0.8")
    double minScore;

    @Inject
    Driver driver;

    @Inject
    EmbeddingModel embeddingModel;

    @Override
    public List<Content> retrieve(Query query) {
        final var vector = embed(query.text());

        try (final var session = driver.session()) {
            final var rows = session.run(RETRIEVAL_QUERY, Map.of(
                    "index", GraphStore.VECTOR_INDEX,
                    "k", maxResults,
                    "minScore", minScore,
                    "vector", vector)).list();

            final var contents = new ArrayList<Content>(rows.size());
            final var scores = new ArrayList<String>(rows.size());
            for (final var row : rows) {
                contents.add(Content.from(toText(row)));
                scores.add("%s=%.4f".formatted(row.get("name").asString("?"), row.get("score").asDouble()));
            }
            Log.debugf("Retrieved %d graph context snippet(s) for query '%s' (min-score=%.2f); scores: %s",
                    contents.size(), query.text(), minScore, scores);
            return contents;
        }
    }

    private static String toText(Record row) {
        final var text = new StringBuilder()
                .append("Product: ").append(row.get("name").asString("?"))
                .append(" (price ").append(row.get("price").asObject()).append(").")
                .append(" Description: ").append(row.get("description").asString(""));

        final var orders = row.get("orders").asList(value -> value.asMap());
        if (orders.isEmpty()) {
            text.append(" This product has not been ordered yet.");
        }
        else {
            final var joiner = new StringJoiner("; ");
            for (final Map<String, Object> order : orders) {
                joiner.add("%s ordered %s in order #%s (%s)".formatted(
                        order.get("customer"), order.get("quantity"), order.get("order"), order.get("status")));
            }
            text.append(" Orders: ").append(joiner).append(".");
        }
        return text.toString();
    }

    private List<Float> embed(String text) {
        final var vector = embeddingModel.embed(TextSegment.from(text)).content().vector();
        final var list = new ArrayList<Float>(vector.length);
        for (final float value : vector) {
            list.add(value);
        }
        return list;
    }
}
