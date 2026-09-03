package dev.langchain4j.agentic.observability;

import dev.langchain4j.agentic.planner.AgentInstance;
import dev.langchain4j.agentic.scope.AgenticScope;
import java.util.Map;
import java.util.Objects;

public final class AgentRequest {

    private final AgenticScope agenticScope;
    private final AgentInstance agent;
    private final Map<String, Object> inputs;

    public AgentRequest(AgenticScope agenticScope, AgentInstance agent, Map<String, Object> inputs) {
        this.agenticScope = agenticScope;
        this.agent = agent;
        this.inputs = inputs;
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

    public String agentName() {
        return agent.name();
    }

    public String agentId() {
        return agent.agentId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AgentRequest)) return false;
        AgentRequest that = (AgentRequest) o;
        return Objects.equals(agenticScope, that.agenticScope)
                && Objects.equals(agent, that.agent)
                && Objects.equals(inputs, that.inputs);
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash = 31 * hash + (agenticScope == null ? 0 : agenticScope.hashCode());
        hash = 31 * hash + (agent == null ? 0 : agent.hashCode());
        hash = 31 * hash + (inputs == null ? 0 : inputs.hashCode());
        return hash;
    }

    @Override
    public String toString() {
        return "AgentRequest[agenticScope=" + agenticScope
                + ", agent=" + agent
                + ", inputs=" + inputs + "]";
    }
}
