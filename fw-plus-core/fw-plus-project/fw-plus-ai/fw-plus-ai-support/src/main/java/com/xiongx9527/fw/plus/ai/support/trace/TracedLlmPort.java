package com.xiongx9527.fw.plus.ai.support.trace;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.LlmListener;
import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import com.xiongx9527.fw.plus.ai.base.model.LlmTokenStream;
import com.xiongx9527.fw.plus.ai.base.model.ModelInfo;
import com.xiongx9527.fw.plus.ai.base.model.ModelValidation;
import com.xiongx9527.fw.plus.ai.base.tool.ToolChoice;
import com.xiongx9527.fw.plus.ai.base.tool.ToolSpecification;
import com.xiongx9527.fw.plus.ai.base.trace.TracePort;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 给任意 {@link LlmPort} 挂链路追踪的装饰器：每次调用前把 {@link TracingLlmListener}
 * 注入请求，无需业务改调用点。
 */
public final class TracedLlmPort implements LlmPort {

    private final LlmPort delegate;

    private final TracePort trace;

    public TracedLlmPort(LlmPort delegate, TracePort trace) {
        this.delegate = delegate;
        this.trace = trace == null ? TracePort.noop() : trace;
    }

    @Override
    public LlmTokenStream stream(LlmRequest request) {
        return delegate.stream(withListener(request));
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        return delegate.complete(withListener(request));
    }

    @Override
    public List<ModelInfo> listModels() {
        return delegate.listModels();
    }

    @Override
    public ModelValidation validateModel(String model) {
        return delegate.validateModel(model);
    }

    private LlmRequest withListener(LlmRequest request) {
        List<LlmListener> listeners = new ArrayList<LlmListener>(request.getListeners());
        listeners.add(new TracingLlmListener(trace));
        return LlmRequest.builder()
                .messages(copy(request.getMessages()))
                .model(request.getModel())
                .temperature(request.getTemperature())
                .topP(request.getTopP())
                .maxTokens(request.getMaxTokens())
                .extraParams(request.getExtraParams())
                .listeners(listeners)
                .tools(request.getTools())
                .toolChoice(request.getToolChoice())
                .build();
    }

    private static List<ChatMessage> copy(List<ChatMessage> messages) {
        return new ArrayList<ChatMessage>(messages);
    }
}
