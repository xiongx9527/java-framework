package dev.langchain4j.agentic.observability;

import dev.langchain4j.agentic.planner.AgentInstance;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.service.tool.ToolExecution;

import java.util.Objects;

public final class AfterAgentToolExecution {

    private final AgentInstance agentInstance;
    private final ToolExecution toolExecution;

    public AfterAgentToolExecution(AgentInstance agentInstance, ToolExecution toolExecution) {
        this.agentInstance = agentInstance;
        this.toolExecution = toolExecution;
    }

    public AgentInstance agentInstance() {
        return agentInstance;
    }

    public ToolExecution toolExecution() {
        return toolExecution;
    }

    public AgenticScope agenticScope() {
        return (AgenticScope) toolExecution.invocationContext().managedParameters().get(AgenticScope.class);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AfterAgentToolExecution)) return false;
        AfterAgentToolExecution that = (AfterAgentToolExecution) o;
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
        return "AfterAgentToolExecution[agentInstance=" + agentInstance
                + ", toolExecution=" + toolExecution + "]";
    }
}
