package dev.langchain4j.guardrail;

import dev.langchain4j.Internal;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.observability.api.AiServiceListenerRegistrar;
import dev.langchain4j.observability.api.event.AiServiceRequestIssuedEvent;
import java.util.List;

import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

@Internal
abstract class AbstractChatExecutor implements ChatExecutor {
    protected final ChatRequest chatRequest;
    protected final InvocationContext invocationContext;
    protected final AiServiceListenerRegistrar eventListenerRegistrar;

    protected AbstractChatExecutor(ChatExecutor.AbstractBuilder<?> builder) {
        chatRequest = ensureNotNull(builder.chatRequest, "chatRequest");
        invocationContext = ensureNotNull(builder.invocationContext, "invocationContext");
        eventListenerRegistrar = builder.eventListenerRegistrar;
    }

    @Override public ChatResponse execute(List<ChatMessage> messages) {
        return executeInternal(chatRequest.toBuilder().messages(messages).build());
    }
    @Override public ChatResponse execute() { return executeInternal(chatRequest); }

    private ChatResponse executeInternal(ChatRequest request) {
        if (eventListenerRegistrar != null) {
            eventListenerRegistrar.fireEvent(AiServiceRequestIssuedEvent.builder()
                    .invocationContext(invocationContext).request(request).build());
        }
        return execute(request);
    }

    protected abstract ChatResponse execute(ChatRequest request);
}
