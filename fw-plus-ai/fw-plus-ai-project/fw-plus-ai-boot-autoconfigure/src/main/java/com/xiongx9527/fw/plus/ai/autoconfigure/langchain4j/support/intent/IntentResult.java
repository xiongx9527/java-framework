package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 意图判定结果：意图枚举 + 置信度 + 槽位 + 判定来源。
 *
 * <p>不可变。{@link #intent()} 为 null 表示「未匹配到任何意图」（此时 {@link #source()} 为
 * {@link IntentSource#FALLBACK}），使用方据此走兜底分支（转人工 / 默认话术 / 不检索 / 全量工具等）；
 * 组件自身<b>不做</b>业务兜底映射，保持零业务耦合。</p>
 *
 * @param <I> 使用方自定义的意图枚举类型
 */
public final class IntentResult<I extends Enum<I>> {

	private final I intent;

	private final double confidence;

	private final Map<String, String> slots;

	private final IntentSource source;

	private IntentResult(I intent, double confidence, Map<String, String> slots, IntentSource source) {
		this.intent = intent;
		this.confidence = confidence;
		this.slots = slots == null || slots.isEmpty()
				? Collections.<String, String>emptyMap()
				: Collections.unmodifiableMap(new LinkedHashMap<String, String>(slots));
		this.source = source;
	}

	/**
	 * 构造一条「命中」结果。
	 *
	 * @param intent     命中的意图（不可为 null）
	 * @param confidence 置信度（0~1）
	 * @param slots      抽取到的槽位，可为 null
	 * @param source     判定来源
	 */
	public static <I extends Enum<I>> IntentResult<I> of(I intent, double confidence,
			Map<String, String> slots, IntentSource source) {
		if (intent == null) {
			throw new IllegalArgumentException("intent 不能为 null；未匹配请使用 none(source)");
		}
		return new IntentResult<I>(intent, confidence, slots, source);
	}

	/** 构造一条不带槽位的「命中」结果。 */
	public static <I extends Enum<I>> IntentResult<I> of(I intent, double confidence, IntentSource source) {
		return of(intent, confidence, null, source);
	}

	/** 构造一条「未匹配」结果（{@link #intent()} 为 null、置信度为 0）。 */
	public static <I extends Enum<I>> IntentResult<I> none(IntentSource source) {
		return new IntentResult<I>(null, 0.0d, null, source);
	}

	public I intent() {
		return intent;
	}

	/** 是否命中意图。 */
	public boolean matched() {
		return intent != null;
	}

	public double confidence() {
		return confidence;
	}

	public Map<String, String> slots() {
		return slots;
	}

	/** 取单个槽位；不存在时返回 null。 */
	public String slot(String name) {
		return name == null ? null : slots.get(name);
	}

	public IntentSource source() {
		return source;
	}

	@Override
	public String toString() {
		return "IntentResult{intent=" + intent + ", confidence=" + confidence
				+ ", slots=" + slots + ", source=" + source + '}';
	}
}
