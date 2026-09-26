package com.xiongx9527.fw.plus.ai.autoconfigure.mcp;

import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.auth.McpAuthService;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.auth.PermitAllAuthService;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.auth.SaTokenOAuth2McpAuthService;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.cluster.McpRedisBroadcaster;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.cluster.McpRedisMessageConsumer;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.cluster.RedisServerSseRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.cluster.RedisServerStreamableHttpRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.runtime.McpServerRuntime;
import com.xiongx9527.fw.plus.ai.mcp.spi.ServerSseRegistry;
import com.xiongx9527.fw.plus.ai.mcp.spi.ServerStreamableHttpRegistry;
import com.xiongx9527.fw.plus.ai.mcp.support.DefaultServerSseRegistry;
import com.xiongx9527.fw.plus.ai.mcp.support.DefaultServerStreamableHttpRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.Subscription;
import org.springframework.data.redis.connection.SubscriptionListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class McpAutoConfigurationBeanSelectionTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(McpAutoConfiguration.class));

	@Test
	public void shouldCreateSingleDefaultRuntimeWithSseTransport() {
		contextRunner.run(context -> {
			assertThat(context.getBean(McpProperties.class).getTransport()).isEqualTo("http-sse");
			List<McpServerRuntime> runtimes = context.getBean("mcpServerRuntimes", List.class);
			assertThat(runtimes).hasSize(1);
			assertThat(runtimes.get(0).getKey()).isEqualTo("default");
			assertThat(runtimes.get(0).getRegistry()).isInstanceOf(DefaultServerSseRegistry.class);
			// 测试 classpath 含 optional 的 sa-token-oauth2，故默认落到 Sa-Token 版（三态首选）
			assertThat(context.getBean(McpAuthService.class)).isInstanceOf(SaTokenOAuth2McpAuthService.class);
		});
	}

	@Test
	public void shouldCreateStreamableHttpDefaultRuntimeWhenEnabled() {
		contextRunner
				.withPropertyValues("fw-plus.ai.mcp.transport=streamable-http")
				.run(context -> {
					assertThat(context.getBean(McpProperties.class).getTransport()).isEqualTo("streamable-http");
					List<McpServerRuntime> runtimes = context.getBean("mcpServerRuntimes", List.class);
					assertThat(runtimes).hasSize(1);
					assertThat(runtimes.get(0).getRegistry()).isInstanceOf(DefaultServerStreamableHttpRegistry.class);
				});
	}

	@Test
	public void shouldCreateNamedRuntimesAfterTheDefault() {
		contextRunner
				.withPropertyValues(
						"fw-plus.ai.mcp.servers[0].key=weather",
						"fw-plus.ai.mcp.servers[0].endpoint=/mcp/weather",
						"fw-plus.ai.mcp.servers[1].key=order",
						"fw-plus.ai.mcp.servers[1].endpoint=/mcp/order")
				.run(context -> {
					List<McpServerRuntime> runtimes = context.getBean("mcpServerRuntimes", List.class);
					assertThat(runtimes).hasSize(3);
					assertThat(runtimes.get(0).getKey()).isEqualTo("default");
					assertThat(runtimes.get(1).getKey()).isEqualTo("weather");
					assertThat(runtimes.get(2).getKey()).isEqualTo("order");
					assertThat(runtimes.get(1).getEndpoint()).isEqualTo("/mcp/weather");
					assertThat(runtimes.get(2).getEndpoint()).isEqualTo("/mcp/order");
				});
	}

	@Test
	public void shouldInheritTopLevelTransportForNamedRuntimeAndAllowOverride() {
		contextRunner
				.withPropertyValues(
						"fw-plus.ai.mcp.transport=streamable-http",
						"fw-plus.ai.mcp.servers[0].key=weather",
						"fw-plus.ai.mcp.servers[0].endpoint=/mcp/weather",
						"fw-plus.ai.mcp.servers[1].key=order",
						"fw-plus.ai.mcp.servers[1].endpoint=/mcp/order",
						"fw-plus.ai.mcp.servers[1].transport=http-sse")
				.run(context -> {
					List<McpServerRuntime> runtimes = context.getBean("mcpServerRuntimes", List.class);
					assertThat(runtimes).hasSize(3);
					// weather 未配置 transport，继承顶层 streamable-http
					assertThat(runtimes.get(1).getTransport()).isEqualTo("streamable-http");
					assertThat(runtimes.get(1).getRegistry()).isInstanceOf(DefaultServerStreamableHttpRegistry.class);
					// order 显式覆盖为 http-sse
					assertThat(runtimes.get(2).getTransport()).isEqualTo("http-sse");
					assertThat(runtimes.get(2).getRegistry()).isInstanceOf(DefaultServerSseRegistry.class);
				});
	}

	@Test
	public void shouldUseRedisRegistryWhenRedisIsAvailable() {
		contextRunner
				.withBean(StringRedisTemplate.class, () -> stringRedisTemplate())
				.withBean(McpRedisBroadcaster.class, () -> new McpRedisBroadcaster(null, "server-1"))
				.withPropertyValues(
						"fw-plus.ai.mcp.transport=http-sse",
						"fw-plus.ai.mcp.server.name=server-1")
				.run(context -> {
					List<McpServerRuntime> runtimes = context.getBean("mcpServerRuntimes", List.class);
					assertThat(runtimes.get(0).getRegistry()).isInstanceOf(RedisServerSseRegistry.class);
					assertThat(context).hasSingleBean(McpRedisBroadcaster.class);
					assertThat(context).hasSingleBean(McpRedisMessageConsumer.class);
					assertThat(context).hasSingleBean(RedisMessageListenerContainer.class);
				});
	}

	@Test
	public void shouldUseRedisStreamableHttpRegistryWhenRedisIsAvailable() {
		contextRunner
				.withBean(StringRedisTemplate.class, () -> stringRedisTemplate())
				.withBean(McpRedisBroadcaster.class, () -> new McpRedisBroadcaster(null, "server-2"))
				.withPropertyValues(
						"fw-plus.ai.mcp.transport=streamable-http",
						"fw-plus.ai.mcp.server.name=server-2")
				.run(context -> {
					List<McpServerRuntime> runtimes = context.getBean("mcpServerRuntimes", List.class);
					assertThat(runtimes.get(0).getRegistry()).isInstanceOf(RedisServerStreamableHttpRegistry.class);
					assertThat(context).hasSingleBean(McpRedisBroadcaster.class);
					assertThat(context).hasSingleBean(McpRedisMessageConsumer.class);
					assertThat(context).hasSingleBean(RedisMessageListenerContainer.class);
				});
	}

	@Test
	public void shouldNotCreateAnyBeanWhenMcpIsDisabled() {
		contextRunner
				.withPropertyValues("fw-plus.ai.mcp.enabled=false")
				.run(context -> {
					assertThat(context).doesNotHaveBean(McpProperties.class);
					assertThat(context).doesNotHaveBean(McpServerInitializer.class);
					assertThat(context).doesNotHaveBean(ServerSseRegistry.class);
					assertThat(context).doesNotHaveBean(ServerStreamableHttpRegistry.class);
					assertThat(context).doesNotHaveBean(McpAuthService.class);
				});
	}

	@Test
	public void shouldFallBackToLocalRegistryWhenRedisIsNotOnClasspath() {
		contextRunner
				.withClassLoader(new FilteredClassLoader(StringRedisTemplate.class))
				.withPropertyValues("fw-plus.ai.mcp.transport=http-sse")
				.run(context -> {
					List<McpServerRuntime> runtimes = context.getBean("mcpServerRuntimes", List.class);
					assertThat(runtimes.get(0).getRegistry()).isInstanceOf(DefaultServerSseRegistry.class);
					assertThat(context).doesNotHaveBean(McpRedisBroadcaster.class);
					assertThat(context).doesNotHaveBean(McpRedisMessageConsumer.class);
					assertThat(context).doesNotHaveBean(RedisMessageListenerContainer.class);
				});
	}

	@Test
	public void shouldUseRedisRegistryWhenOnlyRedisTemplateIsAvailable() {
		contextRunner
				.withBean(StringRedisTemplate.class, () -> stringRedisTemplate())
				.withPropertyValues(
						"fw-plus.ai.mcp.transport=http-sse",
						"fw-plus.ai.mcp.server.name=server-3")
				.run(context -> {
					List<McpServerRuntime> runtimes = context.getBean("mcpServerRuntimes", List.class);
					assertThat(runtimes.get(0).getRegistry()).isInstanceOf(RedisServerSseRegistry.class);
					assertThat(context).hasSingleBean(McpRedisBroadcaster.class);
					assertThat(context).hasSingleBean(McpRedisMessageConsumer.class);
					assertThat(context).hasSingleBean(RedisMessageListenerContainer.class);
				});
	}

	@Test
	public void shouldUseUserProvidedAuthService() {
		contextRunner
				.withBean(McpAuthService.class, PermitAllAuthService::new)
				.run(context -> {
					assertThat(context.getBean(McpAuthService.class)).isInstanceOf(PermitAllAuthService.class);
				});
	}

	@Test
	public void shouldUseSaTokenAuthWhenSaTokenOAuth2OnClasspath() {
		contextRunner.run(context -> {
			McpAuthService authService = context.getBean(McpAuthService.class);
			assertThat(authService).isInstanceOf(SaTokenOAuth2McpAuthService.class);
			// 单一 bean：不能同时存在 Sa-Token 版与放行版
			assertThat(context.getBeanNamesForType(McpAuthService.class)).hasSize(1);
		});
	}

	private StringRedisTemplate stringRedisTemplate() {
		RedisConnectionFactory connectionFactory = mockConnectionFactory();
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
		return redisTemplate;
	}

	private static RedisConnectionFactory mockConnectionFactory() {
		RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
		RedisConnection connection = mock(RedisConnection.class);
		when(connectionFactory.getConnection()).thenReturn(connection);
		when(connection.getSubscription()).thenReturn(mock(Subscription.class));
		// 本地无 Redis，同步回调订阅成功，使监听容器的注册流程正常走完
		doAnswer(invocation -> {
			SubscriptionListener listener = invocation.getArgument(0);
			Object[] arguments = invocation.getArguments();
			for (int i = 1; i < arguments.length; i++) {
				byte[] pattern = (byte[]) arguments[i];
				listener.onPatternSubscribed(pattern, 1);
			}
			return null;
		}).when(connection).pSubscribe(any(MessageListener.class), any());
		return connectionFactory;
	}
}
