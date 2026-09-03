package dev.langchain4j.agentic.behavior;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.declarative.AgentListenerSupplier;
import dev.langchain4j.agentic.declarative.SupervisorAgent;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.observability.AgentRequest;
import dev.langchain4j.agentic.observability.AgentResponse;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.V;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SupervisorListenerBehaviorTest {

    @AfterEach
    void clearEvents() {
        Events.values.clear();
    }

    @Test
    void publicSupervisorRootInvokesResolvedSubagentAndInheritedListener() {
        PlanningModel model = new PlanningModel();
        AgenticServices.AgentConfigurator configurator = new AgenticServices.AgentConfigurator(
                ignored -> { },
                new Function<Class<?>, Object>() {
                    @Override
                    public Object apply(Class<?> type) {
                        return type == Worker.class ? new Worker() : null;
                    }
                },
                null,
                null);

        Root root = AgenticServices.createAgenticSystem(Root.class, model, configurator);
        String result = root.supervise("choose worker");

        assertEquals("worker-result", result);
        assertEquals(2, model.planningCalls.get());
        assertEquals(Arrays.asList(
                "before:supervise",
                "before:worker",
                "after:worker:worker-result",
                "after:supervise:worker-result"), Events.values);
    }

    public interface Root {
        @SupervisorAgent(
                outputKey = "result",
                subAgents = {Worker.class})
        String supervise(@V("request") String request);

        @AgentListenerSupplier
        static AgentListener listener() {
            return new AgentListener() {
                @Override
                public boolean inheritedBySubagents() {
                    return true;
                }

                @Override
                public void beforeAgentInvocation(AgentRequest request) {
                    Events.values.add("before:" + request.agentName());
                }

                @Override
                public void afterAgentInvocation(AgentResponse response) {
                    Events.values.add("after:" + response.agentName() + ":" + response.output());
                }
            };
        }
    }

    public static final class Worker {
        @Agent(name = "worker", outputKey = "workerResult")
        public String run() {
            return "worker-result";
        }
    }

    private static final class Events {
        private static final List<String> values = Collections.synchronizedList(new ArrayList<String>());
    }

    private static final class PlanningModel implements ChatModel {
        private final AtomicInteger planningCalls = new AtomicInteger();

        @Override
        public ChatResponse doChat(ChatRequest request) {
            return response(nextPlan());
        }

        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages) {
            return Response.from(AiMessage.from(nextPlan()));
        }

        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> tools) {
            return Response.from(AiMessage.from(nextPlan()));
        }

        private String nextPlan() {
            if (planningCalls.getAndIncrement() == 0) {
                return "{\"agentName\":\"worker\",\"arguments\":{}}";
            }
            return "{\"agentName\":\"done\",\"arguments\":{\"response\":\"finished\"}}";
        }

        private ChatResponse response(String text) {
            return ChatResponse.builder().aiMessage(AiMessage.from(text)).build();
        }
    }
}
