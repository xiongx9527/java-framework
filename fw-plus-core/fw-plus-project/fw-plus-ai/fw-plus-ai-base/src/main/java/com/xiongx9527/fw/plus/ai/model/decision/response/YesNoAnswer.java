package com.xiongx9527.fw.plus.ai.model.decision.response;

import java.util.Objects;

/**
 * 是否型答案。
 *
 * <p><b>作用</b>：对 {@link com.xiongx9527.fw.plus.ai.model.decision.request.YesNoQuestion}
 * 的回答，给出「是」的概率。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.decision.response.YesNoAnswer}。</p>
 *
 * @author xiongxiao
 */
public final class YesNoAnswer implements DecisionAnswer {

    private final double probability;

    private YesNoAnswer(Builder builder) {
        this.probability = builder.probability != null ? builder.probability : 0.0d;
    }

    /**
     * 「是」的概率。
     *
     * @return 概率，取值 [0, 1]
     */
    public double probability() {
        return probability;
    }

    /**
     * 判断在给定阈值下是否判为「是」。
     *
     * @param threshold 阈值
     * @return 概率 &gt; 阈值时为 {@code true}
     */
    public boolean isYes(double threshold) {
        return probability > threshold;
    }

    /**
     * 由概率创建。
     *
     * @param probability 概率
     * @return 答案对象
     */
    public static YesNoAnswer from(double probability) {
        return builder().probability(probability).build();
    }

    /**
     * 同 {@link #from(double)}。
     *
     * @param probability 概率
     * @return 答案对象
     */
    public static YesNoAnswer of(double probability) {
        return from(probability);
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
        if (!(o instanceof YesNoAnswer)) {
            return false;
        }
        return Double.compare(probability, ((YesNoAnswer) o).probability) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(probability);
    }

    @Override
    public String toString() {
        return "YesNoAnswer{probability=" + probability + "}";
    }

    /** {@link YesNoAnswer} 构建器。 */
    public static class Builder {

        private Double probability;

        /** 构造空构建器。 */
        public Builder() {
        }

        /**
         * 设置概率。
         *
         * @param probability 概率
         * @return 本构建器
         */
        public Builder probability(Double probability) {
            this.probability = probability;
            return this;
        }

        /**
         * 构建答案。
         *
         * @return 答案对象
         */
        public YesNoAnswer build() {
            return new YesNoAnswer(this);
        }
    }
}
