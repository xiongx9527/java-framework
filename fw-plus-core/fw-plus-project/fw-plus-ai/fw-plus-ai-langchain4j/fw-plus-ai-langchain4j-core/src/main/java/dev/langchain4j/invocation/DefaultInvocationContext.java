package dev.langchain4j.invocation;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Default immutable invocation context implementation. */
public final class DefaultInvocationContext implements InvocationContext {
    private final UUID invocationId;
    private final String interfaceName;
    private final String methodName;
    private final List<Object> methodArguments;
    private final UserMessage userMessage;
    private final Object chatMemoryId;
    private final ChatRequestParameters defaultRequestParameters;
    private final ModelProvider modelProvider;
    private final InvocationParameters invocationParameters;
    private final Map<Class<? extends LangChain4jManaged>, LangChain4jManaged> managedParameters;
    private final Instant timestamp;

    public DefaultInvocationContext(InvocationContext.Builder builder) {
        if (builder == null) throw new NullPointerException("builder");
        invocationId = builder.invocationId();
        interfaceName = builder.interfaceName();
        methodName = builder.methodName();
        methodArguments = new ArrayList<Object>(builder.methodArguments());
        userMessage = builder.userMessage();
        chatMemoryId = builder.chatMemoryId();
        defaultRequestParameters = builder.defaultRequestParameters();
        modelProvider = builder.modelProvider();
        invocationParameters = builder.invocationParameters();
        managedParameters = builder.managedParameters();
        timestamp = builder.timestamp();
    }

    public UUID invocationId() { return invocationId; }
    public String interfaceName() { return interfaceName; }
    public String methodName() { return methodName; }
    public List<Object> methodArguments() { return methodArguments; }
    public UserMessage userMessage() { return userMessage; }
    public Object chatMemoryId() { return chatMemoryId; }
    public ChatRequestParameters defaultRequestParameters() { return defaultRequestParameters; }
    public ModelProvider modelProvider() { return modelProvider; }
    public InvocationParameters invocationParameters() { return invocationParameters; }
    public Map<Class<? extends LangChain4jManaged>, LangChain4jManaged> managedParameters() { return managedParameters; }
    public Instant timestamp() { return timestamp; }

    @Override public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        DefaultInvocationContext that = (DefaultInvocationContext) object;
        return Objects.equals(invocationId, that.invocationId)
                && Objects.equals(interfaceName, that.interfaceName)
                && Objects.equals(methodName, that.methodName)
                && Objects.equals(methodArguments, that.methodArguments)
                && Objects.equals(userMessage, that.userMessage)
                && Objects.equals(chatMemoryId, that.chatMemoryId)
                && Objects.equals(defaultRequestParameters, that.defaultRequestParameters)
                && Objects.equals(modelProvider, that.modelProvider)
                && Objects.equals(invocationParameters, that.invocationParameters)
                && Objects.equals(managedParameters, that.managedParameters)
                && Objects.equals(timestamp, that.timestamp);
    }

    @Override public int hashCode() {
        return Objects.hash(invocationId, interfaceName, methodName, methodArguments, userMessage,
                chatMemoryId, defaultRequestParameters, modelProvider, invocationParameters,
                managedParameters, timestamp);
    }

    @Override public String toString() {
        return "DefaultInvocationContext{" + "invocationId=" + invocationId
                + ", interfaceName='" + interfaceName + '\'' + ", methodName='" + methodName + '\''
                + ", methodArguments=" + methodArguments + ", userMessage=" + userMessage
                + ", chatMemoryId=" + chatMemoryId + ", defaultRequestParameters=" + defaultRequestParameters
                + ", modelProvider=" + modelProvider + ", invocationParameters=" + invocationParameters
                + ", managedParameters=" + managedParameters + ", timestamp=" + timestamp + '}';
    }
}
