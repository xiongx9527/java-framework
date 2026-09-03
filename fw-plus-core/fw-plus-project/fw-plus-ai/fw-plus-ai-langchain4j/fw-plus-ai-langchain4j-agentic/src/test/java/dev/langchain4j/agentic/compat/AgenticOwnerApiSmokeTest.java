package dev.langchain4j.agentic.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServiceContext;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.memory.ChatMemoryService;
import dev.langchain4j.service.tool.ToolErrorContext;
import dev.langchain4j.service.tool.ToolErrorHandlerResult;
import dev.langchain4j.service.tool.ToolExecutionResult;
import dev.langchain4j.service.tool.ToolService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

class AgenticOwnerApiSmokeTest {

    @Test
    void exposesAgenticAggregateTypesAndModernBuilderMethods() throws Exception {
        assertNotNull(AiServiceContext.create(Assistant.class));
        assertNotNull(AiServices.class.getMethod("builder", AiServiceContext.class));
        assertNotNull(AiServices.class.getMethod("chatModel", dev.langchain4j.model.chat.ChatModel.class));
        assertNotNull(AiServices.class.getMethod("streamingChatModel", dev.langchain4j.model.chat.StreamingChatModel.class));
        assertNotNull(AiServices.class.getMethod("chatRequestTransformer", UnaryOperator.class));
        assertNotNull(AiServices.class.getMethod("systemMessageTransformer", java.util.function.BiFunction.class));
        assertNotNull(AiServices.class.getMethod("beforeToolExecution", java.util.function.Consumer.class));
        assertNotNull(AiServices.class.getMethod("afterToolExecution", java.util.function.Consumer.class));
        assertNotNull(AiServices.class.getMethod("executeToolsConcurrently"));
        assertNotNull(AiServices.class.getMethod("toolArgumentsErrorHandler", dev.langchain4j.service.tool.ToolArgumentsErrorHandler.class));
        assertNotNull(AiServices.class.getMethod("toolExecutionErrorHandler", dev.langchain4j.service.tool.ToolExecutionErrorHandler.class));
        assertNotNull(AiServices.class.getMethod("registerListener", dev.langchain4j.observability.api.listener.AiServiceListener.class));
    }

    @Test
    void chatMemoryServiceKeepsDefaultAndProviderMemoriesIsolated() {
        ChatMemory shared = MessageWindowChatMemory.withMaxMessages(10);
        ChatMemoryService defaults = new ChatMemoryService(shared);
        assertSame(shared, defaults.getChatMemory(ChatMemoryService.DEFAULT));
        assertSame(shared, defaults.getOrCreateChatMemory(ChatMemoryService.DEFAULT));
        assertTrue(defaults.getChatMemoryIDs().isEmpty());

        AtomicInteger created = new AtomicInteger();
        Map<Object, ChatMemory> memories = new LinkedHashMap<Object, ChatMemory>();
        ChatMemoryService provider = new ChatMemoryService(id -> {
            created.incrementAndGet();
            ChatMemory memory = MessageWindowChatMemory.withMaxMessages(10);
            memories.put(id, memory);
            return memory;
        });
        ChatMemory first = provider.getOrCreateChatMemory("alice");
        assertSame(first, provider.getOrCreateChatMemory("alice"));
        assertEquals(1, created.get());
        assertNotNull(provider.getChatMemory("alice"));
        assertSame(first, provider.evictChatMemory("alice"));
        assertEquals(0, provider.getChatMemories().size());
        assertEquals(1, memories.size());
    }

    @Test
    void toolServiceExecutesToolAndFiresBeforeAfterWithContext() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .id("call-1").name("greet").arguments("{}").build();
        InvocationContext invocationContext = InvocationContext.builder().chatMemoryId("alice").build();
        AtomicInteger before = new AtomicInteger();
        AtomicInteger after = new AtomicInteger();
        ToolService service = new ToolService();
        service.beforeToolExecution(event -> {
            assertSame(request, event.request());
            assertSame(invocationContext, event.invocationContext());
            before.incrementAndGet();
        });
        service.afterToolExecution(event -> {
            assertSame(request, event.request());
            assertEquals("hello", event.result());
            after.incrementAndGet();
        });
        Map<String, dev.langchain4j.service.tool.ToolExecutor> executors = new LinkedHashMap<String, dev.langchain4j.service.tool.ToolExecutor>();
        executors.put("greet", (toolRequest, memoryId) -> "hello");
        ToolExecutionResult result = service.executeTool(invocationContext, executors, request, null, null);
        assertEquals("hello", result.resultText());
        assertEquals(1, before.get());
        assertEquals(1, after.get());
        assertTrue(!result.isError());
    }

    @Test
    void toolErrorHandlerContextCarriesRequestAndMemory() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .id("call-2").name("broken").arguments("{}").build();
        InvocationContext invocationContext = InvocationContext.builder().chatMemoryId("bob").build();
        ToolErrorContext context = ToolErrorContext.builder()
                .toolExecutionRequest(request)
                .invocationContext(invocationContext)
                .rawError(new IllegalStateException("raw"))
                .build();
        ToolErrorHandlerResult handled = ToolErrorHandlerResult.text("retry");
        assertEquals("bob", context.memoryId());
        assertEquals("raw", context.rawError().getMessage());
        assertEquals("retry", handled.text());
    }

    interface Assistant {
        String answer(String message);
    }
}
