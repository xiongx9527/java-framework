package dev.langchain4j.agentic.scope;

import java.util.Objects;

public final class AgenticScopeKey {

    private final String agentId;
    private final Object memoryId;

    public AgenticScopeKey(String agentId, Object memoryId) {
        this.agentId = agentId;
        this.memoryId = memoryId;
    }

    public String agentId() {
        return agentId;
    }

    public Object memoryId() {
        return memoryId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AgenticScopeKey)) return false;
        AgenticScopeKey that = (AgenticScopeKey) o;
        return Objects.equals(agentId, that.agentId)
                && Objects.equals(memoryId, that.memoryId);
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash = 31 * hash + (agentId == null ? 0 : agentId.hashCode());
        hash = 31 * hash + (memoryId == null ? 0 : memoryId.hashCode());
        return hash;
    }

    @Override
    public String toString() {
        return "AgenticScopeKey[agentId=" + agentId
                + ", memoryId=" + memoryId + "]";
    }
}
