package com.xiongx9527.fw.plus.ai.interactive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 一道题 / 一个槽位（问卷、预约、参数收集的通用单元）。
 *
 * <p>题型 {@link Kind}：</p>
 * <ul>
 *   <li>{@code TEXT}：填空（自由输入，可带格式 / 范围校验）；</li>
 *   <li>{@code SINGLE_CHOICE}：单选 / 选择题（选项来自目录或静态枚举，提交用 id）；</li>
 *   <li>{@code MULTI_CHOICE}：多选。</li>
 * </ul>
 *
 * <p>文案走 {@code titleKey}（i18n key），不由模型生成；选项必须可校验（见 {@link ClarifyOption}）。</p>
 */
public final class Question {

    /** 题型：填空 / 单选（含选择题）/ 多选。 */
    public enum Kind {
        TEXT, SINGLE_CHOICE, MULTI_CHOICE
    }

    private final String name;
    private final Kind kind;
    private final String titleKey;
    private final List<ClarifyOption> options;
    private final boolean required;
    private final String pattern;
    private final Long min;
    private final Long max;
    private final String example;

    private Question(Builder builder) {
        this.name = builder.name;
        this.kind = builder.kind == null ? Kind.TEXT : builder.kind;
        this.titleKey = builder.titleKey;
        this.options = builder.options == null ? Collections.<ClarifyOption>emptyList()
                : Collections.unmodifiableList(new ArrayList<ClarifyOption>(builder.options));
        this.required = builder.required;
        this.pattern = builder.pattern;
        this.min = builder.min;
        this.max = builder.max;
        this.example = builder.example;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    public Kind getKind() {
        return kind;
    }

    public String getTitleKey() {
        return titleKey;
    }

    public List<ClarifyOption> getOptions() {
        return options;
    }

    public boolean isRequired() {
        return required;
    }

    public String getPattern() {
        return pattern;
    }

    public Long getMin() {
        return min;
    }

    public Long getMax() {
        return max;
    }

    public String getExample() {
        return example;
    }

    public boolean isMulti() {
        return kind == Kind.MULTI_CHOICE;
    }

    public boolean isFreeText() {
        return kind == Kind.TEXT;
    }

    public static final class Builder {

        private String name;
        private Kind kind;
        private String titleKey;
        private List<ClarifyOption> options;
        private boolean required;
        private String pattern;
        private Long min;
        private Long max;
        private String example;

        public Builder name(String value) {
            this.name = value;
            return this;
        }

        public Builder kind(Kind value) {
            this.kind = value;
            return this;
        }

        public Builder titleKey(String value) {
            this.titleKey = value;
            return this;
        }

        public Builder options(List<ClarifyOption> value) {
            this.options = value;
            return this;
        }

        public Builder required(boolean value) {
            this.required = value;
            return this;
        }

        public Builder pattern(String value) {
            this.pattern = value;
            return this;
        }

        public Builder min(Long value) {
            this.min = value;
            return this;
        }

        public Builder max(Long value) {
            this.max = value;
            return this;
        }

        public Builder example(String value) {
            this.example = value;
            return this;
        }

        public Question build() {
            if (name == null) {
                throw new IllegalStateException("Question.name is required");
            }
            return new Question(this);
        }
    }
}
