package com.xiongx9527.fw.plus.ai.base.tool;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import com.xiongx9527.fw.plus.ai.base.model.LlmUsage;
import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionException;
import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionRequest;
import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionResult;
import com.xiongx9527.fw.plus.ai.base.tool.ToolProvider;
import com.xiongx9527.fw.plus.ai.base.tool.ToolSpecification;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 工具调用环：把「模型 ↔ 工具」的多轮循环收敛成一个入口。
 *
 * <p>这是 support 对 base 能力的组合——base 只提供「模型能 emit 工具调用」与「工具可被执行」，
 * 真正的「选工具、跑循环、截断、回填」由本类完成。业务只调一次，不用自己拼 tools[]、
 * 解析 tool_calls、跑循环、回灌结果。</p>
 *
 * <p>循环：每步带上可见工具请求模型；若模型返回工具调用，逐一执行并把结果回填为
 * {@link ChatMessage#tool(String, String, String)}，直到模型不再请求工具或达到最大步数。
 * 流式场景通过 {@link ToolCallingSink#onDelta} 实时转发最终答案增量。</p>
 */
public final class ToolCallingLoop {

    private static final int DEFAULT_MAX_STEPS = 5;

    private static final int DEFAULT_MAX_RESULT_CHARS = 4000;

    private final LlmPort llm;

    private final ToolProvider tools;

    private final int defaultMaxSteps;

    private final int maxResultChars;

    private final List<ToolCallingListener> listeners;

    public ToolCallingLoop(LlmPort llm, ToolProvider tools) {
        this(llm, tools, DEFAULT_MAX_STEPS, DEFAULT_MAX_RESULT_CHARS, null);
    }

    public ToolCallingLoop(LlmPort llm, ToolProvider tools, int defaultMaxSteps, int maxResultChars,
                           List<ToolCallingListener> listeners) {
        if (llm == null) {
            throw new IllegalArgumentException("llm port is required");
        }
        this.llm = llm;
        this.tools = tools;
        this.defaultMaxSteps = defaultMaxSteps <= 0 ? DEFAULT_MAX_STEPS : defaultMaxSteps;
        this.maxResultChars = maxResultChars <= 0 ? DEFAULT_MAX_RESULT_CHARS : maxResultChars;
        this.listeners = listeners;
    }

    /** 非流式：返回完整结果（sink 传 {@code null} 即可）。 */
    public ToolCallingResult call(ToolCallingRequest request) {
        return call(request, null);
    }

    /**
     * 执行工具调用环。
     *
     * @param sink 流式输出槽；{@code null} 表示不转发增量
     */
    public ToolCallingResult call(ToolCallingRequest request, ToolCallingSink sink) {
        if (request == null || request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new IllegalArgumentException("tool calling request requires messages");
        }
        List<ToolSpecification> specs = resolveSpecs(request.getAllowedTools());
        List<ChatMessage> messages = new ArrayList<ChatMessage>(request.getMessages());
        int maxSteps = request.getMaxSteps() > 0 ? request.getMaxSteps() : defaultMaxSteps;
        LlmUsage total = LlmUsage.ZERO;
        List<ToolTrace> traces = new ArrayList<ToolTrace>();

        for (int step = 0; step < maxSteps; step++) {
            LlmRequest llmRequest = LlmRequest.builder()
                    .messages(messages)
                    .model(request.getModel())
                    .tools(specs)
                    .toolChoice(specs.isEmpty()
                            ? com.xiongx9527.fw.plus.ai.base.tool.ToolChoice.none()
                            : com.xiongx9527.fw.plus.ai.base.tool.ToolChoice.auto())
                    .build();
            SinkStreamHandler handler = new SinkStreamHandler(sink);
            notifyStepStart();
            llm.stream(llmRequest, handler);
            total = sum(total, handler.usage);

            if (handler.errored) {
                notifyStepEnd();
                return new ToolCallingResult(handler.content.toString(), traces, total, ToolStopReason.ERROR);
            }

            if (handler.toolCalls.isEmpty()) {
                notifyStepEnd();
                String answer = handler.content.length() == 0 ? null : handler.content.toString();
                return new ToolCallingResult(answer, traces, total, ToolStopReason.FINAL);
            }

            messages.add(ChatMessage.assistantWithToolCalls(handler.toolCalls,
                    handler.content.length() == 0 ? null : handler.content.toString()));
            for (ToolExecutionRequest call : handler.toolCalls) {
                ToolTrace trace = execute(call, sink);
                traces.add(trace);
                messages.add(ChatMessage.tool(call.getId(), call.getName(), truncate(lastResult)));
            }
            notifyStepEnd();
        }
        return new ToolCallingResult(null, traces, total, ToolStopReason.MAX_STEPS);
    }

    private String lastResult;

    private List<ToolSpecification> resolveSpecs(Set<String> allowed) {
        if (tools == null) {
            return new ArrayList<ToolSpecification>();
        }
        List<ToolSpecification> all = tools.specifications();
        if (allowed == null || allowed.isEmpty()) {
            return new ArrayList<ToolSpecification>(all);
        }
        List<ToolSpecification> filtered = new ArrayList<ToolSpecification>();
        for (ToolSpecification spec : all) {
            if (allowed.contains(spec.getName())) {
                filtered.add(spec);
            }
        }
        return filtered;
    }

    private ToolTrace execute(ToolExecutionRequest call, ToolCallingSink sink) {
        String name = call.getName();
        long start = System.currentTimeMillis();
        if (listeners != null) {
            for (ToolCallingListener listener : listeners) {
                listener.onToolStart(name);
            }
        }
        boolean ok;
        ToolExecutionResult result;
        try {
            String raw = tools == null ? null : tools.execute(call);
            lastResult = raw == null ? "" : raw;
            ok = true;
            result = ToolExecutionResult.success(call, lastResult);
        } catch (ToolExecutionException e) {
            lastResult = "tool error: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            ok = false;
            result = ToolExecutionResult.failure(call, lastResult);
        } catch (RuntimeException e) {
            lastResult = "tool error: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            ok = false;
            result = ToolExecutionResult.failure(call, lastResult);
        }
        long latency = System.currentTimeMillis() - start;
        if (listeners != null) {
            for (ToolCallingListener listener : listeners) {
                listener.onToolEnd(name, ok, latency);
            }
        }
        if (sink != null) {
            sink.onToolResponse(result);
        }
        return new ToolTrace(name, ToolTrace.summarize(call.getArguments(), 200), ok, latency);
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxResultChars ? text : text.substring(0, maxResultChars);
    }

    private static LlmUsage sum(LlmUsage a, LlmUsage b) {
        if (a == null) {
            a = LlmUsage.ZERO;
        }
        if (b == null) {
            b = LlmUsage.ZERO;
        }
        return new LlmUsage(a.getInputTokens() + b.getInputTokens(),
                a.getOutputTokens() + b.getOutputTokens(),
                a.getTotalTokens() + b.getTotalTokens());
    }

    private void notifyStepStart() {
        if (listeners != null) {
            for (ToolCallingListener listener : listeners) {
                listener.onStepStart();
            }
        }
    }

    private void notifyStepEnd() {
        if (listeners != null) {
            for (ToolCallingListener listener : listeners) {
                listener.onStepEnd();
            }
        }
    }

    /** 收集单次模型调用的增量、工具调用与用量。 */
    private static final class SinkStreamHandler implements LlmStreamHandler {

        private final ToolCallingSink sink;

        private final StringBuilder content = new StringBuilder();

        private final StringBuilder reasoning = new StringBuilder();

        private final List<ToolExecutionRequest> toolCalls = new ArrayList<ToolExecutionRequest>();

        private LlmUsage usage = LlmUsage.ZERO;

        private boolean errored;

        private SinkStreamHandler(ToolCallingSink sink) {
            this.sink = sink;
        }

        @Override
        public void onToken(String delta, String reasoningDelta) {
            if (delta != null) {
                content.append(delta);
                if (sink != null) {
                    sink.onDelta(delta, null);
                }
            }
            if (reasoningDelta != null) {
                reasoning.append(reasoningDelta);
                if (sink != null) {
                    sink.onDelta(null, reasoningDelta);
                }
            }
        }

        @Override
        public void onToolCall(ToolExecutionRequest call) {
            if (call != null) {
                toolCalls.add(call);
            }
        }

        @Override
        public void onComplete() {
            // 单步结束
        }

        @Override
        public void onError(String message) {
            errored = true;
            if (sink != null) {
                sink.onError(message);
            }
        }

        @Override
        public void onUsageDetail(long inputTokens, long outputTokens, long totalTokens) {
            this.usage = new LlmUsage(inputTokens, outputTokens, totalTokens);
        }
    }
}
