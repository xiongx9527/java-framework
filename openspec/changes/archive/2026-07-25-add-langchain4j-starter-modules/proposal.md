## 背景

`fw-plus-core` 已经存在 `fw-plus-ai-langchain4j` 聚合模块，但当前只包含 core 和 OpenAI 模块。补齐用户指定的 LangChain4j Spring Boot starter 本地模块，可以让 core 侧 AI 层按统一 Maven 模块方式暴露更多模型提供商和向量存储集成，并保留本地维护这些 starter 自动配置代码的能力。

## 变更内容

- 在 `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j` 下新增 LangChain4j starter 本地模块。
- 在 `fw-plus-ai-langchain4j` 聚合 POM 中注册这些新模块。
- 每个新模块从 LangChain4j `0.35.0` 对应 starter jar 包复制自动配置 Java 源码。
- 不复制 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`，避免模块被 Spring Boot 自动导入。
- 每个新模块依赖对应的底层 `dev.langchain4j` provider/vector-store artifact，不再直接依赖外部 `*-spring-boot-starter`。
- 将新增的 core 侧 `fw-plus-ai-langchain4j-*` jar 模块加入 `fw-plus-core/fw-plus-project/fw-plus-dependencies` 统一版本管理。
- 本次变更不新增应用业务代码，也不改变现有 Java 公共 API 行为。

## 能力

### 新增能力
- `langchain4j-starter-modules`：按 fw-plus Maven 模块方式提供指定 LangChain4j starter 的本地自动配置源码。

### 修改能力

## 影响范围

- 影响代码：`fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j`、`fw-plus-core/fw-plus-project/fw-plus-dependencies`。
- 影响依赖：Anthropic、Ollama、Azure AI Search、Azure OpenAI、Voyage AI、GitHub Models、Vertex AI Gemini、Elasticsearch、Redis、Qianfan、Milvus、DashScope 对应的底层 LangChain4j provider/vector-store artifact。
- 构建影响：core 侧 LangChain4j Maven reactor 会新增一批子模块。
