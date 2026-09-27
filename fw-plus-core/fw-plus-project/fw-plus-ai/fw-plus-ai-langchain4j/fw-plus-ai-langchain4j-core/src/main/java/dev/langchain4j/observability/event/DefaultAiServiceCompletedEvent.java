package dev.langchain4j.observability.event;
import dev.langchain4j.observability.api.event.AiServiceCompletedEvent;
import java.util.Optional;
public class DefaultAiServiceCompletedEvent extends AbstractAiServiceEvent implements AiServiceCompletedEvent {
    private final Object result; public DefaultAiServiceCompletedEvent(AiServiceCompletedEventBuilder b){super(b);result=b.getResult();} public Optional<Object> result(){return Optional.ofNullable(result);}
}
