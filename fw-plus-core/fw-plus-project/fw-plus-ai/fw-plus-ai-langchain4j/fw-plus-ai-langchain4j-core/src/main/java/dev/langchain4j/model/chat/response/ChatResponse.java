package dev.langchain4j.model.chat.response;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import java.util.Objects;

import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

/** A complete response from a chat model. */
public class ChatResponse {
    private final AiMessage aiMessage;
    private final ChatResponseMetadata metadata;

    protected ChatResponse(Builder builder) {
        this.aiMessage = ensureNotNull(builder.aiMessage, "aiMessage");
        if (builder.metadata != null && builder.hasIndividualMetadata()) {
            throw new IllegalArgumentException("Cannot set both 'metadata' and individual metadata on ChatResponse");
        }
        this.metadata = builder.metadata != null ? builder.metadata : ChatResponseMetadata.builder()
                .id(builder.id).modelName(builder.modelName).tokenUsage(builder.tokenUsage)
                .finishReason(builder.finishReason).build();
    }

    public AiMessage aiMessage() { return aiMessage; }
    public ChatResponseMetadata metadata() { return metadata; }
    public String id() { return metadata.id(); }
    public String modelName() { return metadata.modelName(); }
    public TokenUsage tokenUsage() { return metadata.tokenUsage(); }
    public FinishReason finishReason() { return metadata.finishReason(); }
    public Builder toBuilder() { return new Builder(this); }

    @Override public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        ChatResponse that = (ChatResponse) object;
        return Objects.equals(aiMessage, that.aiMessage) && Objects.equals(metadata, that.metadata);
    }
    @Override public int hashCode() { return Objects.hash(aiMessage, metadata); }
    @Override public String toString() { return "ChatResponse { aiMessage = " + aiMessage + ", metadata = " + metadata + " }"; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private AiMessage aiMessage;
        private ChatResponseMetadata metadata;
        private String id;
        private String modelName;
        private TokenUsage tokenUsage;
        private FinishReason finishReason;
        public Builder() { }
        public Builder(ChatResponse response) { this.aiMessage = response.aiMessage; this.metadata = response.metadata; }
        public Builder aiMessage(AiMessage value) { this.aiMessage = value; return this; }
        public Builder metadata(ChatResponseMetadata value) { this.metadata = value; return this; }
        public Builder id(String value) { this.id = value; return this; }
        public Builder modelName(String value) { this.modelName = value; return this; }
        public Builder tokenUsage(TokenUsage value) { this.tokenUsage = value; return this; }
        public Builder finishReason(FinishReason value) { this.finishReason = value; return this; }
        public ChatResponse build() { return new ChatResponse(this); }
        private boolean hasIndividualMetadata() {
            return id != null || modelName != null || tokenUsage != null || finishReason != null;
        }
    }
}
