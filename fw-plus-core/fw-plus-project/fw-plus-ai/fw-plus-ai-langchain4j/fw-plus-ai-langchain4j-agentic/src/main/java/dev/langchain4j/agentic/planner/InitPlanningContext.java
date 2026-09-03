package dev.langchain4j.agentic.planner;

import dev.langchain4j.agentic.scope.AgenticScope;

import java.util.List;
import java.util.Objects;

public final class InitPlanningContext {

    private final AgenticScope agenticScope;
    private final AgentInstance plannerAgent;
    private final List<AgentInstance> subagents;

    public InitPlanningContext(AgenticScope agenticScope, AgentInstance plannerAgent,
                               List<AgentInstance> subagents) {
        this.agenticScope = agenticScope;
        this.plannerAgent = plannerAgent;
        this.subagents = subagents;
    }

    public AgenticScope agenticScope() {
        return agenticScope;
    }

    public AgentInstance plannerAgent() {
        return plannerAgent;
    }

    public List<AgentInstance> subagents() {
        return subagents;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InitPlanningContext)) return false;
        InitPlanningContext that = (InitPlanningContext) o;
        return Objects.equals(agenticScope, that.agenticScope)
                && Objects.equals(plannerAgent, that.plannerAgent)
                && Objects.equals(subagents, that.subagents);
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash = 31 * hash + (agenticScope == null ? 0 : agenticScope.hashCode());
        hash = 31 * hash + (plannerAgent == null ? 0 : plannerAgent.hashCode());
        hash = 31 * hash + (subagents == null ? 0 : subagents.hashCode());
        return hash;
    }

    @Override
    public String toString() {
        return "InitPlanningContext[agenticScope=" + agenticScope
                + ", plannerAgent=" + plannerAgent
                + ", subagents=" + subagents + "]";
    }
}
