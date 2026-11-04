package com.xiongx9527.fw.plus.ai.interactive.questionnaire;

import com.xiongx9527.fw.plus.ai.interactive.Question;

/**
 * 答卷视图：一道题 + 用户已填的答案（未答为 null）。
 * 用于 {@link QuestionnaireInstance} 的上一题 / 当前题 / 下一题返回，前端据此渲染并回填答案。
 */
public final class AnsweredQuestion {

    private final Question question;
    private final Object answer;

    AnsweredQuestion(Question question, Object answer) {
        this.question = question;
        this.answer = answer;
    }

    public Question getQuestion() {
        return question;
    }

    public Object getAnswer() {
        return answer;
    }

    public boolean isAnswered() {
        if (answer == null) {
            return false;
        }
        if (answer instanceof String) {
            return !((String) answer).trim().isEmpty();
        }
        if (answer instanceof java.util.Collection) {
            return !((java.util.Collection<?>) answer).isEmpty();
        }
        return true;
    }
}
