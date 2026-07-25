## ADDED Requirements

### Requirement: 暴露指定 LangChain4j starter 模块
系统 SHALL 在 `fw-plus-ai-langchain4j` 下为每个用户指定的 LangChain4j Spring Boot starter dependency 暴露一个 fw-plus Maven 模块。

#### Scenario: 聚合 POM 注册指定 starter 模块
- **WHEN** 检查 `fw-plus-ai-langchain4j` 聚合 POM
- **THEN** 它列出 Anthropic、Ollama、Azure AI Search、Azure OpenAI、Voyage AI、GitHub Models、Vertex AI Gemini、Elasticsearch、Redis、Qianfan、Milvus、DashScope 对应的子模块。

#### Scenario: 子模块包装指定 starter 依赖
- **WHEN** 检查任一新增 starter 子模块 POM
- **THEN** 它声明对应的 `dev.langchain4j` Spring Boot starter artifact，并使用用户指定版本。

### Requirement: 保持现有 LangChain4j 模块可构建
系统 SHALL 在新增 starter 模块时保留 LangChain4j 聚合下已有的 `core` 和 `openai` 模块。

#### Scenario: LangChain4j 聚合构建成功
- **WHEN** 构建 core 侧 LangChain4j Maven reactor
- **THEN** 现有模块和新增模块都能解析并编译成功。
