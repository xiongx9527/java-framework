package com.xiongx9527.fw.plus.ai.model.moderation.listener;

import com.xiongx9527.fw.plus.ai.model.ModelProvider;
import com.xiongx9527.fw.plus.ai.model.moderation.ModerationRequest;
import com.xiongx9527.fw.plus.ai.model.moderation.ModerationResponse;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 内容审核响应上下文。
 *
 * <p><b>作用</b>：在请求信息之外补充审核结论，便于监听器统计拦截率。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.moderation.listener.ModerationModelResponseContext}。</p>
 *
 * @author xiongxiao
 */
public class ModerationModelResponseContext {

    private final ModerationResponse moderationResponse;
    private final ModerationRequest moderationRequest;
    private final ModelProvider modelProvider;
    private final Map<Object, Object> attributes;

    /**
     * 构造上下文。
     *
     * @param moderationResponse 审核响应
     * @param moderationRequest  审核请求
     * @param modelProvider      厂商标识，可为 {@code null}
     * @param attributes         附加属性，可为 {@code null}
     */
    public ModerationModelResponseContext(ModerationResponse moderationResponse, ModerationRequest moderationRequest,
                                          ModelProvider modelProvider, Map<Object, Object> attributes) {
        this.moderationResponse = moderationResponse;
        this.moderationRequest = moderationRequest;
        this.modelProvider = modelProvider;
        this.attributes = attributes == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<Object, Object>(attributes));
    }

    /**
     * 审核响应。
     *
     * @return 响应对象
     */
    public ModerationResponse moderationResponse() {
        return moderationResponse;
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
        return "ModerationModelResponseContext{moderationResponse=" + moderationResponse
                + ", modelProvider=" + modelProvider + ", attributes=" + attributes + "}";
    }
}
