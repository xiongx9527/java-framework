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
import com.xiongx9527.fw.plus.ai.base.model.ChatRole;
import com.xiongx9527.fw.plus.ai.base.output.JsonOutputException;
import com.xiongx9527.fw.plus.ai.base.output.JsonSupport;
import com.xiongx9527.fw.plus.ai.base.model.moderation.ModerationException;
import com.xiongx9527.fw.plus.ai.base.model.moderation.ModerationListener;
import com.xiongx9527.fw.plus.ai.base.model.moderation.ModerationModel;
import com.xiongx9527.fw.plus.ai.base.model.moderation.ModerationResult;
import com.xiongx9527.fw.plus.ai.base.output.JsonOutputPort;
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
import com.xiongx9527.fw.plus.ai.base.store.Document;
import com.xiongx9527.fw.plus.ai.base.template.PromptTemplate;
import com.xiongx9527.fw.plus.ai.base.tool.CompositeToolProvider;
import com.xiongx9527.fw.plus.ai.base.tool.SkillGatedToolProvider;
import com.xiongx9527.fw.plus.ai.base.tool.ToolCallingLoop;
import com.xiongx9527.fw.plus.ai.base.tool.ToolCallingRequest;
import com.xiongx9527.fw.plus.ai.base.tool.ToolCallingResult;
import com.xiongx9527.fw.plus.ai.base.tool.ToolCallingSink;
import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionResult;
import com.xiongx9527.fw.plus.ai.base.tool.ToolProvider;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AiServices {

    private AiServices() {
    }

    public static <T> Builder<T> builder(Class<T> serviceType) {
        return new Builder<T>(serviceType);
    }

    public static final class Builder<T> {

        private final Class<T> serviceType;
        private LlmPort llm;
        private ChatMemoryProvider chatMemoryProvider;
        private IntentPort intentClassifier;
        private final List<IntentScoped<ToolProvider>> toolProviderScopes = new ArrayList<IntentScoped<ToolProvider>>();
        private final List<IntentScoped<RetrievalAugmentor>> retrievalScopes = new ArrayList<IntentScoped<RetrievalAugmentor>>();
        private final List<IntentScoped<SkillBox>> skillScopes = new ArrayList<IntentScoped<SkillBox>>();
        private List<LlmListener> listeners;
        private boolean emitLifecycleEvents = true;
        private boolean skipToolsWhenRetrievalHit = false;

        private ModerationModel moderationModel;
        private final List<ModerationListener> moderationListeners = new ArrayList<ModerationListener>();
        private JsonOutputPort jsonOutput;
        private final List<AiObservationListener> observationListeners = new ArrayList<AiObservationListener>();

        private Builder(Class<T> serviceType) {
            if (serviceType == null || !serviceType.isInterface()) {
                throw new IllegalArgumentException("ai service type must be an interface");
            }
            this.serviceType = serviceType;
        }

        public Builder<T> llm(LlmPort value) {
            this.llm = value;
            return this;
        }

        public Builder<T> chatMemoryProvider(ChatMemoryProvider value) {
            this.chatMemoryProvider = value;
            return this;
        }

        public Builder<T> intentClassifier(IntentPort value) {
            this.intentClassifier = value;
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
         */
        public Builder<T> mcpClients(ToolArgumentCodec codec, McpClient... clients) {
            if (codec != null && clients != null) {
                for (McpClient client : clients) {
                    if (client != null) {
                        this.toolProviderScopes.add(IntentScoped.global(new McpToolProvider(client, codec)));
                    }
                }
            }
            return this;
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

        /** 接入调用级观测监听（Langfuse / Langsmith 等），用于 trace / 监控。 */
        public Builder<T> observation(AiObservationListener... listeners) {
            if (listeners != null) {
                Collections.addAll(this.observationListeners, listeners);
            }
            return this;
        }

        public T build() {
            if (llm == null) {
                throw new IllegalStateException("llm port is required");
            }
            return serviceType.cast(Proxy.newProxyInstance(
                    serviceType.getClassLoader(), new Class<?>[]{serviceType},
                    new AiServiceInvocationHandler(new AiServiceContext(
                            serviceType, llm, chatMemoryProvider, intentClassifier,
                            toolProviderScopes, retrievalScopes, skillScopes, listeners,
                            emitLifecycleEvents, skipToolsWhenRetrievalHit,
                            moderationModel, moderationListeners, jsonOutput, observationListeners))));
        }
    }

    private static final class AiServiceInvocationHandler implements InvocationHandler {

        private final AiServiceContext context;

        private AiServiceInvocationHandler(AiServiceContext context) {
            this.context = context;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getDeclaringClass() == Object.class) {
                return invokeObjectMethod(proxy, method, args);
            }
            Map<String, Object> variables = variables(method, args);
            String memoryId = memoryId(method, args);
            LlmStreamHandler streamHandler = streamHandler(method, args);
            boolean emit = context.isEmitLifecycleEvents() && streamHandler != null;

            String userText = userPrompt(method, variables);
            moderateInput(userText);
            long startNanos = System.nanoTime();
            AiCallObservation.Builder observation = AiCallObservation.builder()
                    .serviceType(context.getServiceType())
                    .method(method)
                    .userText(userText);
            notifyObservationStart(observation);
            ChatMessage userMessage = ChatMessage.user(userText);

            String intentRef = classify(userText, emit, streamHandler);
            RetrievalAugmentor retrieval = resolveRetrieval(intentRef);
            String skillCatalog = resolveSkillCatalog(intentRef);

            List<ChatMessage> messages = new ArrayList<ChatMessage>();
            String systemPrompt = template(method.getAnnotation(SystemMessage.class),
                    context.getServiceType().getAnnotation(SystemMessage.class));
            if (systemPrompt != null || skillCatalog != null) {
                String sys = systemPrompt == null ? "" : systemPrompt;
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

            ChatMemory memory = context.getChatMemoryProvider() == null || memoryId == null
                    ? null : context.getChatMemoryProvider().get(memoryId);
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

            ToolProvider tools = resolveTools(intentRef);
            if (context.isSkipToolsWhenRetrievalHit() && retrievalInjected) {
                tools = null;
            }

            LlmRequest request = LlmRequest.builder()
                    .messages(messages)
                    .tools(tools == null ? null : tools.specifications())
                    .listeners(context.getListeners())
                    .build();

            Object result;
            try {
                result = invokeModel(request, tools, streamHandler, method);
            } catch (Throwable t) {
                notifyObservationError(observation.intentRef(intentRef)
                        .retrievalInjected(retrievalInjected)
                        .latencyMillis((System.nanoTime() - startNanos) / 1000000L)
                        .error(t.getMessage())
                        .build());
                throw t;
            }

            // 输出审核（结构化 JSON 结果不视为自然语言，跳过）
            if (result instanceof String && !method.isAnnotationPresent(JsonResult.class)) {
                moderateOutput((String) result);
            }

            if (memory != null && result instanceof String) {
                memory.add(ChatMessage.assistant((String) result));
            }

            notifyObservationEnd(observation.intentRef(intentRef)
                    .retrievalInjected(retrievalInjected)
                    .answer(result instanceof String ? (String) result : null)
                    .latencyMillis((System.nanoTime() - startNanos) / 1000000L));
            return result;
        }

        private Object invokeModel(LlmRequest request, ToolProvider tools,
                                   LlmStreamHandler streamHandler, Method method) {
            LlmPort llm = context.getLlm();
            Class<?> returnType = method.getReturnType();
            // 标注 @JsonResult 且无工具：走稳定 JSON 输出端口（流式 / 非流式分别处理）
            if (context.getJsonOutput() != null && method.isAnnotationPresent(JsonResult.class)
                    && tools == null) {
                if (streamHandler != null) {
                    context.getJsonOutput().streamJson(request, streamHandler);
                    return null;
                }
                String json = context.getJsonOutput().completeJson(request);
                if (returnType == LlmResponse.class) {
                    return LlmResponse.builder().content(json).build();
                }
                return json;
            }
            if (tools != null) {
                boolean jsonResult = context.getJsonOutput() != null
                        && method.isAnnotationPresent(JsonResult.class);
                List<ChatMessage> toolMessages = request.getMessages();
                if (jsonResult) {
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
                String answer = callResult.getAnswer();
                if (jsonResult) {
                    String json = JsonSupport.repairJson(answer);
                    if (json == null) {
                        throw new JsonOutputException("no parseable JSON in tool-calling result");
                    }
                    answer = json;
                }
                if (returnType == LlmResponse.class) {
                    return LlmResponse.builder().content(answer).usage(callResult.getUsage()).build();
                }
                return answer;
            }
            if (streamHandler != null) {
                llm.stream(request, streamHandler);
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
            IntentPort classifier = context.getIntentClassifier();
            if (classifier == null || userText == null) {
                return null;
            }
            if (emit) {
                streamHandler.onIntent(IntentEvent.start(candidateIntents().size()));
            }
            IntentResult result = classifier.classify(IntentRequest.builder()
                    .text(userText).candidates(candidateIntents()).build());
            if (emit && result != null) {
                List<IntentMatch> matches = result.getMatches();
                for (int i = 0; i < matches.size(); i++) {
                    IntentMatch m = matches.get(i);
                    streamHandler.onIntent(IntentEvent.match(
                            m.getRef(), m.getLabel(), m.getScore(), i + 1, matches.size()));
                }
            }
            return result == null || result.getTop() == null ? null : result.getTop().getRef();
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
            for (AiObservationListener listener : context.getObservationListeners()) {
                listener.onEnd(observation.build());
            }
        }

        private void notifyObservationError(AiCallObservation observation) {
            for (AiObservationListener listener : context.getObservationListeners()) {
                listener.onError(observation);
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

        private String resolveSkillCatalog(String intentRef) {
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
            return catalog.isEmpty() ? null : "Available skills (use only when relevant):\n" + catalog;
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
            return variables;
        }

        private String memoryId(Method method, Object[] args) {
            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i].getAnnotation(MemoryId.class) != null && args != null && i < args.length) {
                    return args[i] == null ? null : String.valueOf(args[i]);
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
            SystemMessage annotation = methodLevel != null ? methodLevel : typeLevel;
            if (annotation == null) {
                return null;
            }
            return join(annotation.value());
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
}

