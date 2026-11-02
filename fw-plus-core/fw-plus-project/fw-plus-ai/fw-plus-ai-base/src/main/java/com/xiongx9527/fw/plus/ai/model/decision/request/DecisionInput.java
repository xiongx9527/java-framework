package com.xiongx9527.fw.plus.ai.model.decision.request;

/**
 * 决策输入（标记接口）。
 *
 * <p><b>作用</b>：统一 {@link TextInput} / {@link FieldsInput} / {@link ContentsInput} /
 * {@link MessagesInput} 四种输入形态，供 {@link DecisionRequest} 持有。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.decision.request.DecisionInput}。</p>
 *
 * @author xiongxiao
 */
public interface DecisionInput {

    /**
     * 输入类型。
     *
     * @return 输入类型，非 {@code null}
     */
    DecisionInputType type();
}
