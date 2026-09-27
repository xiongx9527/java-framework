package dev.langchain4j.observability.event;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.observability.api.event.AiServiceStartedEvent;
import java.util.Optional;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
public class DefaultAiServiceStartedEvent extends AbstractAiServiceEvent implements AiServiceStartedEvent {
    private final SystemMessage systemMessage; private final UserMessage userMessage;
    public DefaultAiServiceStartedEvent(AiServiceStartedEventBuilder b) { super(b); systemMessage=b.systemMessage(); userMessage=ensureNotNull(b.userMessage(),"userMessage"); }
    public Optional<SystemMessage> systemMessage(){return Optional.ofNullable(systemMessage);} public UserMessage userMessage(){return userMessage;}
}
