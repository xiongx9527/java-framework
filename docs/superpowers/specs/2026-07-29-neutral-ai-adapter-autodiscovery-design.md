---
comet_change: neutral-ai-adapter-autodiscovery
role: technical-design
canonical_spec: openspec
archived-with: 2026-07-30-neutral-ai-adapter-autodiscovery
status: final
---

# 中立 AI Adapter 自动发现技术设计

## 1. 范围与约束

本设计实现 Java 8 / Spring Boot 2.7.18 发布线的框架中立模型与向量存储装配。它删除旧 LangChain4j model/store 模块及 starter，新增统一 LangChain4j adapter，并让应用可按已安装 adapter 自动发现实现。

`fw-plus-ai-model`、`fw-plus-ai-store-embedding`、中立 starter、公共自动配置和 Magic-API model/vector 只允许使用 Java 8、中立门面和 registry。它们不得出现 LangChain4j、Spring AI、Spring AI Alibaba 或 AgentScope 2.0 的 POM 依赖、公开类型、导入、包名和配置前缀。

Spring AI、Spring AI Alibaba、AgentScope 2.0 的实现不属于本次 Java 8 改造。后续在独立 Java 17 / Spring Boot 3.x reactor 实现，复用本设计定义的中立 SPI。AgentScope 2.0 采用进程内 Java SDK，业务方自行选择是否开启 agent 或 workflow 能力。AskData 模块保留，并在其他 adapter、Boot 与 Magic-API 工作完成且用户再次明确确认后，作为最后一个集成任务迁移依赖和回归测试。

## 2. 目标模块边界

| 层次 | 模块 | 职责 | 不得依赖 |
| --- | --- | --- | --- |
| 中立 Core | `fw-plus-ai-model` | 模型定义、门面、registry、factory、选择器、异常、SPI | 所有具体 AI framework |
| 中立 Core | `fw-plus-ai-store-embedding` | 文档/向量门面、store registry、factory、选择器、异常、SPI | 所有具体 AI framework |
| 框架实现 | `fw-plus-ai-langchain4j-adapter` | LangChain4j 原生对象转换、provider 构造、模型和 store SPI | Spring Boot 自动配置职责 |
| 框架 Boot 集成 | `fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure` | LangChain4j 条件 Bean、provider Bean、配置绑定、adapter 注册 | 中立 Magic-API 逻辑 |
| 框架 Boot 集成 | `fw-plus-ai-langchain4j-adapter-spring-boot-starter` | 组合中立 starter、adapter 与 adapter 自动配置 | 业务 provider 强制依赖 |
| 中立 Boot | 现有中立 starter/公共自动配置 | 中立 properties、factory/registry、Magic-API 资源 | LangChain4j 和未来 framework |

Starter 是 adapter 的 Spring Boot 集成实现，不是其唯一装配形式。非 Spring 应用可以直接依赖 adapter 并显式传入 SPI 实例；未来每种 framework 可以各自提供可选 adapter starter。

## 3. Adapter SPI 与选择算法

模型和向量 store 各保留现有独立 SPI，避免为本次变更新增第三个公共抽象模块：

- `AiModelAdapter` 增加 framework 标识与对模型 provider/type 的支持声明。
- `AiEmbeddingStoreAdapter` 增加 framework 标识与对 store provider/type 的支持声明。
- 两个中立 factory 各自拥有小型选择器，算法相同但不让 model/store 相互依赖。

选择器输入为一个中立 definition 与已发现 adapter 列表，输出为恰好一个 adapter 或受控异常：

1. `framework` 非空：过滤 framework 相等的 adapter，再验证 provider/type；没有精确候选即失败。
2. `framework` 为空：仅按 provider/type 计算候选；候选数为 1 时使用该 adapter，候选数为 0 或大于 1 时失败。
3. 成功创建的中立门面必须公开实际 adapter 的 framework、provider、type 与稳定 key。
4. 失败信息必须包含 key、请求 framework、provider、type 和候选 framework；不得输出 apiKey、secret、token 或完整连接串。

选择器不得通过 Bean 顺序、类名、LangChain4j 优先级或空实现消除歧义。只安装 LangChain4j adapter 时，其作为唯一候选自动生效，因此维持“默认 LangChain4j”的使用体验。

## 4. Boot 装配与配置

中立 Boot 自动配置只绑定以下中立配置：

- `fw-plus.ai.model`
- `fw-plus.ai.embedding-store`

它创建中立 factory、registry 和 Magic-API 所需的动态资源支持，并从 Spring 容器收集 adapter Bean。中立模块没有旧 `fw-plus.ai.langchain4j.*` 兼容绑定。

LangChain4j adapter 自动配置只在 adapter 和所需 LangChain4j API 位于 classpath 时注册 provider adapter 与实现 adapter。它不注册中立 registry，也不让 Magic-API 依赖原生 LangChain4j 类型。具体 provider SDK 仍按已有 `@ConditionalOnClass` 条件提供，避免 adapter starter 传递所有 provider SDK。

旧 `LangChain4jModelAutoConfiguration`、`EmbeddingStoreAutoConfiguration` 中的中立 factory/registry、旧配置迁移、legacy adapter 与原生 registry 逻辑必须拆开：中立部分迁入公共自动配置，LangChain4j 专属部分迁入 adapter 自动配置，旧兼容桥接删除。

## 5. 依赖与代码迁移

删除以下公开 artifact 和目录：

- `fw-plus-ai-langchain4j-model`
- `fw-plus-ai-langchain4j-store-embeding`
- `fw-plus-ai-langchain4j-model-spring-boot-starter`
- `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter`

迁移顺序：

1. 先建立中立选择器和单元测试，保持 factory 创建入口稳定。
2. 将旧 model/store 中的 LangChain4j provider 构造、包装对象和转换逻辑迁入统一 adapter。
3. 将所有 LangChain4j 自动配置、provider Bean 和条件装配迁入 adapter 自动配置。
4. 将 provider 子模块、Magic-API model/vector starter、样例、聚合 POM 和 BOM 指向统一 adapter 或中立 API。
5. 最后迁移 AskData starter 的旧 artifact 依赖并完成目标回归测试，再删除旧 artifact。
6. 删除旧目录、旧 POM 坐标、旧配置类、旧 registry 类型与兼容桥接。

Magic-API 的 `model[...]` 和 `vector[...]` 保持业务 key 语义。动态资源的 `framework` 使用同一选择器；缺省时仅唯一候选可用。脚本侧只取得中立门面，不再获得 `LangChain4jModel` 或 `EmbeddingStore<TextSegment>`。

## 6. 测试设计

### 6.1 Core 单元测试

- 模型和 store 分别覆盖显式 framework 成功、显式 framework 缺失、唯一候选、零候选、多候选。
- 断言异常包含诊断上下文且不包含敏感配置。
- 保留并迁移默认 key、多实例、重复 key、未支持 provider/type 和 adapter 运行时失败测试。

### 6.2 Boot 与集成测试

- 中立 starter 在 classpath 不含 LangChain4j 时仍可加载中立 factory/registry；没有 adapter 的已启用 definition 按规则失败。
- 引入 LangChain4j adapter starter 后，唯一候选自动创建模型/store；多个测试 adapter 时缺省 framework 失败、显式 `langchain4j` 成功。
- Magic-API model/vector 动态资源只通过中立 registry 工作，且不暴露原生类型。
- AskData、Magic-API 和样例在旧 artifact 删除后仍可通过目标测试编译和运行。

### 6.3 静态与构建验证

- 对中立模块、starter、公共自动配置和 Magic-API 执行 POM/源码扫描，禁止具体 framework 坐标、导入、公开签名和旧前缀。
- Core reactor 先构建，再构建 AI Boot 目标模块；检查 Maven dependency tree 不再解析四个旧 artifact。
- 运行 OpenSpec 严格校验与 `git diff --check`。

## 7. 升级、失败诊断与回滚

这是破坏性升级。文档必须逐项列出旧依赖和新依赖：应用用 LangChain4j 时显式增加统一 adapter starter；模型与向量 store 改用中立前缀；多 adapter 时为每个 definition 设置 framework。

禁止混装新统一 adapter 与四个旧 artifact。升级失败时先用 Maven dependency tree 查找残留旧坐标，再检查缺失 adapter、未安装 provider SDK 或多个候选。回滚方式是回退到仍发布旧模块的上一框架版本，而不是在新版本中恢复旧 artifact。

## 8. Java 17 / Spring Boot 3.x 后续蓝图

后续 change 创建独立 Java 17 parent、BOM、adapter reactor 和 Boot 3 starter。该线使用 `jakarta.*` 命名空间、Boot 3 自动配置注册方式和 Java 17 编译级别，不修改当前 Java 8 发布物。

每个后续 adapter 都必须：

- 实现相同的模型与向量 store 中立 SPI；
- 明确 provider/type 能力并遵守相同唯一候选规则；
- 提供可选 Spring Boot 3 starter，而不污染中立 Core；
- 覆盖 classpath 条件、显式 framework、多 adapter 和敏感信息脱敏测试；
- 文档声明尚不支持的 framework 特性。

Spring AI 与 Spring AI Alibaba 的版本、provider 能力矩阵和 artifact 命名由该后续 change 根据目标 BOM 确定。AgentScope 2.0 adapter 以进程内 SDK 适配业务可用的模型/工具/agent 能力；网络协议、A2A/MCP 服务桥接和跨进程编排不属于该 adapter 的默认职责。

## 9. 风险控制

| 风险 | 控制措施 |
| --- | --- |
| provider 迁移遗漏 | 先迁移测试，再迁移实现；逐项保留 provider/type 覆盖 |
| 多 adapter 破坏历史缺省配置 | 明确失败并给出候选；文档要求配置 framework |
| 中立边界回退 | 静态依赖、导入、公开签名、配置前缀四层检查 |
| Magic-API 暴露原生对象 | 集成测试注入中立 fake registry，禁止原生类型导入 |
| 新旧坐标混用 | BOM、聚合、样例和依赖树检查全部禁止旧 artifact |
