package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.starter;

import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.cluster.McpRedisBroadcaster;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.notify.MagicMcpClusterBroadcaster;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.notify.MagicMcpClusterChangeListener;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.notify.MagicMcpNotifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 插件侧集群装配：仅在 spring-data-redis 就绪且 starter 广播器存在时生效。
 *
 * <p>类级 {@link ConditionalOnClass} 与 starter 的 Redis 装配同手法：spring-data-redis 是
 * optional 依赖，缺失时本类不加载，避免内省 Redis 类型触发 {@code NoClassDefFoundError}。
 * 无 Redis 时能力变更只在本节点生效（单节点降级）。</p>
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
@ConditionalOnBean(McpRedisBroadcaster.class)
public class MagicMcpClusterConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public MagicMcpClusterBroadcaster magicMcpClusterBroadcaster(McpRedisBroadcaster broadcaster) {
		return new MagicMcpClusterBroadcaster(broadcaster);
	}

	@Bean
	@ConditionalOnMissingBean
	public MagicMcpClusterChangeListener magicMcpClusterChangeListener(McpRedisBroadcaster broadcaster,
			MagicMcpNotifier notifier) {
		return new MagicMcpClusterChangeListener(broadcaster.getServerId(), notifier);
	}

	@Bean
	@ConditionalOnMissingBean(name = "magicMcpClusterListenerContainer")
	public RedisMessageListenerContainer magicMcpClusterListenerContainer(StringRedisTemplate redisTemplate,
			MagicMcpClusterChangeListener listener) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(redisTemplate.getConnectionFactory());
		container.addMessageListener(listener, new PatternTopic(McpRedisBroadcaster.CHANNEL));
		return container;
	}
}
