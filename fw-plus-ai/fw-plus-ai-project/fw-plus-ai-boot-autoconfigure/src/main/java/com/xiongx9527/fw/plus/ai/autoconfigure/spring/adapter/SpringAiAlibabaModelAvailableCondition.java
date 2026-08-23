package com.xiongx9527.fw.plus.ai.autoconfigure.spring.adapter;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

final class SpringAiAlibabaModelAvailableCondition extends AnyNestedCondition {

    SpringAiAlibabaModelAvailableCondition() {
        super(ConfigurationPhase.PARSE_CONFIGURATION);
    }

    @ConditionalOnClass(name = "com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel")
    static class ChatModelAvailable {
    }

    @ConditionalOnClass(name = "com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel")
    static class EmbeddingModelAvailable {
    }
}
