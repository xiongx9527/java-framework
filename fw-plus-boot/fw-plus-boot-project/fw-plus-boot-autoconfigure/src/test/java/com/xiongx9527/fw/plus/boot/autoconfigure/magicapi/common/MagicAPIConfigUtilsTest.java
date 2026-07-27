package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MagicAPIConfigUtilsTest {

	@Test
	void mergesConfiguredPropertiesWithMixedJsonTypes() {
		Map<String, Object> source = new LinkedHashMap<String, Object>();
		source.put("timeout", 1000);
		source.put("ssl", false);

		Map<String, Object> merged = MagicAPIConfigUtils.mergeProperties(source,
				"{\"timeout\":3000,\"nested\":{\"values\":[\"read\",2,true,null]}}");

		assertThat(merged).isNotSameAs(source);
		assertThat(merged).containsEntry("timeout", 3000)
				.containsEntry("ssl", false);
		assertThat(merged.get("nested")).isInstanceOf(Map.class);
		Map<?, ?> nested = (Map<?, ?>) merged.get("nested");
		assertThat(nested.get("values")).isInstanceOf(List.class);
		assertThat((List<?>) nested.get("values")).containsExactly("read", 2, true, null);

		merged.put("ssl", true);
		assertThat(source).containsEntry("ssl", false);
	}

	@Test
	void nullAndBlankPropertiesProduceEmptyMutableMap() {
		Map<String, Object> merged = MagicAPIConfigUtils.mergeProperties(null, " ");

		assertThat(merged).isEmpty();
		merged.put("enabled", true);
		assertThat(merged).containsEntry("enabled", true);
	}
}
