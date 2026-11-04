package com.xiongx9527.fw.plus.ai.interactive.questionnaire;

import com.xiongx9527.fw.plus.ai.interactive.InteractiveException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 问卷服务：按卷 id 登记 {@link Paper} 并开启 {@link QuestionnaireInstance}。
 *
 * <p>业务侧登记若干卷子后，即可"通过卷 id 开启实例"，再走 上一题 / 当前题 / 下一题 / 提交答案 / 交卷。
 * 多实例并发安全由调用方保证（此处每个实例自带状态，互不干扰）。</p>
 */
public final class QuestionnaireService {

    private final Map<String, Paper> registry = new ConcurrentHashMap<String, Paper>();

    public void register(Paper paper) {
        if (paper == null || paper.getId() == null) {
            throw new IllegalArgumentException("paper with id is required");
        }
        registry.put(paper.getId(), paper);
    }

    public boolean contains(String paperId) {
        return registry.containsKey(paperId);
    }

    /** 通过卷 id 开启一个答题实例。 */
    public QuestionnaireInstance open(String paperId) {
        Paper paper = registry.get(paperId);
        if (paper == null) {
            throw new InteractiveException("unknown paper: " + paperId);
        }
        return QuestionnaireInstance.open(paper);
    }
}
