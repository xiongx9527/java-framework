package com.xiongx9527.fw.plus.ai.model.decision.response;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 量表型答案。
 *
 * <p><b>作用</b>：对 {@link com.xiongx9527.fw.plus.ai.model.decision.request.ScaleQuestion}
 * 的回答，给出各刻度的概率分布与期望分值。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.decision.response.ScaleAnswer}。</p>
 *
 * @author xiongxiao
 */
public final class ScaleAnswer implements DecisionAnswer {

    private final double mean;
    private final List<Double> probabilities;
    private final Double confidence;

    private ScaleAnswer(Builder builder) {
        this.mean = builder.mean != null ? builder.mean : 0.0d;
        this.probabilities = Collections.unmodifiableList(
                new java.util.ArrayList<Double>(builder.probabilities));
        this.confidence = builder.confidence;
    }

    /**
     * 期望分值。
     *
     * @return 分值
     */
    public double mean() {
        return mean;
    }

    /**
     * 各刻度的概率分布。
     *
     * @return 只读列表，非 {@code null}
     */
    public List<Double> probabilities() {
        return probabilities;
    }

    /**
     * 置信度。
     *
     * @return 置信度；未提供时为 {@code null}
     */
    public Double confidence() {
        return confidence;
    }

    /**
     * 创建构建器。
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ScaleAnswer)) {
            return false;
        }
        ScaleAnswer that = (ScaleAnswer) o;
        return Double.compare(mean, that.mean) == 0
                && Objects.equals(probabilities, that.probabilities)
                && Objects.equals(confidence, that.confidence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mean, probabilities, confidence);
    }

    @Override
    public String toString() {
        return "ScaleAnswer{mean=" + mean + ", probabilities=" + probabilities + "}";
    }

    /** {@link ScaleAnswer} 构建器。 */
    public static class Builder {

        private Double mean;
        private final List<Double> probabilities = new java.util.ArrayList<Double>();
        private Double confidence;

        /** 构造空构建器。 */
        public Builder() {
        }

        /**
         * 设置期望分值。
         *
         * @param mean 分值
         * @return 本构建器
         */
        public Builder mean(Double mean) {
            this.mean = mean;
            return this;
        }

        /**
         * 设置概率分布。
         *
         * @param probabilities 概率列表
         * @return 本构建器
         */
        public Builder probabilities(List<Double> probabilities) {
            this.probabilities.clear();
            if (probabilities != null) {
                this.probabilities.addAll(probabilities);
            }
            return this;
        }

        /**
         * 设置置信度。
         *
         * @param confidence 置信度
         * @return 本构建器
         */
        public Builder confidence(Double confidence) {
            this.confidence = confidence;
            return this;
        }

        /**
         * 构建答案。
         *
         * @return 答案对象
         */
        public ScaleAnswer build() {
            return new ScaleAnswer(this);
        }
    }
}
