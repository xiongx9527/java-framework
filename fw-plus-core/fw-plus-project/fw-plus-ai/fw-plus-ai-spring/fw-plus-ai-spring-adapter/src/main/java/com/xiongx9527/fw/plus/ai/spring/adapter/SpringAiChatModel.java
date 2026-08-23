package com.xiongx9527.fw.plus.ai.spring.adapter;

import com.xiongx9527.fw.plus.ai.model.AiChatModel;
import com.xiongx9527.fw.plus.ai.model.AiChatRequest;
import com.xiongx9527.fw.plus.ai.model.AiChatResult;
import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import com.xiongx9527.fw.plus.ai.model.AiModelUsage;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

final class SpringAiChatModel extends SpringAiModel implements AiChatModel {

    private final ChatModel delegate;

    SpringAiChatModel(AiModelDefinition definition, ChatModel delegate) {
        super(definition);
        this.delegate = delegate;
    }

    @Override
    public AiChatResult chat(AiChatRequest request) {
        if (request == null) {
            throw requestRejected("request must not be null");
        }
        if (!request.getParameters().isEmpty()) {
            throw requestRejected("per-request parameters are not supported");
        }
        return execute(request.getTimeoutMillis(), () -> toResult(delegate.call(new Prompt(request.getInput()))));
    }

    private AiChatResult toResult(ChatResponse response) {
        if (response == null) {
            throw requestRejected("returned a null response");
        }
        Generation generation = response.getResult();
        if (generation == null || generation.getOutput() == null || generation.getOutput().getText() == null
                || generation.getOutput().getText().trim().isEmpty()) {
            throw requestRejected("returned an invalid response");
        }
        String finishReason = generation.getMetadata() == null
                ? null : generation.getMetadata().getFinishReason();
        Usage usage = response.getMetadata() == null ? null : response.getMetadata().getUsage();
        return new AiChatResult(generation.getOutput().getText(), getKey(), finishReason, usage(usage));
    }

    private AiModelUsage usage(Usage usage) {
        if (usage == null || usage instanceof EmptyUsage || usage.getPromptTokens() == null
                || usage.getCompletionTokens() == null
                || usage.getTotalTokens() == null) {
            return AiModelUsage.unknown();
        }
        return new AiModelUsage(usage.getPromptTokens().longValue(), usage.getCompletionTokens().longValue(),
                usage.getTotalTokens().longValue());
    }
}
