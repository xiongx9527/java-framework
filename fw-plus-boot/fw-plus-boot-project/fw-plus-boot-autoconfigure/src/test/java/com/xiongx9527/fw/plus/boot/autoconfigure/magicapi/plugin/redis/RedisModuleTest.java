package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.model.MagicDynamicRedisDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.model.RedisDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.module.RedisModule;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;

class RedisModuleTest {

	@Test
	void dynamicAttributeUsesNamedRedisConnectionFactory() throws Exception {
		MagicDynamicRedisDataSource dataSource = new MagicDynamicRedisDataSource();
		dataSource.buildRedisConnectionFactory(validInfo("redisA"));
		RedisModule module = new RedisModule(dataSource, defaultFactory());

		RedisModule dynamic = module.getDynamicAttribute("redisA");

		assertThat(dynamic).isNotSameAs(module);
		assertThat(dynamic.getRedisTemplate().getConnectionFactory())
				.isSameAs(dataSource.getRedisConnectionFactory("redisA"));
		dataSource.destroy();
	}

	@Test
	void missingDynamicAttributeFallsBackToCurrentModule() {
		RedisModule module = new RedisModule(new MagicDynamicRedisDataSource(), defaultFactory());

		assertThat(module.getDynamicAttribute("missing")).isSameAs(module);
	}

	@Test
	void dynamicModuleCopiesDataSourceMetadata() throws Exception {
		MagicDynamicRedisDataSource dataSource = new MagicDynamicRedisDataSource();
		RedisDataSourceInfo info = validInfo("redisB");
		info.setUsername("user");
		info.setPassword("pwd");
		info.setDatabase("2");
		dataSource.buildRedisConnectionFactory(info);
		RedisModule module = new RedisModule(dataSource, defaultFactory());

		RedisModule dynamic = module.getDynamicAttribute("redisB");

		assertThat(dynamic.getHost()).isEqualTo("127.0.0.1");
		assertThat(dynamic.getPort()).isEqualTo("6379");
		assertThat(dynamic.getUsername()).isEqualTo("user");
		assertThat(dynamic.getPassword()).isEqualTo("pwd");
		assertThat(dynamic.getDatabase()).isEqualTo("2");
		dataSource.destroy();
	}

	private RedisDataSourceInfo validInfo(String key) {
		RedisDataSourceInfo info = new RedisDataSourceInfo();
		info.setName(key);
		info.setKey(key);
		info.setHostname("127.0.0.1");
		info.setPort("6379");
		info.setDatabase("0");
		info.setClientType("lettuce");
		info.setMode("standalone");
		return info;
	}

	private LettuceConnectionFactory defaultFactory() {
		return new LettuceConnectionFactory("127.0.0.1", 6379);
	}
}
