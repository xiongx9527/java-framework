## 背景

`fw-plus-core` 已经存在 `fw-plus-ai-langchain4j` 聚合模块，但当前只包含 core 和 OpenAI 模块。补齐用户指定的 LangChain4j Spring Boot starter 包装模块，可以让 core 侧 AI 层按统一 Maven 模块方式暴露更多模型提供商和向量存储集成。

## 变更内容

- 在 `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j` 下新增 LangChain4j starter 包装模块。
- 在 `fw-plus-ai-langchain4j` 聚合 POM 中注册这些新模块。
- 每个新模块只依赖一个用户指定的 `dev.langchain4j` Spring Boot starter artifact，并使用指定版本。
- 本次变更只做 Maven 模块和依赖编排，不新增业务代码，也不改变现有 Java 公共 API 行为。

## 能力

### 新增能力
- `langchain4j-starter-modules`：按 fw-plus Maven 模块方式提供指定 LangChain4j starter 集成。

### 修改能力

## 影响范围

- 影响代码：`fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j`。
- 影响依赖：Anthropic、Ollama、Azure AI Search、Azure OpenAI、Voyage AI、GitHub Models、Vertex AI Gemini、Elasticsearch、Redis、Qianfan、Milvus、DashScope 对应的 LangChain4j starter。
- 构建影响：core 侧 LangChain4j Maven reactor 会新增一批子模块。
