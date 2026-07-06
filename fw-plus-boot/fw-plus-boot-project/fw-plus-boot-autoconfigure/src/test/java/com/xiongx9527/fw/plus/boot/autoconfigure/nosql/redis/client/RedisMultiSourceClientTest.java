package com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.client;

import com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.helper.RedisHelper;
import com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.register.RedisDataSourceRegister;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class RedisMultiSourceClientTest {

	private final RedisMultiSourceClient client = new RedisMultiSourceClient();

	@AfterEach
	void tearDown() {
		RedisDataSourceRegister.getRedisTemplateRegister().clear();
		RedisDataSourceRegister.getRedisHelperRegister().clear();
	}

	@Test
	void opsDefaultDbShouldReturnTemplateRegisteredByDatasourceName() {
		RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
		RedisDataSourceRegister.registerRedisTemplate("orders", redisTemplate);

		assertThat(client.opsDefaultDb("orders")).isSameAs(redisTemplate);
	}

	@Test
	void opsDefaultDbShouldRejectBlankDatasourceName() {
		assertThatThrownBy(() -> client.opsDefaultDb("   "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("datasource");
	}

	@Test
	void opsDefaultDbShouldIncludeDatasourceNameWhenMissing() {
		assertThatThrownBy(() -> client.opsDefaultDb("missing"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("missing");
	}

	@Test
	void commonOpsDbShouldRejectMissingHelperWithDatasourceName() {
		assertThatThrownBy(() -> client.opsDbZero("missing"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("missing");
	}
}
