## Context

本修复延续现有模拟上下文方案，不引入真实 WebSocket session，也不改变正式连接协议。

## Fix Approach

- 将 `/ws/execute` 请求体中的 `params` 读取为 Map，并传入 `buildTestContext`。
- `findSubscribeInfo` 只返回 `enabled=true` 且 id 匹配的订阅脚本。
- `/ws/datasource/test` 必须获取到 `BaseDynamicWebSocketHandler`，否则失败；获取到后继续使用 `WsModule.generateToken` 验证 token 生成能力。
- 单元测试覆盖 params 透传、禁用脚本拒绝、handler 缺失失败和 handler 存在成功。

## Non-Goals

- 不接入 MagicAPI DebugRequest / 断点调试。
- 不改变 `ws-subscribe` 资源格式。
- 不新增外部依赖。
