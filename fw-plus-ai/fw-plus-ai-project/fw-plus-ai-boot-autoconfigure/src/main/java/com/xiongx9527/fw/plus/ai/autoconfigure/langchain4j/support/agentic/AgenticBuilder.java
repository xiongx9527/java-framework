package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent.IntentClassifier;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent.IntentRag;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent.IntentSupplier;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent.IntentTools;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.AiPersistenceStrategy;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.EphemeralChatMemory;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.EphemeralChatMemoryProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.LengthBoundedAiPersistenceStrategy;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.PersistToolResultsAiPersistenceStrategy;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.agent.AgentBuilder;
import dev.langchain4j.agentic.agent.UntypedAgentBuilder;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.scope.AgenticScopePersister;
import dev.langchain4j.agentic.scope.AgenticScopeStore;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 单 agent 构建器（隔离层）：对标 {@code AssistantBuilder} 的「静态工厂 → 流式累加 → build() 产出」
 * 流程，用于构建<b>一个</b> agent——封装 {@link AgenticServices#agentBuilder()}
 * 与 {@link AgenticServices#agentBuilder(Class)}。
 *
 * <p>一个泛型类同时覆盖 typed 与 untyped 两条路径
 * （上游 {@code UntypedAgentBuilder extends AgentBuilder<UntypedAgent, UntypedAgentBuilder>}）：</p>
 * <pre>{@code
 * // typed：传入带 @Agent 注解的接口，产物为该接口代理
 * EchoAgent echo = AgenticBuilder.agent(EchoAgent.class)
 *         .chatModel(model).systemMessage("你是回显助手").tools(svc).outputKey("out").build();
 *
 * // untyped：产物为 UntypedAgent 句柄
 * UntypedAgent agent = AgenticBuilder.agent()
 *         .chatModel(model).returnType(String.class).inputKey(String.class, "question")
 *         .userMessage("回答：{{question}}").outputKey("answer").build();
 * }</pre>
 *
 * <p>能力面与 {@code AssistantBuilder} 对齐，分四块：</p>
 * <ul>
 *   <li><b>模型 / 提示 / 工具 / 检索</b>：{@code chatModel} / {@code streamingChatModel} /
 *       {@code systemMessage} / {@code userMessage} / {@code tools} / {@code toolProvider} /
 *       {@code contentRetriever} / {@code retrievalAugmentor} 等；</li>
 *   <li><b>会话记忆与存储</b>：{@code chatMemory} / {@code chatMemoryProvider} /
 *       {@code memory(Integer)} / {@code memoryStore(ChatMemoryStore)} / {@code tokenizer} /
 *       {@code maxTokens}，以及 AI 持久化策略（{@code aiPersistenceStrategy} / {@code ephemeral*}
 *       / {@code ephemeralChatMemory}），与助手侧同构（内置记忆默认包一层
 *       {@link EphemeralChatMemory}）；</li>
 *   <li><b>意图识别</b>：{@link #intent(IntentClassifier)} 返回 {@link IntentRouting} 视图，
 *       复用 {@link IntentRag.Builder} / {@link IntentTools.Builder} 按意图分流检索与工具；</li>
 *   <li><b>agentic 原生存储</b>：{@link #scopeStore(AgenticScopeStore)} 把编排共享状态
 *       （{@code AgenticScope}）持久化到给定 store。</li>
 * </ul>
 *
 * <p>隔离层说明：所有对 langchain4j-agentic 实验性 API 的引用都收口在本类（及同级门面）之内，
 * 业务/脚本用本类即可构建 agent，无需直接接触上游 {@code AgentBuilder}。</p>
 *
 * @param <T> typed 时为调用方声明的 {@code @Agent} 接口类型；untyped 时为 {@link UntypedAgent}
 */
public final class AgenticBuilder<T> {

    private static final int DEFAULT_MEMORY_MAX_MESSAGES = 20;

    private final AgentBuilder<T, ?> delegate;

    // ------------------------------------------------------------------
    // 记忆 / 存储
    // ------------------------------------------------------------------

    private ChatMemory chatMemory;

    private ChatMemoryProvider chatMemoryProvider;

    private AgenticScopeStore scopeStore;

    private int memoryMaxMessages = DEFAULT_MEMORY_MAX_MESSAGES;

    private TokenCountEstimator tokenizer;

    private Integer maxTokens;

    private ChatMemoryStore memoryStore;

    /** 是否配置过会改变记忆装配的入口（memory/memoryStore/tokenizer/策略/ephemeral）。 */
    private boolean memoryConfigured;

    private AiPersistenceStrategy aiPersistenceStrategy;

    private String aiPersistenceStrategyKey;

    private boolean strictAiPersistenceStrategy = false;

    private boolean ephemeralLimitAiLength = false;

    private int ephemeralMaxAiCharsPerMessage = -1;

    private int ephemeralMaxAiTotalChars = -1;

    private int ephemeralMaxAiMessages = Integer.MAX_VALUE;

    private boolean ephemeralTruncateAiIfExceeds = false;

    private boolean ephemeralPersistToolResults = false;

    private int ephemeralMaxToolResultChars = 512;

    private int ephemeralMaxToolResults = 0;

    private List<String> ephemeralSeparators;

    private boolean ephemeralStripRagFromHistory = true;

    private boolean ephemeralIncludeCurrentUserWithEphemeral = true;

    private boolean ephemeralSendToolResultsInCurrentTurn = true;

    private boolean ephemeralSendToolRequestsInCurrentTurn = true;

    private AgenticBuilder(AgentBuilder<T, ?> delegate) {
        this.delegate = delegate;
    }

    /** 单 agent（typed）：传入带 {@code @Agent} 注解的接口，产物为该接口的可执行代理。 */
    public static <T> AgenticBuilder<T> agent(Class<T> agentServiceClass) {
        return new AgenticBuilder<>(AgenticServices.agentBuilder(agentServiceClass));
    }

    /** 单 agent（untyped）：产物为 {@link UntypedAgent} 句柄。 */
    public static AgenticBuilder<UntypedAgent> agent() {
        return new AgenticBuilder<>(AgenticServices.agentBuilder());
    }

    // ------------------------------------------------------------------
    // 模型 / 提示 / 工具 / 检索
    // ------------------------------------------------------------------

    /** 对话模型（必填）：agent 调用 LLM 时使用。 */
    public AgenticBuilder<T> chatModel(ChatModel model) {
        delegate.chatModel(model);
        return this;
    }

    /** 流式模型：接口方法返回 {@code TokenStream} 时逐段推送。 */
    public AgenticBuilder<T> streamingChatModel(StreamingChatModel model) {
        delegate.streamingChatModel(model);
        return this;
    }

    /** 系统提示：固定文本。 */
    public AgenticBuilder<T> systemMessage(String text) {
        delegate.systemMessage(text);
        return this;
    }

    /** 系统提示：自定义 provider（入参为 AgenticScope 的相关键）。 */
    public AgenticBuilder<T> systemMessageProvider(Function<Object, String> provider) {
        delegate.systemMessageProvider(provider);
        return this;
    }

    /** 用户消息模板（untyped 常用，如 {@code "回答：{{question}}"}）。 */
    public AgenticBuilder<T> userMessage(String userMessage) {
        delegate.userMessage(userMessage);
        return this;
    }

    /** 工具对象（含 {@code @Tool} 方法）：可多次调用累积。 */
    public AgenticBuilder<T> tools(Object... toolObjects) {
        delegate.tools(toolObjects);
        return this;
    }

    /** 工具提供者：可多次调用累积。 */
    public AgenticBuilder<T> toolProvider(ToolProvider... providers) {
        delegate.toolProviders(providers);
        return this;
    }

    /** 简化 RAG：直接指定内容检索器。 */
    public AgenticBuilder<T> contentRetriever(ContentRetriever contentRetriever) {
        delegate.contentRetriever(contentRetriever);
        return this;
    }

    /** 高级 RAG 管线：自定义检索/排序/增强策略。 */
    public AgenticBuilder<T> retrievalAugmentor(RetrievalAugmentor retrievalAugmentor) {
        delegate.retrievalAugmentor(retrievalAugmentor);
        return this;
    }

    /** 工具循环最大轮数。 */
    public AgenticBuilder<T> maxToolCallingRoundTrips(int maxToolCallingRoundTrips) {
        delegate.maxToolCallingRoundTrips(maxToolCallingRoundTrips);
        return this;
    }

    public AgenticBuilder<T> name(String name) {
        delegate.name(name);
        return this;
    }

    public AgenticBuilder<T> description(String description) {
        delegate.description(description);
        return this;
    }

    /** 输出键：agent 输出写入 AgenticScope 的键名。 */
    public AgenticBuilder<T> outputKey(String outputKey) {
        delegate.outputKey(outputKey);
        return this;
    }

    public AgenticBuilder<T> async(boolean async) {
        delegate.async(async);
        return this;
    }

    /** 观测监听器：观察 agent 调用前后、工具执行、错误等。 */
    public AgenticBuilder<T> listener(AgentListener listener) {
        delegate.listener(listener);
        return this;
    }

    // ------------------------------------------------------------------
    // 会话记忆与存储（对齐 AssistantBuilder）
    // ------------------------------------------------------------------

    /** 记忆：直接用一份 {@link ChatMemory}（想保留工具往返等完整历史时用这条）。 */
    public AgenticBuilder<T> chatMemory(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
        return this;
    }

    /** 记忆：直接用 {@link ChatMemoryProvider}（想保留工具往返等完整历史时用这条）。 */
    public AgenticBuilder<T> chatMemoryProvider(ChatMemoryProvider provider) {
        this.chatMemoryProvider = provider;
        return this;
    }

    /** 进程内记忆：每个 {@code memoryId} 一份窗口记忆（默认包 {@link EphemeralChatMemory}），窗口大小 {@code maxMessages}（默认 20）。 */
    public AgenticBuilder<T> memory(Integer maxMessages) {
        this.memoryMaxMessages = maxMessages == null || maxMessages < 1 ? DEFAULT_MEMORY_MAX_MESSAGES : maxMessages;
        this.memoryConfigured = true;
        return this;
    }

    /**
     * 按 token 截断记忆：配置后记忆改用 {@link TokenWindowChatMemory}（忽略 {@link #memory(Integer)}），
     * 上限由 {@link #maxTokens(Integer)} 控制（默认 1000）。
     */
    public AgenticBuilder<T> tokenizer(TokenCountEstimator tokenizer) {
        this.tokenizer = tokenizer;
        this.memoryConfigured = true;
        return this;
    }

    /** TokenWindowChatMemory 的 token 上限（仅在配置了 tokenizer 时生效，默认 1000）。 */
    public AgenticBuilder<T> maxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
        this.memoryConfigured = true;
        return this;
    }

    /** 记忆存储：{@code langchain4jChatMemoryStore['key']}（记忆由 magic-api 函数托管，可持久化）。 */
    public AgenticBuilder<T> memoryStore(ChatMemoryStore store) {
        this.memoryStore = store;
        this.memoryConfigured = true;
        return this;
    }

    /**
     * agentic 原生存储：把编排共享状态（{@code AgenticScope}）持久化到给定 {@link AgenticScopeStore}，
     * 支持按 {@code (agentId, memoryId)} 跨进程/跨调用恢复。
     *
     * <p><b>注意</b>：上游 {@link AgenticScopePersister} 是进程级静态单例，本方法设置的 store 会对
     * 本进程内<b>所有</b>后续构建的 agentic agent 生效（在 {@link #build()} 时写入）。</p>
     */
    public AgenticBuilder<T> scopeStore(AgenticScopeStore scopeStore) {
        this.scopeStore = scopeStore;
        return this;
    }

    /**
     * 设置 AI 持久化策略（对象实例，优先级最高：对象 &gt; Key &gt; 默认）。
     */
    public AgenticBuilder<T> aiPersistenceStrategy(AiPersistenceStrategy strategy) {
        this.aiPersistenceStrategy = strategy;
        this.aiPersistenceStrategyKey = null;
        this.memoryConfigured = true;
        return this;
    }

    /**
     * 设置 AI 持久化策略（名称或全限定类名，优先级次于策略对象）。
     */
    public AgenticBuilder<T> aiPersistenceStrategy(String keyOrClass) {
        if (keyOrClass == null || keyOrClass.trim().isEmpty()) {
            this.aiPersistenceStrategyKey = null;
            return this;
        }
        this.aiPersistenceStrategyKey = keyOrClass.trim();
        this.aiPersistenceStrategy = null;
        this.memoryConfigured = true;
        return this;
    }

    /** 注册自定义 AI 持久化策略（仅注册，不立即使用）。 */
    public AgenticBuilder<T> registerAiPersistenceStrategy(String name, AiPersistenceStrategy strategy) {
        AiPersistenceStrategy.Registry.register(name, strategy);
        return this;
    }

    /** 注册自定义 AI 持久化策略并立即使用。 */
    public AgenticBuilder<T> registerAndUseAiPersistenceStrategy(String name, AiPersistenceStrategy strategy) {
        AiPersistenceStrategy.Registry.register(name, strategy);
        this.aiPersistenceStrategyKey = name.trim();
        this.aiPersistenceStrategy = null;
        this.memoryConfigured = true;
        return this;
    }

    /** 使用已注册的 AI 持久化策略（按名称或类名）。 */
    public AgenticBuilder<T> useAiPersistenceStrategy(String nameOrClass) {
        return aiPersistenceStrategy(nameOrClass);
    }

    /** 是否严格校验 AI 持久化策略 Key。 */
    public AgenticBuilder<T> strictAiPersistenceStrategy(boolean strict) {
        this.strictAiPersistenceStrategy = strict;
        return this;
    }

    public AgenticBuilder<T> ephemeralLimitAiLength(boolean limit) {
        this.ephemeralLimitAiLength = limit;
        this.memoryConfigured = true;
        return this;
    }

    public AgenticBuilder<T> ephemeralMaxAiCharsPerMessage(int max) {
        this.ephemeralMaxAiCharsPerMessage = max;
        this.memoryConfigured = true;
        return this;
    }

    public AgenticBuilder<T> ephemeralMaxAiTotalChars(int max) {
        this.ephemeralMaxAiTotalChars = max;
        this.memoryConfigured = true;
        return this;
    }

    public AgenticBuilder<T> ephemeralMaxAiMessages(int max) {
        this.ephemeralMaxAiMessages = max;
        this.memoryConfigured = true;
        return this;
    }

    public AgenticBuilder<T> ephemeralTruncateAiIfExceeds(boolean truncate) {
        this.ephemeralTruncateAiIfExceeds = truncate;
        this.memoryConfigured = true;
        return this;
    }

    public AgenticBuilder<T> ephemeralPersistToolResults(boolean persist) {
        this.ephemeralPersistToolResults = persist;
        this.memoryConfigured = true;
        return this;
    }

    public AgenticBuilder<T> ephemeralMaxToolResultChars(int max) {
        this.ephemeralMaxToolResultChars = max;
        this.memoryConfigured = true;
        return this;
    }

    public AgenticBuilder<T> ephemeralMaxToolResults(int max) {
        this.ephemeralMaxToolResults = max;
        this.memoryConfigured = true;
        return this;
    }

    public AgenticBuilder<T> ephemeralSeparators(String... separators) {
        if (separators != null && separators.length > 0) {
            this.ephemeralSeparators = Arrays.asList(separators);
        }
        this.memoryConfigured = true;
        return this;
    }

    public AgenticBuilder<T> ephemeralSeparators(List<String> separators) {
        if (separators != null && !separators.isEmpty()) {
            this.ephemeralSeparators = new ArrayList<>(separators);
        }
        this.memoryConfigured = true;
        return this;
    }

    public AgenticBuilder<T> ephemeralStripRagFromHistory(boolean strip) {
        this.ephemeralStripRagFromHistory = strip;
        this.memoryConfigured = true;
        return this;
    }

    public AgenticBuilder<T> ephemeralIncludeCurrentUserWithEphemeral(boolean include) {
        this.ephemeralIncludeCurrentUserWithEphemeral = include;
        this.memoryConfigured = true;
        return this;
    }

    public AgenticBuilder<T> ephemeralSendToolResultsInCurrentTurn(boolean send) {
        this.ephemeralSendToolResultsInCurrentTurn = send;
        this.memoryConfigured = true;
        return this;
    }

    public AgenticBuilder<T> ephemeralSendToolRequestsInCurrentTurn(boolean send) {
        this.ephemeralSendToolRequestsInCurrentTurn = send;
        this.memoryConfigured = true;
        return this;
    }

    /**
     * 用 {@link EphemeralChatMemoryProperties} 一次性配置 Ephemeral 记忆（策略解析与自定义策略注册一并完成）。
     */
    public AgenticBuilder<T> ephemeralChatMemory(EphemeralChatMemoryProperties properties) {
        if (properties == null || !properties.isEnabled()) {
            return this;
        }
        this.aiPersistenceStrategy = properties.resolveAiPersistenceStrategy();
        this.aiPersistenceStrategyKey = null;
        this.ephemeralSeparators = new ArrayList<>(properties.getEphemeralSeparators());
        this.ephemeralStripRagFromHistory = properties.isStripRagFromHistory();
        this.ephemeralIncludeCurrentUserWithEphemeral = properties.isIncludeCurrentUserWithEphemeral();
        this.ephemeralSendToolResultsInCurrentTurn = properties.isSendToolResultsInCurrentTurn();
        this.ephemeralSendToolRequestsInCurrentTurn = properties.isSendToolRequestsInCurrentTurn();
        this.memoryConfigured = true;
        return this;
    }

    // ------------------------------------------------------------------
    // 意图识别
    // ------------------------------------------------------------------

    /**
     * 意图识别一站式入口：挂上分类器，并在返回的 {@link IntentRouting} 上按需配置意图化检索
     * （{@link IntentRag}）与意图化工具（{@link IntentTools}）。
     *
     * <p>{@link IntentRouting#done()} / {@link IntentRouting#build()} 会把配好的意图检索器设为
     * {@code contentRetriever}、意图工具提供者设为 {@code toolProvider}（与直接调用这两个方法同义，
     * <b>后调用者覆盖先调用者</b>）；只挂分类器、不配路由时，不影响原有检索/工具装配。</p>
     *
     * @param <I> 使用方自定义的意图枚举类型
     */
    public <I extends Enum<I>> IntentRouting<I> intent(IntentClassifier<I> classifier) {
        if (classifier == null) {
            throw new IllegalArgumentException("classifier 不能为 null");
        }
        return new IntentRouting<>(classifier);
    }

    // ------------------------------------------------------------------
    // untyped 专属（typed 构建器调用抛 IllegalStateException）
    // ------------------------------------------------------------------

    /** untyped 专属：显式声明产物返回类型（typed 由接口方法签名决定，无需调用）。 */
    public AgenticBuilder<T> returnType(Class<?> returnType) {
        untyped().returnType(returnType);
        return this;
    }

    /** untyped 专属：声明一个输入键（作为 AgenticScope 输入变量）。 */
    public AgenticBuilder<T> inputKey(Class<?> type, String name) {
        untyped().inputKey(type, name);
        return this;
    }

    /** untyped 专属：声明两个输入键。 */
    public AgenticBuilder<T> inputKeys(Class<?> type1, String name1, Class<?> type2, String name2) {
        untyped().inputKeys(type1, name1, type2, name2);
        return this;
    }

    private UntypedAgentBuilder untyped() {
        if (delegate instanceof UntypedAgentBuilder untyped) {
            return untyped;
        }
        throw new IllegalStateException("returnType/inputKey/inputKeys 仅适用于 untyped agent（用 AgenticBuilder.agent() 构建）");
    }

    // ------------------------------------------------------------------
    // 装配
    // ------------------------------------------------------------------

    /** 产出 agent：typed 返回接口代理，untyped 返回 {@link UntypedAgent}。 */
    public T build() {
        if (scopeStore != null) {
            AgenticScopePersister.setStore(scopeStore);
        }
        if (chatMemory != null) {
            delegate.chatMemory(chatMemory);
        }
        if (chatMemoryProvider != null) {
            delegate.chatMemoryProvider(chatMemoryProvider);
        } else if (memoryConfigured) {
            delegate.chatMemoryProvider(resolveChatMemoryProvider());
        }
        return delegate.build();
    }

    /** 记忆：显式 provider → 传入的 store → 进程内 store；每个 memoryId 一份窗口记忆（默认包 EphemeralChatMemory）。 */
    private ChatMemoryProvider resolveChatMemoryProvider() {
        final ChatMemoryStore store = memoryStore == null ? new InMemoryChatMemoryStore() : memoryStore;
        final AiPersistenceStrategy resolvedStrategy = resolveEphemeralStrategy();
        final List<String> separators = ephemeralSeparators != null
                ? ephemeralSeparators
                : Collections.singletonList(EphemeralChatMemory.DEFAULT_RAG_SEPARATOR);
        return memoryId -> {
            ChatMemory delegate;
            if (tokenizer != null) {
                int limit = maxTokens == null ? 1000 : maxTokens;
                delegate = TokenWindowChatMemory.builder()
                        .id(memoryId)
                        .maxTokens(limit, tokenizer)
                        .chatMemoryStore(store)
                        .build();
            } else {
                delegate = MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(memoryMaxMessages)
                        .chatMemoryStore(store)
                        .build();
            }
            return EphemeralChatMemory.builder(delegate, store)
                    .aiPersistenceStrategy(resolvedStrategy)
                    .ephemeralSeparators(separators)
                    .stripRagFromHistory(ephemeralStripRagFromHistory)
                    .includeCurrentUserWithEphemeral(ephemeralIncludeCurrentUserWithEphemeral)
                    .sendToolResultsInCurrentTurn(ephemeralSendToolResultsInCurrentTurn)
                    .sendToolRequestsInCurrentTurn(ephemeralSendToolRequestsInCurrentTurn)
                    .build();
        };
    }

    /**
     * 解析 AI 持久化策略，优先级：策略对象 &gt; 策略 Key（name/class）&gt; 默认
     * {@link AiPersistenceStrategy#PERSIST_NON_TOOL_FINAL_ANSWER}。
     * <p>随后依次叠加「长度限制」与「工具结果持久化」两个装饰器（按开关）。</p>
     */
    private AiPersistenceStrategy resolveEphemeralStrategy() {
        AiPersistenceStrategy strategy;
        if (aiPersistenceStrategy != null) {
            strategy = aiPersistenceStrategy;
        } else if (aiPersistenceStrategyKey != null && !aiPersistenceStrategyKey.trim().isEmpty()) {
            AiPersistenceStrategy resolved = AiPersistenceStrategy.Registry.tryResolve(aiPersistenceStrategyKey);
            if (resolved == null) {
                if (strictAiPersistenceStrategy) {
                    throw new IllegalArgumentException("未知的 AI 持久化策略：'" + aiPersistenceStrategyKey
                            + "'（未在 AiPersistenceStrategy.Registry 注册，也无法作为 AiPersistenceStrategy 类实例化）");
                }
                resolved = AiPersistenceStrategy.PERSIST_NON_TOOL_FINAL_ANSWER;
            }
            strategy = resolved;
        } else {
            strategy = AiPersistenceStrategy.PERSIST_NON_TOOL_FINAL_ANSWER;
        }

        if (ephemeralLimitAiLength) {
            strategy = LengthBoundedAiPersistenceStrategy.builder(strategy)
                    .maxCharsPerMessage(ephemeralMaxAiCharsPerMessage)
                    .maxTotalChars(ephemeralMaxAiTotalChars)
                    .maxMessages(ephemeralMaxAiMessages <= 0 ? Integer.MAX_VALUE : ephemeralMaxAiMessages)
                    .truncateIfExceeds(ephemeralTruncateAiIfExceeds)
                    .build();
        }

        if (ephemeralPersistToolResults) {
            strategy = PersistToolResultsAiPersistenceStrategy.builder(strategy)
                    .persistToolResults(true)
                    .maxToolResultChars(ephemeralMaxToolResultChars)
                    .maxToolResults(ephemeralMaxToolResults)
                    .build();
        }

        return strategy;
    }

    /**
     * {@link #intent(IntentClassifier)} 返回的意图配置视图：复用既有的 {@link IntentRag.Builder}
     * 与 {@link IntentTools.Builder} 收集意图路由，最后接线回 {@link AgenticBuilder}。
     * 两个配置入口共享同一条意图来源（同一个分类器实例），避免各建一套。
     *
     * @param <I> 使用方自定义的意图枚举类型
     */
    public final class IntentRouting<I extends Enum<I>> {

        private final IntentRag.Builder<I> ragBuilder;

        private final IntentTools.Builder<I> toolsBuilder;

        private boolean ragConfigured;

        private boolean toolsConfigured;

        private IntentRouting(IntentClassifier<I> classifier) {
            IntentSupplier<I> supplier = IntentSupplier.of(classifier);
            this.ragBuilder = IntentRag.bySupplier(supplier);
            this.toolsBuilder = IntentTools.bySupplier(supplier);
        }

        /** 意图化检索配置（{@link IntentRag} 的 route / routeNone / fallback）。 */
        public IntentRag.Builder<I> rag() {
            ragConfigured = true;
            return ragBuilder;
        }

        /** 意图化工具配置（{@link IntentTools} 的 source / allow / allowNone / alwaysInclude / fallback）。 */
        public IntentTools.Builder<I> tools() {
            toolsConfigured = true;
            return toolsBuilder;
        }

        /**
         * 把已配置的意图路由接线到 {@link AgenticBuilder} 并返回它，便于继续链式配置。
         * 未调用 {@link #rag()} / {@link #tools()} 的分支不接线。
         */
        public AgenticBuilder<T> done() {
            if (ragConfigured) {
                contentRetriever(ragBuilder.build());
            }
            if (toolsConfigured) {
                toolProvider(toolsBuilder.build());
            }
            return AgenticBuilder.this;
        }

        /** 接线后直接产出 agent，等价于 {@code done().build()}。 */
        public T build() {
            return done().build();
        }
    }
}
