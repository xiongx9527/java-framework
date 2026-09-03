package dev.langchain4j.guardrail;

import dev.langchain4j.Internal;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

@Internal
final class SynchronousChatExecutor extends AbstractChatExecutor {
    private final ChatModel chatModel;
    SynchronousChatExecutor(ChatExecutor.SynchronousBuilder builder) {
        super(builder);
        chatModel = ensureNotNull(builder.chatModel, "chatModel");
    }
    @Override protected ChatResponse execute(ChatRequest request) { return chatModel.chat(request); }
}
