package com.xiongx9527.fw.plus.ai.interactive.questionnaire;

import com.xiongx9527.fw.plus.ai.interactive.AnswerExtractor;
import com.xiongx9527.fw.plus.ai.interactive.DefaultAnswerExtractor;
import com.xiongx9527.fw.plus.ai.interactive.ExtractionResult;
import com.xiongx9527.fw.plus.ai.interactive.InteractiveException;
import com.xiongx9527.fw.plus.ai.interactive.Question;
import com.xiongx9527.fw.plus.ai.interactive.QuestionValidator;
import com.xiongx9527.fw.plus.ai.interactive.ValidationResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 问卷实例（通过卷 id 开启后的答题会话）。
 *
 * <p>提供顺序答题导航：{@link #current()} 当前题、{@link #previous()} 上一题、
 * {@link #next()} 下一题，三者均返回带用户答案的 {@link AnsweredQuestion}；
 * {@link #submitAnswer(Object)} / {@link #submitAnswer(String, AnswerExtractor)} 提交当前题答案
 * （含抽取与校验）；{@link #finish()} 交卷，返回 题干名 → 答案 的全量映射交业务方提交。</p>
 */
public final class QuestionnaireInstance {

    private final Paper paper;
    private final java.util.List<Question> questions;
    private final Map<String, Object> answers = new LinkedHashMap<String, Object>();
    private final QuestionValidator validator = new QuestionValidator();
    private int index;

    private QuestionnaireInstance(Paper paper) {
        this.paper = paper;
        this.questions = paper.getQuestions();
    }

    public static QuestionnaireInstance open(Paper paper) {
        if (paper == null) {
            throw new IllegalArgumentException("paper is required");
        }
        return new QuestionnaireInstance(paper);
    }

    public String getPaperId() {
        return paper.getId();
    }

    public int getIndex() {
        return index;
    }

    public int size() {
        return questions.size();
    }

    public boolean isFirst() {
        return index <= 0;
    }

    public boolean isLast() {
        return questions.isEmpty() || index >= questions.size() - 1;
    }

    /** 当前题（含用户答案）；无题返回 null。 */
    public AnsweredQuestion current() {
        if (questions.isEmpty()) {
            return null;
        }
        return view(questions.get(index));
    }

    /** 上一题（含用户答案）；已是第一题返回 null（不移动）。 */
    public AnsweredQuestion previous() {
        if (index <= 0) {
            return null;
        }
        index--;
        return view(questions.get(index));
    }

    /** 下一题（含用户答案）；已是最后一题返回 null（不移动）。 */
    public AnsweredQuestion next() {
        if (questions.isEmpty() || index >= questions.size() - 1) {
            return null;
        }
        index++;
        return view(questions.get(index));
    }

    /** 直接提交当前题答案（校验通过后写入）。 */
    public void submitAnswer(Object value) {
        Question q = questions.get(index);
        ValidationResult vr = validator.validate(q, value);
        if (!vr.isValid()) {
            throw new InteractiveException("invalid answer for " + q.getName() + ": " + vr.getKey());
        }
        answers.put(q.getName(), value);
    }

    /** 从用户一句话抽取并提交当前题答案；抽取不确定则抛异常，由调用方继续追问。 */
    public void submitAnswer(String utterance, AnswerExtractor extractor) {
        Question q = questions.get(index);
        AnswerExtractor ex = extractor == null ? new DefaultAnswerExtractor() : extractor;
        ExtractionResult er = ex.extract(q, utterance);
        if (!er.isCertain()) {
            throw new InteractiveException("cannot extract answer for " + q.getName());
        }
        submitAnswer(er.getValue());
    }

    /** 所有必答题是否已作答。 */
    public boolean isComplete() {
        for (Question q : questions) {
            if (q.isRequired() && isBlank(answers.get(q.getName()))) {
                return false;
            }
        }
        return true;
    }

    /** 交卷：返回全量 题干名 → 答案（业务据此提交）。未完成也可交，是否允许由业务决定。 */
    public Map<String, Object> finish() {
        return new LinkedHashMap<String, Object>(answers);
    }

    private AnsweredQuestion view(Question q) {
        return new AnsweredQuestion(q, answers.get(q.getName()));
    }

    private static boolean isBlank(Object v) {
        if (v == null) {
            return true;
        }
        if (v instanceof String) {
            return ((String) v).trim().isEmpty();
        }
        if (v instanceof java.util.Collection) {
            return ((java.util.Collection<?>) v).isEmpty();
        }
        return false;
    }
}
