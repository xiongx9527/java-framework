---
change: expand-mcp-config-notifications
design-doc: docs/superpowers/specs/2026-07-11-expand-mcp-config-notifications-design.md
base-ref: d496155c8a91fcc307cfb055e86a65e7f8d24623
---

# 扩展 MCP 配置通知覆盖面实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按 `docs/superpowers/specs/2026-07-11-expand-mcp-config-notifications-design.md` 扩展 Boot 层 MCP 配置变化通知器，让客户端可见的新增 MCP 配置 key 触发既有 list_changed 通知，同时固定去重和忽略边界。

**Architecture:** 只改 `McpConfigurationChangeNotifier` 的 key → notification method 映射和它的单元测试；不改配置事件链路、SSE/集群广播或 MCP 协议结构。沿用当前 `LinkedHashSet` 去重和发送顺序，新增 4 个精确 key 判断，不引入策略类、枚举表或外部配置。

**Tech Stack:** Java 8、Spring Boot 2.7.18、JUnit 5、AssertJ、Maven Wrapper/Boot autoconfigure 模块。

## Global Constraints

- Canonical design doc: `docs/superpowers/specs/2026-07-11-expand-mcp-config-notifications-design.md`。
- OpenSpec change: `openspec/changes/expand-mcp-config-notifications/`。
- 只修改 Boot MCP notifier 及相关测试。
- 不修改 `ConfigMagicDynamicRegistry`、`ConfigChangeEvent`、`ConfigChangeEventListener`。
- 不修改 `McpNotificationService`、SSE stream、MagicNotify cluster。
- 不新增 MCP JSON-RPC 通知类型，不改变协议结构。
- 不调整前端控制台、Cloud、AI、Product 层代码。
- 不把所有 `magic-api.plugin.mcp.*` key 自动映射为通知。
- Java 8 兼容；不要使用 `List.of`、`Map.of`、`Set.of`、var、Stream API 中 Java 9+ 功能。
- 每个 OpenSpec task 验收后按 Comet build 规则勾选 `openspec/changes/expand-mcp-config-notifications/tasks.md` 并单独提交。

---

## File Structure

- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/notification/McpConfigurationChangeNotifier.java`
  - 责任：维护 MCP 配置 key 到既有 MCP list_changed notification method 的映射。
  - 本次只新增 4 个精确 key 常量和一个私有判断方法。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/notification/McpConfigurationChangeNotifierTest.java`
  - 责任：验证新增映射、既有映射、同批去重、忽略 key、`@OnConfigChange` 过滤边界。
  - 本次复用现有 `RecordingNotificationService`，不新增 mock 框架。
- Reference only: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/starter/MagicAPIMcpProperties.java`
  - 责任：确认实际 relaxed-binding 配置 key 来自 `enabled`、`endpoint`、`sessionTimeoutSeconds`、`sseStreamEnabled`、`maxRequestBytes`、`maxResponseChars`、`trustedOrigins`、`auth.*`、`headers.*`、`protocol.*`、`tools.functions.*`、`tools.api.*`。
  - 只读，不修改。
- Reference only: `openspec/changes/expand-mcp-config-notifications/tasks.md`
  - 责任：任务完成后的 Comet/OpenSpec 勾选来源；执行计划时不要提前勾选未完成项。

## Mapping to Preserve

按设计文档“保留既有映射”和“新增精确 key 映射”执行：

| Key | 通知 |
| --- | --- |
| `magic-api.plugin.mcp.tools.functions.*` | `notifications/tools/list_changed` |
| `magic-api.plugin.mcp.tools.api.*` | `notifications/tools/list_changed` |
| `magic-api.plugin.mcp.protocol.*` | tools/resources/prompts 三类 list_changed |
| `magic-api.plugin.mcp.auth.*` | tools/resources/prompts 三类 list_changed |
| `magic-api.plugin.mcp.headers.*` | tools/resources/prompts 三类 list_changed |
| `magic-api.plugin.mcp.endpoint` | tools/resources/prompts 三类 list_changed |
| `magic-api.plugin.mcp.session-timeout-seconds` | tools/resources/prompts 三类 list_changed |
| `magic-api.plugin.mcp.sse-stream-enabled` | tools/resources/prompts 三类 list_changed |
| `magic-api.plugin.mcp.trusted-origins` | tools/resources/prompts 三类 list_changed |
| `magic-api.plugin.mcp.enabled` | 不通知 |
| `magic-api.plugin.mcp.max-request-bytes` | 不通知 |
| `magic-api.plugin.mcp.max-response-chars` | 不通知 |
| 未列出的 `magic-api.plugin.mcp.*` | 不通知 |
| 非 MCP key | 不通知 |

## Task 1: 固定新增精确 key 与忽略边界测试

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/notification/McpConfigurationChangeNotifierTest.java`
- Reference: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/starter/MagicAPIMcpProperties.java`
- Reference: `docs/superpowers/specs/2026-07-11-expand-mcp-config-notifications-design.md`

**Interfaces:**
- Consumes: 当前 `McpConfigurationChangeNotifier#onMcpConfigurationChanged(Map<String, Object> properties)`。
- Produces: 测试方法 `shouldNotifyAllCapabilityListsWhenEndpointConfigChanges()`、`shouldNotifyAllCapabilityListsWhenSessionAndTransportConfigChanges()`、`shouldNotifyAllCapabilityListsWhenTrustedOriginsConfigChanges()`、`shouldIgnoreNonClientVisibleAndUnknownMcpConfigKeys()`，供 Task 2 实现驱动。

- [x] **Step 1: 确认属性树和 key 命名**

  检查 `MagicAPIMcpProperties` 当前字段，确认这些字段对应 Spring relaxed binding key：

  ```text
  enabled -> magic-api.plugin.mcp.enabled
  endpoint -> magic-api.plugin.mcp.endpoint
  sessionTimeoutSeconds -> magic-api.plugin.mcp.session-timeout-seconds
  sseStreamEnabled -> magic-api.plugin.mcp.sse-stream-enabled
  maxRequestBytes -> magic-api.plugin.mcp.max-request-bytes
  maxResponseChars -> magic-api.plugin.mcp.max-response-chars
  trustedOrigins -> magic-api.plugin.mcp.trusted-origins
  auth.* -> magic-api.plugin.mcp.auth.*
  headers.* -> magic-api.plugin.mcp.headers.*
  protocol.* -> magic-api.plugin.mcp.protocol.*
  tools.functions.* -> magic-api.plugin.mcp.tools.functions.*
  tools.api.* -> magic-api.plugin.mcp.tools.api.*
  ```

  Expected: 与设计文档第 84-105 行一致；若字段名已变化，先停下并更新本计划对应 key，再继续。

- [x] **Step 2: 替换旧的忽略测试**

  在 `McpConfigurationChangeNotifierTest.java` 中删除当前方法 `shouldIgnoreNonMcpAndUncoveredMcpConfigKeys()`，因为它把 `magic-api.plugin.mcp.endpoint` 当作忽略项。用下面的新测试替代：

  ```java
  	@Test
  	void shouldIgnoreNonClientVisibleAndUnknownMcpConfigKeys() {
  		RecordingNotificationService notificationService = new RecordingNotificationService();
  		McpConfigurationChangeNotifier notifier = new McpConfigurationChangeNotifier(notificationService);
  		Map<String, Object> properties = new HashMap<>();
  		properties.put("server.port", 8080);
  		properties.put("magic-api.plugin.mcp.enabled", true);
  		properties.put("magic-api.plugin.mcp.max-request-bytes", 1024);
  		properties.put("magic-api.plugin.mcp.max-response-chars", 4096);
  		properties.put("magic-api.plugin.mcp.logging.level", "debug");
  
  		notifier.onMcpConfigurationChanged(properties);
  
  		assertThat(notificationService.methods).isEmpty();
  	}
  ```

- [x] **Step 3: 新增 endpoint 测试**

  在 `shouldNotifyAllCapabilityListsWhenHeaderConfigChanges()` 后添加：

  ```java
  	@Test
  	void shouldNotifyAllCapabilityListsWhenEndpointConfigChanges() {
  		RecordingNotificationService notificationService = new RecordingNotificationService();
  		McpConfigurationChangeNotifier notifier = new McpConfigurationChangeNotifier(notificationService);
  		Map<String, Object> properties = new HashMap<>();
  		properties.put("magic-api.plugin.mcp.endpoint", "/mcp-v2");
  
  		notifier.onMcpConfigurationChanged(properties);
  
  		assertThat(notificationService.methods).containsExactly(
  				"notifications/tools/list_changed",
  				"notifications/resources/list_changed",
  				"notifications/prompts/list_changed");
  	}
  ```

- [x] **Step 4: 新增 session/transport 测试**

  在 endpoint 测试后添加：

  ```java
  	@Test
  	void shouldNotifyAllCapabilityListsWhenSessionAndTransportConfigChanges() {
  		RecordingNotificationService notificationService = new RecordingNotificationService();
  		McpConfigurationChangeNotifier notifier = new McpConfigurationChangeNotifier(notificationService);
  		Map<String, Object> properties = new HashMap<>();
  		properties.put("magic-api.plugin.mcp.session-timeout-seconds", 60);
  		properties.put("magic-api.plugin.mcp.sse-stream-enabled", false);
  
  		notifier.onMcpConfigurationChanged(properties);
  
  		assertThat(notificationService.methods).containsExactly(
  				"notifications/tools/list_changed",
  				"notifications/resources/list_changed",
  				"notifications/prompts/list_changed");
  	}
  ```

- [x] **Step 5: 新增 trusted-origins 测试**

  在 session/transport 测试后添加：

  ```java
  	@Test
  	void shouldNotifyAllCapabilityListsWhenTrustedOriginsConfigChanges() {
  		RecordingNotificationService notificationService = new RecordingNotificationService();
  		McpConfigurationChangeNotifier notifier = new McpConfigurationChangeNotifier(notificationService);
  		Map<String, Object> properties = new HashMap<>();
  		properties.put("magic-api.plugin.mcp.trusted-origins", "https://example.com");
  
  		notifier.onMcpConfigurationChanged(properties);
  
  		assertThat(notificationService.methods).containsExactly(
  				"notifications/tools/list_changed",
  				"notifications/resources/list_changed",
  				"notifications/prompts/list_changed");
  	}
  ```

- [x] **Step 6: 运行测试确认失败**

  Run:

  ```bash
  cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-boot && \
  ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure \
    -Dtest=McpConfigurationChangeNotifierTest test
  ```

  Expected: 新增 3 个 notify 测试失败，失败信息显示 `notificationService.methods` 为空；忽略测试通过。

- [x] **Step 7: 提交失败测试**

  ```bash
  cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework && \
  git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/notification/McpConfigurationChangeNotifierTest.java && \
  git commit -m "test: cover expanded mcp config notifications"
  ```

  Expected: 提交成功。若项目策略不允许提交失败测试，跳过本提交，把 Task 1 与 Task 2 合并为一个提交；不要勾选 OpenSpec 2.1 前先确认失败输出。

## Task 2: 扩展通知器精确 key 映射并验证去重

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/notification/McpConfigurationChangeNotifier.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/notification/McpConfigurationChangeNotifierTest.java`

**Interfaces:**
- Consumes: Task 1 测试方法；现有 `McpNotificationService.toolsListChanged()`、`resourcesListChanged()`、`promptsListChanged()`。
- Produces: 私有方法 `isCapabilityViewKey(String key): boolean`；`collectNotificationMethods` 对新增 4 个精确 key 添加 tools/resources/prompts 三类通知。

- [ ] **Step 1: 修改通知器常量和判断方法**

  在 `McpConfigurationChangeNotifier.java` 的 `HEADERS_PREFIX` 后添加精确 key 常量：

  ```java
  	private static final String ENDPOINT_KEY = MCP_PREFIX + ".endpoint";
  	private static final String SESSION_TIMEOUT_SECONDS_KEY = MCP_PREFIX + ".session-timeout-seconds";
  	private static final String SSE_STREAM_ENABLED_KEY = MCP_PREFIX + ".sse-stream-enabled";
  	private static final String TRUSTED_ORIGINS_KEY = MCP_PREFIX + ".trusted-origins";
  ```

  将 `collectNotificationMethods` 改成：

  ```java
  	private void collectNotificationMethods(String key, Set<String> methods) {
  		if (key == null) {
  			return;
  		}
  		if (key.startsWith(FUNCTIONS_TOOLS_PREFIX) || key.startsWith(API_TOOLS_PREFIX)) {
  			methods.add(TOOLS_LIST_CHANGED);
  			return;
  		}
  		if (key.startsWith(PROTOCOL_PREFIX) || key.startsWith(AUTH_PREFIX) || key.startsWith(HEADERS_PREFIX)
  				|| isCapabilityViewKey(key)) {
  			methods.add(TOOLS_LIST_CHANGED);
  			methods.add(RESOURCES_LIST_CHANGED);
  			methods.add(PROMPTS_LIST_CHANGED);
  		}
  	}
  
  	private boolean isCapabilityViewKey(String key) {
  		return ENDPOINT_KEY.equals(key)
  				|| SESSION_TIMEOUT_SECONDS_KEY.equals(key)
  				|| SSE_STREAM_ENABLED_KEY.equals(key)
  				|| TRUSTED_ORIGINS_KEY.equals(key);
  	}
  ```

- [ ] **Step 2: 扩展同批去重测试**

  在 `McpConfigurationChangeNotifierTest.java` 的 `shouldDeduplicateNotificationMethodsWithinOneRefresh()` 中，把 properties 初始化块改成：

  ```java
  		Map<String, Object> properties = new HashMap<>();
  		properties.put("magic-api.plugin.mcp.tools.functions.enabled", true);
  		properties.put("magic-api.plugin.mcp.tools.api.enabled", true);
  		properties.put("magic-api.plugin.mcp.auth.enabled", false);
  		properties.put("magic-api.plugin.mcp.endpoint", "/mcp-v2");
  		properties.put("magic-api.plugin.mcp.sse-stream-enabled", false);
  		properties.put("magic-api.plugin.mcp.max-request-bytes", 1024);
  ```

  Expected: 仍然只发送三类 method 各一次，顺序保持 tools、resources、prompts。

- [ ] **Step 3: 运行通知器单测确认通过**

  Run:

  ```bash
  cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-boot && \
  ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure \
    -Dtest=McpConfigurationChangeNotifierTest test
  ```

  Expected: `Tests run: 10` 或更多，`Failures: 0, Errors: 0`。

- [ ] **Step 4: 勾选 OpenSpec 通知器相关任务**

  仅当 Step 3 通过后，编辑 `openspec/changes/expand-mcp-config-notifications/tasks.md`，把这些项改为已完成：

  ```markdown
  - [x] 2.1 为新增覆盖 key 编写 `McpConfigurationChangeNotifierTest` 失败用例，覆盖对应 tools/resources/prompts 或能力视图刷新通知。
  - [x] 2.2 扩展 `McpConfigurationChangeNotifier` 的 key 前缀识别和通知映射，保持 Java 8 兼容和确定性顺序。
  - [x] 2.3 补充同批刷新内新增 key 与既有 key 混合时的去重测试。
  - [x] 2.4 补充未纳入 MCP key 和非 MCP key 不触发通知的回归测试。
  ```

  Do not mark section 1 or 3 yet.

- [ ] **Step 5: 提交通知器实现**

  ```bash
  cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework && \
  git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/notification/McpConfigurationChangeNotifier.java \
    fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/notification/McpConfigurationChangeNotifierTest.java \
    openspec/changes/expand-mcp-config-notifications/tasks.md && \
  git commit -m "feat: expand mcp config change notifications"
  ```

  Expected: 提交成功，diff 只包含上述 3 个文件。

## Task 3: 完成属性影响面确认与定向回归

**Files:**
- Modify: `openspec/changes/expand-mcp-config-notifications/tasks.md`
- Reference: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/starter/MagicAPIMcpProperties.java`
- Reference: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/core/McpLifecycleHandlers.java`
- Reference: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/web/McpSseStreamService.java`
- Reference: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/session/McpSessionService.java`
- Reference: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/auth/SaTokenMcpAuthService.java`
- Reference: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/logging/McpLoggingHandler.java`
- Reference: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/starter/MagicAPIMcpConfigurationTest.java`
- Reference: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/config/service/ConfigMagicDynamicRegistryTest.java`

**Interfaces:**
- Consumes: Task 2 已通过的 notifier 行为。
- Produces: OpenSpec section 1 和 3 的完成勾选；定向测试证据。

- [ ] **Step 1: 对照使用点确认纳入和忽略理由**

  用代码阅读确认下表，不新增文档文件；确认结果体现在 OpenSpec task 勾选和提交说明中：

  ```text
  endpoint: transport endpoint，客户端交互入口变化 -> 三类 list_changed
  session-timeout-seconds: session 存活假设变化 -> 三类 list_changed
  sse-stream-enabled: SSE stream 交互能力变化 -> 三类 list_changed
  trusted-origins: 跨域信任边界变化 -> 三类 list_changed
  protocol.*: 协议版本视图变化 -> 保持三类 list_changed
  auth.*: 认证/作用域语义变化 -> 保持三类 list_changed
  headers.*: header 名称语义变化 -> 保持三类 list_changed
  tools.functions.* / tools.api.*: tool 暴露范围变化 -> 保持 tools list_changed
  enabled: 自动配置生命周期，不用 list_changed 表达 -> 忽略
  max-request-bytes / max-response-chars: 服务端保护限制，不改变 list 视图 -> 忽略
  logging/completion/provider/handler 未列出 key: 非 list 视图变更或当前无配置属性 -> 忽略
  ```

  Expected: 没发现除 4 个新增精确 key 之外还必须纳入的 client-visible 配置 key。若发现新的必须纳入 key，停止并回到设计确认，不要擅自扩大范围。

- [ ] **Step 2: 运行 Design Doc 指定定向回归**

  Run:

  ```bash
  cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-boot && \
  ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure \
    -Dtest='McpConfigurationChangeNotifierTest,ConfigMagicDynamicRegistryTest,MagicAPIMcpConfigurationTest' test
  ```

  Expected: 三组测试全部通过，`Failures: 0, Errors: 0`。

- [ ] **Step 3: 如测试类路径不匹配，使用当前模块实际测试名重跑**

  只有当 Step 2 因测试名匹配不到而失败时，先定位实际测试类名，再运行等价命令。保持范围只覆盖 MCP/config 相关测试，不扩大到全模块：

  ```bash
  cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-boot && \
  find fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java -name '*Mcp*Test.java' -o -name 'ConfigMagicDynamicRegistryTest.java'
  ```

  Expected: 找到 `McpConfigurationChangeNotifierTest`、`MagicAPIMcpConfigurationTest`、`ConfigMagicDynamicRegistryTest` 或它们的当前等价名称。随后用 `-Dtest='实际类名1,实际类名2,实际类名3' test` 重跑并通过。

- [ ] **Step 4: 勾选 OpenSpec 影响面与验证任务**

  仅当 Step 1 确认完成且 Step 2/3 测试通过后，编辑 `openspec/changes/expand-mcp-config-notifications/tasks.md`，把这些项改为已完成：

  ```markdown
  - [x] 1.1 对照 `MagicAPIMcpProperties` 梳理当前 `magic-api.plugin.mcp.*` 属性树，标记已覆盖、候选新增和明确忽略的 key 前缀。
  - [x] 1.2 追踪候选新增 key 在 lifecycle、transport/session、logging、completion、provider/handler 中的使用点，确认是否影响客户端可见能力视图或交互语义。
  - [x] 1.3 形成最终 key → notification method 映射清单，并记录不纳入通知的 MCP key 理由。
  - [x] 3.1 运行 `McpConfigurationChangeNotifierTest`，确认新增映射、去重和忽略逻辑通过。
  - [x] 3.2 运行 `MagicAPIMcpConfigurationTest` 和 `ConfigMagicDynamicRegistryTest`，确认 MCP disabled/absent 与 MagicAPI config 通用事件链路不回归。
  - [x] 3.3 运行 Boot autoconfigure 模块中 MCP/config 相关定向测试，确认现有通知、集群广播和 lifecycle 行为未回归。
  ```

- [ ] **Step 5: 提交验证勾选**

  ```bash
  cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework && \
  git add openspec/changes/expand-mcp-config-notifications/tasks.md && \
  git commit -m "chore: verify expanded mcp config notifications"
  ```

  Expected: 提交成功；工作区中只有用户原有未跟踪文件或后续流程文件。

## Self-Review

- Spec coverage: 覆盖新增 4 个精确 key、既有 tools/protocol/auth/headers 映射、同批 method 去重、明确忽略 enabled/max-request/max-response/未知 MCP/非 MCP key、定向回归命令。
- Placeholder scan: 未使用 TBD/TODO/implement later/similar to 等占位描述；每个代码变更步骤给出具体代码块或具体编辑文本。
- Type consistency: 仅使用现有 `McpConfigurationChangeNotifier#onMcpConfigurationChanged(Map<String, Object>)`、`RecordingNotificationService.methods`、`McpNotificationService.*ListChanged()`；新增私有方法签名为 `isCapabilityViewKey(String key)`，不对外暴露。
