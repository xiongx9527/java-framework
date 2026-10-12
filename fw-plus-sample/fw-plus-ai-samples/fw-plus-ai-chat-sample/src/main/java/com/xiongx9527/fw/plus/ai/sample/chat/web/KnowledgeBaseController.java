package com.xiongx9527.fw.plus.ai.sample.chat.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 内置 mock 知识库服务：实现知识库服务端契约，供
 * {@link com.xiongx9527.fw.plus.ai.sample.chat.ai.knowledge.RemoteKnowledgeBaseContentRetriever} 调用。
 *
 * <p>契约（与检索器 {@code KnowledgeSearchRequest/Response} 对齐）：</p>
 * <pre>
 * POST /api/search          Content-Type: application/json
 * {"query":"用户原话","topK":3}
 * → {"items":[{"title":"...","content":"...","score":0.91}]}
 * </pre>
 *
 * <p>本地不接向量库，用关键词子串匹配内置文档，仅用于把 RAG 链路跑通：
 * chat-sample 既是检索调用方，又通过本接口自给上下文。要接真实知识库时，
 * 只需把 {@code fw.plus.sample.chat.knowledge-base.base-url} 指向实现了同契约的服务，
 * 并移除本 Controller 即可。</p>
 *
 * <p>文档主题与 {@link com.xiongx9527.fw.plus.ai.sample.chat.ai.WeatherTools} 的 mock 呼应，
 * 使"天气 / 两地比较"类提问能被知识库命中。</p>
 */
@RestController
public class KnowledgeBaseController {

	/** 一篇知识库文档。 */
	private static final class Doc {
		private final String title;
		private final String content;
		private final String category;
		private final List<String> tags;
		private final List<String> keywords;

		private Doc(String title, String content, String category, String[] tags, String... keywords) {
			this.title = title;
			this.content = content;
			this.category = category;
			this.tags = new ArrayList<>();
			if (tags != null) {
				for (String tag : tags) {
					this.tags.add(tag);
				}
			}
			this.keywords = new ArrayList<>();
			for (String keyword : keywords) {
				this.keywords.add(keyword);
			}
		}
	}

	/** 内置文档集。 */
	private static final List<Doc> DOCS = new ArrayList<>();

	static {
		/*
		 * 反例，勿启用：把实时天气数据当静态知识塞进知识库的后果。
		 *
		 * RAG 注入发生在工具决策之前——DefaultAiServices 先 augment 再 generate，
		 * 模型第一轮就在 user message 里看到"气温 29℃/湿度 78%"这类完整数值，
		 * 上下文里已有一份看起来像答案的数据，于是直接引用、跳过 tool_call。
		 * 调用次数是省了，用的却是知识库里的过期快照，两头不讨好。
		 *
		 * 分层原则：实时值 / 当前状态走 Tool，静态规则 / 本地文档才进知识库。
		 * 下面被注释的"当前天气"属于前者；同文件里"夏季气候"那类才是知识库该有的内容。
		 *
		 * 可以试着打开后问"杭州天气怎么样"，ChatContextLogger 会显示 RAG 段注入成功
		 * 但没有任何 TOOL_CALL——模型跳过了工具。
		 */
		/**
		DOCS.add(new Doc("杭州当前天气",
				"杭州实时天气：多云转阴，气温 29℃，湿度 78%，风力 2 级，体感闷热。",
				"杭州", "天气", "气温", "现在", "实时", "热", "怎么样"));
		DOCS.add(new Doc("北京当前天气",
				"北京实时天气：晴、午后有雷阵雨，气温 32℃，湿度 45%，风力 3 级，干热。",
				"北京", "天气", "气温", "现在", "实时", "热", "怎么样"));
		DOCS.add(new Doc("杭州夏季气候",
				"杭州夏季炎热潮湿，7-8 月平均 28-35℃，湿度常达 75% 以上，闷热多阵雨。",
				"杭州", "夏天", "夏季", "气候", "出行"));
		DOCS.add(new Doc("北京夏季气候",
				"北京夏季干热，7-8 月平均 26-33℃，湿度约 40-50%，日照强、紫外线强，偶有雷阵雨。",
				"北京", "夏天", "夏季", "气候", "出行"));**/
		DOCS.add(new Doc("夏季出行建议",
				"夏季出行注意防暑补水；湿热地区（如杭州）宜早晚出行，干热地区（如北京）注意防晒。",
				"guide", new String[]{"出行", "夏季"},
				"出行", "旅游", "建议", "适合", "夏天", "夏季"));
		DOCS.add(new Doc("杭州热门景点",
				"杭州著名景点有西湖、灵隐寺、西溪湿地；夏季建议清晨或傍晚游西湖避暑。",
				"sight", new String[]{"杭州", "避暑"},
				"杭州", "景点", "旅游", "西湖"));
		DOCS.add(new Doc("北京热门景点",
				"北京著名景点有故宫、长城、颐和园；夏季注意防晒，长城宜清晨前往。",
				"sight", new String[]{"北京", "防晒"},
				"北京", "景点", "旅游", "故宫", "长城"));
	}

	/** 检索请求体：{@code {"query":"...","topK":3}}，与 {@code KnowledgeSearchRequest} 对齐。 */
	public static class SearchRequest {
		private String query;
		private int topK;
		private Map<String, String> filters;

		public String getQuery() {
			return query;
		}

		public void setQuery(String query) {
			this.query = query;
		}

		public int getTopK() {
			return topK;
		}

		public Map<String, String> getFilters() {
			return filters == null ? new LinkedHashMap<String, String>() : filters;
		}

		public void setTopK(int topK) {
			this.topK = topK;
		}
	}

	/**
	 * 知识库检索：按关键词匹配内置文档，返回 {@code topK} 条 {@code {title,content,score}}。
	 *
	 * <p>无关键词命中时退回文档集前 {@code topK} 条，保证 RAG 演示有上下文可见。</p>
	 */
	@PostMapping("/api/search")
	public Map<String, Object> search(@RequestBody SearchRequest request) {
		String query = (request == null || request.getQuery() == null) ? "" : request.getQuery().trim();
		int topK = (request == null || request.getTopK() <= 0) ? 3 : request.getTopK();

		Map<String, String> filters = (request == null || request.getFilters() == null)
				? new LinkedHashMap<String, String>()
				: request.getFilters();

		List<Doc> matched = new ArrayList<>();
		for (Doc doc : DOCS) {
			if (!matchesFilters(doc, filters)) {
				continue;
			}
			if (hit(doc, query)) {
				matched.add(doc);
			}
		}
		List<Doc> chosen = matched.isEmpty() ? subset(DOCS, topK) : subset(matched, topK);

		List<Map<String, Object>> items = new ArrayList<>();
		double score = 0.95;
		for (Doc doc : chosen) {
			Map<String, Object> item = new LinkedHashMap<>();
			Map<String, Object> metadata = new LinkedHashMap<>();
			metadata.put("title", doc.title);
			metadata.put("category", doc.category);
			metadata.put("tags", String.join(",", doc.tags));
			metadata.put("keywords", String.join(",", doc.keywords));
			item.put("title", doc.title);
			item.put("content", doc.content);
			item.put("score", score);
			item.put("metadata", metadata);
			items.add(item);
			score = Math.max(0.4, score - 0.05);
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("items", items);
		return body;
	}

	/** filters 逐键匹配：tags/keywords 用逗号包含，其余字段用相等。 */
	private static boolean matchesFilters(Doc doc, Map<String, String> filters) {
		for (Map.Entry<String, String> entry : filters.entrySet()) {
			if (entry.getValue() == null || entry.getValue().trim().isEmpty()) {
				continue;
			}
			String key = entry.getKey();
			String expected = entry.getValue().trim();
			if ("tags".equals(key)) {
				if (!doc.tags.contains(expected)) {
					return false;
				}
			} else if ("keywords".equals(key)) {
				if (!doc.keywords.contains(expected)) {
					return false;
				}
			} else if ("category".equals(key)) {
				if (!expected.equals(doc.category)) {
					return false;
				}
			} else if ("title".equals(key)) {
				if (!doc.title.contains(expected)) {
					return false;
				}
			}
		}
		return true;
	}

	/** query 是否命中该文档任一关键词（忽略大小写的子串匹配）。 */
	private static boolean hit(Doc doc, String query) {
		if (query.isEmpty()) {
			return false;
		}
		String lower = query.toLowerCase();
		for (String keyword : doc.keywords) {
			if (lower.contains(keyword.toLowerCase())) {
				return true;
			}
		}
		return false;
	}

	private static List<Doc> subset(List<Doc> docs, int topK) {
		List<Doc> result = new ArrayList<>();
		for (int i = 0; i < docs.size() && i < topK; i++) {
			result.add(docs.get(i));
		}
		return result;
	}
}
