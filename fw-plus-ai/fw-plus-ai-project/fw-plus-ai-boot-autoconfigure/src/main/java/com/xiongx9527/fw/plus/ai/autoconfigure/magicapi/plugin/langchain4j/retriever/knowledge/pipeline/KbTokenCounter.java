package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.pipeline;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.TokenCountEstimator;

/**
 * token 计数单一封装点（升级兼容约束，设计 D9）：
 * 实现 langchain4j 1.x 的 {@link TokenCountEstimator}（core 无默认实现，各提供商实现绑定
 * 自家 API 不通用），估算体为 CJK/拉丁混合字符近似（中文按 1 字 ≈ 1 token、英文按
 * 4 字符 ≈ 1 token 的混合近似），不引入 Tokenizer 词表依赖；
 * 后续如需精确计数只改本类（如换用 OpenAiTokenCountEstimator）。
 */
public final class KbTokenCounter implements TokenCountEstimator {

    private static final KbTokenCounter INSTANCE = new KbTokenCounter();

    private KbTokenCounter() {
    }

    /** 单例（无状态）。 */
    public static KbTokenCounter instance() {
        return INSTANCE;
    }

    /** 近似估算 token 数（知识库链路沿用入口）。 */
    public static int estimate(String text) {
        return INSTANCE.estimateTokenCountInText(text);
    }

    @Override
    public int estimateTokenCountInText(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int cjk = 0;
        int other = 0;
        for (int i = 0; i < text.length(); i++) {
            if (Character.UnicodeScript.of(text.charAt(i)) == Character.UnicodeScript.HAN) {
                cjk++;
            } else {
                other++;
            }
        }
        return cjk + (int) Math.ceil(other / 4.0);
    }

    @Override
    public int estimateTokenCountInMessage(ChatMessage message) {
        return estimateTokenCountInText(message == null ? null : String.valueOf(message));
    }

    @Override
    public int estimateTokenCountInMessages(Iterable<ChatMessage> messages) {
        if (messages == null) {
            return 0;
        }
        int total = 0;
        for (ChatMessage message : messages) {
            total += estimateTokenCountInMessage(message);
        }
        return total;
    }
}
