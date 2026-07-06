# 设计

## 修复方案

保持现有裸 WebSocket 协议与 MagicAPI 脚本执行模型不变，仅在脚本异常路径补齐结构化错误响应。

- 在 `BaseDynamicWebSocketHandler#handleSubscribeMessage` 的脚本异常 catch 分支中调用已有 `sendErrorMessage(session, "SCRIPT_ERROR", ...)`。
- 如果错误消息发送失败，仅记录日志，不再升级为关闭连接，避免可恢复业务错误影响连接可用性。
- 增加 `BaseDynamicWebSocketHandlerTest` 中对 `handleMessage` 的直接测试，使用可控 fake `WebSocketSession` 与测试 handler 验证输出和 close status。
- 修正 `standard-websocket-plugin` 主规格 Purpose 的 TBD 占位文案。

## 范围约束

- 不新增公开接口。
- 不改变前端控制台请求格式。
- 不改变 token、Origin、子协议协商策略。
