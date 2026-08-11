package com.xiongx9527.fw.plus.ai.askdata.memory;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * AskData 的不可变长期记忆条目。
 *
 * <p>每条记忆固定绑定项目、认证操作人、数据源、语义版本和授权对象范围。正文与 SQL 只能保存经过业务确认和
 * 脱敏后的摘要；原始 Prompt、参数值、凭据及 SQL 结果不得写入本对象。保留策略和过期时间由业务决定。</p>
 */
public final class AskDataMemoryEntry {

	private final String id;
	private final AskDataProjectContext context;
	private final String datasourceKey;
	private final String semanticVersion;
	private final String content;
	private final String sqlSummary;
	private final Set<String> authorizationScope;
	private final String source;
	private final double confidence;
	private final String retentionPolicy;
	private final long createdAtEpochMillis;
	private final long expiresAtEpochMillis;

	/**
	 * 兼容创建永久保留、未限定授权对象的最小条目。
	 *
	 * <p>该构造器适用于迁移和单元测试。生产写入应使用完整构造器明确授权范围、来源、置信度和保留策略。</p>
	 */
	public AskDataMemoryEntry(AskDataProjectContext context, String datasourceKey, String semanticVersion,
			String content) {
		this(UUID.randomUUID().toString(), context, datasourceKey, semanticVersion, content, null,
				Collections.<String>emptySet(), "legacy", 0D, "business-managed", System.currentTimeMillis(), 0L);
	}

	public AskDataMemoryEntry(String id, AskDataProjectContext context, String datasourceKey, String semanticVersion,
			String content, String sqlSummary, Set<String> authorizationScope, String source, double confidence,
			String retentionPolicy, long createdAtEpochMillis, long expiresAtEpochMillis) {
		if (context == null) {
			throw new IllegalArgumentException("context 不能为空");
		}
		if (confidence < 0D || confidence > 1D) {
			throw new IllegalArgumentException("confidence 必须位于 0 到 1");
		}
		if (createdAtEpochMillis < 1L || expiresAtEpochMillis < 0L
				|| expiresAtEpochMillis > 0L && expiresAtEpochMillis <= createdAtEpochMillis) {
			throw new IllegalArgumentException("记忆保留时间非法");
		}
		this.id = requireNonBlank(id, "id");
		this.context = context;
		this.datasourceKey = requireNonBlank(datasourceKey, "datasourceKey");
		this.semanticVersion = requireNonBlank(semanticVersion, "semanticVersion");
		this.content = requireNonBlank(content, "content");
		this.sqlSummary = normalize(sqlSummary);
		this.authorizationScope = authorizationScope == null ? Collections.<String>emptySet()
				: Collections.unmodifiableSet(new LinkedHashSet<String>(authorizationScope));
		this.source = requireNonBlank(source, "source");
		this.confidence = confidence;
		this.retentionPolicy = requireNonBlank(retentionPolicy, "retentionPolicy");
		this.createdAtEpochMillis = createdAtEpochMillis;
		this.expiresAtEpochMillis = expiresAtEpochMillis;
	}

	public String getId() { return id; }
	public AskDataProjectContext getContext() { return context; }
	public String getDatasourceKey() { return datasourceKey; }
	public String getSemanticVersion() { return semanticVersion; }
	public String getContent() { return content; }
	public String getSqlSummary() { return sqlSummary; }
	public Set<String> getAuthorizationScope() { return authorizationScope; }
	public String getSource() { return source; }
	public double getConfidence() { return confidence; }
	public String getRetentionPolicy() { return retentionPolicy; }
	public long getCreatedAtEpochMillis() { return createdAtEpochMillis; }
	public long getExpiresAtEpochMillis() { return expiresAtEpochMillis; }
	public boolean isExpired(long nowEpochMillis) {
		return expiresAtEpochMillis > 0L && expiresAtEpochMillis <= nowEpochMillis;
	}

	private static String requireNonBlank(String value, String name) {
		String normalized = normalize(value);
		if (normalized == null) {
			throw new IllegalArgumentException(name + " 不能为空白");
		}
		return normalized;
	}

	private static String normalize(String value) {
		return value == null || value.trim().length() == 0 ? null : value.trim();
	}
}
