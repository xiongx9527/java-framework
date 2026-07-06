# 设计：Magic MCP 标准查询热修复

## 方案
在现有实现上做协议边界补强，不改变对外 endpoint 和资源模型：

1. `McpJsonRpcDispatcher` 增加统一分发入口，支持 `Map` 单请求和 `List` batch；保留现有单请求 API 兼容测试。
2. dispatcher 负责校验 JSON-RPC 基本格式、版本和通知语义，并在调用 feature handler 前执行 lifecycle ready guard。
3. `McpHttpTransportService` 使用统一分发入口；当 `initialize` 创建 session 后，将 `context.sessionId` 写入 `Mcp-Session-Id` 响应头。
4. `McpSseStreamService` 注入 `McpSessionService`，GET SSE 打开前 require session 且要求 `READY`。
5. 测试覆盖初始化响应头、batch、无效版本、初始化前 feature 拦截、GET SSE session 校验。

## 非目标
- 不新增 MCP capability。
- 不改 Magic 控制台 UI 查询/测试入口；该项作为后续增强。
- 不改变已有 tools/resources/prompts provider 数据结构。
