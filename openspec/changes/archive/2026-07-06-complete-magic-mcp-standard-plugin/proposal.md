# Complete Magic MCP Standard Plugin

## Why
Magic MCP 插件已经具备基础 Streamable HTTP、tools/resources/prompts、鉴权和控制台资源管理能力，但系统审查显示它仍停留在较早协议版本和基础查询能力层面：协议版本协商、capabilities 精确声明、server-to-client 请求闭环、控制台诊断预览、tool schema 生成和 SSE frame 兼容性仍不完整。若继续作为标准 MCP 插件发布，这些缺口会影响 MCP client 兼容性、排障体验和实际可用性。

## What Changes
- 升级协议目标到 MCP `2025-11-25`，实现受支持版本列表、版本协商和不支持版本错误。
- 强化 Streamable HTTP 语义：POST session 校验、协议版本响应头、SSE message event 输出。
- 精确声明 server capabilities，包括 listChanged、subscribe、logging、completion 以及可用的客户端请求能力。
- 完成 server-to-client 请求生命周期：roots/sampling/elicitation 请求发送、响应关联、错误、取消、超时清理。
- 增强 Magic API/function 暴露为 MCP tool 的 schema 质量。
- 增加 Magic 控制台 MCP 诊断/预览能力，用于验证 tools/resources/prompts 查询和调用。
- 补齐 README、OpenSpec 场景和回归测试。

## Scope
一期以兼容性和实用闭环为目标，不重写为独立 MCP SDK，不改变 MCP 资源/Prompt 的 Magic-API 存储模型，不引入 `mcp-datasource`。
