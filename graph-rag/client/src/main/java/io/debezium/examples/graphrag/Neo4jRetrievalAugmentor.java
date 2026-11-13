/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.graphrag;

import java.util.function.Supplier;

import dev.langchain4j.rag.AugmentationRequest;
import dev.langchain4j.rag.AugmentationResult;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

// Routes every question to both retrievers (semantic + Text2Cypher) and merges their results.
@ApplicationScoped
public class Neo4jRetrievalAugmentor implements Supplier<RetrievalAugmentor> {

    private final RetrievalAugmentor augmentor;

    Neo4jRetrievalAugmentor(Neo4jGraphContentRetriever vectorRetriever, Neo4jText2CypherRetriever cypherRetriever) {
        augmentor = new RetrievalAugmentorDecorator(DefaultRetrievalAugmentor
                .builder()
                .queryRouter(new DefaultQueryRouter(vectorRetriever, cypherRetriever))
                .build());
    }

    @Override
    public RetrievalAugmentor get() {
        return augmentor;
    }

    private static class RetrievalAugmentorDecorator implements RetrievalAugmentor {

        private final RetrievalAugmentor delegate;

        RetrievalAugmentorDecorator(RetrievalAugmentor delegate) {
            this.delegate = delegate;
        }

        @Override
        public AugmentationResult augment(AugmentationRequest augmentationRequest) {
            Log.debugf("Requested augmentation of %s", augmentationRequest.chatMessage());
            final var result = delegate.augment(augmentationRequest);
            Log.debugf("Result of augmentation is %s", result.contents());
            return result;
        }
    }
}
