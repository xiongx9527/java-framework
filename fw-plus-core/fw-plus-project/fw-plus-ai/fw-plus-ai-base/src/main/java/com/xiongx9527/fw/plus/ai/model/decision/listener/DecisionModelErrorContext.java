package com.xiongx9527.fw.plus.ai.model.decision.listener;

import com.xiongx9527.fw.plus.ai.model.ModelProvider;
import com.xiongx9527.fw.plus.ai.model.decision.request.DecisionRequest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 决策异常上下文。
 *
 * <p><b>作用</b>：携带失败原因与原始请求，供「决策失败应放行还是拒绝」的策略层决策。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.decision.listener.DecisionModelErrorContext}。</p>
 *
 * @author xiongxiao
 */
public class DecisionModelErrorContext {

    private final Throwable error;
    private final DecisionRequest decisionRequest;
    private final ModelProvider modelProvider;
    private final Map<Object, Object> attributes;

    /**
     * 构造上下文。
     *
     * @param error           异常
     * @param decisionRequest 决策请求
     * @param modelProvider   厂商标识，可为 {@code null}
     * @param attributes      附加属性，可为 {@code null}
     */
    public DecisionModelErrorContext(Throwable error, DecisionRequest decisionRequest, ModelProvider modelProvider,
                                     Map<Object, Object> attributes) {
        this.error = error;
        this.decisionRequest = decisionRequest;
        this.modelProvider = modelProvider;
        this.attributes = attributes == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<Object, Object>(attributes));
    }

    /**
     * 异常。
     *
     * @return 异常对象
     */
    public Throwable error() {
        return error;
    }

    /**
     * 决策请求。
     *
     * @return 请求对象
     */
    public DecisionRequest decisionRequest() {
        return decisionRequest;
    }

    /**
     * 厂商标识。
     *
     * @return 厂商标识；未知时为 {@code null}
     */
    public ModelProvider modelProvider() {
        return modelProvider;
    }

    /**
     * 附加属性。
     *
     * @return 只读属性 Map，非 {@code null}
     */
    public Map<Object, Object> attributes() {
        return attributes;
    }
}
