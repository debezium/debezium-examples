package io.debezium.examples.airag;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "RagCommand", mixinStandardHelpOptions = true)
public class RagCommand implements Runnable {

    @Parameters(index = "0", paramLabel = "<operation>", description = "Operation to perform: init, query-chat, list-milvus, insert-document, delete-document", arity = "1")
    private String operation;

    @Parameters(index = "1", paramLabel = "<param>", description = "Command parameter (required if operation is 'query-chat', 'insert-document' or 'delete-document')", arity = "0..*")
    private String[] queryParam;

    @Inject
    Chat chat;

    @Inject
    MilvusStore embeddingStore;

    @Inject
    DocumentDatabase documentDatabase;

    @Override
    public void run() {
        try {
            switch (operation.toLowerCase()) {
            case "init":
                handleInit();
                break;
            case "query-chat":
                handleQuery();
                break;
            case "list-milvus":
                embeddingStore.list();
                break;
            case "delete-document":
                handleDocumentDelete();
                break;
            case "insert-document":
                handleDocumentInsert();
                break;
            default:
                Log.errorf("Invalid operation '%s'", operation);
                CommandLine.usage(this, System.err);
                System.exit(1);
            }
        }
        catch (Exception e) {
            Log.errorf("Error during operation '%s'", operation, e);
            System.exit(1);
        }
    }

    private void handleInit() throws Exception {
        embeddingStore.init();
        documentDatabase.init();
    }


    @ActivateRequestContext
    void handleQuery() {
        if (queryParam == null || queryParam.length == 0) {
            System.err.println("Error: At least one query argument required for 'query' operation.");
            CommandLine.usage(this, System.err);
            System.exit(1);
            return;
        }
        final var query = String.join(" ", queryParam);
        Log.infof("Sending query: %s", query);
        final var reply = chat.chat(query);
        Log.infof("Chat reply: %s", reply);
    }

    void handleDocumentInsert() throws Exception {
        if (queryParam == null || queryParam.length != 1) {
            System.err.println("Error: One ArXiv document id required.");
            CommandLine.usage(this, System.err);
            System.exit(1);
            return;
        }
        documentDatabase.insert(queryParam[0]);
    }

    void handleDocumentDelete() throws Exception {
        if (queryParam == null || queryParam.length != 1) {
            System.err.println("Error: One ArXiv document id required.");
            CommandLine.usage(this, System.err);
            System.exit(1);
            return;
        }
        documentDatabase.delete(queryParam[0]);
    }}