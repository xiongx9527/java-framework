package dev.langchain4j.model.chat.response;

import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import java.util.Objects;

/** Common metadata returned by chat models. */
public class ChatResponseMetadata {
    private final String id;
    private final String modelName;
    private final TokenUsage tokenUsage;
    private final FinishReason finishReason;

    protected ChatResponseMetadata(Builder<?> builder) {
        this.id = builder.id;
        this.modelName = builder.modelName;
        this.tokenUsage = builder.tokenUsage;
        this.finishReason = builder.finishReason;
    }

    public String id() { return id; }
    public String modelName() { return modelName; }
    public TokenUsage tokenUsage() { return tokenUsage; }
    public FinishReason finishReason() { return finishReason; }
    public Builder<?> toBuilder() { return toBuilder(builder()); }
    protected Builder<?> toBuilder(Builder<?> builder) {
        return builder.id(id).modelName(modelName).tokenUsage(tokenUsage).finishReason(finishReason);
    }

    @Override public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        ChatResponseMetadata that = (ChatResponseMetadata) object;
        return Objects.equals(id, that.id) && Objects.equals(modelName, that.modelName)
                && Objects.equals(tokenUsage, that.tokenUsage) && Objects.equals(finishReason, that.finishReason);
    }
    @Override public int hashCode() { return Objects.hash(id, modelName, tokenUsage, finishReason); }
    @Override public String toString() { return "ChatResponseMetadata{id='" + id + "', modelName='" + modelName
            + "', tokenUsage=" + tokenUsage + ", finishReason=" + finishReason + '}'; }

    @SuppressWarnings("rawtypes")
    public static Builder<?> builder() { return new Builder(); }

    public static class Builder<T extends Builder<T>> {
        private String id;
        private String modelName;
        private TokenUsage tokenUsage;
        private FinishReason finishReason;
        @SuppressWarnings("unchecked") public T id(String value) { this.id = value; return (T) this; }
        @SuppressWarnings("unchecked") public T modelName(String value) { this.modelName = value; return (T) this; }
        @SuppressWarnings("unchecked") public T tokenUsage(TokenUsage value) { this.tokenUsage = value; return (T) this; }
        @SuppressWarnings("unchecked") public T finishReason(FinishReason value) { this.finishReason = value; return (T) this; }
        public ChatResponseMetadata build() { return new ChatResponseMetadata(this); }
    }
}
