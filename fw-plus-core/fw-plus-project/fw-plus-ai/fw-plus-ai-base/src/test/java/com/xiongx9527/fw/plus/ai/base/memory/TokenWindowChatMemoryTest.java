package com.xiongx9527.fw.plus.ai.base.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.ChatRole;
import com.xiongx9527.fw.plus.ai.base.model.TokenCountEstimator;
import java.util.List;
import org.junit.jupiter.api.Test;

class TokenWindowChatMemoryTest {

    /** 每个字符算 1 token，便于精确断言。 */
    private static final TokenCountEstimator CHARS = text -> text == null ? 0 : text.length();

    @Test
    void trimsByTokenBudgetKeepingSystemMessage() {
        ChatMemory memory = TokenWindowChatMemory.builder()
                .id("t1")
                .maxTokens(20)
                .estimator(CHARS)
                .build();
        memory.add(ChatMessage.system("sys"));
        memory.add(ChatMessage.user("aaaaaaaaaa"));
        memory.add(ChatMessage.assistant("bbbbbbbbbb"));
        memory.add(ChatMessage.user("cc"));

        List<ChatMessage> messages = memory.messages();

        assertEquals(ChatRole.SYSTEM, messages.get(0).getRole());
        assertTrue(CHARS.estimateInMessages(messages) <= 20);
        assertEquals("cc", messages.get(messages.size() - 1).getContent());
    }

    @Test
    void oversizedMessageStillDroppedWhenOverBudget() {
        ChatMemory memory = TokenWindowChatMemory.builder()
                .id("t2")
                .maxTokens(50)
                .estimator(CHARS)
                .build();
        memory.add(ChatMessage.user("x"));
        memory.add(ChatMessage.assistant("0123456789012345678901234567890123456789"));

        assertTrue(CHARS.estimateInMessages(memory.messages()) <= 50);
    }

    @Test
    void defaultEstimatorApproximatesFourCharsPerToken() {
        assertEquals(0, TokenWindowChatMemory.DEFAULT_ESTIMATOR.estimate(""));
        assertEquals(1, TokenWindowChatMemory.DEFAULT_ESTIMATOR.estimate("abc"));
        assertEquals(2, TokenWindowChatMemory.DEFAULT_ESTIMATOR.estimate("abcdefgh"));
    }
}
