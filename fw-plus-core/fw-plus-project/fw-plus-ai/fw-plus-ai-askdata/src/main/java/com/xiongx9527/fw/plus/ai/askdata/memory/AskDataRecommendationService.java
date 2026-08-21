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

	/**
	 * 记录指定项目和操作人的个人推荐偏好。
	 *
	 * <p>实现必须以 {@code projectId + operatorId} 作为不可拆分的隔离键保存推荐，禁止将个人推荐提升为
	 * 项目级通用建议，也不得被其他操作人读取。推荐内容不能为空白；持久化实现还应按实际部署要求保证
	 * 访问控制和一致性。</p>
	 *
	 * @param context 含项目和操作人标识的隔离上下文，不能为空
	 * @param recommendation 仅属于该操作人的推荐内容，不能为空白
	 * @throws IllegalArgumentException 当参数为空或推荐内容为空白时抛出
	 */
	void record(AskDataProjectContext context, String recommendation);

	/**
	 * 查询当前项目和操作人的个人推荐。
	 *
	 * <p>该方法只允许返回与 {@code context} 完全匹配的个人推荐；没有个人记录时返回空列表。它不以项目级
	 * 通用建议作为回退，调用方需要该能力时必须显式调用带策略参数的重载方法。</p>
	 *
	 * @param context 当前项目和操作人的隔离上下文，不能为空
	 * @return 当前操作人的个人推荐；没有记录时返回空列表
	 * @throws IllegalArgumentException 当 {@code context} 为 {@code null} 时抛出
	 */
	List<String> recommend(AskDataProjectContext context);

	/**
	 * 注册项目级、非个人化的通用建议。
	 *
	 * <p>通用建议只能在操作人身份缺失且调用方策略明确允许时作为回退，不能覆盖或混入任何操作人的个人
	 * 推荐。实现必须按项目隔离通用建议，禁止跨项目共享。</p>
	 *
	 * @param projectId 项目唯一标识，不能为空白
	 * @param recommendation 不含个人偏好的通用推荐内容，不能为空白
	 * @throws IllegalArgumentException 当参数为空或空白时抛出
	 */
	void registerGeneric(String projectId, String recommendation);

	/**
	 * 按项目和可选操作人查询推荐，并由调用方显式决定是否允许通用建议回退。
	 *
	 * <p>当 {@code operatorId} 非空白时，必须只返回该项目和该操作人的个人推荐，即使没有记录也不得回退
	 * 到通用建议。当 {@code operatorId} 缺失或仅包含空白字符时，只有 {@code allowGenericSuggestions}
	 * 为 {@code true} 才可返回同项目的非个人通用建议；否则必须返回空列表。实现不得将该回退用于跨项目
	 * 或跨操作人的数据。</p>
	 *
	 * @param projectId 项目唯一标识，不能为空白
	 * @param operatorId 操作人唯一标识；为空或空白时视为身份缺失
	 * @param allowGenericSuggestions 是否允许身份缺失时回退项目级通用建议
	 * @return 符合隔离和回退规则的推荐列表；无匹配时返回空列表
	 * @throws IllegalArgumentException 当 {@code projectId} 为空或空白时抛出
	 */
	List<String> recommend(String projectId, String operatorId, boolean allowGenericSuggestions);

	/**
	 * 创建用于契约测试和单节点开发的内存参考实现。
	 *
	 * @return 不具备跨进程持久化能力的内存实现
	 */
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
			if (!isBlank(operatorId)) {
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

		private static boolean isBlank(String value) {
			if (value == null || value.length() == 0) {
				return true;
			}
			for (int index = 0; index < value.length(); index++) {
				char character = value.charAt(index);
				if (!Character.isWhitespace(character) && !Character.isSpaceChar(character)) {
					return false;
				}
			}
			return true;
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
