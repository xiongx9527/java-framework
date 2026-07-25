## 上下文

core 侧 AI 层使用 `fw-plus-core/fw-plus-project/fw-plus-ai` 下的 Maven 聚合结构。现有 `fw-plus-ai-langchain4j` 聚合模块已经包含 `fw-plus-ai-langchain4j-core` 和 `fw-plus-ai-langchain4j-openai`，因此新增 starter 应沿用同级子模块布局，而不是把依赖直接堆到 core 模块或新增运行时代码。

## 目标 / 非目标

**目标：**
- 为用户指定的每个 LangChain4j Spring Boot starter 新增一个 fw-plus 子模块。
- 让新模块 POM 保持轻量，并与现有 LangChain4j 模块风格一致。
- 保持 fw-plus-core 的 Java 8 和 Spring Boot 2.7.x 约束。

**非目标：**
- 不新增应用业务逻辑、自动配置实现或 Magic-API 集成代码。
- 不迁移现有 AI boot autoconfigure 代码。
- 不改变模型配置行为。

## 决策

- 每个请求的 starter artifact 对应一个 Maven 子模块，便于消费者按需引入。
- 所有新模块放在 `fw-plus-ai-langchain4j` 下，因为本次请求明确指定 core 侧 LangChain4j 聚合路径。
- 模块 POM 中直接声明用户要求的版本：大多数 starter 使用 `0.35.0`，`vertex-ai-gemini` 使用 `0.36.0`。
- 模块命名使用 `fw-plus-ai-` 前缀并贴近 artifact 名称，例如 `fw-plus-ai-langchain4j-anthropic`。

## 风险 / 权衡

- 新子模块会增加 reactor 模块数量。缓解方式：每个模块保持 POM-only，并用目标 reactor 构建验证。
- starter artifact 可能带来提供商专属传递依赖。缓解方式：通过 `clean test` 验证 Maven 依赖解析。

## Tweak 升级判定记录

- 本次变更预计新增超过 6 个文件，已触发 tweak 文件数提示阈值。
- 用户选择继续 tweak 轻量流程，原因是本次改动是 POM-only 的模块编排，不引入运行时架构变化。
