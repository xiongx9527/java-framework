package dev.langchain4j.agentic.compat;

import dev.langchain4j.service.AiServiceContext;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.ParameterNameResolver;
import dev.langchain4j.service.memory.ChatMemoryAccess;
import dev.langchain4j.service.memory.ChatMemoryService;
import dev.langchain4j.service.tool.BeforeToolExecution;
import dev.langchain4j.service.tool.ToolArgumentsErrorHandler;
import dev.langchain4j.service.tool.ToolExecutionErrorHandler;
import dev.langchain4j.service.tool.ToolService;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.search.ToolSearchRequest;
import dev.langchain4j.service.tool.search.ToolSearchResult;
import dev.langchain4j.service.guardrail.InputGuardrails;
import dev.langchain4j.service.guardrail.OutputGuardrails;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AggregateAgenticApiCompatibilityTest {

    @Test
    public void exposesAgenticAggregateTypesAndBuilderEntryPoints() throws Exception {
        assertNotNull(ParameterNameResolver.class);
        assertNotNull(ChatMemoryAccess.class);
        assertNotNull(ChatMemoryService.class);
        assertNotNull(BeforeToolExecution.class);
        assertNotNull(ToolArgumentsErrorHandler.class);
        assertNotNull(ToolExecutionErrorHandler.class);
        assertNotNull(ToolService.class);
        Method builder = AiServices.class.getMethod("builder", AiServiceContext.class);
        assertTrue(java.lang.reflect.Modifier.isPublic(builder.getModifiers()));
        assertEquals(AiServices.class, builder.getReturnType());
    }

    @Test
    public void memoryServiceKeepsProviderMemoriesIsolatedAndEvictable() {
        final java.util.Map<Object, dev.langchain4j.memory.ChatMemory> created =
                new java.util.concurrent.ConcurrentHashMap<Object, dev.langchain4j.memory.ChatMemory>();
        ChatMemoryService service = new ChatMemoryService(id -> {
            dev.langchain4j.memory.ChatMemory memory =
                    dev.langchain4j.memory.chat.MessageWindowChatMemory.withMaxMessages(10);
            created.put(id, memory);
            return memory;
        });
        dev.langchain4j.memory.ChatMemory first = service.getOrCreateChatMemory("one");
        dev.langchain4j.memory.ChatMemory same = service.getOrCreateChatMemory("one");
        dev.langchain4j.memory.ChatMemory second = service.getOrCreateChatMemory("two");
        assertTrue(first == same);
        assertTrue(first != second);
        assertEquals(2, service.getChatMemoryIDs().size());
        assertTrue(service.evictChatMemory("one") == first);
        assertEquals(null, service.getChatMemory("one"));
        assertEquals(1, service.getChatMemoryIDs().size());
    }

    @Test
    public void aggregateLeafApisKeepModernAndLegacyContracts() throws Exception {
        assertNotNull(AiServiceTool.class.getMethod("returnBehavior"));
        assertNotNull(AiServiceTool.class.getMethod("immediateReturn"));
        assertEquals(java.lang.annotation.RetentionPolicy.RUNTIME,
                InputGuardrails.class.getAnnotation(java.lang.annotation.Retention.class).value());
        assertEquals(2, OutputGuardrails.class.getMethod("maxRetries").getDefaultValue());
        assertNotNull(Class.forName("dev.langchain4j.spi.services.AiServiceContextFactory")
                .getMethod("create", Class.class));

        for (String method : Arrays.asList("getRawClass", "typeHasRawClass",
                "resolveFirstGenericParameterClass", "validateReturnTypesAreProperlyParametrized")) {
            assertTrue(Arrays.stream(dev.langchain4j.service.TypeUtils.class.getMethods())
                    .anyMatch(candidate -> candidate.getName().equals(method)));
        }
        assertNotNull(dev.langchain4j.service.TypeUtils.class
                .getMethod("resolveFirstGenericParameterType", java.lang.reflect.Type.class));
        assertNotNull(dev.langchain4j.service.TypeUtils.class
                .getMethod("isImageType", Class.class));

        ToolSearchResult result = new ToolSearchResult(Arrays.asList("one", "two"), "found");
        assertEquals(Arrays.asList("one", "two"), result.foundToolNames());
        assertEquals("found", result.toolResultMessageText());
        assertNotNull(ToolSearchRequest.class.getMethod("builder"));
    }

    @Test
    public void aiServiceCoreTypesPreserveLegacyAndModernContracts() throws Exception {
        final dev.langchain4j.model.chat.ChatLanguageModel legacyModel =
                new dev.langchain4j.model.chat.ChatLanguageModel() {
                    @Override
                    public dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> generate(
                            java.util.List<dev.langchain4j.data.message.ChatMessage> messages) {
                        return dev.langchain4j.model.output.Response.from(
                                dev.langchain4j.data.message.AiMessage.from("legacy"));
                    }
                };
        dev.langchain4j.service.AiServiceContext context =
                new dev.langchain4j.service.AiServiceContext(LegacyContextAssistant.class);
        context.chatModel = legacyModel;
        assertEquals("legacy", context.chatModel()
                .chat(dev.langchain4j.model.chat.request.ChatRequest.builder()
                        .messages(dev.langchain4j.data.message.UserMessage.from("hello"))
                        .build())
                .aiMessage().text());

        assertNotNull(dev.langchain4j.service.AiServices.class.getMethod(
                "create", Class.class, dev.langchain4j.model.chat.ChatLanguageModel.class));
        assertNotNull(dev.langchain4j.service.AiServices.class.getMethod(
                "chatLanguageModel", dev.langchain4j.model.chat.ChatLanguageModel.class));
        assertNotNull(dev.langchain4j.service.AiServices.class.getMethod(
                "streamingChatLanguageModel", dev.langchain4j.model.chat.StreamingChatLanguageModel.class));
        assertNotNull(dev.langchain4j.service.TokenStream.class.getMethod(
                "onNext", java.util.function.Consumer.class));
        assertNotNull(dev.langchain4j.service.TokenStream.class.getMethod(
                "onComplete", java.util.function.Consumer.class));

        dev.langchain4j.agent.tool.ToolSpecification specification =
                dev.langchain4j.agent.tool.ToolSpecification.builder().name("legacy-tool").build();
        dev.langchain4j.service.tool.ToolExecutor executor = (request, memoryId) -> "done";
        context.toolSpecifications = java.util.Collections.singletonList(specification);
        context.toolExecutors = java.util.Collections.singletonMap("legacy-tool", executor);
        context.chatMemories = new java.util.concurrent.ConcurrentHashMap<Object,
                dev.langchain4j.memory.ChatMemory>();
        dev.langchain4j.memory.ChatMemory existingMemory =
                dev.langchain4j.memory.chat.MessageWindowChatMemory.withMaxMessages(10);
        context.chatMemories.put("existing-memory", existingMemory);
        context.chatMemoryProvider = ignored ->
                dev.langchain4j.memory.chat.MessageWindowChatMemory.withMaxMessages(10);
        assertNotNull(dev.langchain4j.service.AiServices.builder(context).build());
        assertEquals(java.util.Collections.singletonList(specification), context.toolService.toolSpecifications());
        assertTrue(context.toolService.toolExecutors().get("legacy-tool") == executor);
        assertTrue(context.chatMemory("existing-memory") == existingMemory);
        assertTrue(context.chatMemoryService.getChatMemory("existing-memory") == existingMemory);
        assertNotNull(context.chatMemory("legacy-memory"));
        assertNotNull(context.chatMemoryService.getChatMemory("legacy-memory"));

        dev.langchain4j.service.AiServiceContext nullMemoryContext =
                new dev.langchain4j.service.AiServiceContext(LegacyContextAssistant.class);
        nullMemoryContext.chatMemories = new java.util.concurrent.ConcurrentHashMap<Object,
                dev.langchain4j.memory.ChatMemory>();
        nullMemoryContext.chatMemoryProvider = ignored -> null;
        assertEquals(null, nullMemoryContext.chatMemory("missing-memory"));
        assertTrue(nullMemoryContext.chatMemories.isEmpty());

        dev.langchain4j.service.AiServiceContext mapOnlyContext =
                new dev.langchain4j.service.AiServiceContext(LegacyContextAssistant.class);
        mapOnlyContext.chatModel = legacyModel;
        mapOnlyContext.chatMemories = new java.util.concurrent.ConcurrentHashMap<Object,
                dev.langchain4j.memory.ChatMemory>();
        dev.langchain4j.memory.ChatMemory defaultMemory =
                dev.langchain4j.memory.chat.MessageWindowChatMemory.withMaxMessages(10);
        dev.langchain4j.memory.ChatMemory mappedMemory =
                dev.langchain4j.memory.chat.MessageWindowChatMemory.withMaxMessages(10);
        mapOnlyContext.chatMemories.put(
                dev.langchain4j.service.memory.ChatMemoryService.DEFAULT, defaultMemory);
        mapOnlyContext.chatMemories.put("legacy-id", mappedMemory);
        assertNotNull(dev.langchain4j.service.AiServices.builder(mapOnlyContext).build());
        assertTrue(mapOnlyContext.chatMemory("legacy-id") == mappedMemory);
        assertTrue(mapOnlyContext.chatMemoryService.getOrCreateChatMemory("legacy-id") == mappedMemory);
        assertTrue(mapOnlyContext.chatMemory("other-id") == defaultMemory);

        @SuppressWarnings("rawtypes")
        java.util.List legacyToolObjects = java.util.Collections.singletonList(new LegacyToolTarget());
        dev.langchain4j.service.AiServiceContext legacyToolsContext =
                new dev.langchain4j.service.AiServiceContext(LegacyContextAssistant.class);
        legacyToolsContext.chatModel = legacyModel;
        dev.langchain4j.service.AiServices<LegacyContextAssistant> legacyBuilder =
                dev.langchain4j.service.AiServices.builder(legacyToolsContext);
        dev.langchain4j.service.AiServices.class.getMethod("tools", java.util.List.class)
                .invoke(legacyBuilder, legacyToolObjects);
        assertEquals("legacyLookup", legacyToolsContext.toolService.toolSpecifications().get(0).name());
    }

    @Test
    public void toolOverloadsKeepLegacyListSourceCompatibilityAndModernGenericSignature() throws Exception {
        dev.langchain4j.service.AiServiceContext context =
                dev.langchain4j.service.AiServiceContext.create(LegacyContextAssistant.class);
        dev.langchain4j.service.AiServices<LegacyContextAssistant> builder =
                dev.langchain4j.service.AiServices.builder(context);
        java.util.List<Object> legacyTools =
                java.util.Collections.<Object>singletonList(new LegacyToolTarget());

        builder.tools(legacyTools);

        assertEquals("legacyLookup", context.toolService.toolSpecifications().get(0).name());
        java.lang.reflect.Method modernListOverload =
                dev.langchain4j.service.AiServices.class.getMethod("tools", java.util.List.class);
        java.lang.reflect.ParameterizedType listType = (java.lang.reflect.ParameterizedType)
                modernListOverload.getGenericParameterTypes()[0];
        assertEquals(dev.langchain4j.service.tool.AiServiceTool.class,
                listType.getActualTypeArguments()[0]);
    }

    @Test
    public void toolExecutionModelsKeepLegacyAndModernContracts() {
        dev.langchain4j.agent.tool.ToolExecutionRequest request =
                dev.langchain4j.agent.tool.ToolExecutionRequest.builder()
                        .id("id").name("legacy-tool").arguments("{}").build();
        dev.langchain4j.service.tool.ToolExecution execution =
                dev.langchain4j.service.tool.ToolExecution.builder()
                        .request(request).result("legacy-result").build();
        assertTrue(execution.request() == request);
        assertEquals("legacy-result", execution.result());
        assertEquals(null, execution.invocationContext());

        dev.langchain4j.service.tool.ToolExecution nullLegacyExecution =
                dev.langchain4j.service.tool.ToolExecution.builder()
                        .request(null).result((String) null).build();
        assertEquals(null, nullLegacyExecution.request());
        assertEquals(null, nullLegacyExecution.result());
        assertEquals(null, nullLegacyExecution.resultObject());
    }

    @Test
    public void toolHandlersExposeTwoArgumentFunctionalContracts() throws Exception {
        Method arguments = ToolArgumentsErrorHandler.class.getMethod("handle", Throwable.class,
                Class.forName("dev.langchain4j.service.tool.ToolErrorContext"));
        Method execution = ToolExecutionErrorHandler.class.getMethod("handle", Throwable.class,
                Class.forName("dev.langchain4j.service.tool.ToolErrorContext"));
        assertEquals(2, arguments.getParameterTypes().length);
        assertEquals(2, execution.getParameterTypes().length);
        assertEquals("handle", arguments.getName());
        assertEquals("handle", execution.getName());
    }

    @Test
    public void compileClosureTypesExposeRealModernBehavior() throws Exception {
        dev.langchain4j.agent.tool.ToolExecutionRequest hallucinated =
                dev.langchain4j.agent.tool.ToolExecutionRequest.builder()
                        .id("id").name("missing-tool").arguments("{}").build();
        try {
            dev.langchain4j.service.tool.HallucinatedToolNameStrategy.THROW_EXCEPTION.apply(hallucinated);
            throw new AssertionError("hallucinated tool strategy must fail");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("missing-tool"));
        }

        Method method = AggregateAgenticApiCompatibilityTest.class.getDeclaredMethod(
                "templateVariables", String.class, java.util.Map.class);
        java.util.Map<String, Object> details = new java.util.LinkedHashMap<String, Object>();
        details.put("city", "Paris");
        java.util.Map<String, Object> variables =
                dev.langchain4j.service.InternalReflectionVariableResolver.findTemplateVariables(
                        "{{name}} {{city}}", method, new Object[] {"Alice", details});
        assertEquals("Alice", variables.get("name"));
        assertEquals("Paris", variables.get("city"));
        assertTrue(variables.get("details") == details);

        dev.langchain4j.agent.tool.ToolSpecification tool =
                dev.langchain4j.agent.tool.ToolSpecification.builder().name("lookup").build();
        dev.langchain4j.service.tool.ToolServiceContext toolContext =
                dev.langchain4j.service.tool.ToolServiceContext.builder()
                        .effectiveTools(java.util.Collections.singletonList(tool))
                        .availableTools(java.util.Collections.singletonList(tool))
                        .toolExecutors(java.util.Collections.<String, dev.langchain4j.service.tool.ToolExecutor>emptyMap())
                        .build();
        Object methodKey = new Object();
        java.util.List<dev.langchain4j.data.message.ChatMessage> messages =
                java.util.Collections.<dev.langchain4j.data.message.ChatMessage>singletonList(
                        dev.langchain4j.data.message.UserMessage.from("hello"));
        dev.langchain4j.service.AiServiceTokenStreamParameters parameters =
                dev.langchain4j.service.AiServiceTokenStreamParameters.builder()
                        .messages(messages).toolServiceContext(toolContext).methodKey(methodKey).build();
        assertTrue(parameters.messages() == messages);
        assertTrue(parameters.toolServiceContext() == toolContext);
        assertEquals(java.util.Collections.singletonList(tool), parameters.effectiveTools());
        assertTrue(parameters.methodKey() == methodKey);

        Class<?> reprompt = Class.forName("dev.langchain4j.service.ToolAwareRepromptExecutor");
        assertTrue(java.lang.reflect.Modifier.isFinal(reprompt.getModifiers()));
        assertTrue(!java.lang.reflect.Modifier.isPublic(reprompt.getModifiers()));
    }

    @Test
    public void toolExecutorsPreserveLegacyExecutionAndAddInvocationContext() throws Exception {
        final Object memoryId = new Object();
        dev.langchain4j.service.tool.ToolExecutor legacy =
                new dev.langchain4j.service.tool.ToolExecutor() {
                    @Override
                    public String execute(dev.langchain4j.agent.tool.ToolExecutionRequest request, Object actualMemoryId) {
                        assertTrue(actualMemoryId == memoryId);
                        return "legacy-result";
                    }
                };
        dev.langchain4j.invocation.InvocationContext invocationContext =
                dev.langchain4j.invocation.InvocationContext.builder().chatMemoryId(memoryId).build();
        dev.langchain4j.agent.tool.ToolExecutionRequest request =
                dev.langchain4j.agent.tool.ToolExecutionRequest.builder()
                        .id("id").name("calculate").arguments("{\"number\": 5}").build();
        assertEquals("legacy-result", legacy.executeWithContext(request, invocationContext).resultText());

        ToolTarget target = new ToolTarget();
        Method method = ToolTarget.class.getMethod(
                "calculate", int.class, String.class, dev.langchain4j.invocation.InvocationContext.class);
        dev.langchain4j.service.tool.DefaultToolExecutor executor =
                dev.langchain4j.service.tool.DefaultToolExecutor.builder()
                        .object(target).originalMethod(method).methodToInvoke(method).build();
        dev.langchain4j.service.tool.ToolExecutionResult result = executor.executeWithContext(request, invocationContext);
        assertEquals("5:metric:true", result.resultText());
        assertTrue(executor.originalMethod() == method);
        assertEquals("5:metric:true", new dev.langchain4j.service.tool.DefaultToolExecutor(target, method)
                .execute(request, memoryId));
    }

    @Test
    public void toolProvidersKeepLegacyMapApiAndModernInvocationTools() {
        final dev.langchain4j.agent.tool.ToolSpecification specification =
                dev.langchain4j.agent.tool.ToolSpecification.builder().name("lookup").build();
        dev.langchain4j.service.tool.ToolExecutor executor = (request, memoryId) -> "ok";
        java.util.Map<dev.langchain4j.agent.tool.ToolSpecification,
                dev.langchain4j.service.tool.ToolExecutor> legacyTools =
                new java.util.LinkedHashMap<dev.langchain4j.agent.tool.ToolSpecification,
                        dev.langchain4j.service.tool.ToolExecutor>();
        legacyTools.put(specification, executor);
        dev.langchain4j.service.tool.ToolProviderResult legacy =
                new dev.langchain4j.service.tool.ToolProviderResult(legacyTools);
        assertEquals(legacyTools, legacy.tools());
        assertEquals(executor, legacy.toolExecutorByName("lookup"));

        dev.langchain4j.service.tool.AiServiceTool immediate =
                dev.langchain4j.service.tool.AiServiceTool.builder()
                        .toolSpecification(specification).toolExecutor(executor)
                        .returnBehavior(dev.langchain4j.agent.tool.ReturnBehavior.IMMEDIATE).build();
        dev.langchain4j.service.tool.ToolProviderResult modern =
                dev.langchain4j.service.tool.ToolProviderResult.builder().add(immediate).build();
        assertEquals(java.util.Collections.singletonList(immediate), modern.aiServiceTools());
        assertTrue(modern.immediateReturnToolNames().contains("lookup"));
        try {
            dev.langchain4j.service.tool.ToolProviderResult.builder().add(immediate).add(immediate).build();
            throw new AssertionError("duplicate tool names must fail");
        } catch (dev.langchain4j.service.IllegalConfigurationException expected) {
            assertTrue(expected.getMessage().contains("lookup"));
        }

        dev.langchain4j.data.message.UserMessage user =
                dev.langchain4j.data.message.UserMessage.from("hello");
        dev.langchain4j.invocation.InvocationContext context =
                dev.langchain4j.invocation.InvocationContext.builder().chatMemoryId("memory")
                        .invocationParameters(new dev.langchain4j.invocation.InvocationParameters()).build();
        dev.langchain4j.service.tool.ToolProviderRequest request =
                dev.langchain4j.service.tool.ToolProviderRequest.builder()
                        .invocationContext(context).userMessage(user)
                        .messages(java.util.Collections.<dev.langchain4j.data.message.ChatMessage>singletonList(user))
                        .build();
        assertTrue(request.invocationContext() == context);
        assertEquals("memory", request.chatMemoryId());
        assertEquals(user, request.userMessage());
        assertEquals(1, request.messages().size());
        assertEquals(user, new dev.langchain4j.service.tool.ToolProviderRequest("legacy", user).userMessage());

        dev.langchain4j.service.tool.ToolProvider provider = ignored -> modern;
        assertTrue(!provider.isDynamic());
        assertTrue(provider.provideTools(request) == modern);
    }

    @Test
    public void moderationResultAndOutputParserPreserveLegacyAndModernContracts() {
        dev.langchain4j.model.moderation.Moderation moderation =
                dev.langchain4j.model.moderation.Moderation.flagged("unsafe");
        dev.langchain4j.service.ModerationException moderationException =
                new dev.langchain4j.service.ModerationException("flagged", moderation);
        assertTrue(moderationException.moderation() == moderation);
        assertEquals("legacy", new dev.langchain4j.service.ModerationException("legacy").getMessage());

        dev.langchain4j.model.chat.response.ChatResponse intermediate =
                dev.langchain4j.model.chat.response.ChatResponse.builder()
                        .aiMessage(dev.langchain4j.data.message.AiMessage.from("tool")).build();
        dev.langchain4j.model.chat.response.ChatResponse finalResponse =
                dev.langchain4j.model.chat.response.ChatResponse.builder()
                        .aiMessage(dev.langchain4j.data.message.AiMessage.from("done")).build();
        dev.langchain4j.service.Result<String> modern = dev.langchain4j.service.Result.<String>builder()
                .content("done").intermediateResponses(java.util.Collections.singletonList(intermediate))
                .finalResponse(finalResponse).build();
        assertEquals(java.util.Collections.singletonList(intermediate), modern.intermediateResponses());
        assertTrue(modern.finalResponse() == finalResponse);
        dev.langchain4j.service.Result<String> legacy = new dev.langchain4j.service.Result<String>(
                "legacy", null, null, null, null);
        assertEquals("legacy", legacy.content());

        dev.langchain4j.service.output.ServiceOutputParser parser =
                new dev.langchain4j.service.output.ServiceOutputParser();
        assertEquals("done", parser.parse(finalResponse, String.class));
        dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> legacyResponse =
                new dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage>(
                        dev.langchain4j.data.message.AiMessage.from("legacy-response"));
        assertEquals("legacy-response", parser.parse(legacyResponse, String.class));
        assertTrue(!parser.jsonSchema(String.class).isPresent());
    }

    @Test
    public void streamingTokenStreamKeepsLegacyAliasesAndModernConstructor() throws Exception {
        final java.util.List<String> tokens = new java.util.ArrayList<String>();
        final java.util.List<String> completions = new java.util.ArrayList<String>();
        dev.langchain4j.service.AiServiceContext context =
                dev.langchain4j.service.AiServiceContext.create(AggregateAgenticApiCompatibilityTest.class);
        context.streamingChatModel = new dev.langchain4j.model.chat.StreamingChatModel() {
            @Override
            public void generate(
                    java.util.List<dev.langchain4j.data.message.ChatMessage> messages,
                    dev.langchain4j.model.StreamingResponseHandler<dev.langchain4j.data.message.AiMessage> handler) {
            }

            @Override
            public void doChat(
                    dev.langchain4j.model.chat.request.ChatRequest request,
                    dev.langchain4j.model.chat.response.StreamingChatResponseHandler handler) {
                handler.onPartialResponse("token");
                handler.onCompleteResponse(dev.langchain4j.model.chat.response.ChatResponse.builder()
                        .aiMessage(dev.langchain4j.data.message.AiMessage.from("complete"))
                        .tokenUsage(new dev.langchain4j.model.output.TokenUsage(1, 1, 2))
                        .build());
            }
        };
        dev.langchain4j.data.message.UserMessage user =
                dev.langchain4j.data.message.UserMessage.from("hello");
        dev.langchain4j.invocation.InvocationContext invocation =
                dev.langchain4j.invocation.InvocationContext.builder()
                        .chatMemoryId("memory")
                        .methodArguments(java.util.Collections.<Object>emptyList())
                        .invocationParameters(new dev.langchain4j.invocation.InvocationParameters())
                        .build();
        dev.langchain4j.service.tool.ToolServiceContext toolContext =
                dev.langchain4j.service.tool.ToolServiceContext.builder()
                        .effectiveTools(java.util.Collections.<dev.langchain4j.agent.tool.ToolSpecification>emptyList())
                        .availableTools(java.util.Collections.<dev.langchain4j.agent.tool.ToolSpecification>emptyList())
                        .toolExecutors(java.util.Collections.<String, dev.langchain4j.service.tool.ToolExecutor>emptyMap())
                        .build();
        dev.langchain4j.service.AiServiceTokenStream stream = new dev.langchain4j.service.AiServiceTokenStream(
                dev.langchain4j.service.AiServiceTokenStreamParameters.builder()
                        .messages(java.util.Collections.<dev.langchain4j.data.message.ChatMessage>singletonList(user))
                        .toolServiceContext(toolContext)
                        .toolArgumentsErrorHandler(context.toolService.argumentsErrorHandler())
                        .toolExecutionErrorHandler(context.toolService.executionErrorHandler())
                        .context(context)
                        .invocationContext(invocation)
                        .methodKey(AggregateAgenticApiCompatibilityTest.class.getMethod(
                                "streamingTokenStreamKeepsLegacyAliasesAndModernConstructor"))
                        .build());
        stream.onNext(tokens::add);
        stream.onComplete(response -> completions.add(response.content().text()));
        stream.ignoreErrors();
        stream.start();
        assertEquals(java.util.Collections.singletonList("token"), tokens);
        assertEquals(java.util.Collections.singletonList("complete"), completions);

        final java.util.List<dev.langchain4j.model.chat.response.PartialResponseContext> partialContexts =
                new java.util.ArrayList<dev.langchain4j.model.chat.response.PartialResponseContext>();
        dev.langchain4j.service.AiServiceTokenStream contextualStream =
                new dev.langchain4j.service.AiServiceTokenStream(
                        dev.langchain4j.service.AiServiceTokenStreamParameters.builder()
                                .messages(java.util.Collections.<dev.langchain4j.data.message.ChatMessage>singletonList(user))
                                .toolServiceContext(toolContext)
                                .toolArgumentsErrorHandler(context.toolService.argumentsErrorHandler())
                                .toolExecutionErrorHandler(context.toolService.executionErrorHandler())
                                .context(context)
                                .invocationContext(invocation)
                                .methodKey(AggregateAgenticApiCompatibilityTest.class.getMethod(
                                        "streamingTokenStreamKeepsLegacyAliasesAndModernConstructor"))
                                .build());
        contextualStream.onPartialResponseWithContext((partial, partialContext) ->
                    partialContexts.add(partialContext))
                .onCompleteResponse(response -> { })
                .ignoreErrors()
                .start();
        assertEquals(1, partialContexts.size());
        dev.langchain4j.model.chat.response.StreamingHandle cancellation =
                partialContexts.get(0).streamingHandle();
        assertTrue(!cancellation.isCancelled());
        dev.langchain4j.exception.UnsupportedFeatureException unsupported = assertThrows(
                dev.langchain4j.exception.UnsupportedFeatureException.class, cancellation::cancel);
        assertTrue(unsupported.getMessage().contains("Streaming cancellation is not supported"));
        assertTrue(!cancellation.isCancelled());

        assertNotNull(dev.langchain4j.service.AiServiceTokenStream.class.getConstructor(
                java.util.List.class, java.util.List.class, java.util.Map.class,
                java.util.List.class, dev.langchain4j.service.AiServiceContext.class, Object.class));
    }

    public interface LegacyContextAssistant {
        String chat(String userMessage);
    }

    public static final class LegacyToolTarget {
        @dev.langchain4j.agent.tool.Tool("Legacy lookup")
        public String legacyLookup() {
            return "legacy";
        }
    }

    public static final class ToolTarget {
        public String calculate(
                @dev.langchain4j.agent.tool.P(name = "number") int number,
                @dev.langchain4j.agent.tool.P(name = "units", defaultValue = "metric") String units,
                dev.langchain4j.invocation.InvocationContext context) {
            return number + ":" + units + ":" + (context.chatMemoryId() != null);
        }
    }

    private static void templateVariables(
            @dev.langchain4j.service.V("name") String name,
            @dev.langchain4j.service.V("details") java.util.Map<String, Object> details) {
    }
}
