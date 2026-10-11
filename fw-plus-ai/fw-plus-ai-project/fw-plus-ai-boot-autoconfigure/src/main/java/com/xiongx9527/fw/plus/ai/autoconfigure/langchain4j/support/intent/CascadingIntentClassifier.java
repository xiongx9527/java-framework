package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 级联分类器：按顺序依次尝试各层级，首个「命中且置信度达标」的结果即短路返回。
 *
 * <p>这是本组件的默认装配形态（规则 → 向量 → 大模型）：贵的层级只在便宜层级不确定时才触发。
 * 单个层级抛错（如模型调用失败）只记日志并跳过，不影响后续层级；全部未命中返回
 * {@link IntentResult#none(IntentSource#FALLBACK)}。</p>
 *
 * <p>{@code minConfidence} 是级联层的额外安全网：层级自身可返回命中结果，但若置信度低于该值
 * 仍会被继续向下尝试。</p>
 *
 * @param <I> 使用方自定义的意图枚举类型
 */
public final class CascadingIntentClassifier<I extends Enum<I>> implements IntentClassifier<I> {

	private static final Logger logger = LoggerFactory.getLogger(CascadingIntentClassifier.class);

	private final List<IntentClassifier<I>> delegates;

	private final double minConfidence;

	public CascadingIntentClassifier(List<IntentClassifier<I>> delegates, double minConfidence) {
		this.delegates = delegates == null
				? Collections.<IntentClassifier<I>>emptyList()
				: new ArrayList<IntentClassifier<I>>(delegates);
		this.minConfidence = minConfidence;
	}

	@Override
	public IntentResult<I> classify(IntentContext context) {
		for (IntentClassifier<I> delegate : delegates) {
			IntentResult<I> result;
			try {
				result = delegate.classify(context);
			} catch (RuntimeException e) {
				logger.warn("意图级联：{} 判定失败，跳过", delegate.getClass().getName(), e);
				continue;
			}
			if (result != null && result.matched() && result.confidence() >= minConfidence) {
				return result;
			}
		}
		return IntentResult.none(IntentSource.FALLBACK);
	}

	public List<IntentClassifier<I>> delegates() {
		return Collections.unmodifiableList(delegates);
	}

	public double minConfidence() {
		return minConfidence;
	}
}
