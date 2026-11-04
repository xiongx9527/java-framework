package com.xiongx9527.fw.plus.ai.interactive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 一张问卷 / 一组待收集的参数（智能问卷、预约、参数收集通用）。
 *
 * <p>收集完成后业务方拿到的就是 {@code answers}：题干 {@code name} → 对应答案。
 * 业务方可据此调用自己的接口 / 工具提交答案。</p>
 */
public final class Form {

    private final String id;
    private final String titleKey;
    private final List<Question> questions;

    private Form(Builder builder) {
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

        public Form build() {
            if (id == null) {
                throw new IllegalStateException("Form.id is required");
            }
            return new Form(this);
        }
    }
}
