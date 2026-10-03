package dev.langchain4j.model.ollama;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequest;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponse;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.ollama.spi.OllamaStreamingChatModelBuilderFactory;
import dev.langchain4j.model.output.Response;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.ValidationUtils.ensureNotBlank;
import static dev.langchain4j.internal.ValidationUtils.ensureNotEmpty;
import static dev.langchain4j.model.ollama.OllamaMessagesUtils.toOllamaMessages;
import static dev.langchain4j.model.ollama.OllamaMessagesUtils.toOllamaTools;
import static dev.langchain4j.model.ollama.OllamaChatModelListenerUtils.createModelListenerResponse;
import static dev.langchain4j.spi.ServiceHelper.loadFactories;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyList;

import static dev.langchain4j.model.ollama.OllamaChatModelListenerUtils.createModelListenerResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <a href="https://github.com/jmorganca/ollama/blob/main/docs/api.md">Ollama API reference</a>
 * <br>
 * <a href="https://github.com/jmorganca/ollama/blob/main/docs/modelfile.md#valid-parameters-and-values">Ollama API parameters</a>.
 */
public class OllamaStreamingChatModel implements StreamingChatLanguageModel {

    private static final Logger log = LoggerFactory.getLogger(OllamaStreamingChatModel.class);

    private final OllamaClient client;
    private final String modelName;
    private final Options options;
    private final String format;
    private final List<ChatModelListener> listeners;

    public OllamaStreamingChatModel(String baseUrl,
                                    String modelName,
                                    Double temperature,
                                    Integer topK,
                                    Double topP,
                                    Double repeatPenalty,
                                    Integer seed,
                                    Integer numPredict,
                                    Integer numCtx,
                                    List<String> stop,
                                    String format,
                                    Duration timeout,
                                    Boolean logRequests,
                                    Boolean logResponses,
                                    Map<String, String> customHeaders,
                                    List<ChatModelListener> listeners
    ) {
        this.client = OllamaClient.builder()
                .baseUrl(baseUrl)
                .timeout(getOrDefault(timeout, ofSeconds(60)))
                .logRequests(logRequests)
                .logStreamingResponses(logResponses)
                .customHeaders(customHeaders)
                .build();
        this.modelName = ensureNotBlank(modelName, "modelName");
        this.options = Options.builder()
                .temperature(temperature)
                .topK(topK)
                .topP(topP)
                .repeatPenalty(repeatPenalty)
                .seed(seed)
                .numPredict(numPredict)
                .numCtx(numCtx)
                .stop(stop)
                .build();
        this.format = format;
        this.listeners = new ArrayList<>(getOrDefault(listeners, emptyList()));
    }

    public static OllamaStreamingChatModelBuilder builder() {
        for (OllamaStreamingChatModelBuilderFactory factory : loadFactories(OllamaStreamingChatModelBuilderFactory.class)) {
            return factory.get();
        }
        return new OllamaStreamingChatModelBuilder();
    }

    @Override
    public void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {
        generate(messages, null, null, handler);
    }

    @Override
    public void generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications,
                         StreamingResponseHandler<AiMessage> handler) {
        generate(messages, toolSpecifications, null, handler);
    }

    @Override
    public void generate(List<ChatMessage> messages, ToolSpecification toolSpecification,
                         StreamingResponseHandler<AiMessage> handler) {
        generate(messages, null, toolSpecification, handler);
    }

    private void generate(List<ChatMessage> messages,
                          List<ToolSpecification> toolSpecifications,
                          ToolSpecification toolThatMustBeExecuted,
                          StreamingResponseHandler<AiMessage> handler) {
        ensureNotEmpty(messages, "messages");

        ChatRequest.Builder requestBuilder = ChatRequest.builder()
                .model(modelName)
                .messages(toOllamaMessages(messages))
                .options(options)
                .format(format)
                .stream(true);

        if (toolThatMustBeExecuted != null) {
            requestBuilder.tools(toOllamaTools(Collections.singletonList(toolThatMustBeExecuted)));
        } else if (toolSpecifications != null && !toolSpecifications.isEmpty()) {
            requestBuilder.tools(toOllamaTools(toolSpecifications));
        }

        // 上游 OllamaClient 在创建 ChatModelRequest 时硬编码了空 toolSpecifications（见
        // OllamaClient.streamingChat 中 createModelListenerRequest(request, messages, new ArrayList<>())），
        // 导致 ChatModelListener 永远收到空工具列表。这里自己通知 listener，listeners 不再传给 OllamaClient。
        ChatModelRequest modelListenerRequest = ChatModelRequest.builder()
                .model(modelName)
                .temperature(options.getTemperature())
                .topP(options.getTopP())
                .maxTokens(options.getNumPredict())
                .messages(messages)
                .toolSpecifications(toolThatMustBeExecuted != null ? Collections.singletonList(toolThatMustBeExecuted) : toolSpecifications)
                .build();
        Map<Object, Object> attributes = new ConcurrentHashMap<>();
        ChatModelRequestContext requestContext = new ChatModelRequestContext(modelListenerRequest, attributes);
        listeners.forEach(listener -> {
            try {
                listener.onRequest(requestContext);
            } catch (Exception e) {
                log.warn("Exception while calling model listener", e);
            }
        });

        // 用带 listener 通知的 StreamingResponseHandler 包住原始 handler，
        // listeners 传空给 client.streamingChat，避免上游重复触发 response/error 及空工具列表的 onRequest
        StreamingResponseHandler<AiMessage> listenerAwareHandler = new StreamingResponseHandler<AiMessage>() {
            @Override
            public void onNext(String token) {
                handler.onNext(token);
            }

            @Override
            public void onError(Throwable error) {
                ChatModelResponse partialResponse = null;
                listeners.forEach(listener -> {
                    try {
                        listener.onError(new ChatModelErrorContext(error, modelListenerRequest, partialResponse, attributes));
                    } catch (Exception e) {
                        log.warn("Exception while calling model listener", e);
                    }
                });
                handler.onError(error);
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                ChatModelResponse modelListenerResponse = createModelListenerResponse(modelName, response);
                ChatModelResponseContext responseContext = new ChatModelResponseContext(modelListenerResponse, modelListenerRequest, attributes);
                listeners.forEach(listener -> {
                    try {
                        listener.onResponse(responseContext);
                    } catch (Exception e) {
                        log.warn("Exception while calling model listener", e);
                    }
                });
                handler.onComplete(response);
            }
        };

        client.streamingChat(requestBuilder.build(), listenerAwareHandler, Collections.emptyList(), messages);
    }

    public static class OllamaStreamingChatModelBuilder {

        private String baseUrl;
        private String modelName;
        private Double temperature;
        private Integer topK;
        private Double topP;
        private Double repeatPenalty;
        private Integer seed;
        private Integer numPredict;
        private Integer numCtx;
        private List<String> stop;
        private String format;
        private Duration timeout;
        private Map<String, String> customHeaders;
        private Boolean logRequests;
        private Boolean logResponses;
        private List<ChatModelListener> listeners;

        public OllamaStreamingChatModelBuilder() {
            // This is public so it can be extended
            // By default with Lombok it becomes package private
        }

        public OllamaStreamingChatModelBuilder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public OllamaStreamingChatModelBuilder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public OllamaStreamingChatModelBuilder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public OllamaStreamingChatModelBuilder topK(Integer topK) {
            this.topK = topK;
            return this;
        }

        public OllamaStreamingChatModelBuilder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        public OllamaStreamingChatModelBuilder repeatPenalty(Double repeatPenalty) {
            this.repeatPenalty = repeatPenalty;
            return this;
        }

        public OllamaStreamingChatModelBuilder seed(Integer seed) {
            this.seed = seed;
            return this;
        }

        public OllamaStreamingChatModelBuilder numPredict(Integer numPredict) {
            this.numPredict = numPredict;
            return this;
        }

        public OllamaStreamingChatModelBuilder numCtx(Integer numCtx) {
            this.numCtx = numCtx;
            return this;
        }

        public OllamaStreamingChatModelBuilder stop(List<String> stop) {
            this.stop = stop;
            return this;
        }

        public OllamaStreamingChatModelBuilder format(String format) {
            this.format = format;
            return this;
        }

        public OllamaStreamingChatModelBuilder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public OllamaStreamingChatModelBuilder customHeaders(Map<String, String> customHeaders) {
            this.customHeaders = customHeaders;
            return this;
        }

        public OllamaStreamingChatModelBuilder logRequests(Boolean logRequests) {
            this.logRequests = logRequests;
            return this;
        }

        public OllamaStreamingChatModelBuilder logResponses(Boolean logResponses) {
            this.logResponses = logResponses;
            return this;
        }

        public OllamaStreamingChatModelBuilder listeners(List<ChatModelListener> listeners) {
            this.listeners = listeners;
            return this;
        }

        public OllamaStreamingChatModel build() {
            return new OllamaStreamingChatModel(
                    baseUrl,
                    modelName,
                    temperature,
                    topK,
                    topP,
                    repeatPenalty,
                    seed,
                    numPredict,
                    numCtx,
                    stop,
                    format,
                    timeout,
                    logRequests,
                    logResponses,
                    customHeaders,
                    listeners
            );
        }
    }
}
