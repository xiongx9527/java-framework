package com.xiongx9527.fw.plus.ai.support.trace;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.ChatRole;
import com.xiongx9527.fw.plus.ai.base.model.LlmListener;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;
import com.xiongx9527.fw.plus.ai.base.model.LlmUsage;
import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionRequest;
import com.xiongx9527.fw.plus.ai.base.tool.ToolSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 模型调用上下文日志：把每次发给模型的内容按类型拆开打印，便于排查 prompt 拼接效果。
 *
 * <p>覆盖六类内容：TOOLS（本轮工具清单）、SYSTEM（系统提示）、USER（用户原话）、
 * TOOL_CALL（模型请求的工具及参数）、TOOL_RESPONSE（工具执行结果）、RESPONSE（回复与 token 用量）。</p>
 *
 * <p>与具体观测后端无关，只是一个 {@link LlmListener} 实现：把它加入请求监听器即可，
 * 同步与流式两条链路都会经过这里。内部在 {@code onRequest} 暂存请求对象，
 * 在 {@code onResponse}/{@code onError} 时关联打印（与 {@code TracingLlmListener} 同构）。</p>
 */
public final class LoggingLlmListener implements LlmListener {

    private static final Logger log = LoggerFactory.getLogger(LoggingLlmListener.class);

    /** 单条消息最长打印字符数，超出截断，避免长文与长回复刷屏。 */
    private static final int MAX_LENGTH = 600;

    private LlmRequest currentRequest;

    @Override
    public void onRequest(LlmRequest request) {
        this.currentRequest = request;
        List<ChatMessage> messages = request.getMessages();
        int size = messages == null ? 0 : messages.size();
        log.info("==== model request context ({} messages) ====", size);
        logTools(request.getTools());
        if (messages != null) {
            int index = 0;
            for (ChatMessage message : messages) {
                logMessage(index++, message);
            }
        }
    }

    private void logTools(List<ToolSpecification> toolSpecifications) {
        if (toolSpecifications == null || toolSpecifications.isEmpty()) {
            log.info("TOOLS        no tools registered this turn");
            return;
        }
        log.info("TOOLS        {} tools available", toolSpecifications.size());
        for (ToolSpecification tool : toolSpecifications) {
            log.info("TOOLS        - {} : {}", tool.getName(), abbreviate(tool.getDescription()));
        }
    }

    @Override
    public void onResponse(LlmResponse response) {
        if (response == null) {
            return;
        }
        if (response.hasToolCalls()) {
            for (ToolExecutionRequest request : response.getToolCalls()) {
                log.info("TOOL_CALL    model requested tool {} args {}",
                        request.getName(), abbreviate(request.getArguments()));
            }
        } else {
            log.info("RESPONSE     model reply: {}", abbreviate(response.getContent()));
        }
        LlmUsage usage = response.getUsage();
        if (usage != null) {
            log.info("RESPONSE     token usage {}", usage);
        }
    }

    @Override
    public void onError(String message) {
        log.warn("model call failed: {}", message);
    }

    private void logMessage(int index, ChatMessage message) {
        ChatRole role = message.getRole();
        if (role == ChatRole.SYSTEM) {
            log.info("[{}] SYSTEM      {}", index, abbreviate(message.getContent()));
        } else if (role == ChatRole.USER) {
            log.info("[{}] USER        {}", index, abbreviate(message.getContent()));
        } else if (role == ChatRole.TOOL) {
            log.info("[{}] TOOL_RESPONSE tool {} returned: {}", index, message.getName(), abbreviate(message.getContent()));
        } else if (role == ChatRole.ASSISTANT) {
            logAiMessage(index, message);
        }
    }

    private void logAiMessage(int index, ChatMessage message) {
        if (!message.hasToolCalls()) {
            log.info("[{}] AI          history reply: {}", index, abbreviate(message.getContent()));
            return;
        }
        for (ToolExecutionRequest request : message.getToolCalls()) {
            log.info("[{}] TOOL_CALL    tool request {} args {}", index, request.getName(), abbreviate(request.getArguments()));
        }
    }

    private static String abbreviate(String text) {
        if (text == null) {
            return "null";
        }
        String normalized = text.replace('\n', ' ').replace('\r', ' ').trim();
        if (normalized.length() <= MAX_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_LENGTH) + " ...[truncated, original length " + text.length() + "]";
    }
}
