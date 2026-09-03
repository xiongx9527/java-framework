package dev.langchain4j.observability.api.event;

import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.observability.event.DefaultAiServiceCompletedEvent;
import java.util.Optional;

public interface AiServiceCompletedEvent extends AiServiceEvent {
    Optional<Object> result();
    @Override default Class<AiServiceCompletedEvent> eventClass() { return AiServiceCompletedEvent.class; }
    @Override default AiServiceCompletedEventBuilder toBuilder() { return new AiServiceCompletedEventBuilder(this); }
    static AiServiceCompletedEventBuilder builder() { return new AiServiceCompletedEventBuilder(); }

    class AiServiceCompletedEventBuilder extends Builder<AiServiceCompletedEvent> {
        private Object result;
        protected AiServiceCompletedEventBuilder() {}
        protected AiServiceCompletedEventBuilder(AiServiceCompletedEvent source) { super(source); result(source.result().orElse(null)); }
        @Override public AiServiceCompletedEventBuilder invocationContext(InvocationContext context) { return (AiServiceCompletedEventBuilder) super.invocationContext(context); }
        public AiServiceCompletedEventBuilder result(Object result) { this.result = result; return this; }
        public Object getResult() { return result; }
        @Override public AiServiceCompletedEvent build() { return new DefaultAiServiceCompletedEvent(this); }
    }
}
