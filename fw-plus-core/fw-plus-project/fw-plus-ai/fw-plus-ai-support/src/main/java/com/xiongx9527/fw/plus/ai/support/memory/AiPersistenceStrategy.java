package com.xiongx9527.fw.plus.ai.support.memory;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.ChatRole;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * AI 消息持久化策略（支持按消息粒度指定插入位置）。
 *
 * <p>设计约定：</p>
 * <ul>
 *   <li>持久化时自动过滤掉「无有效字符」的助手消息（{@link #hasEffectiveText(ChatMessage)} 为 false）。</li>
 *   <li>默认不持久化工具调用请求 / 工具执行结果消息。</li>
 *   <li>持久化发生在新一轮开始（收到新的用户消息）时。</li>
 * </ul>
 */
public interface AiPersistenceStrategy {

    default List<PersistedAiItem> selectForPersistenceItems(List<ChatMessage> previousTurnMessages) {
        List<ChatMessage> msgs = selectForPersistenceMessages(previousTurnMessages);
        return PersistedAiItem.fromMessages(msgs, AiPersistencePosition.AFTER_LAST_USER);
    }

    default List<ChatMessage> selectForPersistenceMessages(List<ChatMessage> previousTurnMessages) {
        return Collections.emptyList();
    }

    // ==================== 有效字符判断 ====================

    static boolean isAssistant(ChatMessage message) {
        return message != null && message.getRole() == ChatRole.ASSISTANT;
    }

    static boolean hasEffectiveText(ChatMessage ai) {
        if (!isAssistant(ai)) {
            return false;
        }
        String text = ai.getContent();
        if (text == null) {
            return false;
        }
        String stripped = text.replaceAll("\\s+", "");
        return !stripped.isEmpty();
    }

    static boolean containsToolRoundTrip(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return false;
        }
        for (ChatMessage m : messages) {
            if (m.getRole() == ChatRole.TOOL) {
                return true;
            }
            if (isAssistant(m) && m.hasToolCalls()) {
                return true;
            }
        }
        return false;
    }

    static List<ChatMessage> filterEffectiveAiOnly(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return messages;
        }
        List<ChatMessage> filtered = new ArrayList<ChatMessage>(messages.size());
        for (ChatMessage m : messages) {
            if (isAssistant(m)) {
                if (hasEffectiveText(m)) {
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

    AiPersistenceStrategy PERSIST_LAST_FINAL_ANSWER = new AiPersistenceStrategy() {
        @Override
        public List<PersistedAiItem> selectForPersistenceItems(List<ChatMessage> previousTurnMessages) {
            if (previousTurnMessages == null || previousTurnMessages.isEmpty()) {
                return Collections.emptyList();
            }
            for (int i = previousTurnMessages.size() - 1; i >= 0; i--) {
                ChatMessage m = previousTurnMessages.get(i);
                if (isAssistant(m) && !m.hasToolCalls() && hasEffectiveText(m)) {
                    return Collections.singletonList(
                            PersistedAiItem.builder(m)
                                    .position(AiPersistencePosition.TAIL)
                                    .priority(100)
                                    .build());
                }
            }
            return Collections.emptyList();
        }
    };

    AiPersistenceStrategy PERSIST_NON_TOOL_FINAL_ANSWER = new AiPersistenceStrategy() {
        @Override
        public List<PersistedAiItem> selectForPersistenceItems(List<ChatMessage> previousTurnMessages) {
            if (previousTurnMessages == null || previousTurnMessages.isEmpty()) {
                return Collections.emptyList();
            }
            if (containsToolRoundTrip(previousTurnMessages)) {
                return Collections.emptyList();
            }
            for (int i = previousTurnMessages.size() - 1; i >= 0; i--) {
                ChatMessage m = previousTurnMessages.get(i);
                if (isAssistant(m) && !m.hasToolCalls() && hasEffectiveText(m)) {
                    return Collections.singletonList(
                            PersistedAiItem.builder(m)
                                    .position(AiPersistencePosition.TAIL)
                                    .priority(100)
                                    .build());
                }
            }
            return Collections.emptyList();
        }
    };

    AiPersistenceStrategy PERSIST_ALL_FINAL_ANSWERS = new AiPersistenceStrategy() {
        @Override
        public List<PersistedAiItem> selectForPersistenceItems(List<ChatMessage> previousTurnMessages) {
            if (previousTurnMessages == null || previousTurnMessages.isEmpty()) {
                return Collections.emptyList();
            }
            List<PersistedAiItem> items = new ArrayList<PersistedAiItem>();
            for (ChatMessage m : previousTurnMessages) {
                if (isAssistant(m) && !m.hasToolCalls() && hasEffectiveText(m)) {
                    items.add(PersistedAiItem.builder(m)
                            .position(AiPersistencePosition.AFTER_LAST_USER)
                            .priority(0)
                            .build());
                }
            }
            return items;
        }
    };

    AiPersistenceStrategy PERSIST_FINAL_ANSWER_AND_NON_TOOL_AI = new AiPersistenceStrategy() {
        @Override
        public List<PersistedAiItem> selectForPersistenceItems(List<ChatMessage> previousTurnMessages) {
            if (previousTurnMessages == null || previousTurnMessages.isEmpty()) {
                return Collections.emptyList();
            }
            List<PersistedAiItem> items = new ArrayList<PersistedAiItem>();
            for (int i = 0; i < previousTurnMessages.size(); i++) {
                ChatMessage m = previousTurnMessages.get(i);
                if (isAssistant(m) && !m.hasToolCalls() && hasEffectiveText(m)) {
                    boolean isLastFinal = true;
                    for (int j = i + 1; j < previousTurnMessages.size(); j++) {
                        ChatMessage nm = previousTurnMessages.get(j);
                        if (isAssistant(nm) && !nm.hasToolCalls() && hasEffectiveText(nm)) {
                            isLastFinal = false;
                            break;
                        }
                    }
                    if (isLastFinal) {
                        items.add(PersistedAiItem.builder(m)
                                .position(AiPersistencePosition.TAIL)
                                .priority(100)
                                .build());
                    } else {
                        items.add(PersistedAiItem.builder(m)
                                .position(AiPersistencePosition.MIDDLE)
                                .priority(-10)
                                .build());
                    }
                }
            }
            return items;
        }
    };

    AiPersistenceStrategy PERSIST_ALL_AI_DROP_TOOL_MESSAGES = new AiPersistenceStrategy() {
        @Override
        public List<PersistedAiItem> selectForPersistenceItems(List<ChatMessage> previousTurnMessages) {
            if (previousTurnMessages == null || previousTurnMessages.isEmpty()) {
                return Collections.emptyList();
            }
            List<PersistedAiItem> items = new ArrayList<PersistedAiItem>();
            for (ChatMessage m : previousTurnMessages) {
                if (!hasEffectiveText(m)) {
                    continue;
                }
                if (!m.hasToolCalls()) {
                    items.add(PersistedAiItem.builder(m)
                            .position(AiPersistencePosition.TAIL)
                            .priority(90)
                            .build());
                } else {
                    items.add(PersistedAiItem.builder(m)
                            .position(AiPersistencePosition.MIDDLE)
                            .priority(-20)
                            .build());
                }
            }
            return items;
        }
    };

    AiPersistenceStrategy PERSIST_LAST_AI_ONLY = new AiPersistenceStrategy() {
        @Override
        public List<PersistedAiItem> selectForPersistenceItems(List<ChatMessage> previousTurnMessages) {
            if (previousTurnMessages == null || previousTurnMessages.isEmpty()) {
                return Collections.emptyList();
            }
            for (int i = previousTurnMessages.size() - 1; i >= 0; i--) {
                ChatMessage m = previousTurnMessages.get(i);
                if (!hasEffectiveText(m)) {
                    continue;
                }
                if (!m.hasToolCalls()) {
                    return Collections.singletonList(
                            PersistedAiItem.builder(m)
                                    .position(AiPersistencePosition.TAIL)
                                    .priority(100)
                                    .build());
                } else {
                    return Collections.singletonList(
                            PersistedAiItem.builder(m)
                                    .position(AiPersistencePosition.MIDDLE)
                                    .priority(-15)
                                    .build());
                }
            }
            return Collections.emptyList();
        }
    };

    AiPersistenceStrategy PERSIST_CONCLUSION_MARKED_AI = new AiPersistenceStrategy() {
        @Override
        public List<PersistedAiItem> selectForPersistenceItems(List<ChatMessage> previousTurnMessages) {
            if (previousTurnMessages == null || previousTurnMessages.isEmpty()) {
                return Collections.emptyList();
            }
            List<PersistedAiItem> items = new ArrayList<PersistedAiItem>();
            for (ChatMessage m : previousTurnMessages) {
                if (!isAssistant(m) || !hasEffectiveText(m)) {
                    continue;
                }
                String text = m.getContent();
                String normalized = text.trim();
                boolean isConclusion = !m.hasToolCalls();
                if (!isConclusion) {
                    if (CONCLUSION_PATTERN.matcher(normalized).find()
                            || normalized.startsWith("#")
                            || normalized.contains("\u0060\u0060\u0060")
                            || LINE_LIST_PATTERN.matcher(normalized).find()) {
                        isConclusion = true;
                    }
                }
                if (isConclusion) {
                    items.add(PersistedAiItem.builder(m)
                            .position(AiPersistencePosition.TAIL)
                            .priority(95)
                            .build());
                } else {
                    items.add(PersistedAiItem.builder(m)
                            .position(AiPersistencePosition.MIDDLE)
                            .priority(-25)
                            .build());
                }
            }
            return items;
        }
    };

    AiPersistenceStrategy PERSIST_FINAL_ANSWERS_PLUS_MARKED_INTERMEDIATES = new AiPersistenceStrategy() {
        @Override
        public List<PersistedAiItem> selectForPersistenceItems(List<ChatMessage> previousTurnMessages) {
            if (previousTurnMessages == null || previousTurnMessages.isEmpty()) {
                return Collections.emptyList();
            }
            List<PersistedAiItem> items = new ArrayList<PersistedAiItem>();
            for (ChatMessage m : previousTurnMessages) {
                if (!isAssistant(m) || !hasEffectiveText(m)) {
                    continue;
                }
                if (!m.hasToolCalls()) {
                    items.add(PersistedAiItem.builder(m)
                            .position(AiPersistencePosition.TAIL)
                            .priority(100)
                            .build());
                    continue;
                }
                String text = m.getContent();
                String normalized = text.trim();
                if (CONCLUSION_PATTERN.matcher(normalized).find()
                        || normalized.startsWith("#")
                        || normalized.contains("\u0060\u0060\u0060")
                        || LINE_LIST_PATTERN.matcher(normalized).find()) {
                    items.add(PersistedAiItem.builder(m)
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
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    Pattern LINE_LIST_PATTERN = Pattern.compile("(?m)^\\s*(?:[-*+]\\s+|\\d+\\.\\s+).+");

    // ==================== Registry ====================

    final class Registry {
        private static final Map<String, AiPersistenceStrategy> STRATEGIES = new ConcurrentHashMap<String, AiPersistenceStrategy>();

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
                // 类名无法实例化时静默回退
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
