package io.debezium.examples.airag;

import java.util.function.Supplier;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.AugmentationRequest;
import dev.langchain4j.rag.AugmentationResult;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Metadata;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MilvusRetrieavalAugmentor implements Supplier<RetrievalAugmentor> {

    private final RetrievalAugmentor augmentor;

    MilvusRetrieavalAugmentor(MilvusEmbeddingStore store, EmbeddingModel model) {
        EmbeddingStoreContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingModel(model)
                .embeddingStore(store)
                .maxResults(3)
                .build();
        augmentor = new RetrievalAugmentorDecorator(DefaultRetrievalAugmentor
                .builder()
                .contentRetriever(contentRetriever)
                .build());
    }

    @Override
    public RetrievalAugmentor get() {
        return augmentor;
    }

    private class RetrievalAugmentorDecorator implements RetrievalAugmentor {

        private final RetrievalAugmentor delegate;

        public RetrievalAugmentorDecorator(RetrievalAugmentor delegate) {
            this.delegate = delegate;
        }

        @Override
        public AugmentationResult augment(AugmentationRequest augmentationRequest) {
            Log.infof("Requested augmentation of %s", augmentationRequest.chatMessage());
            final var result = delegate.augment(augmentationRequest);
            Log.infof("Result of augmentation is %s", result.contents());
            return result;
        }
    }
}