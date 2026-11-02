package com.xiongx9527.fw.plus.ai.model.decision.request;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 量表型问题。
 *
 * <p><b>作用</b>：让模型在一段有序刻度上给出结论（如 1~5 分、低/中/高），
 * 适用于「风险等级 / 满意度 / 优先级」等连续型决策。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.decision.request.ScaleQuestion}。</p>
 *
 * <p><b>实现约定</b>：不可变；{@link #levels()} 与 {@link #levelDescriptions()} 一一对应。</p>
 *
 * @author xiongxiao
 */
public final class ScaleQuestion implements Question {

    private final String text;
    private final List<String> levels;
    private final List<String> levelDescriptions;

    private ScaleQuestion(Builder builder) {
        if (builder.text == null || builder.text.trim().isEmpty()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        this.text = builder.text;
        this.levels = Collections.unmodifiableList(new java.util.ArrayList<String>(builder.levels));
        this.levelDescriptions = Collections.unmodifiableList(
                new java.util.ArrayList<String>(builder.levelDescriptions));
    }

    @Override
    public String text() {
        return text;
    }

    /**
     * 刻度级别（顺序即分值顺序）。
     *
     * @return 只读列表，非 {@code null}
     */
    public List<String> levels() {
        return levels;
    }

    /**
     * 各级别的文字描述（与 {@link #levels()} 一一对应）。
     *
     * @return 只读列表，非 {@code null}
     */
    public List<String> levelDescriptions() {
        return levelDescriptions;
    }

    /**
     * 由文本与级别列表创建（无描述）。
     *
     * @param text   问题文本
     * @param levels 级别列表
     * @return 问题对象
     */
    public static ScaleQuestion from(String text, List<String> levels) {
        return builder().text(text).levels(levels).build();
    }

    /**
     * 同 {@link #from(String, List)}。
     *
     * @param text   问题文本
     * @param levels 级别列表
     * @return 问题对象
     */
    public static ScaleQuestion of(String text, List<String> levels) {
        return from(text, levels);
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
        if (!(o instanceof ScaleQuestion)) {
            return false;
        }
        ScaleQuestion that = (ScaleQuestion) o;
        return Objects.equals(text, that.text)
                && Objects.equals(levels, that.levels)
                && Objects.equals(levelDescriptions, that.levelDescriptions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, levels, levelDescriptions);
    }

    @Override
    public String toString() {
        return "ScaleQuestion{text='" + text + "', levels=" + levels + "}";
    }

    /** {@link ScaleQuestion} 构建器。 */
    public static class Builder {

        private String text;
        private final List<String> levels = new java.util.ArrayList<String>();
        private final List<String> levelDescriptions = new java.util.ArrayList<String>();

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
         * 设置级别列表（同时清空描述）。
         *
         * @param levels 级别列表
         * @return 本构建器
         */
        public Builder levels(List<String> levels) {
            this.levels.clear();
            if (levels != null) {
                this.levels.addAll(levels);
            }
            this.levelDescriptions.clear();
            return this;
        }

        /**
         * 追加一个级别（无描述）。
         *
         * @param level 级别
         * @return 本构建器
         */
        public Builder level(String level) {
            this.levels.add(level);
            this.levelDescriptions.add(null);
            return this;
        }

        /**
         * 追加一个级别及其描述。
         *
         * @param level       级别
         * @param description 描述
         * @return 本构建器
         */
        public Builder level(String level, String description) {
            this.levels.add(level);
            this.levelDescriptions.add(description);
            return this;
        }

        /**
         * 构建问题。
         *
         * @return 问题对象
         */
        public ScaleQuestion build() {
            return new ScaleQuestion(this);
        }
    }
}
