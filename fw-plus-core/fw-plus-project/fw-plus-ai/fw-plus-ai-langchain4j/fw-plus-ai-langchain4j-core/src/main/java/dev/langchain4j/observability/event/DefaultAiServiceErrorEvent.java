package dev.langchain4j.observability.event;
import dev.langchain4j.observability.api.event.AiServiceErrorEvent;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
public class DefaultAiServiceErrorEvent extends AbstractAiServiceEvent implements AiServiceErrorEvent {
    private final Throwable error; public DefaultAiServiceErrorEvent(AiServiceErrorEventBuilder b){super(b);error=ensureNotNull(b.getError(),"error");} public Throwable error(){return error;}
}
