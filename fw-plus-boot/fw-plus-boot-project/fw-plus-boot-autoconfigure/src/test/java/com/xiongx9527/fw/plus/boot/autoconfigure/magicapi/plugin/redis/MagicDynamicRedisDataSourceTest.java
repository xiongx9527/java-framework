package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.model.MagicDynamicRedisDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.model.RedisDataSourceInfo;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MagicDynamicRedisDataSourceTest {

	@Test
	void createConnectionFactoryUsesNormalizedDefaults() {
		MagicDynamicRedisDataSource dataSource = new MagicDynamicRedisDataSource();
		RedisDataSourceInfo info = validInfo("redisA");
		info.setClientType("");
		info.setMode("");

		RedisConnectionFactory factory = dataSource.createRedisConnectionFactory(info);

		assertThat(factory).isInstanceOf(LettuceConnectionFactory.class);
		dataSource.destroyRedisConnectionFactory(factory);
	}

	@Test
	void buildRedisConnectionFactoryStoresInfoCopy() throws Exception {
		MagicDynamicRedisDataSource dataSource = new MagicDynamicRedisDataSource();
		RedisDataSourceInfo info = validInfo("redisB");

		dataSource.buildRedisConnectionFactory(info);
		info.setHostname("mutated");

		assertThat(dataSource.getRedisConnectionFactory("redisB")).isNotNull();
		assertThat(dataSource.getRedisDataSourceInfo("redisB").getHostname()).isEqualTo("127.0.0.1");
		dataSource.destroy();
	}

	@Test
	void buildRedisConnectionFactoryKeepsExistingFactoryWhenNewInfoInvalid() throws Exception {
		MagicDynamicRedisDataSource dataSource = new MagicDynamicRedisDataSource();
		dataSource.buildRedisConnectionFactory(validInfo("redisC"));
		RedisConnectionFactory oldFactory = dataSource.getRedisConnectionFactory("redisC");

		RedisDataSourceInfo invalid = validInfo("redisC");
		invalid.setPort("abc");

		assertThatThrownBy(() -> dataSource.buildRedisConnectionFactory(invalid))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("port");
		assertThat(dataSource.getRedisConnectionFactory("redisC")).isSameAs(oldFactory);
		dataSource.destroy();
	}

	@Test
	void destroyRedisConnectionFactoryByKeyRemovesFactoryAndInfo() {
		MagicDynamicRedisDataSource dataSource = new MagicDynamicRedisDataSource();
		dataSource.buildRedisConnectionFactory(validInfo("redisD"));

		dataSource.destroyRedisConnectionFactoryByKey("redisD");

		assertThat(dataSource.getRedisConnectionFactory("redisD")).isNull();
		assertThat(dataSource.getRedisDataSourceInfo("redisD")).isNull();
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
}
