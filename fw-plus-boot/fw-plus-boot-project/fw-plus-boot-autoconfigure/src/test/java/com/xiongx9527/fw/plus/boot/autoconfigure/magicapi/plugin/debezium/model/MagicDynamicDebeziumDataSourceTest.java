package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class MagicDynamicDebeziumDataSourceTest {

	/**
	 * Debezium 3.x 移除了 snapshot.mode 的旧取值 schema_only / schema_only_recovery，
	 * 插件侧通过 normalizeSnapshotMode 兼容映射为 no_data / recovery。
	 */
	@Test
	void shouldNormalizeLegacySnapshotModes() throws Exception {
		Method normalize = MagicDynamicDebeziumDataSource.class.getDeclaredMethod("normalizeSnapshotMode", String.class);
		normalize.setAccessible(true);

		assertThat(normalize.invoke(null, "schema_only")).isEqualTo("no_data");
		assertThat(normalize.invoke(null, "schema_only_recovery")).isEqualTo("recovery");
		assertThat(normalize.invoke(null, "no_data")).isEqualTo("no_data");
		assertThat(normalize.invoke(null, "recovery")).isEqualTo("recovery");
		assertThat(normalize.invoke(null, "initial")).isEqualTo("initial");
	}
}
