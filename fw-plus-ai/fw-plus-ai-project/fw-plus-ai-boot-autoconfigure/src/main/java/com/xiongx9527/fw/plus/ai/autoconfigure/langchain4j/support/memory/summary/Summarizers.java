package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 摘要策略注册表：按名字命中内置策略，按全限定类名反射构造（照 {@code AiPersistenceStrategy.Registry} 先例）。
 *
 * <p>内置 5 个策略（全部首版实现）：{@code refine}（默认）/ {@code rebuild} / {@code map-reduce} /
 * {@code segmented} / {@code structured}。新增策略不改任何调用方——注册名或提供无参构造的 FQCN 即可。</p>
 */
public final class Summarizers {

	public static final String REFINE = "refine";
	public static final String REBUILD = "rebuild";
	public static final String MAP_REDUCE = "map-reduce";
	public static final String SEGMENTED = "segmented";
	public static final String STRUCTURED = "structured";

	/** 默认策略名 */
	public static final String DEFAULT = REFINE;

	/** 抽取式回退：无模型时每 token 约按 4 字符折算 */
	private static final int CHARS_PER_TOKEN = 4;

	private Summarizers() {
	}

	/**
	 * 解析摘要策略：先按注册名（内置），再按 FQCN 反射（无参构造）。解析失败返回 {@code null}。
	 *
	 * @param nameOrClass 注册名或全限定类名
	 * @param model       摘要模型；可为 null（内置策略退化为抽取式截断）
	 */
	public static Summarizer resolve(String nameOrClass, SummaryModel model) {
		return REGISTRY.resolve(nameOrClass, model);
	}

	/** 解析摘要策略；失败时回退默认 {@link #REFINE} */
	public static Summarizer resolveOrDefault(String nameOrClass, SummaryModel model) {
		Summarizer resolved = resolve(nameOrClass, model);
		return resolved == null ? new RefineSummarizer(model) : resolved;
	}

	/** 策略注册表 */
	public static final class REGISTRY {

		private static final Map<String, Builder> BUILDERS = new ConcurrentHashMap<String, Builder>();

		static {
			register(REFINE, RefineSummarizer::new);
			register(REBUILD, RebuildSummarizer::new);
			register(MAP_REDUCE, MapReduceSummarizer::new);
			register(SEGMENTED, SegmentedSummarizer::new);
			register(STRUCTURED, StructuredStateSummarizer::new);
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
		public static Summarizer resolve(String nameOrClass, SummaryModel model) {
			if (nameOrClass == null || nameOrClass.trim().isEmpty()) {
				return null;
			}
			String key = nameOrClass.trim();
			Builder builder = BUILDERS.get(key.toLowerCase());
			if (builder != null) {
				return builder.build(model);
			}
			return reflect(key, model);
		}

		private static Summarizer reflect(String className, SummaryModel model) {
			try {
				Class<?> clazz = Class.forName(className);
				if (!Summarizer.class.isAssignableFrom(clazz)) {
					return null;
				}
				// 优先构造器 (SummaryModel)，其次无参构造
				try {
					Constructor<?> withModel = clazz.getDeclaredConstructor(SummaryModel.class);
					if (!withModel.isAccessible()) {
						withModel.setAccessible(true);
					}
					return (Summarizer) withModel.newInstance(model);
				} catch (NoSuchMethodException ignored) {
					Constructor<?> ctor = clazz.getDeclaredConstructor();
					if (!ctor.isAccessible()) {
						ctor.setAccessible(true);
					}
					return (Summarizer) ctor.newInstance();
				}
			} catch (Exception ignored) {
				return null;
			}
		}
	}

	/** 策略工厂（把模型绑定到策略实例） */
	public interface Builder {

		Summarizer build(SummaryModel model);

	}

	// ------------------------------------------------------------------
	// 内置策略
	// ------------------------------------------------------------------

	/** 无模型时的抽取式回退：截断到 token 预算（不调 LLM）。 */
	static String extractive(String text, int maxTokens) {
		if (text == null) {
			return "";
		}
		String flattened = text.replaceAll("\\s+", " ").trim();
		if (maxTokens <= 0) {
			return flattened;
		}
		int maxChars = maxTokens * CHARS_PER_TOKEN;
		return flattened.length() <= maxChars ? flattened : flattened.substring(0, maxChars);
	}

	static String join(List<String> parts, CharSequence delimiter) {
		StringBuilder builder = new StringBuilder();
		for (String part : parts) {
			if (part == null || part.trim().isEmpty()) {
				continue;
			}
			if (builder.length() > 0) {
				builder.append(delimiter);
			}
			builder.append(part.trim());
		}
		return builder.toString();
	}

	/** 把 source 按行切分成若干段（用于 map-reduce / segmented）。 */
	static List<String> segments(String source, int maxSegments) {
		List<String> result = new ArrayList<String>();
		if (source == null || source.trim().isEmpty()) {
			return result;
		}
		String remaining = source;
		int perSegment = maxSegments <= 0 ? source.length() : Math.max(1, source.length() / maxSegments);
		int index = 0;
		while (index < remaining.length() && result.size() < Math.max(1, maxSegments)) {
			int end = Math.min(remaining.length(), index + perSegment);
			result.add(remaining.substring(index, end));
			index = end;
		}
		return result;
	}

	/** 增量精炼（默认）：旧摘要 + 新增轮次 → 新摘要。 */
	public static final class RefineSummarizer implements Summarizer {

		private final SummaryModel model;

		public RefineSummarizer(SummaryModel model) {
			this.model = model;
		}

		@Override
		public String name() {
			return REFINE;
		}

		@Override
		public String summarize(SummarizeRequest request) {
			String addition = join(request.getNewTurns(), "\n");
			if (addition.trim().isEmpty()) {
				addition = request.sourceText();
			}
			String combined = join(java.util.Arrays.asList(request.getExistingSummary(), addition), "\n");
			if (model != null) {
				return model.summarize("请把以下历史增量精炼为简洁摘要（保留主线与关键事实）：\n" + combined);
			}
			return extractive(combined, request.getMaxTokens());
		}
	}

	/** 全量重摘：对窗口外全文重摘（防漂移，由 rebuild-every 定期插入）。 */
	public static final class RebuildSummarizer implements Summarizer {

		private final SummaryModel model;

		public RebuildSummarizer(SummaryModel model) {
			this.model = model;
		}

		@Override
		public String name() {
			return REBUILD;
		}

		@Override
		public String summarize(SummarizeRequest request) {
			String source = request.sourceText();
			if (model != null) {
				return model.summarize("请把以下完整历史重新压缩为简洁摘要（保留主线与关键事实）：\n" + source);
			}
			return extractive(source, request.getMaxTokens());
		}
	}

	/** 分段映射归并：分段摘 → 归并。 */
	public static final class MapReduceSummarizer implements Summarizer {

		private final SummaryModel model;

		public MapReduceSummarizer(SummaryModel model) {
			this.model = model;
		}

		@Override
		public String name() {
			return MAP_REDUCE;
		}

		@Override
		public String summarize(SummarizeRequest request) {
			String source = request.sourceText();
			List<String> segments = segments(source, 4);
			List<String> partial = new ArrayList<String>();
			for (String segment : segments) {
				if (model != null) {
					partial.add(model.summarize("请压缩以下片段：\n" + segment));
				} else {
					partial.add(extractive(segment, 0));
				}
			}
			String merged = join(partial, "\n");
			if (model != null && partial.size() > 1) {
				return model.summarize("请归并以下分段摘要为一份摘要：\n" + merged);
			}
			return extractive(merged, request.getMaxTokens());
		}
	}

	/** 逐段留存：逐段摘要留存并拼接（可回溯分段）。 */
	public static final class SegmentedSummarizer implements Summarizer {

		private final SummaryModel model;

		public SegmentedSummarizer(SummaryModel model) {
			this.model = model;
		}

		@Override
		public String name() {
			return SEGMENTED;
		}

		@Override
		public String summarize(SummarizeRequest request) {
			List<String> segments = segments(request.sourceText(), 4);
			List<String> blocks = new ArrayList<String>();
			int index = 1;
			for (String segment : segments) {
				String body = model != null
						? model.summarize("请摘要以下片段：\n" + segment)
						: extractive(segment, 0);
				blocks.add("[段" + index + "] " + body);
				index++;
			}
			return extractive(join(blocks, "\n"), request.getMaxTokens());
		}
	}

	/** 结构化状态更新：按槽位就地更新。 */
	public static final class StructuredStateSummarizer implements Summarizer {

		private final SummaryModel model;

		public StructuredStateSummarizer(SummaryModel model) {
			this.model = model;
		}

		@Override
		public String name() {
			return STRUCTURED;
		}

		@Override
		public String summarize(SummarizeRequest request) {
			String source = join(java.util.Arrays.asList(request.getExistingSummary(), request.sourceText()), "\n");
			if (model != null) {
				return model.summarize(
						"请将以下历史更新为结构化状态（目标/约束/已完成/待办/关键事实）：\n" + source);
			}
			return extractive(source, request.getMaxTokens());
		}
	}

}