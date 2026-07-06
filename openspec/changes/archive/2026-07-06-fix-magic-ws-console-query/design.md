## Context

`standard-websocket-plugin` 已覆盖 WebSocket 握手校验、控制帧、业务消息、生命周期脚本和广播能力。当前缺口集中在 MagicAPI 控制台配套能力：`/ws/execute` 和 `/ws/datasource/test` 固定返回成功，导致管理端测试没有真实语义；前端代码提示仍引用旧包名和不存在的 Producer 类型；`ws-subscribe` 的前端路径要求与后端 storage 不一致；脚本上下文中 `init`、业务消息、`close` 的变量命名不统一。

本次变更跨后端控制器、运行时上下文和前端插件配置，但不改变客户端正式 WebSocket 连接协议。

## Goals / Non-Goals

**Goals:**

- 让 MagicAPI 控制台的 WS 数据源测试和订阅脚本测试具备真实校验/执行语义。
- 让 WS 前端插件的 JavaClass 扩展、资源元数据和后端模型保持一致。
- 让 `ws-subscribe` 脚本在 `init`、业务消息、`close` 场景中获得兼容且一致的上下文变量。
- 补充关键测试，避免固定返回 `ok` 或旧包名残留再次出现。

**Non-Goals:**

- 不重构 WebSocket session 管理、Redis 广播或 JWT 签名策略。
- 不建立真实 WebSocket 连接来测试控制台脚本运行；控制台测试使用模拟上下文。
- 不修改 MagicAPI 资源文件格式的已有必填字段。
- 不新增外部依赖或升级 MagicAPI/Spring Boot/JWT 版本。

## Decisions

### Decision 1: `/ws/execute` 使用模拟上下文执行脚本

控制台测试应按脚本 `id` 从 `WsSubscribeInfoMagicResourceStorage` 读取订阅脚本，构造 `MagicScriptContext` 并执行。上下文使用可预测的模拟值，例如 `appId` 来自订阅 `key`，`type`/`businessType` 来自 `businessId`，`clientId`/`sessionId` 使用测试标识，`params` 使用请求体传入的可选参数。

备选方案是发起真实 WebSocket 连接并触发 handler。该方案更接近端到端，但需要 token、session、网络握手和异步关闭处理，控制台单次脚本测试成本过高，也容易误伤真实客户端。因此本次选择模拟上下文。

### Decision 2: `/ws/datasource/test` 复用后端校验并验证 token 生成能力

数据源测试不能只返回 `ok`。它应执行与保存一致的必填字段和 `other` JSON 校验，并验证该 `key + secret` 能通过 `WsModule` 或同等逻辑生成 token。这样可以发现缺少全局 token secret、数据源 secret 无效、JSON 配置错误等实际运行问题。

备选方案是只调用 storage `validate`。这能覆盖字段规则，但不能覆盖 token secret 配置，因此不足以代表 WebSocket 接入可用性。

### Decision 3: 前端代码提示只暴露真实 `WsModule` 能力

前端 `magic-ws-datasource.js` 应改为真实类 `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.module.WsModule`，并移除不存在的 `WsProducer` 类型和 `extra === 'producer'` 过滤。代码提示应围绕 `ws['key']` 动态模块和 `broadcastMessageToClient`、`broadcastDistributedMessageToAllClient`、`generateToken` 等真实方法表达。

备选方案是新增 Producer 抽象来适配旧前端。该方案扩大范围且当前后端没有生产者模型，不符合最小修复原则。

### Decision 4: 以兼容方式统一脚本上下文变量

所有订阅脚本执行场景都提供 `type` 和 `businessType`。业务消息继续保留 `params`，生命周期脚本可不强制提供消息参数。这样既符合现有 spec 中业务消息要求的 `type`，也兼容已有可能使用 `businessType` 的 init 脚本。

备选方案是统一只保留 `type`。该方案更简洁，但可能破坏已有脚本。

## Risks / Trade-offs

- 控制台模拟执行不能覆盖真实 session 行为 → 通过文档和测试明确其用途是脚本逻辑测试，不替代端到端连接验证。
- 复用 storage 校验可能依赖 MagicResourceService 状态 → 对 controller 层添加单元测试，必要时拆出可测试的校验辅助方法。
- 前端 JavaClass API 受 MagicAPI 编辑器约束 → 保持最小改动，只修复类名、过滤条件和暴露类型，不重写编辑器插件机制。
- 同时提供 `type` 和 `businessType` 会增加上下文字段冗余 → 这是兼容性成本，可在文档中说明二者等价。

## Migration Plan

1. 后端先补齐 `/ws/execute` 与 `/ws/datasource/test` 的真实行为和测试。
2. 再统一 handler 中脚本上下文变量，保持向后兼容。
3. 最后修复前端插件配置和 README 中控制台测试说明。
4. 回滚时可分别回退控制器、handler、前端配置；不涉及数据迁移。

## Open Questions

无阻塞问题。默认采用模拟上下文执行控制台脚本测试，不建立真实 WebSocket session。
