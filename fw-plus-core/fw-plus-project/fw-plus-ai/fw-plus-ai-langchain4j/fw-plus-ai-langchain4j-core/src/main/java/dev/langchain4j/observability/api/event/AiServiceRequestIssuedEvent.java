package dev.langchain4j.observability.api.event;

import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.observability.event.DefaultAiServiceRequestIssuedEvent;

public interface AiServiceRequestIssuedEvent extends AiServiceEvent {
    ChatRequest request();

    @Override
    default Class<AiServiceRequestIssuedEvent> eventClass() { return AiServiceRequestIssuedEvent.class; }

    @Override
    default AiServiceRequestIssuedEventBuilder toBuilder() { return new AiServiceRequestIssuedEventBuilder(this); }

    static AiServiceRequestIssuedEventBuilder builder() { return new AiServiceRequestIssuedEventBuilder(); }

    class AiServiceRequestIssuedEventBuilder extends Builder<AiServiceRequestIssuedEvent> {
        private ChatRequest request;
        protected AiServiceRequestIssuedEventBuilder() {}
        protected AiServiceRequestIssuedEventBuilder(AiServiceRequestIssuedEvent source) { super(source); request(source.request()); }
        @Override public AiServiceRequestIssuedEventBuilder invocationContext(InvocationContext context) { return (AiServiceRequestIssuedEventBuilder) super.invocationContext(context); }
        public AiServiceRequestIssuedEventBuilder request(ChatRequest request) { this.request = request; return this; }
        public ChatRequest request() { return request; }
        @Override public AiServiceRequestIssuedEvent build() { return new DefaultAiServiceRequestIssuedEvent(this); }
    }
}
