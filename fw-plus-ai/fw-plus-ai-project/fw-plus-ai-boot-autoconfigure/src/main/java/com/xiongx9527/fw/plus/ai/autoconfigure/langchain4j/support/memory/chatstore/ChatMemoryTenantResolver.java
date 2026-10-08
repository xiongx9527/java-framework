package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore;

/**
 * ChatMemoryStore 租户解析器：为存储层提供当前租户 ID（{@code tenant_id} 维度）。
 *
 * <p>租户维度优先取 memId 段[0]（{@code {tenantId}:{appId}:{agentId}:{userId}} 四段契约），
 * 段数不足 4 时回退本解析器；使用方提供自己的 Bean（如从登录上下文/请求头解析）即自动
 * 覆盖自动配置注册的默认实现（默认返回配置项
 * {@code fw.plus.ai.langchain4j.chat-memory.default-tenant-id}）。</p>
 */
public interface ChatMemoryTenantResolver {

	/**
	 * 解析当前租户 ID，返回 {@code null} 时存储层使用默认租户。
	 */
	String resolveTenantId();
}
