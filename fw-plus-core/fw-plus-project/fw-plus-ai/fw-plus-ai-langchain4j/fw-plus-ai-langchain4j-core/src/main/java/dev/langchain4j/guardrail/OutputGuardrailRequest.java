package dev.langchain4j.guardrail;

import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;

/**
 * Represents the parameter passed to {@link OutputGuardrail#validate(OutputGuardrailRequest)}.
 */
public final class OutputGuardrailRequest implements GuardrailRequest<OutputGuardrailRequest> {
    private final ChatResponse responseFromLLM;
    private final ChatExecutor chatExecutor;
    private final GuardrailRequestParams requestParams;

    private OutputGuardrailRequest(Builder builder) {
        this.responseFromLLM = ensureNotNull(builder.responseFromLLM, "responseFromLLM");
        this.requestParams = ensureNotNull(builder.requestParams, "requestParams");
        this.chatExecutor = ensureNotNull(builder.chatExecutor, "chatExecutor");
    }

    /**
     * Returns the response from the LLM.
     *
     * @return the response from the LLM
     */
    public ChatResponse responseFromLLM() {
        return responseFromLLM;
    }

    /**
     * Returns the chat executor.
     *
     * @return the chat executor
     */
    public ChatExecutor chatExecutor() {
        return chatExecutor;
    }

    /**
     * Returns the common parameters that are shared across guardrail checks.
     *
     * @return an instance of {@code GuardrailRequestParams} containing shared parameters
     */
    @Override
    public GuardrailRequestParams requestParams() {
        return requestParams;
    }

    @Override
    public OutputGuardrailRequest withText(String text) {
        ensureNotNull(text, "text");

        java.util.List<dev.langchain4j.agent.tool.ToolExecutionRequest> requests =
                this.responseFromLLM.aiMessage().toolExecutionRequests();
        AiMessage aiMessage = requests == null || requests.isEmpty()
                ? new AiMessage(text) : new AiMessage(text, requests);

        ChatResponse chatResponse = ChatResponse.builder()
                .aiMessage(aiMessage)
                .tokenUsage(responseFromLLM.tokenUsage())
                .finishReason(responseFromLLM.finishReason())
                .build();

        return builder()
                .responseFromLLM(chatResponse)
                .chatExecutor(this.chatExecutor)
                .requestParams(this.requestParams)
                .build();
    }

    /**
     * Creates a new builder for {@link OutputGuardrailRequest}.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link OutputGuardrailRequest}.
     */
    public static class Builder {
        private ChatResponse responseFromLLM;
        private ChatExecutor chatExecutor;
        private GuardrailRequestParams requestParams;

        private Builder() {}

        /**
         * Sets the response from the LLM.
         *
         * @param responseFromLLM the response from the LLM
         * @return this builder
         */
        public Builder responseFromLLM(ChatResponse responseFromLLM) {
            this.responseFromLLM = responseFromLLM;
            return this;
        }

        /**
         * Sets the chat executor.
         *
         * @param chatExecutor the chat executor
         * @return this builder
         */
        public Builder chatExecutor(ChatExecutor chatExecutor) {
            this.chatExecutor = chatExecutor;
            return this;
        }

        /**
         * Sets the common parameters.
         *
         * @param requestParams the common parameters
         * @return this builder
         */
        public Builder requestParams(GuardrailRequestParams requestParams) {
            this.requestParams = requestParams;
            return this;
        }

        /**
         * Builds a new {@link OutputGuardrailRequest}.
         *
         * @return a new {@link OutputGuardrailRequest}
         */
        public OutputGuardrailRequest build() {
            return new OutputGuardrailRequest(this);
        }
    }
}
