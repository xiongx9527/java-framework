package dev.langchain4j.rag.query;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.invocation.InvocationParameters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

/** Metadata associated with a retrieval query. */
public class Metadata {
    private final ChatMessage chatMessage;
    private final SystemMessage systemMessage;
    private final List<ChatMessage> chatMemory;
    private final InvocationContext invocationContext;

    public Metadata(Builder builder) {
        this.chatMessage = ensureNotNull(builder.chatMessage, "chatMessage");
        this.systemMessage = builder.systemMessage;
        this.chatMemory = immutableCopy(builder.chatMemory);
        this.invocationContext = ensureNotNull(builder.invocationContext, "invocationContext");
    }

    public Metadata(UserMessage userMessage, Object chatMemoryId, List<ChatMessage> chatMemory) {
        this((ChatMessage) userMessage, chatMemoryId, chatMemory);
    }

    public Metadata(ChatMessage chatMessage, Object chatMemoryId, List<ChatMessage> chatMemory) {
        this.chatMessage = ensureNotNull(chatMessage, "chatMessage");
        this.systemMessage = null;
        this.chatMemory = immutableCopy(chatMemory);
        this.invocationContext = InvocationContext.builder().chatMemoryId(chatMemoryId).build();
    }

    public Metadata(ChatMessage chatMessage, SystemMessage systemMessage,
                    Object chatMemoryId, List<ChatMessage> chatMemory) {
        this.chatMessage = ensureNotNull(chatMessage, "chatMessage");
        this.systemMessage = ensureNotNull(systemMessage, "systemMessage");
        this.chatMemory = immutableCopy(chatMemory);
        this.invocationContext = InvocationContext.builder().chatMemoryId(chatMemoryId).build();
    }

    public UserMessage userMessage() { return (UserMessage) chatMessage; }
    public ChatMessage chatMessage() { return chatMessage; }
    public SystemMessage systemMessage() { return systemMessage; }
    public Object chatMemoryId() { return invocationContext.chatMemoryId(); }
    public List<ChatMessage> chatMemory() { return chatMemory; }
    public InvocationContext invocationContext() { return invocationContext; }
    public InvocationParameters invocationParameters() { return invocationContext.invocationParameters(); }

    @Override public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Metadata that = (Metadata) object;
        return Objects.equals(chatMessage, that.chatMessage) && Objects.equals(systemMessage, that.systemMessage)
                && Objects.equals(chatMemory, that.chatMemory) && Objects.equals(invocationContext, that.invocationContext);
    }
    @Override public int hashCode() { return Objects.hash(chatMessage, systemMessage, chatMemory, invocationContext); }
    @Override public String toString() { return "Metadata { chatMessage = " + chatMessage + ", systemMessage = "
            + systemMessage + ", chatMemory = " + chatMemory + ", invocationContext = " + invocationContext + " }"; }

    public static Metadata from(UserMessage message, Object id, List<ChatMessage> memory) {
        return new Metadata(message, id, memory);
    }
    public static Metadata from(ChatMessage message, Object id, List<ChatMessage> memory) {
        return new Metadata(message, id, memory);
    }
    public static Metadata from(ChatMessage message, SystemMessage systemMessage, Object id, List<ChatMessage> memory) {
        return new Metadata(message, systemMessage, id, memory);
    }
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private ChatMessage chatMessage;
        private SystemMessage systemMessage;
        private List<ChatMessage> chatMemory;
        private InvocationContext invocationContext;
        public Builder chatMessage(ChatMessage value) { this.chatMessage = value; return this; }
        public Builder systemMessage(SystemMessage value) { this.systemMessage = value; return this; }
        public Builder chatMemory(List<ChatMessage> value) { this.chatMemory = value; return this; }
        public Builder invocationContext(InvocationContext value) { this.invocationContext = value; return this; }
        public Metadata build() { return new Metadata(this); }
    }

    private static List<ChatMessage> immutableCopy(List<ChatMessage> messages) {
        return messages == null ? Collections.<ChatMessage>emptyList()
                : Collections.unmodifiableList(new ArrayList<ChatMessage>(messages));
    }
}
