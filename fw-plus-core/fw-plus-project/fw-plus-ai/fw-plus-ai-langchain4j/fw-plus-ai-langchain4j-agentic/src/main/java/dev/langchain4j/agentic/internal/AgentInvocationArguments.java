package dev.langchain4j.agentic.internal;

import java.util.Map;
import java.util.Objects;

public final class AgentInvocationArguments {

    private final Map<String, Object> namedArgs;
    private final Object[] positionalArgs;

    public AgentInvocationArguments(Map<String, Object> namedArgs, Object[] positionalArgs) {
        this.namedArgs = namedArgs;
        this.positionalArgs = positionalArgs;
    }

    public Map<String, Object> namedArgs() {
        return namedArgs;
    }

    public Object[] positionalArgs() {
        return positionalArgs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AgentInvocationArguments)) return false;
        AgentInvocationArguments that = (AgentInvocationArguments) o;
        return Objects.equals(namedArgs, that.namedArgs)
                && Objects.equals(positionalArgs, that.positionalArgs);
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash = 31 * hash + (namedArgs == null ? 0 : namedArgs.hashCode());
        hash = 31 * hash + (positionalArgs == null ? 0 : positionalArgs.hashCode());
        return hash;
    }

    @Override
    public String toString() {
        return "AgentInvocationArguments[namedArgs=" + namedArgs
                + ", positionalArgs=" + positionalArgs + "]";
    }
}
