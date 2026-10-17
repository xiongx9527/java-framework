package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 事实召回策略注册表：按名字命中内置策略（{@code semantic} / {@code keyword} / {@code hybrid} /
 * {@code rerank}），按全限定类名反射构造（与 {@code FactExtractors.REGISTRY} 同范式）；
 * 均失败视为未知名，由 {@link #resolveOrThrow} 报错（装配层快速失败）或 {@link #resolve} 返回 {@code null}。
 *
 * <p>策略语义（设计 §3.5）：</p>
 * <ul>
 *   <li>{@code semantic}（默认）：委托 mem0 服务端排序（{@code store.search}）；</li>
 *   <li>{@code keyword}：样本 200 条 + 本地覆盖率评分（仅 score &gt; 0，稳定降序取 top-K）；</li>
 *   <li>{@code hybrid}：semantic(2K) + keyword(2K) → RRF（k=60）融合，按事实文本去重；</li>
 *   <li>{@code rerank}：semantic(4K) → 模型重排（{@code ScoringModel}）；未配模型或模型失败
 *       回退本地关键词重叠重排。</li>
 * </ul>
 */
public final class FactRetrievers {

	public static final String SEMANTIC = "semantic";

	public static final String KEYWORD = "keyword";

	public static final String HYBRID = "hybrid";

	public static final String RERANK = "rerank";

	/** 默认策略名 */
	public static final String DEFAULT = SEMANTIC;

	private FactRetrievers() {
	}

	/**
	 * 解析召回策略：先按注册名（内置），再按 FQCN 反射（(FactStore, ScoringModel) →
	 * (FactStore) → 无参构造）。解析失败返回 {@code null}。
	 */
	public static FactRetriever resolve(String nameOrClass, FactStore store, ScoringModel rerankModel) {
		return REGISTRY.resolve(nameOrClass, store, rerankModel);
	}

	/** 解析召回策略；失败抛 {@link IllegalArgumentException}（装配层快速失败用）。 */
	public static FactRetriever resolveOrThrow(String nameOrClass, FactStore store, ScoringModel rerankModel) {
		FactRetriever resolved = resolve(nameOrClass, store, rerankModel);
		if (resolved == null) {
			throw new IllegalArgumentException("未知事实召回策略（非内置 " + SEMANTIC + "/" + KEYWORD + "/" + HYBRID
					+ "/" + RERANK + "，也非可构造的 FQCN）：" + nameOrClass);
		}
		return resolved;
	}

	/** 策略注册表 */
	public static final class REGISTRY {

		private static final Map<String, Builder> BUILDERS = new ConcurrentHashMap<String, Builder>();

		static {
			register(SEMANTIC, (store, rerankModel) -> new SemanticFactRetriever(store));
			register(KEYWORD, (store, rerankModel) -> new KeywordFactRetriever(store));
			register(HYBRID, (store, rerankModel) -> new HybridFactRetriever(store));
			register(RERANK, (store, rerankModel) -> new RerankFactRetriever(store, rerankModel));
		}

		private REGISTRY() {
		}

		public static void register(String name, Builder builder) {
			if (name == null || name.trim().isEmpty() || builder == null) {
				return;
			}
			BUILDERS.put(name.trim().toLowerCase(), builder);
		}

		public static boolean isBuiltIn(String name) {
			return name != null && BUILDERS.containsKey(name.trim().toLowerCase());
		}

		/** 内置名优先；否则按 FQCN 反射构造；均失败返回 null */
		public static FactRetriever resolve(String nameOrClass, FactStore store, ScoringModel rerankModel) {
			if (nameOrClass == null || nameOrClass.trim().isEmpty()) {
				return null;
			}
			String key = nameOrClass.trim();
			Builder builder = BUILDERS.get(key.toLowerCase());
			if (builder != null) {
				return builder.build(store, rerankModel);
			}
			return reflect(key, store, rerankModel);
		}

		private static FactRetriever reflect(String className, FactStore store, ScoringModel rerankModel) {
			try {
				Class<?> clazz = Class.forName(className);
				if (!FactRetriever.class.isAssignableFrom(clazz)) {
					return null;
				}
				// 依次尝试 (FactStore, ScoringModel)、(FactStore)、无参构造
				try {
					Constructor<?> withBoth = clazz.getDeclaredConstructor(FactStore.class, ScoringModel.class);
					withBoth.setAccessible(true);
					return (FactRetriever) withBoth.newInstance(store, rerankModel);
				} catch (NoSuchMethodException ignored) {
					// 继续尝试下一构造形态
				}
				try {
					Constructor<?> withStore = clazz.getDeclaredConstructor(FactStore.class);
					withStore.setAccessible(true);
					return (FactRetriever) withStore.newInstance(store);
				} catch (NoSuchMethodException ignored) {
					Constructor<?> noArg = clazz.getDeclaredConstructor();
					noArg.setAccessible(true);
					return (FactRetriever) noArg.newInstance();
				}
			} catch (Exception ignored) {
				return null;
			}
		}
	}

	/** 策略工厂（把存储/重排模型绑定到策略实例） */
	public interface Builder {

		FactRetriever build(FactStore store, ScoringModel rerankModel);

	}

	// ------------------------------------------------------------------
	// 内置策略
	// ------------------------------------------------------------------

	/** 语义召回（默认）：委托 mem0 服务端排序，本地只做清洗与截断。 */
	public static final class SemanticFactRetriever implements FactRetriever {

		private final FactStore store;

		public SemanticFactRetriever(FactStore store) {
			if (store == null) {
				throw new IllegalArgumentException("SemanticFactRetriever: store 不能为空");
			}
			this.store = store;
		}

		@Override
		public List<String> retrieve(String query, String userId, int topK) {
			if (!ready(query, userId, topK)) {
				return new ArrayList<String>();
			}
			return sanitize(store.search(query.trim(), userId.trim(), topK), topK);
		}
	}

	/** 关键词召回：样本 200 条 + 本地覆盖率评分。 */
	public static final class KeywordFactRetriever implements FactRetriever {

		/** 本地评分样本上限 */
		static final int SAMPLE_LIMIT = 200;

		private final FactStore store;

		public KeywordFactRetriever(FactStore store) {
			if (store == null) {
				throw new IllegalArgumentException("KeywordFactRetriever: store 不能为空");
			}
			this.store = store;
		}

		@Override
		public List<String> retrieve(String query, String userId, int topK) {
			if (!ready(query, userId, topK)) {
				return new ArrayList<String>();
			}
			Set<String> queryTokens = FactTokens.tokenize(query);
			if (queryTokens.isEmpty()) {
				return new ArrayList<String>();
			}
			List<String> candidates = store.listAll(userId.trim(), SAMPLE_LIMIT);
			if (candidates == null || candidates.isEmpty()) {
				return new ArrayList<String>();
			}
			// 文本 → 最高分（保序去重）
			Map<String, Double> scored = new LinkedHashMap<String, Double>();
			for (String fact : candidates) {
				String text = normalize(fact);
				if (text == null) {
					continue;
				}
				double score = FactTokens.coverage(queryTokens, FactTokens.tokenize(text));
				if (score <= 0.0d) {
					continue;
				}
				Double current = scored.get(text);
				if (current == null || score > current) {
					scored.put(text, score);
				}
			}
			List<Ranked> ranked = new ArrayList<Ranked>(scored.size());
			for (Map.Entry<String, Double> entry : scored.entrySet()) {
				ranked.add(new Ranked(entry.getKey(), entry.getValue()));
			}
			// 稳定降序：同分保持首次出现顺序
			ranked.sort((a, b) -> Double.compare(b.score, a.score));
			return texts(ranked, topK);
		}
	}

	/** 混合召回：semantic(2K) + keyword(2K) → RRF（k=60）融合。 */
	public static final class HybridFactRetriever implements FactRetriever {

		/** RRF 融合常数 */
		static final int RRF_K = 60;

		private final SemanticFactRetriever semantic;

		private final KeywordFactRetriever keyword;

		public HybridFactRetriever(FactStore store) {
			if (store == null) {
				throw new IllegalArgumentException("HybridFactRetriever: store 不能为空");
			}
			this.semantic = new SemanticFactRetriever(store);
			this.keyword = new KeywordFactRetriever(store);
		}

		@Override
		public List<String> retrieve(String query, String userId, int topK) {
			if (!ready(query, userId, topK)) {
				return new ArrayList<String>();
			}
			int expanded = topK * 2;
			Map<String, Double> fused = new LinkedHashMap<String, Double>();
			accumulate(fused, semantic.retrieve(query, userId, expanded));
			accumulate(fused, keyword.retrieve(query, userId, expanded));
			List<Ranked> ranked = new ArrayList<Ranked>(fused.size());
			for (Map.Entry<String, Double> entry : fused.entrySet()) {
				ranked.add(new Ranked(entry.getKey(), entry.getValue()));
			}
			// 稳定降序：同分保持插入序（semantic 先、keyword 后）
			ranked.sort((a, b) -> Double.compare(b.score, a.score));
			return texts(ranked, topK);
		}

		private static void accumulate(Map<String, Double> fused, List<String> hits) {
			for (int i = 0; i < hits.size(); i++) {
				String fact = hits.get(i);
				double rrf = 1.0d / (RRF_K + i + 1);
				Double current = fused.get(fact);
				fused.put(fact, current == null ? rrf : current + rrf);
			}
		}
	}

	/** 重排召回：semantic(4K) → 模型重排；未配模型或失败回退本地重叠重排。 */
	@Slf4j
	public static final class RerankFactRetriever implements FactRetriever {

		/** 语义召回放大倍数（取 4K 再重排） */
		static final int EXPAND_FACTOR = 4;

		private final FactStore store;

		private final ScoringModel model;

		public RerankFactRetriever(FactStore store, ScoringModel model) {
			if (store == null) {
				throw new IllegalArgumentException("RerankFactRetriever: store 不能为空");
			}
			this.store = store;
			this.model = model;
		}

		@Override
		public List<String> retrieve(String query, String userId, int topK) {
			if (!ready(query, userId, topK)) {
				return new ArrayList<String>();
			}
			int expanded = topK * EXPAND_FACTOR;
			List<String> candidates = sanitize(store.search(query.trim(), userId.trim(), expanded), expanded);
			if (candidates.isEmpty()) {
				return new ArrayList<String>();
			}
			List<String> ranked = model == null ? null : rankByModel(candidates, query);
			if (ranked == null) {
				ranked = rankByOverlap(candidates, query);
			}
			return ranked.size() <= topK ? ranked : new ArrayList<String>(ranked.subList(0, topK));
		}

		private List<String> rankByModel(List<String> candidates, String query) {
			try {
				List<TextSegment> segments = new ArrayList<TextSegment>(candidates.size());
				for (String fact : candidates) {
					segments.add(TextSegment.from(fact));
				}
				Response<List<Double>> response = model.scoreAll(segments, query);
				List<Double> scores = response == null ? null : response.content();
				if (scores == null || scores.size() != candidates.size()) {
					log.warn("rerank 模型评分条数不匹配（{} != {}），回退本地重叠",
							scores == null ? 0 : scores.size(), candidates.size());
					return null;
				}
				List<Ranked> ranked = new ArrayList<Ranked>(candidates.size());
				for (int i = 0; i < candidates.size(); i++) {
					Double score = scores.get(i);
					ranked.add(new Ranked(candidates.get(i), score == null ? 0.0d : score));
				}
				ranked.sort((a, b) -> Double.compare(b.score, a.score));
				return texts(ranked, candidates.size());
			} catch (Exception e) {
				log.warn("rerank 模型调用失败，回退本地重叠：{}", e.toString());
				return null;
			}
		}

		private static List<String> rankByOverlap(List<String> candidates, String query) {
			Set<String> queryTokens = FactTokens.tokenize(query);
			List<Ranked> ranked = new ArrayList<Ranked>(candidates.size());
			for (String fact : candidates) {
				ranked.add(new Ranked(fact, FactTokens.coverage(queryTokens, FactTokens.tokenize(fact))));
			}
			ranked.sort((a, b) -> Double.compare(b.score, a.score));
			return texts(ranked, candidates.size());
		}
	}

	// ------------------------------------------------------------------
	// 共享工具
	// ------------------------------------------------------------------

	/** 文本 + 分数（本地评分与重排共用，稳定排序前构造） */
	static final class Ranked {

		private final String text;

		private final double score;

		Ranked(String text, double score) {
			this.text = text;
			this.score = score;
		}
	}

	/** 就绪守卫：query / userId 非空白、topK &gt;= 1。 */
	private static boolean ready(String query, String userId, int topK) {
		return query != null && !query.trim().isEmpty()
				&& userId != null && !userId.trim().isEmpty()
				&& topK >= 1;
	}

	/** 清洗：过滤 null / 空白、trim、保序去重、截断 topK。 */
	private static List<String> sanitize(List<String> facts, int topK) {
		List<String> cleaned = new ArrayList<String>();
		if (facts != null) {
			for (String fact : facts) {
				String text = normalize(fact);
				if (text == null || cleaned.contains(text)) {
					continue;
				}
				cleaned.add(text);
				if (cleaned.size() >= topK) {
					break;
				}
			}
		}
		return cleaned;
	}

	private static String normalize(String text) {
		if (text == null) {
			return null;
		}
		String trimmed = text.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static List<String> texts(List<Ranked> ranked, int limit) {
		List<String> facts = new ArrayList<String>(Math.min(ranked.size(), Math.max(limit, 0)));
		for (Ranked item : ranked) {
			if (facts.size() >= limit) {
				break;
			}
			facts.add(item.text);
		}
		return facts;
	}
}
