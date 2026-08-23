package com.xiongx9527.fw.plus.ai.autoconfigure.spring.adapter;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

final class SpringAiModelApiAvailableCondition extends AnyNestedCondition {

    SpringAiModelApiAvailableCondition() {
        super(ConfigurationPhase.PARSE_CONFIGURATION);
    }

    @ConditionalOnClass(name = "org.springframework.ai.chat.model.ChatModel")
    static class ChatModelApiAvailable {
    }

    @ConditionalOnClass(name = "org.springframework.ai.embedding.EmbeddingModel")
    static class EmbeddingModelApiAvailable {
    }
}
