package com.xiongx9527.fw.plus.ai.interactive;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 一次收集推进的结果。
 * INCOMPLETE 还需要反问（带 ClarifyFrame）；COMPLETE 必填已齐，answers 可交业务方提交；
 * DEGRADED 已达最大反问轮次仍未齐，调用方应套默认值并告知用户（missing 为缺的项）。
 */
public final class CollectionResult {

    public enum Status {
        INCOMPLETE, COMPLETE, DEGRADED
    }

    private final Status status;
    private final ClarifyFrame clarifyFrame;
    private final Map<String, Object> answers;
    private final List<Question> missing;

    CollectionResult(Status status, ClarifyFrame clarifyFrame,
                     Map<String, Object> answers, List<Question> missing) {
        this.status = status;
        this.clarifyFrame = clarifyFrame;
        this.answers = answers == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(answers));
        this.missing = missing == null ? Collections.emptyList()
                : Collections.unmodifiableList(missing);
    }

    public static CollectionResult of(Status status, ClarifyFrame frame,
                                      Map<String, Object> answers, List<Question> missing) {
        return new CollectionResult(status, frame, answers, missing);
    }

    public Status getStatus() {
        return status;
    }

    public ClarifyFrame getClarifyFrame() {
        return clarifyFrame;
    }

    public Map<String, Object> getAnswers() {
        return answers;
    }

    public List<Question> getMissing() {
        return missing;
    }
}
