package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.session;

import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpInstanceProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties;
import com.xiongx9527.fw.plus.ai.mcp.transport.session.McpSessionStore;
import com.xiongx9527.fw.plus.ai.mcp.transport.session.InMemoryMcpSessionStore;
import com.xiongx9527.fw.plus.ai.mcp.transport.session.McpSessionStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * MCP 会话存储自动配置。
 *
 * <p>条件装配策略：
 * <ul>
 *   <li>Redis 在 classpath 且有连接时：装配 {@link RedisMcpSessionStore}；</li>
 *   <li>否则：装配 {@link InMemoryMcpSessionStore}（默认）。</li>
 * </ul>
 *
 * @author xiongxiao
 * @date 2026-09-22
 */
@Configuration(proxyBeanMethods = false)
public class McpSessionAutoConfiguration {

	/**
	 * Redis 会话存储（仅 Redis 在 classpath 时激活）
	 */
	@Bean
	@ConditionalOnClass(RedisTemplate.class)
	@ConditionalOnMissingBean(McpSessionStore.class)
	public McpSessionStore redisMcpSessionStore(
			RedisTemplate<String, String> redisTemplate,
			McpProperties properties,
			com.fasterxml.jackson.databind.ObjectMapper objectMapper) {

		long timeout = 1800L;
		if (properties.getInstances() != null && !properties.getInstances().isEmpty()) {
			McpInstanceProperties first = properties.getInstances().get(0);
			timeout = first.getSessionTimeoutSeconds();
		}
		return new RedisMcpSessionStore(redisTemplate, objectMapper, timeout);
	}

	/**
	 * 内存会话存储（兜底：Redis 未提供时使用）
	 */
	@Bean
	@ConditionalOnMissingBean(McpSessionStore.class)
	public McpSessionStore inMemoryMcpSessionStore(McpProperties properties) {
		long timeout = 1800L;
		if (properties.getInstances() != null && !properties.getInstances().isEmpty()) {
			McpInstanceProperties first = properties.getInstances().get(0);
			timeout = first.getSessionTimeoutSeconds();
		}
		return new InMemoryMcpSessionStore(timeout);
	}
}
