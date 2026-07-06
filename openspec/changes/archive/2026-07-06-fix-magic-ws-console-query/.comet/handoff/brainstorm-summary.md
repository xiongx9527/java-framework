# Brainstorm Summary

- Change: fix-magic-ws-console-query
- Date: 2026-07-06

## 确认的技术方案

采用“最小兼容修复”方案：不改变正式 WebSocket 连接协议，不创建真实 WebSocket session 来运行控制台测试。`/ws/execute` 按 `ws-subscribe` 脚本 id 加载脚本，构造确定性模拟上下文后执行；`/ws/datasource/test` 执行真实字段、JSON 和 token 生成校验；前端控制台改为真实 `WsModule` 类名和真实模块能力；所有脚本执行路径同时提供 `type` 与 `businessType`。

## 关键取舍与风险

- 控制台测试使用模拟上下文，不能替代真实 WebSocket 端到端验证；但能稳定覆盖脚本逻辑和管理端“运行”能力。
- 数据源测试需要覆盖全局 token secret，否则仅字段合法仍无法连接；因此测试应验证 token 生成前置条件。
- 前端代码提示只修复真实模型映射，不新增 Producer 抽象，避免扩大范围。
- `type` 与 `businessType` 同时存在会有冗余，但能兼容旧脚本并符合标准消息契约。

## 测试策略

- 增加 `MagicWsController` 单元测试，覆盖 `/ws/execute` 成功执行、未知脚本、脚本异常、数据源测试成功与失败。
- 更新 `BaseDynamicWebSocketHandler` 测试，验证 init、业务消息、close 路径均注入兼容变量。
- 对前端 JS 增加静态风险校验或轻量测试，防止旧包名、`WsProducer`、`extra=producer` 与 `requirePath` 不一致回归。
- 运行 `openspec validate fix-magic-ws-console-query` 和 WS 相关 Maven 测试。

## Spec Patch

无。当前 delta spec 已覆盖控制台验证、订阅脚本测试、编辑器元数据一致性和上下文兼容场景。
