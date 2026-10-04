package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 长度限制包装策略：对被包装策略选出的消息按字符数/条数限制进行过滤或裁剪。
 * <p>对无有效字符的 AiMessage 始终过滤。</p>
 */
public final class LengthBoundedAiPersistenceStrategy implements AiPersistenceStrategy {

    private final AiPersistenceStrategy delegate;
    private final int maxCharsPerMessage;
    private final int maxTotalChars;
    private final int maxMessages;
    private final boolean truncateIfExceeds;

    private LengthBoundedAiPersistenceStrategy(Builder b) {
        this.delegate = b.delegate;
        this.maxCharsPerMessage = b.maxCharsPerMessage;
        this.maxTotalChars = b.maxTotalChars;
        this.maxMessages = b.maxMessages;
        this.truncateIfExceeds = b.truncateIfExceeds;
    }

    public static Builder builder(AiPersistenceStrategy delegate) {
        return new Builder(delegate);
    }

    @Override
    public List<PersistedAiItem> selectForPersistenceItems(List<ChatMessage> previousTurnMessages) {
        List<PersistedAiItem> selected = delegate.selectForPersistenceItems(previousTurnMessages);
        if (selected == null || selected.isEmpty()) {
            return Collections.emptyList();
        }

        List<PersistedAiItem> result = new ArrayList<>(Math.min(selected.size(), maxMessages));
        int total = 0;
        for (int i = 0; i < selected.size() && result.size() < maxMessages; i++) {
            PersistedAiItem item = selected.get(i);
            if (item == null || item.message() == null) {
                continue;
            }
            ChatMessage m = item.message();
            if (!(m instanceof AiMessage)) {
                result.add(item);
                continue;
            }
            AiMessage ai = (AiMessage) m;
            if (!AiPersistenceStrategy.hasEffectiveText(ai)) {
                continue;
            }
            String text = ai.text();
            int len = text == null ? 0 : text.length();

            if (maxCharsPerMessage >= 0 && len > maxCharsPerMessage) {
                if (!truncateIfExceeds) {
                    continue;
                }
                int cut = Math.max(0, maxCharsPerMessage);
                String truncatedText = text.substring(0, cut);
                AiMessage truncatedAi = AiMessage.from(truncatedText);
                if (ai.toolExecutionRequests() != null && !ai.toolExecutionRequests().isEmpty()) {
                    truncatedAi = AiMessage.from(truncatedText, ai.toolExecutionRequests());
                }
                if (!AiPersistenceStrategy.hasEffectiveText(truncatedAi)) {
                    continue;
                }
                int tLen = truncatedAi.text() == null ? 0 : truncatedAi.text().length();
                if (maxTotalChars >= 0 && total + tLen > maxTotalChars) {
                    continue;
                }
                result.add(PersistedAiItem.builder(truncatedAi)
                        .position(item.position())
                        .priority(item.priority())
                        .build());
                total += tLen;
                continue;
            }

            if (maxTotalChars >= 0 && total + len > maxTotalChars) {
                continue;
            }
            result.add(item);
            total += len;
        }
        return result.isEmpty() ? Collections.emptyList() : result;
    }

    public static final class Builder {
        private AiPersistenceStrategy delegate = AiPersistenceStrategy.DROP_ALL_AI;
        private int maxCharsPerMessage = -1;
        private int maxTotalChars = -1;
        private int maxMessages = Integer.MAX_VALUE;
        private boolean truncateIfExceeds = false;

        private Builder(AiPersistenceStrategy delegate) {
            if (delegate != null) {
                this.delegate = delegate;
            }
        }

        public Builder maxCharsPerMessage(int maxCharsPerMessage) {
            this.maxCharsPerMessage = maxCharsPerMessage;
            return this;
        }

        public Builder maxTotalChars(int maxTotalChars) {
            this.maxTotalChars = maxTotalChars;
            return this;
        }

        public Builder maxMessages(int maxMessages) {
            this.maxMessages = Math.max(0, maxMessages);
            return this;
        }

        public Builder truncateIfExceeds(boolean truncateIfExceeds) {
            this.truncateIfExceeds = truncateIfExceeds;
            return this;
        }

        public LengthBoundedAiPersistenceStrategy build() {
            return new LengthBoundedAiPersistenceStrategy(this);
        }
    }
}
