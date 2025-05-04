package io.debezium.examples.airag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
public class RagCommandTest {

    @Test
    void testInitOperation(QuarkusMainLauncher launcher) {
        var result = launcher.launch("init");
        assertEquals(0, result.exitCode());
        assertTrue(() -> result.getOutput().contains("Created collection 'demo_ai_documents'"));
    }

    @Test
    void testListMilvusOperation(QuarkusMainLauncher launcher) {
        var result = launcher.launch("list-milvus");
        assertEquals(0, result.exitCode());
        assertTrue(() -> result.getOutput().contains("Milvus list results size:"));
    }

    @Test
    void testQueryChatOperation(QuarkusMainLauncher launcher) {
        var result = launcher.launch("query-chat", "Describe IterQR framework in 20 words");
        assertEquals(0, result.exitCode());
        assertTrue(() -> result.getOutput().contains("Chat reply:"));
    }

    @Test
    void testDeleteDocumentOperation(QuarkusMainLauncher launcher) {
        var result = launcher.launch("delete-document", DocumentDatabase.SAMPLE_ARXIV_DOCUMENT);
        assertEquals(0, result.exitCode());
    }

    @Test
    void testinsertDocumentOperation(QuarkusMainLauncher launcher) {
        var result = launcher.launch("insert-document", DocumentDatabase.SAMPLE_ARXIV_DOCUMENT);
        assertEquals(0, result.exitCode());
    }
}