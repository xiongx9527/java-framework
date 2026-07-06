package com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.register;

import com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.helper.RedisHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisDataSourceRegisterTest {

	@AfterEach
	void tearDown() {
		RedisDataSourceRegister.getRedisTemplateRegister().clear();
		RedisDataSourceRegister.getRedisHelperRegister().clear();
	}

	@Test
	void shouldGetRegisteredRedisTemplateByDatasourceName() {
		RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
		RedisDataSourceRegister.registerRedisTemplate("orders", redisTemplate);

		assertThat(RedisDataSourceRegister.getRedisTemplate("orders")).isSameAs(redisTemplate);
	}

	@Test
	void shouldGetRegisteredRedisHelperByDatasourceName() {
		RedisHelper redisHelper = mock(RedisHelper.class);
		RedisDataSourceRegister.registerRedisHelper("orders", redisHelper);

		assertThat(RedisDataSourceRegister.getRedisHelper("orders")).isSameAs(redisHelper);
	}

	@Test
	void shouldIgnoreBlankDatasourceNameWhenRegistering() {
		RedisDataSourceRegister.registerRedisTemplate("   ", mock(RedisTemplate.class));
		RedisDataSourceRegister.registerRedisHelper("   ", mock(RedisHelper.class));

		assertThat(RedisDataSourceRegister.getRedisTemplateRegister()).isEmpty();
		assertThat(RedisDataSourceRegister.getRedisHelperRegister()).isEmpty();
	}
}
