## Context

当前 `LangChain4jAutoConfiguration` 仅通过 `@Import` 导入 `LangChain4jCoreAutoConfiguration`，而 core 层 `fw-plus-ai-langchain4j` 下多个本地 starter 模块的自动配置类没有被 fw-plus AI 聚合配置纳入。部分原始配置类内部已有以 api-key、base-url 或 embedding-store enabled 为条件的 bean 级启用逻辑，但聚合导入本身缺少 fw-plus 侧模块级开关。

## Goals / Non-Goals

**Goals:**

- 让 `LangChain4jAutoConfiguration` 明确导入 core、OpenAI、Anthropic、Ollama、Azure AI Search、Azure OpenAI、Voyage AI、GitHub Models、Vertex AI Gemini、Elasticsearch、Redis、Qianfan、Milvus、DashScope 对应的配置。
- 每个被导入的 fw-plus wrapper 都具备 `enabled` 启动条件，默认启用，允许通过配置关闭。
- `LangChain4jCoreAutoConfiguration` 补齐启动条件。

**Non-Goals:**

- 不修改各 LangChain4j starter 原始 `AutoConfig` 的 bean 创建逻辑。
- 不调整 Maven 依赖版本或 starter 模块源码来源。
- 不新增运行时连接校验。

## Decisions

- 使用 fw-plus 自有 wrapper 配置类承载 `@ConditionalOnProperty`，再 `@Import` 原始 LangChain4j 配置类。这样可以避免修改复制自 LangChain4j starter 的源码，并保持条件位于 `LangChain4jAutoConfiguration` 导入链路上。
- 使用 `fw.plus.ai.langchain4j.<module>.enabled` 作为 fw-plus 聚合层开关，`matchIfMissing = true` 与项目现有自动配置默认启用风格一致。
- 对多个包名相同类名 `AutoConfig` 使用全限定类名写在 `@Import` 中，避免 Java import 别名不可用导致命名冲突。

## Risks / Trade-offs

- [Risk] 默认导入更多 wrapper 配置类可能扩大 Spring 条件评估范围。Mitigation: wrapper 仅导入现有配置类，实际 bean 创建仍受原始配置类内部条件和 `@ConditionalOnMissingBean` 约束。
- [Risk] enabled 属性名需要与团队配置习惯保持一致。Mitigation: 使用已有 `fw.plus.*` 风格，并只添加模块级开关，不替换原始 `langchain4j.*` 属性。
