package io.debezium.examples.airag;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService(retrievalAugmentor = MilvusRetrievalAugmentor.class)
public interface Chat {

    @SystemMessage("You are an expert that provides short summaries.")
    String chat(@UserMessage String message);
}