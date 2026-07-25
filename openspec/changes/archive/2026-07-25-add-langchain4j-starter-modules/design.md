## 上下文

core 侧 AI 层使用 `fw-plus-core/fw-plus-project/fw-plus-ai` 下的 Maven 聚合结构。现有 `fw-plus-ai-langchain4j` 聚合模块已经包含 `fw-plus-ai-langchain4j-core` 和 `fw-plus-ai-langchain4j-openai`，其中 `openai` 模块已复制 LangChain4j Spring Boot starter 的自动配置源码。因此新增 starter 应沿用同级子模块布局，并从 LangChain4j `0.35.0` 的 starter jar 包复制自动配置 Java 源码，而不是只依赖外部 starter。

## 目标 / 非目标

**目标：**
- 为用户指定的每个 LangChain4j Spring Boot starter 新增一个 fw-plus 子模块。
- 从 `0.35.0` 的 starter jar 包复制 `src/main/java` 自动配置源码。
- 不复制 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`，避免新增模块被自动导入。
- 让新模块 POM 依赖底层 `langchain4j-*` provider/vector-store artifact，而不是外部 `*-spring-boot-starter`。
- 将新增 jar 模块纳入 core 层 `fw-plus-dependencies` 的 dependencyManagement。
- 保持 fw-plus-core 的 Java 8 和 Spring Boot 2.7.x 约束。

**非目标：**
- 不新增应用业务逻辑或 Magic-API 集成代码。
- 不迁移现有 AI boot autoconfigure 代码。
- 不改变模型配置行为。

## 决策

- 每个请求的 starter artifact 对应一个 Maven 子模块，便于消费者按需引入。
- 所有新模块放在 `fw-plus-ai-langchain4j` 下，因为本次请求明确指定 core 侧 LangChain4j 聚合路径。
- 源码来自 LangChain4j `0.35.0` 的 starter jar 包；`vertex-ai-gemini` 也按用户后续要求统一使用 `0.35.0` jar 包内容。
- 模块 POM 只声明底层 `langchain4j-*` artifact，版本由 core 层 `langchain4j-bom` 的 `0.35.0` 统一管理。
- core 层 `fw-plus-dependencies` 使用 `${project.version}` 管理新增的 `com.xiongx9527.fw.plus:fw-plus-ai-langchain4j-*` jar 模块，与已有 `core/openai` 模块保持一致。
- 模块命名使用 `fw-plus-ai-` 前缀并贴近 artifact 名称，例如 `fw-plus-ai-langchain4j-anthropic`。

## 风险 / 权衡

- 新子模块会增加 reactor 模块数量和本地维护源码数量。缓解方式：保持 Java 代码为上游 `0.35.0` starter jar 包的机械复制，并用目标 reactor 构建验证。
- provider/vector-store artifact 可能带来专属传递依赖。缓解方式：通过 `clean test` 验证 Maven 依赖解析和 Java 8 编译。

## Tweak 升级判定记录

- 本次变更新增文件数超过 6，已触发 tweak 文件数提示阈值。
- 用户选择继续 tweak 轻量流程，初始原因为模块编排不引入运行时架构变化；后续用户纠偏要求从 `0.35.0` jar 包复制 starter 自动配置 Java 源码且不复制 AutoConfiguration imports，仍限定在同一个 Maven 聚合能力内，不引入新的应用业务流程或跨层架构协调。
