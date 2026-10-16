package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 工具结果持久化包装策略。
 * <p>默认所有策略都不持久化 ToolExecutionResultMessage。此包装器仅在显式开启时才追加工具结果。</p>
 */
public final class PersistToolResultsAiPersistenceStrategy implements AiPersistenceStrategy {

    private final AiPersistenceStrategy delegate;
    private final boolean persistToolResults;
    private final int maxToolResultChars;
    private final int maxToolResults;

    private PersistToolResultsAiPersistenceStrategy(Builder b) {
        this.delegate = b.delegate;
        this.persistToolResults = b.persistToolResults;
        this.maxToolResultChars = b.maxToolResultChars;
        this.maxToolResults = b.maxToolResults;
    }

    public static Builder builder(AiPersistenceStrategy delegate) {
        return new Builder(delegate);
    }

    @Override
    public List<PersistedAiItem> selectForPersistenceItems(List<ChatMessage> previousTurnMessages) {
        List<PersistedAiItem> selected = delegate.selectForPersistenceItems(previousTurnMessages);
        List<PersistedAiItem> result = new ArrayList<>(
                (selected == null ? 0 : selected.size()) + (previousTurnMessages == null ? 0 : previousTurnMessages.size())
        );
        if (selected != null && !selected.isEmpty()) {
            result.addAll(selected);
        }

        if (!persistToolResults || previousTurnMessages == null || previousTurnMessages.isEmpty()) {
            return result.isEmpty() ? Collections.emptyList() : result;
        }

        int count = 0;
        for (ChatMessage m : previousTurnMessages) {
            if (m instanceof ToolExecutionResultMessage) {
                if (count >= maxToolResults) {
                    continue;
                }
                ToolExecutionResultMessage tr = (ToolExecutionResultMessage) m;
                String txt = tr.text();
                if (txt == null) {
                    continue;
                }
                String stripped = txt.replaceAll("\\s+", "");
                if (stripped.isEmpty()) {
                    continue;
                }
                if (maxToolResultChars >= 0 && txt.length() > maxToolResultChars) {
                    continue;
                }
                result.add(PersistedAiItem.builder(tr)
                        .position(AiPersistencePosition.AFTER_LAST_USER)
                        .priority(10)
                        .build());
                count++;
            }
        }
        return result.isEmpty() ? Collections.emptyList() : result;
    }

    public static final class Builder {
        private AiPersistenceStrategy delegate = AiPersistenceStrategy.DROP_ALL_AI;
        private boolean persistToolResults = false;
        private int maxToolResultChars = 512;
        private int maxToolResults = 0;

        private Builder(AiPersistenceStrategy delegate) {
            if (delegate != null) {
                this.delegate = delegate;
            }
        }

        public Builder persistToolResults(boolean persistToolResults) {
            this.persistToolResults = persistToolResults;
            return this;
        }

        public Builder maxToolResultChars(int maxToolResultChars) {
            this.maxToolResultChars = maxToolResultChars;
            return this;
        }

        public Builder maxToolResults(int maxToolResults) {
            this.maxToolResults = Math.max(0, maxToolResults);
            return this;
        }

        public PersistToolResultsAiPersistenceStrategy build() {
            return new PersistToolResultsAiPersistenceStrategy(this);
        }
    }
}
