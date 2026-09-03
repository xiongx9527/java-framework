package dev.langchain4j.agentic.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.AudioContent;
import dev.langchain4j.data.message.VideoContent;
import dev.langchain4j.data.message.PdfFileContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.JacksonChatMessageJsonCodec;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.invocation.LangChain4jManaged;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailRequest;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import dev.langchain4j.observability.api.event.AiServiceEvent;
import dev.langchain4j.observability.api.event.AiServiceResponseReceivedEvent;
import dev.langchain4j.observability.api.DefaultAiServiceListenerRegistrar;
import dev.langchain4j.observability.api.AiServiceListenerRegistrar;
import dev.langchain4j.observability.api.listener.AiServiceListener;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class CoreAgenticApiCompatibilityTest {

    private static final List<String> REQUIRED_TYPES = Arrays.asList(
            "dev.langchain4j.Internal",
            "dev.langchain4j.data.message.JacksonChatMessageJsonCodec",
            "dev.langchain4j.exception.LangChain4jException",
            "dev.langchain4j.guardrail.InputGuardrail",
            "dev.langchain4j.guardrail.OutputGuardrail",
            "dev.langchain4j.guardrail.config.InputGuardrailsConfig",
            "dev.langchain4j.guardrail.config.OutputGuardrailsConfig",
            "dev.langchain4j.internal.DefaultExecutorProvider",
            "dev.langchain4j.invocation.InvocationContext",
            "dev.langchain4j.invocation.InvocationParameters",
            "dev.langchain4j.invocation.LangChain4jManaged",
            "dev.langchain4j.model.chat.ChatModel",
            "dev.langchain4j.model.chat.StreamingChatModel",
            "dev.langchain4j.model.chat.response.StreamingChatResponseHandler",
            "dev.langchain4j.observability.api.event.AiServiceResponseReceivedEvent",
            "dev.langchain4j.observability.api.listener.AiServiceListener",
            "dev.langchain4j.observability.api.listener.AiServiceResponseReceivedListener");

    @Test
    void exposesEveryCoreTypeRequiredByAgentic() throws Exception {
        for (String name : REQUIRED_TYPES) {
            Class<?> type = Class.forName(name);
            assertNotNull(type, name);
        }
    }

    @Test
    void exposesModelBridgesAndInvocationContracts() throws Exception {
        Class<?> chatModel = Class.forName("dev.langchain4j.model.chat.ChatModel");
        assertTrue(chatModel.isInterface());
        assertNotNull(chatModel.getMethod("chat", String.class));
        assertNotNull(chatModel.getMethod("chat", List.class));

        Class<?> streamingModel = Class.forName("dev.langchain4j.model.chat.StreamingChatModel");
        assertTrue(streamingModel.isInterface());
        assertNotNull(streamingModel.getMethod("chat", String.class,
                Class.forName("dev.langchain4j.model.chat.response.StreamingChatResponseHandler")));

        Class<?> managed = Class.forName("dev.langchain4j.invocation.LangChain4jManaged");
        assertNotNull(managed.getMethod("setCurrent", java.util.Map.class));
        assertNotNull(managed.getMethod("removeCurrent"));

        Class<?> context = Class.forName("dev.langchain4j.invocation.InvocationContext");
        assertNotNull(context.getMethod("builder"));
        assertNotNull(context.getMethod("invocationParameters"));
    }

    @Test
    void validationUtilsPreservesLegacyApi() throws Exception {
        Class<?> type = Class.forName("dev.langchain4j.internal.ValidationUtils");
        assertNotNull(type.getMethod("ensureEq", Object.class, Object.class, String.class, Object[].class));
        assertNotNull(type.getMethod("ensureNotNull", Object.class, String.class, Object[].class));
        assertNotNull(type.getMethod("ensureTrue", boolean.class, String.class));
        assertNotNull(type.getMethod("ensureGreaterThanZero", Integer.class, String.class));
        assertNotNull(type.getMethod("ensureBetween", Double.class, double.class, double.class, String.class));
        assertNotNull(type.getMethod("ensureBetween", Integer.class, int.class, int.class, String.class));
        assertNotNull(type.getMethod("ensureBetween", Long.class, long.class, long.class, String.class));
        assertNotNull(type.getMethod("ensureNotEmpty", Object[].class, String.class));
        assertNotNull(type.getMethod("ensureNotEmpty", Object[].class, String.class, Object[].class));
    }

    @Test
    void validationUtilsPreservesLegacyBehavior() {
        dev.langchain4j.internal.ValidationUtils.ensureEq("a", "a", "equal");
        assertEquals(Integer.valueOf(2), Integer.valueOf(dev.langchain4j.internal.ValidationUtils.ensureGreaterThanZero(2, "n")));
        assertEquals(Double.valueOf(0.5), Double.valueOf(dev.langchain4j.internal.ValidationUtils.ensureBetween(0.5, 0, 1, "d")));
        assertEquals("value", dev.langchain4j.internal.ValidationUtils.ensureNotNull("value", "value"));
    }

    @Test
    void utilsPreservesLegacyApi() throws Exception {
        Class<?> type = Class.forName("dev.langchain4j.internal.Utils");
        assertNotNull(type.getMethod("isNullOrBlank", String.class));
        assertNotNull(type.getMethod("isNullOrEmpty", String.class));
        assertNotNull(type.getMethod("isNullOrEmpty", Collection.class));
        assertNotNull(type.getMethod("isNullOrEmpty", Iterable.class));
        assertNotNull(type.getMethod("isNotNullOrBlank", String.class));
        assertNotNull(type.getMethod("isNotNullOrEmpty", String.class));
        assertNotNull(type.getMethod("areNotNullOrBlank", String[].class));
        assertNotNull(type.getMethod("isCollectionEmpty", Collection.class));
        assertNotNull(type.getMethod("repeat", String.class, int.class));
        assertNotNull(type.getMethod("randomUUID"));
        assertNotNull(type.getMethod("generateUUIDFrom", String.class));
        assertNotNull(type.getMethod("ensureTrailingForwardSlash", String.class));
        assertNotNull(type.getMethod("firstChars", String.class, int.class));
        assertNotNull(type.getMethod("readBytes", String.class));
        assertNotNull(type.getMethod("copyIfNotNull", List.class));
        assertNotNull(type.getMethod("copyIfNotNull", Map.class));
        assertNotNull(type.getMethod("copy", java.util.Set.class));
        assertNotNull(type.getMethod("allMethods", Class.class));
    }

    @Test
    void utilsPreservesLegacyBehavior() {
        assertTrue(dev.langchain4j.internal.Utils.isNotNullOrBlank("x"));
        assertTrue(dev.langchain4j.internal.Utils.areNotNullOrBlank("x", "y"));
        assertEquals("abab", dev.langchain4j.internal.Utils.repeat("ab", 2));
        assertEquals("x/", dev.langchain4j.internal.Utils.ensureTrailingForwardSlash("x"));
        assertEquals("abc", dev.langchain4j.internal.Utils.firstChars("abcdef", 3));
        assertEquals("\"x\"", dev.langchain4j.internal.Utils.quoted("x"));
    }

    @Test
    void listenerContextsPreserveModernAndLegacyApi() throws Exception {
        Class<?> request = Class.forName("dev.langchain4j.model.chat.listener.ChatModelRequestContext");
        Class<?> response = Class.forName("dev.langchain4j.model.chat.listener.ChatModelResponseContext");
        Class<?> error = Class.forName("dev.langchain4j.model.chat.listener.ChatModelErrorContext");
        assertNotNull(request.getConstructor(Class.forName("dev.langchain4j.model.chat.listener.ChatModelRequest"), Map.class));
        assertNotNull(request.getMethod("request"));
        assertNotNull(response.getMethod("response"));
        assertNotNull(response.getMethod("request"));
        assertNotNull(error.getMethod("request"));
        assertNotNull(error.getMethod("partialResponse"));
        assertNotNull(request.getMethod("request"));
        assertNotNull(response.getMethod("request"));
        assertNotNull(error.getMethod("request"));
    }

    @Test
    void nonEmptyDefaultParametersFailExplicitlyForLegacyCompatibility() {
        ChatModel model = new ChatModel() {
            @Override public dev.langchain4j.model.output.Response<AiMessage> generate(List<ChatMessage> messages) {
                return new dev.langchain4j.model.output.Response<AiMessage>(AiMessage.from("x"));
            }
            @Override public dev.langchain4j.model.chat.request.ChatRequestParameters defaultRequestParameters() {
                return dev.langchain4j.model.chat.request.DefaultChatRequestParameters.builder()
                        .temperature(Double.valueOf(0.2)).build();
            }
        };
        assertThrows(dev.langchain4j.exception.LangChain4jException.class,
                () -> model.chat(ChatRequest.builder().messages(UserMessage.from("x")).build()));
    }

    @Test
    void chatBridgeDispatchesListeners() {
        final List<String> events = new java.util.ArrayList<String>();
        ChatModel model = new ChatModel() {
            @Override public dev.langchain4j.model.output.Response<AiMessage> generate(List<ChatMessage> messages) { return new dev.langchain4j.model.output.Response<AiMessage>(AiMessage.from("x")); }
            @Override public ChatResponse doChat(ChatRequest request) { return ChatResponse.builder().aiMessage(AiMessage.from("x")).build(); }
            @Override public List<dev.langchain4j.model.chat.listener.ChatModelListener> listeners() {
                return Collections.<dev.langchain4j.model.chat.listener.ChatModelListener>singletonList(new dev.langchain4j.model.chat.listener.ChatModelListener() {
                    @Override public void onRequest(dev.langchain4j.model.chat.listener.ChatModelRequestContext c) { events.add("request"); c.attributes().put("seen", true); }
                    @Override public void onResponse(dev.langchain4j.model.chat.listener.ChatModelResponseContext c) { events.add("response"); assertEquals(Boolean.TRUE, c.attributes().get("seen")); }
                    @Override public void onError(dev.langchain4j.model.chat.listener.ChatModelErrorContext c) { events.add("error"); }
                });
            }
        };
        model.chat(ChatRequest.builder().messages(UserMessage.from("x")).build(),
                dev.langchain4j.model.chat.ChatRequestOptions.builder().addListenerAttribute("seed", true).build());
        assertEquals(Arrays.asList("request", "response"), events);
    }

    @Test
    void unsupportedFeatureExceptionKeepsMessageAndCoreHierarchy() {
        dev.langchain4j.exception.UnsupportedFeatureException exception =
                new dev.langchain4j.exception.UnsupportedFeatureException("unsupported");
        assertEquals("unsupported", exception.getMessage());
        assertTrue(exception instanceof dev.langchain4j.exception.LangChain4jException);
    }

    @Test
    void executorProviderReturnsPlatformExecutor() throws Exception {
        Class<?> provider = Class.forName("dev.langchain4j.internal.DefaultExecutorProvider");
        Method method = provider.getMethod("getDefaultExecutorService");
        assertTrue(Modifier.isStatic(method.getModifiers()));
        Object executor = method.invoke(null);
        assertTrue(executor instanceof java.util.concurrent.ExecutorService);
        ExecutorService service = (ExecutorService) executor;
        Future<?> future = service.submit(new Runnable() {
            @Override public void run() { }
        });
        future.get();
        java.util.concurrent.CountDownLatch daemonCheck = new java.util.concurrent.CountDownLatch(1);
        Future<Boolean> daemon = service.submit(new java.util.concurrent.Callable<Boolean>() {
            @Override public Boolean call() {
                daemonCheck.countDown();
                return Boolean.valueOf(Thread.currentThread().isDaemon());
            }
        });
        daemonCheck.await();
        assertTrue(daemon.get().booleanValue());
        assertFalse(service.isShutdown());
    }

    @Test
    void managedParametersAreThreadLocalAndCleaned() {
        LangChain4jManaged outer = new LangChain4jManaged() { };
        LangChain4jManaged inner = new LangChain4jManaged() { };
        Map<Class<? extends LangChain4jManaged>, LangChain4jManaged> outerValues =
                Collections.<Class<? extends LangChain4jManaged>, LangChain4jManaged>singletonMap(LangChain4jManaged.class, outer);
        Map<Class<? extends LangChain4jManaged>, LangChain4jManaged> innerValues =
                Collections.<Class<? extends LangChain4jManaged>, LangChain4jManaged>singletonMap(LangChain4jManaged.class, inner);
        LangChain4jManaged.setCurrent(outerValues);
        Map<Class<? extends LangChain4jManaged>, LangChain4jManaged> previous = LangChain4jManaged.push(innerValues);
        assertSame(outerValues, previous);
        assertSame(inner, LangChain4jManaged.current(LangChain4jManaged.class));
        LangChain4jManaged.restore(previous);
        assertSame(outer, LangChain4jManaged.current(LangChain4jManaged.class));
        LangChain4jManaged.removeCurrent();

        LangChain4jManaged marker = new LangChain4jManaged() { };
        Map<Class<? extends LangChain4jManaged>, LangChain4jManaged> values =
                Collections.<Class<? extends LangChain4jManaged>, LangChain4jManaged>singletonMap(
                        LangChain4jManaged.class, marker);
        LangChain4jManaged.setCurrent(values);
        try {
            assertSame(values, LangChain4jManaged.current());
            assertSame(marker, LangChain4jManaged.current(LangChain4jManaged.class));
        } finally {
            LangChain4jManaged.removeCurrent();
        }
        assertEquals(null, LangChain4jManaged.current());
    }

    @Test
    void invocationParametersRemainMutableAndContextBuilderPreservesValues() {
        InvocationParameters parameters = InvocationParameters.from("id", "42");
        parameters.put("count", Integer.valueOf(2));
        assertEquals("42", parameters.<String>get("id"));
        assertEquals(Integer.valueOf(2), parameters.<Integer>getOrDefault("count", Integer.valueOf(0)));
        InvocationContext context = InvocationContext.builder()
                .invocationId(java.util.UUID.randomUUID())
                .interfaceName("Assistant")
                .methodName("chat")
                .methodArgument("hello")
                .invocationParameters(parameters)
                .timestampNow()
                .build();
        assertEquals("Assistant", context.interfaceName());
        assertEquals(Collections.singletonList("hello"), context.methodArguments());
        assertSame(parameters, context.invocationParameters());
        assertEquals(context, context.toBuilder().build());
    }

    @Test
    void chatBridgePreservesResponseAndStreamingTokens() {
        final ChatResponse response = ChatResponse.builder().aiMessage(AiMessage.from("done")).build();
        ChatModel model = new ChatModel() {
            @Override public dev.langchain4j.model.output.Response<AiMessage> generate(List<ChatMessage> messages) {
                return new dev.langchain4j.model.output.Response<AiMessage>(response.aiMessage());
            }
            @Override public ChatResponse doChat(ChatRequest request) {
                assertEquals(1, request.messages().size());
                return response;
            }
        };
        assertEquals("done", model.chat("hello"));
        assertEquals("done", model.chat(ChatRequest.builder().messages(UserMessage.from("hello")).build()).aiMessage().text());
        final List<String> listenerEvents = new java.util.ArrayList<String>();
        ChatModel listening = new ChatModel() {
            @Override public dev.langchain4j.model.output.Response<AiMessage> generate(List<ChatMessage> messages) {
                return new dev.langchain4j.model.output.Response<AiMessage>(AiMessage.from("legacy"));
            }
            @Override public ChatResponse doChat(ChatRequest request) {
                assertEquals(Boolean.TRUE, request.messages().get(0) instanceof UserMessage);
                return response;
            }
            @Override public List<dev.langchain4j.model.chat.listener.ChatModelListener> listeners() {
                return Collections.<dev.langchain4j.model.chat.listener.ChatModelListener>singletonList(new dev.langchain4j.model.chat.listener.ChatModelListener() {
                    @Override public void onRequest(dev.langchain4j.model.chat.listener.ChatModelRequestContext c) { listenerEvents.add("request"); }
                    @Override public void onResponse(dev.langchain4j.model.chat.listener.ChatModelResponseContext c) { listenerEvents.add("response"); }
                    @Override public void onError(dev.langchain4j.model.chat.listener.ChatModelErrorContext c) { listenerEvents.add("error"); }
                });
            }
        };
        listening.chat(ChatRequest.builder().messages(UserMessage.from("hello")).build(),
                dev.langchain4j.model.chat.ChatRequestOptions.builder().addListenerAttribute("x", true).build());
        assertEquals(Arrays.asList("request", "response"), listenerEvents);

        final List<String> tokens = new java.util.ArrayList<String>();
        final List<dev.langchain4j.agent.tool.ToolSpecification> streamingTools = new java.util.ArrayList<dev.langchain4j.agent.tool.ToolSpecification>();
        StreamingChatModel streaming = new StreamingChatModel() {
            @Override public void generate(List<ChatMessage> messages,
                                           dev.langchain4j.model.StreamingResponseHandler<AiMessage> handler) {
                handler.onNext("a");
                handler.onNext("b");
                handler.onComplete(new dev.langchain4j.model.output.Response<AiMessage>(response.aiMessage()));
            }
            @Override public void generate(List<ChatMessage> messages,
                                           List<dev.langchain4j.agent.tool.ToolSpecification> tools,
                                           dev.langchain4j.model.StreamingResponseHandler<AiMessage> handler) {
                streamingTools.addAll(tools);
                generate(messages, handler);
            }
            @Override public void doChat(ChatRequest request, StreamingChatResponseHandler handler) {
                handler.onPartialResponse("a");
                handler.onPartialResponse("b");
                handler.onCompleteResponse(response);
            }
        };
        streaming.chat("hello", new StreamingChatResponseHandler() {
            @Override public void onCompleteResponse(ChatResponse completeResponse) { }
            @Override public void onError(Throwable error) { throw new AssertionError(error); }
            @Override public void onPartialResponse(String token) { tokens.add(token); }
        });
        assertEquals(Arrays.asList("a", "b"), tokens);
        StreamingChatModel defaulted = new StreamingChatModel() {
            @Override public void generate(List<ChatMessage> messages,
                                           dev.langchain4j.model.StreamingResponseHandler<AiMessage> handler) { }
            @Override public void doChat(ChatRequest request, StreamingChatResponseHandler handler) { }
            @Override public dev.langchain4j.model.chat.request.ChatRequestParameters defaultRequestParameters() {
                return dev.langchain4j.model.chat.request.DefaultChatRequestParameters.builder()
                        .temperature(Double.valueOf(0.1)).build();
            }
        };
        assertThrows(dev.langchain4j.exception.LangChain4jException.class,
                () -> defaulted.chat("x", new StreamingChatResponseHandler() {
                    @Override public void onCompleteResponse(ChatResponse response) { }
                    @Override public void onError(Throwable error) { }
                    @Override public void onPartialResponse(String token) { }
                }));
        dev.langchain4j.agent.tool.ToolSpecification streamTool = dev.langchain4j.agent.tool.ToolSpecification.builder().name("stream").build();
        streaming.generate(Collections.<ChatMessage>singletonList(UserMessage.from("hello")),
                Collections.singletonList(streamTool), new dev.langchain4j.model.StreamingResponseHandler<AiMessage>() {
                    @Override public void onNext(String token) { }
                    @Override public void onComplete(dev.langchain4j.model.output.Response<AiMessage> complete) { }
                    @Override public void onError(Throwable error) { throw new AssertionError(error); }
                });
        assertEquals(Collections.singletonList(streamTool), streamingTools);
    }

    @Test
    void streamingLegacyBridgeForwardsToolsTokensCompletionAndErrors() {
        final List<dev.langchain4j.agent.tool.ToolSpecification> toolsSeen = new java.util.ArrayList<dev.langchain4j.agent.tool.ToolSpecification>();
        final List<String> tokensSeen = new java.util.ArrayList<String>();
        final List<Throwable> errorsSeen = new java.util.ArrayList<Throwable>();
        final dev.langchain4j.agent.tool.ToolSpecification tool = dev.langchain4j.agent.tool.ToolSpecification.builder().name("lookup").build();
        dev.langchain4j.model.chat.StreamingChatLanguageModel legacy = new dev.langchain4j.model.chat.StreamingChatLanguageModel() {
            @Override public void generate(List<ChatMessage> messages,
                    dev.langchain4j.model.StreamingResponseHandler<AiMessage> handler) {
                handler.onNext("plain");
                handler.onComplete(new dev.langchain4j.model.output.Response<AiMessage>(AiMessage.from("complete")));
            }
            @Override public void generate(List<ChatMessage> messages, List<dev.langchain4j.agent.tool.ToolSpecification> tools,
                    dev.langchain4j.model.StreamingResponseHandler<AiMessage> handler) {
                toolsSeen.addAll(tools);
                handler.onNext("tool-token");
                handler.onComplete(new dev.langchain4j.model.output.Response<AiMessage>(AiMessage.from("tool-complete")));
            }
        };
        StreamingChatModel bridge = StreamingChatModel.fromLegacy(legacy);
        bridge.generate(Collections.<ChatMessage>singletonList(UserMessage.from("x")), Collections.singletonList(tool),
                new dev.langchain4j.model.StreamingResponseHandler<AiMessage>() {
                    @Override public void onNext(String token) { tokensSeen.add(token); }
                    @Override public void onComplete(dev.langchain4j.model.output.Response<AiMessage> response) {
                        assertEquals("tool-complete", response.content().text());
                    }
                    @Override public void onError(Throwable error) { errorsSeen.add(error); }
                });
        assertEquals(Collections.singletonList(tool), toolsSeen);
        assertEquals(Collections.singletonList("tool-token"), tokensSeen);
        assertTrue(errorsSeen.isEmpty());

        dev.langchain4j.model.chat.StreamingChatLanguageModel failingLegacy = new dev.langchain4j.model.chat.StreamingChatLanguageModel() {
            @Override public void generate(List<ChatMessage> messages,
                    dev.langchain4j.model.StreamingResponseHandler<AiMessage> handler) {
                handler.onError(new IllegalStateException("stream-failure"));
            }
        };
        final List<Throwable> failureCallbacks = new java.util.ArrayList<Throwable>();
        StreamingChatModel failingBridge = StreamingChatModel.fromLegacy(failingLegacy);
        failingBridge.generate(Collections.<ChatMessage>singletonList(UserMessage.from("x")),
                new dev.langchain4j.model.StreamingResponseHandler<AiMessage>() {
                    @Override public void onNext(String token) { }
                    @Override public void onComplete(dev.langchain4j.model.output.Response<AiMessage> response) { }
                    @Override public void onError(Throwable error) { failureCallbacks.add(error); }
                });
        assertEquals(1, failureCallbacks.size());
        assertEquals("stream-failure", failureCallbacks.get(0).getMessage());
    }

    @Test
    void legacyModelBridgesForwardToolSpecificationsAndMetadata() {
        final List<dev.langchain4j.agent.tool.ToolSpecification> seenTools = new java.util.ArrayList<dev.langchain4j.agent.tool.ToolSpecification>();
        final dev.langchain4j.agent.tool.ToolSpecification tool = dev.langchain4j.agent.tool.ToolSpecification.builder()
                .name("lookup").description("description").build();
        dev.langchain4j.model.chat.ChatLanguageModel legacy = new dev.langchain4j.model.chat.ChatLanguageModel() {
            @Override public dev.langchain4j.model.output.Response<AiMessage> generate(List<ChatMessage> messages) {
                return new dev.langchain4j.model.output.Response<AiMessage>(AiMessage.from("legacy"));
            }
            @Override public dev.langchain4j.model.output.Response<AiMessage> generate(List<ChatMessage> messages,
                    List<dev.langchain4j.agent.tool.ToolSpecification> tools) {
                seenTools.addAll(tools);
                return new dev.langchain4j.model.output.Response<AiMessage>(AiMessage.from("with-tool"));
            }
        };
        ChatModel bridge = ChatModel.fromLegacy(legacy);
        dev.langchain4j.model.output.Response<AiMessage> response = bridge.generate(
                Collections.<ChatMessage>singletonList(UserMessage.from("hello")),
                Collections.singletonList(tool));
        assertEquals("with-tool", response.content().text());
        assertEquals(Collections.singletonList(tool), seenTools);
    }

    @Test
    void jacksonCodecRoundTripsUserAndAssistantMessages() {
        JacksonChatMessageJsonCodec codec = new JacksonChatMessageJsonCodec();
        List<ChatMessage> messages = Arrays.<ChatMessage>asList(UserMessage.from("hello"), AiMessage.from("world"));
        String json = codec.messagesToJson(messages);
        assertEquals(messages, codec.messagesFromJson(json));
        assertEquals(messages.get(0), codec.messageFromJson(codec.messageToJson(messages.get(0))));
    }

    @Test
    void jacksonCodecRoundTripsToolsAndMultimodalUserContent() {
        JacksonChatMessageJsonCodec codec = new JacksonChatMessageJsonCodec();
        dev.langchain4j.agent.tool.ToolExecutionRequest tool =
                dev.langchain4j.agent.tool.ToolExecutionRequest.builder().id("id").name("lookup")
                        .arguments("{\"q\":\"x\"}").build();
        AiMessage ai = AiMessage.from("answer", Collections.singletonList(tool));
        assertEquals(ai, codec.messageFromJson(codec.messageToJson(ai)));
        UserMessage user = UserMessage.from("alice", Arrays.<dev.langchain4j.data.message.Content>asList(
                TextContent.from("hello"), ImageContent.from("https://example.test/image"),
                AudioContent.from("https://example.test/audio"), VideoContent.from("https://example.test/video"),
                PdfFileContent.from("https://example.test/document.pdf")));
        UserMessage restored = (UserMessage) codec.messageFromJson(codec.messageToJson(user));
        assertEquals(user, restored);
        assertEquals("alice", restored.name());
        assertEquals(5, restored.contents().size());
        ImageContent highUrl = ImageContent.from("https://example.test/high.png", ImageContent.DetailLevel.HIGH);
        ImageContent highBase64 = ImageContent.from("aGVsbG8=", "image/png", ImageContent.DetailLevel.HIGH);
        UserMessage highImages = UserMessage.from(Arrays.<dev.langchain4j.data.message.Content>asList(highUrl, highBase64));
        UserMessage restoredHighImages = (UserMessage) codec.messageFromJson(codec.messageToJson(highImages));
        assertEquals(highImages, restoredHighImages);
        assertEquals(ImageContent.DetailLevel.HIGH, ((ImageContent) restoredHighImages.contents().get(0)).detailLevel());
        assertEquals(ImageContent.DetailLevel.HIGH, ((ImageContent) restoredHighImages.contents().get(1)).detailLevel());
        ToolExecutionResultMessage toolResult = new ToolExecutionResultMessage("id", "lookup", "ok");
        assertEquals(toolResult, codec.messageFromJson(codec.messageToJson(toolResult)));
    }

    @Test
    void inputGuardrailHelpersProduceSuccessfulResult() {
        InputGuardrail guardrail = new InputGuardrail() {
            @Override public InputGuardrailResult validate(UserMessage message) { return successWith("accepted"); }
        };
        InputGuardrailResult result = guardrail.validate(UserMessage.from("hello"));
        assertTrue(result.isSuccess());
        assertEquals("accepted", result.successfulText());
        assertEquals(InputGuardrailResult.Result.SUCCESS_WITH_RESULT, result.result());
    }

    @Test
    void guardrailRetryRepromptFatalAndMessageRemovalContracts() {
        OutputGuardrail guardrail = new OutputGuardrail() {
            @Override public OutputGuardrailResult validate(AiMessage message) {
                return retry("bad");
            }
        };
        OutputGuardrailResult retry = guardrail.validate(AiMessage.from("x"));
        assertTrue(retry.isRetry());
        assertEquals(OutputGuardrailResult.Result.FATAL, retry.result());
        OutputGuardrailResult reprompt = guardrail.reprompt("bad", "try again");
        assertTrue(reprompt.isReprompt());
        assertEquals("try again", reprompt.getReprompt().orElse(null));
        OutputGuardrailResult blocked = reprompt.blockRetry();
        assertTrue(!blocked.isRetry());
        OutputGuardrailResult removed = guardrail.failureWithMessageRemoval("remove");
        assertTrue(removed.shouldRemoveViolatingMessage());
        assertEquals("remove", removed.failures().get(0).message());
        ChatResponse original = ChatResponse.builder().aiMessage(AiMessage.from("old"))
                .tokenUsage(new TokenUsage(1, 2, 3)).finishReason(FinishReason.STOP).build();
        OutputGuardrailRequest request = OutputGuardrailRequest.builder().responseFromLLM(original)
                .chatExecutor(new dev.langchain4j.guardrail.ChatExecutor() {
                    @Override public ChatResponse execute() { return original; }
                    @Override public ChatResponse execute(List<ChatMessage> messages) { return original; }
                }).requestParams(dev.langchain4j.guardrail.GuardrailRequestParams.builder()
                        .userMessageTemplate("{message}").variables(Collections.<String, Object>emptyMap()).build()).build();
        OutputGuardrailRequest rewritten = request.withText("new");
        assertEquals("new", rewritten.responseFromLLM().aiMessage().text());
        assertEquals(original.tokenUsage(), rewritten.responseFromLLM().tokenUsage());
        assertEquals(original.finishReason(), rewritten.responseFromLLM().finishReason());
        assertThrows(IllegalArgumentException.class, () -> guardrail.failure(null));
    }

    @Test
    void registrarDispatchesExactlyByEventClassAndIsolatesListenerFailures() {
        final AiServiceListenerRegistrar registrar = new DefaultAiServiceListenerRegistrar();
        final List<String> calls = new java.util.ArrayList<String>();
        AiServiceListener<AiServiceResponseReceivedEvent> listener = new AiServiceListener<AiServiceResponseReceivedEvent>() {
            @Override public Class<AiServiceResponseReceivedEvent> getEventClass() { return AiServiceResponseReceivedEvent.class; }
            @Override public void onEvent(AiServiceResponseReceivedEvent event) { calls.add("ok"); }
        };
        registrar.register(listener);
        registrar.register(listener);
        assertThrows(NullPointerException.class, () -> registrar.register((AiServiceListener<?>) null));
        assertThrows(NullPointerException.class, () -> registrar.unregister((AiServiceListener<?>) null));
        InvocationContext context = InvocationContext.builder().invocationId(java.util.UUID.randomUUID())
                .interfaceName("A").methodName("m").invocationParameters(new InvocationParameters()).timestampNow().build();
        ChatRequest request = ChatRequest.builder().messages(UserMessage.from("in")).build();
        ChatResponse response = ChatResponse.builder().aiMessage(AiMessage.from("out")).build();
        AiServiceResponseReceivedEvent event = AiServiceResponseReceivedEvent.builder()
                .invocationContext(context).request(request).response(response).build();
        registrar.fireEvent(event);
        assertEquals(Collections.singletonList("ok"), calls);
        registrar.unregister(listener);
        registrar.fireEvent(event);
        assertEquals(Collections.singletonList("ok"), calls);
        registrar.shouldThrowExceptionOnEventError(false);
        registrar.register(new AiServiceListener<AiServiceResponseReceivedEvent>() {
            @Override public Class<AiServiceResponseReceivedEvent> getEventClass() { return AiServiceResponseReceivedEvent.class; }
            @Override public void onEvent(AiServiceResponseReceivedEvent ignored) { throw new IllegalStateException("listener"); }
        });
        registrar.fireEvent(event);
        registrar.shouldThrowExceptionOnEventError(true);
        assertThrows(IllegalStateException.class, () -> registrar.fireEvent(event));
    }

    @Test
    void registrarSupportsConcurrentRegistrationAndDispatch() throws Exception {
        final DefaultAiServiceListenerRegistrar registrar = new DefaultAiServiceListenerRegistrar();
        final InvocationContext context = InvocationContext.builder().invocationId(java.util.UUID.randomUUID())
                .interfaceName("A").methodName("m").invocationParameters(new InvocationParameters()).timestampNow().build();
        final ChatRequest request = ChatRequest.builder().messages(UserMessage.from("in")).build();
        final ChatResponse response = ChatResponse.builder().aiMessage(AiMessage.from("out")).build();
        final AiServiceResponseReceivedEvent event = AiServiceResponseReceivedEvent.builder()
                .invocationContext(context).request(request).response(response).build();
        final java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        AiServiceListener<AiServiceResponseReceivedEvent> listener = new AiServiceListener<AiServiceResponseReceivedEvent>() {
            @Override public Class<AiServiceResponseReceivedEvent> getEventClass() { return AiServiceResponseReceivedEvent.class; }
            @Override public void onEvent(AiServiceResponseReceivedEvent ignored) { calls.incrementAndGet(); }
        };
        java.util.concurrent.ExecutorService workers = java.util.concurrent.Executors.newFixedThreadPool(4);
        try {
            List<Future<?>> futures = new java.util.ArrayList<Future<?>>();
            for (int i = 0; i < 20; i++) {
                futures.add(workers.submit(new Runnable() {
                    @Override public void run() { registrar.register(listener); }
                }));
            }
            for (Future<?> future : futures) future.get();

            futures.clear();
            for (int i = 0; i < 20; i++) {
                futures.add(workers.submit(new Runnable() {
                    @Override public void run() { registrar.fireEvent(event); }
                }));
            }
            for (Future<?> future : futures) future.get();
            assertEquals(20, calls.get());

            futures.clear();
            for (int i = 0; i < 20; i++) {
                futures.add(workers.submit(new Runnable() {
                    @Override public void run() { registrar.unregister(listener); }
                }));
            }
            for (Future<?> future : futures) future.get();
            registrar.fireEvent(event);
            assertEquals(20, calls.get());
        } finally {
            workers.shutdownNow();
        }
        assertThrows(NullPointerException.class, () -> registrar.unregister((AiServiceListener<?>) null));
    }

    @Test
    void coreUtilsFindsAnnotatedProxyMethodsAndConcreteHierarchyMethods() throws Exception {
        java.lang.reflect.Method direct = AnnotatedTool.class.getMethod("direct", String.class);
        assertSame(direct, dev.langchain4j.internal.Utils.getAnnotatedMethod(direct, Deprecated.class).get());

        AnnotatedContract proxy = (AnnotatedContract) java.lang.reflect.Proxy.newProxyInstance(
                AnnotatedContract.class.getClassLoader(),
                new Class<?>[] {AnnotatedContract.class},
                (instance, method, arguments) -> null);
        java.lang.reflect.Method proxyMethod = proxy.getClass().getMethod("proxied", String.class);
        assertEquals(AnnotatedContract.class.getMethod("proxied", String.class),
                dev.langchain4j.internal.Utils.getAnnotatedMethod(proxyMethod, Deprecated.class).get());

        @SuppressWarnings("unchecked")
        java.util.List<java.lang.reflect.Method> allMethods =
                (java.util.List<java.lang.reflect.Method>) dev.langchain4j.internal.Utils.class
                        .getMethod("allMethods", Class.class)
                        .invoke(null, AbstractTool.class);
        java.util.Set<String> allSignatures = new java.util.HashSet<String>();
        for (java.lang.reflect.Method method : allMethods) {
            allSignatures.add(method.getName() + java.util.Arrays.toString(method.getParameterTypes()));
        }
        assertTrue(allSignatures.contains("abstractMethod[]"));
        assertTrue(allSignatures.contains("defaultMethod[]"));
        assertTrue(allSignatures.contains("staticMethod[]"));
        assertThrows(UnsupportedOperationException.class, () -> allMethods.add(direct));

        java.util.List<java.lang.reflect.Method> interfaceConcreteMethods =
                dev.langchain4j.internal.Utils.allConcreteMethods(AbstractTool.class);
        java.util.Set<String> interfaceConcreteSignatures = new java.util.HashSet<String>();
        for (java.lang.reflect.Method method : interfaceConcreteMethods) {
            interfaceConcreteSignatures.add(method.getName() + java.util.Arrays.toString(method.getParameterTypes()));
        }
        assertFalse(interfaceConcreteSignatures.contains("abstractMethod[]"));
        assertTrue(interfaceConcreteSignatures.contains("defaultMethod[]"));
        assertTrue(interfaceConcreteSignatures.contains("staticMethod[]"));

        java.util.Set<String> declaredAbstractSignatures = new java.util.HashSet<String>();
        for (java.lang.reflect.Method method :
                dev.langchain4j.internal.Utils.allMethods(DeclaredAbstractTool.class)) {
            declaredAbstractSignatures.add(method.getName());
        }
        assertFalse(declaredAbstractSignatures.contains("declaredAbstract"));
        assertTrue(declaredAbstractSignatures.contains("declaredConcrete"));

        java.util.List<java.lang.reflect.Method> methods =
                dev.langchain4j.internal.Utils.allConcreteMethods(ConcreteTool.class);
        java.util.Set<String> signatures = new java.util.HashSet<String>();
        for (java.lang.reflect.Method method : methods) {
            signatures.add(method.getName() + java.util.Arrays.toString(method.getParameterTypes()));
            assertFalse(method.isBridge());
            assertFalse(method.isSynthetic());
            assertFalse(java.lang.reflect.Modifier.isAbstract(method.getModifiers()));
        }
        assertTrue(signatures.contains("own[class java.lang.String]"));
        assertTrue(signatures.contains("inherited[int]"));
        assertTrue(signatures.contains("defaultMethod[]"));
        assertTrue(signatures.contains("staticMethod[]"));
        assertEquals(1, signatures.stream().filter(signature -> signature.equals("overridden[]")).count());
        assertTrue(signatures.contains("abstractMethod[]"));
        assertFalse(signatures.contains("toString[]"));
    }

    interface AnnotatedContract {
        @Deprecated
        void proxied(String value);
    }

    static final class AnnotatedTool {
        @Deprecated
        public void direct(String value) {
        }
    }

    interface ToolContract {
        void abstractMethod();
        default void defaultMethod() {
        }
        static void staticMethod() {
        }
    }

    abstract static class AbstractTool implements ToolContract {
    }

    abstract static class DeclaredAbstractTool {
        abstract void declaredAbstract();
        void declaredConcrete() {
        }
    }

    static class ParentTool {
        public void inherited(int value) {
        }
        public void overridden() {
        }
    }

    static final class ConcreteTool extends ParentTool implements ToolContract {
        public void own(String value) {
        }
        @Override public void overridden() {
        }
        @Override public void abstractMethod() {
        }
    }

    @Test
    void toolParameterAnnotationPreservesLegacyAndModernDefaults() throws Exception {
        assertNotNull(dev.langchain4j.agent.tool.P.class.getMethod("value"));
        assertEquals("", dev.langchain4j.agent.tool.P.class.getMethod("name").getDefaultValue());
        assertEquals("", dev.langchain4j.agent.tool.P.class.getMethod("description").getDefaultValue());
        assertEquals(Boolean.TRUE, dev.langchain4j.agent.tool.P.class
                .getMethod("required").getDefaultValue());
        assertEquals(dev.langchain4j.agent.tool.P.NO_DEFAULT,
                dev.langchain4j.agent.tool.P.class.getMethod("defaultValue").getDefaultValue());
        assertFalse(dev.langchain4j.agent.tool.P.NO_DEFAULT.isEmpty());
    }

    @Test
    void toolSpecificationsUseModernParameterNamesDescriptionsAndDefaults() throws Exception {
        Method method = ModernParameterTool.class.getMethod("weather", String.class, String.class);
        dev.langchain4j.agent.tool.ToolSpecification specification =
                dev.langchain4j.agent.tool.ToolSpecifications.toolSpecificationFrom(method);
        dev.langchain4j.agent.tool.ToolParameters parameters = specification.parameters();
        assertTrue(parameters.properties().containsKey("city"));
        assertTrue(parameters.properties().containsKey("units"));
        assertEquals("City description", parameters.properties().get("city").get("description"));
        assertEquals("Units description", parameters.properties().get("units").get("description"));
        assertTrue(parameters.required().contains("city"));
        assertFalse(parameters.required().contains("units"));
        assertFalse(parameters.properties().containsKey("input"));

        Method conflicting = ModernParameterTool.class.getMethod("conflicting", String.class);
        assertThrows(IllegalArgumentException.class,
                () -> dev.langchain4j.agent.tool.ToolSpecifications.toolSpecificationFrom(conflicting));
    }

    public static final class ModernParameterTool {
        @dev.langchain4j.agent.tool.Tool("Gets weather")
        public void weather(
                @dev.langchain4j.agent.tool.P(name = "city", description = "City description") String input,
                @dev.langchain4j.agent.tool.P(
                        name = "units", value = "Units description", defaultValue = "metric") String units) {
        }

        @dev.langchain4j.agent.tool.Tool("Invalid")
        public void conflicting(
                @dev.langchain4j.agent.tool.P(value = "one", description = "two") String value) {
        }
    }

    @Test
    void toolSearchModelsPreserveLegacyAbiAndModernMetadata() throws Exception {
        assertEquals(dev.langchain4j.agent.tool.ToolParameters.class,
                dev.langchain4j.agent.tool.ToolSpecification.class.getMethod("parameters").getReturnType());
        assertNotNull(dev.langchain4j.agent.tool.ToolSpecification.Builder.class
                .getMethod("parameters", dev.langchain4j.agent.tool.ToolParameters.class));
        assertNotNull(dev.langchain4j.agent.tool.ToolSpecification.Builder.class
                .getMethod("addOptionalParameter", String.class,
                        dev.langchain4j.agent.tool.JsonSchemaProperty[].class));

        Map<String, Object> metadata = new java.util.LinkedHashMap<String, Object>();
        metadata.put(dev.langchain4j.agent.tool.ToolSpecification.METADATA_SEARCH_BEHAVIOR,
                dev.langchain4j.agent.tool.SearchBehavior.ALWAYS_VISIBLE);
        dev.langchain4j.agent.tool.ToolSpecification specification =
                dev.langchain4j.agent.tool.ToolSpecification.builder()
                        .name("lookup").description("Lookup").metadata(metadata).strict(Boolean.TRUE).build();
        metadata.clear();
        assertEquals(dev.langchain4j.agent.tool.SearchBehavior.ALWAYS_VISIBLE,
                specification.metadata().get(
                        dev.langchain4j.agent.tool.ToolSpecification.METADATA_SEARCH_BEHAVIOR));
        assertEquals(Boolean.TRUE, specification.strict());
        assertThrows(UnsupportedOperationException.class,
                () -> specification.metadata().put("other", "value"));
        assertEquals(specification, specification.toBuilder().build());

        Map<String, Object> attributes = new java.util.LinkedHashMap<String, Object>();
        attributes.put("found_tools", Arrays.asList("lookup"));
        ToolExecutionResultMessage message = ToolExecutionResultMessage.builder()
                .id("id").toolName("search").text("found").isError(Boolean.FALSE)
                .attributes(attributes).build();
        attributes.clear();
        assertEquals("found", message.text());
        assertEquals(Arrays.asList("lookup"), message.attributes().get("found_tools"));
        assertFalse(message.isError());
        assertEquals(message, message.toBuilder().build());
        assertEquals("legacy", new ToolExecutionResultMessage("id", "tool", "legacy").text());

        assertEquals(Arrays.asList("a", "b"), dev.langchain4j.internal.Utils.merge(
                Collections.singletonList("a"), Collections.singletonList("b")));
        Map<String, String> first = Collections.singletonMap("key", "one");
        Map<String, String> second = Collections.singletonMap("key", "two");
        assertThrows(IllegalArgumentException.class,
                () -> dev.langchain4j.internal.Utils.merge(first, second));
    }

    @Test
    void aiServiceLifecycleAndToolEventsKeepPayloadsAndCopyBuilders() {
        InvocationContext context = InvocationContext.builder().invocationId(java.util.UUID.randomUUID())
                .interfaceName("A").methodName("m").invocationParameters(new InvocationParameters()).timestampNow().build();
        dev.langchain4j.data.message.SystemMessage system =
                dev.langchain4j.data.message.SystemMessage.from("system");
        UserMessage user = UserMessage.from("user");
        dev.langchain4j.observability.api.event.AiServiceStartedEvent started =
                dev.langchain4j.observability.api.event.AiServiceStartedEvent.builder()
                        .invocationContext(context).systemMessage(system).userMessage(user).build();
        assertSame(context, started.invocationContext());
        assertSame(system, started.systemMessage().get());
        assertSame(user, started.userMessage());
        assertEquals(started.systemMessage(), started.toBuilder().build().systemMessage());

        ChatRequest request = ChatRequest.builder().messages(user).build();
        dev.langchain4j.observability.api.event.AiServiceRequestIssuedEvent issued =
                dev.langchain4j.observability.api.event.AiServiceRequestIssuedEvent.builder()
                        .invocationContext(context).request(request).build();
        assertSame(request, issued.request());
        assertSame(request, issued.toBuilder().build().request());

        Object result = new Object();
        dev.langchain4j.observability.api.event.AiServiceCompletedEvent completed =
                dev.langchain4j.observability.api.event.AiServiceCompletedEvent.builder()
                        .invocationContext(context).result(result).build();
        assertSame(result, completed.result().get());
        assertFalse(dev.langchain4j.observability.api.event.AiServiceCompletedEvent.builder()
                .invocationContext(context).result(null).build().result().isPresent());

        IllegalStateException error = new IllegalStateException("failed");
        dev.langchain4j.observability.api.event.AiServiceErrorEvent failed =
                dev.langchain4j.observability.api.event.AiServiceErrorEvent.builder()
                        .invocationContext(context).error(error).build();
        assertSame(error, failed.error());
        assertThrows(IllegalArgumentException.class, () ->
                dev.langchain4j.observability.api.event.AiServiceErrorEvent.builder()
                        .invocationContext(context).error(null).build());

        dev.langchain4j.agent.tool.ToolExecutionRequest toolRequest =
                dev.langchain4j.agent.tool.ToolExecutionRequest.builder()
                        .id("id").name("lookup").arguments("{}").build();
        dev.langchain4j.observability.api.event.ToolExecutedEvent toolEvent =
                dev.langchain4j.observability.api.event.ToolExecutedEvent.builder()
                        .invocationContext(context).request(toolRequest).resultText("ok").build();
        assertSame(toolRequest, toolEvent.request());
        assertEquals("ok", toolEvent.resultText());
        assertEquals(Collections.singletonList(TextContent.from("ok")), toolEvent.resultContents());
        assertThrows(IllegalArgumentException.class, () ->
                dev.langchain4j.observability.api.event.ToolExecutedEvent.builder()
                        .invocationContext(context).request(toolRequest).build());
        assertThrows(IllegalArgumentException.class, () ->
                dev.langchain4j.observability.api.event.ToolExecutedEvent.builder()
                        .invocationContext(context).request(toolRequest).resultText("ok")
                        .resultContents(Collections.<dev.langchain4j.data.message.Content>singletonList(
                                TextContent.from("other"))).build());
    }

    @Test
    void responseEventBuilderKeepsInvocationRequestAndResponse() {
        InvocationContext context = InvocationContext.builder().invocationId(java.util.UUID.randomUUID())
                .interfaceName("A").methodName("m").invocationParameters(new InvocationParameters()).timestampNow().build();
        ChatRequest request = ChatRequest.builder().messages(UserMessage.from("in")).build();
        ChatResponse response = ChatResponse.builder().aiMessage(AiMessage.from("out")).build();
        AiServiceResponseReceivedEvent event = AiServiceResponseReceivedEvent.builder()
                .invocationContext(context).request(request).response(response).build();
        assertSame(context, event.invocationContext());
        assertSame(request, event.request());
        assertSame(response, event.response());
    }

    @Test
    void coreGuardrailExecutorRunsChainRetriesAndPublishesEvents() {
        final java.util.List<dev.langchain4j.observability.api.event.InputGuardrailExecutedEvent> events =
                new java.util.ArrayList<dev.langchain4j.observability.api.event.InputGuardrailExecutedEvent>();
        dev.langchain4j.observability.api.AiServiceListenerRegistrar registrar =
                new dev.langchain4j.observability.api.DefaultAiServiceListenerRegistrar();
        registrar.register(new dev.langchain4j.observability.api.listener.InputGuardrailExecutedListener() {
            @Override public void onEvent(dev.langchain4j.observability.api.event.InputGuardrailExecutedEvent event) {
                events.add(event);
            }
        });
        dev.langchain4j.guardrail.GuardrailRequestParams params =
                dev.langchain4j.guardrail.GuardrailRequestParams.builder()
                        .userMessageTemplate("{message}")
                        .variables(Collections.<String, Object>emptyMap())
                        .invocationContext(InvocationContext.builder().invocationId(java.util.UUID.randomUUID())
                                .interfaceName("Guardrail").methodName("validate")
                                .invocationParameters(new InvocationParameters()).timestampNow().build())
                        .aiServiceListenerRegistrar(registrar).build();
        dev.langchain4j.guardrail.InputGuardrailRequest inputRequest =
                dev.langchain4j.guardrail.InputGuardrailRequest.builder()
                        .userMessage(UserMessage.from("hello")).commonParams(params).build();
        dev.langchain4j.guardrail.InputGuardrail input = new dev.langchain4j.guardrail.InputGuardrail() {
            @Override public dev.langchain4j.guardrail.InputGuardrailResult validate(UserMessage message) {
                return successWith("rewritten");
            }
        };
        dev.langchain4j.guardrail.InputGuardrailExecutor inputExecutor =
                dev.langchain4j.guardrail.InputGuardrailExecutor.builder().guardrails(input).build();
        assertEquals("rewritten", inputExecutor.execute(inputRequest).successfulText());
        assertEquals(1, events.size());
        assertEquals(input.getClass(), events.get(0).guardrailClass());
        assertEquals(input.name(), events.get(0).guardrailName());
        assertTrue(events.get(0).duration().toNanos() >= 0);

        final int[] attempts = new int[] {0};
        dev.langchain4j.guardrail.OutputGuardrail output = new dev.langchain4j.guardrail.OutputGuardrail() {
            @Override public dev.langchain4j.guardrail.OutputGuardrailResult validate(AiMessage message) {
                return attempts[0]++ == 0 ? retry("retry once") : successWith("accepted");
            }
        };
        dev.langchain4j.model.chat.response.ChatResponse response =
                ChatResponse.builder().aiMessage(AiMessage.from("answer")).build();
        dev.langchain4j.guardrail.OutputGuardrailRequest outputRequest =
                dev.langchain4j.guardrail.OutputGuardrailRequest.builder()
                        .responseFromLLM(response)
                        .chatExecutor(new dev.langchain4j.guardrail.ChatExecutor() {
                            @Override public ChatResponse execute() { return response; }
                            @Override public ChatResponse execute(List<ChatMessage> messages) { return response; }
                        }).requestParams(params).build();
        dev.langchain4j.guardrail.OutputGuardrailExecutor outputExecutor =
                dev.langchain4j.guardrail.OutputGuardrailExecutor.builder()
                        .config(dev.langchain4j.guardrail.config.OutputGuardrailsConfig.builder().maxRetries(2).build())
                        .guardrails(output).build();
        assertTrue(outputExecutor.execute(outputRequest).isSuccess());
        assertEquals(2, attempts[0]);
    }

    @Test
    void coreGuardrailExecutorReportsFailureAndPreservesToolContracts() throws Exception {
        assertNotNull(Class.forName("dev.langchain4j.guardrail.AbstractGuardrailExecutor"));
        assertNotNull(Class.forName("dev.langchain4j.guardrail.GuardrailExecutor"));
        assertNotNull(Class.forName("dev.langchain4j.guardrail.InputGuardrailExecutor"));
        assertNotNull(Class.forName("dev.langchain4j.guardrail.OutputGuardrailExecutor"));
        assertNotNull(Class.forName("dev.langchain4j.spi.guardrail.GuardrailExecutorBuilderFactory"));
        assertNotNull(Class.forName("dev.langchain4j.spi.guardrail.InputGuardrailExecutorBuilderFactory"));
        assertNotNull(Class.forName("dev.langchain4j.spi.guardrail.OutputGuardrailExecutorBuilderFactory"));
        assertEquals("TO_LLM", dev.langchain4j.agent.tool.ReturnBehavior.TO_LLM.name());
        assertEquals("IMMEDIATE", dev.langchain4j.agent.tool.ReturnBehavior.IMMEDIATE.name());
        assertEquals("IMMEDIATE_IF_LAST", dev.langchain4j.agent.tool.ReturnBehavior.IMMEDIATE_IF_LAST.name());
        assertNotNull(dev.langchain4j.agent.tool.CompensateFor.class.getMethod("value"));
        Throwable cause = new IllegalArgumentException("bad arguments");
        dev.langchain4j.exception.ToolArgumentsException arguments =
                new dev.langchain4j.exception.ToolArgumentsException("invalid", cause, Integer.valueOf(400));
        assertSame(cause, arguments.getCause());
        assertEquals(Integer.valueOf(400), arguments.errorCode());
        dev.langchain4j.exception.ToolExecutionException execution =
                new dev.langchain4j.exception.ToolExecutionException(cause, Integer.valueOf(500));
        assertSame(cause, execution.getCause());
        assertEquals(Integer.valueOf(500), execution.errorCode());

        dev.langchain4j.guardrail.InputGuardrail failing = new dev.langchain4j.guardrail.InputGuardrail() {
            @Override public dev.langchain4j.guardrail.InputGuardrailResult validate(UserMessage message) {
                return failure("rejected");
            }
        };
        dev.langchain4j.guardrail.GuardrailRequestParams params =
                dev.langchain4j.guardrail.GuardrailRequestParams.builder()
                        .userMessageTemplate("{message}").variables(Collections.<String, Object>emptyMap())
                        .invocationContext(InvocationContext.builder().invocationId(java.util.UUID.randomUUID())
                                .interfaceName("Guardrail").methodName("validate")
                                .invocationParameters(new InvocationParameters()).timestampNow().build()).build();
        dev.langchain4j.guardrail.InputGuardrailRequest request =
                dev.langchain4j.guardrail.InputGuardrailRequest.builder()
                        .userMessage(UserMessage.from("hello")).commonParams(params).build();
        dev.langchain4j.guardrail.InputGuardrailExecutor executor =
                dev.langchain4j.guardrail.InputGuardrailExecutor.builder().guardrails(failing).build();
        assertThrows(dev.langchain4j.guardrail.InputGuardrailException.class, () -> executor.execute(request));
    }

    @Test
    void outputExecutorStopsAtFatalAndEnforcesMaximumRetries() {
        final int[] fatalCalls = new int[] {0};
        dev.langchain4j.guardrail.OutputGuardrail fatal = new dev.langchain4j.guardrail.OutputGuardrail() {
            @Override public dev.langchain4j.guardrail.OutputGuardrailResult validate(AiMessage message) {
                fatalCalls[0]++;
                return fatal("fatal");
            }
        };
        dev.langchain4j.model.chat.response.ChatResponse response =
                ChatResponse.builder().aiMessage(AiMessage.from("answer")).build();
        dev.langchain4j.guardrail.GuardrailRequestParams params =
                dev.langchain4j.guardrail.GuardrailRequestParams.builder()
                        .userMessageTemplate("{message}").variables(Collections.<String, Object>emptyMap())
                        .invocationContext(InvocationContext.builder().invocationId(java.util.UUID.randomUUID())
                                .interfaceName("Guardrail").methodName("validate")
                                .invocationParameters(new InvocationParameters()).timestampNow().build()).build();
        dev.langchain4j.guardrail.OutputGuardrailRequest request =
                dev.langchain4j.guardrail.OutputGuardrailRequest.builder().responseFromLLM(response)
                        .chatExecutor(new dev.langchain4j.guardrail.ChatExecutor() {
                            @Override public ChatResponse execute() { return response; }
                            @Override public ChatResponse execute(List<ChatMessage> messages) { return response; }
                        }).requestParams(params).build();
        dev.langchain4j.guardrail.OutputGuardrailExecutor executor =
                dev.langchain4j.guardrail.OutputGuardrailExecutor.builder()
                        .config(dev.langchain4j.guardrail.config.OutputGuardrailsConfig.builder().maxRetries(3).build())
                        .guardrails(fatal).build();
        assertThrows(dev.langchain4j.guardrail.OutputGuardrailException.class, () -> executor.execute(request));
        assertEquals(1, fatalCalls[0]);

        final int[] retryCalls = new int[] {0};
        dev.langchain4j.guardrail.OutputGuardrail retry = new dev.langchain4j.guardrail.OutputGuardrail() {
            @Override public dev.langchain4j.guardrail.OutputGuardrailResult validate(AiMessage message) {
                retryCalls[0]++;
                return retry("still invalid");
            }
        };
        dev.langchain4j.guardrail.OutputGuardrailExecutor retryExecutor =
                dev.langchain4j.guardrail.OutputGuardrailExecutor.builder()
                        .config(dev.langchain4j.guardrail.config.OutputGuardrailsConfig.builder().maxRetries(2).build())
                        .guardrails(retry).build();
        dev.langchain4j.guardrail.OutputGuardrailException exception = assertThrows(
                dev.langchain4j.guardrail.OutputGuardrailException.class, () -> retryExecutor.execute(request));
        assertEquals(2, retryCalls[0]);
        assertNotNull(exception.result());
        assertTrue(exception.getMessage().contains("maximum number of retries"));
    }

    @Test
    void guardrailSpiAndReflectionFallbacksAreUsableOnJava8() {
        assertNotNull(dev.langchain4j.spi.ServiceHelper.loadFactory(
                dev.langchain4j.spi.observability.AiServiceListenerRegistrarFactory.class));
        dev.langchain4j.spi.classloading.ClassMetadataProviderFactory<java.lang.reflect.Method> metadata =
                dev.langchain4j.classloading.ClassMetadataProvider.getClassMetadataProviderFactory();
        assertTrue(metadata.getNonStaticMethodsOnClass(SampleGuardrail.class).iterator().hasNext());
        SampleGuardrail sample = dev.langchain4j.classinstance.ClassInstanceLoader.getClassInstance(SampleGuardrail.class);
        assertNotNull(sample);
    }

    @Test
    void toolAnnotationPreservesLegacyMembersAndAddsModernPolicies() throws Exception {
        assertEquals(String.class, dev.langchain4j.agent.tool.Tool.class.getMethod("name").getReturnType());
        assertEquals(String[].class, dev.langchain4j.agent.tool.Tool.class.getMethod("value").getReturnType());
        assertEquals(dev.langchain4j.agent.tool.ReturnBehavior.TO_LLM,
                dev.langchain4j.agent.tool.Tool.class.getMethod("returnBehavior").getDefaultValue());
        assertEquals(dev.langchain4j.agent.tool.SearchBehavior.SEARCHABLE,
                dev.langchain4j.agent.tool.Tool.class.getMethod("searchBehavior").getDefaultValue());
        assertEquals("{}", dev.langchain4j.agent.tool.Tool.class.getMethod("metadata").getDefaultValue());
    }

    @Test
    void userAndAiMessagesKeepLegacyFactoriesAndModernCopyBuilders() {
        Map<String, Object> userAttributes = new java.util.LinkedHashMap<String, Object>();
        userAttributes.put("tenant", "one");
        UserMessage user = UserMessage.builder().name("alice").addContent(TextContent.from("hello"))
                .attributes(userAttributes).build();
        userAttributes.clear();
        assertEquals("one", user.attribute("tenant", String.class));
        assertEquals(user, user.toBuilder().build());
        assertSame(user, UserMessage.findLast(Arrays.<ChatMessage>asList(AiMessage.from("a"), user)).get());
        UserMessage replacement = UserMessage.from("replacement");
        List<ChatMessage> original = Arrays.<ChatMessage>asList(user, AiMessage.from("answer"));
        List<ChatMessage> replaced = UserMessage.replaceLast(original, replacement);
        assertEquals(replacement, replaced.get(0));
        assertSame(original, UserMessage.replaceLast(original, null));

        Map<String, Object> aiAttributes = new java.util.LinkedHashMap<String, Object>();
        aiAttributes.put("provider", "test");
        AiMessage ai = AiMessage.builder().text("answer").thinking("reasoning")
                .attributes(aiAttributes).build();
        aiAttributes.clear();
        assertEquals("reasoning", ai.thinking());
        assertEquals("test", ai.attribute("provider", String.class));
        assertTrue(ai.images().isEmpty());
        assertEquals(ai, ai.toBuilder().build());
        assertEquals("changed", ai.withText("changed").text());
        assertEquals("legacy", AiMessage.from("legacy").text());
    }

    @Test
    void chatRequestAndResponseKeepLegacyAccessorsAndModernMetadata() {
        dev.langchain4j.model.chat.request.ChatRequestParameters parameters =
                dev.langchain4j.model.chat.request.DefaultChatRequestParameters.builder()
                        .modelName("model").temperature(Double.valueOf(0.4)).build();
        ChatRequest request = ChatRequest.builder().messages(UserMessage.from("hello"))
                .parameters(parameters).topP(Double.valueOf(0.8)).build();
        assertEquals("model", request.modelName());
        assertEquals(Double.valueOf(0.4), request.temperature());
        assertEquals(Double.valueOf(0.8), request.topP());
        assertEquals(request, request.toBuilder().build());
        assertEquals(request.toolSpecifications(), request.parameters().toolSpecifications());
        assertEquals(request.responseFormat(), request.parameters().responseFormat());

        dev.langchain4j.model.chat.response.ChatResponseMetadata metadata =
                dev.langchain4j.model.chat.response.ChatResponseMetadata.builder()
                        .id("response-id").modelName("model")
                        .tokenUsage(new TokenUsage(1, 2, 3)).finishReason(FinishReason.STOP).build();
        ChatResponse response = ChatResponse.builder().aiMessage(AiMessage.from("answer"))
                .metadata(metadata).build();
        assertSame(metadata, response.metadata());
        assertEquals("response-id", response.id());
        assertEquals("model", response.modelName());
        assertEquals(metadata.tokenUsage(), response.tokenUsage());
        assertEquals(metadata.finishReason(), response.finishReason());
        assertEquals(response, response.toBuilder().build());
        assertEquals("legacy", ChatResponse.builder().aiMessage(AiMessage.from("legacy"))
                .tokenUsage(new TokenUsage(1, 1, 2)).finishReason(FinishReason.STOP).build()
                .aiMessage().text());
    }

    @Test
    void chatMemoryBulkOperationsRetainLegacySingleMessageContract() {
        final List<ChatMessage> messages = new java.util.ArrayList<ChatMessage>();
        dev.langchain4j.memory.ChatMemory memory = new dev.langchain4j.memory.ChatMemory() {
            @Override public Object id() { return "memory"; }
            @Override public void add(ChatMessage message) { messages.add(message); }
            @Override public List<ChatMessage> messages() { return new java.util.ArrayList<ChatMessage>(messages); }
            @Override public void clear() { messages.clear(); }
        };
        memory.add(UserMessage.from("one"), AiMessage.from("two"));
        assertEquals(2, memory.messages().size());
        memory.set(UserMessage.from("replacement"));
        assertEquals(Collections.<ChatMessage>singletonList(UserMessage.from("replacement")), memory.messages());
        memory.set(Arrays.<ChatMessage>asList(UserMessage.from("a"), AiMessage.from("b")));
        assertEquals(2, memory.messages().size());
        assertThrows(IllegalArgumentException.class, () -> memory.set(new ChatMessage[0]));
    }

    @Test
    void ragMetadataKeepsLegacyUserAccessorAndModernInvocationContext() {
        UserMessage user = UserMessage.from("question");
        dev.langchain4j.rag.query.Metadata legacy =
                new dev.langchain4j.rag.query.Metadata(user, "memory", Collections.<ChatMessage>singletonList(user));
        assertSame(user, legacy.userMessage());
        assertSame(user, legacy.chatMessage());
        assertEquals("memory", legacy.chatMemoryId());

        dev.langchain4j.data.message.SystemMessage system =
                dev.langchain4j.data.message.SystemMessage.from("system");
        InvocationContext context = InvocationContext.builder().invocationId(java.util.UUID.randomUUID())
                .chatMemoryId("modern-memory").interfaceName("Assistant").methodName("chat")
                .invocationParameters(InvocationParameters.from("key", "value")).timestampNow().build();
        dev.langchain4j.rag.query.Metadata modern = dev.langchain4j.rag.query.Metadata.builder()
                .chatMessage(user).systemMessage(system).chatMemory(Collections.<ChatMessage>singletonList(user))
                .invocationContext(context).build();
        assertSame(system, modern.systemMessage());
        assertSame(context, modern.invocationContext());
        assertSame(context.invocationParameters(), modern.invocationParameters());
    }

    @Test
    void chatExecutorBuildersExecuteSynchronousModelsAndExposeStreamingVariant() throws Exception {
        final ChatResponse expected = ChatResponse.builder().aiMessage(AiMessage.from("done")).build();
        ChatModel model = new ChatModel() {
            @Override public dev.langchain4j.model.output.Response<AiMessage> generate(List<ChatMessage> messages) {
                return new dev.langchain4j.model.output.Response<AiMessage>(expected.aiMessage());
            }
            @Override public ChatResponse doChat(ChatRequest request) { return expected; }
        };
        InvocationContext context = InvocationContext.builder().invocationId(java.util.UUID.randomUUID())
                .interfaceName("Assistant").methodName("chat")
                .invocationParameters(new InvocationParameters()).timestampNow().build();
        dev.langchain4j.guardrail.ChatExecutor executor = dev.langchain4j.guardrail.ChatExecutor.builder(model)
                .chatRequest(ChatRequest.builder().messages(UserMessage.from("hello")).build())
                .invocationContext(context).build();
        assertSame(expected, executor.execute());
        assertSame(expected, executor.execute(Collections.<ChatMessage>singletonList(UserMessage.from("other"))));
        assertNotNull(dev.langchain4j.guardrail.ChatExecutor.class.getMethod("builder", StreamingChatModel.class));
    }

    @Test
    void streamingChatExecutorAlwaysTerminatesAndPropagatesOriginalError() throws Exception {
        final IllegalStateException modelError = new IllegalStateException("model failed");
        final IllegalArgumentException handlerError = new IllegalArgumentException("handler failed");
        StreamingChatModel model = new StreamingChatModel() {
            @Override public void generate(List<ChatMessage> messages,
                                           dev.langchain4j.model.StreamingResponseHandler<AiMessage> handler) { }
            @Override public void doChat(ChatRequest request, final StreamingChatResponseHandler handler) {
                new Thread(new Runnable() {
                    @Override public void run() { handler.onError(modelError); }
                }).start();
            }
        };
        InvocationContext context = InvocationContext.builder().invocationId(java.util.UUID.randomUUID())
                .interfaceName("Assistant").methodName("chat")
                .invocationParameters(new InvocationParameters()).timestampNow().build();
        final dev.langchain4j.guardrail.ChatExecutor executor = dev.langchain4j.guardrail.ChatExecutor.builder(model)
                .chatRequest(ChatRequest.builder().messages(UserMessage.from("hello")).build())
                .invocationContext(context)
                .errorHandler(error -> { throw handlerError; })
                .build();
        java.util.concurrent.ExecutorService worker = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            Future<ChatResponse> result = worker.submit(new java.util.concurrent.Callable<ChatResponse>() {
                @Override public ChatResponse call() { return executor.execute(); }
            });
            java.util.concurrent.ExecutionException failure = assertThrows(
                    java.util.concurrent.ExecutionException.class,
                    () -> result.get(2, java.util.concurrent.TimeUnit.SECONDS));
            assertSame(modelError, failure.getCause());
            assertEquals(Collections.singletonList(handlerError), Arrays.asList(modelError.getSuppressed()));
        } finally {
            worker.shutdownNow();
        }
    }

    public static class SampleGuardrail {
        public void validate() { }
    }
}
