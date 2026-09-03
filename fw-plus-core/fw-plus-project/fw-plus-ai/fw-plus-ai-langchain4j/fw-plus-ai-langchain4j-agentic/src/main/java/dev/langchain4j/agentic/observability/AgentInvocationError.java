package dev.langchain4j.agentic.observability;

import java.util.Map;
import java.util.Objects;
import dev.langchain4j.agentic.planner.AgentInstance;
import dev.langchain4j.agentic.scope.AgenticScope;

public final class AgentInvocationError {

    private final AgenticScope agenticScope;
    private final AgentInstance agent;
    private final Map<String, Object> inputs;
    private final Throwable error;

    public AgentInvocationError(AgenticScope agenticScope, AgentInstance agent, Map<String, Object> inputs,
                                Throwable error) {
        this.agenticScope = agenticScope;
        this.agent = agent;
        this.inputs = inputs;
        this.error = error;
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

    public Throwable error() {
        return error;
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
        if (!(o instanceof AgentInvocationError)) return false;
        AgentInvocationError that = (AgentInvocationError) o;
        return Objects.equals(agenticScope, that.agenticScope)
                && Objects.equals(agent, that.agent)
                && Objects.equals(inputs, that.inputs)
                && Objects.equals(error, that.error);
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash = 31 * hash + (agenticScope == null ? 0 : agenticScope.hashCode());
        hash = 31 * hash + (agent == null ? 0 : agent.hashCode());
        hash = 31 * hash + (inputs == null ? 0 : inputs.hashCode());
        hash = 31 * hash + (error == null ? 0 : error.hashCode());
        return hash;
    }

    @Override
    public String toString() {
        return "AgentInvocationError[agenticScope=" + agenticScope
                + ", agent=" + agent
                + ", inputs=" + inputs
                + ", error=" + error + "]";
    }
}
