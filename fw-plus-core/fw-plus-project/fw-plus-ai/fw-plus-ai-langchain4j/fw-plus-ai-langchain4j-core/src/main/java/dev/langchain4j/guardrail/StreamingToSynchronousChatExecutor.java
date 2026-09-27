package dev.langchain4j.guardrail;

import dev.langchain4j.Internal;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

@Internal
final class StreamingToSynchronousChatExecutor extends AbstractChatExecutor {
    private final StreamingChatModel streamingChatModel;
    private final Consumer<Throwable> errorHandler;

    StreamingToSynchronousChatExecutor(ChatExecutor.StreamingToSynchronousBuilder builder) {
        super(builder);
        streamingChatModel = ensureNotNull(builder.streamingChatModel, "streamingChatModel");
        errorHandler = builder.errorHandler;
    }

    @Override protected ChatResponse execute(ChatRequest request) {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<ChatResponse> response = new AtomicReference<ChatResponse>();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        streamingChatModel.chat(request, new StreamingChatResponseHandler() {
            @Override public void onPartialResponse(String partialResponse) { }
            @Override public void onCompleteResponse(ChatResponse completeResponse) {
                response.set(completeResponse);
                latch.countDown();
            }
            @Override public void onError(Throwable throwable) {
                error.set(throwable);
                try {
                    if (errorHandler != null) errorHandler.accept(throwable);
                } catch (Throwable handlerError) {
                    if (handlerError != throwable) throwable.addSuppressed(handlerError);
                } finally {
                    latch.countDown();
                }
            }
        });
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(exception);
        }
        if (error.get() != null) {
            Throwable throwable = error.get();
            if (throwable instanceof RuntimeException) throw (RuntimeException) throwable;
            if (throwable instanceof Error) throw (Error) throwable;
            throw new RuntimeException(throwable);
        }
        ChatResponse completeResponse = response.get();
        if (completeResponse == null) {
            throw new IllegalStateException("Streaming chat completed without a response or an error");
        }
        return completeResponse;
    }
}
