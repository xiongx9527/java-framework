package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * EphemeralChatMemory 配置项。
 * <p>支持内置策略名、自定义策略名、自定义策略类全限定名。</p>
 */
public class EphemeralChatMemoryProperties {

    /**
     * 是否启用 EphemeralChatMemory。
     */
    private boolean enabled = true;

    /**
     * AI 持久化策略。
     */
    private String aiPersistenceStrategy = "PERSIST_NON_TOOL_FINAL_ANSWER";

    /**
     * 自定义 AI 持久化策略注册表（name -> className）。
     */
    private Map<String, String> customStrategies = new LinkedHashMap<>();

    /**
     * 是否持久化 ToolExecutionResultMessage。
     */
    private boolean persistToolResults = false;

    /**
     * 持久化的 ToolExecutionResultMessage 最大字符数（-1 不限制）。
     */
    private int maxToolResultChars = 512;

    /**
     * 持久化的 ToolExecutionResultMessage 最大条数。
     */
    private int maxToolResults = 0;

    /**
     * 是否对持久化的 AI 消息进行长度限制。
     */
    private boolean limitAiLength = false;

    /**
     * 单条持久化 AI 消息最大字符数（-1 不限制）。
     */
    private int maxAiCharsPerMessage = -1;

    /**
     * 持久化 AI 消息总字符数上限（-1 不限制）。
     */
    private int maxAiTotalChars = -1;

    /**
     * 持久化 AI 消息最大条数（<=0 表示不限制条数）。
     */
    private int maxAiMessages = Integer.MAX_VALUE;

    /**
     * 当单条 AI 超过长度限制时是否截断。
     */
    private boolean truncateAiIfExceeds = false;

    /**
     * 临时内容分隔符（按顺序匹配，用于剥离 RAG/拼接段）。
     */
    private List<String> ephemeralSeparators = Collections.singletonList(EphemeralChatMemory.DEFAULT_RAG_SEPARATOR);

    /**
     * 历史 UserMessage 是否剥离临时段。
     */
    private boolean stripRagFromHistory = true;

    /**
     * 当前轮 UserMessage 是否保留临时段。
     */
    private boolean includeCurrentUserWithEphemeral = true;

    /**
     * 当前轮是否发送 ToolExecutionResultMessage。
     */
    private boolean sendToolResultsInCurrentTurn = true;

    /**
     * 当前轮是否发送带工具调用请求的 AiMessage（工具调用中间步）。
     */
    private boolean sendToolRequestsInCurrentTurn = true;

    // Getter/Setter
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAiPersistenceStrategy() {
        return aiPersistenceStrategy;
    }

    public void setAiPersistenceStrategy(String aiPersistenceStrategy) {
        this.aiPersistenceStrategy = aiPersistenceStrategy;
    }

    public Map<String, String> getCustomStrategies() {
        return customStrategies;
    }

    public void setCustomStrategies(Map<String, String> customStrategies) {
        if (customStrategies == null) {
            this.customStrategies = new LinkedHashMap<>();
        } else {
            this.customStrategies = new LinkedHashMap<>(customStrategies);
        }
    }

    public boolean isPersistToolResults() {
        return persistToolResults;
    }

    public void setPersistToolResults(boolean persistToolResults) {
        this.persistToolResults = persistToolResults;
    }

    public int getMaxToolResultChars() {
        return maxToolResultChars;
    }

    public void setMaxToolResultChars(int maxToolResultChars) {
        this.maxToolResultChars = maxToolResultChars;
    }

    public int getMaxToolResults() {
        return maxToolResults;
    }

    public void setMaxToolResults(int maxToolResults) {
        this.maxToolResults = maxToolResults;
    }

    public boolean isLimitAiLength() {
        return limitAiLength;
    }

    public void setLimitAiLength(boolean limitAiLength) {
        this.limitAiLength = limitAiLength;
    }

    public int getMaxAiCharsPerMessage() {
        return maxAiCharsPerMessage;
    }

    public void setMaxAiCharsPerMessage(int maxAiCharsPerMessage) {
        this.maxAiCharsPerMessage = maxAiCharsPerMessage;
    }

    public int getMaxAiTotalChars() {
        return maxAiTotalChars;
    }

    public void setMaxAiTotalChars(int maxAiTotalChars) {
        this.maxAiTotalChars = maxAiTotalChars;
    }

    public int getMaxAiMessages() {
        return maxAiMessages;
    }

    public void setMaxAiMessages(int maxAiMessages) {
        this.maxAiMessages = maxAiMessages;
    }

    public boolean isTruncateAiIfExceeds() {
        return truncateAiIfExceeds;
    }

    public void setTruncateAiIfExceeds(boolean truncateAiIfExceeds) {
        this.truncateAiIfExceeds = truncateAiIfExceeds;
    }

    public List<String> getEphemeralSeparators() {
        return ephemeralSeparators;
    }

    public void setEphemeralSeparators(List<String> ephemeralSeparators) {
        if (ephemeralSeparators == null || ephemeralSeparators.isEmpty()) {
            this.ephemeralSeparators = Collections.singletonList(EphemeralChatMemory.DEFAULT_RAG_SEPARATOR);
            return;
        }
        this.ephemeralSeparators = new ArrayList<>(ephemeralSeparators);
    }

    public boolean isStripRagFromHistory() {
        return stripRagFromHistory;
    }

    public void setStripRagFromHistory(boolean stripRagFromHistory) {
        this.stripRagFromHistory = stripRagFromHistory;
    }

    public boolean isIncludeCurrentUserWithEphemeral() {
        return includeCurrentUserWithEphemeral;
    }

    public void setIncludeCurrentUserWithEphemeral(boolean includeCurrentUserWithEphemeral) {
        this.includeCurrentUserWithEphemeral = includeCurrentUserWithEphemeral;
    }

    public boolean isSendToolResultsInCurrentTurn() {
        return sendToolResultsInCurrentTurn;
    }

    public void setSendToolResultsInCurrentTurn(boolean sendToolResultsInCurrentTurn) {
        this.sendToolResultsInCurrentTurn = sendToolResultsInCurrentTurn;
    }

    public boolean isSendToolRequestsInCurrentTurn() {
        return sendToolRequestsInCurrentTurn;
    }

    public void setSendToolRequestsInCurrentTurn(boolean sendToolRequestsInCurrentTurn) {
        this.sendToolRequestsInCurrentTurn = sendToolRequestsInCurrentTurn;
    }

    public void registerCustomStrategies() {
        if (customStrategies == null || customStrategies.isEmpty()) {
            return;
        }
        customStrategies.forEach((name, className) -> {
            if (name == null || name.trim().isEmpty() || className == null || className.trim().isEmpty()) {
                return;
            }
            try {
                Class<?> clazz = Class.forName(className.trim());
                if (AiPersistenceStrategy.class.isAssignableFrom(clazz)) {
                    java.lang.reflect.Constructor<?> ctor = clazz.getDeclaredConstructor();
                    if (!ctor.isAccessible()) {
                        ctor.setAccessible(true);
                    }
                    AiPersistenceStrategy instance = (AiPersistenceStrategy) ctor.newInstance();
                    AiPersistenceStrategy.Registry.register(name.trim(), instance);
                }
            } catch (Exception ignored) {
            }
        });
    }

    public AiPersistenceStrategy resolveAiPersistenceStrategy() {
        registerCustomStrategies();
        AiPersistenceStrategy base = AiPersistenceStrategy.Registry.resolve(aiPersistenceStrategy);
        AiPersistenceStrategy strategy = base;

        if (limitAiLength) {
            strategy = LengthBoundedAiPersistenceStrategy.builder(strategy)
                    .maxCharsPerMessage(maxAiCharsPerMessage)
                    .maxTotalChars(maxAiTotalChars)
                    .maxMessages(maxAiMessages <= 0 ? Integer.MAX_VALUE : maxAiMessages)
                    .truncateIfExceeds(truncateAiIfExceeds)
                    .build();
        }

        if (persistToolResults) {
            strategy = PersistToolResultsAiPersistenceStrategy.builder(strategy)
                    .persistToolResults(true)
                    .maxToolResultChars(maxToolResultChars)
                    .maxToolResults(maxToolResults)
                    .build();
        }

        return strategy;
    }
}
