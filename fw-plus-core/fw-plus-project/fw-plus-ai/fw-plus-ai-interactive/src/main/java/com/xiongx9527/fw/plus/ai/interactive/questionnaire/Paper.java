package com.xiongx9527.fw.plus.ai.interactive.questionnaire;

import com.xiongx9527.fw.plus.ai.interactive.Question;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 卷子（一份智能问卷的定义）：卷 id + 标题 + 有序的题（{@link Question}）。
 *
 * <p>题目支持填空 / 单选 / 多选（见 {@link Question.Kind}），可带格式 / 范围校验与可校验选项。
 * 文案走 {@code titleKey}（i18n），不由模型生成。业务通过卷 id 开启 {@link QuestionnaireInstance}。</p>
 */
public final class Paper {

    private final String id;
    private final String titleKey;
    private final List<Question> questions;

    private Paper(Builder builder) {
        this.id = builder.id;
        this.titleKey = builder.titleKey;
        this.questions = builder.questions == null ? Collections.<Question>emptyList()
                : Collections.unmodifiableList(new ArrayList<Question>(builder.questions));
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public String getTitleKey() {
        return titleKey;
    }

    /** 有序题目。 */
    public List<Question> getQuestions() {
        return questions;
    }

    public static final class Builder {

        private String id;
        private String titleKey;
        private List<Question> questions;

        public Builder id(String value) {
            this.id = value;
            return this;
        }

        public Builder titleKey(String value) {
            this.titleKey = value;
            return this;
        }

        public Builder questions(List<Question> value) {
            this.questions = value;
            return this;
        }

        public Paper build() {
            if (id == null) {
                throw new IllegalStateException("Paper.id is required");
            }
            return new Paper(this);
        }
    }
}
