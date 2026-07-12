---
change: fix-magic-ws-console-query
design-doc: docs/superpowers/specs/2026-07-06-fix-magic-ws-console-query-design.md
base-ref: 761ee27494afe47f031884a62c684aa4951afc39
archived-with: 2026-07-06-fix-magic-ws-console-query
---

# Fix Magic WebSocket Console Query Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 Magic WebSocket 控制台数据源测试、订阅脚本执行、代码提示和脚本上下文标准一致性。

**Architecture:** 后端保持现有 `MagicWsController`、`BaseDynamicWebSocketHandler` 和 storage 结构，新增最小辅助 DTO/方法以支持控制台模拟执行与真实校验。前端只修复 WS 插件资源配置与 JavaClass hint，不新增 Producer 抽象或改变正式 WebSocket 连接协议。

**Tech Stack:** Java 8, Spring Boot 2.7.18, Magic-API 2.2.2, JUnit 5/Mockito, Maven Wrapper, Vue/Vite MagicAPI console plugin JavaScript。

## Global Constraints

- 使用简体中文文档和注释风格，保持现有项目命名。
- `fw-plus-core` 仍受 Java 1.8 约束；本次主要修改 `fw-plus-boot`，仍避免 Java 9+ API。
- 不新增外部依赖，不修改 MagicAPI、Spring Boot 或 JWT 版本。
- 不改变 WebSocket 正式连接 URL、token 参数、clientId 参数和消息协议。
- 控制台 `/ws/execute` 是模拟上下文脚本测试，不建立真实 WebSocket session。
- 每完成 OpenSpec `tasks.md` 中一个可验收任务后同步勾选。

archived-with: 2026-07-06-fix-magic-ws-console-query
---

## File Structure

- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws/web/MagicWsController.java`
  - 负责 `/ws/execute` 与 `/ws/datasource/test` 的真实控制台行为。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws/handler/BaseDynamicWebSocketHandler.java`
  - 负责 init、业务消息、close 脚本上下文兼容变量。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-ws/src/console/src/service/magic-ws-datasource.js`
  - 负责真实 `WsModule` JavaClass hint。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-ws/src/console/src/service/magic-ws-subscribe.js`
  - 负责 `requirePath` 与后端 storage 一致。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-ws/README.md`
  - 负责说明控制台测试语义与上下文变量。
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws/web/MagicWsControllerTest.java`
  - 覆盖控制台接口行为。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws/handler/BaseDynamicWebSocketHandlerTest.java`
  - 覆盖上下文变量兼容。
- Create or Modify: WS 前端插件静态校验测试文件，位置根据现有测试结构确定。
  - 覆盖旧包名、Producer 残留和 `requirePath` 不一致。

archived-with: 2026-07-06-fix-magic-ws-console-query
---

### Task 1: 后端控制台接口

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws/web/MagicWsController.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws/web/MagicWsControllerTest.java`
- Update checklist: `openspec/changes/fix-magic-ws-console-query/tasks.md`

**Interfaces:**
- Consumes: `WsSubscribeInfoMagicResourceStorage.findAll()`, `WsDataSourceInfoMagicResourceStorage.validate(...)`, `WsModule.generateToken(secret)`, `ScriptManager.executeScript(script, context)`。
- Produces: `/ws/execute` 返回脚本执行结果或失败信息；`/ws/datasource/test` 返回真实校验结果。

- [x] **Step 1: 编写 `/ws/execute` 失败优先测试**

在 `MagicWsControllerTest` 中创建测试，使用 mock storage 注入 controller。测试名称建议：

```java
@Test
void executeShouldReturnFailureWhenSubscribeScriptNotFound() {
    MagicWsController controller = new MagicWsController(null);
    // 通过反射或新增包级构造器注入 WsSubscribeInfoMagicResourceStorage mock
    // storage.findAll() 返回空列表
    JsonBean<Object> result = controller.execute("missing", null);
    assertThat(String.valueOf(result.getData())).contains("missing");
}
```

- [x] **Step 2: 编写 `/ws/execute` 成功执行测试**

准备一个 `WsSubscribeInfo`：`id=script-1`、`key=default`、`businessId=order-refresh`、`script="return type + ':' + businessType;"`。验证 controller 返回脚本结果，并确认上下文包含 `type` 与 `businessType`。如果 MagicScript 单测成本过高，抽出 `buildTestContext(WsSubscribeInfo, Map<String,Object>)` 包级方法并断言上下文变量。

- [x] **Step 3: 编写 `/ws/datasource/test` 校验测试**

覆盖三类：

```java
@Test
void datasourceTestShouldRejectMissingKey() { ... }

@Test
void datasourceTestShouldRejectInvalidOtherJson() { ... }

@Test
void datasourceTestShouldReturnOkWhenTokenCanBeGenerated() { ... }
```

- [x] **Step 4: 实现最小 controller 改动**

在 `MagicWsController` 中新增依赖获取方法，优先使用 Spring bean，测试中允许替换：

```java
WsSubscribeInfoMagicResourceStorage getWsSubscribeStorage() { ... }
WsDataSourceInfoMagicResourceStorage getWsDataSourceStorage() { ... }
BaseDynamicWebSocketHandler getWebSocketHandler() { ... }
```

`execute` 逻辑：查找 id → 构造 `MagicScriptContext` → 执行脚本 → `new JsonBean<>(result)`；异常时返回可读失败结果。

`test` 逻辑：调用 storage 校验或等价校验 → 创建 `new WsModule(handler, properties.getKey()).generateToken(properties.getSecret())` 或复用 token 生成校验 → 成功返回 `ok`。

- [x] **Step 5: 运行后端控制器测试**

Run:

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicWsControllerTest test
```

Expected: `MagicWsControllerTest` PASS。

- [x] **Step 6: 勾选 OpenSpec 任务 1.1、1.2、1.3**

在 `openspec/changes/fix-magic-ws-console-query/tasks.md` 勾选：

```markdown
- [x] 1.1 为 `/ws/execute` 增加按订阅脚本 id 加载、模拟上下文执行和错误返回逻辑
- [x] 1.2 为 `/ws/datasource/test` 增加必填字段、`other` JSON 和 token 生成能力校验
- [x] 1.3 补充 `MagicWsController` 单元测试覆盖成功、未知脚本、非法数据源和缺少 token secret 场景
```

- [x] **Step 7: 提交 Task 1**

```bash
git add openspec/changes/fix-magic-ws-console-query fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws/web/MagicWsController.java fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws/web/MagicWsControllerTest.java
git commit -m "fix: implement magic ws console backend tests"
```

archived-with: 2026-07-06-fix-magic-ws-console-query
---

### Task 2: 脚本上下文兼容

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws/handler/BaseDynamicWebSocketHandler.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws/handler/BaseDynamicWebSocketHandlerTest.java`
- Update checklist: `openspec/changes/fix-magic-ws-console-query/tasks.md`

**Interfaces:**
- Consumes: existing private `init`, `handleSubscribeMessage`, `end` execution paths。
- Produces: all script contexts expose compatible `type` and `businessType` variables。

- [x] **Step 1: 编写业务消息上下文测试**

在 `BaseDynamicWebSocketHandlerTest` 中构造匹配 `businessId="order-refresh"` 的订阅脚本，脚本或 mock 断言上下文包含：

```java
assertThat(context.get("type")).isEqualTo("order-refresh");
assertThat(context.get("businessType")).isEqualTo("order-refresh");
assertThat(context.get("params")).isNotNull();
```

如果现有测试无法直接拦截 `MagicScriptContext`，优先抽出包级 `populateSubscribeContext(...)` 辅助方法后测试该方法。

- [x] **Step 2: 编写 init/close 上下文测试**

覆盖 init 和 close 的 matched `businessId`：

```java
assertThat(context.get("type")).isEqualTo("init");
assertThat(context.get("businessType")).isEqualTo("init");
```

close 同理为 `close`。

- [x] **Step 3: 实现上下文注入辅助方法**

在 `BaseDynamicWebSocketHandler` 中新增小方法避免三处重复：

```java
private void putBusinessType(MagicScriptContext context, String businessType) {
    context.set("type", businessType);
    context.set("businessType", businessType);
}
```

在 init、业务消息、close 三处调用该方法。业务消息继续设置 `params`。

- [x] **Step 4: 运行 handler 测试**

Run:

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=BaseDynamicWebSocketHandlerTest test
```

Expected: `BaseDynamicWebSocketHandlerTest` PASS。

- [x] **Step 5: 勾选 OpenSpec 任务 2.1、2.2、2.3、2.4**

在 `openspec/changes/fix-magic-ws-console-query/tasks.md` 勾选对应 4 项。

- [x] **Step 6: 提交 Task 2**

```bash
git add openspec/changes/fix-magic-ws-console-query fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws/handler/BaseDynamicWebSocketHandler.java fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws/handler/BaseDynamicWebSocketHandlerTest.java
git commit -m "fix: align magic ws script context variables"
```

archived-with: 2026-07-06-fix-magic-ws-console-query
---

### Task 3: 前端控制台插件

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-ws/src/console/src/service/magic-ws-datasource.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-ws/src/console/src/service/magic-ws-subscribe.js`
- Create or Modify: frontend static test under existing test location if present
- Update checklist: `openspec/changes/fix-magic-ws-console-query/tasks.md`

**Interfaces:**
- Consumes: backend `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.module.WsModule`。
- Produces: editor no longer references old package names or nonexistent producer classes；`ws-subscribe.requirePath` matches backend false。

- [x] **Step 1: 编写静态校验命令或测试**

如果该 console 包没有测试框架，使用 shell 静态校验作为任务验证：

```bash
! rg "com\.jugui|WsProducer|extra === 'producer'" fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-ws/src/console/src/service
rg "requirePath: false" fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-ws/src/console/src/service/magic-ws-subscribe.js
```

Expected: 第一条无匹配；第二条匹配 `requirePath: false`。

- [x] **Step 2: 修复 `magic-ws-datasource.js`**

将 JavaClass hint 改为真实类名：

```javascript
const WS_MODULE_CLASS = 'com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.module.WsModule'
```

移除 `extra === 'producer'` 和 `WsProducer`，返回基于 `ws-datasource` key 的真实模块提示；如 MagicAPI JavaClass 只接受 Java 类型，使用 `WS_MODULE_CLASS` 作为 `type`。

- [x] **Step 3: 修复 `magic-ws-subscribe.js`**

将：

```javascript
requirePath: true,
```

改为：

```javascript
requirePath: false,
```

- [x] **Step 4: 运行静态校验**

Run:

```bash
! rg "com\.jugui|WsProducer|extra === 'producer'" fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-ws/src/console/src/service
rg "requirePath: false" fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-ws/src/console/src/service/magic-ws-subscribe.js
```

Expected: PASS。

- [x] **Step 5: 勾选 OpenSpec 任务 3.1、3.2、3.3、3.4**

在 `openspec/changes/fix-magic-ws-console-query/tasks.md` 勾选对应 4 项。

- [x] **Step 6: 提交 Task 3**

```bash
git add openspec/changes/fix-magic-ws-console-query fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-ws/src/console/src/service/magic-ws-datasource.js fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-ws/src/console/src/service/magic-ws-subscribe.js
git commit -m "fix: align magic ws console metadata"
```

archived-with: 2026-07-06-fix-magic-ws-console-query
---

### Task 4: 文档与最终验证

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-ws/README.md`
- Update checklist: `openspec/changes/fix-magic-ws-console-query/tasks.md`

**Interfaces:**
- Consumes: implemented backend/frontend behavior。
- Produces: README documents console test semantics and compatibility variables；all change tasks checked。

- [x] **Step 1: 更新 README 控制台测试说明**

在 README 中补充：

```markdown
### 控制台测试说明

- `ws-datasource` 测试会校验必填字段、`other` JSON 和 token 生成前置条件。
- `ws-subscribe` 运行按钮使用模拟 WebSocket 上下文执行当前脚本，不会建立真实 WebSocket 连接。
- 模拟上下文包含 `appId`、`clientId`、`sessionId`、`type`、`businessType`、`params`。
- 推荐新脚本使用 `type`，`businessType` 作为兼容别名保留。
```

- [x] **Step 2: 运行 OpenSpec 校验**

Run:

```bash
openspec validate fix-magic-ws-console-query
```

Expected: `Change 'fix-magic-ws-console-query' is valid`。

- [x] **Step 3: 运行 WS 相关 Maven 测试**

Run:

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='*WebSocket*Test,*Ws*Test,MagicWsControllerTest' test
```

Expected: PASS。若 surefire 通配不兼容，改用明确测试类列表。

- [x] **Step 4: 勾选 OpenSpec 任务 4.1、4.2、4.3**

在 `openspec/changes/fix-magic-ws-console-query/tasks.md` 勾选对应 3 项。

- [x] **Step 5: 提交 Task 4**

```bash
git add openspec/changes/fix-magic-ws-console-query fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-ws/README.md
git commit -m "docs: document magic ws console test behavior"
```

archived-with: 2026-07-06-fix-magic-ws-console-query
---

## Self-Review

- Spec coverage: 控制台数据源校验由 Task 1/4 覆盖；订阅脚本执行由 Task 1 覆盖；编辑器元数据一致性由 Task 3 覆盖；上下文兼容由 Task 2/4 覆盖。
- Placeholder scan: 本计划不包含 TBD/TODO/implement later 占位任务。
- Type consistency: `type` 与 `businessType` 在 controller 模拟执行和 handler 运行时执行中保持一致；真实类名统一为 `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.module.WsModule`。
