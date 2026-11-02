package com.xiongx9527.fw.plus.ai.model.decision.request;

import java.util.Objects;

/**
 * 决策请求参数的默认实现。
 *
 * <p><b>作用</b>：不可变地承载模型名，合并时以 {@code that} 中非空项优先。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.decision.request.DefaultDecisionRequestParameters}。</p>
 *
 * @author xiongxiao
 */
public class DefaultDecisionRequestParameters implements DecisionRequestParameters {

    private final String modelName;

    /**
     * 由构建器构造。
     *
     * @param builder 构建器
     */
    protected DefaultDecisionRequestParameters(Builder<?> builder) {
        this.modelName = builder.modelName;
    }

    @Override
    public String modelName() {
        return modelName;
    }

    @Override
    public DecisionRequestParameters overrideWith(DecisionRequestParameters that) {
        if (that == null || that.modelName() == null) {
            return this;
        }
        return builder().modelName(that.modelName()).build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DefaultDecisionRequestParameters)) {
            return false;
        }
        return Objects.equals(modelName, ((DefaultDecisionRequestParameters) o).modelName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(modelName);
    }

    @Override
    public String toString() {
        return "DecisionRequestParameters{modelName='" + modelName + "'}";
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
     * @param <T> 具体构建器类型
     */
    public static class Builder<T extends Builder<T>> {

        /** 模型名。 */
        protected String modelName;

        /** 构造空构建器。 */
        public Builder() {
        }

        /**
         * 设置模型名。
         *
         * @param modelName 模型名
         * @return 本构建器
         */
        public T modelName(String modelName) {
            this.modelName = modelName;
            return self();
        }

        /**
         * 用另一份参数覆盖当前已设置的项。
         *
         * @param that 覆盖来源
         * @return 本构建器
         */
        public T overrideWith(DecisionRequestParameters that) {
            if (that != null && that.modelName() != null) {
                this.modelName = that.modelName();
            }
            return self();
        }

        /**
         * 返回具体构建器类型。
         *
         * @return 本构建器
         */
        @SuppressWarnings("unchecked")
        protected T self() {
            return (T) this;
        }

        /**
         * 构建参数对象。
         *
         * @return 参数对象
         */
        public DefaultDecisionRequestParameters build() {
            return new DefaultDecisionRequestParameters(this);
        }
    }
}
