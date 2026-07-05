## Why

当前 MagicAPI WebSocket 插件已经具备基础连接、脚本订阅和服务端推送能力，但部分行为仍偏业务约定而非 WebSocket 标准语义，例如控制帧、关闭码、握手失败、非法消息、二进制消息和错误反馈边界不够清晰。按标准整改后，可降低客户端对接成本，提高浏览器、网关、负载均衡、集群部署和生产运维场景下的一致性与可诊断性。

## What Changes

- 标准化 WebSocket 握手：在握手阶段完成必要参数、token、appId、Origin 和子协议处理，失败时明确拒绝握手或使用标准关闭语义。
- 标准化控制帧：支持标准 Ping/Pong 控制帧，保留文本 `ping` / `pong` 兼容行为。
- 标准化消息处理：明确文本 JSON 业务消息格式、非法 JSON、缺少 `type`、未知 `type`、脚本异常和非文本消息的处理规则。
- 标准化关闭语义：按场景使用合理 close code，例如正常关闭、协议错误、策略违规、消息类型不支持、服务异常、服务过载、服务重启。
- 补齐功能完整性：连接生命周期、业务消息、服务端点对点推送、全量广播、多 session、Redis 集群广播、并发发送和资源清理均需有明确行为与测试覆盖。
- 更新对接文档：说明标准客户端行为、兼容行为、错误处理、心跳、关闭码和集群注意事项。

## Capabilities

### New Capabilities
- `standard-websocket-plugin`: 规范 MagicAPI WebSocket 插件的握手、消息、控制帧、关闭语义、脚本联动、服务端推送和集群广播行为。

### Modified Capabilities

无。

## Impact

- 代码范围：`fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws`。
- 测试范围：`fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/ws`。
- 文档范围：`fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-ws/README.md`。
- 对外行为：保留现有 MagicAPI 脚本资源模型和主要 API 名称；对明显不合理的边界行为允许调整为标准 WebSocket 行为。
- 依赖：优先使用 Spring WebSocket 现有能力，不新增重量级协议依赖。
