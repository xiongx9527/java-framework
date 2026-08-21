package com.xiongx9527.fw.plus.ai.askdata.memory;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * AskData 长期记忆契约。
 */
public interface AskDataLongTermMemory {

	/**
	 * 保存一条已绑定隔离上下文的长期记忆。
	 *
	 * <p>实现必须连同条目中的项目、操作人、数据源和语义版本一并保存，不能把任一维度降级为全局记忆。
	 * 生产实现应提供满足部署形态的一致性、持久化和访问控制；本接口不允许以缺失字段或其他操作人的内容
	 * 作为回退。</p>
	 *
	 * @param entry 待保存的完整记忆条目，不能为空
	 * @throws IllegalArgumentException 当 {@code entry} 为 {@code null} 或条目内容不完整时抛出
	 */
	void remember(AskDataMemoryEntry entry);

	/**
	 * 查询与当前项目、操作人、数据源及语义版本完全匹配的长期记忆。
	 *
	 * <p>查询结果必须同时匹配 {@code context} 中的项目和操作人，以及指定的数据源、语义版本；任何一维
	 * 不匹配的条目都不得返回。无匹配时返回空列表，不得为了提高命中率回退到其他操作人、其他项目、
	 * 其他数据源或其他语义版本的记忆。</p>
	 *
	 * @param context 当前请求的项目与操作人隔离上下文，不能为空
	 * @param datasourceKey 数据源唯一标识，不能为空白
	 * @param semanticVersion 语义模型版本，不能为空白
	 * @return 完全匹配的记忆条目；没有匹配项时返回空列表
	 * @throws IllegalArgumentException 当任一参数为空或空白时抛出
	 */
	List<AskDataMemoryEntry> find(AskDataProjectContext context, String datasourceKey, String semanticVersion);

	/**
	 * 在当前授权对象和时间范围内召回仍有效的记忆。
	 *
	 * <p>条目声明授权范围时，调用方的当前授权必须完整包含该范围；权限撤销后条目即不可召回。空授权范围只兼容
	 * 旧条目，业务应逐步迁移为显式范围。</p>
	 */
	default List<AskDataMemoryEntry> find(AskDataProjectContext context, String datasourceKey, String semanticVersion,
			Set<String> authorizedScope, long nowEpochMillis) {
		List<AskDataMemoryEntry> visible = new ArrayList<AskDataMemoryEntry>();
		Set<String> checkedScope = authorizedScope == null ? Collections.<String>emptySet() : authorizedScope;
		for (AskDataMemoryEntry entry : find(context, datasourceKey, semanticVersion)) {
			if (entry != null && !entry.isExpired(nowEpochMillis)
					&& (entry.getAuthorizationScope().isEmpty()
							|| checkedScope.containsAll(entry.getAuthorizationScope()))) {
				visible.add(entry);
			}
		}
		return Collections.unmodifiableList(visible);
	}

	/**
	 * 按隔离范围遗忘一条记忆并保存脱敏删除审计。
	 */
	default boolean forget(AskDataProjectContext context, String memoryId, String reason,
			long forgottenAtEpochMillis) {
		return false;
	}

	/**
	 * 返回当前项目和操作人的遗忘审计，不包含记忆正文。
	 */
	default List<ForgetAudit> listForgetAudits(AskDataProjectContext context) {
		return Collections.emptyList();
	}

	/**
	 * 创建用于契约测试和单节点开发的内存参考实现。
	 *
	 * @return 不具备跨进程持久化能力的内存实现
	 */
	static AskDataLongTermMemory inMemory() {
		return new InMemory();
	}

	/**
	 * 用于契约测试和单节点开发的内存参考实现。
	 */
	final class InMemory implements AskDataLongTermMemory {

		private final List<AskDataMemoryEntry> entries = Collections.synchronizedList(new ArrayList<AskDataMemoryEntry>());
		private final List<ForgetAudit> forgetAudits = Collections.synchronizedList(new ArrayList<ForgetAudit>());

		@Override
		public void remember(AskDataMemoryEntry entry) {
			if (entry == null) {
				throw new IllegalArgumentException("entry 不能为空");
			}
			entries.add(entry);
		}

		@Override
		public List<AskDataMemoryEntry> find(AskDataProjectContext context, String datasourceKey, String semanticVersion) {
			return find(context, datasourceKey, semanticVersion, Collections.<String>emptySet(),
					System.currentTimeMillis());
		}

		@Override
		public List<AskDataMemoryEntry> find(AskDataProjectContext context, String datasourceKey, String semanticVersion,
				Set<String> authorizedScope, long nowEpochMillis) {
			if (context == null) {
				throw new IllegalArgumentException("context 不能为空");
			}
			if (nowEpochMillis < 1L) {
				throw new IllegalArgumentException("nowEpochMillis 必须大于 0");
			}
			String normalizedDatasourceKey = requireNonBlank(datasourceKey, "datasourceKey");
			String normalizedSemanticVersion = requireNonBlank(semanticVersion, "semanticVersion");
			Set<String> checkedScope = authorizedScope == null ? Collections.<String>emptySet() : authorizedScope;
			List<AskDataMemoryEntry> matched = new ArrayList<AskDataMemoryEntry>();
			synchronized (entries) {
				for (AskDataMemoryEntry entry : entries) {
					AskDataProjectContext entryContext = entry.getContext();
					if (context.getProjectId().equals(entryContext.getProjectId())
							&& context.getOperatorId().equals(entryContext.getOperatorId())
							&& normalizedDatasourceKey.equals(entry.getDatasourceKey())
							&& normalizedSemanticVersion.equals(entry.getSemanticVersion())
							&& !entry.isExpired(nowEpochMillis)
							&& (entry.getAuthorizationScope().isEmpty()
									|| checkedScope.containsAll(entry.getAuthorizationScope()))) {
						matched.add(entry);
					}
				}
			}
			return Collections.unmodifiableList(matched);
		}

		@Override
		public boolean forget(AskDataProjectContext context, String memoryId, String reason,
				long forgottenAtEpochMillis) {
			if (context == null || forgottenAtEpochMillis < 1L) {
				throw new IllegalArgumentException("context 和 forgottenAtEpochMillis 必须有效");
			}
			String checkedId = requireNonBlank(memoryId, "memoryId");
			String checkedReason = requireNonBlank(reason, "reason");
			synchronized (entries) {
				for (int index = 0; index < entries.size(); index++) {
					AskDataMemoryEntry entry = entries.get(index);
					if (checkedId.equals(entry.getId()) && sameScope(context, entry.getContext())) {
						entries.remove(index);
						forgetAudits.add(new ForgetAudit(context, checkedId, checkedReason, forgottenAtEpochMillis));
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public List<ForgetAudit> listForgetAudits(AskDataProjectContext context) {
			if (context == null) {
				throw new IllegalArgumentException("context 不能为空");
			}
			List<ForgetAudit> result = new ArrayList<ForgetAudit>();
			synchronized (forgetAudits) {
				for (ForgetAudit audit : forgetAudits) {
					if (sameScope(context, audit.getContext())) {
						result.add(audit);
					}
				}
			}
			return Collections.unmodifiableList(result);
		}

		private static boolean sameScope(AskDataProjectContext left, AskDataProjectContext right) {
			return left.getProjectId().equals(right.getProjectId())
					&& left.getOperatorId().equals(right.getOperatorId());
		}

		private static String requireNonBlank(String value, String name) {
			if (value == null || value.trim().length() == 0) {
				throw new IllegalArgumentException(name + " 不能为空白");
			}
			return value.trim();
		}
	}

	/** 已脱敏的长期记忆遗忘审计。 */
	final class ForgetAudit {
		private final AskDataProjectContext context;
		private final String memoryId;
		private final String reason;
		private final long forgottenAtEpochMillis;
		public ForgetAudit(AskDataProjectContext context, String memoryId, String reason,
				long forgottenAtEpochMillis) {
			this.context = context;
			this.memoryId = memoryId;
			this.reason = reason;
			this.forgottenAtEpochMillis = forgottenAtEpochMillis;
		}
		public AskDataProjectContext getContext() { return context; }
		public String getMemoryId() { return memoryId; }
		public String getReason() { return reason; }
		public long getForgottenAtEpochMillis() { return forgottenAtEpochMillis; }
	}
}
