package com.xiongx9527.fw.plus.ai.interactive.tool;

import com.xiongx9527.fw.plus.ai.interactive.questionnaire.AnsweredQuestion;

/**
 * 问卷实例会话：携带实例 id 与首题（含用户答案），供后续导航引用。
 */
public final class QuestionnaireSession {

    private final String instanceId;
    private final AnsweredQuestion current;

    private QuestionnaireSession(Builder builder) {
        this.instanceId = builder.instanceId;
        this.current = builder.current;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getInstanceId() {
        return instanceId;
    }

    public AnsweredQuestion getCurrent() {
        return current;
    }

    public static final class Builder {

        private String instanceId;
        private AnsweredQuestion current;

        public Builder instanceId(String value) {
            this.instanceId = value;
            return this;
        }

        public Builder current(AnsweredQuestion value) {
            this.current = value;
            return this;
        }

        public QuestionnaireSession build() {
            return new QuestionnaireSession(this);
        }
    }
}
