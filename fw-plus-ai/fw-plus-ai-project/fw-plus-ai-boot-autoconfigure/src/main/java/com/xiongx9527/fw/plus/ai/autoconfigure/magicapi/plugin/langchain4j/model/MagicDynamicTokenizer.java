package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jTokenizerModule;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import com.xiongx9527.fw.plus.common.util.StringUtils;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.TokenCountEstimator;

/**
 * {@link TokenCountEstimator} 的动态代理：持有脚本模块与资源 key，每次调用实时解析当前实例，
 * magic 资源热更新/注销后不持有失效引用。解析失败回落容器中的 TokenCountEstimator bean
 * （key 空取默认、非空按 bean 名），与 {@code MagicDynamicChatMemoryStore} 的兜底语义一致。
 */
public class MagicDynamicTokenizer implements TokenCountEstimator {

    private final Langchain4jTokenizerModule tokenizerModule;

    private final String key;

    public MagicDynamicTokenizer(Langchain4jTokenizerModule tokenizerModule, String key) {
        this.tokenizerModule = tokenizerModule;
        this.key = key;
    }

    private TokenCountEstimator getTokenizer() {
        try {
            return tokenizerModule.getDynamicAttribute(key);
        } catch (Exception e) {
            if (StringUtils.isEmpty(key)) {
                return SpringUtils.getBean(TokenCountEstimator.class);
            }
            return SpringUtils.getBean(key, TokenCountEstimator.class);
        }
    }

    @Override
    public int estimateTokenCountInText(String text) {
        return getTokenizer().estimateTokenCountInText(text);
    }

    @Override
    public int estimateTokenCountInMessage(ChatMessage message) {
        return getTokenizer().estimateTokenCountInMessage(message);
    }

    @Override
    public int estimateTokenCountInMessages(Iterable<ChatMessage> messages) {
        return getTokenizer().estimateTokenCountInMessages(messages);
    }
}
