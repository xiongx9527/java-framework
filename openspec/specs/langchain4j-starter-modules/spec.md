# langchain4j-starter-modules Specification

## Purpose
TBD - created by archiving change add-langchain4j-starter-modules. Update Purpose after archive.
## Requirements
### Requirement: 暴露指定 LangChain4j starter 本地模块
系统 SHALL 在 `fw-plus-ai-langchain4j` 下为每个用户指定的 LangChain4j Spring Boot starter 暴露一个 fw-plus Maven 模块，并在模块内保留对应 starter 自动配置代码。系统还 SHALL 暴露 `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter`，用于统一管理多向量 store starter 能力。

#### Scenario: 聚合 POM 注册指定 starter 模块
- **WHEN** 检查 `fw-plus-ai-langchain4j` 聚合 POM
- **THEN** 它列出 Anthropic、Ollama、Azure AI Search、Azure OpenAI、Voyage AI、GitHub Models、Vertex AI Gemini、Elasticsearch、Redis、Qianfan、Milvus、DashScope 对应的子模块。
- **AND** 它列出 `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter` 子模块。

#### Scenario: 子模块复制指定 starter 代码
- **WHEN** 检查任一新增 starter 子模块 POM
- **THEN** 它不直接依赖对应的 `dev.langchain4j` Spring Boot starter artifact。
- **AND** 它包含从 LangChain4j `0.35.0` starter jar 包复制的 `src/main/java` 自动配置源码。
- **AND** 它声明对应的底层 `dev.langchain4j` provider/vector-store artifact，版本由 core 层 BOM 统一管理。

#### Scenario: 子模块不自动注册 AutoConfiguration imports
- **WHEN** 检查任一新增 starter 子模块资源
- **THEN** 它不包含 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。
- **AND** 它不包含 `META-INF/spring/org.springframework.boot.autoconfig.AutoConfiguration.imports`。

#### Scenario: core BOM 管理新增 jar 模块
- **WHEN** 检查 `fw-plus-core/fw-plus-project/fw-plus-dependencies` 的 dependencyManagement
- **THEN** 它包含 Anthropic、Ollama、Azure AI Search、Azure OpenAI、Voyage AI、GitHub Models、Vertex AI Gemini、Elasticsearch、Redis、Qianfan、Milvus、DashScope 对应的 `fw-plus-ai-langchain4j-*` jar 模块。
- **AND** 它包含 `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter` jar 模块。
- **AND** 这些模块使用 `${project.version}` 作为版本。

#### Scenario: fw-plus AI 聚合配置导入本地 starter 自动配置
- **WHEN** 检查 `LangChain4jAutoConfiguration`
- **THEN** 它导入 core、Anthropic、Ollama、Azure AI Search、Azure OpenAI、Voyage AI、GitHub Models、Vertex AI Gemini、Elasticsearch、Redis、Qianfan、Milvus、DashScope 对应的 fw-plus 配置类。
- **AND** 每个被导入配置类都具备 `enabled` 启动条件。

#### Scenario: core 自动配置具备启动条件
- **WHEN** 检查 `LangChain4jCoreAutoConfiguration`
- **THEN** 它导入 `dev.langchain4j.spring.LangChain4jAutoConfig`。
- **AND** 它具备 `enabled` 启动条件。

### Requirement: 保持现有 LangChain4j 模块可构建
系统 SHALL 在新增 starter 模块时保留 LangChain4j 聚合下已有的 `core` 和 `openai` 模块。

#### Scenario: LangChain4j 聚合构建成功
- **WHEN** 构建 core 侧 LangChain4j Maven reactor
- **THEN** 现有模块和新增模块都能解析并编译成功。

