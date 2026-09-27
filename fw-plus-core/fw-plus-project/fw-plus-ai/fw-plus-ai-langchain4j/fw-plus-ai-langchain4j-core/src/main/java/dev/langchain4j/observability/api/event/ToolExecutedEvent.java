package dev.langchain4j.observability.api.event;

import dev.langchain4j.Experimental;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.observability.event.DefaultToolExecutedEvent;
import java.util.List;

public interface ToolExecutedEvent extends AiServiceEvent {
    ToolExecutionRequest request();
    String resultText();
    @Experimental List<Content> resultContents();
    @Override default Class<ToolExecutedEvent> eventClass() { return ToolExecutedEvent.class; }
    @Override default ToolExecutedEventBuilder toBuilder() { return new ToolExecutedEventBuilder(this); }
    static ToolExecutedEventBuilder builder() { return new ToolExecutedEventBuilder(); }

    class ToolExecutedEventBuilder extends Builder<ToolExecutedEvent> {
        private ToolExecutionRequest request;
        private String resultText;
        private List<Content> resultContents;
        protected ToolExecutedEventBuilder() {}
        protected ToolExecutedEventBuilder(ToolExecutedEvent source) { super(source); request(source.request()); resultContents(source.resultContents()); }
        @Override public ToolExecutedEventBuilder invocationContext(InvocationContext context) { return (ToolExecutedEventBuilder) super.invocationContext(context); }
        public ToolExecutedEventBuilder request(ToolExecutionRequest request) { this.request = request; return this; }
        public ToolExecutedEventBuilder resultText(String resultText) { this.resultText = resultText; return this; }
        @Experimental public ToolExecutedEventBuilder resultContents(List<Content> contents) { this.resultContents = contents; return this; }
        public ToolExecutionRequest request() { return request; }
        public String resultText() { return resultText; }
        public List<Content> resultContents() { return resultContents; }
        @Override public ToolExecutedEvent build() { return new DefaultToolExecutedEvent(this); }
    }
}
