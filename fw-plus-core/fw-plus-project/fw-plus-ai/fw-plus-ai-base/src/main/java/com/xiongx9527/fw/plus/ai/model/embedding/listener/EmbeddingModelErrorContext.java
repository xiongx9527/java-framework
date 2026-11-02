package com.xiongx9527.fw.plus.ai.model.embedding.listener;

import com.xiongx9527.fw.plus.ai.data.segment.TextSegment;
import com.xiongx9527.fw.plus.ai.model.ModelProvider;
import com.xiongx9527.fw.plus.ai.model.embedding.EmbeddingModel;
import com.xiongx9527.fw.plus.ai.model.embedding.request.EmbeddingRequest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 嵌入模型异常上下文。
 *
 * <p><b>作用</b>：携带失败原因与原始请求，供降级（跳过向量化、回退关键词检索）与告警使用。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.embedding.listener.EmbeddingModelErrorContext}。</p>
 *
 * @author xiongxiao
 */
public class EmbeddingModelErrorContext {

    private final Throwable error;
    private final EmbeddingRequest embeddingRequest;
    private final EmbeddingModel embeddingModel;
    private final ModelProvider modelProvider;
    private final Map<Object, Object> attributes;
    private final List<TextSegment> textSegments;

    /**
     * 由构建器构造。
     *
     * @param builder 构建器
     */
    public EmbeddingModelErrorContext(Builder builder) {
        this.error = builder.error;
        this.embeddingRequest = builder.embeddingRequest;
        this.embeddingModel = builder.embeddingModel;
        this.modelProvider = builder.modelProvider != null
                ? builder.modelProvider
                : (builder.embeddingModel == null ? null : builder.embeddingModel.provider());
        this.attributes = builder.attributes == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<Object, Object>(builder.attributes));
        this.textSegments = builder.textSegments == null
                ? Collections.<TextSegment>emptyList()
                : Collections.unmodifiableList(builder.textSegments);
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
     * 嵌入请求。
     *
     * @return 请求对象
     */
    public EmbeddingRequest embeddingRequest() {
        return embeddingRequest;
    }

    /**
     * 嵌入模型。
     *
     * @return 模型实例
     */
    public EmbeddingModel embeddingModel() {
        return embeddingModel;
    }

    /**
     * 厂商标识。
     *
     * @return 厂商标识；模型未声明时为 {@code null}
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

    /**
     * 文本片段视图。
     *
     * @return 只读文本片段列表，非 {@code null}
     */
    public List<TextSegment> textSegments() {
        return textSegments;
    }

    /**
     * 创建构建器。
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /** {@link EmbeddingModelErrorContext} 构建器。 */
    public static class Builder {

        private Throwable error;
        private EmbeddingRequest embeddingRequest;
        private EmbeddingModel embeddingModel;
        private ModelProvider modelProvider;
        private Map<Object, Object> attributes;
        private List<TextSegment> textSegments;

        /** 构造空构建器。 */
        Builder() {
        }

        /**
         * 设置异常。
         *
         * @param error 异常
         * @return 本构建器
         */
        public Builder error(Throwable error) {
            this.error = error;
            return this;
        }

        /**
         * 设置嵌入请求。
         *
         * @param embeddingRequest 请求
         * @return 本构建器
         */
        public Builder embeddingRequest(EmbeddingRequest embeddingRequest) {
            this.embeddingRequest = embeddingRequest;
            return this;
        }

        /**
         * 设置嵌入模型。
         *
         * @param embeddingModel 模型
         * @return 本构建器
         */
        public Builder embeddingModel(EmbeddingModel embeddingModel) {
            this.embeddingModel = embeddingModel;
            return this;
        }

        /**
         * 设置厂商标识。
         *
         * @param modelProvider 厂商标识
         * @return 本构建器
         */
        public Builder modelProvider(ModelProvider modelProvider) {
            this.modelProvider = modelProvider;
            return this;
        }

        /**
         * 设置附加属性。
         *
         * @param attributes 属性 Map
         * @return 本构建器
         */
        public Builder attributes(Map<Object, Object> attributes) {
            this.attributes = attributes;
            return this;
        }

        /**
         * 设置文本片段视图。
         *
         * @param textSegments 文本片段列表
         * @return 本构建器
         */
        public Builder textSegments(List<TextSegment> textSegments) {
            this.textSegments = textSegments;
            return this;
        }

        /**
         * 构建上下文。
         *
         * @return 上下文对象
         */
        public EmbeddingModelErrorContext build() {
            return new EmbeddingModelErrorContext(this);
        }
    }
}
