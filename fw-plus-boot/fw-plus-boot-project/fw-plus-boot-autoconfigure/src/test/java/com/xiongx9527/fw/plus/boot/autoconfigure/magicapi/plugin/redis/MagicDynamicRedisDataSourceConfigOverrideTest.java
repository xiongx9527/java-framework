package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class MagicDynamicRedisDataSourceConfigOverrideTest {

	@Test
	void keepsMagicApiRedisConfigOverrideCalls() throws Exception {
		String source = new String(Files.readAllBytes(Paths.get(
				"src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/redis/model/MagicDynamicRedisDataSource.java")),
				StandardCharsets.UTF_8);

		assertThat(source).contains("MagicAPIConfigUtils.getRedisConfig");
		assertThat(source).contains("getRedisConfig(dataSourceInfo.getKey(), \"hostname\"");
		assertThat(source).contains("getRedisConfig(dataSourceInfo.getKey(), \"port\"");
		assertThat(source).contains("getRedisConfig(dataSourceInfo.getKey(), \"properties\"");
	}
}
