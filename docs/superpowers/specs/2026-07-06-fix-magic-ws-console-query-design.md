---
comet_change: fix-magic-ws-console-query
role: technical-design
canonical_spec: openspec
archived-with: 2026-07-06-fix-magic-ws-console-query
status: final
---

# Fix Magic WebSocket Console Query Design

## 背景

Magic WebSocket 插件运行时已具备标准连接、消息分发、生命周期脚本和广播能力，但 MagicAPI 控制台配套能力仍有不一致：控制器测试接口固定返回成功，前端代码提示引用旧包名和不存在类型，订阅资源路径元数据与后端 storage 不一致，脚本上下文变量在不同执行路径中命名不统一。

本设计以 OpenSpec change `fix-magic-ws-console-query` 为事实源，目标是让管理端查询、测试、代码提示和运行时脚本上下文都符合 `standard-websocket-plugin` 的标准。

## 确认方案

采用最小兼容修复：

1. `/ws/execute` 按订阅脚本 id 加载并执行脚本，使用确定性模拟上下文，不建立真实 WebSocket session。
2. `/ws/datasource/test` 执行真实字段校验、`other` JSON 校验和 token 生成前置条件校验。
3. 前端插件只引用真实 `WsModule` 后端类和真实模块方法，移除旧包名、`WsProducer` 和 `extra=producer` 遗留逻辑。
4. 后端所有脚本执行路径同时注入 `type` 与 `businessType`，业务消息保留 `params`。
5. 通过单元测试和静态检查覆盖管理端接口、上下文兼容和前端配置一致性。

## 后端设计

### `/ws/execute`

控制器接收脚本 id 和可选测试参数，使用 `WsSubscribeInfoMagicResourceStorage` 从 MagicAPI 资源中加载 `WsSubscribeInfo`：

- 找不到脚本时返回失败结果，不执行其他脚本。
- 找到脚本时构造 `MagicScriptContext`：
  - `appId`：订阅 `key`
  - `clientId`：请求传入或默认测试值
  - `sessionId`：请求传入或默认测试值
  - `type`：订阅 `businessId`
  - `businessType`：订阅 `businessId`
  - `params`：请求传入参数，缺省为空 JSON 对象
- 调用 `ScriptManager.executeScript` 执行脚本。
- 成功时返回脚本结果；失败时返回结构化错误信息。

控制台测试语义是“脚本逻辑测试”，不是“真实连接测试”。因此不触发真实 session 注册、init/close 生命周期或 Redis 广播副作用。

### `/ws/datasource/test`

数据源测试需要比固定 `ok` 更接近真实可用性：

- 校验 `key`、`name`、`secret` 必填。
- 校验 `other` 若非空则必须是 JSON。
- 校验全局 `magic-api.plugin.ws.token.secret` 可用。
- 校验使用数据源 `key + secret` 能生成连接 token。

如果校验失败，应返回失败原因；如果全部通过，返回 `ok`。

## 运行时上下文设计

当前 handler 在 `init` 中提供 `businessType`，业务消息提供 `type`，`close` 提供 `type`。为避免破坏既有脚本并满足标准契约，统一为：

- `init`：`type=initBusinessId`，`businessType=initBusinessId`
- 业务消息：`type=incomingType`，`businessType=incomingType`，`params=parsedJson`
- `close`：`type=closeBusinessId`，`businessType=closeBusinessId`

后续文档推荐使用 `type`，但保留 `businessType` 作为兼容别名。

## 前端插件设计

`magic-ws-datasource.js` 应使用真实后端类：

- `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.module.WsModule`

代码提示应表达真实 `ws` 模块能力，而不是不存在的 producer 类型。移除：

- `com.jugui...` 旧包名
- `com.jugui...WsProducer`
- `extra === 'producer'` 过滤条件

`magic-ws-subscribe.js` 的 `requirePath` 应与 `WsSubscribeInfoMagicResourceStorage.requirePath()` 保持一致，即不要求路径。

## 测试设计

### 单元测试

- `MagicWsControllerTest`
  - `/ws/execute` 成功执行脚本并返回结果。
  - 未知脚本 id 返回失败。
  - 脚本执行异常返回失败。
  - `/ws/datasource/test` 成功返回 `ok`。
  - 缺少必填字段、非法 `other`、缺少 token secret 返回失败。

- `BaseDynamicWebSocketHandlerTest`
  - init 脚本上下文包含 `type` 与 `businessType`。
  - 业务消息脚本上下文包含 `type`、`businessType` 和 `params`。
  - close 脚本上下文包含 `type` 与 `businessType`。

- 前端静态校验
  - 不包含 `com.jugui`。
  - 不包含 `WsProducer`。
  - 不包含 `extra === 'producer'`。
  - `ws-subscribe` 的 `requirePath` 与后端一致。

### 验证命令

- `openspec validate fix-magic-ws-console-query`
- 在 `fw-plus-boot` 中使用 Maven Wrapper 运行 WS 相关测试模块。

## 边界条件

- 控制台模拟执行不保证脚本中依赖真实 WebSocket session 的操作一定成功；这类脚本仍需通过真实客户端连接验证。
- token 生成依赖全局 token secret，数据源自身 secret 只用于业务级授权校验。
- 不修改已存在资源文件格式，因此旧 `ws-datasource` 和 `ws-subscribe` 文件无需迁移。

## 实施顺序

1. 后端控制器与测试。
2. handler 上下文变量兼容与测试。
3. 前端控制台插件修复与静态校验。
4. README 更新与最终验证。
