package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.rag;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 组合检索器：持有多个 {@link ContentRetriever}，并行扇出后把结果合并成一份。
 *
 * <p>执行与合并规则：</p>
 * <ul>
 *   <li>所有子检索器<b>并行</b>发起（各占一个线程），总耗时约等于最慢的一路，而不是各路之和；</li>
 *   <li>合并按子检索器的<b>声明顺序</b>（不是完成顺序），保证结果稳定；</li>
 *   <li>{@code deduplicate} 开启时按正文文本去重（同一份资料来自多个库时只留第一条）；</li>
 *   <li>{@code maxResults} 大于 0 时合并到该条数即返回（提前收敛，避免把无关内容堆给模型）；</li>
 *   <li>单路抛错只记日志并跳过（多路检索中一路不可用不应让整次检索失败）。</li>
 * </ul>
 *
 * <p>线程池按子检索器数量创建，并在 {@link #close()} 时释放（datasource 下线/更新时由实例持有者关闭）。
 * 注意：子检索器在检索时才解析，因此配置里不要让本实例直接或间接引用自己（会无限递归）。</p>
 */
public class AggregatingContentRetriever implements ContentRetriever, Closeable {

	private static final Logger logger = LoggerFactory.getLogger(AggregatingContentRetriever.class);

	private final List<ContentRetriever> delegates;

	/** 子检索器 key，仅用于失败日志 */
	private final List<String> keys;

	private final int maxResults;

	private final boolean deduplicate;

	/** 并行执行池；无子检索器或已关闭时为 null */
	private ExecutorService executor;

	public AggregatingContentRetriever(List<ContentRetriever> delegates, List<String> keys,
			int maxResults, boolean deduplicate) {
		this.delegates = delegates == null ? Collections.<ContentRetriever>emptyList() : delegates;
		this.keys = keys == null ? Collections.<String>emptyList() : keys;
		this.maxResults = maxResults;
		this.deduplicate = deduplicate;
		this.executor = this.delegates.isEmpty() ? null : newExecutor(this.delegates.size());
	}

	/** 数组构造：默认不限制条数、不去重（需要 keys/上限/去重用全参构造）。 */
	public AggregatingContentRetriever(ContentRetriever... delegates) {
		this(delegates == null ? Collections.<ContentRetriever>emptyList() : Arrays.asList(delegates),
				null, 0, false);
	}

	/** 数组构造：带合并上限与去重开关（keys 缺省为序号）。 */
	public AggregatingContentRetriever(int maxResults, boolean deduplicate, ContentRetriever... delegates) {
		this(delegates == null ? Collections.<ContentRetriever>emptyList() : Arrays.asList(delegates),
				null, maxResults, deduplicate);
	}

	@Override
	public List<Content> retrieve(Query query) {
		if (executor == null) {
			return Collections.emptyList();
		}
		List<Future<List<Content>>> futures = new ArrayList<>(delegates.size());
		for (final ContentRetriever delegate : delegates) {
			futures.add(executor.submit(() -> delegate.retrieve(query)));
		}

		List<Content> merged = new ArrayList<>();
		Set<String> seen = deduplicate ? new HashSet<String>() : null;
		for (int index = 0; index < futures.size(); index++) {
			List<Content> part;
			try {
				part = futures.get(index).get();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return merged;
			} catch (ExecutionException e) {
				logger.warn("组合检索：子检索器 {} 检索失败，已跳过", keyOf(index), e.getCause());
				continue;
			}
			if (part == null) {
				continue;
			}
			for (Content content : part) {
				if (seen != null && !seen.add(textOf(content))) {
					continue;
				}
				merged.add(content);
				if (maxResults > 0 && merged.size() >= maxResults) {
					return merged;
				}
			}
		}
		return merged;
	}

	@Override
	public void close() {
		ExecutorService current = executor;
		executor = null;
		if (current != null) {
			current.shutdownNow();
		}
	}

	private String keyOf(int index) {
		return index < keys.size() ? keys.get(index) : String.valueOf(index);
	}

	private static ExecutorService newExecutor(int size) {
		final AtomicInteger counter = new AtomicInteger();
		ThreadFactory factory = runnable -> {
			Thread thread = new Thread(runnable, "magic-content-retriever-" + counter.incrementAndGet());
			thread.setDaemon(true);
			return thread;
		};
		return Executors.newFixedThreadPool(size, factory);
	}

	private static String textOf(Content content) {
		if (content == null) {
			return "";
		}
		TextSegment segment = content.textSegment();
		return segment == null || segment.text() == null ? "" : segment.text();
	}
}
