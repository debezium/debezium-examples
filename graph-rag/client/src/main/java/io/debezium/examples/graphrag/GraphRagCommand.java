/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.graphrag;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "GraphRagCommand", mixinStandardHelpOptions = true)
public class GraphRagCommand implements Runnable {

    @Parameters(index = "0", paramLabel = "<operation>", description = "Operation to perform: create-index, list-graph, query-chat, chat", arity = "1")
    private String operation;

    @Parameters(index = "1", paramLabel = "<param>", description = "Command parameter (the question when operation is 'query-chat')", arity = "0..*")
    private String[] queryParam;

    @Inject
    Chat chat;

    @Inject
    GraphStore graphStore;

    @Override
    public void run() {
        try {
            switch (operation.toLowerCase()) {
                case "create-index":
                    graphStore.createIndex();
                    break;
                case "list-graph":
                    graphStore.list();
                    break;
                case "query-chat":
                    handleQuery();
                    break;
                case "chat":
                    interactiveChat();
                    break;
                default:
                    Log.errorf("Invalid operation '%s'", operation);
                    CommandLine.usage(this, System.err);
                    System.exit(1);
            }
        }
        catch (Exception e) {
            Log.errorf(e, "Error during operation '%s'", operation);
            System.exit(1);
        }
    }

    @ActivateRequestContext
    void handleQuery() {
        if (queryParam == null || queryParam.length == 0) {
            System.err.println("Error: At least one query argument required for 'query-chat' operation.");
            CommandLine.usage(this, System.err);
            System.exit(1);
            return;
        }
        final var query = String.join(" ", queryParam);
        Log.infof("Sending query: %s", query);
        final var reply = chat.chat(query);
        Log.infof("Chat reply: %s", reply);
    }

    @ActivateRequestContext
    void interactiveChat() throws Exception {
        System.out.println("Graph-RAG chat. Ask about products, orders and customers. Type 'exit' to quit.");
        try (final var reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            while (true) {
                System.out.print("\nYou: ");
                System.out.flush();
                final var line = reader.readLine();
                if (line == null) {
                    break;
                }
                final var question = line.strip();
                if (question.isEmpty()) {
                    continue;
                }
                if (question.equalsIgnoreCase("exit") || question.equalsIgnoreCase("quit")) {
                    break;
                }
                try {
                    System.out.println("Assistant: " + chat.chat(question));
                }
                catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }
            }
        }
        System.out.println("Bye.");
    }
}
