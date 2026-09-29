package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever;

import dev.langchain4j.data.document.Metadata;
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
 *       {@code tags} / {@code keywords} 这类多值字段按「包含」匹配，其余按「相等」匹配。</li>
 * </ul>
 */
public class FilteringContentRetriever implements ContentRetriever {

	private static final String KEY_SCORE = "score";

	/** 多值字段：按「包含」而非「相等」匹配 */
	private static final String MULTI_VALUE_TAGS = "tags";

	private static final String MULTI_VALUE_KEYWORDS = "keywords";

	private final ContentRetriever delegate;

	private final double minScore;

	private final Map<String, String> filters;

	public FilteringContentRetriever(ContentRetriever delegate, double minScore, Map<String, String> filters) {
		this.delegate = delegate;
		this.minScore = minScore;
		this.filters = filters == null ? Collections.<String, String>emptyMap() : filters;
	}

	@Override
	public List<Content> retrieve(Query query) {
		List<Content> retrieved = delegate.retrieve(query);
		if (retrieved == null || retrieved.isEmpty()) {
			return Collections.emptyList();
		}
		List<Content> kept = new ArrayList<>();
		for (Content content : retrieved) {
			if (scoreOf(content) < minScore) {
				continue;
			}
			if (!matchesFilters(content)) {
				continue;
			}
			kept.add(content);
		}
		return kept;
	}

	/** filters 全部命中才保留；任一不满足即淘汰。 */
	private boolean matchesFilters(Content content) {
		Metadata metadata = content.textSegment().metadata();
		for (Map.Entry<String, String> entry : filters.entrySet()) {
			String actual = metadata == null ? null : metadata.get(entry.getKey());
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
		String score = content.textSegment().metadata(KEY_SCORE);
		if (score == null || score.trim().isEmpty()) {
			return 1.0;
		}
		try {
			return Double.parseDouble(score.trim());
		} catch (NumberFormatException e) {
			return 1.0;
		}
	}
}
