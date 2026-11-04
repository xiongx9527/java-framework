package com.xiongx9527.fw.plus.ai.interactive.tool;

import com.xiongx9527.fw.plus.ai.interactive.AnswerExtractor;
import com.xiongx9527.fw.plus.ai.interactive.questionnaire.AnsweredQuestion;

import java.util.Map;

/**
 * 智能问卷工具定义（端口）：通过卷 id 开启实例，顺序答题（上 / 当前 / 下一题均含用户答案），
 * 提交答案，最后交卷。
 *
 * <p>框架只定义契约与 DTO，实现由业务完成（实现负责按卷 id 解析 {@code Paper}、
 * 管理实例存储，可组合 {@code QuestionnaireInstance} / {@code QuestionnaireService}）。
 * 各题的 {@link AnsweredQuestion} 已带用户答案，便于前端回填与渲染。</p>
 */
public interface QuestionnaireTool {

    /** 通过卷 id 开启实例，返回实例 id 与首题（含答案）。 */
    QuestionnaireSession open(String paperId);

    /** 当前题（含答案）。 */
    AnsweredQuestion current(String instanceId);

    /** 上一题（含答案）；已是第一题返回 null。 */
    AnsweredQuestion previous(String instanceId);

    /** 下一题（含答案）；已是最后一题返回 null。 */
    AnsweredQuestion next(String instanceId);

    /** 提交当前题答案（校验通过后写入）。 */
    void submit(String instanceId, Object value);

    /** 从用户一句话抽取并提交当前题答案。 */
    void submit(String instanceId, String utterance, AnswerExtractor extractor);

    /** 所有必答题是否已作答。 */
    boolean isComplete(String instanceId);

    /** 交卷：返回全量 题干名 → 答案。 */
    Map<String, Object> finish(String instanceId);
}
