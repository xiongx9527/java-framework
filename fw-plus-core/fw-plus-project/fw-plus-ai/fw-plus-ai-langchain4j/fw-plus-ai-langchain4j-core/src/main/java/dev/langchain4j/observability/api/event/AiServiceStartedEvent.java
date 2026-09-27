package dev.langchain4j.observability.api.event;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.observability.event.DefaultAiServiceStartedEvent;
import java.util.Optional;

public interface AiServiceStartedEvent extends AiServiceEvent {
    Optional<SystemMessage> systemMessage();

    UserMessage userMessage();

    @Override
    default Class<AiServiceStartedEvent> eventClass() {
        return AiServiceStartedEvent.class;
    }

    @Override
    default AiServiceStartedEventBuilder toBuilder() {
        return new AiServiceStartedEventBuilder(this);
    }

    static AiServiceStartedEventBuilder builder() {
        return new AiServiceStartedEventBuilder();
    }

    class AiServiceStartedEventBuilder extends Builder<AiServiceStartedEvent> {
        private SystemMessage systemMessage;
        private UserMessage userMessage;

        protected AiServiceStartedEventBuilder() {
        }

        protected AiServiceStartedEventBuilder(AiServiceStartedEvent source) {
            super(source);
            systemMessage(source.systemMessage().orElse(null));
            userMessage(source.userMessage());
        }

        @Override
        public AiServiceStartedEventBuilder invocationContext(InvocationContext invocationContext) {
            return (AiServiceStartedEventBuilder) super.invocationContext(invocationContext);
        }

        public AiServiceStartedEventBuilder systemMessage(SystemMessage systemMessage) {
            this.systemMessage = systemMessage;
            return this;
        }

        public AiServiceStartedEventBuilder systemMessage(Optional<SystemMessage> systemMessage) {
            return systemMessage(systemMessage.orElse(null));
        }

        public AiServiceStartedEventBuilder userMessage(UserMessage userMessage) {
            this.userMessage = userMessage;
            return this;
        }

        public SystemMessage systemMessage() {
            return systemMessage;
        }

        public UserMessage userMessage() {
            return userMessage;
        }

        @Override
        public AiServiceStartedEvent build() {
            return new DefaultAiServiceStartedEvent(this);
        }
    }
}
