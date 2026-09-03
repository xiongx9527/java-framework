package dev.langchain4j.agentic.workflow.impl;

import dev.langchain4j.agentic.internal.CollectionHelper;
import dev.langchain4j.agentic.planner.Action;
import dev.langchain4j.agentic.planner.AgentInstance;
import dev.langchain4j.agentic.planner.AgenticSystemTopology;
import dev.langchain4j.agentic.planner.PlanningContext;
import dev.langchain4j.agentic.planner.Planner;
import dev.langchain4j.agentic.workflow.ConditionalAgent;
import dev.langchain4j.agentic.workflow.ConditionalAgentInstance;

import java.util.List;
import java.util.Objects;

public final class ConditionalPlanner implements Planner {

    private final List<ConditionalAgent> conditionalSubagents;

    public ConditionalPlanner(List<ConditionalAgent> conditionalSubagents) {
        this.conditionalSubagents = conditionalSubagents;
    }

    public List<ConditionalAgent> conditionalSubagents() {
        return conditionalSubagents;
    }

    @Override
    public Action firstAction(PlanningContext planningContext) {
        List<AgentInstance> agentsToCall = conditionalSubagents.stream()
                .filter(conditionalAgent -> conditionalAgent.predicate().test(planningContext.agenticScope()))
                .flatMap(conditionalAgent -> conditionalAgent.agentInstances().stream())
                .collect(CollectionHelper.toUnmodifiableList());
        return agentsToCall.isEmpty() ? done() : call(agentsToCall);
    }

    @Override
    public Action nextAction(PlanningContext planningContext) {
        return done();
    }

    @Override
    public AgenticSystemTopology topology() {
        return AgenticSystemTopology.ROUTER;
    }

    @Override
    public boolean terminated() {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AgentInstance> T as(Class<T> agentInstanceClass, AgentInstance agentInstance) {
        if (agentInstanceClass != ConditionalAgentInstance.class) {
            throw new ClassCastException("Cannot cast to " + agentInstanceClass.getName() + ": incompatible type");
        }
        return (T) new DefaultConditionalAgentInstance(agentInstance, this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConditionalPlanner)) return false;
        ConditionalPlanner that = (ConditionalPlanner) o;
        return Objects.equals(conditionalSubagents, that.conditionalSubagents);
    }

    @Override
    public int hashCode() {
        return conditionalSubagents == null ? 0 : conditionalSubagents.hashCode();
    }

    @Override
    public String toString() {
        return "ConditionalPlanner[conditionalSubagents=" + conditionalSubagents + "]";
    }

}
