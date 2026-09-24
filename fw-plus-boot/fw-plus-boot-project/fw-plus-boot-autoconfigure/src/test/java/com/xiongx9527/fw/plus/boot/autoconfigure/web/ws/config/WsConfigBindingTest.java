package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

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
			assertThat(config.getToken().getSecret()).isEmpty();
			assertThat(config.getToken().getExpireHours()).isEqualTo(2L);
			assertThat(config.getWebsocket().getPath()).isEqualTo("/ws/api");
			assertThat(config.getWebsocket().getPaths()).isEmpty();
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
			assertThat(config.getParam().getToken()).isEqualTo("token");
			assertThat(config.getParam().getClientId()).isEqualTo("clientId");
			assertThat(config.getParam().getMaxClientIdLength()).isEqualTo(50);
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
				"fw.plus.ws.token.secret=s3cret",
				"fw.plus.ws.websocket.path=/custom/ws",
				"fw.plus.ws.session.cache.expireMinutes=5",
				"fw.plus.ws.redis.channel.broadcast=custom:channel",
				"fw.plus.ws.binary.enabled=false",
				"fw.plus.ws.broadcast.async=true").run(context -> {
			WsConfig config = context.getBean(WsConfig.class);
			assertThat(config.getToken().getSecret()).isEqualTo("s3cret");
			assertThat(config.getWebsocket().getPath()).isEqualTo("/custom/ws");
			assertThat(config.getSession().getCache().getExpireMinutes()).isEqualTo(5);
			assertThat(config.getRedis().getChannel().getBroadcast()).isEqualTo("custom:channel");
			assertThat(config.getBinary().isEnabled()).isFalse();
			assertThat(config.getBroadcast().isAsync()).isTrue();
		});
	}

	@Test
	void additionalWebsocketPathsShouldBind() {
		runner.withPropertyValues(
					"fw.plus.ws.websocket.paths=/ws/api2, /ws/api3").run(context -> {
			WsConfig config = context.getBean(WsConfig.class);
			assertThat(config.getWebsocket().getPaths()).containsExactly("/ws/api2", "/ws/api3");
			assertThat(config.flatten().get("websocket.paths"))
					.isEqualTo(config.getWebsocket().getPaths());
		});
	}

	@Test
	void flattenShouldExposeEveryContractKey() {
		runner.run(context -> {
			Map<String, Object> flat = context.getBean(WsConfig.class).flatten();
			assertThat(flat).containsKeys(
					"enabled", "appId",
					"token.secret", "token.expireHours",
					"websocket.path", "websocket.paths", "websocket.trustedDomain", "websocket.subProtocols",
					"websocket.supportsPartialMessages", "websocket.endpoint", "websocket.basePath",
					"session.cache.maxClients", "session.cache.expireMinutes", "session.cache.concurrencyLevel",
					"session.delayMillis", "session.maxTextMessageSize", "session.maxBinaryMessageSize",
					"session.maxTotalSessions", "session.maxSessionsPerClient",
					"session.idleScanEnabled", "session.idleScanIntervalSeconds", "session.closeOnCacheEviction",
					"scheduler.delayedClosePoolSize",
					"redis.channel.broadcast",
					"param.token", "param.clientId", "param.maxClientIdLength",
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
				"fw.plus.ws.token.expireHours=12",
				"fw.plus.ws.session.cache.expireMinutes=5",
				"fw.plus.ws.session.maxSessionsPerClient=7").run(context -> {
			Map<String, Object> flat = context.getBean(WsConfig.class).flatten();
			assertThat(flat.get("token.expireHours")).isEqualTo(12L);
			assertThat(flat.get("session.cache.expireMinutes")).isEqualTo(5);
			assertThat(flat.get("session.maxSessionsPerClient")).isEqualTo(7);
		});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(WsConfig.class)
	static class BindingConfiguration {
	}
}
