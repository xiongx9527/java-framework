package com.xiongx9527.fw.plus.ai.model.embedding.request;

import com.xiongx9527.fw.plus.ai.data.segment.TextSegment;
import com.xiongx9527.fw.plus.ai.message.Content;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 嵌入请求。
 *
 * <p><b>作用</b>：把「要嵌入哪些输入 + 用哪些参数」收成一个不可变对象，
 * 让批量嵌入、多模态嵌入、按用途区分（查询 / 文档）都能走同一个入口。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。逐成员镜像
 * {@code dev.langchain4j.model.embedding.request.EmbeddingRequest}。</p>
 *
 * <p><b>实现约定</b>：{@link #inputs()} 不可为空；{@link #parameters()} 未设置时取
 * {@link EmbeddingRequestParameters#EMPTY}；{@link #modelName()}、{@link #dimensions()}、
 * {@link #inputType()} 是从参数里读出的便捷方法。</p>
 *
 * @author xiongxiao
 */
public class EmbeddingRequest {

    private final List<EmbeddingInput> inputs;
    private final EmbeddingRequestParameters parameters;

    /**
     * 由构建器构造。
     *
     * @param builder 构建器
     */
    protected EmbeddingRequest(Builder builder) {
        this.inputs = Collections.unmodifiableList(new ArrayList<EmbeddingInput>(builder.inputs));
        this.parameters = builder.parameters == null ? EmbeddingRequestParameters.EMPTY : builder.parameters;
    }

    /**
     * 嵌入输入列表。
     *
     * @return 只读输入列表，非 {@code null}
     */
    public List<EmbeddingInput> inputs() {
        return inputs;
    }

    /**
     * 请求参数。
     *
     * @return 参数对象，非 {@code null}
     */
    public EmbeddingRequestParameters parameters() {
        return parameters;
    }

    /**
     * 模型名。
     *
     * @return 模型名；未设置时为 {@code null}
     */
    public String modelName() {
        return parameters.modelName();
    }

    /**
     * 输出维度。
     *
     * @return 维度；未设置时为 {@code null}
     */
    public Integer dimensions() {
        return parameters.dimensions();
    }

    /**
     * 输入用途。
     *
     * @return 用途；未设置时为 {@code null}
     */
    public EmbeddingInputType inputType() {
        return parameters.inputType();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EmbeddingRequest)) {
            return false;
        }
        EmbeddingRequest that = (EmbeddingRequest) o;
        return Objects.equals(inputs, that.inputs) && Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inputs, parameters);
    }

    @Override
    public String toString() {
        return "EmbeddingRequest{inputs=" + inputs + ", parameters=" + parameters + "}";
    }

    /**
     * 创建构建器。
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /** {@link EmbeddingRequest} 构建器。 */
    public static class Builder {

        private final List<EmbeddingInput> inputs = new ArrayList<EmbeddingInput>();
        private EmbeddingRequestParameters parameters;

        /** 构造空构建器。 */
        public Builder() {
        }

        /**
         * 追加一条文本输入。
         *
         * @param input 文本
         * @return 本构建器
         */
        public Builder input(String input) {
            inputs.add(EmbeddingInput.from(input));
            return this;
        }

        /**
         * 追加多条文本输入。
         *
         * @param inputs 文本数组
         * @return 本构建器
         */
        public Builder inputs(String... inputs) {
            for (String input : inputs) {
                input(input);
            }
            return this;
        }

        /**
         * 追加一条文本片段输入。
         *
         * @param textSegment 文本片段
         * @return 本构建器
         */
        public Builder textSegment(TextSegment textSegment) {
            inputs.add(EmbeddingInput.from(textSegment));
            return this;
        }

        /**
         * 追加多条文本片段输入。
         *
         * @param textSegments 文本片段列表
         * @return 本构建器
         */
        public Builder textSegments(List<TextSegment> textSegments) {
            if (textSegments != null) {
                for (TextSegment textSegment : textSegments) {
                    textSegment(textSegment);
                }
            }
            return this;
        }

        /**
         * 追加一条多模态内容输入。
         *
         * @param contents 内容块
         * @return 本构建器
         */
        public Builder input(Content... contents) {
            inputs.add(EmbeddingInput.from(Arrays.asList(contents)));
            return this;
        }

        /**
         * 追加一条已构造好的输入。
         *
         * @param input 输入
         * @return 本构建器
         */
        public Builder input(EmbeddingInput input) {
            inputs.add(input);
            return this;
        }

        /**
         * 追加多条已构造好的输入。
         *
         * @param inputs 输入数组
         * @return 本构建器
         */
        public Builder inputs(EmbeddingInput... inputs) {
            this.inputs.addAll(Arrays.asList(inputs));
            return this;
        }

        /**
         * 追加多条已构造好的输入。
         *
         * @param inputs 输入列表
         * @return 本构建器
         */
        public Builder inputs(List<EmbeddingInput> inputs) {
            this.inputs.addAll(inputs);
            return this;
        }

        /**
         * 设置请求参数。
         *
         * @param parameters 参数
         * @return 本构建器
         */
        public Builder parameters(EmbeddingRequestParameters parameters) {
            this.parameters = parameters;
            return this;
        }

        /**
         * 设置模型名（会并入参数）。
         *
         * @param modelName 模型名
         * @return 本构建器
         */
        public Builder modelName(String modelName) {
            return mergeParameter(EmbeddingRequestParameters.MODEL_NAME, modelName);
        }

        /**
         * 设置输出维度（会并入参数）。
         *
         * @param dimensions 维度
         * @return 本构建器
         */
        public Builder dimensions(Integer dimensions) {
            return mergeParameter(EmbeddingRequestParameters.DIMENSIONS, dimensions);
        }

        /**
         * 设置输入用途（会并入参数）。
         *
         * @param inputType 用途
         * @return 本构建器
         */
        public Builder inputType(EmbeddingInputType inputType) {
            return mergeParameter(EmbeddingRequestParameters.INPUT_TYPE, inputType);
        }

        /**
         * 构建请求。
         *
         * @return 请求对象
         */
        public EmbeddingRequest build() {
            if (inputs.isEmpty()) {
                throw new IllegalArgumentException("inputs must not be empty");
            }
            return new EmbeddingRequest(this);
        }

        private <T> Builder mergeParameter(EmbeddingParameter<T> parameter, T value) {
            DefaultEmbeddingRequestParameters.Builder<?> builder = DefaultEmbeddingRequestParameters.builder();
            if (parameters != null) {
                builder.overrideWith(parameters);
            }
            builder.set(parameter, value);
            this.parameters = builder.build();
            return this;
        }
    }
}
