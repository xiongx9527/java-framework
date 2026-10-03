package dev.langchain4j.model.openai;

import dev.ai4j.openai4j.chat.ChatCompletionChoice;
import dev.ai4j.openai4j.chat.ChatCompletionResponse;
import dev.ai4j.openai4j.chat.Delta;
import dev.ai4j.openai4j.chat.FunctionCall;
import dev.ai4j.openai4j.chat.ToolCall;
import dev.ai4j.openai4j.completion.CompletionChoice;
import dev.ai4j.openai4j.completion.CompletionResponse;
import dev.ai4j.openai4j.shared.Usage;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static dev.langchain4j.model.openai.InternalOpenAiHelper.finishReasonFrom;
import static dev.langchain4j.model.openai.InternalOpenAiHelper.tokenUsageFrom;
import static java.util.stream.Collectors.toList;

/**
 * This class needs to be thread safe because it is called when a streaming result comes back
 * and there is no guarantee that this thread will be the same as the one that initiated the request,
 * in fact it almost certainly won't be.
 */
public class OpenAiStreamingResponseBuilder {

    private final StringBuffer contentBuilder = new StringBuffer();

    private final StringBuffer toolNameBuilder = new StringBuffer();
    private final StringBuffer toolArgumentsBuilder = new StringBuffer();

    private final Map<Integer, ToolExecutionRequestBuilder> indexToToolExecutionRequestBuilder = new ConcurrentHashMap<>();

    private volatile TokenUsage tokenUsage;
    private volatile FinishReason finishReason;

    public void append(ChatCompletionResponse partialResponse) {
        if (partialResponse == null) {
            return;
        }

        Usage usage = partialResponse.usage();
        if (usage != null) {
            this.tokenUsage = tokenUsageFrom(usage);
        }

        List<ChatCompletionChoice> choices = partialResponse.choices();
        if (choices == null || choices.isEmpty()) {
            return;
        }

        ChatCompletionChoice chatCompletionChoice = choices.get(0);
        if (chatCompletionChoice == null) {
            return;
        }

        String finishReason = chatCompletionChoice.finishReason();
        if (finishReason != null) {
            this.finishReason = finishReasonFrom(finishReason);
        }

        Delta delta = chatCompletionChoice.delta();
        if (delta == null) {
            return;
        }

        String content = delta.content();
        if (content != null) {
            contentBuilder.append(content);
            // 官方此处 return，会丢掉同帧 tool_calls；Ollama 兼容端点正是这种格式
        }

        if (delta.functionCall() != null) {
            FunctionCall functionCall = delta.functionCall();

            if (functionCall.name() != null) {
                toolNameBuilder.append(functionCall.name());
            }

            if (functionCall.arguments() != null) {
                toolArgumentsBuilder.append(functionCall.arguments());
            }
        }

        if (delta.toolCalls() != null && !delta.toolCalls().isEmpty()) {
            for (ToolCall toolCall : delta.toolCalls()) {
                if (toolCall == null) {
                    continue;
                }
                ToolExecutionRequestBuilder toolExecutionRequestBuilder
                        = indexToToolExecutionRequestBuilder.computeIfAbsent(toolCall.index(), idx -> new ToolExecutionRequestBuilder());

                if (toolCall.id() != null) {
                    toolExecutionRequestBuilder.idBuilder.append(toolCall.id());
                }

                FunctionCall functionCall = toolCall.function();

                if (functionCall == null) {
                    continue;
                }

                if (functionCall.name() != null) {
                    toolExecutionRequestBuilder.nameBuilder.append(functionCall.name());
                }

                if (functionCall.arguments() != null) {
                    toolExecutionRequestBuilder.argumentsBuilder.append(functionCall.arguments());
                }
            }
        }
    }

    public void append(CompletionResponse partialResponse) {
        if (partialResponse == null) {
            return;
        }

        Usage usage = partialResponse.usage();
        if (usage != null) {
            this.tokenUsage = tokenUsageFrom(usage);
        }

        List<CompletionChoice> choices = partialResponse.choices();
        if (choices == null || choices.isEmpty()) {
            return;
        }

        CompletionChoice completionChoice = choices.get(0);
        if (completionChoice == null) {
            return;
        }

        String finishReason = completionChoice.finishReason();
        if (finishReason != null) {
            this.finishReason = finishReasonFrom(finishReason);
        }

        String token = completionChoice.text();
        if (token != null) {
            contentBuilder.append(token);
        }
    }

    public Response<AiMessage> build() {

        String content = contentBuilder.toString();
        String toolName = toolNameBuilder.toString();

        if (!toolName.isEmpty()) {
            ToolExecutionRequest toolExecutionRequest = ToolExecutionRequest.builder()
                    .name(toolName)
                    .arguments(toolArgumentsBuilder.toString())
                    .build();
            if (!content.isEmpty()) {
                return Response.from(
                        AiMessage.from(content, java.util.Collections.singletonList(toolExecutionRequest)),
                        tokenUsage,
                        finishReason
                );
            }
            return Response.from(
                    AiMessage.from(toolExecutionRequest),
                    tokenUsage,
                    finishReason
            );
        }

        if (!indexToToolExecutionRequestBuilder.isEmpty()) {
            List<Integer> indexes = new java.util.ArrayList<>(indexToToolExecutionRequestBuilder.keySet());
            java.util.Collections.sort(indexes);
            List<ToolExecutionRequest> toolExecutionRequests = new java.util.ArrayList<>();
            for (Integer index : indexes) {
                ToolExecutionRequestBuilder it = indexToToolExecutionRequestBuilder.get(index);
                toolExecutionRequests.add(ToolExecutionRequest.builder()
                        .id(it.idBuilder.toString())
                        .name(it.nameBuilder.toString())
                        .arguments(it.argumentsBuilder.toString())
                        .build());
            }
            if (!content.isEmpty()) {
                return Response.from(
                        AiMessage.from(content, toolExecutionRequests),
                        tokenUsage,
                        finishReason
                );
            }
            return Response.from(
                    AiMessage.from(toolExecutionRequests),
                    tokenUsage,
                    finishReason
            );
        }

        return Response.from(AiMessage.from(content), tokenUsage, finishReason);
    }

    private static class ToolExecutionRequestBuilder {

        private final StringBuffer idBuilder = new StringBuffer();
        private final StringBuffer nameBuilder = new StringBuffer();
        private final StringBuffer argumentsBuilder = new StringBuffer();
    }
}
