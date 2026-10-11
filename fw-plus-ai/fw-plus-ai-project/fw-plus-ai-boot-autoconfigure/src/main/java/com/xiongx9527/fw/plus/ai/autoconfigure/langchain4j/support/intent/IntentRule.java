package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent;

import java.util.regex.Pattern;

/**
 * L0 规则：一个「正则命中即判定为某意图」的确定性规则。
 *
 * <p>用 {@link Pattern#find()} 语义（子串匹配，非整串匹配）。只应放置绝不会误判的输入
 * （如「转人工」「人工客服」），作为最稳妥的短路层。</p>
 *
 * @param <I> 使用方自定义的意图枚举类型
 */
public final class IntentRule<I extends Enum<I>> {

	private final I intent;

	private final Pattern pattern;

	private IntentRule(I intent, Pattern pattern) {
		this.intent = intent;
		this.pattern = pattern;
	}

	public static <I extends Enum<I>> IntentRule<I> of(I intent, String regex) {
		if (intent == null) {
			throw new IllegalArgumentException("intent 不能为 null");
		}
		if (regex == null || regex.trim().isEmpty()) {
			throw new IllegalArgumentException("regex 不能为空");
		}
		return new IntentRule<I>(intent, Pattern.compile(regex));
	}

	public static <I extends Enum<I>> IntentRule<I> of(I intent, Pattern pattern) {
		if (intent == null) {
			throw new IllegalArgumentException("intent 不能为 null");
		}
		if (pattern == null) {
			throw new IllegalArgumentException("pattern 不能为 null");
		}
		return new IntentRule<I>(intent, pattern);
	}

	public I intent() {
		return intent;
	}

	public Pattern pattern() {
		return pattern;
	}

	/** 文本是否命中本规则（子串匹配）。 */
	public boolean matches(String text) {
		return text != null && pattern.matcher(text).find();
	}

	@Override
	public String toString() {
		return "IntentRule{intent=" + intent + ", pattern=" + pattern + '}';
	}
}
