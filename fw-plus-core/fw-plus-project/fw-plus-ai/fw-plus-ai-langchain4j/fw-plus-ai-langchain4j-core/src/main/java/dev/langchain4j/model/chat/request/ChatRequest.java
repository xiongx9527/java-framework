package dev.langchain4j.model.chat.request;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ChatMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static dev.langchain4j.internal.Utils.isNullOrEmpty;
import static dev.langchain4j.internal.ValidationUtils.ensureNotEmpty;
import static java.util.Arrays.asList;

/** A chat request retaining the 0.35 builder API and modern parameter model. */
public class ChatRequest {
    private final List<ChatMessage> messages;
    private final ChatRequestParameters parameters;

    protected ChatRequest(Builder builder) {
        this.messages = Collections.unmodifiableList(
                new ArrayList<ChatMessage>(ensureNotEmpty(builder.messages, "messages")));
        boolean individual = builder.modelName != null || builder.temperature != null || builder.topP != null
                || builder.topK != null || builder.frequencyPenalty != null || builder.presencePenalty != null
                || builder.maxOutputTokens != null || !isNullOrEmpty(builder.stopSequences)
                || !isNullOrEmpty(builder.toolSpecifications) || builder.toolChoice != null
                || builder.responseFormat != null;
        if (!individual) {
            this.parameters = builder.parameters != null ? builder.parameters : DefaultChatRequestParameters.EMPTY;
        } else {
            ChatRequestParameters overrides = ChatRequestParameters.builder().modelName(builder.modelName)
                    .temperature(builder.temperature).topP(builder.topP).topK(builder.topK)
                    .frequencyPenalty(builder.frequencyPenalty).presencePenalty(builder.presencePenalty)
                    .maxOutputTokens(builder.maxOutputTokens).stopSequences(builder.stopSequences)
                    .toolSpecifications(builder.toolSpecifications).toolChoice(builder.toolChoice)
                    .responseFormat(builder.responseFormat).build();
            this.parameters = builder.parameters == null ? overrides : builder.parameters.overrideWith(overrides);
        }
    }

    public List<ChatMessage> messages() { return messages; }
    public ChatRequestParameters parameters() { return parameters; }
    public String modelName() { return parameters.modelName(); }
    public Double temperature() { return parameters.temperature(); }
    public Double topP() { return parameters.topP(); }
    public Integer topK() { return parameters.topK(); }
    public Double frequencyPenalty() { return parameters.frequencyPenalty(); }
    public Double presencePenalty() { return parameters.presencePenalty(); }
    public Integer maxOutputTokens() { return parameters.maxOutputTokens(); }
    public List<String> stopSequences() { return parameters.stopSequences(); }
    public List<ToolSpecification> toolSpecifications() { return parameters.toolSpecifications(); }
    public ToolChoice toolChoice() { return parameters.toolChoice(); }
    public ResponseFormat responseFormat() { return parameters.responseFormat(); }
    public Builder toBuilder() { return new Builder(this); }

    @Override public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        ChatRequest that = (ChatRequest) object;
        return Objects.equals(messages, that.messages) && Objects.equals(parameters, that.parameters);
    }
    @Override public int hashCode() { return Objects.hash(messages, parameters); }
    @Override public String toString() { return "ChatRequest { messages = " + messages + ", parameters = " + parameters + " }"; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private List<ChatMessage> messages;
        private ChatRequestParameters parameters;
        private String modelName;
        private Double temperature;
        private Double topP;
        private Integer topK;
        private Double frequencyPenalty;
        private Double presencePenalty;
        private Integer maxOutputTokens;
        private List<String> stopSequences;
        private List<ToolSpecification> toolSpecifications;
        private ToolChoice toolChoice;
        private ResponseFormat responseFormat;

        public Builder() { }
        public Builder(ChatRequest request) { this.messages = request.messages; this.parameters = request.parameters; }
        public Builder messages(List<ChatMessage> messages) { this.messages = messages; return this; }
        public Builder messages(ChatMessage... messages) { return messages(asList(messages)); }
        public Builder parameters(ChatRequestParameters parameters) { this.parameters = parameters; return this; }
        public Builder modelName(String value) { this.modelName = value; return this; }
        public Builder temperature(Double value) { this.temperature = value; return this; }
        public Builder topP(Double value) { this.topP = value; return this; }
        public Builder topK(Integer value) { this.topK = value; return this; }
        public Builder frequencyPenalty(Double value) { this.frequencyPenalty = value; return this; }
        public Builder presencePenalty(Double value) { this.presencePenalty = value; return this; }
        public Builder maxOutputTokens(Integer value) { this.maxOutputTokens = value; return this; }
        public Builder stopSequences(List<String> value) { this.stopSequences = value; return this; }
        public Builder toolSpecifications(List<ToolSpecification> value) { this.toolSpecifications = value; return this; }
        public Builder toolSpecifications(ToolSpecification... value) { return toolSpecifications(asList(value)); }
        public Builder toolChoice(ToolChoice value) { this.toolChoice = value; return this; }
        public Builder responseFormat(ResponseFormat value) { this.responseFormat = value; return this; }
        public ChatRequest build() { return new ChatRequest(this); }
    }
}
