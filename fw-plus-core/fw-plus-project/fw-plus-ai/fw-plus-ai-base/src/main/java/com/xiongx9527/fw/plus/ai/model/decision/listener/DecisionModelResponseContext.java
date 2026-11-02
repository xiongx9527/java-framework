package com.xiongx9527.fw.plus.ai.model.decision.listener;

import com.xiongx9527.fw.plus.ai.model.ModelProvider;
import com.xiongx9527.fw.plus.ai.model.decision.request.DecisionRequest;
import com.xiongx9527.fw.plus.ai.model.decision.response.DecisionResponse;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 决策响应上下文。
 *
 * <p><b>作用</b>：在请求信息之外补充决策结论，便于监听器统计命中率 / 拒绝率。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.decision.listener.DecisionModelResponseContext}。</p>
 *
 * @author xiongxiao
 */
public class DecisionModelResponseContext {

    private final DecisionResponse decisionResponse;
    private final DecisionRequest decisionRequest;
    private final ModelProvider modelProvider;
    private final Map<Object, Object> attributes;

    /**
     * 构造上下文。
     *
     * @param decisionResponse 决策响应
     * @param decisionRequest  决策请求
     * @param modelProvider    厂商标识，可为 {@code null}
     * @param attributes       附加属性，可为 {@code null}
     */
    public DecisionModelResponseContext(DecisionResponse decisionResponse, DecisionRequest decisionRequest,
                                        ModelProvider modelProvider, Map<Object, Object> attributes) {
        this.decisionResponse = decisionResponse;
        this.decisionRequest = decisionRequest;
        this.modelProvider = modelProvider;
        this.attributes = attributes == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<Object, Object>(attributes));
    }

    /**
     * 决策响应。
     *
     * @return 响应对象
     */
    public DecisionResponse decisionResponse() {
        return decisionResponse;
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
