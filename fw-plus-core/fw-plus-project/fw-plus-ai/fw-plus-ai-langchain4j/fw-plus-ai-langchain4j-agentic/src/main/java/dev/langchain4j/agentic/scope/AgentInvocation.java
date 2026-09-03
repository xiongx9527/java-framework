package dev.langchain4j.agentic.scope;

import dev.langchain4j.agentic.internal.DelayedResponse;

import java.util.Map;
import java.util.Objects;

public final class AgentInvocation {

    private final Class<?> agentType;
    private final String agentName;
    private final String agentId;
    private final Map<String, Object> input;
    private final Object output;

    public AgentInvocation(Class<?> agentType, String agentName, String agentId, Map<String, Object> input,
                           Object output) {
        this.agentType = agentType;
        this.agentName = agentName;
        this.agentId = agentId;
        this.input = input;
        this.output = output;
    }

    public Class<?> agentType() {
        return agentType;
    }

    public String agentName() {
        return agentName;
    }

    public String agentId() {
        return agentId;
    }

    public Map<String, Object> input() {
        return input;
    }

    public Object output() {
        if (output instanceof DelayedResponse<?>) {
            DelayedResponse<?> delayedResponse = (DelayedResponse<?>) output;
            return delayedResponse.result();
        }
        return output;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AgentInvocation)) return false;
        AgentInvocation that = (AgentInvocation) o;
        return Objects.equals(agentType, that.agentType)
                && Objects.equals(agentName, that.agentName)
                && Objects.equals(agentId, that.agentId)
                && Objects.equals(input, that.input)
                && Objects.equals(output, that.output);
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash = 31 * hash + (agentType == null ? 0 : agentType.hashCode());
        hash = 31 * hash + (agentName == null ? 0 : agentName.hashCode());
        hash = 31 * hash + (agentId == null ? 0 : agentId.hashCode());
        hash = 31 * hash + (input == null ? 0 : input.hashCode());
        hash = 31 * hash + (output == null ? 0 : output.hashCode());
        return hash;
    }

    @Override
    public String toString() {
        return "AgentInvocation{" +
                "agentName=" + agentName +
                ", input=" + input +
                ", output=" + output +
                '}';
    }
}
