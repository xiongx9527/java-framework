package dev.langchain4j.guardrail;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.observability.api.AiServiceListenerRegistrar;
import java.util.List;
import java.util.function.Consumer;

import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

/** Executes a chat request for output guardrails. */
public interface ChatExecutor {
    ChatResponse execute();
    ChatResponse execute(List<ChatMessage> chatMessages);

    static SynchronousBuilder builder(ChatModel model) { return new SynchronousBuilder(model); }
    static StreamingToSynchronousBuilder builder(StreamingChatModel model) {
        return new StreamingToSynchronousBuilder(model);
    }

    abstract class AbstractBuilder<T extends AbstractBuilder<T>> {
        protected ChatRequest chatRequest;
        protected InvocationContext invocationContext;
        protected AiServiceListenerRegistrar eventListenerRegistrar;
        protected AbstractBuilder() { }
        @SuppressWarnings("unchecked") public T chatRequest(ChatRequest value) { chatRequest = value; return (T) this; }
        @SuppressWarnings("unchecked") public T invocationContext(InvocationContext value) { invocationContext = value; return (T) this; }
        @SuppressWarnings("unchecked") public T eventListenerRegistrar(AiServiceListenerRegistrar value) {
            eventListenerRegistrar = value;
            return (T) this;
        }
        public abstract ChatExecutor build();
    }

    class SynchronousBuilder extends AbstractBuilder<SynchronousBuilder> {
        protected final ChatModel chatModel;
        protected SynchronousBuilder(ChatModel model) { chatModel = ensureNotNull(model, "chatModel"); }
        @Override public ChatExecutor build() { return new SynchronousChatExecutor(this); }
    }

    class StreamingToSynchronousBuilder extends AbstractBuilder<StreamingToSynchronousBuilder> {
        protected final StreamingChatModel streamingChatModel;
        protected Consumer<Throwable> errorHandler;
        protected StreamingToSynchronousBuilder(StreamingChatModel model) {
            streamingChatModel = ensureNotNull(model, "streamingChatModel");
        }
        public StreamingToSynchronousBuilder errorHandler(Consumer<Throwable> value) {
            errorHandler = value;
            return this;
        }
        @Override public ChatExecutor build() { return new StreamingToSynchronousChatExecutor(this); }
    }
}
