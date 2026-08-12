package com.xiongx9527.fw.plus.ai.askdata.memory;

import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizedObject;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 带归属、授权要求、排序证据和审计来源的不可变推荐候选。
 *
 * <p>非通用候选必须绑定项目和操作人；通用候选只绑定项目，且不得包含个人偏好、反馈或长期记忆分值。
 * 候选声明的对象范围只用于收窄请求授权，绝不能作为新的授权来源。内容和审计摘要必须由实现方提前脱敏，
 * 禁止放入原始 SQL、模型凭据或未授权业务数据。</p>
 */
public final class AskDataRecommendationCandidate {

	private final String id;
	private final String content;
	private final String projectId;
	private final String operatorId;
	private final String datasourceKey;
	private final Set<AskDataAuthorizedObject> requiredObjects;
	private final String source;
	private final double relevanceScore;
	private final double preferenceScore;
	private final double feedbackScore;
	private final double longTermMemoryScore;
	private final boolean generic;
	private final String auditSummary;

	public AskDataRecommendationCandidate(String id, String content, String projectId, String operatorId,
			String datasourceKey, Set<AskDataAuthorizedObject> requiredObjects, String source,
			double relevanceScore, double preferenceScore, double feedbackScore, double longTermMemoryScore,
			boolean generic, String auditSummary) {
		this.id = requireNonBlank(id, "id");
		this.content = requireNonBlank(content, "content");
		this.projectId = requireNonBlank(projectId, "projectId");
		this.operatorId = generic ? requireNull(operatorId) : requireNonBlank(operatorId, "operatorId");
		this.datasourceKey = requireNonBlank(datasourceKey, "datasourceKey");
		this.requiredObjects = requiredObjects(requiredObjects, this.datasourceKey);
		this.source = requireNonBlank(source, "source");
		this.relevanceScore = finite(relevanceScore, "relevanceScore");
		this.preferenceScore = finite(preferenceScore, "preferenceScore");
		this.feedbackScore = finite(feedbackScore, "feedbackScore");
		this.longTermMemoryScore = finite(longTermMemoryScore, "longTermMemoryScore");
		this.generic = generic;
		if (generic && (preferenceScore != 0D || feedbackScore != 0D || longTermMemoryScore != 0D)) {
			throw new IllegalArgumentException("通用候选不得携带个人偏好、反馈或长期记忆分值");
		}
		this.auditSummary = requireNonBlank(auditSummary, "auditSummary");
	}

	public String getId() { return id; }
	public String getContent() { return content; }
	public String getProjectId() { return projectId; }
	public String getOperatorId() { return operatorId; }
	public String getDatasourceKey() { return datasourceKey; }
	public Set<AskDataAuthorizedObject> getRequiredObjects() { return requiredObjects; }
	public String getSource() { return source; }
	public double getRelevanceScore() { return relevanceScore; }
	public double getPreferenceScore() { return preferenceScore; }
	public double getFeedbackScore() { return feedbackScore; }
	public double getLongTermMemoryScore() { return longTermMemoryScore; }
	public boolean isGeneric() { return generic; }
	public String getAuditSummary() { return auditSummary; }

	/**
	 * 返回候选在完成权限过滤后的总排序分值。
	 *
	 * @return 相关性、当前操作人偏好、反馈和长期记忆证据之和
	 */
	public double getScore() {
		return relevanceScore + preferenceScore + feedbackScore + longTermMemoryScore;
	}

	private static Set<AskDataAuthorizedObject> requiredObjects(Set<AskDataAuthorizedObject> values,
			String datasourceKey) {
		if (values == null || values.isEmpty()) {
			throw new IllegalArgumentException("requiredObjects 不能为空");
		}
		Set<AskDataAuthorizedObject> copy = new LinkedHashSet<AskDataAuthorizedObject>();
		for (AskDataAuthorizedObject value : values) {
			if (value == null || !datasourceKey.equals(value.getDatasourceKey())) {
				throw new IllegalArgumentException("requiredObjects 必须属于候选 datasourceKey");
			}
			copy.add(value);
		}
		return Collections.unmodifiableSet(copy);
	}

	private static double finite(double value, String name) {
		if (Double.isNaN(value) || Double.isInfinite(value)) {
			throw new IllegalArgumentException(name + " 必须是有限数值");
		}
		return value;
	}

	private static String requireNull(String value) {
		if (value != null) {
			throw new IllegalArgumentException("通用候选的 operatorId 必须为空");
		}
		return null;
	}

	private static String requireNonBlank(String value, String name) {
		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException(name + " 不能为空白");
		}
		return value.trim();
	}
}
