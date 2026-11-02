package com.xiongx9527.fw.plus.ai.model.decision.response;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 选择型答案。
 *
 * <p><b>作用</b>：对 {@link com.xiongx9527.fw.plus.ai.model.decision.request.ChoiceQuestion}
 * 的回答，给出结论选项与每个选项的概率分布。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.decision.response.ChoiceAnswer}。</p>
 *
 * @author xiongxiao
 */
public final class ChoiceAnswer implements DecisionAnswer {

    private final String value;
    private final Map<String, Double> probabilities;
    private final Double confidence;

    private ChoiceAnswer(Builder builder) {
        this.value = builder.value;
        this.probabilities = Collections.unmodifiableMap(
                new LinkedHashMap<String, Double>(builder.probabilities));
        this.confidence = builder.confidence;
    }

    /**
     * 结论选项标识。
     *
     * @return 选项标识；模型拒绝回答时为 {@code null}
     */
    public String value() {
        return value;
    }

    /**
     * 各选项的概率分布。
     *
     * @return 只读 Map，非 {@code null}
     */
    public Map<String, Double> probabilities() {
        return probabilities;
    }

    /**
     * 给定选项的概率。
     *
     * @param option 选项标识
     * @return 概率；不存在时为 {@code null}
     */
    public double probabilityOf(String option) {
        return probabilities.getOrDefault(option, 0.0d);
    }

    /**
     * 结论之外其余选项的整体「非结论」概率裕度（1 - 最高概率）。
     *
     * @return 裕度
     */
    public double margin() {
        double max = 0.0d;
        for (Double p : probabilities.values()) {
            if (p != null && p > max) {
                max = p;
            }
        }
        return 1.0d - max;
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
        if (!(o instanceof ChoiceAnswer)) {
            return false;
        }
        ChoiceAnswer that = (ChoiceAnswer) o;
        return Objects.equals(value, that.value)
                && Objects.equals(probabilities, that.probabilities)
                && Objects.equals(confidence, that.confidence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, probabilities, confidence);
    }

    @Override
    public String toString() {
        return "ChoiceAnswer{value='" + value + "', probabilities=" + probabilities + "}";
    }

    /** {@link ChoiceAnswer} 构建器。 */
    public static class Builder {

        private String value;
        private final Map<String, Double> probabilities = new LinkedHashMap<String, Double>();
        private Double confidence;

        /** 构造空构建器。 */
        public Builder() {
        }

        /**
         * 设置结论选项标识。
         *
         * @param value 选项标识
         * @return 本构建器
         */
        public Builder value(String value) {
            this.value = value;
            return this;
        }

        /**
         * 设置概率分布。
         *
         * @param probabilities 概率分布
         * @return 本构建器
         */
        public Builder probabilities(Map<String, Double> probabilities) {
            this.probabilities.clear();
            if (probabilities != null) {
                this.probabilities.putAll(probabilities);
            }
            return this;
        }

        /**
         * 设置单个选项的概率。
         *
         * @param option     选项标识
         * @param probability 概率
         * @return 本构建器
         */
        public Builder probability(String option, Double probability) {
            this.probabilities.put(option, probability);
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
         * 批量追加选项标识（概率留空）。
         *
         * @param options 选项标识集合
         * @return 本构建器
         */
        public Builder options(java.util.Collection<String> options) {
            if (options != null) {
                for (String option : options) {
                    this.probabilities.put(option, null);
                }
            }
            return this;
        }

        /**
         * 构建答案。
         *
         * @return 答案对象
         */
        public ChoiceAnswer build() {
            return new ChoiceAnswer(this);
        }
    }
}
