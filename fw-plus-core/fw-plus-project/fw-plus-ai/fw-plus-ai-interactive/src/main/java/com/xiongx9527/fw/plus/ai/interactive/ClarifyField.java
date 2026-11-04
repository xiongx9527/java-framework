package com.xiongx9527.fw.plus.ai.interactive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 反问帧里的一个字段 / 槽位（一次反问最多 3 个，见 §6.32.5）。
 *
 * <p>复用 {@link Question} 的形状，额外带 {@code multi} / {@code freeText} / {@code validUntil}
 * 等前端渲染与校验所需的提示信息。</p>
 */
public final class ClarifyField {

    private final String name;
    private final Question.Kind kind;
    private final String titleKey;
    private final List<ClarifyOption> options;
    private final boolean freeText;
    private final boolean multi;
    private final boolean required;
    private final String pattern;
    private final Long min;
    private final Long max;
    private final String example;
    private final long validUntil;

    private ClarifyField(Builder builder) {
        this.name = builder.name;
        this.kind = builder.kind;
        this.titleKey = builder.titleKey;
        this.options = builder.options == null ? Collections.<ClarifyOption>emptyList()
                : Collections.unmodifiableList(new ArrayList<ClarifyOption>(builder.options));
        this.freeText = builder.freeText;
        this.multi = builder.multi;
        this.required = builder.required;
        this.pattern = builder.pattern;
        this.min = builder.min;
        this.max = builder.max;
        this.example = builder.example;
        this.validUntil = builder.validUntil;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder fromQuestion(Question q) {
        Builder b = new Builder();
        b.name(q.getName());
        b.kind(q.getKind());
        b.titleKey(q.getTitleKey());
        b.options(q.getOptions());
        b.freeText(q.isFreeText());
        b.multi(q.isMulti());
        b.required(q.isRequired());
        b.pattern(q.getPattern());
        b.min(q.getMin());
        b.max(q.getMax());
        b.example(q.getExample());
        return b;
    }

    public String getName() {
        return name;
    }

    public Question.Kind getKind() {
        return kind;
    }

    public String getTitleKey() {
        return titleKey;
    }

    public List<ClarifyOption> getOptions() {
        return options;
    }

    public boolean isFreeText() {
        return freeText;
    }

    public boolean isMulti() {
        return multi;
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

    public long getValidUntil() {
        return validUntil;
    }

    public static final class Builder {

        private String name;
        private Question.Kind kind;
        private String titleKey;
        private List<ClarifyOption> options;
        private boolean freeText;
        private boolean multi;
        private boolean required;
        private String pattern;
        private Long min;
        private Long max;
        private String example;
        private long validUntil;

        public Builder name(String value) {
            this.name = value;
            return this;
        }

        public Builder kind(Question.Kind value) {
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

        public Builder freeText(boolean value) {
            this.freeText = value;
            return this;
        }

        public Builder multi(boolean value) {
            this.multi = value;
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

        public Builder validUntil(long value) {
            this.validUntil = value;
            return this;
        }

        public ClarifyField build() {
            if (name == null) {
                throw new IllegalStateException("ClarifyField.name is required");
            }
            return new ClarifyField(this);
        }
    }
}
