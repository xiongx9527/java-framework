package dev.langchain4j.agentic.observability;

import dev.langchain4j.agentic.planner.AgentInstance;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.service.tool.BeforeToolExecution;

import java.util.Objects;

public final class BeforeAgentToolExecution {

    private final AgentInstance agentInstance;
    private final BeforeToolExecution toolExecution;

    public BeforeAgentToolExecution(AgentInstance agentInstance, BeforeToolExecution toolExecution) {
        this.agentInstance = agentInstance;
        this.toolExecution = toolExecution;
    }

    public AgentInstance agentInstance() {
        return agentInstance;
    }

    public BeforeToolExecution toolExecution() {
        return toolExecution;
    }

    public AgenticScope agenticScope() {
        return (AgenticScope) toolExecution.invocationContext().managedParameters().get(AgenticScope.class);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BeforeAgentToolExecution)) return false;
        BeforeAgentToolExecution that = (BeforeAgentToolExecution) o;
        return Objects.equals(agentInstance, that.agentInstance)
                && Objects.equals(toolExecution, that.toolExecution);
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash = 31 * hash + (agentInstance == null ? 0 : agentInstance.hashCode());
        hash = 31 * hash + (toolExecution == null ? 0 : toolExecution.hashCode());
        return hash;
    }

    @Override
    public String toString() {
        return "BeforeAgentToolExecution[agentInstance=" + agentInstance
                + ", toolExecution=" + toolExecution + "]";
    }
}
