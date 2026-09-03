package dev.langchain4j.service;

import dev.langchain4j.Internal;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.guardrail.GuardrailRequestParams;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.tool.ToolArgumentsErrorHandler;
import dev.langchain4j.service.tool.ToolExecutionErrorHandler;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolServiceContext;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

@Internal
public class AiServiceTokenStreamParameters {
    private final List<ChatMessage> messages;
    private final ToolServiceContext toolServiceContext;
    private final ToolArgumentsErrorHandler toolArgumentsErrorHandler;
    private final ToolExecutionErrorHandler toolExecutionErrorHandler;
    private final Executor toolExecutor;
    private final List<Content> retrievedContents;
    private final AiServiceContext context;
    private final InvocationContext invocationContext;
    private final GuardrailRequestParams commonGuardrailParams;
    private final Object methodKey;

    protected AiServiceTokenStreamParameters(Builder builder) {
        messages = builder.messages;
        toolServiceContext = builder.toolServiceContext;
        toolArgumentsErrorHandler = builder.toolArgumentsErrorHandler;
        toolExecutionErrorHandler = builder.toolExecutionErrorHandler;
        toolExecutor = builder.toolExecutor;
        retrievedContents = builder.retrievedContents;
        context = builder.context;
        invocationContext = builder.invocationContext;
        commonGuardrailParams = builder.commonGuardrailParams;
        methodKey = builder.methodKey;
    }

    public List<ChatMessage> messages() { return messages; }
    public ToolServiceContext toolServiceContext() { return toolServiceContext; }
    @Deprecated public List<ToolSpecification> effectiveTools() {
        return toolServiceContext == null ? null : toolServiceContext.effectiveTools();
    }
    @Deprecated public List<ToolSpecification> toolSpecifications() { return effectiveTools(); }
    @Deprecated public List<ToolSpecification> availableTools() {
        return toolServiceContext == null ? null : toolServiceContext.availableTools();
    }
    @Deprecated public Map<String, ToolExecutor> toolExecutors() {
        return toolServiceContext == null ? null : toolServiceContext.toolExecutors();
    }
    public ToolArgumentsErrorHandler toolArgumentsErrorHandler() { return toolArgumentsErrorHandler; }
    public ToolExecutionErrorHandler toolExecutionErrorHandler() { return toolExecutionErrorHandler; }
    public Executor toolExecutor() { return toolExecutor; }
    public List<Content> retrievedContents() { return retrievedContents; }
    public AiServiceContext context() { return context; }
    public InvocationContext invocationContext() { return invocationContext; }
    public GuardrailRequestParams commonGuardrailParams() { return commonGuardrailParams; }
    public Object methodKey() { return methodKey; }
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private List<ChatMessage> messages;
        private ToolServiceContext toolServiceContext;
        private ToolArgumentsErrorHandler toolArgumentsErrorHandler;
        private ToolExecutionErrorHandler toolExecutionErrorHandler;
        private Executor toolExecutor;
        private List<Content> retrievedContents;
        private AiServiceContext context;
        private InvocationContext invocationContext;
        private GuardrailRequestParams commonGuardrailParams;
        private Object methodKey;
        protected Builder() { }
        public Builder messages(List<ChatMessage> value) { messages = value; return this; }
        public Builder toolServiceContext(ToolServiceContext value) { toolServiceContext = value; return this; }
        @Deprecated public Builder effectiveTools(List<ToolSpecification> value) {
            ensureToolServiceContext();
            toolServiceContext = toolServiceContext.toBuilder().effectiveTools(value).build();
            return this;
        }
        @Deprecated public Builder toolSpecifications(List<ToolSpecification> value) {
            effectiveTools(value);
            availableTools(value);
            return this;
        }
        @Deprecated public Builder availableTools(List<ToolSpecification> value) {
            ensureToolServiceContext();
            toolServiceContext = toolServiceContext.toBuilder().availableTools(value).build();
            return this;
        }
        @Deprecated public Builder toolExecutors(Map<String, ToolExecutor> value) {
            ensureToolServiceContext();
            toolServiceContext = toolServiceContext.toBuilder().toolExecutors(value).build();
            return this;
        }
        public Builder toolArgumentsErrorHandler(ToolArgumentsErrorHandler value) { toolArgumentsErrorHandler = value; return this; }
        public Builder toolExecutionErrorHandler(ToolExecutionErrorHandler value) { toolExecutionErrorHandler = value; return this; }
        public Builder toolExecutor(Executor value) { toolExecutor = value; return this; }
        public Builder retrievedContents(List<Content> value) { retrievedContents = value; return this; }
        public Builder context(AiServiceContext value) { context = value; return this; }
        public Builder invocationContext(InvocationContext value) { invocationContext = value; return this; }
        public Builder commonGuardrailParams(GuardrailRequestParams value) { commonGuardrailParams = value; return this; }
        public Builder methodKey(Object value) { methodKey = value; return this; }
        public AiServiceTokenStreamParameters build() { return new AiServiceTokenStreamParameters(this); }
        private void ensureToolServiceContext() {
            if (toolServiceContext == null) toolServiceContext = ToolServiceContext.builder().build();
        }
    }
}
