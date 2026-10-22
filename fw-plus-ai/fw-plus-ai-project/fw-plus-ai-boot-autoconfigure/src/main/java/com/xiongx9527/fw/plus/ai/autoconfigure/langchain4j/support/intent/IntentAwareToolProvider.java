package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent;

import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 意图化工具提供者：先从一个「全量工具来源」取全部工具，再按当前意图只保留该意图允许的工具，
 * 达到「按意图裁剪工具集」的目的（减少无关工具干扰、降低模型误调）。
 *
 * <p>裁剪规则（三态 + 白名单）：</p>
 * <ul>
 *   <li>命中意图且配置了 {@code allow(i, ...)} → 只保留列出的工具 + {@code alwaysInclude}；</li>
 *   <li>命中意图且声明了 {@code allowNone(i)} → 只保留 {@code alwaysInclude}；</li>
 *   <li>未匹配到意图（或匹配到但未配置）→ 走兜底：全量（{@code fallbackAll()}）、
 *       只留白名单（{@code fallbackNone()}）或指定集合（{@code fallback(...)}）；</li>
 *   <li>{@code alwaysInclude} 中的工具在任何分支下都叠加保留（如 {@code handoff_to_human}）。</li>
 * </ul>
 *
 * <p>全量来源获取失败、意图识别失败时均降级：来源失败输出空工具集，识别失败按「未匹配」兜底，
 * 不让裁剪逻辑拖垮整轮对话。</p>
 *
 * @param <I> 使用方自定义的意图枚举类型
 */
public final class IntentAwareToolProvider<I extends Enum<I>> implements ToolProvider {

	private static final Logger logger = LoggerFactory.getLogger(IntentAwareToolProvider.class);

	private final IntentSupplier<I> intentSupplier;

	private final ToolProvider source;

	private final Map<I, Set<String>> allowed;

	private final Set<I> allowNone;

	private final Set<String> alwaysInclude;

	private final FallbackMode fallbackMode;

	private final Set<String> fallbackTools;

	public IntentAwareToolProvider(IntentSupplier<I> intentSupplier, ToolProvider source,
			Map<I, Set<String>> allowed, Set<I> allowNone, Set<String> alwaysInclude,
			FallbackMode fallbackMode, Set<String> fallbackTools) {
		if (intentSupplier == null) {
			throw new IllegalArgumentException(
					I18nMessageResolver.resolveKey("fw.ai.IntentAwareToolProvider.intentSupplierRequired"));
		}
		this.intentSupplier = intentSupplier;
		this.source = source;
		this.allowed = allowed == null
				? Collections.<I, Set<String>>emptyMap()
				: new LinkedHashMap<I, Set<String>>(allowed);
		this.allowNone = allowNone == null
				? Collections.<I>emptySet()
				: new LinkedHashSet<I>(allowNone);
		this.alwaysInclude = alwaysInclude == null
				? Collections.<String>emptySet()
				: new LinkedHashSet<String>(alwaysInclude);
		this.fallbackMode = fallbackMode == null ? FallbackMode.NONE : fallbackMode;
		this.fallbackTools = fallbackTools == null
				? Collections.<String>emptySet()
				: new LinkedHashSet<String>(fallbackTools);
	}

	@Override
	public ToolProviderResult provideTools(ToolProviderRequest request) {
		Map<ToolSpecification, ToolExecutor> tools = provideAll(request);
		Set<String> keep = select(resolveIntent(request), names(tools.keySet()));

		Map<ToolSpecification, ToolExecutor> filtered = new LinkedHashMap<ToolSpecification, ToolExecutor>();
		for (Map.Entry<ToolSpecification, ToolExecutor> entry : tools.entrySet()) {
			if (keep.contains(entry.getKey().name())) {
				filtered.put(entry.getKey(), entry.getValue());
			}
		}
		ToolProviderResult.Builder builder = ToolProviderResult.builder();
		builder.addAll(filtered);
		return builder.build();
	}

	private Map<ToolSpecification, ToolExecutor> provideAll(ToolProviderRequest request) {
		if (source == null) {
			return Collections.emptyMap();
		}
		try {
			ToolProviderResult result = source.provideTools(request);
			if (result == null || result.tools() == null) {
				return Collections.emptyMap();
			}
			return result.tools();
		} catch (RuntimeException e) {
			logger.warn("意图化工具：全量工具来源获取失败，降级为空工具集", e);
			return Collections.emptyMap();
		}
	}

	private I resolveIntent(ToolProviderRequest request) {
		try {
			return intentSupplier.intent(IntentContext.of(extractText(request)));
		} catch (RuntimeException e) {
			logger.warn("意图化工具：意图识别失败，按未匹配兜底", e);
			return null;
		}
	}

	private Set<String> select(I intent, Set<String> available) {
		Set<String> keep = new LinkedHashSet<String>();
		if (intent != null) {
			if (allowNone.contains(intent)) {
				keep.addAll(alwaysInclude);
				return keep;
			}
			Set<String> configured = allowed.get(intent);
			if (configured != null) {
				keep.addAll(configured);
				keep.addAll(alwaysInclude);
				return keep;
			}
		}
		// 未匹配，或匹配到但未配置 → 兜底
		if (fallbackMode == FallbackMode.ALL) {
			keep.addAll(available);
		} else if (fallbackMode == FallbackMode.SET) {
			keep.addAll(fallbackTools);
		}
		keep.addAll(alwaysInclude);
		return keep;
	}

	private static String extractText(ToolProviderRequest request) {
		if (request == null || request.userMessage() == null) {
			return null;
		}
		return request.userMessage().hasSingleText() ? request.userMessage().singleText() : null;
	}

	private static Set<String> names(Set<ToolSpecification> specifications) {
		Set<String> names = new LinkedHashSet<String>();
		for (ToolSpecification specification : specifications) {
			names.add(specification.name());
		}
		return names;
	}

	/** 未匹配到意图（或未配置）时的兜底策略。 */
	public enum FallbackMode {

		/** 保留全部工具 */
		ALL,

		/** 只保留 {@code alwaysInclude} */
		NONE,

		/** 只保留 {@code fallback(...)} 指定的工具（叠加 {@code alwaysInclude}） */
		SET
	}
}
