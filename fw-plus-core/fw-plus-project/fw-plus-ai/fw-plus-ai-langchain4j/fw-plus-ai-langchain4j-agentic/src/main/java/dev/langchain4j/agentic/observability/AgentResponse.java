package dev.langchain4j.agentic.observability;

import dev.langchain4j.agentic.planner.AgentInstance;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.util.Map;
import java.util.Objects;

public final class AgentResponse {

    private final AgenticScope agenticScope;
    private final AgentInstance agent;
    private final Map<String, Object> inputs;
    private final Object output;
    private final ChatRequest chatRequest;
    private final ChatResponse chatResponse;

    public AgentResponse(AgenticScope agenticScope, AgentInstance agent, Map<String, Object> inputs, Object output,
                         ChatRequest chatRequest, ChatResponse chatResponse) {
        this.agenticScope = agenticScope;
        this.agent = agent;
        this.inputs = inputs;
        this.output = output;
        this.chatRequest = chatRequest;
        this.chatResponse = chatResponse;
    }

    AgentResponse(AgenticScope agenticScope, AgentInstance agent, Map<String, Object> inputs, Object output) {
        this(agenticScope, agent, inputs, output, null, null);
    }

    public AgenticScope agenticScope() {
        return agenticScope;
    }

    public AgentInstance agent() {
        return agent;
    }

    public Map<String, Object> inputs() {
        return inputs;
    }

    public Object output() {
        return output;
    }

    public ChatRequest chatRequest() {
        return chatRequest;
    }

    public ChatResponse chatResponse() {
        return chatResponse;
    }

    public String agentName() {
        return agent.name();
    }

    public String agentId() {
        return agent.agentId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AgentResponse)) return false;
        AgentResponse that = (AgentResponse) o;
        return Objects.equals(agenticScope, that.agenticScope)
                && Objects.equals(agent, that.agent)
                && Objects.equals(inputs, that.inputs)
                && Objects.equals(output, that.output)
                && Objects.equals(chatRequest, that.chatRequest)
                && Objects.equals(chatResponse, that.chatResponse);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + (agenticScope == null ? 0 : agenticScope.hashCode());
        result = 31 * result + (agent == null ? 0 : agent.hashCode());
        result = 31 * result + (inputs == null ? 0 : inputs.hashCode());
        result = 31 * result + (output == null ? 0 : output.hashCode());
        result = 31 * result + (chatRequest == null ? 0 : chatRequest.hashCode());
        result = 31 * result + (chatResponse == null ? 0 : chatResponse.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "AgentResponse[agenticScope=" + agenticScope
                + ", agent=" + agent
                + ", inputs=" + inputs
                + ", output=" + output
                + ", chatRequest=" + chatRequest
                + ", chatResponse=" + chatResponse + "]";
    }
}
