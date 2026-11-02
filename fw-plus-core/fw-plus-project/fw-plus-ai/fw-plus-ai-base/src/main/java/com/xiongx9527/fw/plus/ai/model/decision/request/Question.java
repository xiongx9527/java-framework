package com.xiongx9527.fw.plus.ai.model.decision.request;

/**
 * 决策问题。
 *
 * <p><b>作用</b>：统一的「问题」抽象，被 {@link YesNoQuestion} / {@link ChoiceQuestion} /
 * {@link ScaleQuestion} 三种具体形态实现，使决策请求能混合提问类型。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.decision.request.Question}。</p>
 *
 * @author xiongxiao
 */
public interface Question {

    /**
     * 问题文本。
     *
     * @return 问题文本，非 {@code null}
     */
    String text();
}
