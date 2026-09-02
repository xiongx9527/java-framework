# Outcome

恢复 Tinyflow 可替换 Chain Definition Repository 的完整类型契约，使默认内存实现、运行时服务与 Spring Boot 自动配置一致使用 `TinyflowChainDefinitionRepository`，解除 AI Boot 在 Java 8 下的编译阻塞。

# Scope

- 让 `InMemoryChainDefinitionRepository` 实现 `TinyflowChainDefinitionRepository`。
- 让 `DefaultTinyflowService` 的字段与构造器参数依赖 `TinyflowChainDefinitionRepository`，而不是具体内存实现。
- 使用现有 `TinyflowAutoConfigurationTest.shouldUseCustomWritableDefinitionRepositoryInsteadOfTheDefault` 验证自定义可写仓库能替换默认实现并同时被 `ChainExecutor` 与 `TinyflowService` 使用。
- 在 Java 8 下验证 Tinyflow Core 与 AI Boot direct consumer/condition 测试。

# Non-goals

- 不修改 Agentic change 或其候选代码。
- 不修改 Tinyflow 工作流执行、恢复、持久化或 LLM 行为。
- 不新增 Repository API，不改变 Bean 名称、条件装配规则或配置属性。
- 不升级依赖，不重构其他 Tinyflow 模块。
- 未经用户明确要求，不 commit、不 push、不创建 PR，也不合并到 `dev`。

# Acceptance examples

- **A1：默认仓库契约一致。** 给定未提供自定义 `TinyflowChainDefinitionRepository` 的应用上下文，当 Tinyflow 自动配置启动时，系统创建唯一默认 Definition Repository；该 Bean 同时可按 `TinyflowChainDefinitionRepository` 使用，并支持注册、查询和删除定义。
- **A2：自定义仓库完整替换默认实现。** 给定应用提供自定义 `TinyflowChainDefinitionRepository`，当 Tinyflow 自动配置启动并注册工作流时，系统不创建默认内存仓库，`ChainExecutor` 与 `TinyflowService` 使用同一个自定义仓库，注册后的定义可从该仓库查询。
- **A3：阻塞命令恢复。** 在 Java 8 下构建 Tinyflow Core 并执行 AI Boot `fw-plus-ai-boot-autoconfigure` direct consumer/condition 测试时，不再出现 `InMemoryChainDefinitionRepository` 与 `TinyflowChainDefinitionRepository` 互相不可转换的编译错误，相关测试通过。
- **A4：改动范围受控。** 候选实现只恢复 Tinyflow Definition Repository 的两处遗漏契约迁移及必要的正式产物；不包含 Agentic 候选、其他 Tinyflow 行为或无关文件改动。

# Constraints and invariants

- Java 8 与 Spring Boot 2.7.18 兼容。
- `TinyflowChainDefinitionRepository` 继续扩展上游 `ChainDefinitionRepository`，并保留 `register`、`remove` 可写操作。
- `DefaultTinyflowService` 只依赖该接口，不向下转型到默认实现。
- 保持 `TinyflowAutoConfiguration` 当前 Bean 名称、`@ConditionalOnMissingBean(TinyflowChainDefinitionRepository.class)` 与构造链不变。
- 修复必须独立于 `backport-langchain4j-agentic` change。

# Decisions

- 根因是提交 `f349d2b8` 原本同步完成的三处类型迁移在后续基线中只保留了 AutoConfiguration 一侧；修复恢复 Core 默认实现和 Service 构造器两处遗漏，不改 AutoConfiguration。
- 采用接口依赖而不是把 AutoConfiguration 回退到具体类，因为现有自定义仓库覆盖能力及测试明确要求可替换实现。
- 现有自定义仓库自动配置测试作为编译期与运行时 RED 用例；不为同一契约重复新增测试。
- 使用独立 Native worktree 和 change，避免污染 Agentic 候选。

# Open questions

无。

# Verification expectations

- Java 8 编译并测试 `fw-plus-ai-tinyflow-core`。
- Java 8 执行 `fw-plus-ai-boot-autoconfigure` 及其 reactor 依赖测试，重点确认 `TinyflowAutoConfigurationTest`。
- 执行 `git diff --check` 并核对候选路径不包含 Agentic change 文件。
