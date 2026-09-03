package dev.langchain4j.observability.api.event;

import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.observability.event.DefaultAiServiceErrorEvent;

public interface AiServiceErrorEvent extends AiServiceEvent {
    Throwable error();
    @Override default Class<AiServiceErrorEvent> eventClass() { return AiServiceErrorEvent.class; }
    @Override default AiServiceErrorEventBuilder toBuilder() { return new AiServiceErrorEventBuilder(this); }
    static AiServiceErrorEventBuilder builder() { return new AiServiceErrorEventBuilder(); }

    class AiServiceErrorEventBuilder extends Builder<AiServiceErrorEvent> {
        private Throwable error;
        protected AiServiceErrorEventBuilder() {}
        protected AiServiceErrorEventBuilder(AiServiceErrorEvent source) { super(source); error(source.error()); }
        @Override public AiServiceErrorEventBuilder invocationContext(InvocationContext context) { return (AiServiceErrorEventBuilder) super.invocationContext(context); }
        public AiServiceErrorEventBuilder error(Throwable error) { this.error = error; return this; }
        public Throwable getError() { return error; }
        @Override public AiServiceErrorEvent build() { return new DefaultAiServiceErrorEvent(this); }
    }
}
