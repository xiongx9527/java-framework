package com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.config.properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RedisDataSourcePropertiesTest {

	@Test
	void addRedisPropertiesShouldRecoverWhenDatasourceMapWasSetToNull() {
		RedisDataSourceProperties properties = new RedisDataSourceProperties();
		DataRedisProperties redisProperties = new DataRedisProperties();
		properties.setDatasource(null);

		properties.addRedisProperties("orders", redisProperties);

		assertThat(properties.getRedisProperties("orders")).isSameAs(redisProperties);
	}

	@Test
	void getRedisPropertiesShouldReturnNullForBlankName() {
		RedisDataSourceProperties properties = new RedisDataSourceProperties();

		assertThat(properties.getRedisProperties("   ")).isNull();
	}

	@Test
	void addRedisPropertiesShouldRejectNullProperties() {
		RedisDataSourceProperties properties = new RedisDataSourceProperties();

		assertThatThrownBy(() -> properties.addRedisProperties("orders", null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("redisProperties");
	}
}
