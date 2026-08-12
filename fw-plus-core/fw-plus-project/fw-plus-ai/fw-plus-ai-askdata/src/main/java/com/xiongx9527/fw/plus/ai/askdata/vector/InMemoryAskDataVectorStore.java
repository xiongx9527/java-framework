package com.xiongx9527.fw.plus.ai.askdata.vector;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 单节点内存向量存储。
 *
 * <p>所有数据以 {@code projectId + operatorId} 分区，进程重启后丢失。当前实现只接受文档元数据中真实存在的
 * 等值 filter；无法证明可执行的 filter 会 fail-closed，不能用于集群或持久化场景。</p>
 */
public final class InMemoryAskDataVectorStore implements AskDataVectorStore {

	private final Map<String, Map<String, AskDataVectorDocument>> documents =
			new LinkedHashMap<String, Map<String, AskDataVectorDocument>>();

	@Override
	public synchronized void upsert(AskDataProjectContext context, AskDataVectorDocument document) {
		if (document == null) {
			throw new IllegalArgumentException("document 不能为空");
		}
		Map<String, AskDataVectorDocument> scoped = documents.get(scope(context));
		if (scoped == null) {
			scoped = new LinkedHashMap<String, AskDataVectorDocument>();
			documents.put(scope(context), scoped);
		}
		scoped.put(document.getId(), document);
	}

	@Override
	public synchronized List<AskDataVectorDocument> search(AskDataProjectContext context,
			AskDataVectorSearchRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("request 不能为空");
		}
		Map<String, AskDataVectorDocument> scoped = documents.get(scope(context));
		if (scoped == null) {
			validateFilters(request, Collections.<AskDataVectorDocument>emptyList());
			return Collections.emptyList();
		}
		List<AskDataVectorDocument> candidates = new ArrayList<AskDataVectorDocument>(scoped.values());
		validateFilters(request, candidates);
		List<ScoredDocument> matched = new ArrayList<ScoredDocument>();
		for (AskDataVectorDocument document : candidates) {
			if (matches(document, request.getFilters())) {
				matched.add(new ScoredDocument(document, cosine(request.getVector(), document.getVector())));
			}
		}
		Collections.sort(matched, new Comparator<ScoredDocument>() {
			@Override
			public int compare(ScoredDocument left, ScoredDocument right) {
				int score = Double.compare(right.score, left.score);
				return score == 0 ? left.document.getId().compareTo(right.document.getId()) : score;
			}
		});
		List<AskDataVectorDocument> result = new ArrayList<AskDataVectorDocument>();
		for (int index = 0; index < matched.size() && index < request.getLimit(); index++) {
			result.add(matched.get(index).document);
		}
		return Collections.unmodifiableList(result);
	}

	private static void validateFilters(AskDataVectorSearchRequest request,
			List<AskDataVectorDocument> candidates) {
		for (String filter : request.getFilters().keySet()) {
			boolean supported = false;
			for (AskDataVectorDocument candidate : candidates) {
				if (candidate.getMetadata().containsKey(filter)) {
					supported = true;
					break;
				}
			}
			if (!supported) {
				throw new IllegalArgumentException("无法应用 vector filter: " + filter);
			}
		}
	}

	private static boolean matches(AskDataVectorDocument document, Map<String, String> filters) {
		for (Map.Entry<String, String> filter : filters.entrySet()) {
			if (!equals(filter.getValue(), document.getMetadata().get(filter.getKey()))) {
				return false;
			}
		}
		return true;
	}

	private static double cosine(float[] left, float[] right) {
		if (left.length != right.length || left.length == 0) {
			throw new IllegalArgumentException("向量维度不一致");
		}
		double dot = 0D;
		double leftLength = 0D;
		double rightLength = 0D;
		for (int index = 0; index < left.length; index++) {
			dot += left[index] * right[index];
			leftLength += left[index] * left[index];
			rightLength += right[index] * right[index];
		}
		return leftLength == 0D || rightLength == 0D ? 0D : dot / Math.sqrt(leftLength * rightLength);
	}

	private static String scope(AskDataProjectContext context) {
		if (context == null) {
			throw new IllegalArgumentException("context 不能为空");
		}
		return context.getProjectId() + '\n' + context.getOperatorId();
	}

	private static boolean equals(Object left, Object right) {
		return left == null ? right == null : left.equals(right);
	}

	private static final class ScoredDocument {
		private final AskDataVectorDocument document;
		private final double score;
		private ScoredDocument(AskDataVectorDocument document, double score) {
			this.document = document;
			this.score = score;
		}
	}
}
