package dev.langchain4j.agentic.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.agent.AgentInvocationException;
import dev.langchain4j.agentic.planner.AgenticSystemTopology;
import dev.langchain4j.agentic.scope.AgentInvocation;
import dev.langchain4j.agentic.scope.DefaultAgenticScope;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class AgenticAsyncStreamingRootTest {

    @Test
    void asyncAgentExecutorCompletesValueAndNullWithoutBlockingTheCaller() throws Exception {
        AsyncAgents agents = new AsyncAgents();
        DefaultAgenticScope scope = DefaultAgenticScope.ephemeralAgenticScope();
        RecordingPlanner planner = new RecordingPlanner(false);

        DelayedResponse<?> value = delayed(agents, "value", scope, planner);
        DelayedResponse<?> nullValue = delayed(agents, "nullValue", scope, planner);

        assertEquals("value", value.blockingGet());
        assertNull(nullValue.blockingGet());
        assertTrue(value.isDone());
        assertTrue(nullValue.isDone());
        assertEquals(2, planner.invocations.size());
    }

    @Test
    void asyncAgentExecutorUnwrapsAgentInvocationFailure() throws Exception {
        AsyncAgents agents = new AsyncAgents();
        DelayedResponse<?> response = delayed(agents, "failure",
                DefaultAgenticScope.ephemeralAgenticScope(), new RecordingPlanner(false));

        AgentInvocationException error = assertThrows(AgentInvocationException.class, response::blockingGet);
        assertTrue(error.getCause() instanceof java.lang.reflect.InvocationTargetException);
        assertEquals("async failure", error.getCause().getCause().getMessage());
    }

    @Test
    void streamingAgentExecutorPropagatesPartialAndCompleteEvents() throws Exception {
        StreamingAgents agents = new StreamingAgents();
        FakeTokenStream stream = agents.stream;
        TokenStream returned = (TokenStream) executor(agents, "stream")
                .execute(DefaultAgenticScope.ephemeralAgenticScope(), new RecordingPlanner(true));
        List<String> partials = new ArrayList<String>();
        List<String> completions = new ArrayList<String>();

        assertSame(stream, returned);
        returned.onPartialResponse(partials::add)
                .onCompleteResponse(response -> completions.add(response.aiMessage().text()))
                .start();

        assertEquals(java.util.Collections.singletonList("part-1"), partials);
        assertEquals(java.util.Collections.singletonList("streamed"), completions);
        assertEquals(1, stream.startCount);
    }

    @Test
    void streamingAgentExecutorWrapsCompletionAndPropagatesStreamFailure() throws Exception {
        StreamingAgents agents = new StreamingAgents();
        agents.stream.failure = new IllegalStateException("stream failure");
        assertThrows(IllegalStateException.class, () -> {
            DelayedResponse<String> response = assertInstanceOf(DelayedResponse.class,
                    executor(agents, "stream").execute(
                            DefaultAgenticScope.ephemeralAgenticScope(), new RecordingPlanner(false)));
            assertTrue(response.isDone());
            response.blockingGet();
        });
    }

    private static DelayedResponse<?> delayed(Object target, String methodName,
                                               DefaultAgenticScope scope, RecordingPlanner planner) throws Exception {
        return assertInstanceOf(DelayedResponse.class, executor(target, methodName).execute(scope, planner));
    }

    private static AgentExecutor executor(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        return AgentUtil.nonAiAgentToExecutor(target, method);
    }

    static final class AsyncAgents {
        @Agent(async = true)
        String value() {
            return "value";
        }

        @Agent(async = true)
        String nullValue() {
            return null;
        }

        @Agent(async = true)
        String failure() {
            throw new IllegalStateException("async failure");
        }
    }

    static final class StreamingAgents {
        private final FakeTokenStream stream = new FakeTokenStream();

        @Agent
        TokenStream stream() {
            return stream;
        }
    }

    static final class FakeTokenStream implements TokenStream {
        private Consumer<String> partialResponse;
        private Consumer<ChatResponse> completeResponse;
        private Consumer<Throwable> error;
        private RuntimeException failure;
        private int startCount;

        @Override
        public TokenStream onPartialResponse(Consumer<String> consumer) {
            partialResponse = consumer;
            return this;
        }

        @Override
        public TokenStream onRetrieved(Consumer<List<dev.langchain4j.rag.content.Content>> consumer) {
            return this;
        }

        @Override
        public TokenStream onToolExecuted(Consumer<dev.langchain4j.service.tool.ToolExecution> consumer) {
            return this;
        }

        @Override
        public TokenStream onCompleteResponse(Consumer<ChatResponse> consumer) {
            completeResponse = consumer;
            return this;
        }

        @Override
        public TokenStream onError(Consumer<Throwable> consumer) {
            error = consumer;
            return this;
        }

        @Override
        public TokenStream ignoreErrors() {
            return this;
        }

        @Override
        public void start() {
            startCount++;
            if (failure != null) {
                error.accept(failure);
                return;
            }
            if (partialResponse != null) {
                partialResponse.accept("part-1");
            }
            if (completeResponse != null) {
                completeResponse.accept(ChatResponse.builder()
                        .aiMessage(AiMessage.from("streamed"))
                        .build());
            }
        }
    }

    static final class RecordingPlanner implements PlannerExecutor {
        private final boolean propagateStreaming;
        private final List<AgentInvocation> invocations = new ArrayList<AgentInvocation>();

        RecordingPlanner(boolean propagateStreaming) {
            this.propagateStreaming = propagateStreaming;
        }

        @Override
        public void onSubagentInvoked(AgentInvocation agentInvocation) {
            invocations.add(agentInvocation);
        }

        @Override
        public boolean propagateStreaming() {
            return propagateStreaming;
        }
    }
}
