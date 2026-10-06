package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server;

import com.xiongx9527.fw.plus.auth.core.InstanceAuthRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.cluster.McpRedisBroadcaster;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.cluster.McpRedisMessageConsumer;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.config.McpProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.runtime.McpRuntimeFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.runtime.McpServerFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.runtime.McpServerRuntime;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.web.webmvc.McpRoutingRegistrar;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.web.webflux.McpWebfluxWebBinding;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.List;

/**
 * MCP Server 自动配置。
 * <p>
 * 通过 {@code fw-plus.ai.mcp.enabled} 控制整体开关；顶层配置即 default runtime（key={@code default}），
 * {@code servers[]} 追加命名 runtime，全部由 {@link McpRuntimeFactory} 构建。
 * 传输适配按 Web 栈分流：Servlet 栈导入 {@link McpRoutingRegistrar}，响应式栈导入
 * {@link McpWebfluxWebBinding}，两者互斥。集群 Bean 依赖 Redis 自动配置，
 * 因此声明在其后加载。
 * </p>
 */
@Configuration
@ConditionalOnClass(name = "com.xiongx9527.fw.plus.ai.mcp.server.feature.McpFeatureScanner")
@EnableConfigurationProperties(McpProperties.class)
@ConditionalOnProperty(prefix = "fw-plus.ai.mcp", name = "enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter(name = "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration")
@Import({ McpRoutingRegistrar.class, McpWebfluxWebBinding.class })
public class McpAutoConfiguration {

	/**
	 * 按实例 provider 解析认证服务。未配置 provider 的 runtime 保持未认证并拒绝请求，
	 * 不回退到全局或放行服务；provider 对应模块缺失时 fail-closed 拒绝请求。
	 */
	@Bean
	@ConditionalOnMissingBean
	public InstanceAuthRegistry instanceAuthRegistry() {
		return new InstanceAuthRegistry();
	}

	/**
	 * runtime 构建工厂：插件（如 magic-api MCP 插件）可复用于运行期动态新增 server 实例。
	 */
	@Bean
	public McpRuntimeFactory mcpRuntimeFactory(ObjectProvider<McpServerFactory> serverFactoryProvider) {
		McpServerFactory serverFactory = serverFactoryProvider.getIfAvailable();
		return new McpRuntimeFactory(serverFactory);
	}

	/**
	 * 构建全部 runtime：default（顶层配置）+ 命名 servers；endpoint 唯一性在此校验。
	 */
	@Bean
	public List<McpServerRuntime> mcpServerRuntimes(McpProperties properties,
			InstanceAuthRegistry authRegistry,
			ObjectProvider<McpRedisBroadcaster> broadcasterProvider, McpRuntimeFactory runtimeFactory) {
		return runtimeFactory.createRuntimes(properties, authRegistry, broadcasterProvider);
	}

	@Bean
	public McpServerInitializer mcpServerInitializer(List<McpServerRuntime> runtimes, McpProperties properties,
			ApplicationContext context) {
		return new McpServerInitializer(runtimes, properties, context);
	}

	/**
	 * Redis 集群装配。
	 * <p>
	 * 单独成类并挂类级 {@link ConditionalOnClass}：spring-data-redis 是 optional 依赖，
	 * 缺失时本类不会被加载，避免 {@code @Bean} 方法签名里的 Redis 类型在内省阶段触发
	 * {@link NoClassDefFoundError}。类级 {@link ConditionalOnBean} 对应原广播器方法上的条件，
	 * 即 Redis 自动配置就绪后才装配集群 Bean。
	 * </p>
	 */
	@Configuration
	@ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
	@ConditionalOnBean(type = "org.springframework.data.redis.core.StringRedisTemplate")
	static class RedisClusterConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public McpRedisBroadcaster mcpRedisBroadcaster(StringRedisTemplate redisTemplate) {
			return new McpRedisBroadcaster(redisTemplate);
		}

		@Bean
		@ConditionalOnMissingBean
		public McpRedisMessageConsumer mcpRedisMessageConsumer(List<McpServerRuntime> runtimes,
				McpRedisBroadcaster broadcaster) {
			return new McpRedisMessageConsumer(runtimes, broadcaster);
		}

		@Bean
		@ConditionalOnMissingBean
		public RedisMessageListenerContainer mcpRedisMessageListenerContainer(StringRedisTemplate redisTemplate,
				McpRedisMessageConsumer consumer) {
			RedisMessageListenerContainer container = new RedisMessageListenerContainer();
			container.setConnectionFactory(redisTemplate.getConnectionFactory());
			container.addMessageListener(consumer, new PatternTopic(McpRedisBroadcaster.CHANNEL));
			return container;
		}
	}
}
