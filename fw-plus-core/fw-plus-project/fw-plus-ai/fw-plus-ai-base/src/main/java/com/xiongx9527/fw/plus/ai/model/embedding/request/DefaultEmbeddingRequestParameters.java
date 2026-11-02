package com.xiongx9527.fw.plus.ai.model.embedding.request;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 嵌入请求参数的默认实现。
 *
 * <p><b>作用</b>：用「参数声明 → 值」的 Map 存储参数，新增参数时无需修改本类，
 * 只需在 {@link EmbeddingRequestParameters} 中增加一个常量声明。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.embedding.request.DefaultEmbeddingRequestParameters}。</p>
 *
 * <p><b>实现约定</b>：不可变；值为 {@code null} 的参数不会进入 {@link #presentParameters()}。</p>
 *
 * @author xiongxiao
 */
public class DefaultEmbeddingRequestParameters implements EmbeddingRequestParameters {

    private final Map<EmbeddingParameter<?>, Object> values;

    /**
     * 由构建器构造。
     *
     * @param builder 构建器
     */
    protected DefaultEmbeddingRequestParameters(Builder<?> builder) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<EmbeddingParameter<?>, Object>(builder.values));
    }

    @Override
    public String modelName() {
        return parameter(MODEL_NAME);
    }

    @Override
    public Integer dimensions() {
        return parameter(DIMENSIONS);
    }

    @Override
    public EmbeddingInputType inputType() {
        return parameter(INPUT_TYPE);
    }

    @Override
    public <T> T parameter(EmbeddingParameter<T> parameter) {
        return parameter.cast(values.get(parameter));
    }

    @Override
    public Set<EmbeddingParameter<?>> presentParameters() {
        return Collections.unmodifiableSet(new LinkedHashSet<EmbeddingParameter<?>>(values.keySet()));
    }

    @Override
    public EmbeddingRequestParameters overrideWith(EmbeddingRequestParameters other) {
        if (other == null || other.presentParameters().isEmpty()) {
            return this;
        }
        Builder<?> builder = builder();
        builder.values.putAll(this.values);
        builder.overrideWith(other);
        return builder.build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DefaultEmbeddingRequestParameters)) {
            return false;
        }
        DefaultEmbeddingRequestParameters that = (DefaultEmbeddingRequestParameters) o;
        return Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(values);
    }

    @Override
    public String toString() {
        return "EmbeddingRequestParameters" + values;
    }

    /**
     * 创建构建器。
     *
     * @return 构建器
     */
    @SuppressWarnings("rawtypes")
    public static Builder<?> builder() {
        return new Builder();
    }

    /**
     * 构建器。
     *
     * @param <B> 具体构建器类型（便于子类链式调用）
     */
    public static class Builder<B extends Builder<B>> {

        /** 参数值集合。 */
        protected final Map<EmbeddingParameter<?>, Object> values = new LinkedHashMap<EmbeddingParameter<?>, Object>();

        /** 构造空构建器。 */
        public Builder() {
        }

        /**
         * 设置任意参数。
         *
         * @param parameter 参数声明
         * @param value     参数值
         * @param <T>       参数值类型
         * @return 本构建器
         */
        public <T> B set(EmbeddingParameter<T> parameter, T value) {
            if (value == null) {
                values.remove(parameter);
            } else {
                values.put(parameter, value);
            }
            return self();
        }

        /**
         * 设置模型名。
         *
         * @param modelName 模型名
         * @return 本构建器
         */
        public B modelName(String modelName) {
            return set(MODEL_NAME, modelName);
        }

        /**
         * 设置输出维度。
         *
         * @param dimensions 维度
         * @return 本构建器
         */
        public B dimensions(Integer dimensions) {
            return set(DIMENSIONS, dimensions);
        }

        /**
         * 设置输入用途。
         *
         * @param inputType 用途
         * @return 本构建器
         */
        public B inputType(EmbeddingInputType inputType) {
            return set(INPUT_TYPE, inputType);
        }

        /**
         * 用另一份参数覆盖当前已设置的项。
         *
         * @param other 覆盖来源
         * @return 本构建器
         */
        public B overrideWith(EmbeddingRequestParameters other) {
            if (other == null) {
                return self();
            }
            for (EmbeddingParameter<?> parameter : other.presentParameters()) {
                putUntyped(parameter, other.parameter(parameter));
            }
            return self();
        }

        /**
         * 返回具体构建器类型。
         *
         * @return 本构建器
         */
        @SuppressWarnings("unchecked")
        protected B self() {
            return (B) this;
        }

        /**
         * 构建参数对象。
         *
         * @return 参数对象
         */
        public DefaultEmbeddingRequestParameters build() {
            return new DefaultEmbeddingRequestParameters(this);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private void putUntyped(EmbeddingParameter<?> parameter, Object value) {
            if (value != null) {
                values.put(parameter, value);
            }
        }
    }
}
