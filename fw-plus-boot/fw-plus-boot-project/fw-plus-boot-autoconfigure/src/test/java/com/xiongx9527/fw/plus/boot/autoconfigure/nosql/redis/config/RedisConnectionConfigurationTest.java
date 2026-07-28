package com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.config;

import com.xiongx9527.fw.plus.nosql.redis.exception.RedisUrlSyntaxException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RedisConnectionConfigurationTest {

	@Test
	void constructorShouldRejectNullRedisProperties() {
		assertThatThrownBy(() -> new TestRedisConnectionConfiguration(null, null, null, 0))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("redisProperties");
	}

	@Test
	void constructorShouldRejectNegativeDatabase() {
		assertThatThrownBy(() -> new TestRedisConnectionConfiguration(new RedisProperties(), null, null, -1))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("database");
	}

	@Test
	void parseUrlShouldRejectMissingHost() {
		assertThatThrownBy(() -> RedisConnectionConfiguration.parseUrl("redis://:6379"))
				.isInstanceOf(RedisUrlSyntaxException.class);
	}

	@Test
	void parseUrlShouldRejectMissingPort() {
		assertThatThrownBy(() -> RedisConnectionConfiguration.parseUrl("redis://localhost"))
				.isInstanceOf(RedisUrlSyntaxException.class);
	}

	@Test
	void parseUrlShouldParseUsernamePasswordAndSsl() {
		RedisConnectionConfiguration.ConnectionInfo connectionInfo = RedisConnectionConfiguration
				.parseUrl("rediss://user:secret@example.com:6380");

		assertThat(connectionInfo.isUseSsl()).isTrue();
		assertThat(connectionInfo.getUsername()).isEqualTo("user");
		assertThat(connectionInfo.getPassword()).isEqualTo("secret");
		assertThat(connectionInfo.getHostName()).isEqualTo("example.com");
		assertThat(connectionInfo.getPort()).isEqualTo(6380);
	}

	private static class TestRedisConnectionConfiguration extends RedisConnectionConfiguration {

		TestRedisConnectionConfiguration(RedisProperties properties,
		                                 RedisSentinelConfiguration sentinelConfiguration,
		                                 RedisClusterConfiguration clusterConfiguration,
		                                 int database) {
			super(properties, sentinelConfiguration, clusterConfiguration, database);
		}
	}
}
