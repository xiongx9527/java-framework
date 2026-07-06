package com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis;

import com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.config.properties.RedisDataSourceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisDataSourceContextTest {

	@Test
	void getRedisPropertiesShouldRejectBlankDatasourceName() {
		TestRedisDataSourceContext context = new TestRedisDataSourceContext();
		context.setDataSourceName("   ");

		assertThatThrownBy(context::redisProperties)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("dataSourceName");
	}

	@Test
	void getRedisPropertiesShouldRejectMissingApplicationContext() {
		TestRedisDataSourceContext context = new TestRedisDataSourceContext();
		context.setDataSourceName("orders");

		assertThatThrownBy(context::redisProperties)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("ApplicationContext");
	}

	@Test
	void getRedisPropertiesShouldRejectMissingDatasourceConfig() {
		RedisDataSourceProperties properties = new RedisDataSourceProperties();
		ApplicationContext applicationContext = mock(ApplicationContext.class);
		when(applicationContext.getBean(RedisDataSourceProperties.class)).thenReturn(properties);
		TestRedisDataSourceContext context = new TestRedisDataSourceContext();
		context.setDataSourceName("orders");
		context.setApplicationContext(applicationContext);

		assertThatThrownBy(context::redisProperties)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("orders");
	}

	@Test
	void getRedisPropertiesShouldReturnConfiguredDatasource() {
		RedisProperties redisProperties = new RedisProperties();
		RedisDataSourceProperties properties = new RedisDataSourceProperties();
		properties.addRedisProperties("orders", redisProperties);
		ApplicationContext applicationContext = mock(ApplicationContext.class);
		when(applicationContext.getBean(RedisDataSourceProperties.class)).thenReturn(properties);
		TestRedisDataSourceContext context = new TestRedisDataSourceContext();
		context.setDataSourceName("orders");
		context.setApplicationContext(applicationContext);

		assertThat(context.redisProperties()).isSameAs(redisProperties);
	}

	private static class TestRedisDataSourceContext extends RedisDataSourceContext {
		RedisProperties redisProperties() {
			return getRedisProperties();
		}
	}
}
