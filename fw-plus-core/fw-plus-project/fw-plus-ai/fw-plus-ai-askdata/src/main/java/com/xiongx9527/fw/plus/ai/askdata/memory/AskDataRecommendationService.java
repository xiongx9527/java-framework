package com.xiongx9527.fw.plus.ai.askdata.memory;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AskData 推荐服务契约。
 *
 * <p>个人偏好始终按 {@code projectId + operatorId} 隔离。缺少操作人标识时，仅当调用方策略明确允许，
 * 才可返回项目级通用非个人建议。</p>
 */
public interface AskDataRecommendationService {

	void record(AskDataProjectContext context, String recommendation);

	List<String> recommend(AskDataProjectContext context);

	void registerGeneric(String projectId, String recommendation);

	List<String> recommend(String projectId, String operatorId, boolean allowGenericSuggestions);

	static AskDataRecommendationService inMemory() {
		return new InMemory();
	}

	/**
	 * 用于契约测试和单节点开发的内存参考实现。
	 */
	final class InMemory implements AskDataRecommendationService {

		private final Map<ScopeKey, List<String>> personalRecommendations = new LinkedHashMap<ScopeKey, List<String>>();
		private final Map<String, List<String>> genericRecommendations = new LinkedHashMap<String, List<String>>();

		@Override
		public synchronized void record(AskDataProjectContext context, String recommendation) {
			if (context == null) {
				throw new IllegalArgumentException("context 不能为空");
			}
			listFor(personalRecommendations, new ScopeKey(context.getProjectId(), context.getOperatorId()))
					.add(requireNonBlank(recommendation, "recommendation"));
		}

		@Override
		public synchronized List<String> recommend(AskDataProjectContext context) {
			if (context == null) {
				throw new IllegalArgumentException("context 不能为空");
			}
			return copyOf(personalRecommendations.get(new ScopeKey(context.getProjectId(), context.getOperatorId())));
		}

		@Override
		public synchronized void registerGeneric(String projectId, String recommendation) {
			listFor(genericRecommendations, requireNonBlank(projectId, "projectId"))
					.add(requireNonBlank(recommendation, "recommendation"));
		}

		@Override
		public synchronized List<String> recommend(String projectId, String operatorId, boolean allowGenericSuggestions) {
			String normalizedProjectId = requireNonBlank(projectId, "projectId");
			if (operatorId != null && operatorId.trim().length() > 0) {
				return copyOf(personalRecommendations.get(new ScopeKey(normalizedProjectId, operatorId.trim())));
			}
			return allowGenericSuggestions ? copyOf(genericRecommendations.get(normalizedProjectId)) : Collections.<String>emptyList();
		}

		private static <K> List<String> listFor(Map<K, List<String>> values, K key) {
			List<String> list = values.get(key);
			if (list == null) {
				list = new ArrayList<String>();
				values.put(key, list);
			}
			return list;
		}

		private static List<String> copyOf(List<String> values) {
			return values == null ? Collections.<String>emptyList() : new ArrayList<String>(values);
		}

		private static String requireNonBlank(String value, String name) {
			if (value == null || value.trim().length() == 0) {
				throw new IllegalArgumentException(name + " 不能为空白");
			}
			return value.trim();
		}
	}

	final class ScopeKey {

		private final String projectId;
		private final String operatorId;

		ScopeKey(String projectId, String operatorId) {
			this.projectId = projectId;
			this.operatorId = operatorId;
		}

		@Override
		public boolean equals(Object object) {
			if (this == object) {
				return true;
			}
			if (!(object instanceof ScopeKey)) {
				return false;
			}
			ScopeKey other = (ScopeKey) object;
			return projectId.equals(other.projectId) && operatorId.equals(other.operatorId);
		}

		@Override
		public int hashCode() {
			return 31 * projectId.hashCode() + operatorId.hashCode();
		}
	}
}
