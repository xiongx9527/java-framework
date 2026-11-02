package com.xiongx9527.fw.plus.ai.model.chat.request;

import com.xiongx9527.fw.plus.ai.tool.ToolSpecification;
import java.util.Collections;
import java.util.List;

/** 默认聊天请求参数，镜像 langchain4j 1.23.0 的 {@code DefaultChatRequestParameters}。 */
public class DefaultChatRequestParameters implements ChatRequestParameters {
    private final String modelName;
    private final Double temperature;
    private final Double topP;
    private final Integer topK;
    private final Double frequencyPenalty;
    private final Double presencePenalty;
    private final Integer maxOutputTokens;
    private final List<String> stopSequences;
    private final List<ToolSpecification> toolSpecifications;
    private final ToolChoice toolChoice;
    private final ResponseFormat responseFormat;

    private DefaultChatRequestParameters(Builder b) {
        this.modelName = b.modelName; this.temperature = b.temperature; this.topP = b.topP; this.topK = b.topK;
        this.frequencyPenalty = b.frequencyPenalty; this.presencePenalty = b.presencePenalty; this.maxOutputTokens = b.maxOutputTokens;
        this.stopSequences = b.stopSequences; this.toolSpecifications = b.toolSpecifications;
        this.toolChoice = b.toolChoice; this.responseFormat = b.responseFormat;
    }
    @Override public String modelName() { return modelName; }
    @Override public Double temperature() { return temperature; }
    @Override public Double topP() { return topP; }
    @Override public Integer topK() { return topK; }
    @Override public Double frequencyPenalty() { return frequencyPenalty; }
    @Override public Double presencePenalty() { return presencePenalty; }
    @Override public Integer maxOutputTokens() { return maxOutputTokens; }
    @Override public List<String> stopSequences() { return stopSequences; }
    @Override public List<ToolSpecification> toolSpecifications() { return toolSpecifications; }
    @Override public ToolChoice toolChoice() { return toolChoice; }
    @Override public ResponseFormat responseFormat() { return responseFormat; }
    @Override public ChatRequestParameters overrideWith(ChatRequestParameters other) {
        if (other == null) return this;
        Builder b = builder()
                .modelName(other.modelName() != null ? other.modelName() : modelName)
                .temperature(other.temperature() != null ? other.temperature() : temperature)
                .topP(other.topP() != null ? other.topP() : topP)
                .topK(other.topK() != null ? other.topK() : topK)
                .frequencyPenalty(other.frequencyPenalty() != null ? other.frequencyPenalty() : frequencyPenalty)
                .presencePenalty(other.presencePenalty() != null ? other.presencePenalty() : presencePenalty)
                .maxOutputTokens(other.maxOutputTokens() != null ? other.maxOutputTokens() : maxOutputTokens)
                .stopSequences(other.stopSequences() != null ? other.stopSequences() : stopSequences)
                .toolSpecifications(other.toolSpecifications() != null ? other.toolSpecifications() : toolSpecifications)
                .toolChoice(other.toolChoice() != null ? other.toolChoice() : toolChoice)
                .responseFormat(other.responseFormat() != null ? other.responseFormat() : responseFormat);
        return b.build();
    }
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String modelName; private Double temperature; private Double topP; private Integer topK;
        private Double frequencyPenalty; private Double presencePenalty; private Integer maxOutputTokens;
        private List<String> stopSequences = Collections.emptyList();
        private List<ToolSpecification> toolSpecifications = Collections.emptyList();
        private ToolChoice toolChoice; private ResponseFormat responseFormat;
        public Builder modelName(String v) { this.modelName = v; return this; }
        public Builder temperature(Double v) { this.temperature = v; return this; }
        public Builder topP(Double v) { this.topP = v; return this; }
        public Builder topK(Integer v) { this.topK = v; return this; }
        public Builder frequencyPenalty(Double v) { this.frequencyPenalty = v; return this; }
        public Builder presencePenalty(Double v) { this.presencePenalty = v; return this; }
        public Builder maxOutputTokens(Integer v) { this.maxOutputTokens = v; return this; }
        public Builder stopSequences(List<String> v) { this.stopSequences = v == null ? Collections.<String>emptyList() : v; return this; }
        public Builder toolSpecifications(List<ToolSpecification> v) { this.toolSpecifications = v == null ? Collections.<ToolSpecification>emptyList() : v; return this; }
        public Builder toolChoice(ToolChoice v) { this.toolChoice = v; return this; }
        public Builder responseFormat(ResponseFormat v) { this.responseFormat = v; return this; }
        public DefaultChatRequestParameters build() { return new DefaultChatRequestParameters(this); }
    }
}
