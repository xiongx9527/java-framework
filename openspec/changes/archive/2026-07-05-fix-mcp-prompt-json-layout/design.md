## Context

MCP Prompt 资源由控制台维护，后端通过 `McpPromptInfo.argumentsJson` 和 `McpPromptInfo.messagesJson` 输出 MCP `prompts/list` 与 `prompts/get`。当前前端信息组件把 `arguments` 与 `messages` 作为两个 textarea 直接编辑，既没有对齐 MagicAPI API 信息区常用的 JSON 编辑体验，也容易让用户误填普通字符串或不完整消息对象。

## Goals / Non-Goals

### Goals

- 参数和消息使用 Tab 布局，每个 Tab 内使用 JSON 组件/JSON 编辑器。
- 默认结构清晰，直接符合 MCP Prompt：`arguments` 为数组，`messages` 为数组，message `content` 使用结构化对象。
- 后端保持兼容：既能读取前端保存后的字段，也能继续解析已有 JSON 字符串字段。
- 测试覆盖 list/get/render 和嵌套 JSON 变量替换。

### Non-Goals

- 不改 MCP JSON-RPC 方法名和响应协议。
- 不重做整个 MCP 控制台 UI。
- 不引入新的前端依赖。

## Decisions

### 1. 控制台信息区使用 JSON 组件而不是 textarea

决定：将 prompt info 中 `arguments` 和 `messages` 放入 Tab 布局，分别为“参数”和“消息”两个页签；每个页签内使用项目现有可用的 JSON 组件/JSON 编辑方式。如组件名需以运行时已有组件为准，优先复用 MagicAPI 控制台 API 信息中的 JSON 组件。

Tab 内容区需要形成完整的 100% 高度链路：外层 prompt info 使用 flex column 和 `height: 100%`，Tab 容器与 panel 使用 `flex: 1`/`min-height: 0`，JSON 编辑器绝对定位填满 active panel，避免固定 `240px` 导致编辑区无法占满可用高度。

理由：用户明确指出组件类型应为 JSON 组件，且 API 信息已有类似模式。

### 2. 后端继续以 JSON 字符串为存储兼容层

决定：后端 `MagicApiMcpPromptProvider` 保持解析 `argumentsJson` 与 `messagesJson`，并增强对字段为空、非法结构、嵌套内容替换的测试。

理由：避免破坏已保存资源文件格式，前端可在保存时继续映射为 JSON 字符串字段。

### 3. 默认消息使用 MCP 标准 content 对象

决定：默认 message 使用 `{ "role": "user", "content": { "type": "text", "text": "..." } }`，避免旧的纯字符串 content。

理由：用户指出“消息布局不对”，结构化 content 更贴合 MCP Prompt 内容块表达。

## Risks / Trade-offs

- 前端 JSON 组件名称可能依赖 MagicAPI 控制台运行时注册；实现时需要参考 API 信息区实际组件名。
- 旧资源可能只保存 `messages` 数组而非 `messagesJson` 字符串；需要确认现有保存链路是否自动转换。
- 若 JSON 组件只接受对象不接受数组，需要用包装对象方式展示并在 merge/save 时转换。

## Migration Plan

1. 定位 API 信息区 JSON 组件使用方式。
2. 先补后端 provider 测试，确保结构化 messages 和 arguments 渲染正确。
3. 改造前端 prompt info 为 JSON 组件布局。
4. 更新 README 和必要 i18n。
5. 跑 autoconfigure MCP prompt 相关测试；如能构建前端则补充前端构建检查。
