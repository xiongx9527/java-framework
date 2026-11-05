package com.xiongx9527.fw.plus.ai.base.service;

import com.xiongx9527.fw.plus.ai.base.intent.IntentPort;
import com.xiongx9527.fw.plus.ai.base.intent.IntentRequest;
import com.xiongx9527.fw.plus.ai.base.intent.IntentResult;
import com.xiongx9527.fw.plus.ai.base.intent.IntentScoped;
import com.xiongx9527.fw.plus.ai.base.intent.Intent;
import com.xiongx9527.fw.plus.ai.base.intent.IntentMatch;
import com.xiongx9527.fw.plus.ai.base.intent.IntentEvent;
import com.xiongx9527.fw.plus.ai.base.memory.ChatMemory;
import com.xiongx9527.fw.plus.ai.base.memory.ChatMemoryProvider;
import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.ContextInjectEvent;
import com.xiongx9527.fw.plus.ai.base.model.LlmListener;
import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import com.xiongx9527.fw.plus.ai.base.model.LlmTokenStream;
import com.xiongx9527.fw.plus.ai.base.model.ChatRole;
import com.xiongx9527.fw.plus.ai.base.output.JsonOutputException;
import com.xiongx9527.fw.plus.ai.base.output.JsonSupport;
import com.xiongx9527.fw.plus.ai.base.model.moderation.ModerationException;
import com.xiongx9527.fw.plus.ai.base.model.moderation.ModerationListener;
import com.xiongx9527.fw.plus.ai.base.model.moderation.ModerationModel;
import com.xiongx9527.fw.plus.ai.base.model.moderation.ModerationResult;
import com.xiongx9527.fw.plus.ai.base.output.JsonBindings;
import com.xiongx9527.fw.plus.ai.base.output.JsonOutputPort;
import com.xiongx9527.fw.plus.ai.base.store.StoreRegistry;
import com.xiongx9527.fw.plus.ai.base.mcp.McpClient;
import com.xiongx9527.fw.plus.ai.base.mcp.McpToolProvider;
import com.xiongx9527.fw.plus.ai.base.tool.ToolArgumentCodec;
import com.xiongx9527.fw.plus.ai.base.retrieval.ChainedRetrievalAugmentor;
import com.xiongx9527.fw.plus.ai.base.retrieval.DefaultRetrievalAugmentor;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalAugmentor;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalListener;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalQuery;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retrieved;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalEvent;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retriever;
import com.xiongx9527.fw.plus.ai.base.skill.SkillBox;
import com.xiongx9527.fw.plus.ai.base.skill.SkillDescriptor;
import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
import com.xiongx9527.fw.plus.ai.base.template.PromptTemplate;
import com.xiongx9527.fw.plus.ai.base.trace.Span;
import com.xiongx9527.fw.plus.ai.base.trace.SpanKind;
import com.xiongx9527.fw.plus.ai.base.trace.SpanStatus;
import com.xiongx9527.fw.plus.ai.base.trace.TraceContext;
import com.xiongx9527.fw.plus.ai.base.trace.TracePort;
import com.xiongx9527.fw.plus.ai.base.tool.CompositeToolProvider;
import com.xiongx9527.fw.plus.ai.base.tool.SkillGatedToolProvider;
import com.xiongx9527.fw.plus.ai.base.tool.ToolCallingLoop;
import com.xiongx9527.fw.plus.ai.base.tool.ToolCallingRequest;
import com.xiongx9527.fw.plus.ai.base.tool.ToolCallingResult;
import com.xiongx9527.fw.plus.ai.base.tool.ToolCallingSink;
import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionResult;
import com.xiongx9527.fw.plus.ai.base.tool.ToolProvider;
import com.xiongx9527.fw.plus.ai.base.tool.ToolTrace;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AiServices {

    private AiServices() {
    }

    public static <T> Builder<T> builder(Class<T> serviceType) {
        return new Builder<T>(serviceType);
    }

    /** 接口代理入口：强类型调用。 */
    public static <T> T create(Class<T> service) {
        if (service == null || !service.isInterface()) {
            throw new IllegalArgumentException("ai service type must be an interface");
        }
        return new Builder<T>(service).build();
    }

    /** 无类调用入口：不要求预先定义接口，运行时以动态入参驱动同一条执行管线。 */
    public static Builder<Object> fluent() {
        return new Builder<Object>(Object.class);
    }

    public static final class Builder<T> {

        private final Class<T> serviceType;
        private LlmPort llm;
        private final List<IntentScoped<ChatMemoryProvider>> memoryScopes = new ArrayList<IntentScoped<ChatMemoryProvider>>();
        private final List<IntentScoped<IntentPort>> intentScopes = new ArrayList<IntentScoped<IntentPort>>();
        private final List<IntentScoped<ToolProvider>> toolProviderScopes = new ArrayList<IntentScoped<ToolProvider>>();
        private final List<IntentScoped<RetrievalAugmentor>> retrievalScopes = new ArrayList<IntentScoped<RetrievalAugmentor>>();
        private final List<IntentScoped<SkillBox>> skillScopes = new ArrayList<IntentScoped<SkillBox>>();
        private List<LlmListener> listeners;
        private boolean emitLifecycleEvents = true;
        private boolean skipToolsWhenRetrievalHit = false;

        private ModerationModel moderationModel;
        private final List<ModerationListener> moderationListeners = new ArrayList<ModerationListener>();
        private JsonOutputPort jsonOutput;
        private JsonBindings jsonBindings;
        private StoreRegistry storeRegistry;
        private final List<AiObservationListener> observationListeners = new ArrayList<AiObservationListener>();
        private final List<AiInvocationSummaryListener> summaryListeners = new ArrayList<AiInvocationSummaryListener>();
        private TracePort trace = TracePort.noop();

        private String systemTextOverride;
        private String userTemplateOverride;
        private java.util.function.Supplier<String> memoryIdOverride;
        private Integer memoryIdParamIndex;
        private String memoryIdVarKey;
        private final Map<String, Integer> varOverrides = new LinkedHashMap<String, Integer>();
        private final Map<String, java.util.function.Supplier<Object>> customVariables = new LinkedHashMap<String, java.util.function.Supplier<Object>>();
        private boolean jsonResult = false;
        private Class<?> chatReturnType = String.class;
        private String model;
        private Double temperature;
        private Double topP;
        private Integer maxTokens;
        private Integer seed;
        private Integer topK;
        private Double frequencyPenalty;
        private Double presencePenalty;
        private java.util.List<String> stopSequences;
        private java.util.Map<String, Object> responseFormat;

        /** 注册调用结束后的全量摘要监听。 */
        public Builder<T> summaryListener(AiInvocationSummaryListener value) {
            if (value != null) {
                this.summaryListeners.add(value);
            }
            return this;
        }

        /** 无类调用（{@link #fluent()}）下是否走稳定 JSON 输出端口；等价于接口方法上的 {@code @JsonResult}。 */
        public Builder<T> jsonResult(boolean value) {
            this.jsonResult = value;
            return this;
        }

        /** 无类调用（{@link #fluent()}）的返回类型：{@code String}（默认）或 {@code LlmResponse}。 */
        public Builder<T> returnType(Class<?> value) {
            this.chatReturnType = value == null ? String.class : value;
            return this;
        }

        /** 默认模型 id；为 {@code null} 时由 {@code LlmPort} 的默认模型决定。 */
        public Builder<T> model(String value) {
            this.model = value;
            return this;
        }

        /** 默认采样温度；为 {@code null} 时由后端决定。 */
        public Builder<T> temperature(Double value) {
            this.temperature = value;
            return this;
        }

        /** 默认 topP；为 {@code null} 时由后端决定。 */
        public Builder<T> topP(Double value) {
            this.topP = value;
            return this;
        }

        /** 默认最大输出 token 数；为 {@code null} 时由后端决定。 */
        public Builder<T> maxTokens(Integer value) {
            this.maxTokens = value;
            return this;
        }

        /** 默认随机种子；为 {@code null} 时由后端决定。 */
        public Builder<T> seed(Integer value) {
            this.seed = value;
            return this;
        }

        /** 默认 topK。 */
        public Builder<T> topK(Integer value) {
            this.topK = value;
            return this;
        }

        /** 默认频率惩罚。 */
        public Builder<T> frequencyPenalty(Double value) {
            this.frequencyPenalty = value;
            return this;
        }

        /** 默认存在惩罚。 */
        public Builder<T> presencePenalty(Double value) {
            this.presencePenalty = value;
            return this;
        }

        /** 默认停止序列。 */
        public Builder<T> stopSequences(java.util.List<String> value) {
            this.stopSequences = value;
            return this;
        }

        /** 默认结构化输出约束（如 {@code {"type":"json_object"}}）。 */
        public Builder<T> responseFormat(java.util.Map<String, Object> value) {
            this.responseFormat = value;
            return this;
        }

        private Builder(Class<T> serviceType) {
            if (serviceType == null) {
                throw new IllegalArgumentException("service type is required");
            }
            this.serviceType = serviceType;
        }

        public Builder<T> llm(LlmPort value) {
            this.llm = value;
            return this;
        }

        public Builder<T> chatMemoryProvider(ChatMemoryProvider value) {
            if (value != null) {
                this.memoryScopes.add(IntentScoped.global(value));
            }
            return this;
        }

        /**
         * 绑定指定意图的记忆提供器：仅当分类命中 {@code intents} 之一时才使用该记忆，
         * 否则回退到全局（{@link #chatMemoryProvider(ChatMemoryProvider)}）提供器。
         */
        public Builder<T> chatMemoryProviderOn(ChatMemoryProvider value, String... intents) {
            if (value != null) {
                this.memoryScopes.add(IntentScoped.on(value, intents));
            }
            return this;
        }

        public Builder<T> intentClassifier(IntentPort value) {
            if (value != null) {
                this.intentScopes.add(IntentScoped.global(value));
            }
            return this;
        }

        /**
         * 绑定指定意图的分类器：仅当已有意图上下文命中 {@code intents} 之一时启用（顶层调用因意图未知，
         * 恒走全局分类器）；用于智能体 / 嵌套路由下的二次分类。
         */
        public Builder<T> intentClassifierOn(IntentPort value, String... intents) {
            if (value != null) {
                this.intentScopes.add(IntentScoped.on(value, intents));
            }
            return this;
        }

        public Builder<T> toolProvider(ToolProvider value) {
            if (value != null) {
                this.toolProviderScopes.add(IntentScoped.global(value));
            }
            return this;
        }

        public Builder<T> toolProviders(ToolProvider... values) {
            List<ToolProvider> list = new ArrayList<ToolProvider>();
            if (values != null) {
                Collections.addAll(list, values);
            }
            if (!list.isEmpty()) {
                this.toolProviderScopes.add(IntentScoped.global(CompositeToolProvider.of(list.toArray(new ToolProvider[0]))));
            }
            return this;
        }

        /**
         * 直接提供工具（数组形式），等价于 {@link #toolProviders(ToolProvider...)}。
         *
         * <p>适合「把若干工具对象一次性注册为全局可用」的场景；若需按意图绑定，用
         * {@link #toolProviderOn(ToolProvider, String...)}。</p>
         */
        public Builder<T> tools(ToolProvider... values) {
            return toolProviders(values);
        }

        /**
         * 批量接入 MCP 客户端（数组）：每个 {@link McpClient} 包成 {@link McpToolProvider} 后作为全局工具。
         *
         * <p>对上层而言 MCP 工具与本地 {@code @Tool} 没有区别；{@code codec} 负责工具参数的
         * 序列化（base 不绑定 JSON 库，由调用方注入 Jackson / fastjson 等实现）。</p>
         *
         * <p>若希望 MCP 工具只在特定意图下才可用，用 {@link #mcpClientsOn(ToolArgumentCodec, String[], McpClient...)}。</p>
         */
        public Builder<T> mcpClients(ToolArgumentCodec codec, McpClient... clients) {
            return mcpClientsOn(codec, null, clients);
        }

        /**
         * 批量接入 MCP 客户端（数组），并绑定到指定意图：仅当分类结果命中 {@code intents} 之一时，
         * 这些 MCP 工具才会被激活；{@code intents} 为 {@code null} 或空时退化为全局工具。
         *
         * <p>与 {@link #toolProviderOn(ToolProvider, String...)} 语义一致，用于同一支 AI 服务下
         * 按意图挂载不同的 MCP 后端。</p>
         */
        public Builder<T> mcpClientsOn(ToolArgumentCodec codec, String[] intents, McpClient... clients) {
            if (codec != null && clients != null) {
                for (McpClient client : clients) {
                    if (client == null) {
                        continue;
                    }
                    this.toolProviderScopes.add(mcpScope(new McpToolProvider(client, codec), intents));
                }
            }
            return this;
        }

        private static IntentScoped<ToolProvider> mcpScope(ToolProvider provider, String[] intents) {
            if (intents == null || intents.length == 0) {
                return IntentScoped.global(provider);
            }
            return IntentScoped.on(provider, intents);
        }

        public Builder<T> toolProviderOn(ToolProvider value, String... intents) {
            if (value != null) {
                this.toolProviderScopes.add(IntentScoped.on(value, intents));
            }
            return this;
        }

        public Builder<T> retrievalAugmentor(RetrievalAugmentor value) {
            if (value != null) {
                this.retrievalScopes.add(IntentScoped.global(value));
            }
            return this;
        }

        public Builder<T> retrievalAugmentors(RetrievalAugmentor... values) {
            RetrievalAugmentor chained = ChainedRetrievalAugmentor.of(values);
            if (chained != null) {
                this.retrievalScopes.add(IntentScoped.global(chained));
            }
            return this;
        }

        public Builder<T> retrievalOn(RetrievalAugmentor value, String... intents) {
            if (value != null) {
                this.retrievalScopes.add(IntentScoped.on(value, intents));
            }
            return this;
        }

        public Builder<T> retriever(Retriever<Document> retriever) {
            if (retriever != null) {
                this.retrievalScopes.add(IntentScoped.global(new DefaultRetrievalAugmentor(retriever)));
            }
            return this;
        }

        public Builder<T> skillBox(SkillBox value) {
            if (value != null) {
                this.skillScopes.add(IntentScoped.global(value));
            }
            return this;
        }

        public Builder<T> skillBoxOn(SkillBox value, String... intents) {
            if (value != null) {
                this.skillScopes.add(IntentScoped.on(value, intents));
            }
            return this;
        }

        public Builder<T> skills(SkillDescriptor... values) {
            SkillBox box = new SkillBox();
            if (values != null) {
                for (SkillDescriptor d : values) {
                    if (d != null) {
                        box.registry().register(d, true);
                    }
                }
            }
            return skillBox(box);
        }

        public Builder<T> listeners(List<LlmListener> value) {
            this.listeners = value;
            return this;
        }

        /** 接入命名存储注册器：把向量 / 图 / 自定义后端统一登记，上层按名或类型取用。 */
        public Builder<T> storeRegistry(StoreRegistry value) {
            this.storeRegistry = value;
            return this;
        }

        /** 是否向流处理器派发意图 / 检索 / 注入 / 工具 等生命周期事件（默认开启）。 */
        public Builder<T> emitLifecycleEvents(boolean value) {
            this.emitLifecycleEvents = value;
            return this;
        }

        /** 检索已命中上下文时跳过工具调用（对齐 langchain4j 的“检索优先、命中即答”行为，默认关闭）。 */
        public Builder<T> skipToolsWhenRetrievalHit(boolean value) {
            this.skipToolsWhenRetrievalHit = value;
            return this;
        }

        /** 接入内容审核：调用模型前审核用户输入、调用后审核输出，命中即抛 {@link ModerationException}。 */
        public Builder<T> moderation(ModerationModel model, ModerationListener... listeners) {
            this.moderationModel = model;
            if (listeners != null) {
                Collections.addAll(this.moderationListeners, listeners);
            }
            return this;
        }

        /** 接入稳定 JSON 输出端口：配合方法上的 {@link JsonResult} 注解强制并兜底提取 JSON。 */
        public Builder<T> jsonOutput(JsonOutputPort value) {
            this.jsonOutput = value;
            return this;
        }

        /**
         * 接入 JSON 绑定器：被 {@link JsonResult} 标注且返回类型既非 {@code String} 也非
         * {@link com.xiongx9527.fw.plus.ai.base.model.LlmResponse} 时，把提取的 JSON 文本反序列化为该 POJO。
         * 不设置则回退为返回原始 JSON 文本（保持向后兼容）。
         */
        public Builder<T> jsonBindings(JsonBindings value) {
            this.jsonBindings = value;
            return this;
        }

        /** 接入调用级观测监听（Langfuse / Langsmith 等），用于 trace / 监控。 */
        public Builder<T> observation(AiObservationListener... listeners) {
            if (listeners != null) {
                Collections.addAll(this.observationListeners, listeners);
            }
            return this;
        }

        /**
         * 接入链路追踪端口：每次 AI 服务调用生成 {@code ai.call} 根节点，并为意图分类、工具调用
         * 补子节点；未接入时不做任何额外开销。
         *
         * <p>根节点会复用调用线程上已有的 traceId（便于把 AI 调用挂到上游请求链路上），
         * 调用结束后恢复原上下文。</p>
         */
        public Builder<T> trace(TracePort value) {
            this.trace = value == null ? TracePort.noop() : value;
            return this;
        }

        /** 覆盖类型 / 方法上的 {@code @SystemMessage}：直接给定 system 文本（支持 {@code {{var}}} 占位）。 */
        public Builder<T> systemText(String value) {
            this.systemTextOverride = value;
            return this;
        }

        /**
         * 覆盖方法上的 {@code @UserMessage}：用给定模板渲染用户输入，支持
         * {@code {{var}}} / {@code {{var:format}}} / {@code {{var ?? default}}} 三种占位表达式。
         */
        public Builder<T> userTemplate(String value) {
            this.userTemplateOverride = value;
            return this;
        }

        /** 动态会话 id：每次调用实时计算（覆盖 {@code @MemoryId}）。 */
        public Builder<T> memoryId(java.util.function.Supplier<String> supplier) {
            this.memoryIdOverride = supplier;
            return this;
        }

        /**
         * 从业务输入变量表里按 key 动态取会话 id：每次调用从入参变量（无类调用为 {@code call(vars)} 的 map，
         * 接口调用为 {@code @V} 绑定的变量）中读 {@code key} 的值作为会话 id。
         * 适合会话 / 用户 id 由请求体带入、随业务输入动态变化的场景；优先级低于 {@code @MemoryId} /
         * {@link #memoryIdFromParam(int)} / {@link #memoryId(Supplier)}。
         */
        public Builder<T> memoryIdFromVar(String key) {
            this.memoryIdVarKey = key;
            return this;
        }

        /** 把第 {@code paramIndex} 个方法参数当作会话 id（覆盖 {@code @MemoryId}）。 */
        public Builder<T> memoryIdFromParam(int paramIndex) {
            this.memoryIdParamIndex = paramIndex;
            return this;
        }

        /** 把第 {@code paramIndex} 个方法参数绑定到模板变量 {@code name}（替代参数上的 {@code @V}）。 */
        public Builder<T> variable(String name, int paramIndex) {
            if (name != null) {
                this.varOverrides.put(name, paramIndex);
            }
            return this;
        }

        /** 注入动态自定义变量：每次调用实时计算后并入模板（不依赖固定值）。 */
        public Builder<T> variable(String name, java.util.function.Supplier<Object> supplier) {
            if (name != null) {
                this.customVariables.put(name, supplier);
            }
            return this;
        }

        public T build() {
            if (llm == null) {
                throw new IllegalStateException("llm port is required");
            }
            return serviceType.cast(Proxy.newProxyInstance(
                    serviceType.getClassLoader(), new Class<?>[]{serviceType},
                    new AiServiceInvocationHandler(
                            new AiServiceContext(
                            serviceType, llm, memoryScopes, intentScopes,
                            toolProviderScopes, retrievalScopes, skillScopes, listeners,
                            emitLifecycleEvents, skipToolsWhenRetrievalHit,
                            moderationModel, moderationListeners, jsonOutput, jsonBindings, observationListeners, trace,
                            systemTextOverride, userTemplateOverride, memoryIdOverride, memoryIdParamIndex,
                            memoryIdVarKey,
                            varOverrides, customVariables, storeRegistry,
                            model, temperature, topP, maxTokens, seed,
                            topK, frequencyPenalty, presencePenalty, stopSequences, responseFormat,
                            summaryListeners))));
        }

        /** 构建无类调用器：复用同一套上下文与执行管线，仅入口从接口代理换成动态入参。 */
        public ChatCall buildChatCall() {
            if (llm == null) {
                throw new IllegalStateException("llm port is required");
            }
            AiServiceContext ctx = new AiServiceContext(
                    serviceType, llm, memoryScopes, intentScopes,
                    toolProviderScopes, retrievalScopes, skillScopes, listeners,
                    emitLifecycleEvents, skipToolsWhenRetrievalHit,
                    moderationModel, moderationListeners, jsonOutput, jsonBindings, observationListeners, trace,
                    systemTextOverride, userTemplateOverride, memoryIdOverride, memoryIdParamIndex,
                    memoryIdVarKey,
                    varOverrides, customVariables, storeRegistry,
                    model, temperature, topP, maxTokens, seed,
                    topK, frequencyPenalty, presencePenalty, stopSequences, responseFormat,
                    summaryListeners);
            return new ChatCall(ctx, jsonResult, chatReturnType);
        }
    }

    private static final class AiServiceInvocationHandler implements InvocationHandler {

        private final AiServiceContext context;

        private AiServiceInvocationHandler(AiServiceContext context) {
            this.context = context;
        }

        public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getDeclaringClass() == Object.class) {
                return invokeObjectMethod(proxy, method, args);
            }
            Map<String, Object> baseVariables = variables(method, args);
            String memoryId = memoryId(method, args, baseVariables);
            LlmStreamHandler streamHandler = streamHandler(method, args);
            Map<String, Object> userVars = withSystemVars(baseVariables, memoryId, null);
            String userText = userPrompt(method, userVars);
            String systemPromptRaw = template(method.getAnnotation(SystemMessage.class),
                    context.getServiceType().getAnnotation(SystemMessage.class));
            CallInput input = new CallInput();
            input.method = method;
            input.userText = userText;
            input.systemPromptRaw = systemPromptRaw;
            input.baseVariables = baseVariables;
            input.memoryId = memoryId;
            input.streamHandler = streamHandler;
            input.jsonResult = method.isAnnotationPresent(JsonResult.class);
            input.returnType = method.getReturnType();
            input.genericReturnType = method.getGenericReturnType();
            if (LlmTokenStream.class.equals(method.getReturnType())) {
                return LlmTokenStream.create(handler -> {
                    input.streamHandler = handler;
                    execute(input);
                });
            }
            return execute(input);
        }

        /** 执行管线：意图分类 → 检索增强 → 工具/记忆选择 → 模型调用（含 JSON / 审核 / 观测）。 */
        Object execute(CallInput input) {
            String userText = input.userText;
            Map<String, Object> baseVariables = input.baseVariables;
            String memoryId = input.memoryId;
            LlmStreamHandler streamHandler = input.streamHandler;
            boolean jsonResult = input.jsonResult;
            Class<?> returnType = input.returnType;
            java.lang.reflect.Type genericReturnType = input.genericReturnType;
            Method method = input.method;
            boolean emit = context.isEmitLifecycleEvents() && streamHandler != null;

            // 复用调用线程已有的 traceId（挂到上游请求链路），否则新开一条链路
            String previousTraceId = TraceContext.traceId();
            String previousSpanId = TraceContext.spanId();
            String traceId = previousTraceId == null ? UUID.randomUUID().toString() : previousTraceId;
            Span.Builder callSpan = context.getTrace().start("ai.call", SpanKind.CHAIN);
            if (previousSpanId != null) {
                callSpan.parent(previousSpanId);
            }
            callSpan.input(userText).attribute("service", context.getServiceType().getSimpleName());
            if (method != null) {
                callSpan.attribute("method", method.getName());
            }
            if (memoryId != null) {
                callSpan.attribute("memoryId", memoryId);
            }
            TraceContext.bind(traceId, callSpan.getSpanId());

            try {
                moderateInput(userText);
                long startNanos = System.nanoTime();
                AiCallObservation.Builder observation = AiCallObservation.builder()
                        .serviceType(context.getServiceType())
                        .method(method)
                        .userText(userText);
                notifyObservationStart(observation);
                observation.event("request");
                ChatMessage userMessage = ChatMessage.user(userText);

                String intentRef = classify(userText, emit, streamHandler);
                if (intentRef != null) {
                    callSpan.attribute("intent", intentRef);
                    observation.event("intent=" + intentRef);
                }
                Map<String, Object> sysVars = withSystemVars(baseVariables, memoryId, intentRef);
                RetrievalAugmentor retrieval = resolveRetrieval(intentRef);
                String skillCatalog = resolveSkillCatalog(intentRef, sysVars);

                List<ChatMessage> messages = new ArrayList<ChatMessage>();
                String systemPromptRaw = input.systemPromptRaw;
                if (systemPromptRaw != null || skillCatalog != null) {
                    String sys = systemPromptRaw == null ? "" : renderTemplate(systemPromptRaw, sysVars);
                    if (skillCatalog != null) {
                        sys = sys.isEmpty() ? skillCatalog : sys + "\n\n" + skillCatalog;
                        if (emit) {
                            streamHandler.onContextInject(ContextInjectEvent.start(
                                    ContextInjectEvent.Kind.SKILL, skillCatalog, null));
                            streamHandler.onContextInject(ContextInjectEvent.end(
                                    ContextInjectEvent.Kind.SKILL, skillCatalog, null));
                        }
                    }
                    messages.add(ChatMessage.system(sys));
                }

                ChatMemoryProvider memoryProvider = selectScoped(context.getMemoryScopes(), intentRef);
                ChatMemory memory = memoryProvider == null || memoryId == null
                        ? null : memoryProvider.get(memoryId);
                if (memory != null) {
                    messages.addAll(memory.messages());
                }
                messages.add(userMessage);
                if (memory != null) {
                    memory.add(userMessage);
                }

                boolean retrievalInjected = false;
                if (retrieval != null) {
                    final RetrievalQuery rq = RetrievalQuery.builder().text(userText).build();
                    final boolean[] injected = {false};
                    RetrievalListener bridge = emit ? new RetrievalListener() {
                        @Override
                        public void onQuery(RetrievalQuery q) {
                            streamHandler.onRetrieval(RetrievalEvent.start(q));
                        }

                        @Override
                        public void onHits(List<? extends Retrieved<?>> hits) {
                            streamHandler.onRetrieval(RetrievalEvent.end(rq, hits, null));
                        }

                        @Override
                        public void onContext(String content) {
                            streamHandler.onRetrieval(RetrievalEvent.end(rq, null, content));
                            streamHandler.onContextInject(ContextInjectEvent.start(
                                    ContextInjectEvent.Kind.RETRIEVAL, content, null));
                            streamHandler.onContextInject(ContextInjectEvent.end(
                                    ContextInjectEvent.Kind.RETRIEVAL, content, null));
                            injected[0] = true;
                        }
                    } : null;
                    RetrievalAugmentor wired = emit ? ChainedRetrievalAugmentor.of(bridge, retrieval) : retrieval;
                    messages = wired.augment(messages, rq);
                    retrievalInjected = injected[0];
                }
                callSpan.attribute("retrievalInjected", retrievalInjected);

                ToolProvider tools = resolveTools(intentRef);
                if (context.isSkipToolsWhenRetrievalHit() && retrievalInjected) {
                    tools = null;
                }

                observation.event("llm.request");
                LlmRequest request = LlmRequest.builder()
                        .messages(messages)
                        .model(context.getModel())
                        .temperature(context.getTemperature())
                        .topP(context.getTopP())
                        .maxTokens(context.getMaxTokens())
                        .seed(context.getSeed())
                        .topK(context.getTopK())
                        .frequencyPenalty(context.getFrequencyPenalty())
                        .presencePenalty(context.getPresencePenalty())
                        .stopSequences(context.getStopSequences())
                        .responseFormat(context.getResponseFormat())
                        .tools(tools == null ? null : tools.specifications())
                        .listeners(context.getListeners())
                        .build();

                Object result;
                try {
                    result = invokeModel(request, tools, streamHandler, jsonResult, returnType, genericReturnType);
                } catch (Throwable t) {
                    notifyObservationError(observation.intentRef(intentRef)
                            .retrievalInjected(retrievalInjected)
                            .latencyMillis((System.nanoTime() - startNanos) / 1000000L)
                            .error(t.getMessage())
                            .build());
                    throw t;
                }

                // 输出审核（结构化 JSON 结果不视为自然语言，跳过）
                if (result instanceof String && !jsonResult) {
                    moderateOutput((String) result);
                }

                if (memory != null && result instanceof String) {
                    memory.add(ChatMessage.assistant((String) result));
                }

                callSpan.output(result instanceof String ? (String) result : null);
                notifyObservationEnd(observation.intentRef(intentRef)
                        .retrievalInjected(retrievalInjected)
                        .answer(result instanceof String ? (String) result : null)
                        .latencyMillis((System.nanoTime() - startNanos) / 1000000L));
                return result;
            } catch (Throwable t) {
                callSpan.error(errorMessage(t));
                throw t;
            } finally {
                callSpan.end(System.currentTimeMillis());
                endSpan(callSpan);
                if (previousTraceId != null) {
                    TraceContext.bind(previousTraceId, previousSpanId);
                } else {
                    TraceContext.unbind();
                }
            }
        }

        /** 结束并上报节点；观测失败不影响业务主链路。 */
        private void endSpan(Span.Builder span) {
            try {
                context.getTrace().end(span);
            } catch (RuntimeException ignored) {
                // 与 LlmListener 派发保持一致：观测异常不得影响业务
            }
        }

        private static String errorMessage(Throwable t) {
            String message = t.getMessage();
            return message == null || message.isEmpty() ? t.getClass().getSimpleName() : message;
        }

        /** 把工具调用环产生的轨迹补成 TOOL 子节点（父节点为当前 {@code ai.call}）。 */
        private void recordToolSpans(List<ToolTrace> traces) {
            if (traces == null || traces.isEmpty()) {
                return;
            }
            String traceId = TraceContext.traceId();
            String parentSpanId = TraceContext.spanId();
            for (ToolTrace trace : traces) {
                Span.Builder span = Span.start(traceId, trace.getName(), SpanKind.TOOL);
                if (parentSpanId != null) {
                    span.parent(parentSpanId);
                }
                span.input(trace.getArgumentsSummary());
                span.attribute("ok", trace.isOk());
                span.attribute("latencyMillis", trace.getLatencyMillis());
                if (!trace.isOk()) {
                    span.status(SpanStatus.ERROR);
                }
                span.end(System.currentTimeMillis());
                endSpan(span);
            }
        }


        private Object invokeModel(LlmRequest request, ToolProvider tools,
                                   LlmStreamHandler streamHandler, boolean jsonResult,
                                   Class<?> returnType, java.lang.reflect.Type genericReturnType) {
            LlmPort llm = context.getLlm();
            // 标注 @JsonResult 且无工具：走稳定 JSON 输出端口（流式 / 非流式分别处理）
            if (context.getJsonOutput() != null && jsonResult
                    && tools == null) {
                if (streamHandler != null) {
                    context.getJsonOutput().streamJson(request, streamHandler);
                    return null;
                }
                String json = context.getJsonOutput().completeJson(request);
                if (returnType == LlmResponse.class) {
                    return LlmResponse.builder().content(json).build();
                }
                if (String.class.equals(returnType)) {
                    return json;
                }
                JsonBindings bindings = context.getJsonBindings();
                if (bindings == null) {
                    throw new IllegalStateException("@JsonResult method declares return type "
                            + returnType.getName() + " but no JsonBindings is configured; "
                            + "either return String/LlmResponse or call Builder.jsonBindings(...)");
                }
                return bindings.bind(json, genericReturnType != null ? genericReturnType : returnType);
            }
            if (tools != null) {
                boolean useJson = context.getJsonOutput() != null && jsonResult;
                List<ChatMessage> toolMessages = request.getMessages();
                if (useJson) {
                    toolMessages = withJsonInstruction(toolMessages, context.getJsonOutput().instruction());
                }
                ToolCallingLoop loop = new ToolCallingLoop(llm, tools);
                ToolCallingRequest tcRequest = ToolCallingRequest.builder()
                        .messages(toolMessages)
                        .model(request.getModel())
                        .build();
                ToolCallingSink sink = streamHandler == null ? null
                        : new LlmStreamHandlerSink(streamHandler);
                ToolCallingResult callResult = loop.call(tcRequest, sink);
                recordToolSpans(callResult.getTraces());
                String answer = callResult.getAnswer();
                if (useJson) {
                    String json = JsonSupport.repairJson(answer);
                    if (json == null) {
                        throw new JsonOutputException("no parseable JSON in tool-calling result");
                    }
                    answer = json;
                }
                if (returnType == LlmResponse.class) {
                    return LlmResponse.builder().content(answer).usage(callResult.getUsage()).build();
                }
                if (String.class.equals(returnType)) {
                    return answer;
                }
                JsonBindings bindings = context.getJsonBindings();
                if (bindings == null) {
                    throw new IllegalStateException("@JsonResult method declares return type "
                            + returnType.getName() + " but no JsonBindings is configured; "
                            + "either return String/LlmResponse or call Builder.jsonBindings(...)");
                }
                return bindings.bind(answer, genericReturnType != null ? genericReturnType : returnType);
            }
            if (streamHandler != null) {
                llm.stream(request).handler(streamHandler).start();
                return null;
            }
            LlmResponse response = llm.complete(request);
            String content = response == null ? null : response.getContent();
            if (returnType == LlmResponse.class) {
                return response;
            }
            return content;
        }

        private String classify(String userText, boolean emit, LlmStreamHandler streamHandler) {
            IntentPort classifier = selectScoped(context.getIntentScopes(), null);
            if (classifier == null || userText == null) {
                return null;
            }
            String parentTraceId = TraceContext.traceId();
            String parentSpanId = TraceContext.spanId();
            Span.Builder intentSpan = Span.start(parentTraceId, "intent", SpanKind.CHAIN);
            if (parentSpanId != null) {
                intentSpan.parent(parentSpanId);
            }
            intentSpan.input(userText);
            TraceContext.bind(intentSpan.getTraceId(), intentSpan.getSpanId());
            try {
                List<Intent> candidates = candidateIntents();
                if (emit) {
                    streamHandler.onIntent(IntentEvent.start(candidates.size()));
                }
                IntentResult result = classifier.classify(IntentRequest.builder()
                        .text(userText).candidates(candidates).build());
                if (emit && result != null) {
                    List<IntentMatch> matches = result.getMatches();
                    for (int i = 0; i < matches.size(); i++) {
                        IntentMatch m = matches.get(i);
                        streamHandler.onIntent(IntentEvent.match(
                                m.getRef(), m.getLabel(), m.getScore(), i + 1, matches.size()));
                    }
                }
                String ref = result == null || result.getTop() == null ? null : result.getTop().getRef();
                intentSpan.output(ref);
                intentSpan.attribute("candidateCount", candidates.size());
                return ref;
            } catch (RuntimeException e) {
                intentSpan.error(errorMessage(e));
                throw e;
            } finally {
                intentSpan.end(System.currentTimeMillis());
                endSpan(intentSpan);
                if (parentTraceId != null) {
                    TraceContext.bind(parentTraceId, parentSpanId);
                } else {
                    TraceContext.unbind();
                }
            }
        }

        /** 汇总各作用域声明的意图引用，作为分类候选。 */
        private List<Intent> candidateIntents() {
            Set<String> refs = new LinkedHashSet<String>();
            for (IntentScoped<ToolProvider> s : context.getToolProviderScopes()) {
                refs.addAll(s.getIntents());
            }
            for (IntentScoped<RetrievalAugmentor> s : context.getRetrievalScopes()) {
                refs.addAll(s.getIntents());
            }
            for (IntentScoped<SkillBox> s : context.getSkillScopes()) {
                refs.addAll(s.getIntents());
            }
            List<Intent> intents = new ArrayList<Intent>();
            for (String ref : refs) {
                intents.add(Intent.of(ref, ref));
            }
            return intents;
        }

        private void moderateInput(String text) {
            ModerationModel model = context.getModerationModel();
            if (model == null || text == null) {
                return;
            }
            for (ModerationListener listener : context.getModerationListeners()) {
                listener.onRequest(text);
            }
            ModerationResult result = model.moderate(text);
            for (ModerationListener listener : context.getModerationListeners()) {
                listener.onResponse(result);
            }
            if (result.isFlagged()) {
                for (ModerationListener listener : context.getModerationListeners()) {
                    listener.onFlagged(result);
                }
                throw new ModerationException(result);
            }
        }

        private void moderateOutput(String text) {
            ModerationModel model = context.getModerationModel();
            if (model == null || text == null) {
                return;
            }
            ModerationResult result = model.moderate(text);
            for (ModerationListener listener : context.getModerationListeners()) {
                listener.onResponse(result);
            }
            if (result.isFlagged()) {
                for (ModerationListener listener : context.getModerationListeners()) {
                    listener.onFlagged(result);
                }
                throw new ModerationException(result);
            }
        }

        private void notifyObservationStart(AiCallObservation.Builder observation) {
            for (AiObservationListener listener : context.getObservationListeners()) {
                listener.onStart(observation.build());
            }
        }

        private void notifyObservationEnd(AiCallObservation.Builder observation) {
            observation.event("end");
            AiCallObservation snapshot = observation.build();
            for (AiObservationListener listener : context.getObservationListeners()) {
                listener.onEnd(snapshot);
            }
            for (AiInvocationSummaryListener listener : context.getSummaryListeners()) {
                try {
                    listener.onSummary(snapshot);
                } catch (RuntimeException ignored) {
                    // 监听器异常不得影响主链路
                }
            }
        }

        private void notifyObservationError(AiCallObservation observation) {
            for (AiObservationListener listener : context.getObservationListeners()) {
                listener.onError(observation);
            }
            for (AiInvocationSummaryListener listener : context.getSummaryListeners()) {
                try {
                    listener.onSummary(observation);
                } catch (RuntimeException ignored) {
                    // 监听器异常不得影响主链路
                }
            }
        }

        /** 把 JSON 指令注入 system 消息（已有则追加，否则前置），不破坏原有提示。 */
        private static List<ChatMessage> withJsonInstruction(List<ChatMessage> messages, String instruction) {
            List<ChatMessage> out = new ArrayList<ChatMessage>(messages);
            boolean injected = false;
            for (int i = 0; i < out.size(); i++) {
                if (out.get(i).getRole() == ChatRole.SYSTEM) {
                    String text = out.get(i).getContent();
                    out.set(i, ChatMessage.system((text == null ? "" : text + "\n\n") + instruction));
                    injected = true;
                    break;
                }
            }
            if (!injected) {
                out.add(0, ChatMessage.system(instruction));
            }
            return out;
        }

        private ToolProvider resolveTools(String intentRef) {
            List<ToolProvider> selected = select(context.getToolProviderScopes(), intentRef);
            if (selected.isEmpty()) {
                return null;
            }
            ToolProvider combined = selected.size() == 1 ? selected.get(0)
                    : CompositeToolProvider.of(selected.toArray(new ToolProvider[0]));
            Set<String> gating = skillGating(intentRef);
            if (!gating.isEmpty()) {
                combined = new SkillGatedToolProvider(combined, gating);
            }
            return combined;
        }

        private Set<String> skillGating(String intentRef) {
            Set<String> allowed = new LinkedHashSet<String>();
            for (SkillBox box : select(context.getSkillScopes(), intentRef)) {
                for (SkillDescriptor d : box.list()) {
                    if (!d.getToolWhitelist().isEmpty()) {
                        allowed.addAll(d.getToolWhitelist());
                    }
                }
            }
            return allowed;
        }

        private RetrievalAugmentor resolveRetrieval(String intentRef) {
            List<RetrievalAugmentor> selected = select(context.getRetrievalScopes(), intentRef);
            return ChainedRetrievalAugmentor.of(selected.toArray(new RetrievalAugmentor[0]));
        }

        private String resolveSkillCatalog(String intentRef, Map<String, Object> vars) {
            List<SkillBox> selected = select(context.getSkillScopes(), intentRef);
            if (selected.isEmpty()) {
                return null;
            }
            SkillBox merged = new SkillBox();
            for (SkillBox box : selected) {
                for (SkillDescriptor d : box.list()) {
                    merged.registry().register(d, true);
                }
            }
            String catalog = merged.describeForPrompt(20);
            if (catalog.isEmpty()) {
                return null;
            }
            String rendered = PromptTemplate.from(catalog).apply(vars);
            return "Available skills (use only when relevant):\n" + rendered;
        }

        private static <T> List<T> select(List<IntentScoped<T>> scopes, String intentRef) {
            List<T> out = new ArrayList<T>();
            if (scopes != null) {
                for (IntentScoped<T> scope : scopes) {
                    if (scope.appliesTo(intentRef)) {
                        out.add(scope.get());
                    }
                }
            }
            return out;
        }

        /**
         * 选出单个作用域组件：命中具体意图的优先，未命中时回退到全局（空意图集）组件。
         * 用于记忆 / 分类器等「同一时刻只应有一个生效」的场景。
         */
        private static <T> T selectScoped(List<IntentScoped<T>> scopes, String intentRef) {
            T fallback = null;
            if (scopes != null) {
                for (IntentScoped<T> scope : scopes) {
                    if (scope.getIntents().isEmpty()) {
                        fallback = scope.get();
                    } else if (scope.appliesTo(intentRef)) {
                        return scope.get();
                    }
                }
            }
            return fallback;
        }

        private Object invokeObjectMethod(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("toString".equals(name)) {
                return "AiService(" + context.getServiceType().getName() + ")";
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(name)) {
                return proxy == (args == null || args.length == 0 ? null : args[0]);
            }
            return null;
        }

        private Map<String, Object> variables(Method method, Object[] args) {
            Map<String, Object> variables = new LinkedHashMap<String, Object>();
            Parameter[] parameters = method.getParameters();
            Object unnamed = null;
            for (int i = 0; i < parameters.length; i++) {
                Object value = args == null || i >= args.length ? null : args[i];
                V v = parameters[i].getAnnotation(V.class);
                if (v != null && v.value() != null) {
                    variables.put(v.value(), value);
                } else if (parameters[i].getAnnotation(MemoryId.class) == null
                        && parameters[i].getAnnotation(V.class) == null
                        && !(value instanceof LlmStreamHandler)) {
                    unnamed = value;
                }
            }
            if (unnamed != null) {
                variables.put("it", unnamed);
            }
            for (Map.Entry<String, Integer> e : context.getVarOverrides().entrySet()) {
                Integer idx = e.getValue();
                if (idx != null && args != null && idx >= 0 && idx < args.length) {
                    variables.put(e.getKey(), args[idx]);
                }
            }
            for (Map.Entry<String, java.util.function.Supplier<Object>> e : context.getCustomVariables().entrySet()) {
                java.util.function.Supplier<Object> s = e.getValue();
                if (s != null) {
                    variables.put(e.getKey(), s.get());
                }
            }
            return variables;
        }

        private String memoryId(Method method, Object[] args, Map<String, Object> baseVariables) {
            java.util.function.Supplier<String> override = context.getMemoryIdOverride();
            if (override != null) {
                return override.get();
            }
            Integer idx = context.getMemoryIdParamIndex();
            if (idx != null && args != null && idx >= 0 && idx < args.length) {
                return args[idx] == null ? null : String.valueOf(args[idx]);
            }
            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i].getAnnotation(MemoryId.class) != null && args != null && i < args.length) {
                    return args[i] == null ? null : String.valueOf(args[i]);
                }
            }
            String varKey = context.getMemoryIdVarKey();
            if (varKey != null && baseVariables != null) {
                Object v = baseVariables.get(varKey);
                if (v != null) {
                    return String.valueOf(v);
                }
            }
            return null;
        }

        private LlmStreamHandler streamHandler(Method method, Object[] args) {
            if (args == null) {
                return null;
            }
            for (Object arg : args) {
                if (arg instanceof LlmStreamHandler) {
                    return (LlmStreamHandler) arg;
                }
            }
            return null;
        }

        private String userPrompt(Method method, Map<String, Object> variables) {
            String override = context.getUserTemplateOverride();
            if (override != null) {
                return PromptTemplate.from(override).apply(variables);
            }
            UserMessage annotation = method.getAnnotation(UserMessage.class);
            if (annotation == null) {
                Object single = variables.get("it");
                if (single != null) {
                    return String.valueOf(single);
                }
                throw new IllegalStateException("missing @UserMessage on method: " + method.getName());
            }
            return PromptTemplate.from(join(annotation.value())).apply(variables);
        }

        private String template(SystemMessage methodLevel, SystemMessage typeLevel) {
            String override = context.getSystemTextOverride();
            if (override != null) {
                return override;
            }
            SystemMessage annotation = methodLevel != null ? methodLevel : typeLevel;
            if (annotation == null) {
                return null;
            }
            return join(annotation.value());
        }

        /** 把内置系统变量并入变量表：now / today / traceId / service，以及已知的 memoryId / intent。 */
        private Map<String, Object> withSystemVars(Map<String, Object> base, String memoryId, String intentRef) {
            Map<String, Object> vars = new LinkedHashMap<String, Object>(base);
            vars.put("now", new Date());
            vars.put("today", LocalDate.now());
            String traceId = TraceContext.traceId();
            if (traceId != null) {
                vars.put("traceId", traceId);
            }
            vars.put("service", context.getServiceType().getSimpleName());
            if (memoryId != null) {
                vars.put("memoryId", memoryId);
            }
            if (intentRef != null) {
                vars.put("intent", intentRef);
            }
            return vars;
        }

        private static String renderTemplate(String raw, Map<String, Object> vars) {
            if (raw == null) {
                return null;
            }
            return PromptTemplate.from(raw).apply(vars);
        }

        private String join(String[] parts) {
            if (parts == null || parts.length == 0) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) {
                    sb.append(System.lineSeparator());
                }
                sb.append(parts[i]);
            }
            return sb.toString();
        }
    }

    /** 一次调用的入参快照：接口代理路径与无类 {@link ChatCall} 路径共用同一条执行管线。 */
    static final class CallInput {
        Method method;
        String userText;
        String systemPromptRaw;
        Map<String, Object> baseVariables;
        String memoryId;
        LlmStreamHandler streamHandler;
        boolean jsonResult;
        Class<?> returnType;
        java.lang.reflect.Type genericReturnType;
    }

    /** 把工具调用环的 {@link ToolCallingSink} 适配到用户传入的 {@link LlmStreamHandler}。 */
    private static final class LlmStreamHandlerSink implements ToolCallingSink {

        private final LlmStreamHandler handler;

        private LlmStreamHandlerSink(LlmStreamHandler handler) {
            this.handler = handler;
        }

        @Override
        public void onDelta(String content, String reasoning) {
            handler.onToken(content, reasoning);
        }

        @Override
        public void onError(String message) {
            handler.onError(message);
        }

        @Override
        public void onToolResponse(ToolExecutionResult result) {
            handler.onToolResponse(result);
        }
    }

    /**
     * 无类（class-free）调用器：不要求预先定义接口，以动态入参驱动与接口代理完全相同的执行管线
     * （意图分类、检索增强、工具 / 记忆按意图选择、JSON 兜底、输入 / 输出审核、链路观测一应俱全）。
     *
     * <p>典型用法：</p>
     * <pre>{@code
     * ChatCall call = AiServices.fluent()
     *         .llm(llm)
     *         .systemText("你是助手，当前时间 {{now}}")
     *         .userTemplate("回答问题：{{question}}")
     *         .tools(toolProvider)
     *         .jsonResult(true)
     *         .buildChatCall();
     * String answer = call.call(Collections.singletonMap("question", "1+1=?"));
     * }</pre>
     */
    public static final class ChatCall {

        private final AiServiceContext context;
        private final boolean jsonResult;
        private final Class<?> returnType;

        ChatCall(AiServiceContext context, boolean jsonResult, Class<?> returnType) {
            this.context = context;
            this.jsonResult = jsonResult;
            this.returnType = returnType == null ? String.class : returnType;
        }

        public String call(String userText) {
            Object r = doCall(userText, null, null);
            return r == null ? null : String.valueOf(r);
        }

        public String call(String userText, String memoryId) {
            Object r = doCall(userText, memoryId, null);
            return r == null ? null : String.valueOf(r);
        }

        public String call(java.util.Map<String, Object> vars) {
            Object r = doCall(vars, null, null);
            return r == null ? null : String.valueOf(r);
        }

        public String call(java.util.Map<String, Object> vars, String memoryId) {
            Object r = doCall(vars, memoryId, null);
            return r == null ? null : String.valueOf(r);
        }

        /** 返回原始结果（{@code LlmResponse} 或 {@code String}），便于取 usage / 元数据。 */
        public Object callRaw(String userText) {
            return doCall(userText, null, null);
        }

        public Object callRaw(java.util.Map<String, Object> vars) {
            return doCall(vars, null, null);
        }

        public void stream(String userText, LlmStreamHandler handler) {
            doCall(userText, null, handler);
        }

        public void stream(java.util.Map<String, Object> vars, LlmStreamHandler handler) {
            doCall(vars, null, handler);
        }

        private Object doCall(Object input, String memoryId, LlmStreamHandler handler) {
            java.util.Map<String, Object> baseVariables = new java.util.LinkedHashMap<String, Object>();
            // 动态自定义变量（每次调用实时计算后并入模板，不依赖固定值）
            for (java.util.Map.Entry<String, java.util.function.Supplier<Object>> e
                    : context.getCustomVariables().entrySet()) {
                if (e.getValue() != null) {
                    baseVariables.put(e.getKey(), e.getValue().get());
                }
            }
            String userText;
            if (input instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> m = (java.util.Map<String, Object>) input;
                baseVariables.putAll(m);
                String tmpl = context.getUserTemplateOverride();
                if (tmpl != null) {
                    userText = PromptTemplate.from(tmpl).apply(baseVariables);
                } else {
                    Object it = m.get("it");
                    userText = it == null ? "" : String.valueOf(it);
                }
            } else {
                userText = input == null ? "" : String.valueOf(input);
            }
            if (memoryId == null) {
                java.util.function.Supplier<String> ov = context.getMemoryIdOverride();
                if (ov != null) {
                    memoryId = ov.get();
                }
            }
            if (memoryId == null) {
                String varKey = context.getMemoryIdVarKey();
                if (varKey != null) {
                    Object v = baseVariables.get(varKey);
                    if (v != null) {
                        memoryId = String.valueOf(v);
                    }
                }
            }
            CallInput ci = new CallInput();
            ci.method = null;
            ci.userText = userText;
            ci.systemPromptRaw = context.getSystemTextOverride();
            ci.baseVariables = baseVariables;
            ci.memoryId = memoryId;
            ci.streamHandler = handler;
            ci.jsonResult = jsonResult;
            ci.returnType = returnType;
            ci.genericReturnType = returnType;
            return new AiServiceInvocationHandler(context).execute(ci);
        }
    }
}
