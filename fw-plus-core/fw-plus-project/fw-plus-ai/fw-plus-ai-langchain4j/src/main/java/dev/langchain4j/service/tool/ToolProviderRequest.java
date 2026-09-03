package dev.langchain4j.service.tool;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.invocation.InvocationParameters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

public class ToolProviderRequest {
    private final InvocationContext invocationContext;
    private final UserMessage userMessage;
    private final List<ChatMessage> messages;

    public ToolProviderRequest(Builder builder) {
        invocationContext = ensureNotNull(builder.invocationContext, "invocationContext");
        userMessage = ensureNotNull(builder.userMessage, "userMessage");
        messages = builder.messages == null ? Collections.<ChatMessage>emptyList()
                : Collections.unmodifiableList(new ArrayList<ChatMessage>(builder.messages));
    }

    public ToolProviderRequest(Object chatMemoryId, UserMessage userMessage) {
        invocationContext = InvocationContext.builder().chatMemoryId(chatMemoryId).build();
        this.userMessage = ensureNotNull(userMessage, "userMessage");
        messages = Collections.emptyList();
    }

    public InvocationContext invocationContext() { return invocationContext; }
    public InvocationParameters invocationParameters() { return invocationContext.invocationParameters(); }
    public UserMessage userMessage() { return userMessage; }
    public Object chatMemoryId() { return invocationContext.chatMemoryId(); }
    public List<ChatMessage> messages() { return messages; }
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private InvocationContext invocationContext;
        private UserMessage userMessage;
        private List<ChatMessage> messages;
        public Builder invocationContext(InvocationContext value) { invocationContext = value; return this; }
        public Builder userMessage(UserMessage value) { userMessage = value; return this; }
        public Builder messages(List<ChatMessage> value) { messages = value; return this; }
        public ToolProviderRequest build() { return new ToolProviderRequest(this); }
    }
}
