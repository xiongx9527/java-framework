package com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DynamicRedisTemplateFactoryTest {

	@Test
	void constructorShouldRejectNullRedisProperties() {
		assertThatThrownBy(() -> new DynamicRedisTemplateFactory<String, String>(null, null, null,
				Collections.emptyList(), Collections.emptyList()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("redisProperties");
	}

	@Test
	void constructorShouldRejectNullCustomizerLists() {
		RedisProperties properties = new RedisProperties();

		assertThatThrownBy(() -> new DynamicRedisTemplateFactory<String, String>(properties, null, null,
				null, Collections.emptyList()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("jedisBuilderCustomizers");

		assertThatThrownBy(() -> new DynamicRedisTemplateFactory<String, String>(properties, null, null,
				Collections.emptyList(), null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("lettuceBuilderCustomizers");
	}
}
