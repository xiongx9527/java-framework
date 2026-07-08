package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.model.RedisDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.model.RedisDataSourceValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RedisDataSourceValidatorTest {

	@Test
	void validStandaloneInfoPassesValidation() {
		assertThatCode(() -> RedisDataSourceValidator.validate(validStandaloneInfo()))
				.doesNotThrowAnyException();
	}

	@Test
	void rejectsBlankRequiredFields() {
		RedisDataSourceInfo info = validStandaloneInfo();
		info.setKey(" ");

		assertThatThrownBy(() -> RedisDataSourceValidator.validate(info))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("key");
	}

	@Test
	void rejectsUnsupportedClientTypeAndMode() {
		RedisDataSourceInfo badClientType = validStandaloneInfo();
		badClientType.setClientType("redisson");
		assertThatThrownBy(() -> RedisDataSourceValidator.validate(badClientType))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("clientType");

		RedisDataSourceInfo badMode = validStandaloneInfo();
		badMode.setMode("master-replica");
		assertThatThrownBy(() -> RedisDataSourceValidator.validate(badMode))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("mode");
	}

	@Test
	void rejectsInvalidPortAndDatabase() {
		RedisDataSourceInfo badPort = validStandaloneInfo();
		badPort.setPort("abc");
		assertThatThrownBy(() -> RedisDataSourceValidator.validate(badPort))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("port");

		RedisDataSourceInfo badDatabase = validStandaloneInfo();
		badDatabase.setDatabase("-1");
		assertThatThrownBy(() -> RedisDataSourceValidator.validate(badDatabase))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("database");
	}

	@Test
	void validatesOtherAsJsonObject() {
		RedisDataSourceInfo arrayJson = validStandaloneInfo();
		arrayJson.setOther("[]");
		assertThatThrownBy(() -> RedisDataSourceValidator.validate(arrayJson))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("other");

		RedisDataSourceInfo invalidJson = validStandaloneInfo();
		invalidJson.setOther("{");
		assertThatThrownBy(() -> RedisDataSourceValidator.validate(invalidJson))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("other");
	}

	@Test
	void clusterModeRequiresAtLeastOneNode() {
		RedisDataSourceInfo info = validStandaloneInfo();
		info.setMode("cluster");
		info.setHostname("");
		info.setPort("");
		info.setOther("{\"nodes\":[{\"host\":\"127.0.0.1\",\"port\":6379}]}");
		assertThatCode(() -> RedisDataSourceValidator.validate(info)).doesNotThrowAnyException();

		info.setOther("{}");
		assertThatThrownBy(() -> RedisDataSourceValidator.validate(info))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("nodes");
	}

	@Test
	void sentinelModeRequiresMasterAndSentinel() {
		RedisDataSourceInfo info = validStandaloneInfo();
		info.setMode("sentinel");
		info.setHostname("");
		info.setPort("");
		info.setOther("{\"masterName\":\"mymaster\",\"sentinels\":[{\"host\":\"127.0.0.1\",\"port\":26379}]}");
		assertThatCode(() -> RedisDataSourceValidator.validate(info)).doesNotThrowAnyException();

		info.setOther("{\"sentinels\":[{\"host\":\"127.0.0.1\",\"port\":26379}]}");
		assertThatThrownBy(() -> RedisDataSourceValidator.validate(info))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("masterName");
	}

	@Test
	void normalizesBlankClientTypeAndModeToDefaults() {
		assertThat(RedisDataSourceValidator.normalizeClientType(" ")).isEqualTo("lettuce");
		assertThat(RedisDataSourceValidator.normalizeMode(null)).isEqualTo("standalone");
	}

	private RedisDataSourceInfo validStandaloneInfo() {
		RedisDataSourceInfo info = new RedisDataSourceInfo();
		info.setName("默认Redis");
		info.setKey("defaultRedis");
		info.setHostname("127.0.0.1");
		info.setPort("6379");
		info.setDatabase("0");
		info.setClientType("lettuce");
		info.setMode("standalone");
		info.setOther("{\"timeout\":2000,\"pool\":{\"maxActive\":8}}");
		return info;
	}
}
