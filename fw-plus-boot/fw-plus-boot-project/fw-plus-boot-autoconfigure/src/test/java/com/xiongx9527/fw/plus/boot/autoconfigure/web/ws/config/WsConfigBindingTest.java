package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link WsConfig} 绑定契约。
 *
 * <p>读取侧统一按点号扁平键取配置，一旦 POJO 字段名与 {@code fw.plus.ws.*} 键名不匹配，
 * 绑定会静默回落默认值而不报错。这里锁定字段名与键名的对应关系。</p>
 */
class WsConfigBindingTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(BindingConfiguration.class);

	@Test
	void defaultsShouldMatchFrameworkDefaults() {
		runner.run(context -> {
			WsConfig config = context.getBean(WsConfig.class);
			assertThat(config.isEnabled()).isTrue();
			assertThat(config.getAppId()).isEqualTo("default");
			assertThat(config.getPath()).isEqualTo("/ws/api");
			assertThat(config.getPaths()).isEmpty();
			assertThat(config.getWebsocket().getTrustedDomain()).isEmpty();
			assertThat(config.getWebsocket().getSubProtocols()).isEmpty();
			assertThat(config.getWebsocket().isSupportsPartialMessages()).isFalse();
			assertThat(config.getSession().getCache().getMaxClients()).isEqualTo(10000);
			assertThat(config.getSession().getCache().getExpireMinutes()).isEqualTo(30);
			assertThat(config.getSession().getCache().getConcurrencyLevel()).isEqualTo(16);
			assertThat(config.getSession().getDelayMillis()).isEqualTo(1000);
			assertThat(config.getSession().getMaxTextMessageSize()).isEqualTo(65536);
			assertThat(config.getSession().getMaxBinaryMessageSize()).isEqualTo(65536);
			assertThat(config.getSession().getMaxTotalSessions()).isEqualTo(10000);
			assertThat(config.getSession().getMaxSessionsPerClient()).isEqualTo(100);
			assertThat(config.getSession().isIdleScanEnabled()).isFalse();
			assertThat(config.getSession().getIdleScanIntervalSeconds()).isEqualTo(60);
			assertThat(config.getSession().isCloseOnCacheEviction()).isFalse();
			assertThat(config.getScheduler().getDelayedClosePoolSize()).isEqualTo(2);
			assertThat(config.getRedis().getChannel().getBroadcast()).isEqualTo("fw-plus:websocket:broadcast");
			assertThat(config.getParam().getClientId()).isEqualTo("clientId");
			assertThat(config.getParam().getMaxClientIdLength()).isEqualTo(50);
			assertThat(config.getServers()).isEmpty();
			assertThat(config.getInit().getType()).isEqualTo("init");
			assertThat(config.getClose().getType()).isEqualTo("close");
			assertThat(config.getBinary().isEnabled()).isTrue();
			assertThat(config.getBinary().getType()).isEqualTo("binary");
			assertThat(config.getBroadcast().isAsync()).isFalse();
			assertThat(config.getWebflux().getScriptThreadPool().getCoreSize()).isEqualTo(8);
			assertThat(config.getWebflux().getScriptThreadPool().getMaxSize()).isEqualTo(32);
			assertThat(config.getWebflux().getScriptThreadPool().getQueueCapacity()).isEqualTo(1000);
		});
	}

	@Test
	void nestedPropertiesShouldBind() {
		runner.withPropertyValues(
				"fw.plus.ws.appId=portal",
				"fw.plus.ws.path=/custom/ws",
				"fw.plus.ws.session.cache.expireMinutes=5",
				"fw.plus.ws.redis.channel.broadcast=custom:channel",
				"fw.plus.ws.binary.enabled=false",
				"fw.plus.ws.broadcast.async=true").run(context -> {
				WsConfig config = context.getBean(WsConfig.class);
				assertThat(config.getAppId()).isEqualTo("portal");
				assertThat(config.getPath()).isEqualTo("/custom/ws");
			assertThat(config.getSession().getCache().getExpireMinutes()).isEqualTo(5);
			assertThat(config.getRedis().getChannel().getBroadcast()).isEqualTo("custom:channel");
			assertThat(config.getBinary().isEnabled()).isFalse();
			assertThat(config.getBroadcast().isAsync()).isTrue();
		});
	}

	@Test
	void additionalPathsShouldBind() {
		runner.withPropertyValues(
					"fw.plus.ws.paths=/ws/api2, /ws/api3").run(context -> {
			WsConfig config = context.getBean(WsConfig.class);
			assertThat(config.getPaths()).containsExactly("/ws/api2", "/ws/api3");
			assertThat(config.flatten().get("paths"))
					.isEqualTo(config.getPaths());
		});
	}

	@Test
	void explicitEmptyPathShouldBindAsEmptyString() {
		runner.withPropertyValues(
				"fw.plus.ws.path=").run(context -> {
			WsConfig config = context.getBean(WsConfig.class);
			// 空串是合法值，表示「只按基础路径注册」，不得回落内置默认
			assertThat(config.getPath()).isEmpty();
			assertThat((String) config.flatten().get("path")).isEmpty();
		});
	}

	@Test
	void legacyWebsocketEndpointKeysShouldNotBind() {
		runner.withPropertyValues(
				"fw.plus.ws.websocket.path=/legacy/ws",
				"fw.plus.ws.websocket.paths=/legacy/api2",
				"fw.plus.ws.websocket.endpoint=/ws/execute",
				"fw.plus.ws.websocket.basePath=/legacy/base").run(context -> {
			WsConfig config = context.getBean(WsConfig.class);
			// 旧键已移除，一律按 Spring 默认忽略；端点只认顶层 path / paths
			assertThat(config.getPath()).isEqualTo("/ws/api");
			assertThat(config.getPaths()).isEmpty();
			assertThat(config.getWebsocket().getTrustedDomain()).isEmpty();
			Map<String, Object> flat = config.flatten();
			assertThat(flat).doesNotContainKeys(
					"websocket.path", "websocket.paths", "websocket.endpoint", "websocket.basePath");
		});
	}

	@Test
	void serversShouldBind() {
		runner.withPropertyValues(
				"fw.plus.ws.servers[0].key=report",
				"fw.plus.ws.servers[0].path=/ws/api2",
				"fw.plus.ws.servers[0].paths=/ws/api3,/ws/api4",
				"fw.plus.ws.servers[0].appId=report",
				"fw.plus.ws.servers[1].key=trade",
				"fw.plus.ws.servers[1].path=/ws/api5").run(context -> {
				WsConfig config = context.getBean(WsConfig.class);
				assertThat(config.getServers()).hasSize(2);

				WsConfig.WsInstance first = config.getServers().get(0);
				assertThat(first.getKey()).isEqualTo("report");
				assertThat(first.getPath()).isEqualTo("/ws/api2");
				assertThat(first.getPaths()).containsExactly("/ws/api3", "/ws/api4");
				assertThat(first.getAppId()).isEqualTo("report");

				WsConfig.WsInstance second = config.getServers().get(1);
				assertThat(second.getKey()).isEqualTo("trade");
				assertThat(second.getPath()).isEqualTo("/ws/api5");
				// 未填写 appId 时保持空串，解析侧据此回落到全局 fw.plus.ws.appId
				assertThat(second.getAppId()).isEmpty();
			});
	}

	@Test
	void serversShouldNotEnterFlattenedKeys() {
		runner.withPropertyValues(
				"fw.plus.ws.servers[0].key=report",
				"fw.plus.ws.servers[0].path=/ws/api2",
				"fw.plus.ws.servers[0].appId=report").run(context -> {
				Map<String, Object> flat = context.getBean(WsConfig.class).flatten();
				assertThat(flat).doesNotContainKeys("servers", "servers.key", "servers.appId");
			});
	}

	@Test
	void websocketShouldOnlyCarryHandshakeParameters() {
		runner.run(context -> {
			WsConfig config = context.getBean(WsConfig.class);
			String websocketFields = Arrays.stream(WsConfig.Websocket.class.getDeclaredFields())
					.map(Field::getName)
					.sorted()
					.collect(Collectors.joining(","));
			assertThat(websocketFields).isEqualTo("subProtocols,supportsPartialMessages,trustedDomain");
			assertThat(WsConfig.class.getDeclaredFields())
					.extracting(Field::getName)
					.contains("path", "paths");
		});
	}

	@Test
	void legacyInstancesKeyShouldNotBind() {
		runner.withPropertyValues(
				"fw.plus.ws.instances[0].name=report",
				"fw.plus.ws.instances[0].path=/ws/api2",
				"fw.plus.ws.instances[0].appId=report").run(context -> {
				WsConfig config = context.getBean(WsConfig.class);
				assertThat(config.getServers()).isEmpty();
				assertThat(config.flatten()).doesNotContainKeys("instances", "instances.name", "instances.appId");
			});
	}

	@Test
	void flattenShouldExposeEveryContractKey() {
		runner.run(context -> {
			Map<String, Object> flat = context.getBean(WsConfig.class).flatten();
			assertThat(flat).containsKeys(
					"enabled", "appId",
					"path", "paths",
					"websocket.trustedDomain", "websocket.subProtocols",
					"websocket.supportsPartialMessages",
					"session.cache.maxClients", "session.cache.expireMinutes", "session.cache.concurrencyLevel",
					"session.delayMillis", "session.maxTextMessageSize", "session.maxBinaryMessageSize",
					"session.maxTotalSessions", "session.maxSessionsPerClient",
					"session.idleScanEnabled", "session.idleScanIntervalSeconds", "session.closeOnCacheEviction",
					"scheduler.delayedClosePoolSize",
					"redis.channel.broadcast",
					"param.clientId", "param.maxClientIdLength",
					"init.type", "close.type",
					"binary.enabled", "binary.type",
					"broadcast.async",
					"webflux.scriptThreadPool.coreSize", "webflux.scriptThreadPool.maxSize",
					"webflux.scriptThreadPool.queueCapacity");
		});
	}

	@Test
	void flattenedValuesShouldReachReaders() {
		runner.withPropertyValues(
				"fw.plus.ws.session.delayMillis=2000",
				"fw.plus.ws.session.cache.expireMinutes=5",
				"fw.plus.ws.session.maxSessionsPerClient=7").run(context -> {
				Map<String, Object> flat = context.getBean(WsConfig.class).flatten();
				assertThat(flat.get("session.delayMillis")).isEqualTo(2000);
				assertThat(flat.get("session.cache.expireMinutes")).isEqualTo(5);
			assertThat(flat.get("session.maxSessionsPerClient")).isEqualTo(7);
		});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(WsConfig.class)
	static class BindingConfiguration {
	}
}
