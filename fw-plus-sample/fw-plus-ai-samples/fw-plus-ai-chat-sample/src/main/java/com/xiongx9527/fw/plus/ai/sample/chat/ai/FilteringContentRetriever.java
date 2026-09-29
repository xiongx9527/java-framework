package com.xiongx9527.fw.plus.ai.sample.chat.ai;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 在原始 {@link ContentRetriever} 之上做元数据过滤与渲染的装饰器。
 *
 * <p>三件事：</p>
 * <ul>
 *   <li><b>score 阈值</b>——低于 {@code minScore} 的条目丢弃。知识库 mock 未命中关键词时会把整库
 *       文档兜底返回，不筛就会把无关资料喂给模型；</li>
 *   <li><b>metadata 过滤</b>——{@code filters} 里的每个键都要在条目 metadata 中匹配
 *       （{@code tags} / {@code keywords} 这类多值字段用「包含」，其余用「相等」）。
 *       远端知识库可能不支持 filters（或支持得不一样），客户端再筛一遍才可靠；</li>
 *   <li><b>渲染</b>——把 {@code title} 拼进正文（《标题》内容），模型能指明出处。</li>
 * </ul>
 *
 * <p>经本类过滤后的结果由框架拼进 user message，因此会随 user message 进入会话记忆——
 * 这是用 {@code retrievalAugmentor} 的既定代价，见 {@code RagAutoConfig} 的说明。</p>
 */
class FilteringContentRetriever implements ContentRetriever {

	private static final String KEY_TITLE = "title";
	private static final String KEY_SCORE = "score";

	/** 多值字段：用「包含」而非「相等」匹配。 */
	private static final String MULTI_VALUE_TAGS = "tags";
	private static final String MULTI_VALUE_KEYWORDS = "keywords";

	private final ContentRetriever delegate;
	private final double minScore;
	private final Map<String, String> filters;

	FilteringContentRetriever(ContentRetriever delegate, double minScore, Map<String, String> filters) {
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
			if (!matchesFilters(content, filters)) {
				continue;
			}
			kept.add(render(content));
		}
		return kept;
	}

	/** filters 全部命中才算通过；任一不满足即淘汰。 */
	private boolean matchesFilters(Content content, Map<String, String> filters) {
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

	/** 把 title 拼到正文前面，模型可以指明出处。 */
	private static Content render(Content content) {
		String title = content.textSegment().metadata(KEY_TITLE);
		String text = content.textSegment().text();
		if (title == null || title.trim().isEmpty()) {
			return content;
		}
		return Content.from("《" + title + "》" + text);
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
