package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.scanner;

import com.ajaxjs.mcp.server.feature.FeatureMgr;
import com.xiongx9527.fw.plus.ai.mcp.annotation.McpServer;
import com.xiongx9527.fw.plus.ai.mcp.feature.McpFeatureScanner;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证 {@link McpFeatureScanner} 按 {@link McpServer} 注解把能力归属到对应 runtime 的
 * {@link FeatureMgr}。扫描基包使用本测试所在包的 fixture 子包。
 */
class McpFeatureScannerTest {

	private final McpFeatureScanner scanner = new McpFeatureScanner();

	private final String fixturePackage = "com.xiongx9527.fw.plus.ai.autoconfigure.mcp.scanner.fixture";
	private final String ghostPackage = "com.xiongx9527.fw.plus.ai.autoconfigure.mcp.scanner.ghost";

	@Test
	void shouldRouteAnnotatedCapabilitiesToMatchingFeatureMgr() throws Exception {
		Map<String, FeatureMgr> mgrs = mgrs("default", "order", "weather");

		scanner.register(Collections.singletonList(fixturePackage), mgrs);

		// 未标注 + @McpServer(空) 归属 default
		assertThat(mgrs.get("default").getToolStore()).containsKey("getWeather");
		assertThat(mgrs.get("default").getToolStore()).containsKey("getWeatherDetail");
		// 类级 @McpServer("order") 归属 order
		assertThat(mgrs.get("order").getToolStore()).containsKey("createOrder");
		// 方法级 @McpServer("weather") 覆盖类级，归属 weather
		assertThat(mgrs.get("weather").getToolStore()).containsKey("getOrder");
		// 各 runtime 相互隔离
		assertThat(mgrs.get("order").getToolStore()).doesNotContainKey("getOrder");
		assertThat(mgrs.get("default").getToolStore()).doesNotContainKey("createOrder");
	}

	@Test
	void shouldThrowWhenCapabilityBoundToUnresolvedServerKey() throws Exception {
		Map<String, FeatureMgr> mgrs = mgrs("default");

		assertThatThrownBy(() -> scanner.register(Collections.singletonList(ghostPackage), mgrs))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("unresolved server key 'ghost'");
	}

	@Test
	void shouldRequireDefaultKeyInFeatureMgrs() {
		Map<String, FeatureMgr> mgrs = new HashMap<>();
		mgrs.put("order", new FeatureMgr());

		assertThatThrownBy(() -> scanner.register(Collections.singletonList(fixturePackage), mgrs))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("featureMgrs must contain key 'default'");
	}

	private Map<String, FeatureMgr> mgrs(String... keys) {
		Map<String, FeatureMgr> map = new HashMap<>();
		for (String key : keys) {
			map.put(key, new FeatureMgr());
		}
		return map;
	}
}