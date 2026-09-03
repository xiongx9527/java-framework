package dev.langchain4j.observability.event;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.observability.api.event.AiServiceRequestIssuedEvent;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
public class DefaultAiServiceRequestIssuedEvent extends AbstractAiServiceEvent implements AiServiceRequestIssuedEvent {
    private final ChatRequest request; public DefaultAiServiceRequestIssuedEvent(AiServiceRequestIssuedEventBuilder b){super(b);request=ensureNotNull(b.request(),"request");} public ChatRequest request(){return request;}
}
