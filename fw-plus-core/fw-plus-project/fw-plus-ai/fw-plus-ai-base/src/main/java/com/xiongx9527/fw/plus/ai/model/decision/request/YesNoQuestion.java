package com.xiongx9527.fw.plus.ai.model.decision.request;

import java.util.Objects;

/**
 * 是否型问题。
 *
 * <p><b>作用</b>：最简单的一类决策问题，固定答案为「是 / 否」，并可附上判定的原文说明。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.decision.request.YesNoQuestion}。</p>
 *
 * <p><b>实现约定</b>：不可变；{@link #text()} 非空，{@link #yesWhen()} / {@link #noWhen()} 可为 {@code null}。</p>
 *
 * @author xiongxiao
 */
public final class YesNoQuestion implements Question {

    private final String text;
    private final String yesWhen;
    private final String noWhen;

    private YesNoQuestion(Builder builder) {
        if (builder.text == null || builder.text.trim().isEmpty()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        this.text = builder.text;
        this.yesWhen = builder.yesWhen;
        this.noWhen = builder.noWhen;
    }

    @Override
    public String text() {
        return text;
    }

    /**
     * 「是」的判定说明。
     *
     * @return 说明；未提供时为 {@code null}
     */
    public String yesWhen() {
        return yesWhen;
    }

    /**
     * 「否」的判定说明。
     *
     * @return 说明；未提供时为 {@code null}
     */
    public String noWhen() {
        return noWhen;
    }

    /**
     * 由文本创建（无判定说明）。
     *
     * @param text 问题文本
     * @return 问题对象
     */
    public static YesNoQuestion from(String text) {
        return builder().text(text).build();
    }

    /**
     * 由文本创建（无判定说明）。
     *
     * @param text 问题文本
     * @return 问题对象
     */
    public static YesNoQuestion of(String text) {
        return from(text);
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
        if (!(o instanceof YesNoQuestion)) {
            return false;
        }
        YesNoQuestion that = (YesNoQuestion) o;
        return Objects.equals(text, that.text)
                && Objects.equals(yesWhen, that.yesWhen)
                && Objects.equals(noWhen, that.noWhen);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, yesWhen, noWhen);
    }

    @Override
    public String toString() {
        return "YesNoQuestion{text='" + text + "', yesWhen='" + yesWhen + "', noWhen='" + noWhen + "'}";
    }

    /** {@link YesNoQuestion} 构建器。 */
    public static class Builder {

        private String text;
        private String yesWhen;
        private String noWhen;

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
         * 设置「是」的判定说明。
         *
         * @param yesWhen 说明
         * @return 本构建器
         */
        public Builder yesWhen(String yesWhen) {
            this.yesWhen = yesWhen;
            return this;
        }

        /**
         * 设置「否」的判定说明。
         *
         * @param noWhen 说明
         * @return 本构建器
         */
        public Builder noWhen(String noWhen) {
            this.noWhen = noWhen;
            return this;
        }

        /**
         * 构建问题。
         *
         * @return 问题对象
         */
        public YesNoQuestion build() {
            return new YesNoQuestion(this);
        }
    }
}
