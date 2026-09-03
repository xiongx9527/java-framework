package dev.langchain4j.agentic.agent;

import dev.langchain4j.agentic.scope.AgenticScope;

import java.util.Objects;

public final class ErrorContext {

    private final String agentName;
    private final AgenticScope agenticScope;
    private final AgentInvocationException exception;

    public ErrorContext(String agentName, AgenticScope agenticScope, AgentInvocationException exception) {
        this.agentName = agentName;
        this.agenticScope = agenticScope;
        this.exception = exception;
    }

    public String agentName() {
        return agentName;
    }

    public AgenticScope agenticScope() {
        return agenticScope;
    }

    public AgentInvocationException exception() {
        return exception;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ErrorContext)) return false;
        ErrorContext that = (ErrorContext) o;
        return Objects.equals(agentName, that.agentName)
                && Objects.equals(agenticScope, that.agenticScope)
                && Objects.equals(exception, that.exception);
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash = 31 * hash + (agentName == null ? 0 : agentName.hashCode());
        hash = 31 * hash + (agenticScope == null ? 0 : agenticScope.hashCode());
        hash = 31 * hash + (exception == null ? 0 : exception.hashCode());
        return hash;
    }

    @Override
    public String toString() {
        return "ErrorContext[agentName=" + agentName
                + ", agenticScope=" + agenticScope
                + ", exception=" + exception + "]";
    }
}
