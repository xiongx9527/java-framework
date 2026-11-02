package com.xiongx9527.fw.plus.ai.model.decision.request;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 选择型问题。
 *
 * <p><b>作用</b>：限定候选答案集合（键值对），让模型从给定选项中给出唯一结论，
 * 适用于「分类 / 路由 / 标签」型决策。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.decision.request.ChoiceQuestion}。</p>
 *
 * <p><b>实现约定</b>：不可变；{@link #options()} 的 Map 中键为选项标识、值为展示文本。</p>
 *
 * @author xiongxiao
 */
public final class ChoiceQuestion implements Question {

    private final String text;
    private final Map<String, String> options;

    private ChoiceQuestion(Builder builder) {
        if (builder.text == null || builder.text.trim().isEmpty()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        this.text = builder.text;
        this.options = Collections.unmodifiableMap(new LinkedHashMap<String, String>(builder.options));
    }

    @Override
    public String text() {
        return text;
    }

    /**
     * 候选选项（标识 → 展示文本）。
     *
     * @return 只读 Map，非 {@code null}
     */
    public Map<String, String> options() {
        return options;
    }

    /**
     * 由文本与键值对选项创建。
     *
     * @param text    问题文本
     * @param options 候选选项
     * @return 问题对象
     */
    public static ChoiceQuestion from(String text, Map<String, String> options) {
        return builder().text(text).options(options).build();
    }

    /**
     * 由文本与列表型选项创建（选项标识与展示文本相同）。
     *
     * @param text    问题文本
     * @param options 候选选项
     * @return 问题对象
     */
    public static ChoiceQuestion from(String text, java.util.List<String> options) {
        Builder builder = builder().text(text);
        if (options != null) {
            for (String option : options) {
                builder.option(option);
            }
        }
        return builder.build();
    }

    /**
     * 同 {@link #from(String, Map)}。
     *
     * @param text    问题文本
     * @param options 候选选项
     * @return 问题对象
     */
    public static ChoiceQuestion of(String text, Map<String, String> options) {
        return from(text, options);
    }

    /**
     * 同 {@link #from(String, List)}。
     *
     * @param text    问题文本
     * @param options 候选选项
     * @return 问题对象
     */
    public static ChoiceQuestion of(String text, java.util.List<String> options) {
        return from(text, options);
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
        if (!(o instanceof ChoiceQuestion)) {
            return false;
        }
        ChoiceQuestion that = (ChoiceQuestion) o;
        return Objects.equals(text, that.text) && Objects.equals(options, that.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, options);
    }

    @Override
    public String toString() {
        return "ChoiceQuestion{text='" + text + "', options=" + options + "}";
    }

    /** {@link ChoiceQuestion} 构建器。 */
    public static class Builder {

        private String text;
        private final Map<String, String> options = new LinkedHashMap<String, String>();

        /** 构造空构建器。 */
        public Builder() {
        }

        /**
         * 设置问题文本。
         *
         * @param text 文本
         * @return 本构建器
         */
        public Builder text(String text) {
            this.text = text;
            return this;
        }

        /**
         * 设置键值对选项。
         *
         * @param options 候选选项
         * @return 本构建器
         */
        public Builder options(Map<String, String> options) {
            this.options.clear();
            if (options != null) {
                this.options.putAll(options);
            }
            return this;
        }

        /**
         * 追加一个选项（标识 = 展示文本）。
         *
         * @param option 选项
         * @return 本构建器
         */
        public Builder option(String option) {
            this.options.put(option, option);
            return this;
        }

        /**
         * 追加一个选项（标识 + 展示文本）。
         *
         * @param key   选项标识
         * @param value 展示文本
         * @return 本构建器
         */
        public Builder option(String key, String value) {
            this.options.put(key, value);
            return this;
        }

        /**
         * 构建问题。
         *
         * @return 问题对象
         */
        public ChoiceQuestion build() {
            return new ChoiceQuestion(this);
        }
    }
}
