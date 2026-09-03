package dev.langchain4j.service;

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.tool.ToolExecution;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

public class Result<T> {
    private final T content;
    private final TokenUsage tokenUsage;
    private final List<Content> sources;
    private final FinishReason finishReason;
    private final List<ToolExecution> toolExecutions;
    private final List<ChatResponse> intermediateResponses;
    private final ChatResponse finalResponse;

    public Result(ResultBuilder<T> builder) {
        content = builder.content;
        tokenUsage = builder.tokenUsage;
        sources = copy(builder.sources);
        finishReason = builder.finishReason;
        toolExecutions = copy(builder.toolExecutions);
        intermediateResponses = copy(builder.intermediateResponses);
        finalResponse = builder.finalResponse;
    }

    public Result(T content, TokenUsage tokenUsage, List<Content> sources,
                  FinishReason finishReason, List<ToolExecution> toolExecutions) {
        this.content = ensureNotNull(content, "content");
        this.tokenUsage = tokenUsage;
        this.sources = nullableCopy(sources);
        this.finishReason = finishReason;
        this.toolExecutions = nullableCopy(toolExecutions);
        intermediateResponses = Collections.emptyList();
        finalResponse = null;
    }

    public static <T> ResultBuilder<T> builder() { return new ResultBuilder<T>(); }
    public T content() { return content; }
    public TokenUsage tokenUsage() { return tokenUsage; }
    public List<Content> sources() { return sources; }
    public FinishReason finishReason() { return finishReason; }
    public List<ToolExecution> toolExecutions() { return toolExecutions; }
    public List<ChatResponse> intermediateResponses() { return intermediateResponses; }
    public ChatResponse finalResponse() { return finalResponse; }

    private static <E> List<E> copy(List<E> values) {
        return values == null ? Collections.<E>emptyList()
                : Collections.unmodifiableList(new ArrayList<E>(values));
    }
    private static <E> List<E> nullableCopy(List<E> values) {
        return values == null ? null : Collections.unmodifiableList(new ArrayList<E>(values));
    }

    public static class ResultBuilder<T> {
        private T content;
        private TokenUsage tokenUsage;
        private List<Content> sources;
        private FinishReason finishReason;
        private List<ToolExecution> toolExecutions;
        private List<ChatResponse> intermediateResponses;
        private ChatResponse finalResponse;
        ResultBuilder() { }
        public ResultBuilder<T> content(T value) { content = value; return this; }
        public ResultBuilder<T> tokenUsage(TokenUsage value) { tokenUsage = value; return this; }
        public ResultBuilder<T> sources(List<Content> value) { sources = value; return this; }
        public ResultBuilder<T> finishReason(FinishReason value) { finishReason = value; return this; }
        public ResultBuilder<T> toolExecutions(List<ToolExecution> value) { toolExecutions = value; return this; }
        public ResultBuilder<T> intermediateResponses(List<ChatResponse> value) { intermediateResponses = value; return this; }
        public ResultBuilder<T> finalResponse(ChatResponse value) { finalResponse = value; return this; }
        public Result<T> build() { return new Result<T>(this); }
    }
}
