## ADDED Requirements

### Requirement: 暴露指定 LangChain4j starter 本地模块
系统 SHALL 在 `fw-plus-ai-langchain4j` 下为每个用户指定的 LangChain4j Spring Boot starter 暴露一个 fw-plus Maven 模块，并在模块内保留对应 starter 自动配置代码。

#### Scenario: 聚合 POM 注册指定 starter 模块
- **WHEN** 检查 `fw-plus-ai-langchain4j` 聚合 POM
- **THEN** 它列出 Anthropic、Ollama、Azure AI Search、Azure OpenAI、Voyage AI、GitHub Models、Vertex AI Gemini、Elasticsearch、Redis、Qianfan、Milvus、DashScope 对应的子模块。

#### Scenario: 子模块复制指定 starter 代码
- **WHEN** 检查任一新增 starter 子模块 POM
- **THEN** 它不直接依赖对应的 `dev.langchain4j` Spring Boot starter artifact。
- **AND** 它包含从 LangChain4j `0.35.0` starter jar 包复制的 `src/main/java` 自动配置源码。
- **AND** 它声明对应的底层 `dev.langchain4j` provider/vector-store artifact，版本由 core 层 BOM 统一管理。

#### Scenario: 子模块不自动注册 AutoConfiguration imports
- **WHEN** 检查任一新增 starter 子模块资源
- **THEN** 它不包含 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。
- **AND** 它不包含 `META-INF/spring/org.springframework.boot.autoconfig.AutoConfiguration.imports`。

### Requirement: 保持现有 LangChain4j 模块可构建
系统 SHALL 在新增 starter 模块时保留 LangChain4j 聚合下已有的 `core` 和 `openai` 模块。

#### Scenario: LangChain4j 聚合构建成功
- **WHEN** 构建 core 侧 LangChain4j Maven reactor
- **THEN** 现有模块和新增模块都能解析并编译成功。
