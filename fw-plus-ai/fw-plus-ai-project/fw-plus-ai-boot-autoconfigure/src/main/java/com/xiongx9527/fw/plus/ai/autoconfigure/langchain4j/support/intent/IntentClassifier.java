package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent;

/**
 * 意图分类器：把一段文本判定为某个意图枚举。
 *
 * <p>本组件所有判定层级（规则 / 向量 / 大模型 / 级联）都实现本接口，因此可自由替换、叠加与组合。
 * 未匹配时务必返回 {@link IntentResult#none(IntentSource)} 而非抛异常，以便级联继续尝试下一级。</p>
 *
 * @param <I> 使用方自定义的意图枚举类型
 */
@FunctionalInterface
public interface IntentClassifier<I extends Enum<I>> {

	IntentResult<I> classify(IntentContext context);
}
