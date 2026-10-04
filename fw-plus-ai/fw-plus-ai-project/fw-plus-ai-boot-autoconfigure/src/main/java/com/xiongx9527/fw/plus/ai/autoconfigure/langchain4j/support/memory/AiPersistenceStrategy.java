package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * AI 消息持久化策略（支持按消息粒度指定插入位置）。
 * <p>
 * 设计约定：
 * <ul>
 *   <li>持久化时自动过滤掉「无有效字符」的 AiMessage（{@link #hasEffectiveText(AiMessage)} 为 false）。</li>
 *   <li>默认不持久化 ToolExecutionRequest/ToolExecutionResultMessage。</li>
 *   <li>持久化发生在新一轮开始（收到新的 UserMessage）时。</li>
 * </ul>
 */
public interface AiPersistenceStrategy {

    /**
     * 从上一轮输出中筛选出需要持久化的消息项（含插入位置与优先级）。
     */
    default List<PersistedAiItem> selectForPersistenceItems(List<ChatMessage> previousTurnMessages) {
        List<ChatMessage> msgs = selectForPersistenceMessages(previousTurnMessages);
        return PersistedAiItem.fromMessages(msgs, AiPersistencePosition.AFTER_LAST_USER);
    }

    /**
     * 筛选消息列表（兼容用法）。
     */
    default List<ChatMessage> selectForPersistenceMessages(List<ChatMessage> previousTurnMessages) {
        return Collections.emptyList();
    }

    @Deprecated
    default List<ChatMessage> selectForPersistence(List<ChatMessage> previousTurnMessages) {
        return selectForPersistenceMessages(previousTurnMessages);
    }

    // ==================== 有效字符判断 ====================

    static boolean hasEffectiveText(AiMessage ai) {
        if (ai == null) {
            return false;
        }
        String text = ai.text();
        if (text == null) {
            return false;
        }
        // 去除所有空白字符
        String stripped = text.replaceAll("\\s+", "");
        return !stripped.isEmpty();
    }

    /**
     * 判断一段「上一轮输出」里是否发生过工具调用往返。
     *
     * <p>用于识别工具派生的结论——实时数据（天气、行情、库存）过期后仍留在上下文里，
     * 会让模型凭旧值直接作答、不再重新调用工具。</p>
     */
    static boolean containsToolRoundTrip(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return false;
        }
        for (ChatMessage m : messages) {
            if (m instanceof ToolExecutionResultMessage) {
                return true;
            }
            if (m instanceof AiMessage && ((AiMessage) m).hasToolExecutionRequests()) {
                return true;
            }
        }
        return false;
    }

    static List<ChatMessage> filterEffectiveAiOnly(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return messages;
        }
        List<ChatMessage> filtered = new ArrayList<>(messages.size());
        for (ChatMessage m : messages) {
            if (m instanceof AiMessage) {
                if (hasEffectiveText((AiMessage) m)) {
                    filtered.add(m);
                }
            } else {
                filtered.add(m);
            }
        }
        return filtered;
    }

    // ==================== 基础策略 ====================

    AiPersistenceStrategy DROP_ALL_AI = new AiPersistenceStrategy() {
        @Override
        public List<PersistedAiItem> selectForPersistenceItems(List<ChatMessage> previousTurnMessages) {
            return Collections.emptyList();
        }

        @Override
        public List<ChatMessage> selectForPersistenceMessages(List<ChatMessage> previousTurnMessages) {
            return Collections.emptyList();
        }
    };

    /**
     * 只保留最后一个最终回答（关键结论）——放到 TAIL（最靠近下一轮，时效性最高）。
     */
    AiPersistenceStrategy PERSIST_LAST_FINAL_ANSWER = new AiPersistenceStrategy() {
        @Override
        public List<PersistedAiItem> selectForPersistenceItems(List<ChatMessage> previousTurnMessages) {
            if (previousTurnMessages == null || previousTurnMessages.isEmpty()) {
                return Collections.emptyList();
            }
            for (int i = previousTurnMessages.size() - 1; i >= 0; i--) {
                ChatMessage m = previousTurnMessages.get(i);
                if (m instanceof AiMessage) {
                    AiMessage ai = (AiMessage) m;
                    if (!ai.hasToolExecutionRequests() && hasEffectiveText(ai)) {
                        return Collections.singletonList(
                                PersistedAiItem.builder(ai)
                                        .position(AiPersistencePosition.TAIL)
                                        .priority(100)
                                        .build()
                        );
                    }
                }
            }
            return Collections.emptyList();
        }
    };

    /**
     * <b>默认策略</b>：只沉淀「本轮没有调用工具」的最后一个最终回答（放到 TAIL）。
     *
     * <p>取舍说明——连续性与数据新鲜度天然冲突：</p>
     * <ul>
     *   <li>无条件沉淀最终回答（{@link #PERSIST_LAST_FINAL_ANSWER}）能保住上下文连续性，
     *       但工具派生的结论（天气、行情、库存）会变成「现成答案」，模型据此直接作答、
     *       不再重新调用工具，数据随之过期。</li>
     *   <li>本策略用 {@link #containsToolRoundTrip} 判定：上一轮用过工具就整轮不沉淀，
     *       实时数据每轮重查；纯聊天、只走 RAG 的问答照常保留上一轮结论，不丢连续性。</li>
     * </ul>
     */
    AiPersistenceStrategy PERSIST_NON_TOOL_FINAL_ANSWER = new AiPersistenceStrategy() {
        @Override
        public List<PersistedAiItem> selectForPersistenceItems(List<ChatMessage> previousTurnMessages) {
            if (previousTurnMessages == null || previousTurnMessages.isEmpty()) {
                return Collections.emptyList();
            }
            // 上一轮用过工具：其结论可能已过期，沉淀会让模型跳过工具调用
            if (containsToolRoundTrip(previousTurnMessages)) {
                return Collections.emptyList();
            }
            for (int i = previousTurnMessages.size() - 1; i >= 0; i--) {
                ChatMessage m = previousTurnMessages.get(i);
                if (m instanceof AiMessage) {
                    AiMessage ai = (AiMessage) m;
                    if (!ai.hasToolExecutionRequests() && hasEffectiveText(ai)) {
                        return Collections.singletonList(
                                PersistedAiItem.builder(ai)
                                        .position(AiPersistencePosition.TAIL)
                                        .priority(100)
                                        .build()
                        );
                    }
                }
            }
            return Collections.emptyList();
        }
    };

    /**
     * 所有最终回答——默认放到 AFTER_LAST_USER
     */
    AiPersistenceStrategy PERSIST_ALL_FINAL_ANSWERS = new AiPersistenceStrategy() {
        @Override
        public List<PersistedAiItem> selectForPersistenceItems(List<ChatMessage> previousTurnMessages) {
            if (previousTurnMessages == null || previousTurnMessages.isEmpty()) {
                return Collections.emptyList();
            }
            List<PersistedAiItem> items = new ArrayList<>();
            for (ChatMessage m : previousTurnMessages) {
                if (m instanceof AiMessage) {
                    AiMessage ai = (AiMessage) m;
                    if (!ai.hasToolExecutionRequests() && hasEffectiveText(ai)) {
                        items.add(PersistedAiItem.builder(ai)
                                .position(AiPersistencePosition.AFTER_LAST_USER)
                                .priority(0)
                                .build());
                    }
                }
            }
            return items;
        }
    };

    /**
     * 最终回答 + 非工具调用型 AI——最终回答放 TAIL，高优先级；其余非工具调用说明放 MIDDLE，优先级较低
     */
    AiPersistenceStrategy PERSIST_FINAL_ANSWER_AND_NON_TOOL_AI = new AiPersistenceStrategy() {
        @Override
        public List<PersistedAiItem> selectForPersistenceItems(List<ChatMessage> previousTurnMessages) {
            if (previousTurnMessages == null || previousTurnMessages.isEmpty()) {
                return Collections.emptyList();
            }
            List<PersistedAiItem> items = new ArrayList<>();
            for (int i = 0; i < previousTurnMessages.size(); i++) {
                ChatMessage m = previousTurnMessages.get(i);
                if (m instanceof AiMessage) {
                    AiMessage ai = (AiMessage) m;
                    if (!ai.hasToolExecutionRequests() && hasEffectiveText(ai)) {
                        boolean isLastFinal = true;
                        for (int j = i + 1; j < previousTurnMessages.size(); j++) {
                            ChatMessage nm = previousTurnMessages.get(j);
                            if (nm instanceof AiMessage) {
                                AiMessage nai = (AiMessage) nm;
                                if (!nai.hasToolExecutionRequests() && hasEffectiveText(nai)) {
                                    isLastFinal = false;
                                    break;
                                }
                            }
                        }
                        if (isLastFinal) {
                            items.add(PersistedAiItem.builder(ai)
                                    .position(AiPersistencePosition.TAIL)
                                    .priority(100)
                                    .build());
                        } else {
                            items.add(PersistedAiItem.builder(ai)
                                    .position(AiPersistencePosition.MIDDLE)
                                    .priority(-10)
                                    .build());
                        }
                    }
                }
            }
            return items;
        }
    };

    /**
     * 所有 AI（不含工具往返）——带工具调用请求的中间思考型 AI 放 MIDDLE；不带工具调用的最终型 AI 放 TAIL
     */
    AiPersistenceStrategy PERSIST_ALL_AI_DROP_TOOL_MESSAGES = new AiPersistenceStrategy() {
        @Override
        public List<PersistedAiItem> selectForPersistenceItems(List<ChatMessage> previousTurnMessages) {
            if (previousTurnMessages == null || previousTurnMessages.isEmpty()) {
                return Collections.emptyList();
            }
            List<PersistedAiItem> items = new ArrayList<>();
            for (ChatMessage m : previousTurnMessages) {
                if (m instanceof AiMessage) {
                    AiMessage ai = (AiMessage) m;
                    if (!hasEffectiveText(ai)) {
                        continue;
                    }
                    if (!ai.hasToolExecutionRequests()) {
                        items.add(PersistedAiItem.builder(ai)
                                .position(AiPersistencePosition.TAIL)
                                .priority(90)
                                .build());
                    } else {
                        items.add(PersistedAiItem.builder(ai)
                                .position(AiPersistencePosition.MIDDLE)
                                .priority(-20)
                                .build());
                    }
                }
            }
            return items;
        }
    };

    /**
     * 只保留最后一条 AI
     */
    AiPersistenceStrategy PERSIST_LAST_AI_ONLY = new AiPersistenceStrategy() {
        @Override
        public List<PersistedAiItem> selectForPersistenceItems(List<ChatMessage> previousTurnMessages) {
            if (previousTurnMessages == null || previousTurnMessages.isEmpty()) {
                return Collections.emptyList();
            }
            for (int i = previousTurnMessages.size() - 1; i >= 0; i--) {
                if (previousTurnMessages.get(i) instanceof AiMessage) {
                    AiMessage ai = (AiMessage) previousTurnMessages.get(i);
                    if (!hasEffectiveText(ai)) {
                        continue;
                    }
                    if (!ai.hasToolExecutionRequests()) {
                        return Collections.singletonList(
                                PersistedAiItem.builder(ai)
                                        .position(AiPersistencePosition.TAIL)
                                        .priority(100)
                                        .build()
                        );
                    } else {
                        return Collections.singletonList(
                                PersistedAiItem.builder(ai)
                                        .position(AiPersistencePosition.MIDDLE)
                                        .priority(-15)
                                        .build()
                        );
                    }
                }
            }
            return Collections.emptyList();
        }
    };

    /**
     * 按结论性标记保留 AI
     */
    AiPersistenceStrategy PERSIST_CONCLUSION_MARKED_AI = new AiPersistenceStrategy() {
        @Override
        public List<PersistedAiItem> selectForPersistenceItems(List<ChatMessage> previousTurnMessages) {
            if (previousTurnMessages == null || previousTurnMessages.isEmpty()) {
                return Collections.emptyList();
            }
            List<PersistedAiItem> items = new ArrayList<>();
            for (ChatMessage m : previousTurnMessages) {
                if (!(m instanceof AiMessage)) {
                    continue;
                }
                AiMessage ai = (AiMessage) m;
                if (!hasEffectiveText(ai)) {
                    continue;
                }
                String text = ai.text();
                String normalized = text.trim();
                boolean isConclusion = !ai.hasToolExecutionRequests();
                if (!isConclusion) {
                    if (CONCLUSION_PATTERN.matcher(normalized).find()
                            || normalized.startsWith("#")
                            || normalized.contains("```")
                            || LINE_LIST_PATTERN.matcher(normalized).find()) {
                        isConclusion = true;
                    }
                }
                if (isConclusion) {
                    items.add(PersistedAiItem.builder(ai)
                            .position(AiPersistencePosition.TAIL)
                            .priority(95)
                            .build());
                } else {
                    items.add(PersistedAiItem.builder(ai)
                            .position(AiPersistencePosition.MIDDLE)
                            .priority(-25)
                            .build());
                }
            }
            return items;
        }
    };

    /**
     * 最终回答 + 带结论标记的中间 AI：最终回答放 TAIL；带结论标记的中间 AI 放 AFTER_LAST_USER
     */
    AiPersistenceStrategy PERSIST_FINAL_ANSWERS_PLUS_MARKED_INTERMEDIATES = new AiPersistenceStrategy() {
        @Override
        public List<PersistedAiItem> selectForPersistenceItems(List<ChatMessage> previousTurnMessages) {
            if (previousTurnMessages == null || previousTurnMessages.isEmpty()) {
                return Collections.emptyList();
            }
            List<PersistedAiItem> items = new ArrayList<>();
            for (ChatMessage m : previousTurnMessages) {
                if (!(m instanceof AiMessage)) {
                    continue;
                }
                AiMessage ai = (AiMessage) m;
                if (!hasEffectiveText(ai)) {
                    continue;
                }
                if (!ai.hasToolExecutionRequests()) {
                    items.add(PersistedAiItem.builder(ai)
                            .position(AiPersistencePosition.TAIL)
                            .priority(100)
                            .build());
                    continue;
                }
                String text = ai.text();
                String normalized = text.trim();
                if (CONCLUSION_PATTERN.matcher(normalized).find()
                        || normalized.startsWith("#")
                        || normalized.contains("```")
                        || LINE_LIST_PATTERN.matcher(normalized).find()) {
                    items.add(PersistedAiItem.builder(ai)
                            .position(AiPersistencePosition.AFTER_LAST_USER)
                            .priority(50)
                            .build());
                }
            }
            return items;
        }
    };

    AiPersistenceStrategy PERSIST_CONCLUSION_AI_ONLY = PERSIST_LAST_FINAL_ANSWER;

    // ==================== 正则 ====================

    Pattern CONCLUSION_PATTERN = Pattern.compile(
            "^(?:\\s*(?:#{1,6})?\\s*)?(?:结论|总结|最终结论|最终总结|结论如下|总结如下|建议|建议如下|决定|方案|方案如下|final\\s+answer|conclusion|summary|answer)[:：\\s]",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    Pattern LINE_LIST_PATTERN = Pattern.compile("(?m)^\\s*(?:[-*+]\\s+|\\d+\\.\\s+).+");

    // ==================== Registry ====================

    final class Registry {
        private static final Map<String, AiPersistenceStrategy> STRATEGIES = new ConcurrentHashMap<>();

        static {
            register("DROP_ALL_AI", DROP_ALL_AI);
            register("PERSIST_NON_TOOL_FINAL_ANSWER", PERSIST_NON_TOOL_FINAL_ANSWER);
            register("PERSIST_LAST_FINAL_ANSWER", PERSIST_LAST_FINAL_ANSWER);
            register("PERSIST_ALL_FINAL_ANSWERS", PERSIST_ALL_FINAL_ANSWERS);
            register("PERSIST_FINAL_ANSWER_AND_NON_TOOL_AI", PERSIST_FINAL_ANSWER_AND_NON_TOOL_AI);
            register("PERSIST_ALL_AI_DROP_TOOL_MESSAGES", PERSIST_ALL_AI_DROP_TOOL_MESSAGES);
            register("PERSIST_LAST_AI_ONLY", PERSIST_LAST_AI_ONLY);
            register("PERSIST_CONCLUSION_MARKED_AI", PERSIST_CONCLUSION_MARKED_AI);
            register("PERSIST_FINAL_ANSWERS_PLUS_MARKED_INTERMEDIATES", PERSIST_FINAL_ANSWERS_PLUS_MARKED_INTERMEDIATES);
            register("PERSIST_CONCLUSION_AI_ONLY", PERSIST_CONCLUSION_AI_ONLY);
        }

        private Registry() {
        }

        public static void register(String name, AiPersistenceStrategy strategy) {
            if (name == null || name.isEmpty() || strategy == null) {
                return;
            }
            STRATEGIES.put(name.trim(), strategy);
        }

        public static void registerAll(Map<String, AiPersistenceStrategy> map) {
            if (map == null || map.isEmpty()) {
                return;
            }
            map.forEach(Registry::register);
        }

        public static AiPersistenceStrategy get(String name) {
            if (name == null || name.isEmpty()) {
                return null;
            }
            return STRATEGIES.get(name.trim());
        }

        /**
         * 尝试解析策略：命中注册名或可按类名实例化时返回实例，否则返回 {@code null}（不做默认回退）。
         */
        public static AiPersistenceStrategy tryResolve(String nameOrClass) {
            if (nameOrClass == null || nameOrClass.isEmpty()) {
                return null;
            }
            String key = nameOrClass.trim();
            AiPersistenceStrategy registered = get(key);
            if (registered != null) {
                return registered;
            }
            try {
                Class<?> clazz = Class.forName(key);
                if (AiPersistenceStrategy.class.isAssignableFrom(clazz)) {
                    Constructor<?> ctor = clazz.getDeclaredConstructor();
                    if (!ctor.isAccessible()) {
                        ctor.setAccessible(true);
                    }
                    return (AiPersistenceStrategy) ctor.newInstance();
                }
            } catch (Exception ignored) {
            }
            return null;
        }

        public static AiPersistenceStrategy resolve(String nameOrClass) {
            AiPersistenceStrategy resolved = tryResolve(nameOrClass);
            return resolved == null ? PERSIST_NON_TOOL_FINAL_ANSWER : resolved;
        }

        public static void clearCustom() {
            STRATEGIES.entrySet().removeIf(e -> {
                String k = e.getKey();
                return !"DROP_ALL_AI".equals(k)
                        && !"PERSIST_NON_TOOL_FINAL_ANSWER".equals(k)
                        && !"PERSIST_LAST_FINAL_ANSWER".equals(k)
                        && !"PERSIST_ALL_FINAL_ANSWERS".equals(k)
                        && !"PERSIST_FINAL_ANSWER_AND_NON_TOOL_AI".equals(k)
                        && !"PERSIST_ALL_AI_DROP_TOOL_MESSAGES".equals(k)
                        && !"PERSIST_LAST_AI_ONLY".equals(k)
                        && !"PERSIST_CONCLUSION_MARKED_AI".equals(k)
                        && !"PERSIST_FINAL_ANSWERS_PLUS_MARKED_INTERMEDIATES".equals(k)
                        && !"PERSIST_CONCLUSION_AI_ONLY".equals(k);
            });
        }
    }
}
