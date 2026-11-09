/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.graphrag;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService(retrievalAugmentor = Neo4jRetrievalAugmentor.class)
public interface Chat {

    @SystemMessage("""
            You are a helpful assistant for an online store. Answer the user's question using ONLY
            the product, order and customer facts provided in the context, which come from the
            store's live knowledge graph. If the context does not contain the answer, say you do not
            know. Keep answers concise.
            """)
    String chat(@UserMessage String message);
}
