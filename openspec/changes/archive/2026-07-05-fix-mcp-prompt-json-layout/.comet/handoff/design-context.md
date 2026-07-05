# Comet Design Handoff

- Change: fix-mcp-prompt-json-layout
- Phase: design
- Mode: compact
- Context hash: 830b0c9a486d2c2f93c08b2e6c9e8139fba17a5ff17d5996f557c21c04f2eb55

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/fix-mcp-prompt-json-layout/proposal.md

- Source: openspec/changes/fix-mcp-prompt-json-layout/proposal.md
- Lines: 1-27
- SHA256: aed1c418599d1e4ab49fb60b71fec242b6dca19e13674d5cfd01f55dbc64d514

```md
## Why

当前 MCP Prompt 控制台信息区将 `arguments` 和 `messages` 分别用普通 textarea 编辑，参数和消息布局与 API 信息区常见的 JSON/结构化配置体验不一致，也容易生成不符合 MCP Prompt 协议的消息结构。用户明确要求参考 API 信息，并将组件类型改为 JSON 组件，以修正参数、消息布局。

## What Changes

- 将 MCP Prompt 信息面板中的参数与消息编辑改为 JSON 组件/JSON 编辑体验，不再使用普通 textarea 作为主要编辑方式。
- 调整默认 Prompt 脚本/数据布局，使 `arguments`、`messages` 与 MCP Prompt API 信息结构一致。
- 保持后端 `McpPromptInfo`、`MagicApiMcpPromptProvider` 对 `argumentsJson` / `messagesJson` 的兼容解析能力。
- 增加测试覆盖，确保参数、消息布局为 JSON 对象/数组时能正确 list/get/render。
- 更新 MCP 插件文档，说明 Prompt 参数与消息的 JSON 格式。

## Capabilities

### New Capabilities
- `mcp-prompt-json-layout`: 规范 MCP Prompt 控制台参数和消息的 JSON 组件布局、默认结构和后端解析兼容行为。

### Modified Capabilities

无。

## Impact

- 控制台前端：`fw-plus-magicapi-plugin-mcp/src/console/src/components/magic-mcp-prompt-info.vue`、`service/magic-mcp-prompt.js`、必要 i18n 文案。
- 后端提供者：`MagicApiMcpPromptProvider` 及相关测试。
- 文档：`fw-plus-magicapi-plugin-mcp/README.md`。
- 不改 MCP JSON-RPC 协议方法名，不改 `mcp-prompt` 资源类型。
```

## openspec/changes/fix-mcp-prompt-json-layout/design.md

- Source: openspec/changes/fix-mcp-prompt-json-layout/design.md
- Lines: 1-52
- SHA256: 7b4c6788342cacff22acfef9afa0042fe799b745af29bcb56fa6f8ee2acd47c0

```md
## Context

MCP Prompt 资源由控制台维护，后端通过 `McpPromptInfo.argumentsJson` 和 `McpPromptInfo.messagesJson` 输出 MCP `prompts/list` 与 `prompts/get`。当前前端信息组件把 `arguments` 与 `messages` 作为两个 textarea 直接编辑，既没有对齐 MagicAPI API 信息区常用的 JSON 编辑体验，也容易让用户误填普通字符串或不完整消息对象。

## Goals / Non-Goals

### Goals

- 参数和消息使用 JSON 组件/JSON 编辑器布局。
- 默认结构清晰，直接符合 MCP Prompt：`arguments` 为数组，`messages` 为数组，message `content` 使用结构化对象。
- 后端保持兼容：既能读取前端保存后的字段，也能继续解析已有 JSON 字符串字段。
- 测试覆盖 list/get/render 和嵌套 JSON 变量替换。

### Non-Goals

- 不改 MCP JSON-RPC 方法名和响应协议。
- 不重做整个 MCP 控制台 UI。
- 不引入新的前端依赖。

## Decisions

### 1. 控制台信息区使用 JSON 组件而不是 textarea

决定：将 prompt info 中 `arguments` 和 `messages` 的编辑控件改为项目现有可用的 JSON 组件/JSON 编辑方式；如组件名需以运行时已有组件为准，优先复用 MagicAPI 控制台 API 信息中的 JSON 组件。

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
```

## openspec/changes/fix-mcp-prompt-json-layout/tasks.md

- Source: openspec/changes/fix-mcp-prompt-json-layout/tasks.md
- Lines: 1-18
- SHA256: c08fcde2c746e1658f58cb2696d613983fce360a3592c29c703068b12ef8bd09

```md
## 1. Backend Prompt Contract

- [ ] 1.1 Add provider tests for structured `content` object placeholder rendering
- [ ] 1.2 Add provider tests for prompt argument JSON parsing and required flags
- [ ] 1.3 Fix provider parsing/rendering if tests expose layout issues

## 2. Console JSON Layout

- [ ] 2.1 Locate API information JSON component usage and mirror the pattern for MCP Prompt
- [ ] 2.2 Replace MCP Prompt arguments/messages textarea layout with JSON component/editor layout
- [ ] 2.3 Update default prompt script/data to use structured MCP message content
- [ ] 2.4 Update i18n placeholders/help text for JSON layout

## 3. Documentation and Verification

- [ ] 3.1 Update MCP README with prompt arguments/messages JSON examples
- [ ] 3.2 Run MCP prompt/provider targeted tests
- [ ] 3.3 Run selected autoconfigure verification command
```

## openspec/changes/fix-mcp-prompt-json-layout/specs/mcp-prompt-json-layout/spec.md

- Source: openspec/changes/fix-mcp-prompt-json-layout/specs/mcp-prompt-json-layout/spec.md
- Lines: 1-30
- SHA256: 6cc28890c106478aab73a45f8392d3bfa76f984c077ae867a23877e11110d2ea

```md
## ADDED Requirements

### Requirement: Prompt arguments use JSON component layout
MCP Prompt console SHALL edit prompt arguments through a JSON component/editor layout rather than plain textarea fields.

#### Scenario: Edit prompt arguments
- **WHEN** a user opens an MCP Prompt resource in the console
- **THEN** the arguments field SHALL be presented as JSON structured data suitable for an array of argument definitions

### Requirement: Prompt messages use JSON component layout
MCP Prompt console SHALL edit prompt messages through a JSON component/editor layout and use MCP-compatible structured message content.

#### Scenario: Edit prompt messages
- **WHEN** a user opens an MCP Prompt resource in the console
- **THEN** the messages field SHALL be presented as JSON structured data suitable for an array of `{role, content}` message objects

#### Scenario: Default prompt message layout
- **WHEN** a new MCP Prompt resource is created
- **THEN** the default messages SHALL use structured content such as `{ "type": "text", "text": "..." }` instead of an ambiguous plain string layout

### Requirement: Prompt provider preserves structured JSON
The backend prompt provider SHALL preserve structured JSON arguments and messages and SHALL render nested string placeholders from prompt arguments.

#### Scenario: Render nested content placeholder
- **WHEN** a prompt message contains nested text `{{content}}` inside `content.text`
- **THEN** `prompts/get` SHALL return the nested value rendered with the provided argument

#### Scenario: List prompt arguments
- **WHEN** `prompts/list` is called
- **THEN** the provider SHALL return argument definitions parsed from the JSON argument layout
```

