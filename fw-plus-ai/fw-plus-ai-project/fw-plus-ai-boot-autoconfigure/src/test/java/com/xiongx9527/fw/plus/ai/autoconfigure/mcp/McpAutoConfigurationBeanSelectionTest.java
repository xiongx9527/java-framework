package com.xiongx9527.fw.plus.ai.autoconfigure.mcp;

import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.cluster.RedisServerSseRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.cluster.RedisServerStreamableHttpRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.cluster.McpRedisBroadcaster;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.cluster.McpRedisMessageConsumer;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.web.McpSseController;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.web.McpStreamableHttpController;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class McpAutoConfigurationBeanSelectionTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(McpAutoConfiguration.class));

	@Test
	public void shouldCreateDefaultSseTransportBean() {
		contextRunner.run(context -> {
			assertThat(context.getBean(McpProperties.class).getTransport()).isEqualTo("http-sse");
			assertThat(context).hasSingleBean(McpSseController.class);
			assertThat(context).doesNotHaveBean(McpStreamableHttpController.class);
			assertThat(context).hasSingleBean(ServerSseRegistry.class);
			assertThat(context).hasSingleBean(DefaultServerSseRegistry.class);
			assertFalse(context.containsBeanDefinition("defaultServerStreamableHttpRegistry"));
		});
	}

	@Test
	public void shouldCreateStreamableHttpTransportBeanWhenEnabled() {
		contextRunner
				.withPropertyValues("fw-plus.ai.mcp.transport=streamable-http")
				.run(context -> {
					assertThat(context.getBean(McpProperties.class).getTransport()).isEqualTo("streamable-http");
					assertThat(context).hasSingleBean(McpStreamableHttpController.class);
					assertThat(context).doesNotHaveBean(McpSseController.class);
					assertThat(context).hasSingleBean(ServerStreamableHttpRegistry.class);
					assertThat(context).hasSingleBean(DefaultServerStreamableHttpRegistry.class);
					assertFalse(context.containsBeanDefinition("defaultServerSseRegistry"));
				});
	}

	@Test
	public void shouldCreateRedisClusterSseTransportBeanWhenRedisIsAvailable() {
		contextRunner
				.withBean(StringRedisTemplate.class, () -> stringRedisTemplate())
				.withBean(McpRedisBroadcaster.class, () -> new McpRedisBroadcaster(null, "server-1"))
				.withPropertyValues(
						"fw-plus.ai.mcp.transport=http-sse",
						"fw-plus.ai.mcp.server.name=server-1")
				.run(context -> {
					assertThat(context).hasSingleBean(ServerSseRegistry.class);
					assertThat(context).hasSingleBean(RedisServerSseRegistry.class);
					assertThat(context.getBean(ServerSseRegistry.class)).isSameAs(context.getBean(RedisServerSseRegistry.class));
					assertThat(context).hasSingleBean(McpRedisBroadcaster.class);
					assertThat(context).hasSingleBean(McpRedisMessageConsumer.class);
					assertThat(context).hasSingleBean(RedisMessageListenerContainer.class);
					assertFalse(context.containsBeanDefinition("defaultServerSseRegistry"));
				});
	}

	@Test
	public void shouldCreateRedisClusterStreamableHttpTransportBeanWhenRedisIsAvailable() {
		contextRunner
				.withBean(StringRedisTemplate.class, () -> stringRedisTemplate())
				.withBean(McpRedisBroadcaster.class, () -> new McpRedisBroadcaster(null, "server-2"))
				.withPropertyValues(
						"fw-plus.ai.mcp.transport=streamable-http",
						"fw-plus.ai.mcp.server.name=server-2")
				.run(context -> {
					assertThat(context).hasSingleBean(ServerStreamableHttpRegistry.class);
					assertThat(context).hasSingleBean(RedisServerStreamableHttpRegistry.class);
					assertThat(context.getBean(ServerStreamableHttpRegistry.class)).isSameAs(context.getBean(RedisServerStreamableHttpRegistry.class));
					assertThat(context).hasSingleBean(McpRedisBroadcaster.class);
					assertThat(context).hasSingleBean(McpRedisMessageConsumer.class);
					assertThat(context).hasSingleBean(RedisMessageListenerContainer.class);
					assertFalse(context.containsBeanDefinition("defaultServerStreamableHttpRegistry"));
				});
	}

	@Test
	public void shouldNotCreateAnyBeanWhenMcpIsDisabled() {
		contextRunner
				.withPropertyValues("fw-plus.ai.mcp.enabled=false")
				.run(context -> {
					assertThat(context).doesNotHaveBean(McpProperties.class);
					assertThat(context).doesNotHaveBean(McpServerInitializer.class);
					assertThat(context).doesNotHaveBean(McpSseController.class);
					assertThat(context).doesNotHaveBean(McpStreamableHttpController.class);
					assertThat(context).doesNotHaveBean(ServerSseRegistry.class);
					assertThat(context).doesNotHaveBean(ServerStreamableHttpRegistry.class);
				});
	}

	@Test
	public void shouldFallBackToLocalRegistryWhenRedisIsNotOnClasspath() {
		contextRunner
				.withClassLoader(new FilteredClassLoader(StringRedisTemplate.class))
				.withPropertyValues("fw-plus.ai.mcp.transport=http-sse")
				.run(context -> {
					assertThat(context).hasSingleBean(ServerSseRegistry.class);
					assertThat(context).hasSingleBean(DefaultServerSseRegistry.class);
					assertThat(context).doesNotHaveBean(McpRedisBroadcaster.class);
					assertThat(context).doesNotHaveBean(McpRedisMessageConsumer.class);
					assertThat(context).doesNotHaveBean(RedisMessageListenerContainer.class);
				});
	}

	@Test
	public void shouldCreateRedisClusterBeansWhenOnlyStringRedisTemplateIsAvailable() {
		contextRunner
				.withBean(StringRedisTemplate.class, () -> stringRedisTemplate())
				.withPropertyValues(
						"fw-plus.ai.mcp.transport=http-sse",
						"fw-plus.ai.mcp.server.name=server-3")
				.run(context -> {
					assertThat(context).hasSingleBean(McpRedisBroadcaster.class);
					assertThat(context).hasSingleBean(McpRedisMessageConsumer.class);
					assertThat(context).hasSingleBean(RedisMessageListenerContainer.class);
					assertThat(context).hasSingleBean(RedisServerSseRegistry.class);
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
