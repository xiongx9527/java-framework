# 修复 Magic MCP 标准查询缺口

## 问题描述
Magic MCP 服务端已提供 Streamable HTTP、JSON-RPC、tools/resources/prompts 等基础能力，但严格对照现有 `standard-mcp-server-plugin` 规格后发现：初始化响应未返回 `Mcp-Session-Id`，JSON-RPC dispatcher 缺少 batch 和 `jsonrpc` 版本校验，功能查询在会话 ready 前可能被执行，GET SSE 对未知或过期 session 校验不足。

## 根因分析
当前实现把 JSON-RPC 分发、HTTP 会话头、生命周期检查分散在 transport、dispatcher 和各 handler 中，缺少统一的 MCP 协议边界校验；测试覆盖偏向单请求 happy path，未覆盖 batch、会话头传播和初始化前 feature 拦截。

## 修复目标
- 初始化成功时通过 HTTP 响应头返回 `Mcp-Session-Id`。
- JSON-RPC dispatcher 支持单请求和 batch，并严格校验 `jsonrpc == "2.0"`。
- 除生命周期允许方法外，feature 查询必须在 session ready 后执行。
- GET SSE 必须拒绝未知、过期或未 ready session。
- 增加针对上述缺口的回归测试。
