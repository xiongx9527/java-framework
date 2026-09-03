package dev.langchain4j.agentic.planner;

import dev.langchain4j.agentic.internal.AgentUtil;
import java.lang.reflect.Type;
import java.util.Objects;

public final class AgentArgument {

    private final Type type;
    private final String name;
    private final Object defaultValue;
    private final boolean isOptional;

    public AgentArgument(Type type, String name, Object defaultValue, boolean isOptional) {
        this.type = type;
        this.name = name;
        this.defaultValue = defaultValue;
        this.isOptional = isOptional;
    }

    public AgentArgument(Type type, String name) {
        this(type, name, null);
    }

    public AgentArgument(Type type, String name, Object defaultValue) {
        this(type, name, defaultValue, false);
    }

    public Type type() {
        return type;
    }

    public String name() {
        return name;
    }

    public Object defaultValue() {
        return defaultValue;
    }

    public boolean isOptional() {
        return isOptional;
    }

    public Class<?> rawType() {
        return AgentUtil.rawType(type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AgentArgument)) return false;
        AgentArgument that = (AgentArgument) o;
        return isOptional == that.isOptional
                && Objects.equals(type, that.type)
                && Objects.equals(name, that.name)
                && Objects.equals(defaultValue, that.defaultValue);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + (type == null ? 0 : type.hashCode());
        result = 31 * result + (name == null ? 0 : name.hashCode());
        result = 31 * result + (defaultValue == null ? 0 : defaultValue.hashCode());
        result = 31 * result + Boolean.hashCode(isOptional);
        return result;
    }

    @Override
    public String toString() {
        return "AgentArgument[type=" + type
                + ", name=" + name
                + ", defaultValue=" + defaultValue
                + ", isOptional=" + isOptional + "]";
    }
}
