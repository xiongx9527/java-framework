package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 可配置的 Ephemeral ChatMemory 装饰器。
 *
 * <p>核心设计：</p>
 * <ul>
 *   <li><b>持久化时机</b>：在收到新的 {@link UserMessage}（新一轮开始）时，从 {@link ChatMemoryStore}
 *       取出当前消息，构建「持久历史」并写回。持久历史 = 系统提示 + 历史用户原话 +
 *       按 {@link AiPersistenceStrategy} 选出的「上一轮」输出。</li>
 *   <li><b>无有效字符过滤</b>：只有包含有效字符（非纯空白）的 AI 消息才会持久化，其余不带到下一轮。</li>
 *   <li><b>位置控制</b>：策略可为每条消息指定 {@link AiPersistencePosition}，实现「关键放前尾、不重要放中间」。</li>
 *   <li><b>默认不放</b>：RAG 检索段、工具调用往返默认只存在于本轮请求窗口，不写入持久历史。</li>
 * </ul>
 *
 * <p><b>不持有跨轮实例状态</b>：实例按 memoryId 复用，所有决策只基于 store 中的消息顺序。</p>
 */
public class EphemeralChatMemory implements ChatMemory {

    /**
     * 默认的检索内容注入分隔串。
     * <p>与框架默认 injector 模板的分隔段一致：注入结果即「用户原话 + 本分隔串 + 检索内容」。</p>
     */
    public static final String DEFAULT_RAG_SEPARATOR = "\n\nAnswer using the following information:\n";

    private final ChatMemory delegate;
    private final ChatMemoryStore store;
    private final AiPersistenceStrategy aiPersistenceStrategy;
    private final List<String> ephemeralSeparators;
    private final boolean stripRagFromHistory;
    private final boolean includeCurrentUserWithEphemeral;
    private final boolean sendToolResultsInCurrentTurn;
    private final boolean sendToolRequestsInCurrentTurn;

    private EphemeralChatMemory(Builder builder) {
        this.delegate = builder.delegate;
        this.store = builder.store;
        this.aiPersistenceStrategy = builder.aiPersistenceStrategy;
        this.ephemeralSeparators = builder.ephemeralSeparators;
        this.stripRagFromHistory = builder.stripRagFromHistory;
        this.includeCurrentUserWithEphemeral = builder.includeCurrentUserWithEphemeral;
        this.sendToolResultsInCurrentTurn = builder.sendToolResultsInCurrentTurn;
        this.sendToolRequestsInCurrentTurn = builder.sendToolRequestsInCurrentTurn;
    }

    public static Builder builder(ChatMemory delegate, ChatMemoryStore store) {
        return new Builder(delegate, store);
    }

    @Override
    public Object id() {
        return delegate.id();
    }

    @Override
    public void add(ChatMessage message) {
        if (message.type() == ChatMessageType.USER) {
            // 新一轮开始：先固化「上一轮」输出到持久历史，再追加当前用户消息。
            // 必须在 delegate.add 之前做，delegate 会基于清理过的 store 追加。
            List<ChatMessage> currentStored = store.getMessages(id());
            store.updateMessages(id(), buildDurableHistory(currentStored));
        }
        delegate.add(message);
    }

    @Override
    public List<ChatMessage> messages() {
        List<ChatMessage> stored = delegate.messages();
        if (stored.isEmpty()) {
            return stored;
        }

        int lastUserIndex = lastUserIndex(stored);

        List<ChatMessage> sendable = new ArrayList<>(stored.size());
        for (int i = 0; i < stored.size(); i++) {
            ChatMessage message = stored.get(i);
            if (i < lastUserIndex) {
                // 历史轮次：系统提示、用户原话、以及被策略显式保留下来的上一轮输出（AI）
                if (message.type() == ChatMessageType.USER) {
                    sendable.add(stripRagFromHistory ? stripRag((UserMessage) message) : message);
                } else {
                    // SYSTEM 与「按策略沉淀下来的 AI」原样回放
                    sendable.add(message);
                }
            } else {
                // 当前轮
                if (message.type() == ChatMessageType.USER) {
                    sendable.add(includeCurrentUserWithEphemeral ? message : stripRag((UserMessage) message));
                } else if (message.type() == ChatMessageType.AI) {
                    AiMessage ai = (AiMessage) message;
                    if (ai.hasToolExecutionRequests()) {
                        // 工具调用中间步：本轮需要发出
                        if (sendToolRequestsInCurrentTurn) {
                            sendable.add(message);
                        }
                    }
                    // 非工具调用的最终回答：只是本轮输出，不回放进请求上下文
                } else if (message.type() == ChatMessageType.TOOL_EXECUTION_RESULT) {
                    if (sendToolResultsInCurrentTurn) {
                        sendable.add(message);
                    }
                } else {
                    sendable.add(message);
                }
            }
        }
        return sendable;
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    /**
     * 构建持久历史：系统提示 + 历史用户原话 + 按策略与位置插入的上一轮输出。
     */
    private List<ChatMessage> buildDurableHistory(List<ChatMessage> stored) {
        if (stored == null || stored.isEmpty()) {
            return stored == null ? new ArrayList<>() : stored;
        }

        int lastUserIndex = lastUserIndex(stored);

        List<ChatMessage> systemList = new ArrayList<>();
        List<ChatMessage> historyUserList = new ArrayList<>();
        List<ChatMessage> previousTurnOutputs = new ArrayList<>();

        // 此时 store 里还没有「本轮用户消息」，所以：
        //   - 所有 USER 都是历史轮次的原话；
        //   - 位于「最后一条历史用户消息」之后的 AI / 工具往返属于上一轮的输出，交给策略筛选；
        //   - 更早的 AI / 工具往返在其自身轮次的边界已处理过，丢弃。
        // 注意：MessageWindowChatMemory 在系统提示变更时会把新 SystemMessage 追加到列表末尾
        // （即可能落在最后一条用户消息之后），因此 SystemMessage 必须无条件保留，不能按位置丢弃。
        for (int i = 0; i < stored.size(); i++) {
            ChatMessage message = stored.get(i);
            if (message.type() == ChatMessageType.SYSTEM) {
                systemList.add(message);
            } else if (message.type() == ChatMessageType.USER) {
                historyUserList.add(stripRagFromHistory ? stripRag((UserMessage) message) : message);
            } else if (i > lastUserIndex) {
                previousTurnOutputs.add(message);
            }
        }

        // 按策略选出需要持久化的消息项（含位置与优先级）
        List<PersistedAiItem> selected = Collections.emptyList();
        if (!previousTurnOutputs.isEmpty() && aiPersistenceStrategy != null) {
            List<PersistedAiItem> items = aiPersistenceStrategy.selectForPersistenceItems(previousTurnOutputs);
            if (items != null && !items.isEmpty()) {
                selected = items;
            }
        }

        // 按位置分组（过滤无有效字符的 AI）
        List<PersistedAiItem> head = new ArrayList<>();
        List<PersistedAiItem> afterSystem = new ArrayList<>();
        List<PersistedAiItem> beforeFirstUser = new ArrayList<>();
        List<PersistedAiItem> middle = new ArrayList<>();
        List<PersistedAiItem> beforeLastUser = new ArrayList<>();
        List<PersistedAiItem> afterLastUser = new ArrayList<>();
        List<PersistedAiItem> tail = new ArrayList<>();

        for (PersistedAiItem item : selected) {
            if (item == null || item.message() == null) {
                continue;
            }
            ChatMessage m = item.message();
            if (m instanceof AiMessage && !AiPersistenceStrategy.hasEffectiveText((AiMessage) m)) {
                continue;
            }
            switch (item.position()) {
                case HEAD:
                    head.add(item);
                    break;
                case AFTER_SYSTEM:
                    afterSystem.add(item);
                    break;
                case BEFORE_FIRST_USER:
                    beforeFirstUser.add(item);
                    break;
                case MIDDLE:
                    middle.add(item);
                    break;
                case BEFORE_LAST_USER:
                    beforeLastUser.add(item);
                    break;
                case TAIL:
                    tail.add(item);
                    break;
                case AFTER_LAST_USER:
                default:
                    afterLastUser.add(item);
                    break;
            }
        }

        sortByPriority(head);
        sortByPriority(afterSystem);
        sortByPriority(beforeFirstUser);
        sortByPriority(middle);
        sortByPriority(beforeLastUser);
        sortByPriority(afterLastUser);
        sortByPriority(tail);

        List<ChatMessage> durable = new ArrayList<>(
                systemList.size() + historyUserList.size() + selected.size() + 4);

        // HEAD
        append(durable, head);
        // System
        durable.addAll(systemList);
        // AFTER_SYSTEM
        append(durable, afterSystem);
        // BEFORE_FIRST_USER
        append(durable, beforeFirstUser);

        // 历史用户块：MIDDLE 插中段，BEFORE_LAST_USER 插在最后一条用户之前
        int userCount = historyUserList.size();
        if (userCount == 0) {
            append(durable, middle);
            append(durable, beforeLastUser);
        } else if (userCount == 1) {
            append(durable, middle);
            append(durable, beforeLastUser);
            durable.add(historyUserList.get(0));
        } else {
            int mid = userCount / 2;
            if (mid < 1) {
                mid = 1;
            }
            if (mid > userCount - 1) {
                mid = userCount - 1;
            }
            durable.addAll(historyUserList.subList(0, mid));
            append(durable, middle);
            durable.addAll(historyUserList.subList(mid, userCount - 1));
            append(durable, beforeLastUser);
            durable.add(historyUserList.get(userCount - 1));
        }

        // AFTER_LAST_USER
        append(durable, afterLastUser);
        // TAIL（最靠近下一轮，时效性最高）
        append(durable, tail);

        return durable;
    }

    private static void append(List<ChatMessage> target, List<PersistedAiItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (PersistedAiItem item : items) {
            if (item != null && item.message() != null) {
                target.add(item.message());
            }
        }
    }

    /** 同位置内按优先级降序；优先级相同保持策略返回的原始顺序（稳定排序）。 */
    private static void sortByPriority(List<PersistedAiItem> items) {
        if (items == null || items.size() < 2) {
            return;
        }
        items.sort(Comparator.comparingInt(PersistedAiItem::priority).reversed());
    }

    private static int lastUserIndex(List<ChatMessage> stored) {
        for (int i = stored.size() - 1; i >= 0; i--) {
            if (stored.get(i).type() == ChatMessageType.USER) {
                return i;
            }
        }
        return -1;
    }

    /** 去掉 user message 尾部由 injector 拼入的临时检索段。 */
    private UserMessage stripRag(UserMessage message) {
        String text = message.singleText();
        if (text == null || text.isEmpty()) {
            return message;
        }
        for (String sep : ephemeralSeparators) {
            if (sep == null || sep.isEmpty()) {
                continue;
            }
            int anchor = text.indexOf(sep);
            if (anchor >= 0) {
                return UserMessage.from(text.substring(0, anchor));
            }
        }
        return message;
    }

    public static class Builder {
        private ChatMemory delegate;
        private ChatMemoryStore store;
        private AiPersistenceStrategy aiPersistenceStrategy = AiPersistenceStrategy.PERSIST_NON_TOOL_FINAL_ANSWER;
        private List<String> ephemeralSeparators = Collections.singletonList(DEFAULT_RAG_SEPARATOR);
        private boolean stripRagFromHistory = true;
        private boolean includeCurrentUserWithEphemeral = true;
        private boolean sendToolResultsInCurrentTurn = true;
        private boolean sendToolRequestsInCurrentTurn = true;

        private Builder(ChatMemory delegate, ChatMemoryStore store) {
            this.delegate = delegate;
            this.store = store;
        }

        public Builder delegate(ChatMemory delegate) {
            this.delegate = delegate;
            return this;
        }

        public Builder store(ChatMemoryStore store) {
            this.store = store;
            return this;
        }

        /**
         * 设置 AI 持久化策略（对象实例，优先级最高：对象 &gt; Key &gt; 默认）。
         */
        public Builder aiPersistenceStrategy(AiPersistenceStrategy aiPersistenceStrategy) {
            this.aiPersistenceStrategy = aiPersistenceStrategy == null
                    ? AiPersistenceStrategy.PERSIST_NON_TOOL_FINAL_ANSWER
                    : aiPersistenceStrategy;
            return this;
        }

        /**
         * 通过名称或全限定类名设置 AI 持久化策略。
         * <p>支持内置策略名、已注册的自定义策略名、全限定类名（需实现 {@link AiPersistenceStrategy} 且有无参构造器）。</p>
         */
        public Builder aiPersistenceStrategy(String nameOrClass) {
            this.aiPersistenceStrategy = AiPersistenceStrategy.Registry.resolve(nameOrClass);
            return this;
        }

        /**
         * 注册自定义策略并按名称使用。
         */
        public Builder registerAndUseAiPersistenceStrategy(String name, AiPersistenceStrategy strategy) {
            AiPersistenceStrategy.Registry.register(name, strategy);
            AiPersistenceStrategy resolved = AiPersistenceStrategy.Registry.get(name);
            this.aiPersistenceStrategy = resolved == null ? strategy : resolved;
            return this;
        }

        public Builder ephemeralSeparators(List<String> ephemeralSeparators) {
            if (ephemeralSeparators != null && !ephemeralSeparators.isEmpty()) {
                this.ephemeralSeparators = new ArrayList<>(ephemeralSeparators);
            }
            return this;
        }

        public Builder ephemeralSeparators(String... separators) {
            if (separators != null && separators.length > 0) {
                this.ephemeralSeparators = Arrays.asList(separators);
            }
            return this;
        }

        /** 历史用户消息是否剥离检索段。默认 true。 */
        public Builder stripRagFromHistory(boolean stripRagFromHistory) {
            this.stripRagFromHistory = stripRagFromHistory;
            return this;
        }

        /** 当前轮用户消息是否保留检索段（本轮推理需要）。默认 true。 */
        public Builder includeCurrentUserWithEphemeral(boolean includeCurrentUserWithEphemeral) {
            this.includeCurrentUserWithEphemeral = includeCurrentUserWithEphemeral;
            return this;
        }

        /** 当前轮是否发送工具执行结果。默认 true。 */
        public Builder sendToolResultsInCurrentTurn(boolean sendToolResultsInCurrentTurn) {
            this.sendToolResultsInCurrentTurn = sendToolResultsInCurrentTurn;
            return this;
        }

        /** 当前轮是否发送工具调用中间步。默认 true。 */
        public Builder sendToolRequestsInCurrentTurn(boolean sendToolRequestsInCurrentTurn) {
            this.sendToolRequestsInCurrentTurn = sendToolRequestsInCurrentTurn;
            return this;
        }

        public EphemeralChatMemory build() {
            if (delegate == null || store == null) {
                throw new IllegalStateException("EphemeralChatMemory.Builder: delegate 与 store 不能为空");
            }
            return new EphemeralChatMemory(this);
        }
    }
}
