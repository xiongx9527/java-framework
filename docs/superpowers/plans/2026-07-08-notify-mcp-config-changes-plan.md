---
change: notify-mcp-config-changes
design-doc: docs/superpowers/specs/2026-07-08-notify-mcp-config-changes-design.md
base-ref: 32ae4fb3e9af2359d01b2160af9585c3560161c5
---

# MCP 配置变化通知实施计划

> **面向执行代理：** 必须使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 按任务执行本计划。步骤使用 checkbox（`- [ ]`）语法跟踪。

**目标：** 在 MCP 相关配置运行期变化后，通过既有 MCP `list_changed` 通知提醒客户端刷新 tools、resources、prompts 能力视图。

**架构：** MCP 语义只放在 MCP notification 包内，由新增 `McpConfigurationChangeNotifier` 监听通用 `ConfigChangeEvent`。MagicAPI `config` 插件只在 Spring `Environment` 刷新完成后发布通用 `ConfigChangeEvent`，不直接依赖 MCP 类。自动配置在 `magic-api.plugin.mcp.enabled=true` 条件下注册通知器，复用 `McpNotificationService.broadcast(...)` 的 SSE capability 过滤和 MagicNotify 集群广播。

**技术栈：** Java 8、Spring Boot 2.7.18、Spring Cloud `ContextRefresher`、Magic-API 2.2.2、JUnit 5、AssertJ、Mockito、Spring `ApplicationContextRunner`。

## 全局约束

- 所有 Java 源码保持 Java 8 兼容，不使用 `List.of()`、`Map.of()`、`Optional.orElseThrow()` 等 Java 9+ API。
- 只修改 `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure` 内与 MCP notification、MCP starter、MagicAPI config registry 直接相关的源码和测试。
- `McpConfigurationChangeNotifier` 必须位于 `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.notification` 包。
- `ConfigMagicDynamicRegistry` 只发布通用 `ConfigChangeEvent`，不得 import 或引用 MCP 包下任何类。
- 配置事件必须在 `ContextRefresher.refresh()` 完成之后发布，避免客户端收到通知后读取旧配置。
- MCP notifier 只处理 `magic-api.plugin.mcp.tools.functions.*`、`magic-api.plugin.mcp.tools.api.*`、`magic-api.plugin.mcp.protocol.*`、`magic-api.plugin.mcp.auth.*`、`magic-api.plugin.mcp.headers.*`。
- `tools.functions.*` 和 `tools.api.*` 变化只发送 `notifications/tools/list_changed`。
- `protocol.*`、`auth.*`、`headers.*` 变化发送 `notifications/tools/list_changed`、`notifications/resources/list_changed`、`notifications/prompts/list_changed`。
- 同一次刷新内映射到同一 MCP notification method 的配置 key 必须去重，每种 method 最多广播一次。
- 非 MCP key 以及未覆盖的 `magic-api.plugin.mcp.*` key 不触发 MCP 通知。
- Maven 验证必须使用模块内 Maven Wrapper：从 `fw-plus-boot` 目录执行 `./mvnw`。
- 执行代码实现时不得回滚或覆盖他人改动；提交前必须检查 `git status --short` 并只暂存本任务相关文件。

---

## 文件结构

- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/notification/McpConfigurationChangeNotifier.java`
  - 单一职责：把过滤后的 MCP 配置 key 映射为既有 MCP `list_changed` notification，并调用 `McpNotificationService.broadcast(...)`。
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/notification/McpConfigurationChangeNotifierTest.java`
  - 单一职责：验证 key 匹配、通知映射、同批去重、非相关 key 忽略。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/starter/MagicAPIMcpConfiguration.java`
  - 注册 `McpConfigurationChangeNotifier` bean，并保持整个 MCP 自动配置受 `magic-api.plugin.mcp.enabled` 条件控制。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/starter/MagicAPIMcpConfigurationTest.java`
  - 验证默认启用时注册 notifier，禁用 MCP 时不注册 MCP bean。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/config/service/ConfigMagicDynamicRegistry.java`
  - 在 MagicAPI `config` 资源刷新 Spring `Environment` 后发布 `ConfigChangeEvent`，删除/禁用/取消注册时也使用原脚本 flatten key 发布事件。
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/config/service/ConfigMagicDynamicRegistryTest.java`
  - 单一职责：验证刷新后发布通用事件、删除/禁用携带原 flatten key、没有 MCP bean 时配置刷新仍可工作。

### Task 1: 新增 MCP 配置变化通知器

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/notification/McpConfigurationChangeNotifier.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/notification/McpConfigurationChangeNotifierTest.java`

**Interfaces:**
- Consumes: `ConfigChangeEventListener` 对 `@OnConfigChange(prefix = "magic-api.plugin.mcp")` 的 Map 参数分发。
- Consumes: `McpNotificationService.broadcast(Map<String, Object> message)`。
- Produces: `public void onMcpConfigurationChanged(Map<String, Object> properties)`，供 `ConfigChangeEventListener` 反射调用。
- Produces: 每次调用按固定顺序广播 tools、resources、prompts 三类去重 notification。

- [x] **Step 1: 写失败测试覆盖 tools key 映射**

Create `McpConfigurationChangeNotifierTest.java`，先写以下测试类。该测试使用 recording service 捕获 broadcast method，预期在实现类不存在或未实现映射时失败。

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.notification;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class McpConfigurationChangeNotifierTest {

	@Test
	void shouldNotifyToolsListWhenFunctionToolConfigChanges() {
		RecordingNotificationService notificationService = new RecordingNotificationService();
		McpConfigurationChangeNotifier notifier = new McpConfigurationChangeNotifier(notificationService);
		Map<String, Object> properties = new HashMap<>();
		properties.put("magic-api.plugin.mcp.tools.functions.enabled", true);

		notifier.onMcpConfigurationChanged(properties);

		assertThat(notificationService.methods).containsExactly("notifications/tools/list_changed");
	}

	@Test
	void shouldNotifyToolsListWhenApiToolConfigChanges() {
		RecordingNotificationService notificationService = new RecordingNotificationService();
		McpConfigurationChangeNotifier notifier = new McpConfigurationChangeNotifier(notificationService);
		Map<String, Object> properties = new HashMap<>();
		properties.put("magic-api.plugin.mcp.tools.api.exposure-default", "disabled");

		notifier.onMcpConfigurationChanged(properties);

		assertThat(notificationService.methods).containsExactly("notifications/tools/list_changed");
	}

	private static class RecordingNotificationService extends McpNotificationService {

		private final List<String> methods = new ArrayList<>();

		private RecordingNotificationService() {
			super(null, null);
		}

		@Override
		public void broadcast(Map<String, Object> message) {
			methods.add(String.valueOf(message.get("method")));
		}
	}
}
```

- [x] **Step 2: 运行 Task 1 当前测试确认失败**

Run:

```bash
cd fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=McpConfigurationChangeNotifierTest test
```

Expected: FAIL，编译失败信息包含 `cannot find symbol` 和 `McpConfigurationChangeNotifier`。

- [x] **Step 3: 补充完整 mapping 与去重测试**

在同一个测试类中追加以下测试方法，覆盖协议、认证、headers、去重、忽略逻辑。

```java
	@Test
	void shouldNotifyAllCapabilityListsWhenProtocolConfigChanges() {
		RecordingNotificationService notificationService = new RecordingNotificationService();
		McpConfigurationChangeNotifier notifier = new McpConfigurationChangeNotifier(notificationService);
		Map<String, Object> properties = new HashMap<>();
		properties.put("magic-api.plugin.mcp.protocol.default-version", "2025-11-25");

		notifier.onMcpConfigurationChanged(properties);

		assertThat(notificationService.methods).containsExactly(
				"notifications/tools/list_changed",
				"notifications/resources/list_changed",
				"notifications/prompts/list_changed");
	}

	@Test
	void shouldNotifyAllCapabilityListsWhenAuthConfigChanges() {
		RecordingNotificationService notificationService = new RecordingNotificationService();
		McpConfigurationChangeNotifier notifier = new McpConfigurationChangeNotifier(notificationService);
		Map<String, Object> properties = new HashMap<>();
		properties.put("magic-api.plugin.mcp.auth.enabled", false);

		notifier.onMcpConfigurationChanged(properties);

		assertThat(notificationService.methods).containsExactly(
				"notifications/tools/list_changed",
				"notifications/resources/list_changed",
				"notifications/prompts/list_changed");
	}

	@Test
	void shouldNotifyAllCapabilityListsWhenHeaderConfigChanges() {
		RecordingNotificationService notificationService = new RecordingNotificationService();
		McpConfigurationChangeNotifier notifier = new McpConfigurationChangeNotifier(notificationService);
		Map<String, Object> properties = new HashMap<>();
		properties.put("magic-api.plugin.mcp.headers.session-id", "Mcp-Session-Id");

		notifier.onMcpConfigurationChanged(properties);

		assertThat(notificationService.methods).containsExactly(
				"notifications/tools/list_changed",
				"notifications/resources/list_changed",
				"notifications/prompts/list_changed");
	}

	@Test
	void shouldDeduplicateNotificationMethodsWithinOneRefresh() {
		RecordingNotificationService notificationService = new RecordingNotificationService();
		McpConfigurationChangeNotifier notifier = new McpConfigurationChangeNotifier(notificationService);
		Map<String, Object> properties = new HashMap<>();
		properties.put("magic-api.plugin.mcp.tools.functions.enabled", true);
		properties.put("magic-api.plugin.mcp.tools.api.enabled", true);
		properties.put("magic-api.plugin.mcp.auth.enabled", false);

		notifier.onMcpConfigurationChanged(properties);

		assertThat(notificationService.methods).containsExactly(
				"notifications/tools/list_changed",
				"notifications/resources/list_changed",
				"notifications/prompts/list_changed");
	}

	@Test
	void shouldIgnoreNonMcpAndUncoveredMcpConfigKeys() {
		RecordingNotificationService notificationService = new RecordingNotificationService();
		McpConfigurationChangeNotifier notifier = new McpConfigurationChangeNotifier(notificationService);
		Map<String, Object> properties = new HashMap<>();
		properties.put("server.port", 8080);
		properties.put("magic-api.plugin.mcp.endpoint", "/mcp");
		properties.put("magic-api.plugin.mcp.enabled", true);

		notifier.onMcpConfigurationChanged(properties);

		assertThat(notificationService.methods).isEmpty();
	}

	@Test
	void shouldIgnoreNullOrEmptyProperties() {
		RecordingNotificationService notificationService = new RecordingNotificationService();
		McpConfigurationChangeNotifier notifier = new McpConfigurationChangeNotifier(notificationService);

		notifier.onMcpConfigurationChanged(null);
		notifier.onMcpConfigurationChanged(new HashMap<String, Object>());

		assertThat(notificationService.methods).isEmpty();
	}
```

- [x] **Step 4: 实现 `McpConfigurationChangeNotifier` 最小可用代码**

Create production class with Java 8-compatible collections and deterministic broadcast order.

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.notification;

import com.xiongx9527.fw.plus.boot.autoconfigure.config.event.OnConfigChange;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class McpConfigurationChangeNotifier {

	private static final String MCP_PREFIX = "magic-api.plugin.mcp";
	private static final String FUNCTIONS_TOOLS_PREFIX = MCP_PREFIX + ".tools.functions.";
	private static final String API_TOOLS_PREFIX = MCP_PREFIX + ".tools.api.";
	private static final String PROTOCOL_PREFIX = MCP_PREFIX + ".protocol.";
	private static final String AUTH_PREFIX = MCP_PREFIX + ".auth.";
	private static final String HEADERS_PREFIX = MCP_PREFIX + ".headers.";

	private static final String TOOLS_LIST_CHANGED = "notifications/tools/list_changed";
	private static final String RESOURCES_LIST_CHANGED = "notifications/resources/list_changed";
	private static final String PROMPTS_LIST_CHANGED = "notifications/prompts/list_changed";

	private final McpNotificationService notificationService;

	public McpConfigurationChangeNotifier(McpNotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@OnConfigChange(prefix = MCP_PREFIX)
	public void onMcpConfigurationChanged(Map<String, Object> properties) {
		if (properties == null || properties.isEmpty()) {
			return;
		}
		Set<String> methods = new LinkedHashSet<>();
		for (String key : properties.keySet()) {
			collectNotificationMethods(key, methods);
		}
		for (String method : methods) {
			broadcast(method);
		}
	}

	private void collectNotificationMethods(String key, Set<String> methods) {
		if (key == null) {
			return;
		}
		if (key.startsWith(FUNCTIONS_TOOLS_PREFIX) || key.startsWith(API_TOOLS_PREFIX)) {
			methods.add(TOOLS_LIST_CHANGED);
			return;
		}
		if (key.startsWith(PROTOCOL_PREFIX) || key.startsWith(AUTH_PREFIX) || key.startsWith(HEADERS_PREFIX)) {
			methods.add(TOOLS_LIST_CHANGED);
			methods.add(RESOURCES_LIST_CHANGED);
			methods.add(PROMPTS_LIST_CHANGED);
		}
	}

	private void broadcast(String method) {
		if (TOOLS_LIST_CHANGED.equals(method)) {
			notificationService.broadcast(McpNotificationService.toolsListChanged());
			return;
		}
		if (RESOURCES_LIST_CHANGED.equals(method)) {
			notificationService.broadcast(McpNotificationService.resourcesListChanged());
			return;
		}
		if (PROMPTS_LIST_CHANGED.equals(method)) {
			notificationService.broadcast(McpNotificationService.promptsListChanged());
		}
	}
}
```

- [x] **Step 5: 运行 Task 1 测试确认通过**

Run:

```bash
cd fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=McpConfigurationChangeNotifierTest test
```

Expected: PASS，输出包含 `Tests run: 8` 且无 failure/error。

- [x] **Step 6: 提交 Task 1**

Run:

```bash
git status --short
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/notification/McpConfigurationChangeNotifier.java fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/notification/McpConfigurationChangeNotifierTest.java
git commit -m "feat: add mcp configuration change notifier"
```

Expected: commit only includes the new notifier and its unit test.

### Task 2: 在 MCP 自动配置中注册通知器

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/starter/MagicAPIMcpConfiguration.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/starter/MagicAPIMcpConfigurationTest.java`

**Interfaces:**
- Consumes: `McpNotificationService` bean。
- Produces: `McpConfigurationChangeNotifier` bean registered only when `MagicAPIMcpConfiguration` is active。
- Preserves: `magic-api.plugin.mcp.enabled=false` 时 `MagicAPIMcpConfiguration` 不存在，MCP 相关 bean 不注册。

- [ ] **Step 1: 写自动配置失败测试**

在 `MagicAPIMcpConfigurationTest.java` imports 中加入：

```java
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.notification.McpConfigurationChangeNotifier;
```

追加以下测试方法：

```java
	@Test
	void shouldRegisterMcpConfigurationChangeNotifierByDefault() {
		contextRunner.run(context -> assertThat(context).hasSingleBean(McpConfigurationChangeNotifier.class));
	}

	@Test
	void shouldNotRegisterMcpConfigurationChangeNotifierWhenMcpDisabled() {
		contextRunner.withPropertyValues("magic-api.plugin.mcp.enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean(McpConfigurationChangeNotifier.class));
	}
```

- [ ] **Step 2: 运行自动配置测试确认失败**

Run:

```bash
cd fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicAPIMcpConfigurationTest test
```

Expected: FAIL，失败断言显示缺少 `McpConfigurationChangeNotifier` bean。

- [ ] **Step 3: 修改 `MagicAPIMcpConfiguration` 注册 bean**

在 `MagicAPIMcpConfiguration.java` imports 中加入：

```java
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.notification.McpConfigurationChangeNotifier;
```

在 `mcpNotificationService(...)` bean 后、cluster publisher bean 前加入：

```java
	@Bean
	@ConditionalOnMissingBean
	public McpConfigurationChangeNotifier mcpConfigurationChangeNotifier(McpNotificationService notificationService) {
		return new McpConfigurationChangeNotifier(notificationService);
	}
```

- [ ] **Step 4: 运行 Task 2 测试确认通过**

Run:

```bash
cd fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='MagicAPIMcpConfigurationTest,McpConfigurationChangeNotifierTest' test
```

Expected: PASS，`MagicAPIMcpConfigurationTest` 和 `McpConfigurationChangeNotifierTest` 均无 failure/error。

- [ ] **Step 5: 提交 Task 2**

Run:

```bash
git status --short
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/starter/MagicAPIMcpConfiguration.java fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/starter/MagicAPIMcpConfigurationTest.java
git commit -m "feat: register mcp config change notifier"
```

Expected: commit only includes MCP starter registration and starter tests.

### Task 3: MagicAPI config 刷新后发布 ConfigChangeEvent

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/config/service/ConfigMagicDynamicRegistry.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/config/service/ConfigMagicDynamicRegistryTest.java`

**Interfaces:**
- Consumes: `ContextRefresher.refresh()`。
- Consumes: `ApplicationEventPublisher.publishEvent(Object event)`。
- Produces: `ConfigChangeEvent` whose `properties` is the flattened YAML key/value map for registered configs。
- Produces: unregister/delete/disable path publishes `ConfigChangeEvent` with original flattened keys after property source removal and refresh。
- Preserves: no direct dependency on MCP notification classes。

- [ ] **Step 1: 写刷新后发布事件测试**

Create `ConfigMagicDynamicRegistryTest.java` with the following class. The tests call private registry methods through `ReflectionTestUtils` to isolate Spring `Environment` refresh/publish behavior from Magic-API static resource service bootstrapping.

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.config.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.config.event.ConfigChangeEvent;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.config.model.ConfigInfo;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ConfigMagicDynamicRegistryTest {

	@Test
	void shouldPublishConfigChangeEventAfterRegisterRefresh() {
		ConfigMagicDynamicRegistry registry = registry();
		RecordingEventPublisher publisher = new RecordingEventPublisher();
		ContextRefresher contextRefresher = mock(ContextRefresher.class);
		MockEnvironment environment = new MockEnvironment();
		ReflectionTestUtils.setField(registry, "eventPublisher", publisher);
		ReflectionTestUtils.setField(registry, "contextRefresher", contextRefresher);
		ReflectionTestUtils.setField(registry, "environment", environment);

		ReflectionTestUtils.invokeMethod(registry, "registerByEntity", config("magic-api:\n  plugin:\n    mcp:\n      tools:\n        functions:\n          enabled: true\n"));

		verify(contextRefresher).refresh();
		assertThat(publisher.events).hasSize(1);
		ConfigChangeEvent event = publisher.events.get(0);
		assertThat(event.getProperties()).containsEntry("magic-api.plugin.mcp.tools.functions.enabled", true);
	}

	@Test
	void shouldPublishUnrelatedConfigChangeEventWithoutMcpBeans() {
		ConfigMagicDynamicRegistry registry = registry();
		RecordingEventPublisher publisher = new RecordingEventPublisher();
		ContextRefresher contextRefresher = mock(ContextRefresher.class);
		MockEnvironment environment = new MockEnvironment();
		ReflectionTestUtils.setField(registry, "eventPublisher", publisher);
		ReflectionTestUtils.setField(registry, "contextRefresher", contextRefresher);
		ReflectionTestUtils.setField(registry, "environment", environment);

		ReflectionTestUtils.invokeMethod(registry, "registerByEntity", config("server:\n  port: 3090\n"));

		verify(contextRefresher).refresh();
		assertThat(publisher.events).hasSize(1);
		assertThat(publisher.events.get(0).getProperties()).containsEntry("server.port", 3090);
	}

	private ConfigMagicDynamicRegistry registry() {
		return new ConfigMagicDynamicRegistry(null);
	}

	private ConfigInfo config(String script) {
		ConfigInfo info = new ConfigInfo();
		info.setEnv("dev");
		info.setAppId("app");
		info.setNamespace("mcp");
		info.setEnabled(true);
		info.setExternalFlag(false);
		info.setScript(script);
		return info;
	}

	private static class RecordingEventPublisher implements ApplicationEventPublisher {

		private final List<ConfigChangeEvent> events = new ArrayList<>();

		@Override
		public void publishEvent(Object event) {
			if (event instanceof ConfigChangeEvent) {
				events.add((ConfigChangeEvent) event);
			}
		}
	}
}
```

- [ ] **Step 2: 运行 ConfigMagicDynamicRegistry 测试确认失败**

Run:

```bash
cd fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=ConfigMagicDynamicRegistryTest test
```

Expected: FAIL，失败原因是 `eventPublisher` 字段不存在或没有发布 `ConfigChangeEvent`。

- [ ] **Step 3: 修改 `ConfigMagicDynamicRegistry` 注入事件发布器并抽取 flatten helper**

在 `ConfigMagicDynamicRegistry.java` imports 中加入：

```java
import com.xiongx9527.fw.plus.boot.autoconfigure.config.event.ConfigChangeEvent;
import org.springframework.context.ApplicationEventPublisher;
```

在字段区加入：

```java
    @Resource
    private ApplicationEventPublisher eventPublisher;
```

在类内加入以下 helper。`parseProperties` 保留空 YAML 行为，`publishConfigChangeEvent` 对空 Map 不发布事件，避免无实际 key 时产生无意义通知。

```java
    private Map<String, Object> parseProperties(ConfigInfo info) {
        Map<String, Object> map = new HashMap<>();
        if (info == null || StringUtils.isBlank(info.getScript())) {
            return map;
        }
        Map<String, Object> loadedMap = yaml.loadAs(info.getScript(), Map.class);
        return this.flattenYamlMap(MapUtils.emptyIfNull(loadedMap));
    }

    private void refreshAndPublish(Map<String, Object> properties) {
        this.contextRefresher.refresh();
        publishConfigChangeEvent(properties);
    }

    private void publishConfigChangeEvent(Map<String, Object> properties) {
        if (this.eventPublisher != null && properties != null && !properties.isEmpty()) {
            this.eventPublisher.publishEvent(new ConfigChangeEvent(this, properties));
        }
    }
```

- [ ] **Step 4: 修改注册路径在 refresh 后发布事件**

将 `registerByEntity(ConfigInfo info)` 内 YAML 解析和 refresh 部分调整为以下结构。保留现有日志和异常包装；只把 `contextRefresher.refresh()` 替换为 `refreshAndPublish(map)`。

```java
            Map<String,Object> map = parseProperties(info);
            boolean changed = false;
            if (this.environment.getPropertySources().contains(propName)) {
                this.environment.getPropertySources().remove(propName);
                changed = true;
            }
            if (!info.isExternalFlag() && info.isEnabled()) {
                this.environment.getPropertySources().addFirst(new MapPropertySource(propName, map));
                changed = true;
            }
            if (changed) {
                refreshAndPublish(map);
            }
```

同时将 public `register(ConfigInfo info)` 从先 unregister 再 register 收敛为一次注册刷新，避免一次保存产生两次 refresh/event：

```java
    @Override
    public boolean register(ConfigInfo info) {
        registerByEntity(info);
        try {
            return super.register(info);
        } catch (Exception e) {
            return false;
        }
    }
```

- [ ] **Step 5: 补充删除/禁用/取消注册发布原 key 测试**

在 `ConfigMagicDynamicRegistryTest.java` imports 中加入：

```java
import org.springframework.core.env.MapPropertySource;
```

追加以下测试方法：

```java
	@Test
	void shouldPublishOriginalKeysWhenConfigIsUnregistered() {
		ConfigMagicDynamicRegistry registry = registry();
		RecordingEventPublisher publisher = new RecordingEventPublisher();
		ContextRefresher contextRefresher = mock(ContextRefresher.class);
		MockEnvironment environment = new MockEnvironment();
		ConfigInfo info = config("magic-api:\n  plugin:\n    mcp:\n      auth:\n        enabled: false\n");
		environment.getPropertySources().addFirst(new MapPropertySource("dev_app_mcp", java.util.Collections.<String, Object>singletonMap(
				"magic-api.plugin.mcp.auth.enabled", false)));
		ReflectionTestUtils.setField(registry, "eventPublisher", publisher);
		ReflectionTestUtils.setField(registry, "contextRefresher", contextRefresher);
		ReflectionTestUtils.setField(registry, "environment", environment);

		ReflectionTestUtils.invokeMethod(registry, "unregisterByEntity", info);

		verify(contextRefresher).refresh();
		assertThat(environment.getPropertySources().contains("dev_app_mcp")).isFalse();
		assertThat(publisher.events).hasSize(1);
		assertThat(publisher.events.get(0).getProperties()).containsEntry("magic-api.plugin.mcp.auth.enabled", false);
	}

	@Test
	void shouldPublishOriginalKeysWhenConfigIsDisabled() {
		ConfigMagicDynamicRegistry registry = registry();
		RecordingEventPublisher publisher = new RecordingEventPublisher();
		ContextRefresher contextRefresher = mock(ContextRefresher.class);
		MockEnvironment environment = new MockEnvironment();
		ConfigInfo info = config("magic-api:\n  plugin:\n    mcp:\n      headers:\n        session-id: Mcp-Session-Id\n");
		info.setEnabled(false);
		environment.getPropertySources().addFirst(new MapPropertySource("dev_app_mcp", java.util.Collections.<String, Object>singletonMap(
				"magic-api.plugin.mcp.headers.session-id", "Mcp-Session-Id")));
		ReflectionTestUtils.setField(registry, "eventPublisher", publisher);
		ReflectionTestUtils.setField(registry, "contextRefresher", contextRefresher);
		ReflectionTestUtils.setField(registry, "environment", environment);

		ReflectionTestUtils.invokeMethod(registry, "registerByEntity", info);

		verify(contextRefresher).refresh();
		assertThat(environment.getPropertySources().contains("dev_app_mcp")).isFalse();
		assertThat(publisher.events).hasSize(1);
		assertThat(publisher.events.get(0).getProperties()).containsEntry("magic-api.plugin.mcp.headers.session-id", "Mcp-Session-Id");
	}
```

- [ ] **Step 6: 修改 unregister 和 disabled 路径携带原 flatten key**

将 `unregisterByEntity(ConfigInfo info)` 中 refresh 替换为带原 properties 的 `refreshAndPublish(parseProperties(info))`：

```java
    private void unregisterByEntity(ConfigInfo info) {
        String propName = buildPropName(info);
        MutablePropertySources propertySources = this.environment.getPropertySources();
        if (propertySources.contains(propName)) {
            Map<String, Object> properties = parseProperties(info);
            propertySources.remove(propName);
            try {
                refreshAndPublish(properties);
            } catch (Exception e) {
                log.error("刷新上下文失败", e);
            }
        }
    }
```

在 `registerByEntity(ConfigInfo info)` 中，`map` 是原脚本 flatten 结果。对于 `info.isEnabled() == false` 或 `info.isExternalFlag() == true` 且原 property source 被移除的场景，`changed == true` 时调用 `refreshAndPublish(map)`，即可携带原 key。不要把 MCP 专属判断放入本类。

- [ ] **Step 7: 运行 Task 3 测试确认通过**

Run:

```bash
cd fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=ConfigMagicDynamicRegistryTest test
```

Expected: PASS，`ConfigMagicDynamicRegistryTest` 无 failure/error。

- [ ] **Step 8: 提交 Task 3**

Run:

```bash
git status --short
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/config/service/ConfigMagicDynamicRegistry.java fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/config/service/ConfigMagicDynamicRegistryTest.java
git commit -m "feat: publish config change events from magic config"
```

Expected: commit only includes MagicAPI config registry event publishing and tests.

### Task 4: 端到端分发链路与回归验证

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/notification/McpConfigurationChangeNotifierTest.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/starter/MagicAPIMcpConfigurationTest.java`

**Interfaces:**
- Consumes: `ConfigChangeEventListener` bean post-processing and `ConfigChangeEvent` dispatch。
- Produces: proof that `ConfigChangeEvent` with MCP keys invokes notifier through `@OnConfigChange` rather than only direct method calls。

- [ ] **Step 1: 写 `@OnConfigChange` 分发链路测试**

在 `McpConfigurationChangeNotifierTest.java` imports 中加入：

```java
import com.xiongx9527.fw.plus.boot.autoconfigure.config.event.ConfigChangeEvent;
import com.xiongx9527.fw.plus.boot.autoconfigure.config.event.ConfigChangeEventListener;
import java.util.Collections;
```

追加以下测试方法：

```java
	@Test
	void shouldReceiveFilteredPropertiesThroughOnConfigChangeListener() {
		RecordingNotificationService notificationService = new RecordingNotificationService();
		McpConfigurationChangeNotifier notifier = new McpConfigurationChangeNotifier(notificationService);
		ConfigChangeEventListener listener = new ConfigChangeEventListener();
		listener.postProcessAfterInitialization(notifier, "mcpConfigurationChangeNotifier");
		Map<String, Object> properties = new HashMap<>();
		properties.put("server.port", 8080);
		properties.put("magic-api.plugin.mcp.tools.functions.enabled", true);

		listener.onApplicationEvent(new ConfigChangeEvent(this, properties));

		assertThat(notificationService.methods).containsExactly("notifications/tools/list_changed");
	}

	@Test
	void shouldIgnoreConfigChangeEventWhenFilteredMcpMapIsEmpty() {
		RecordingNotificationService notificationService = new RecordingNotificationService();
		McpConfigurationChangeNotifier notifier = new McpConfigurationChangeNotifier(notificationService);
		ConfigChangeEventListener listener = new ConfigChangeEventListener();
		listener.postProcessAfterInitialization(notifier, "mcpConfigurationChangeNotifier");

		listener.onApplicationEvent(new ConfigChangeEvent(this, Collections.<String, Object>singletonMap("server.port", 8080)));

		assertThat(notificationService.methods).isEmpty();
	}
```

- [ ] **Step 2: 运行通知器和自动配置测试**

Run:

```bash
cd fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='McpConfigurationChangeNotifierTest,MagicAPIMcpConfigurationTest' test
```

Expected: PASS，新增分发链路测试、自动配置测试均通过。

- [ ] **Step 3: 运行 MCP/config 相关回归测试**

Run:

```bash
cd fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='McpConfigurationChangeNotifierTest,ConfigMagicDynamicRegistryTest,MagicAPIMcpConfigurationTest,McpNotificationServiceTest,McpClusterNotificationServiceTest,MagicNotifyMcpChangeEventPublisherTest,McpChangeNotificationRegistryTest,McpLifecycleHandlersTest,MagicMcpControllerTest,MagicMcpControllerAuthTest' test
```

Expected: PASS，所有列出的测试类无 failure/error。

- [ ] **Step 4: 运行 Boot autoconfigure 模块定向验证命令**

Run:

```bash
cd fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='*Mcp*Test,ConfigMagicDynamicRegistryTest,MagicAPIMcpConfigurationTest,MagicMcpControllerTest,MagicMcpControllerAuthTest' test
```

Expected: PASS，覆盖 MCP notification、starter、config registry、cluster broadcast、lifecycle、controller auth 相关测试。

- [ ] **Step 5: 检查未提交文件并提交测试补充**

Run:

```bash
git status --short
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/notification/McpConfigurationChangeNotifierTest.java fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/starter/MagicAPIMcpConfigurationTest.java
git commit -m "test: cover mcp config change event dispatch"
```

Expected: commit only includes Task 4 test refinements.

## 验收清单

- [ ] 新增 `McpConfigurationChangeNotifier` 并通过 `@OnConfigChange(prefix = "magic-api.plugin.mcp")` 接入 `ConfigChangeEventListener`。
- [ ] `tools.functions.*` 与 `tools.api.*` 配置变化触发一次 `notifications/tools/list_changed`。
- [ ] `protocol.*`、`auth.*`、`headers.*` 配置变化触发 tools、resources、prompts 三类 `list_changed`。
- [ ] 同一次刷新内相同 notification method 去重。
- [ ] 非 MCP key 与未覆盖 MCP key 不触发 MCP notification。
- [ ] `MagicAPIMcpConfiguration` 默认注册 `McpConfigurationChangeNotifier`，`magic-api.plugin.mcp.enabled=false` 时不注册。
- [ ] `ConfigMagicDynamicRegistry` 在 `ContextRefresher.refresh()` 完成后发布 `ConfigChangeEvent`。
- [ ] MagicAPI `config` 删除、禁用、取消注册时基于原脚本 flatten key 发布事件。
- [ ] MagicAPI config 插件没有 MCP 类依赖，MCP 插件禁用或缺失时配置刷新仍可工作。
- [ ] 使用 `cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='*Mcp*Test,ConfigMagicDynamicRegistryTest,MagicAPIMcpConfigurationTest,MagicMcpControllerTest,MagicMcpControllerAuthTest' test` 验证通过。

## 计划自检

- Spec coverage: OpenSpec 中 MCP tools/protocol/auth/headers 配置变化、非 MCP 忽略、同批去重、MagicAPI config 资源变化、MCP disabled/absent 兼容场景，分别由 Task 1、Task 2、Task 3、Task 4 覆盖。
- Placeholder scan: 本计划没有使用 `TBD`、`TODO`、`implement later`、`fill in details` 或无代码的泛化测试描述。
- Type consistency: `McpConfigurationChangeNotifier#onMcpConfigurationChanged(Map<String, Object>)`、`McpNotificationService.broadcast(Map<String, Object>)`、`ConfigChangeEvent(Object, Map<String, Object>)`、`ConfigMagicDynamicRegistry#registerByEntity(ConfigInfo)`、`unregisterByEntity(ConfigInfo)` 名称与当前代码或本计划定义一致。
