package dev.langchain4j.agentic.workflow;

import dev.langchain4j.agentic.planner.AgentInstance;
import dev.langchain4j.agentic.scope.AgenticScope;
import java.util.List;
import java.util.function.Predicate;

import java.util.Objects;

public final class ConditionalAgent {

    private final String condition;
    private final Predicate<AgenticScope> predicate;
    private final List<AgentInstance> agentInstances;

    public ConditionalAgent(String condition, Predicate<AgenticScope> predicate, List<AgentInstance> agentInstances) {
        this.condition = condition;
        this.predicate = predicate;
        this.agentInstances = agentInstances;
    }

    public String condition() {
        return condition;
    }

    public Predicate<AgenticScope> predicate() {
        return predicate;
    }

    public List<AgentInstance> agentInstances() {
        return agentInstances;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConditionalAgent)) return false;
        ConditionalAgent that = (ConditionalAgent) o;
        return Objects.equals(condition, that.condition)
                && Objects.equals(predicate, that.predicate)
                && Objects.equals(agentInstances, that.agentInstances);
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash = 31 * hash + (condition == null ? 0 : condition.hashCode());
        hash = 31 * hash + (predicate == null ? 0 : predicate.hashCode());
        hash = 31 * hash + (agentInstances == null ? 0 : agentInstances.hashCode());
        return hash;
    }

    @Override
    public String toString() {
        return "ConditionalAgent[condition=" + condition
                + ", predicate=" + predicate
                + ", agentInstances=" + agentInstances + "]";
    }
}
