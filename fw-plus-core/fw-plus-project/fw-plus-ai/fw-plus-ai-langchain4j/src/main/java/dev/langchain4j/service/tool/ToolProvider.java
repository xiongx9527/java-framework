package dev.langchain4j.service.tool;

@FunctionalInterface
public interface ToolProvider {
    ToolProviderResult provideTools(ToolProviderRequest request);
    default boolean isDynamic() { return false; }
}
