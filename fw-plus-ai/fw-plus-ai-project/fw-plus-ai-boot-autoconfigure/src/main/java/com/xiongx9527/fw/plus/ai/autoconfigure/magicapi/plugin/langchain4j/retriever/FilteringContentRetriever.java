package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 在另一个 {@link ContentRetriever} 之上做结果过滤的装饰器。
 *
 * <p>langchain4j 0.35.0 未随包提供该类，故在本插件内按同名语义实现：</p>
 * <ul>
 *   <li><b>score 阈值</b>——metadata 中 {@code score} 低于 {@code minScore} 的条目丢弃
 *       （向量库未命中时会把低分条目一起返回，不筛就会把无关资料喂给模型）；</li>
 *   <li><b>metadata 过滤</b>——{@code filters} 里的每个键都要在条目 metadata 中命中：
 *       {@code tags} / {@code keywords} 这类多值字段按「包含」匹配，其余按「相等」匹配；</li>
 *   <li><b>title 渲染</b>——metadata 里有 {@code title} 时，把标题以 {@code 《title》} 形式
 *       渲染到文本头部，便于模型识别资料出处。</li>
 * </ul>
 *
 * <p>配置了 magic-api 过滤函数（{@link MagicApiContentFilter}）时，上面两项内置规则不再生效——
 * 保留哪些条目完全由脚本决定，便于处理内置规则表达不了的场景。</p>
 */
public class FilteringContentRetriever implements ContentRetriever {

	private static final String KEY_SCORE = "score";

	private static final String KEY_TITLE = "title";

	/** 多值字段：按「包含」而非「相等」匹配 */
	private static final String MULTI_VALUE_TAGS = "tags";

	private static final String MULTI_VALUE_KEYWORDS = "keywords";

	private final ContentRetriever delegate;

	private final double minScore;

	private final Map<String, String> filters;

	/** 函数过滤；非空时由脚本决定保留哪些条目，内置的 score/metadata 规则不再生效 */
	private final MagicApiContentFilter functionFilter;

	public FilteringContentRetriever(ContentRetriever delegate, double minScore, Map<String, String> filters) {
		this(delegate, minScore, filters, null);
	}

	public FilteringContentRetriever(ContentRetriever delegate, double minScore, Map<String, String> filters,
			MagicApiContentFilter functionFilter) {
		this.delegate = delegate;
		this.minScore = minScore;
		this.filters = filters == null ? Collections.<String, String>emptyMap() : filters;
		this.functionFilter = functionFilter;
	}

	@Override
	public List<Content> retrieve(Query query) {
		List<Content> retrieved = delegate.retrieve(query);
		if (retrieved == null || retrieved.isEmpty()) {
			return Collections.emptyList();
		}
		if (functionFilter != null) {
			return functionFilter.filter(retrieved, query);
		}
		List<Content> kept = new ArrayList<>();
		for (Content content : retrieved) {
			if (scoreOf(content) < minScore) {
				continue;
			}
			if (!matchesFilters(content)) {
				continue;
			}
			kept.add(renderTitle(content));
		}
		return kept;
	}

	/** metadata 里有 title 时，把标题以《title》形式渲染到文本头部，便于模型识别资料出处。 */
	private static Content renderTitle(Content content) {
		Metadata metadata = content.textSegment().metadata();
		String title = metadata.getString(KEY_TITLE);
		if (title == null || title.trim().isEmpty()
				|| content.textSegment().text().startsWith("《" + title + "》")) {
			return content;
		}
		return Content.from(TextSegment.from("《" + title + "》" + content.textSegment().text(), metadata));
	}

	/** filters 全部命中才保留；任一不满足即淘汰。 */
	private boolean matchesFilters(Content content) {
		Metadata metadata = content.textSegment().metadata();
		for (Map.Entry<String, String> entry : filters.entrySet()) {
			String actual = metadata.getString(entry.getKey());
			if (actual == null) {
				return false;
			}
			if (MULTI_VALUE_TAGS.equals(entry.getKey()) || MULTI_VALUE_KEYWORDS.equals(entry.getKey())) {
				if (!containsToken(actual, entry.getValue())) {
					return false;
				}
			} else if (!entry.getValue().equals(actual)) {
				return false;
			}
		}
		return true;
	}

	/** 逗号 / 分号分隔的多值字段里是否包含指定词。 */
	private static boolean containsToken(String actual, String expected) {
		for (String token : actual.split("[,;]")) {
			if (token.trim().equals(expected.trim())) {
				return true;
			}
		}
		return false;
	}

	/** 取 metadata 里的 score；缺失或解析失败按 1.0（不因元数据缺失丢内容）。 */
	private static double scoreOf(Content content) {
		dev.langchain4j.data.document.Metadata metadata = content.textSegment().metadata();
		if (!metadata.containsKey(KEY_SCORE)) {
			return 1.0;
		}
		// langchain4j 1.x EmbeddingStoreContentRetriever 写入的 score 是 Double，历史数据可能为 String；
		// 1.x Metadata 强类型 getter 读到不匹配类型会抛异常，故双兼容
		try {
			return metadata.getDouble(KEY_SCORE);
		} catch (RuntimeException typeMismatch) {
			// 落到字符串解析
		}
		try {
			return Double.parseDouble(metadata.getString(KEY_SCORE).trim());
		} catch (RuntimeException e) {
			return 1.0;
		}
	}
}
