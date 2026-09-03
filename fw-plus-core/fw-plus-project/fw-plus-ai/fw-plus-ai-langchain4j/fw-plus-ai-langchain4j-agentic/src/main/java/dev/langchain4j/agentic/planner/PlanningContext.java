package dev.langchain4j.agentic.planner;

import dev.langchain4j.agentic.scope.AgentInvocation;
import dev.langchain4j.agentic.scope.AgenticScope;

import java.util.Objects;

public final class PlanningContext {

    private final AgenticScope agenticScope;
    private final AgentInvocation previousAgentInvocation;

    public PlanningContext(AgenticScope agenticScope, AgentInvocation previousAgentInvocation) {
        this.agenticScope = agenticScope;
        this.previousAgentInvocation = previousAgentInvocation;
    }

    public AgenticScope agenticScope() {
        return agenticScope;
    }

    public AgentInvocation previousAgentInvocation() {
        return previousAgentInvocation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlanningContext)) return false;
        PlanningContext that = (PlanningContext) o;
        return Objects.equals(agenticScope, that.agenticScope)
                && Objects.equals(previousAgentInvocation, that.previousAgentInvocation);
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash = 31 * hash + (agenticScope == null ? 0 : agenticScope.hashCode());
        hash = 31 * hash + (previousAgentInvocation == null ? 0 : previousAgentInvocation.hashCode());
        return hash;
    }

    @Override
    public String toString() {
        return "PlanningContext[agenticScope=" + agenticScope
                + ", previousAgentInvocation=" + previousAgentInvocation + "]";
    }
}
