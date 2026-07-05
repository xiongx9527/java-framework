---
comet_change: fix-mcp-prompt-json-layout
role: technical-design
canonical_spec: openspec
archived-with: 2026-07-05-fix-mcp-prompt-json-layout
status: final
---

# MCP Prompt JSON 布局技术设计

## 背景

MCP Prompt 控制台当前将 `arguments` 和 `messages` 放在两个普通 textarea 中编辑。用户反馈参数和消息布局不对，并明确要求参考 API 信息，将组件类型调整为 JSON 组件。后端目前以 `argumentsJson` 和 `messagesJson` 保存/解析 JSON 字符串，provider 已支持嵌套 Map/List 渲染，但缺少对标准结构化 content 的明确测试和文档。

## 目标

- Prompt 参数和消息在控制台使用 Tab 布局，并在各自 Tab 内使用 JSON 组件/JSON 编辑体验。
- 默认 messages 使用 MCP 标准结构化 content 对象。
- 保持后端资源字段和 JSON 字符串解析兼容。
- 补齐 provider 对嵌套 JSON 渲染与参数 required 解析的测试。

## 非目标

- 不改 MCP JSON-RPC `prompts/list` / `prompts/get` 方法名。
- 不重构整个 MCP 控制台。
- 不引入新的前端依赖。

## 方案

### 前端

`magic-mcp-prompt-info.vue` 将 `arguments` 与 `messages` 放入两个 Tab：参数、消息。每个 Tab 内使用 JSON 组件展示对应 JSON。若当前控制台运行时已注册 `magic-json`/JSON editor 类组件，则优先直接绑定 `info.arguments`、`info.messages`；若组件 API 要求字符串，则保留 computed 做 JSON string 与对象数组转换，但组件类型仍为 JSON 编辑组件。

`magic-mcp-prompt.js` 的 `defaultScript` 改为包含：

```json
{
  "arguments": [
    { "name": "content", "description": "Content", "required": true }
  ],
  "messages": [
    {
      "role": "user",
      "content": { "type": "text", "text": "{{content}}" }
    }
  ]
}
```

### 后端

`MagicApiMcpPromptProvider` 保持现有解析方式：

- `argumentsJson` 空白时返回空参数列表。
- `messagesJson` 空白时返回空消息列表。
- 字符串 placeholder 在嵌套 Map/List 中递归替换。

如测试发现字段保存为对象数组而不是 JSON 字符串，需要在 storage 或 provider 层追加兼容读取，但优先不扩大范围。

## 风险与处理

- JSON 组件名称不确定：实现时通过现有控制台组件使用方式确认，避免编造不存在组件。
- 旧资源兼容：不删除 `argumentsJson` / `messagesJson` 字段。
- 非法 JSON 输入：当前前端 JSON 组件应承担基础校验；后端测试聚焦合法 JSON 结构。

## 验证

- `mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=McpPromptsHandlerTest,MagicApiMcpPromptProviderTest test`
- 必要时运行完整 autoconfigure 测试。
