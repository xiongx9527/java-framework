package com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.helper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RedisDbThreadLocalHelperTest {

	@AfterEach
	void tearDown() {
		RedisDbThreadLocalHelper.clear();
		RedisDbThreadLocalHelper.clear();
	}

	@Test
	void setShouldRejectNegativeDbIndex() {
		assertThatThrownBy(() -> RedisDbThreadLocalHelper.set(-1))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("db");
	}

	@Test
	void setShouldPushAndClearLikeStack() {
		RedisDbThreadLocalHelper.set(1);
		RedisDbThreadLocalHelper.set(2);

		assertThat(RedisDbThreadLocalHelper.get()).isEqualTo(2);
		RedisDbThreadLocalHelper.clear();
		assertThat(RedisDbThreadLocalHelper.get()).isEqualTo(1);
		RedisDbThreadLocalHelper.clear();
		assertThat(RedisDbThreadLocalHelper.get()).isNull();
	}

	@Test
	void threadLocalHolderShouldUseTransmittableThreadLocalWhenAvailable() throws Exception {
		Field field = RedisDbThreadLocalHelper.class.getDeclaredField("THREAD_DB");
		field.setAccessible(true);

		assertThat(field.get(null).getClass().getName()).isEqualTo("com.alibaba.ttl.TransmittableThreadLocal");
	}
}
