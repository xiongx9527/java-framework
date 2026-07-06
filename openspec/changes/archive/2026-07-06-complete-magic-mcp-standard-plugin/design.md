# Design: Complete Magic MCP Standard Plugin

## 目标
把 Magic MCP 插件从“基础可用”提升到“标准兼容的一期完整能力”：MCP 2025-11-25 协议协商、精确能力声明、HTTP/SSE 传输语义、server-to-client 请求闭环、控制台诊断体验和工具 schema 质量。

## 方案概览
采用渐进补强，不替换现有模块边界：

1. 协议与传输层
   - 在 `MagicAPIMcpProperties` 增加 `protocol.supportedVersions`、`protocol.defaultVersion`、`clientRequestTimeoutSeconds` 等配置。
   - `McpLifecycleHandlers.initialize` 校验客户端 `protocolVersion`，只接受支持列表，默认返回 `2025-11-25`。
   - `McpHttpTransportService` 对非 initialize 且携带 session header 的 POST 做 session 存在/过期校验，并在响应中写回协议版本/session header。
   - `McpSseStreamService` 使用明确 SSE `message` event 发送 JSON-RPC message，提升跨 client 兼容性。

2. Capabilities 精确声明
   - 根据已注册 provider、订阅服务、通知能力和配置生成 capabilities。
   - tools/resources/prompts 声明 `listChanged`，resources 声明 `subscribe`，logging 声明存在，completion 与客户端请求能力按 provider/config 暴露。

3. Server-to-client 请求闭环
   - 在 session 中记录 pending client requests：id、method、capability、createdAt、timeoutAt、状态。
   - dispatcher 识别 JSON-RPC response/error 消息，交给 `McpClientRequestService` 完成匹配和状态更新。
   - 支持超时清理、取消标记、未匹配 response 忽略或记录。

4. Tool schema 增强
   - Magic API tool 从 path/query/body/options 尽可能生成 JSON Schema；缺失元数据时保留 `{type: object}` fallback。
   - Magic function tool 支持通过资源 metadata/options 读取参数描述；无法识别时不阻塞。
   - 维持 API 暴露默认关闭和 opt-in/scope 过滤。

5. 控制台诊断
   - 在 MCP 插件 console 增加诊断/预览入口：
     - resources: 输入/选择 URI 调用 `resources/read` 预览。
     - prompts: 输入 arguments 调用 `prompts/get` 预览。
     - tools: 查看 `tools/list` 并可填 arguments 调用 `tools/call`。
   - 诊断走后端受保护 endpoint，不直接绕过鉴权或 session 规则。

## 风险与取舍
- MCP 最新规范持续演进：本次锁定 `2025-11-25`，保留 supportedVersions 配置便于后续升级。
- Magic API 元数据不一定足够生成完整 schema：优先生成可确定字段，不能确定时 fallback，避免破坏现有功能。
- 控制台诊断可能涉及敏感 tool 调用：默认复用 MCP 鉴权与 scope，不新增无权限执行路径。

## 测试策略
- 单元测试：版本协商、capabilities、session 校验、SSE event、client response correlation、tool schema fallback。
- 控制器测试：POST session header、unsupported version、batch response、SSE open/send。
- provider 测试：API/function schema、scope/opt-in 不回退。
- 控制台可通过服务层和轻量 JS 测试/构建校验，若项目无前端测试则至少保证打包和静态结构。
