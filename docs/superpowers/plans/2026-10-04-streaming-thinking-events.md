---
change: streaming-thinking-events
design-doc: docs/openspec/changes/streaming-thinking-events/design.md
base-ref: 1a66309a6c45057c6a62e40c8e32e70cf7e54e15
archived-with: 2026-10-09-streaming-thinking-events
---

<!-- comet-task-authority: docs/openspec/changes/streaming-thinking-events/tasks.md -->

# 实施计划：跨提供商流式思考事件

## 当前工作区基线

- 本 change 已有 Ollama streaming 工厂、样例属性/配置及其测试的未提交改动；先检查并补足，不重复实现已完成部分。
- `ChatContextLogger.java` 属于先前存在的工作区修改，不纳入本 change。
- 四个 Core 层 staged 删除属于既有用户改动，必须保持 staged 且不纳入本 change 的提交。
- 样例 `application.yaml` 在本 change 前已有用户配置修改；只保留并核实已有内容，避免覆盖无关值。

## 实施任务

### 1. Provider 能力审计

<!-- comet-task-ref:b419703f-6bc1-4bac-8029-051869cb0fb3 -->

审核 18 个 streaming-chat-model 工厂，核实各自锁定 LangChain4j 版本的公开配置、`StreamingChatResponseHandler`/AiServices 响应映射和默认值。逐一记录“可经独立 thinking 回调输出”“有回调但无启用选项”或“不映射独立 thinking”及证据。README 能力表标明 SDK 坐标/版本、选项和限制。

验收：18 个已注册 provider 全部有记录；能力断言基于源码、公开 API 或字节码，不依据厂商模型宣传；OpenSpec 严格校验通过。

### 2. Provider 配置与控制台

<!-- comet-task-ref:0531124c-6de3-4f06-93b5-d23d6d048ad5 -->

按能力审计结果为有原生选项且适配器映射独立 thinking 的 streaming 工厂添加 options 透传与 builder 单测；测试显式值及未配置时保持 SDK 默认值。检查现有 Ollama 改动，确保 `think` / `return-thinking` 只影响 streaming builder，且对应测试覆盖真实 factory 参数。

验收：各工厂测试通过；未获支持的 provider 没有新增伪造开关。

<!-- comet-task-ref:70bdee51-cba2-4b80-9e13-dd72458b6f22 -->

同步更新 streaming-chat-model `providers.js`、中英文 i18n 及选项文档。复用现有字段和 Node 元数据一致性测试；只有测试要求时才改插件构建配置。

验收：Node 元数据测试通过，UI 字段恰好对应工厂消费字段；涉及前端产物时通过 Maven `package` 流程构建，不手动复制 `dist`。

<!-- comet-task-ref:90db1ab6-0735-4ad7-84e2-19241743bcbf -->

README 能力表对所有不支持独立 thinking 映射的 provider 明示限制；保持控制台不呈现通用 `thinking.enabled` 选项。

验收：逐项限制与已审计适配器行为一致，元数据测试确认没有引入伪开关。

### 3. SSE 桥接与 Ollama 样例

<!-- comet-task-ref:21e7058b-10e9-4de0-831e-b9d4f8a01033 -->

在 `SimpleAssistant` 共享 `ThinkingSseTracker` 上完成两条流式路径：AiServices TokenStream 的已有 callback，以及 DirectTokenStream 注册并转发 LangChain4j `StreamingChatResponseHandler.onPartialThinking`。完成回调中先 flush 活跃思考轮再发 `done`；保留回答、工具与 retrieved 事件形状。对 SSE tracker 与 DirectTokenStream 分别写最小回归测试。

验收：TDD RED 确认缺失的回调/完成边界导致失败，再 GREEN；覆盖 thinking-only、转回答、转工具、无 thinking 和事件顺序。DirectTokenStream 不增加工具执行能力。

<!-- comet-task-ref:6a5cda20-6cf7-4e07-84e0-93fedc3414ad -->

完善 Magic-API Ollama streaming-chat-model 工厂的 `think` / `return-thinking` 原生透传及前端字段。检查现有工厂改动，确保缺省不调用会覆盖 SDK 默认值的 builder setter。

验收：Ollama 工厂选项透传和缺省测试通过；控制台元数据与后端读取一致。

<!-- comet-task-ref:18cb7164-b7af-4891-a6bd-403d393a7595 -->

核实 AI Chat 样例 Ollama 属性/YAML 及 streaming builder；只在流式模型显式开启 Ollama thinking，不改变同步模型行为。保留 application.yaml 中已有的用户配置。

验收：样例属性绑定和 builder 测试通过；README 说明 `think` 与 `return-thinking` 的区别及非支持模型的限制。

### 4. 集成验证与审查

<!-- comet-task-ref:934ee20a-738b-4a52-b235-88dc6c326b01 -->

运行 AI autoconfigure 相关测试、样例配置测试、Magic-API 插件元数据测试，以及受影响插件的 Maven 前端构建。用可控模型测试验证最终 SSE 序列与错误路径；必要时记录 JDK/既有测试编译问题，不能将环境失败当作通过。

验收：所有范围内测试有明确结果；standard review 由独立 reviewer 检查完整 change diff，修复 CRITICAL/IMPORTANT 问题后再进入 Verify。

## 执行顺序

1. 任务 1.1 先建立全量 provider 能力事实。
2. 任务 2.1、2.2、2.3 依赖 1.1，可在其后按受影响 provider 完成。
3. 任务 3.1 独立于 provider 审核；3.2 依赖 Ollama 在 1.1 的能力判定；3.3 依赖 3.2。
4. 任务 4.1 依赖所有实现与文档任务。

## 风险与回退

- Community、GitHub Models 等 provider 可能采用不同 LangChain4j 版本；能力表逐坐标核验，避免套用主 BOM 版本。
- Ollama 服务或模型拒绝 `think` 时走已有 SSE error 路径；样例可移除/关闭配置，不以静默 fallback 掩盖错误。
- 改动仅扩展既有 SSE 事件，不修改已有 payload；必要时回退对应 provider-specific 字段和桥接逻辑即可。
