package com.xiongx9527.fw.plus.ai.model.decision.response;

/**
 * 决策答案（标记接口）。
 *
 * <p><b>作用</b>：统一 {@link YesNoAnswer} / {@link ChoiceAnswer} / {@link ScaleAnswer} /
 * {@link RefusalAnswer} 四种答案形态，供 {@link DecisionResponse} 按问题标识持有。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.decision.response.DecisionAnswer}。</p>
 *
 * @author xiongxiao
 */
public interface DecisionAnswer {
}
