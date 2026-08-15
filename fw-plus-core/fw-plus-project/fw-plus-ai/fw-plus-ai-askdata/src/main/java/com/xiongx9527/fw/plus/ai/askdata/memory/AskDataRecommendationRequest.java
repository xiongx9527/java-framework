package com.xiongx9527.fw.plus.ai.askdata.memory;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizedObject;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 当前操作人的推荐请求及其授权边界。
 *
 * <p>调用方必须先通过业务治理服务取得当前项目、已认证操作人、数据源和授权对象，再创建本对象。推荐实现
 * 只能在这些边界内过滤候选；不得自行扩大对象集合、猜测操作人、跨数据源召回或把授权版本缺失解释为全量
 * 可见。对象不可变，可在并发推荐计算中安全共享，但其中的授权结果只适用于本次业务请求。</p>
 */
public final class AskDataRecommendationRequest {

	private final AskDataProjectContext context;
	private final String datasourceKey;
	private final Set<AskDataAuthorizedObject> authorizedObjects;
	private final String authorizationVersion;
	private final String authorizationSource;
	private final boolean allowGeneric;
	private final int limit;

	/**
	 * 创建已经绑定治理授权的推荐请求。
	 *
	 * @param context 当前项目和已认证操作人，不能为空
	 * @param datasourceKey 当前数据源稳定 key，不能为空白
	 * @param authorizedObjects 当前操作人可用于推荐的对象集合，不能为空且不能包含其他数据源对象
	 * @param authorizationVersion 业务授权快照版本，不能为空白
	 * @param authorizationSource 授权来源的脱敏标识，不能为空白
	 * @param allowGeneric 是否允许同项目、同数据源且不依赖个人行为的通用候选
	 * @param limit 最大返回数量，必须大于零
	 */
	public AskDataRecommendationRequest(AskDataProjectContext context, String datasourceKey,
			Set<AskDataAuthorizedObject> authorizedObjects, String authorizationVersion, String authorizationSource,
			boolean allowGeneric, int limit) {
		if (context == null || limit <= 0) {
			throw new IllegalArgumentException("context 和 limit 必须有效");
		}
		this.context = context;
		this.datasourceKey = requireNonBlank(datasourceKey, "datasourceKey");
		this.authorizedObjects = authorizedObjects(authorizedObjects, this.datasourceKey);
		this.authorizationVersion = requireNonBlank(authorizationVersion, "authorizationVersion");
		this.authorizationSource = requireNonBlank(authorizationSource, "authorizationSource");
		this.allowGeneric = allowGeneric;
		this.limit = limit;
	}

	public AskDataProjectContext getContext() {
		return context;
	}

	public String getDatasourceKey() {
		return datasourceKey;
	}

	public Set<AskDataAuthorizedObject> getAuthorizedObjects() {
		return authorizedObjects;
	}

	public String getAuthorizationVersion() {
		return authorizationVersion;
	}

	public String getAuthorizationSource() {
		return authorizationSource;
	}

	public boolean isAllowGeneric() {
		return allowGeneric;
	}

	public int getLimit() {
		return limit;
	}

	/**
	 * 判断候选是否完全位于当前请求的治理边界内。
	 *
	 * <p>该检查必须先于任何偏好、反馈或长期记忆分值计算。返回 {@code false} 的候选不得进入排序器，也不得
	 * 出现在推荐审计之外的模型上下文中。</p>
	 */
	public boolean allows(AskDataRecommendationCandidate candidate) {
		if (candidate == null || !context.getProjectId().equals(candidate.getProjectId())
				|| !datasourceKey.equals(candidate.getDatasourceKey())) {
			return false;
		}
		if (candidate.isGeneric()) {
			if (!allowGeneric || candidate.getOperatorId() != null) {
				return false;
			}
		} else if (!context.getOperatorId().equals(candidate.getOperatorId())) {
			return false;
		}
		return authorizedObjects.containsAll(candidate.getRequiredObjects());
	}

	private static Set<AskDataAuthorizedObject> authorizedObjects(Set<AskDataAuthorizedObject> values,
			String datasourceKey) {
		if (values == null || values.isEmpty()) {
			throw new IllegalArgumentException("authorizedObjects 不能为空");
		}
		Set<AskDataAuthorizedObject> copy = new LinkedHashSet<AskDataAuthorizedObject>();
		for (AskDataAuthorizedObject value : values) {
			if (value == null || !datasourceKey.equals(value.getDatasourceKey())) {
				throw new IllegalArgumentException("authorizedObjects 必须属于当前 datasourceKey");
			}
			copy.add(value);
		}
		return Collections.unmodifiableSet(copy);
	}

	private static String requireNonBlank(String value, String name) {
		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException(name + " 不能为空白");
		}
		return value.trim();
	}
}
