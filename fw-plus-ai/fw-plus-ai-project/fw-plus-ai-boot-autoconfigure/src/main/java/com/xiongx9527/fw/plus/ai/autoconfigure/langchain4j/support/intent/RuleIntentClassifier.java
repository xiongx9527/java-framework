package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * L0 规则分类器：按声明顺序依次尝试正则规则，首个命中即返回（置信度固定 1.0）。
 *
 * <p>确定性最强、零模型成本，放在级联最前。未命中返回 {@link IntentResult#none(IntentSource)}
 * （来源 {@link IntentSource#RULE}），交给后续层级。</p>
 *
 * @param <I> 使用方自定义的意图枚举类型
 */
public final class RuleIntentClassifier<I extends Enum<I>> implements IntentClassifier<I> {

	private final List<IntentRule<I>> rules;

	public RuleIntentClassifier(List<IntentRule<I>> rules) {
		this.rules = rules == null
				? Collections.<IntentRule<I>>emptyList()
				: new ArrayList<IntentRule<I>>(rules);
	}

	@Override
	public IntentResult<I> classify(IntentContext context) {
		String text = context == null ? null : context.text();
		if (text != null) {
			for (IntentRule<I> rule : rules) {
				if (rule.matches(text)) {
					return IntentResult.of(rule.intent(), 1.0d,
							Collections.<String, String>emptyMap(), IntentSource.RULE);
				}
			}
		}
		return IntentResult.none(IntentSource.RULE);
	}

	public List<IntentRule<I>> rules() {
		return Collections.unmodifiableList(rules);
	}
}
