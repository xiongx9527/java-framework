package dev.langchain4j.invocation;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Context of one AI service invocation. */
public interface InvocationContext {
    UUID invocationId();
    String interfaceName();
    String methodName();
    List<Object> methodArguments();
    default UserMessage userMessage() { return null; }
    Object chatMemoryId();
    default ChatRequestParameters defaultRequestParameters() { return null; }
    default ModelProvider modelProvider() { return null; }
    InvocationParameters invocationParameters();
    default Map<Class<? extends LangChain4jManaged>, LangChain4jManaged> managedParameters() {
        return Collections.emptyMap();
    }
    Instant timestamp();

    default Builder toBuilder() { return new Builder(this); }
    static Builder builder() { return new Builder(); }

    class Builder {
        private UUID invocationId;
        private String interfaceName;
        private String methodName;
        private final List<Object> methodArguments = new ArrayList<Object>();
        private UserMessage userMessage;
        private Object chatMemoryId;
        private ChatRequestParameters defaultRequestParameters;
        private ModelProvider modelProvider;
        private InvocationParameters invocationParameters;
        private Map<Class<? extends LangChain4jManaged>, LangChain4jManaged> managedParameters;
        private Instant timestamp;

        protected Builder() {}
        protected Builder(InvocationContext context) {
            invocationId(context.invocationId());
            interfaceName(context.interfaceName());
            methodName(context.methodName());
            methodArguments(context.methodArguments());
            userMessage(context.userMessage());
            chatMemoryId(context.chatMemoryId());
            defaultRequestParameters(context.defaultRequestParameters());
            modelProvider(context.modelProvider());
            invocationParameters(context.invocationParameters());
            managedParameters(context.managedParameters());
            timestamp(context.timestamp());
        }

        public Builder invocationId(UUID value) { invocationId = value; return this; }
        public Builder interfaceName(String value) { interfaceName = value; return this; }
        public Builder methodName(String value) { methodName = value; return this; }
        public Builder methodArguments(List<Object> values) {
            if (values != null) methodArguments.addAll(values);
            return this;
        }
        public Builder methodArgument(Object value) {
            if (value != null) methodArguments.add(value);
            return this;
        }
        public Builder userMessage(UserMessage value) { userMessage = value; return this; }
        public Builder chatMemoryId(Object value) { chatMemoryId = value; return this; }
        public Builder defaultRequestParameters(ChatRequestParameters value) { defaultRequestParameters = value; return this; }
        public Builder modelProvider(ModelProvider value) { modelProvider = value; return this; }
        public Builder invocationParameters(InvocationParameters value) { invocationParameters = value; return this; }
        public Builder managedParameters(Map<Class<? extends LangChain4jManaged>, LangChain4jManaged> value) { managedParameters = value; return this; }
        public Builder timestamp(Instant value) { timestamp = value; return this; }
        public Builder timestampNow() { return timestamp(Instant.now()); }
        @SuppressWarnings("unchecked")
        public <T extends InvocationContext> T build() { return (T) new DefaultInvocationContext(this); }
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
    }
}
