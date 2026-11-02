package com.xiongx9527.fw.plus.ai.model.moderation.listener;

import com.xiongx9527.fw.plus.ai.model.ModelProvider;
import com.xiongx9527.fw.plus.ai.model.moderation.ModerationRequest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 内容审核异常上下文。
 *
 * <p><b>作用</b>：携带失败原因与原始请求，供「审核不可用时应放行还是拒绝」的策略层决策。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.moderation.listener.ModerationModelErrorContext}。</p>
 *
 * @author xiongxiao
 */
public class ModerationModelErrorContext {

    private final Throwable error;
    private final ModerationRequest moderationRequest;
    private final ModelProvider modelProvider;
    private final Map<Object, Object> attributes;

    /**
     * 构造上下文。
     *
     * @param error             异常
     * @param moderationRequest 审核请求
     * @param modelProvider     厂商标识，可为 {@code null}
     * @param attributes        附加属性，可为 {@code null}
     */
    public ModerationModelErrorContext(Throwable error, ModerationRequest moderationRequest,
                                       ModelProvider modelProvider, Map<Object, Object> attributes) {
        this.error = error;
        this.moderationRequest = moderationRequest;
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
     * 审核请求。
     *
     * @return 请求对象
     */
    public ModerationRequest moderationRequest() {
        return moderationRequest;
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

    @Override
    public String toString() {
        return "ModerationModelErrorContext{error=" + error + ", moderationRequest=" + moderationRequest
                + ", modelProvider=" + modelProvider + "}";
    }
}
