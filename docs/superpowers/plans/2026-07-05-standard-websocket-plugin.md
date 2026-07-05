---
archived-with: 2026-07-05-standardize-websocket-plugin
status: final
---
# Standard WebSocket Plugin Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 MagicAPI WS 插件整改为标准 WebSocket 行为，并补齐缺失的连接、消息、错误、生命周期、推送、集群和文档能力。

**Architecture:** 采用“协议标准化内核 + 兼容层”：协议内核处理握手、控制帧、消息类型、关闭码、错误响应和会话生命周期；兼容层保留现有文本 `ping` 与 `{type: ...}` MagicAPI 业务消息模型。所有生命周期清理进入统一幂等路径，服务端推送和 Redis 集群广播保持现有 API 不退化。

**Tech Stack:** Java 8, Spring Boot 2.7.x, Spring WebSocket, MagicAPI 2.2.2, JUnit 5, Mockito, Maven.

## Global Constraints

- 输出和文档使用简体中文。
- Java 目标为 1.8，不使用 Java 9+ API。
- 不引入 STOMP、SockJS、Socket.IO、MQTT 或新重量级依赖。
- 不修改 MagicAPI 核心资源模型，保留 `ws-datasource`、`ws-subscribe`、`ws` 模块主要 API。
- 不标准行为允许整改；明显冲突时优先 WebSocket 标准语义。
- 当前未跟踪的 `.DS_Store` 和无关 archive change 不属于本计划，不能删除或修改。

---

## File Structure

- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws/handler/BaseDynamicWebSocketHandler.java`
  - 负责协议消息分流、错误响应、关闭码、会话注册/清理、生命周期脚本幂等、服务端推送本地投递。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws/handler/RedisDynamicWebSocketHandler.java`
  - 保持 Redis 集群广播，补齐自身消息跳过、非法广播消息忽略、本地投递不回环。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws/starter/MagicAPIWsConfiguration.java`
  - 负责握手注册、Origin、subprotocol 配置与 handler/interceptor 串联。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws/common/BaseService.java`
  - 如有需要，集中新增配置读取和错误/关闭常量辅助方法。
- Modify/Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws/**`
  - 覆盖协议、生命周期、配置、资源存储和集群广播行为。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-ws/README.md`
  - 更新标准对接、错误格式、关闭码、Ping/Pong、浏览器限制和缺失功能说明。
- Modify: `openspec/changes/standardize-websocket-plugin/tasks.md`
  - 每完成一个 OpenSpec 任务后勾选。

---

### Task 1: 协议消息契约测试

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws/handler/BaseDynamicWebSocketHandlerTest.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws/handler/BaseDynamicWebSocketHandler.java`

**Interfaces:**
- Consumes: Spring `WebSocketMessage` subclasses: `TextMessage`, `PingMessage`, `PongMessage`, `BinaryMessage`.
- Produces: handler behavior where `PingMessage` sends `PongMessage`, text `ping` sends text `pong`, `BinaryMessage` closes with `CloseStatus.NOT_ACCEPTABLE` or equivalent 1003.

- [x] **Step 1: 写标准控制帧失败测试**

Add tests that create a mock/open `WebSocketSession`, call `handleMessage(session, new PingMessage(payload))`, and assert `sendMessage` receives a `PongMessage` without invoking business script lookup.

- [x] **Step 2: 写文本 ping 兼容失败测试**

Add a test for `new TextMessage("ping")` asserting text `pong` is sent and no script dispatch happens.

- [x] **Step 3: 写 Binary 拒绝失败测试**

Add a test for `new BinaryMessage(new byte[] {1})` asserting the session closes with code `1003` and no script dispatch happens.

- [x] **Step 4: 运行测试确认失败**

Run: `cd fw-plus-boot && mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=BaseDynamicWebSocketHandlerTest test`
Expected: new tests fail because current handler lacks full standard frame handling.

- [x] **Step 5: 最小实现控制帧分流**

Update `BaseDynamicWebSocketHandler.handleMessage` to branch by message class before text business handling: Ping → Pong, Pong → no-op, Binary → close 1003, Text → existing text path.

- [x] **Step 6: 运行测试确认通过**

Run the same targeted Maven command. Expected: PASS for the new protocol frame tests.

---

### Task 2: 业务错误结构化响应测试与实现

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws/handler/BaseDynamicWebSocketHandlerTest.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws/handler/BaseDynamicWebSocketHandler.java`

**Interfaces:**
- Consumes: text business messages.
- Produces: JSON text error with fields `type`, `code`, `message`; suggested codes `INVALID_JSON`, `MISSING_TYPE`, `UNKNOWN_TYPE`, `SCRIPT_ERROR`.

- [x] **Step 1: 写非法 JSON 错误响应测试**

Send text that is not `ping` and not JSON. Assert handler sends `{"type":"error",..."code":"INVALID_JSON"...}` and keeps session open.

- [x] **Step 2: 写缺少 type 错误响应测试**

Send `{}` or `{"type":""}`. Assert code `MISSING_TYPE` and no script execution.

- [x] **Step 3: 写未知 type 错误响应测试**

Mock subscribe storage to return no matching enabled script. Send `{"type":"missing"}`. Assert code `UNKNOWN_TYPE` and connection remains open.

- [x] **Step 4: 运行测试确认失败**

Run: `cd fw-plus-boot && mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=BaseDynamicWebSocketHandlerTest test`
Expected: tests fail on missing structured response.

- [x] **Step 5: 实现统一错误响应方法**

Add a private helper such as `sendErrorMessage(WebSocketSession session, String code, String message)` using existing serialized text send path. Refactor text business handling to use it for recoverable errors.

- [x] **Step 6: 运行测试确认通过**

Run the same targeted Maven command. Expected: PASS.

---

### Task 3: 生命周期幂等与清理统一

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws/handler/BaseDynamicWebSocketHandlerTest.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws/handler/BaseDynamicWebSocketHandler.java`

**Interfaces:**
- Consumes: session attributes containing appId/clientId/session state.
- Produces: one registration path, one cleanup path, `close` script at most once per session.

- [x] **Step 1: 写 close 脚本只执行一次测试**

Create a registered session, trigger cleanup path and `afterConnectionClosed`, assert close script dispatch occurs once.

- [x] **Step 2: 写失败连接不触发生命周期脚本测试**

Simulate missing/invalid sessionInfo or failed registration path, assert `init` and `close` scripts are not executed.

- [x] **Step 3: 运行测试确认失败**

Run targeted `BaseDynamicWebSocketHandlerTest`. Expected: fail if current cleanup can call close script multiple times or lacks explicit guard.

- [x] **Step 4: 实现生命周期状态标记**

Store flags in session attributes, for example `wsRegistered`, `wsCloseScriptExecuted`; centralize cleanup in a single method that atomically checks and sets close execution state.

- [x] **Step 5: 替换重复清理调用点**

Make `afterConnectionClosed`, `cleanupByAppId`, cache eviction and shutdown use the unified cleanup method without duplicating close script calls.

- [x] **Step 6: 运行测试确认通过**

Run targeted test. Expected: PASS.

---

### Task 4: 握手、Origin、Subprotocol 与缺失功能配置

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws/starter/MagicAPIWsHandlerConfigurationTest.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws/starter/MagicAPIWsConfiguration.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws/common/BaseService.java`

**Interfaces:**
- Consumes: `magic-api.plugin.ws.websocket.trustedDomain`, `websocket.subProtocols`, token/clientId param names.
- Produces: strict Origin default, explicit wildcard only, subprotocol trim/filter, handler registration matching `magic-api.web + websocket.path`.

- [x] **Step 1: 写 subprotocol 注册测试**

Assert configured `" json, magic , "` becomes `json`, `magic` and is passed to `DefaultHandshakeHandler#setSupportedProtocols` or equivalent registration path.

- [x] **Step 2: 写 Origin 显式 wildcard 测试**

Assert blank Origin config returns no wildcard; explicit `*` returns wildcard.

- [x] **Step 3: 检查是否需要专用握手拦截器**

If current code validates only after connection establishment, add tests for moving token/clientId extraction into handshake attributes without registering invalid sessions.

- [x] **Step 4: 运行配置测试确认失败**

Run: `cd fw-plus-boot && mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicAPIWsHandlerConfigurationTest test`

- [x] **Step 5: 实现配置与注册修正**

Adjust registration/config helper methods; if adding interceptor, keep it package-local and focused on parameter/token/appId/clientId extraction.

- [x] **Step 6: 运行配置测试确认通过**

Run the same targeted Maven command. Expected: PASS.

---

### Task 5: 推送、集群广播和功能完整性回归

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws/handler/BaseDynamicWebSocketHandlerTest.java`
- Modify/Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws/handler/RedisDynamicWebSocketHandlerTest.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws/handler/RedisDynamicWebSocketHandler.java`

**Interfaces:**
- Consumes: `broadcastMessageToClient`, `broadcastDistributedMessageToAllClient`, Redis broadcast payload.
- Produces: all local valid sessions receive messages; Redis self-origin messages are skipped; invalid Redis payload ignored safely.

- [x] **Step 1: 写多 session 点对点推送测试**

Register two sessions for same client/appId and assert both receive point-to-point message.

- [x] **Step 2: 写全量广播测试**

Register sessions under same appId and another appId; assert only target appId sessions receive broadcast.

- [x] **Step 3: 写 Redis 自身消息跳过测试**

Instantiate listener or expose testable path via package-private method if necessary; assert excluded/current serverId match时不投递。

- [x] **Step 4: 写非法 Redis payload 安全忽略测试**

Send invalid JSON/null fields and assert no exception propagates and no local delivery occurs.

- [x] **Step 5: 实现或修正集群路径**

Keep Redis publish path separate from local-only delivery; ensure received Redis message never republishes to Redis.

- [x] **Step 6: 运行相关测试确认通过**

Run: `cd fw-plus-boot && mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=BaseDynamicWebSocketHandlerTest,RedisDynamicWebSocketHandlerTest test`

---

### Task 6: 文档、OpenSpec 任务勾选与完整验证

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-ws/README.md`
- Modify: `openspec/changes/standardize-websocket-plugin/tasks.md`

**Interfaces:**
- Consumes: implemented behavior and OpenSpec tasks.
- Produces: accurate integration documentation and checked-off task list.

- [x] **Step 1: 更新 README 标准对接说明**

Document token API, connection URL, Origin/subprotocol, standard Ping/Pong, browser Ping limitation, text ping compatibility, JSON business message, structured error format, close code mapping, Redis cluster notes.

- [x] **Step 2: 勾选 OpenSpec 已完成任务**

Update `openspec/changes/standardize-websocket-plugin/tasks.md` checkboxes only after corresponding tests and implementation are done.

- [x] **Step 3: 运行 autoconfigure 模块完整测试**

Run: `cd fw-plus-boot && mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test`
Expected: BUILD SUCCESS.

- [x] **Step 4: 检查 git 状态和无关文件**

Run `git status --short` at root and `git -C fw-plus-boot status --short`; verify `.DS_Store` and unrelated archive files remain untouched.

- [x] **Step 5: 准备进入 Comet verify**

Run build guard after all tasks pass: `COMET_ENV=...; . "$COMET_ENV"; "$COMET_BASH" "$COMET_GUARD" standardize-websocket-plugin build --apply`.
