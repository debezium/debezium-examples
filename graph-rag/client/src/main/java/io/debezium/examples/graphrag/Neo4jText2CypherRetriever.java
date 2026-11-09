/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.graphrag;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

// Entity/relational path: the chat model writes a read-only Cypher query that is run on the graph.
@ApplicationScoped
public class Neo4jText2CypherRetriever implements ContentRetriever {

    private static final String PROMPT = """
            Translate the user's question into ONE read-only Neo4j Cypher query over this schema:
            %s
            Requirements:
            - Output ONLY the Cypher query - no explanation, no markdown fences.
            - Use only MATCH, OPTIONAL MATCH, WITH, WHERE, RETURN, ORDER BY, LIMIT.
              Never CREATE, MERGE, SET, DELETE, REMOVE, DROP or LOAD.
            - RETURN readable aliased properties (names, description, status, ids),
              never whole nodes and never the 'embedding' property.
            - Match people and products by name case-insensitively using toLower().
            - The schema always contains customer, product and order data. ANY question about
              customers, products, orders or purchases MUST produce a query - assume the data exists.
            - Output exactly NONE only for pure small-talk (e.g. "hi", "thanks") that asks nothing
              about customers, products or orders.

            Examples:
            Question: What did Sally buy?
            Cypher: MATCH (c:Customer)<-[:PLACED_BY]-(o:Order)-[ci:CONTAINS]->(p:Product)
            WHERE toLower(c.first_name) = 'sally'
            RETURN c.first_name + ' ' + c.last_name AS customer, o.id AS order, o.status AS status, ci.quantity AS quantity, p.name AS product, p.description AS description

            Question: Which orders have been shipped?
            Cypher: MATCH (o:Order)-[:PLACED_BY]->(c:Customer)
            WHERE o.status = 'shipped'
            RETURN o.id AS order, c.first_name + ' ' + c.last_name AS customer, o.status AS status

            Question: %s
            Cypher:""";

    @Inject
    Driver driver;

    @Inject
    ChatModel chatModel;

    @Inject
    Neo4jSchemaProvider schemaProvider;

    @Override
    public List<Content> retrieve(Query query) {
        final var cypher = cleanup(chatModel.chat(PROMPT.formatted(schemaProvider.text(), query.text())));

        if (!isUsableReadQuery(cypher)) {
            Log.debugf("Text2Cypher produced no usable query for '%s' (got: %s)", query.text(), cypher);
            return List.of();
        }

        try (final var session = driver.session()) {
            final var rows = session.run(cypher).list();
            final var contents = new ArrayList<Content>(rows.size());
            for (final var row : rows) {
                contents.add(Content.from(toText(row)));
            }
            return contents;
        }
        catch (Exception e) {
            Log.debugf("Text2Cypher query failed for '%s' (%s): %s", query.text(), cypher, e.getMessage());
            return List.of();
        }
    }

    private static String cleanup(String raw) {
        if (raw == null) {
            return "";
        }
        var text = raw.strip();
        if (text.startsWith("```")) {
            text = text.replaceAll("(?s)```[a-zA-Z]*", "").replace("```", "").strip();
        }
        // The model may echo the "Cypher:" cue from the prompt.
        if (text.regionMatches(true, 0, "cypher:", 0, "cypher:".length())) {
            text = text.substring("cypher:".length()).strip();
        }
        return text;
    }

    // Guards against model-generated text mutating the graph: read clause only, no write keywords.
    private static boolean isUsableReadQuery(String cypher) {
        if (cypher.isBlank() || cypher.equalsIgnoreCase("NONE")) {
            return false;
        }
        final var upper = cypher.toUpperCase();
        if (!(upper.startsWith("MATCH") || upper.startsWith("OPTIONAL MATCH") || upper.startsWith("WITH"))) {
            return false;
        }
        if (!upper.contains("RETURN")) {
            return false;
        }
        for (final var forbidden : List.of("CREATE", "MERGE", "SET ", "DELETE", "REMOVE", "DROP", "LOAD CSV")) {
            if (upper.contains(forbidden)) {
                return false;
            }
        }
        return true;
    }

    private static String toText(Record row) {
        final var joiner = new StringJoiner(", ");
        for (final var key : row.keys()) {
            joiner.add(key + ": " + row.get(key).asObject());
        }
        return joiner.toString();
    }
}
