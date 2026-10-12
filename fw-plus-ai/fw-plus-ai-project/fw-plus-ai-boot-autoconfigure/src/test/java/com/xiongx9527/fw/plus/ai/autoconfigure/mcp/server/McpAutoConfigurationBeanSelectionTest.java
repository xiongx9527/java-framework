package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server;

import com.xiongx9527.fw.plus.auth.core.AuthService;
import com.xiongx9527.fw.plus.auth.core.InstanceAuthRegistry;
import com.xiongx9527.fw.plus.auth.core.PermitAllAuthService;
import com.xiongx9527.fw.plus.auth.satoken.provider.SaTokenLoginAuthService;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.cluster.McpRedisBroadcaster;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.cluster.McpRedisMessageConsumer;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.cluster.RedisServerSseRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.cluster.RedisServerStreamableHttpRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.config.McpProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.runtime.McpServerRuntime;
import com.xiongx9527.fw.plus.ai.mcp.server.spi.ServerSseRegistry;
import com.xiongx9527.fw.plus.ai.mcp.server.spi.ServerStreamableHttpRegistry;
import com.xiongx9527.fw.plus.ai.mcp.server.support.DefaultServerSseRegistry;
import com.xiongx9527.fw.plus.ai.mcp.server.support.DefaultServerStreamableHttpRegistry;
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
            assertThat(runtimes.get(0).getAuthService()).isNull();
            assertThat(context.getBean(InstanceAuthRegistry.class)).isNotNull();
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
                    assertThat(context).doesNotHaveBean(AuthService.class);
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
    public void shouldResolveAuthServicesByProviderConfig() {
        contextRunner
                .withPropertyValues(
                        "fw-plus.ai.mcp.auth.provider=permit-all",
                        "fw-plus.ai.mcp.servers[0].key=weather",
                        "fw-plus.ai.mcp.servers[0].endpoint=/mcp/weather",
                        "fw-plus.ai.mcp.servers[0].auth.provider=sa-token-login")
                .run(context -> {
                    List<McpServerRuntime> runtimes = context.getBean("mcpServerRuntimes", List.class);
                    // default runtime 按顶层 provider 解析
                    assertThat(runtimes.get(0).getAuthService()).isInstanceOf(PermitAllAuthService.class);
                    // 命名 runtime 按自身 provider 解析
                    assertThat(runtimes.get(1).getAuthService()).isInstanceOf(SaTokenLoginAuthService.class);
                });
    }

    @Test
    public void shouldNotInheritTopLevelAuthForNamedRuntimeWithoutProvider() {
        contextRunner
                .withPropertyValues(
                        "fw-plus.ai.mcp.auth.provider=permit-all",
                        "fw-plus.ai.mcp.servers[0].key=weather",
                        "fw-plus.ai.mcp.servers[0].endpoint=/mcp/weather")
                .run(context -> {
                    List<McpServerRuntime> runtimes = context.getBean("mcpServerRuntimes", List.class);
                    assertThat(runtimes.get(0).getAuthService()).isInstanceOf(PermitAllAuthService.class);
                    assertThat(runtimes.get(1).getAuthService()).isNull();
                });
    }

    @Test
    public void shouldInheritScopeCheckSettingForNamedRuntime() {
        contextRunner
                .withPropertyValues(
                        "fw-plus.ai.mcp.scope-check-enabled=true",
                        "fw-plus.ai.mcp.servers[0].key=weather",
                        "fw-plus.ai.mcp.servers[0].endpoint=/mcp/weather")
                .run(context -> {
                    List<McpServerRuntime> runtimes = context.getBean("mcpServerRuntimes", List.class);
                    assertThat(runtimes).hasSize(2);
                    assertThat(runtimes.get(0).getProperties().isScopeCheckEnabled()).isTrue();
                    assertThat(runtimes.get(1).getProperties().isScopeCheckEnabled()).isTrue();
                });
    }

    @Test
    public void shouldNotFallBackToAnUnrelatedAuthServiceBean() {
        contextRunner
                .withBean("unrelated", AuthService.class, () -> mock(AuthService.class))
                .run(context -> {
                    List<McpServerRuntime> runtimes = context.getBean("mcpServerRuntimes", List.class);
                    assertThat(runtimes.get(0).getAuthService()).isNull();
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