package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.model.RedisDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.service.RedisDataSourceInfoMagicResourceStorage;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisDataSourceInfoMagicResourceStorageTest {

	@Test
	void validatesNewEntityWhenExistingEntityIdIsNull() {
		RedisDataSourceInfo existing = validInfo("oldRedis");
		existing.setId(null);
		RedisDataSourceInfo current = validInfo("newRedis");

		RedisDataSourceInfoMagicResourceStorage storage = new RedisDataSourceInfoMagicResourceStorage();
		MagicResourceService magicResourceService = mock(MagicResourceService.class);
		when(magicResourceService.listFiles("redis-datasource:0")).thenReturn(Collections.singletonList(existing));
		storage.setMagicResourceService(magicResourceService);

		assertThatCode(() -> storage.validate(current)).doesNotThrowAnyException();
	}

	@Test
	void rejectsDuplicateKeyForNewEntity() {
		RedisDataSourceInfo existing = validInfo("redisA");
		existing.setId(null);
		RedisDataSourceInfo current = validInfo("redisA");

		RedisDataSourceInfoMagicResourceStorage storage = new RedisDataSourceInfoMagicResourceStorage();
		MagicResourceService magicResourceService = mock(MagicResourceService.class);
		when(magicResourceService.listFiles("redis-datasource:0")).thenReturn(Collections.singletonList(existing));
		storage.setMagicResourceService(magicResourceService);

		assertThatThrownBy(() -> storage.validate(current))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("redisA");
	}

	private RedisDataSourceInfo validInfo(String key) {
		RedisDataSourceInfo info = new RedisDataSourceInfo();
		info.setName(key);
		info.setKey(key);
		info.setHostname("127.0.0.1");
		info.setPort("6379");
		info.setOther("{}");
		return info;
	}
}
