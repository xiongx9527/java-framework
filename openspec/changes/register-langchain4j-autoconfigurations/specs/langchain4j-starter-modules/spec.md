## MODIFIED Requirements

### Requirement: 暴露指定 LangChain4j starter 本地模块
系统 SHALL 在 `fw-plus-ai-langchain4j` 下为每个用户指定的 LangChain4j Spring Boot starter 暴露一个 fw-plus Maven 模块，并在模块内保留对应 starter 自动配置代码。

#### Scenario: fw-plus AI 聚合配置导入本地 starter 自动配置
- **WHEN** 检查 `LangChain4jAutoConfiguration`
- **THEN** 它导入 core、Anthropic、Ollama、Azure AI Search、Azure OpenAI、Voyage AI、GitHub Models、Vertex AI Gemini、Elasticsearch、Redis、Qianfan、Milvus、DashScope 对应的 fw-plus 配置类。
- **AND** 每个被导入配置类都具备 `enabled` 启动条件。

#### Scenario: core 自动配置具备启动条件
- **WHEN** 检查 `LangChain4jCoreAutoConfiguration`
- **THEN** 它导入 `dev.langchain4j.spring.LangChain4jAutoConfig`。
- **AND** 它具备 `enabled` 启动条件。
