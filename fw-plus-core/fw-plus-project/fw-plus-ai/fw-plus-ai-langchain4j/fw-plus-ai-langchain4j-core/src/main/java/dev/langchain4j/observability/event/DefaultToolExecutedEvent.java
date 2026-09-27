package dev.langchain4j.observability.event;
import dev.langchain4j.Experimental;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.observability.api.event.ToolExecutedEvent;
import java.util.Collections;
import java.util.List;
import static dev.langchain4j.internal.Utils.copy;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
public class DefaultToolExecutedEvent extends AbstractAiServiceEvent implements ToolExecutedEvent {
    private final ToolExecutionRequest request; private final List<Content> contents;
    public DefaultToolExecutedEvent(ToolExecutedEventBuilder b){super(b);request=ensureNotNull(b.request(),"request");boolean t=b.resultText()!=null,c=b.resultContents()!=null&&!b.resultContents().isEmpty();if(t&&c)throw new IllegalArgumentException("resultText and resultContents are mutually exclusive");if(t)contents=Collections.<Content>singletonList(TextContent.from(b.resultText()));else if(c)contents=copy(b.resultContents());else throw new IllegalArgumentException("Either resultText or resultContents must be provided");}
    public ToolExecutionRequest request(){return request;} public String resultText(){if(contents.size()==1&&contents.get(0) instanceof TextContent)return ((TextContent)contents.get(0)).text();throw new IllegalStateException("resultText() cannot be called when resultContents contains non-text or multiple content elements. Use resultContents() instead.");} @Experimental public List<Content> resultContents(){return contents;}
}
