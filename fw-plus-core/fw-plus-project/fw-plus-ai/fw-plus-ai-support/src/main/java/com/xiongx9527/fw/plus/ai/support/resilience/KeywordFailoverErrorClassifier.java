package com.xiongx9527.fw.plus.ai.support.resilience;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 基于关键词 / 错误码的配置式故障转移分类器（框架提供的可配置默认实现）。
 *
 * <p>具体「错误 → 动作」映射由调用方决定——产品侧从配置项（如
 * {@code ai.fallback.error-policies}）加载，例如：</p>
 * <pre>
 *   rate_limit,429,503,timeout      -> RETRY     (瞬态，同一候选重试)
 *   insufficient,balance,401,403    -> FAILOVER  (换一个模型可能可用)
 *   bad_request,400                 -> ABORT     (调用方自身错误)
 * </pre>
 *
 * <p>框架不内置任何业务错误清单，只提供这个匹配机制。未命中任何关键词时按
 * {@link FailoverAction#FAILOVER} 处理（中性默认，兜底切下一候选）。</p>
 */
public final class KeywordFailoverErrorClassifier implements FailoverErrorClassifier {

	private final Map<String, FailoverAction> policies;

	private KeywordFailoverErrorClassifier(Map<String, FailoverAction> policies) {
		this.policies = policies;
	}

	/**
	 * 用「关键词 → 动作」映射构造。
	 *
	 * @param entries 形如 {@code "rate_limit:RETRY", "insufficient:FAILOVER", "400:ABORT"} 的条目集合
	 */
	public static KeywordFailoverErrorClassifier of(Collection<String> entries) {
		Map<String, FailoverAction> normalized = new LinkedHashMap<>();
		if (entries != null) {
			for (String e : entries) {
				if (e == null) {
					continue;
				}
				String trimmed = e.trim();
				if (trimmed.isEmpty()) {
					continue;
				}
				int sep = trimmed.lastIndexOf(':');
				if (sep <= 0 || sep >= trimmed.length() - 1) {
					// 无动作后缀：默认当作 FAILOVER
					normalized.put(trimmed.toLowerCase(Locale.ROOT), FailoverAction.FAILOVER);
					continue;
				}
				String keyword = trimmed.substring(0, sep).trim().toLowerCase(Locale.ROOT);
				FailoverAction action = parseAction(trimmed.substring(sep + 1).trim());
				if (!keyword.isEmpty() && action != null) {
					normalized.put(keyword, action);
				}
			}
		}
		return new KeywordFailoverErrorClassifier(normalized);
	}

	/** 便捷：逗号分隔的条目串（产品侧直接喂配置项）。 */
	public static KeywordFailoverErrorClassifier ofCsv(String csv) {
		if (csv == null || csv.trim().isEmpty()) {
			return new KeywordFailoverErrorClassifier(Collections.<String, FailoverAction>emptyMap());
		}
		return of(Arrays.asList(csv.split(",")));
	}

	@Override
	public FailoverAction classify(String errorMessage) {
		if (policies.isEmpty()) {
			return FailoverAction.FAILOVER;
		}
		if (errorMessage == null) {
			return FailoverAction.FAILOVER;
		}
		String lower = errorMessage.toLowerCase(Locale.ROOT);
		// 命中任一关键词即采用其动作（首个命中即可）
		for (Map.Entry<String, FailoverAction> entry : policies.entrySet()) {
			if (lower.contains(entry.getKey())) {
				return entry.getValue();
			}
		}
		return FailoverAction.FAILOVER;
	}

	/** 当前配置的「关键词 → 动作」映射（不可变）。 */
	public Map<String, FailoverAction> policies() {
		return Collections.unmodifiableMap(policies);
	}

	private static FailoverAction parseAction(String text) {
		if (text == null) {
			return null;
		}
		switch (text.toUpperCase(Locale.ROOT)) {
			case "RETRY":
				return FailoverAction.RETRY;
			case "FAILOVER":
				return FailoverAction.FAILOVER;
			case "ABORT":
				return FailoverAction.ABORT;
			default:
				return null;
		}
	}
}
