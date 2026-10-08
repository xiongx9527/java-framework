package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jTokenizerModule;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import com.xiongx9527.fw.plus.common.util.StringUtils;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.Tokenizer;

import java.util.List;

/**
 * {@link Tokenizer} 的动态代理：持有脚本模块与资源 key，每次调用实时解析当前实例，
 * magic 资源热更新/注销后不持有失效引用。解析失败回落容器中的 Tokenizer bean
 * （key 空取默认、非空按 bean 名）。
 */
public class MagicDynamicTokenizer implements Tokenizer {

    private final Langchain4jTokenizerModule tokenizerModule;

    private final String key;

    public MagicDynamicTokenizer(Langchain4jTokenizerModule tokenizerModule, String key) {
        this.tokenizerModule = tokenizerModule;
        this.key = key;
    }

    private Tokenizer getTokenizer() {
        try {
            return tokenizerModule.getDynamicAttribute(key);
        } catch (Exception e) {
            if (StringUtils.isEmpty(key)) {
                return SpringUtils.getBean(Tokenizer.class);
            }
            return SpringUtils.getBean(key, Tokenizer.class);
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

    @Override
    public int estimateTokenCountInToolExecutionRequests(Iterable<ToolExecutionRequest> requests) {
        return getTokenizer().estimateTokenCountInToolExecutionRequests(requests);
    }

    @Override
    public int estimateTokenCountInToolSpecifications(Iterable<ToolSpecification> toolSpecifications) {
        return getTokenizer().estimateTokenCountInToolSpecifications(toolSpecifications);
    }
}
