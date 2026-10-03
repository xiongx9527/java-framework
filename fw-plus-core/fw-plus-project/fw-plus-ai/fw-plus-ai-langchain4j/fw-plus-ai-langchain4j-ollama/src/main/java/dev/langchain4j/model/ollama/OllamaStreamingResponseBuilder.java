package dev.langchain4j.model.ollama;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;

import java.util.ArrayList;
import java.util.List;

import static dev.langchain4j.model.ollama.OllamaMessagesUtils.toToolExecutionRequest;

/**
 * This class needs to be thread safe because it is called when a streaming result comes back
 * and there is no guarantee that this thread will be the same as the one that initiated the request,
 * in fact it almost certainly won't be.
 *
 * <p><b>fw-plus 覆盖版本（同 FQCN 类路径覆盖 langchain4j-ollama 0.35.0）</b>：
 * 上游只累积 {@code content} 且 {@code build()} 在内容为空时返回 {@code null}（挂 listener 时
 * 会在 {@code ChatModelResponseContext} 构造处抛 {@code response cannot be null}）。
 * 本覆盖补齐两点：累积 {@code message.toolCalls}（Ollama 原生 /api/chat 的工具调用以完整
 * 结构下发，直接收集即可）；{@code build()} 永不返回 {@code null}。</p>
 */
class OllamaStreamingResponseBuilder {

    private StringBuffer contentBuilder = new StringBuffer();
    private final List<ToolCall> toolCalls = new ArrayList<>();
    private volatile TokenUsage tokenUsage;

    void append(ChatResponse partialResponse) {
        if (partialResponse == null) {
            return;
        }

        if (partialResponse.getEvalCount() != null && partialResponse.getPromptEvalCount() != null) {
            this.tokenUsage = new TokenUsage(
                    partialResponse.getPromptEvalCount(),
                    partialResponse.getEvalCount()
            );
        }

        if (partialResponse.getMessage() == null) {
            return;
        }

        String content = partialResponse.getMessage().getContent();
        if (content != null) {
            contentBuilder.append(content);
        }

        if (partialResponse.getMessage().getToolCalls() != null) {
            toolCalls.addAll(partialResponse.getMessage().getToolCalls());
        }
    }

    /** 永不返回 null：上游在此场景返回 null，是挂 listener 时流式链路报错的根因之一 */
    Response<AiMessage> build() {
        String content = contentBuilder.toString();

        if (!toolCalls.isEmpty()) {
            List<ToolExecutionRequest> toolExecutionRequests = toToolExecutionRequest(toolCalls);
            AiMessage aiMessage = content.isEmpty()
                    ? AiMessage.from(toolExecutionRequests)
                    : AiMessage.from(content, toolExecutionRequests);
            return Response.from(aiMessage, tokenUsage);
        }

        if (!content.isEmpty()) {
            return Response.from(
                    AiMessage.from(content),
                    tokenUsage
            );
        }

        // 兜底：空回复（如模型静默结束）也给出非 null response，空串对 AiMessage 合法
        return Response.from(
                AiMessage.from(content),
                tokenUsage
        );
    }
}
