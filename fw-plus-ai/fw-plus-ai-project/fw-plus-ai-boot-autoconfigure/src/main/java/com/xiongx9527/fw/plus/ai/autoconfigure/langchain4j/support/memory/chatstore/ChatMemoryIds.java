package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore;

/**
 * memoryId（memId）构建与解析工具：契约为四段拼接
 * {@code {tenantId}:{appId}:{agentId}:{userId}}，各段以冒号分隔。
 *
 * <p>调用方（如 {@code SimpleAssistant.dimensions(...)}）通过 {@link #build} 构建 memId，
 * 存储层按位解析出四个维度：段[0]=租户、段[1]=应用、段[2]=AI 主体、段[3]=用户。
 * 同一维度组合共用一个记忆集合（{@code run_id}=memId 原样精确匹配），
 * 历史记忆跨会话天然连续。</p>
 *
 * <p>段数不足 4 或某段为空时该维度回退默认值——此时会话隔离仍由完整
 * {@code run_id}（memId 原样精确匹配）保证，回退仅影响维度聚合视图。</p>
 */
public final class ChatMemoryIds {

	/** 维度分隔符 */
	public static final String SEPARATOR = ":";

	/** tenant_id 缺省值（与 SimpleAssistant 三参数入口一致） */
	public static final String DEFAULT_TENANT_ID = "0";

	/** app_id 段缺失时的默认值 */
	public static final String DEFAULT_APP_ID = "default";

	/** agent_id 段缺失时的默认值 */
	public static final String DEFAULT_AGENT_ID = "default";

	/** user_id 段缺失时的默认值 */
	public static final String DEFAULT_USER_ID = "default";

	private ChatMemoryIds() {
	}

	/**
	 * 构建四段 memId：{@code {tenantId}:{appId}:{agentId}:{userId}}。
	 *
	 * @throws IllegalArgumentException 任一段为空或包含分隔符 {@code :}
	 */
	public static String build(String tenantId, String appId, String agentId, String userId) {
		return requireSegment("tenantId", tenantId)
				+ SEPARATOR + requireSegment("appId", appId)
				+ SEPARATOR + requireSegment("agentId", agentId)
				+ SEPARATOR + requireSegment("userId", userId);
	}

	/**
	 * 构建四段 memId（tenant 缺省 {@value #DEFAULT_TENANT_ID}）：
	 * {@code 0:{appId}:{agentId}:{userId}}。
	 *
	 * @throws IllegalArgumentException 任一段为空或包含分隔符 {@code :}
	 */
	public static String build(String appId, String agentId, String userId) {
		return build(DEFAULT_TENANT_ID, appId, agentId, userId);
	}

	/** 解析段[0]（tenant_id）；段数不足 4 或该段为空时返回 {@code defaultTenantId}。 */
	public static String parseTenantId(String memoryId, String defaultTenantId) {
		return parseSegment(memoryId, 0, defaultTenantId);
	}

	/** 解析段[1]（app_id）；段数不足 4 或该段为空时返回 {@code defaultAppId}。 */
	public static String parseAppId(String memoryId, String defaultAppId) {
		return parseSegment(memoryId, 1, defaultAppId);
	}

	/** 解析段[2]（agent_id）；段数不足 4 或该段为空时返回 {@code defaultAgentId}。 */
	public static String parseAgentId(String memoryId, String defaultAgentId) {
		return parseSegment(memoryId, 2, defaultAgentId);
	}

	/** 解析段[3]（user_id）；段数不足 4 或该段为空时返回 {@code defaultUserId}。 */
	public static String parseUserId(String memoryId, String defaultUserId) {
		return parseSegment(memoryId, 3, defaultUserId);
	}

	private static String parseSegment(String memoryId, int index, String defaultValue) {
		if (memoryId == null) {
			return defaultValue;
		}
		String[] segments = memoryId.split(SEPARATOR, -1);
		if (segments.length < 4) {
			return defaultValue;
		}
		String segment = segments[index].trim();
		return segment.isEmpty() ? defaultValue : segment;
	}

	private static String requireSegment(String name, String value) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException("ChatMemoryIds.build: " + name + " 不能为空");
		}
		String trimmed = value.trim();
		if (trimmed.contains(SEPARATOR)) {
			throw new IllegalArgumentException(
					"ChatMemoryIds.build: " + name + " 不允许包含分隔符 '" + SEPARATOR + "': " + value);
		}
		return trimmed;
	}
}
