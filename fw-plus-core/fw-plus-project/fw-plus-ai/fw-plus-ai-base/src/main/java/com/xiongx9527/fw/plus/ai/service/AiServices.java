package com.xiongx9527.fw.plus.ai.service;

import com.xiongx9527.fw.plus.ai.data.segment.TextSegment;
import com.xiongx9527.fw.plus.ai.exception.IllegalConfigurationException;
import com.xiongx9527.fw.plus.ai.memory.ChatMemory;
import com.xiongx9527.fw.plus.ai.memory.ChatMemoryProvider;
import com.xiongx9527.fw.plus.ai.message.AiMessage;
import com.xiongx9527.fw.plus.ai.message.ChatMessage;
import com.xiongx9527.fw.plus.ai.message.ToolExecutionResultMessage;
import com.xiongx9527.fw.plus.ai.model.chat.ChatModel;
import com.xiongx9527.fw.plus.ai.model.chat.StreamingChatModel;
import com.xiongx9527.fw.plus.ai.model.moderation.Moderation;
import com.xiongx9527.fw.plus.ai.model.moderation.ModerationException;
import com.xiongx9527.fw.plus.ai.model.moderation.ModerationModel;
import com.xiongx9527.fw.plus.ai.rag.DefaultRetrievalAugmentor;
import com.xiongx9527.fw.plus.ai.rag.RetrievalAugmentor;
import com.xiongx9527.fw.plus.ai.retriever.ContentRetriever;
import com.xiongx9527.fw.plus.ai.retriever.Retriever;
import com.xiongx9527.fw.plus.ai.tool.DefaultToolExecutor;
import com.xiongx9527.fw.plus.ai.tool.Tool;
import com.xiongx9527.fw.plus.ai.tool.ToolExecutor;
import com.xiongx9527.fw.plus.ai.tool.ToolProvider;
import com.xiongx9527.fw.plus.ai.tool.ToolSpecification;
import com.xiongx9527.fw.plus.ai.tool.ToolSpecifications;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 声明式 AI 服务，镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.service.AiServices<T>}（结构与官方一致）。
 *
 * <p>出处：langchain4j 1.23.0（builder 累积式装配 + SPI {@code AiServicesFactory}；chat 模型为 {@link ChatModel} /
 * {@link StreamingChatModel}，1.x 已更名）；Spring AI 无直接等价
 * （由 ChatClient 流式 API + {@code @Tool} 承担）；AgentScope 以 agent 服务形态提供。
 *
 * <p>具体代理生成（{@link #build()}）由各后端 adapter 通过 {@link AiServicesFactory}（META-INF/services）实现。
 */
public abstract class AiServices<T> {

    protected static final Object DEFAULT = "default";
    protected final AiServiceContext context;
    private boolean retrieverSet = false;
    private boolean contentRetrieverSet = false;
    private boolean retrievalAugmentorSet = false;

    protected AiServices(AiServiceContext context) { this.context = context; }

    public static <T> T create(Class<T> aiService, ChatModel chatModel) {
        return builder(aiService).chatModel(chatModel).build();
    }

    public static <T> T create(Class<T> aiService, StreamingChatModel streamingChatModel) {
        return builder(aiService).streamingChatModel(streamingChatModel).build();
    }

    public static <T> AiServices<T> builder(Class<T> aiService) {
        return builder(new AiServiceContext(aiService));
    }

    public static <T> AiServices<T> builder(AiServiceContext context) {
        for (AiServicesFactory factory : ServiceLoader.load(AiServicesFactory.class)) {
            return factory.create(context);
        }
        throw new UnsupportedOperationException(
                "No AiServicesFactory on classpath; register META-INF/services/com.xiongx9527.fw.plus.ai.service.AiServicesFactory in a backend adapter");
    }

    public AiServices<T> chatModel(ChatModel chatModel) { this.context.chatModel = chatModel; return this; }

    public AiServices<T> streamingChatModel(StreamingChatModel streamingChatModel) { this.context.streamingChatModel = streamingChatModel; return this; }

    public AiServices<T> systemMessageProvider(Function<Object, String> systemMessageProvider) {
        this.context.systemMessageProvider = systemMessageProvider.andThen(Optional::ofNullable);
        return this;
    }

    public AiServices<T> chatMemory(ChatMemory chatMemory) {
        this.context.chatMemories = new ConcurrentHashMap<>();
        this.context.chatMemories.put(DEFAULT, chatMemory);
        return this;
    }

    public AiServices<T> chatMemoryProvider(ChatMemoryProvider chatMemoryProvider) {
        this.context.chatMemories = new ConcurrentHashMap<>();
        this.context.chatMemoryProvider = chatMemoryProvider;
        return this;
    }

    public AiServices<T> moderationModel(ModerationModel moderationModel) { this.context.moderationModel = moderationModel; return this; }

    public AiServices<T> tools(Object... objectsWithTools) {
        if (this.context.toolProvider != null) {
            throw new IllegalArgumentException("Either the tools or the tool provider can be configured, but not both!");
        }
        return tools(Arrays.asList(objectsWithTools));
    }

    public AiServices<T> tools(Collection<Object> objectsWithTools) {
        if (this.context.toolSpecifications == null) this.context.toolSpecifications = new ArrayList<>();
        if (this.context.toolExecutors == null) this.context.toolExecutors = new HashMap<>();
        for (Object objectWithTool : objectsWithTools) {
            if (objectWithTool instanceof Class) {
                throw IllegalConfigurationException.illegalConfiguration("Tool '%s' must be an object, not a class", objectWithTool);
            }
            for (Method method : objectWithTool.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(Tool.class)) {
                    ToolSpecification toolSpecification = ToolSpecifications.toolSpecificationFrom(method);
                    this.context.toolSpecifications.add(toolSpecification);
                    this.context.toolExecutors.put(toolSpecification.name(), new DefaultToolExecutor(objectWithTool, method));
                }
            }
        }
        return this;
    }

    public AiServices<T> toolProvider(ToolProvider toolProvider) {
        if (this.context.toolSpecifications != null || this.context.toolExecutors != null) {
            throw new IllegalArgumentException("Either the tools or the tool provider can be configured, but not both!");
        }
        this.context.toolProvider = toolProvider;
        return this;
    }

    public AiServices<T> tools(Map<ToolSpecification, ToolExecutor> tools) {
        if (this.context.toolProvider != null) {
            throw new IllegalArgumentException("Either the tools or the tool provider can be configured, but not both!");
        }
        if (this.context.toolSpecifications == null) this.context.toolSpecifications = new ArrayList<>();
        if (this.context.toolExecutors == null) this.context.toolExecutors = new HashMap<>();
        for (Map.Entry<ToolSpecification, ToolExecutor> e : tools.entrySet()) {
            this.context.toolSpecifications.add(e.getKey());
            this.context.toolExecutors.put(e.getKey().name(), e.getValue());
        }
        return this;
    }

    /** @deprecated 使用 {@link #contentRetriever(ContentRetriever)} 替代。 */
    @Deprecated
    public AiServices<T> retriever(Retriever<TextSegment> retriever) {
        if (!this.contentRetrieverSet && !this.retrievalAugmentorSet) {
            if (retriever != null) {
                AiServices<T> withContentRetriever = this.contentRetriever(retriever.toContentRetriever());
                this.retrieverSet = true;
                return withContentRetriever;
            }
            return this;
        }
        throw IllegalConfigurationException.illegalConfiguration("Only one out of [retriever, contentRetriever, retrievalAugmentor] can be set");
    }

    public AiServices<T> contentRetriever(ContentRetriever contentRetriever) {
        if (!this.retrieverSet && !this.retrievalAugmentorSet) {
            if (contentRetriever == null) throw IllegalConfigurationException.illegalConfiguration("contentRetriever must not be null");
            this.contentRetrieverSet = true;
            this.context.retrievalAugmentor = DefaultRetrievalAugmentor.builder().contentRetriever(contentRetriever).build();
            return this;
        }
        throw IllegalConfigurationException.illegalConfiguration("Only one out of [retriever, contentRetriever, retrievalAugmentor] can be set");
    }

    public AiServices<T> retrievalAugmentor(RetrievalAugmentor retrievalAugmentor) {
        if (!this.retrieverSet && !this.contentRetrieverSet) {
            if (retrievalAugmentor == null) throw IllegalConfigurationException.illegalConfiguration("retrievalAugmentor must not be null");
            this.retrievalAugmentorSet = true;
            this.context.retrievalAugmentor = retrievalAugmentor;
            return this;
        }
        throw IllegalConfigurationException.illegalConfiguration("Only one out of [retriever, contentRetriever, retrievalAugmentor] can be set");
    }

    public abstract T build();

    protected void performBasicValidation() {
        if (this.context.chatModel == null && this.context.streamingChatModel == null) {
            throw IllegalConfigurationException.illegalConfiguration("Please specify either chatModel or streamingChatModel");
        }
    }

    public static List<ChatMessage> removeToolMessages(List<ChatMessage> messages) {
        return messages.stream()
                .filter(it -> !(it instanceof ToolExecutionResultMessage))
                .filter(it -> !(it instanceof AiMessage) || ((AiMessage) it).toolExecutionRequests().isEmpty())
                .collect(Collectors.toList());
    }

    public static void verifyModerationIfNeeded(Future<Moderation> moderationFuture) {
        if (moderationFuture != null) {
            try {
                Moderation moderation = moderationFuture.get();
                if (moderation.flagged()) {
                    throw new ModerationException(String.format("Text \"%s\" violates content policy", moderation.flaggedText()));
                }
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
