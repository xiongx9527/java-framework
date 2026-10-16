package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent;

/**
 * 意图来源：只暴露「当前文本对应的意图」，供 RAG / 工具路由消费。
 *
 * <p>把「怎么识别」与「识别结果给谁用」解耦：RAG 检索器与工具提供者只依赖本接口
 * （{@code byIntent(classifier)} 走现分类；{@code bySupplier(...)} 复用调用方已判定的意图，
 * 避免一次问答重复识别）。返回 null 表示未匹配。</p>
 *
 * @param <I> 使用方自定义的意图枚举类型
 */
@FunctionalInterface
public interface IntentSupplier<I extends Enum<I>> {

	I intent(IntentContext context);

	/** 适配一个分类器：每次调用都现跑一遍分类。 */
	static <I extends Enum<I>> IntentSupplier<I> of(final IntentClassifier<I> classifier) {
		if (classifier == null) {
			throw new IllegalArgumentException("classifier 不能为 null");
		}
		return new IntentSupplier<I>() {
			@Override
			public I intent(IntentContext context) {
				return classifier.classify(context).intent();
			}
		};
	}
}
