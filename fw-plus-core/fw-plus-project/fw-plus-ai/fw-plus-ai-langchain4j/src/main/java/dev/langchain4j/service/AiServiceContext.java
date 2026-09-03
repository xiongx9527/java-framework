package dev.langchain4j.service;

import static dev.langchain4j.spi.ServiceHelper.loadFactory;

import dev.langchain4j.Internal;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.moderation.ModerationModel;
import dev.langchain4j.observability.api.AiServiceListenerRegistrar;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.service.guardrail.GuardrailService;
import dev.langchain4j.service.memory.ChatMemoryService;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolService;
import dev.langchain4j.spi.services.AiServiceContextFactory;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

@Internal
public class AiServiceContext {

    private static final Function<Object, Optional<String>> DEFAULT_USER_MESSAGE_PROVIDER = x -> Optional.empty();
    private static final Function<Object, Optional<String>> DEFAULT_SYSTEM_MESSAGE_PROVIDER = x -> Optional.empty();

    public final Class<?> aiServiceClass;
    public final AiServiceListenerRegistrar eventListenerRegistrar = AiServiceListenerRegistrar.newInstance();

    public Class<?> returnType;

    public ChatLanguageModel chatModel;
    public StreamingChatLanguageModel streamingChatModel;

    public Map<Object, ChatMemory> chatMemories;
    public ChatMemoryProvider chatMemoryProvider;
    public ChatMemoryService chatMemoryService;

    public List<ToolSpecification> toolSpecifications;
    public Map<String, ToolExecutor> toolExecutors;
    public ToolProvider toolProvider;
    public ToolService toolService = new ToolService();

    public final GuardrailService.Builder guardrailServiceBuilder;
    private final AtomicReference<GuardrailService> guardrailService = new AtomicReference<>();

    public ModerationModel moderationModel;

    public RetrievalAugmentor retrievalAugmentor;

    public boolean storeRetrievedContentInChatMemory = true;

    public Function<Object, Optional<String>> userMessageProvider = DEFAULT_USER_MESSAGE_PROVIDER;
    public Function<Object, Optional<String>> systemMessageProvider = DEFAULT_SYSTEM_MESSAGE_PROVIDER;
    public Function<InvocationContext, String> systemMessageProviderWithContext = null;

    public BiFunction<String, InvocationContext, String> systemMessageTransformer = null;

    public BiFunction<ChatRequest, Object, ChatRequest> chatRequestTransformer = (req, memId) -> req;

    public AiServiceContext(Class<?> aiServiceClass) {
        this.aiServiceClass = aiServiceClass;
        this.guardrailServiceBuilder = GuardrailService.builder(aiServiceClass);
    }

    private static class FactoryHolder {
        private static final AiServiceContextFactory contextFactory = loadFactory(AiServiceContextFactory.class);
    }

    public static AiServiceContext create(Class<?> aiServiceClass) {
        return FactoryHolder.contextFactory != null
                ? FactoryHolder.contextFactory.create(aiServiceClass)
                : new AiServiceContext(aiServiceClass);
    }

    public boolean hasChatMemory() {
        return chatMemoryService != null || chatMemories != null;
    }

    /** Retains the LangChain4j 0.35 chat memory lookup ABI. */
    public ChatMemory chatMemory(Object memoryId) {
        if (chatMemoryService != null) {
            return chatMemoryService.getOrCreateChatMemory(memoryId);
        }
        if (chatMemories == null) {
            return null;
        }
        ChatMemory memory = chatMemories.get(memoryId);
        if (memory == null && chatMemoryProvider != null) {
            memory = chatMemoryProvider.get(memoryId);
            if (memory != null) {
                ChatMemory existing = chatMemories.putIfAbsent(memoryId, memory);
                if (existing != null) {
                    memory = existing;
                }
            }
        }
        return memory;
    }

    public void initChatMemories(ChatMemory chatMemory) {
        chatMemoryService = new ChatMemoryService(chatMemory);
        chatMemories = new ConcurrentHashMap<Object, ChatMemory>();
        chatMemories.put(ChatMemoryService.DEFAULT, chatMemory);
        chatMemoryProvider = null;
    }

    public void initChatMemories(ChatMemoryProvider chatMemoryProvider) {
        this.chatMemoryProvider = chatMemoryProvider;
        chatMemoryService = new ChatMemoryService(chatMemoryProvider);
        chatMemories = new ConcurrentHashMap<Object, ChatMemory>();
    }

    public ChatModel chatModel() {
        if (chatModel == null) {
            return null;
        }
        return chatModel instanceof ChatModel ? (ChatModel) chatModel : ChatModel.fromLegacy(chatModel);
    }

    public StreamingChatModel streamingChatModel() {
        if (streamingChatModel == null) {
            return null;
        }
        return streamingChatModel instanceof StreamingChatModel
                ? (StreamingChatModel) streamingChatModel
                : StreamingChatModel.fromLegacy(streamingChatModel);
    }

    public void syncLegacyState() {
        if (chatMemoryService == null && chatMemories != null) {
            if (chatMemoryProvider != null) {
                chatMemoryService = new ChatMemoryService(chatMemories, chatMemoryProvider);
            } else {
                ChatMemory defaultMemory = chatMemories.get(ChatMemoryService.DEFAULT);
                if (defaultMemory != null) {
                    chatMemoryService = new ChatMemoryService(chatMemories, defaultMemory);
                }
            }
        }
        if (toolSpecifications != null && toolExecutors != null && toolService.toolSpecifications().isEmpty()) {
            java.util.Map<ToolSpecification, ToolExecutor> tools =
                    new java.util.LinkedHashMap<ToolSpecification, ToolExecutor>();
            for (ToolSpecification specification : toolSpecifications) {
                ToolExecutor executor = toolExecutors.get(specification.name());
                if (executor != null) {
                    tools.put(specification, executor);
                }
            }
            toolService.tools(tools);
        }
        if (toolProvider != null && toolService.toolProviders().isEmpty()) {
            toolService.toolProvider(toolProvider);
        }
    }

    public boolean hasModerationModel() {
        return moderationModel != null;
    }

    public GuardrailService guardrailService() {
        return this.guardrailService.updateAndGet(
                service -> (service != null) ? service : guardrailServiceBuilder.build());
    }
}
