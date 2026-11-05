package com.xiongx9527.fw.plus.ai.langchain4j.adapter.model;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.TokenCountEstimator;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.support.LcMessages;
import dev.langchain4j.model.Tokenizer;
import java.util.List;

/**
 * 把 LangChain4j 0.35 的 {@link Tokenizer} 适配成 base 的 {@link TokenCountEstimator}。
 * 消息级估算直接用上游分词器，避免 base 默认实现的字符近似带来的偏差。
 */
public class LangChain4jTokenCountEstimator implements TokenCountEstimator {

    private final Tokenizer tokenizer;

    public LangChain4jTokenCountEstimator(Tokenizer tokenizer) {
        if (tokenizer == null) {
            throw new IllegalArgumentException("tokenizer is required");
        }
        this.tokenizer = tokenizer;
    }

    @Override
    public int estimate(String text) {
        return text == null ? 0 : tokenizer.estimateTokenCountInText(text);
    }

    @Override
    public int estimateInMessages(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        return tokenizer.estimateTokenCountInMessages(LcMessages.toUpstream(messages));
    }
}
