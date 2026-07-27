## MODIFIED Requirements

### Requirement: LangChain4j starter 模块只承载 LangChain4j 专用能力
系统 SHALL 保留既有 LangChain4j provider/vector-store starter 模块，但框架通用模型和向量 store 契约 SHALL 由根 AI 聚合下的中立模块和 starter 提供。

#### Scenario: LangChain4j 聚合保持现有专用 starter
- **WHEN** 检查 `fw-plus-ai-langchain4j` 聚合 POM
- **THEN** 它 SHALL 继续列出既有 LangChain4j core、OpenAI、provider/vector-store 和兼容 store starter 模块。
- **AND** 新增的框架无关模型/store 模块 SHALL 位于根 AI 聚合而不是 LangChain4j 聚合。

#### Scenario: 默认 LangChain4j starter 组合中立核心和适配器
- **WHEN** 应用引入 LangChain4j 模型或向量 store starter
- **THEN** starter SHALL 引入所需中立核心/中立 starter以及对应 LangChain4j adapter。
- **AND** starter SHALL 不传递 Spring AI、Spring AI Alibaba 或 AgentScope 2 Java SDK。
