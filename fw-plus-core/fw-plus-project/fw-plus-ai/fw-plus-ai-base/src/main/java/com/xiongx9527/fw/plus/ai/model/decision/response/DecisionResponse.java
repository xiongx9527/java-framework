package com.xiongx9527.fw.plus.ai.model.decision.response;

import com.xiongx9527.fw.plus.ai.model.output.TokenUsage;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 决策响应。
 *
 * <p><b>作用</b>：以「问题标识 → 答案」的形式返回决策结论，并保留模型名与令牌用量。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。逐成员镜像
 * {@code dev.langchain4j.model.decision.response.DecisionResponse}。</p>
 *
 * <p><b>实现约定</b>：通过 {@link #answer(String, Class)} 按问题形态取答案；
 * {@link #isRefused(String)} 用于判断是否被拒绝。</p>
 *
 * @author xiongxiao
 */
public final class DecisionResponse {

    private final Map<String, DecisionAnswer> answers;
    private final DecisionResponseMetadata metadata;
    private final String modelName;
    private final TokenUsage tokenUsage;

    private DecisionResponse(Builder builder) {
        this.answers = Collections.unmodifiableMap(
                new LinkedHashMap<String, DecisionAnswer>(builder.answers));
        this.metadata = builder.metadata == null ? DecisionResponseMetadata.builder().build() : builder.metadata;
        this.modelName = builder.modelName != null ? builder.modelName : this.metadata.modelName();
        this.tokenUsage = builder.tokenUsage != null ? builder.tokenUsage : this.metadata.tokenUsage();
    }

    /**
     * 全部答案（标识 → 答案）。
     *
     * @return 只读 Map，非 {@code null}
     */
    public Map<String, DecisionAnswer> answers() {
        return answers;
    }

    /**
     * 是否型答案。
     *
     * @param key 问题标识
     * @return 答案；不存在或类型不符时为 {@code null}
     */
    public YesNoAnswer yesNo(String key) {
        return answer(key, YesNoAnswer.class);
    }

    /**
     * 选择型答案。
     *
     * @param key 问题标识
     * @return 答案；不存在或类型不符时为 {@code null}
     */
    public ChoiceAnswer choice(String key) {
        return answer(key, ChoiceAnswer.class);
    }

    /**
     * 量表型答案。
     *
     * @param key 问题标识
     * @return 答案；不存在或类型不符时为 {@code null}
     */
    public ScaleAnswer scale(String key) {
        return answer(key, ScaleAnswer.class);
    }

    /**
     * 按问题标识与形态取答案。
     *
     * @param key   问题标识
     * @param type  答案类型
     * @param <A>   答案类型
     * @return 答案；不存在或类型不符时为 {@code null}
     */
    public <A extends DecisionAnswer> A answer(String key, Class<A> type) {
        DecisionAnswer answer = answers.get(key);
        if (type.isInstance(answer)) {
            return type.cast(answer);
        }
        return null;
    }

    /**
     * 判断某问题是否被拒绝回答。
     *
     * @param key 问题标识
     * @return 被拒绝时为 {@code true}
     */
    public boolean isRefused(String key) {
        return answers.get(key) instanceof RefusalAnswer;
    }

    /**
     * 响应元数据。
     *
     * @return 元数据，非 {@code null}
     */
    public DecisionResponseMetadata metadata() {
        return metadata;
    }

    /**
     * 模型名。
     *
     * @return 模型名；未知时为 {@code null}
     */
    public String modelName() {
        return modelName;
    }

    /**
     * 令牌用量。
     *
     * @return 令牌用量；未知时为 {@code null}
     */
    public TokenUsage tokenUsage() {
        return tokenUsage;
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
        if (!(o instanceof DecisionResponse)) {
            return false;
        }
        DecisionResponse that = (DecisionResponse) o;
        return Objects.equals(answers, that.answers) && Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(answers, metadata);
    }

    @Override
    public String toString() {
        return "DecisionResponse{answers=" + answers.keySet() + ", metadata=" + metadata + "}";
    }

    /** {@link DecisionResponse} 构建器。 */
    public static class Builder {

        private final Map<String, DecisionAnswer> answers = new LinkedHashMap<String, DecisionAnswer>();
        private DecisionResponseMetadata metadata;
        private String modelName;
        private TokenUsage tokenUsage;

        /** 构造空构建器。 */
        public Builder() {
        }

        /**
         * 批量设置答案。
         *
         * @param answers 答案集合
         * @return 本构建器
         */
        public Builder answers(Map<String, ? extends DecisionAnswer> answers) {
            this.answers.clear();
            if (answers != null) {
                this.answers.putAll(answers);
            }
            return this;
        }

        /**
         * 追加一个答案。
         *
         * @param key    问题标识
         * @param answer 答案
         * @return 本构建器
         */
        public Builder answer(String key, DecisionAnswer answer) {
            this.answers.put(key, answer);
            return this;
        }

        /**
         * 设置响应元数据。
         *
         * @param metadata 元数据
         * @return 本构建器
         */
        public Builder metadata(DecisionResponseMetadata metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * 设置模型名。
         *
         * @param modelName 模型名
         * @return 本构建器
         */
        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        /**
         * 设置令牌用量。
         *
         * @param tokenUsage 令牌用量
         * @return 本构建器
         */
        public Builder tokenUsage(TokenUsage tokenUsage) {
            this.tokenUsage = tokenUsage;
            return this;
        }

        /**
         * 构建响应。
         *
         * @return 响应对象
         */
        public DecisionResponse build() {
            return new DecisionResponse(this);
        }
    }
}
