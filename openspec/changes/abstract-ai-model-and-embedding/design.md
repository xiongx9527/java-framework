## Context

当前 Core AI 层将 `LangChain4jModel`、`LangChain4jModelRegistry`、`EmbeddingStore<TextSegment>`、`EmbeddingStoreRegistry` 等框架类型放在 `fw-plus-ai-langchain4j-model` 和 `fw-plus-ai-langchain4j-store-embeding` 中。AI Boot 自动配置和 Magic-API model/vector 插件因此直接依赖 LangChain4j；已有配置分别使用 `fw-plus.ai.langchain4j.model` 与 `fw-plus.ai.langchain4j.embedding-store`。

本次是跨 Core、AI Boot、starter、Magic-API 与 BOM 的架构调整。框架基线仍受 Core Java 8 与 Spring Boot 2.7.18 约束；Spring AI、Spring AI Alibaba、AgentScope 2 Java 的实际版本兼容性必须通过有效 POM、依赖树和编译验证确认，不能仅依据 BOM 属性推断。

## Goals / Non-Goals

**Goals:**

- 将模型和向量存储的公共 API 下沉到 `fw-plus-ai-model` 与 `fw-plus-ai-store-embedding`，不暴露任何特定 AI 框架类型。
- 保留 LangChain4j 作为默认完整实现；原 `fw-plus-ai-langchain4j-*` 模块变为适配模块和兼容入口。
- 为 Spring AI、Spring AI Alibaba、AgentScope 2 Java 后续作为进程内 SDK 适配器接入同一注册、配置、starter 与 Magic-API 生命周期提供蓝图。
- 保持旧 LangChain4j 配置、资源 key 与脚本访问可用，并提供从 LangChain4j 切换到其他框架的明确文档和回滚路径。
- 为 API 契约、每个适配器能力、兼容层、自动配置、Magic-API 和 Maven 分层构建建立自动化测试。

**Non-Goals:**

- 不迁移用户已有运行态 Magic-API 资源、密钥或业务代码。
- 不把所有框架的原生 agent/workflow、tool、memory、graph 等高阶能力强行抽象为统一接口。
- 不在公共模块传递具体厂商 SDK，也不承诺把不满足 Java 8/Spring Boot 2.7 前置条件的 SDK 编译进默认 LangChain4j 路径。
- 本次不创建 Java 17 / Spring Boot 3.x reactor、parent、BOM、artifact、starter、自动配置或真实目标 SDK adapter。
- 不改变向量数据库的 provider 连接协议或现有 Magic-API 资源格式，除非规格明确增加框架选择字段。

## Decisions

### 1. 模块分层采用中立核心和可选框架适配器

新增 Core 模块：

```
fw-plus-ai/                                      # 本次 Java 8 交付
├── fw-plus-ai-model/
├── fw-plus-ai-store-embedding/
├── fw-plus-ai-langchain4j/
│   ├── fw-plus-ai-langchain4j-model/
│   └── fw-plus-ai-langchain4j-store-embeding/
```

后续 change 才可在独立 Java 17 / Spring Boot 3.x reactor 创建 Spring AI、Spring AI Alibaba 与 AgentScope 2 Java 的 parent、BOM、starter 和 adapter。它们不是本次模块布局的一部分，不能加入 Java 8 Core reactor 或 Core BOM。

`fw-plus-ai-model` 提供模型定义、能力类型、统一模型门面、工厂、注册表、异常和 `AiModelAdapter` SPI；`fw-plus-ai-store-embedding` 提供中立文档、向量、查询、匹配结果、store 门面、工厂、注册表和 `AiEmbeddingStoreAdapter` SPI。公共 API 使用 Java 8 可用类型，禁止导入 `dev.langchain4j.*`、`org.springframework.ai.*`、Spring AI Alibaba 或 AgentScope 类型。

各框架适配模块负责原生对象构建和中立门面转换，并通过 `supports`/能力声明公开可用模型和向量能力。调用者只能通过中立门面访问通用能力；需要框架专有能力时必须显式、受控地获取原生对象，不能让框架类型重新渗入公共模块、starter 或 Magic-API 脚本边界。

选择此方案而不是直接抽取 `Object` 型注册表，是为了保留可测试的输入输出、能力校验和异常上下文；选择此方案而不是同时统一 agent/workflow，是为了避免把能力差异最大的 API 固化为最低公分母。

### 2. LangChain4j 是默认适配器，其他适配器按能力显式注册

`fw-plus-ai-langchain4j-model` 与 `fw-plus-ai-langchain4j-store-embeding` 保留 artifact 兼容入口，但不再定义公共契约。它们依赖新的中立模块并注册 LangChain4j 实现，维持现有 provider、默认模型、store key 和 `EmbeddingStore<TextSegment>` 的适配行为。

后续 change 中，Spring AI、Spring AI Alibaba 和 AgentScope 2 Java 各自注册独立 adapter。每个 adapter 必须声明实际支持的模型/向量能力；配置请求未支持能力时必须在启动期报告框架、provider、key 和能力类型。禁止通过空实现伪造支持，也禁止由默认 LangChain4j adapter 隐式处理其他框架的配置。AgentScope 2 Java 必须作为进程内 SDK 适配，不能以 HTTP、MCP 或 A2A 桥接替代。

### 3. 配置先中立、后兼容映射，且中立配置优先

新增中立配置前缀 `fw-plus.ai.model` 与 `fw-plus.ai.embedding-store`，其中每个定义显式包含 `framework`、provider、能力类型、key、连接参数和扩展 properties。旧 `fw-plus.ai.langchain4j.model` 与 `fw-plus.ai.langchain4j.embedding-store` 继续被读取，并在未声明同 key 中立定义时映射为 `framework=langchain4j` 的中立定义。

同一 key 同时存在时，中立配置优先，启动日志必须给出可操作迁移提示但不得输出密钥。旧配置在一个兼容周期内保留；移除兼容入口只能通过后续破坏性 change 并附迁移公告完成。

### 4. starter 与 Magic-API 只依赖中立注册表

本次新增中立 model/store starter；原 LangChain4j starter 继续可用，并转调中立 starter 加 LangChain4j adapter，保证默认用户无需改依赖即可升级。后续 change 中，每个目标框架 adapter starter 才可带入其 adapter、可选 SDK 和条件自动配置；本次不提供这些 starter 或其启动诊断。

Magic-API model 和 vector 插件改为注入中立 registry/factory。既有 `model-datasource`、向量 store key、`model[...]` 和 `vector[...]` 访问形式维持不变；资源定义可通过可选 `framework` 指定 adapter，未指定时按已解析的默认模型/store 选择。插件返回中立门面，provider 连接逻辑只允许位于对应 adapter 中。

### 5. 切换迁移文档是交付物，不是附注

文档必须按目标框架分别给出：依赖替换、BOM/版本前置条件、配置映射、Java 注入 API 替换、Magic-API 资源与脚本影响、测试方式、回滚步骤和未支持能力。LangChain4j 到 Spring AI、Spring AI Alibaba、AgentScope 2 Java 的示例必须使用相同业务 key，展示仅替换 adapter/starter 与 `framework` 后模型和 vector 脚本如何保持访问方式稳定。

### 6. 以契约测试隔离 SDK 差异

中立核心使用假 adapter/store 覆盖注册、默认选择、重复 key、能力不支持和异常上下文。每个真实 adapter 用 SDK mock 或本地 fake 覆盖其声明能力，禁止测试依赖真实 API key 或远程向量库。自动配置测试分别验证旧 LangChain4j 配置兼容、中立配置优先、SDK 缺失的条件退化和框架冲突。Magic-API 集成测试验证资源动态注册、按 key/default 访问及错误脱敏；Maven 测试按 Core 后 AI Boot 顺序执行。

## Risks / Trade-offs

- [Spring AI、Spring AI Alibaba 或 AgentScope 2 Java 的最低 JDK/Spring 基线高于当前项目] → 在开始实现前生成有效 POM 和依赖树并执行独立编译探针；不兼容时将 adapter 保持可选且隔离，记录支持基线，不污染默认路径。
- [中立 API 不能覆盖所有原生能力] → 仅规范模型和向量存储共同能力，adapter 声明能力，框架专有高级功能留在各自模块。
- [配置兼容映射导致同 key 行为不明确] → 定义中立配置优先、旧配置仅补全、启动期提示的固定优先级，并以测试锁定。
- [旧 artifact 过早删除破坏下游] → 旧 LangChain4j artifact 保留为薄兼容/适配模块；迁移文档提供逐步替换和回滚。
- [Magic-API 动态资源可能携带明文凭证] → 所有控制器、异常和迁移示例均按脱敏规则处理密钥，测试覆盖错误输出。

## Migration Plan

1. 在 Core 聚合和 BOM 中新增中立模块，将公共类移动到中立包；把原 LangChain4j 模块改为适配器并保留兼容 artifact。
2. 让 AI Boot、starter 和 Magic-API 切换到中立 registry/factory；先通过旧 LangChain4j 配置回归测试，再开放中立配置。
3. 后续独立 change 再逐个增加 Spring AI、Spring AI Alibaba、AgentScope 2 Java adapter 和 starter，先完成 SDK 版本探针、依赖/JDK 编译前置验证，再补齐其声明能力的测试；本次不创建 Java 17 reactor 或执行这些构建。
4. 发布迁移指南：业务方先替换或增加目标框架 starter，再将同一 key 的配置转换为中立格式并设置 `framework`，最后把 Java 代码从旧 registry/原生类型迁为中立 API。Magic-API key 与基础脚本无需变更。
5. 回滚时移除目标框架 adapter/starter，恢复或保留旧 LangChain4j starter 和旧配置；兼容层确保同 key 的 LangChain4j 定义可重新生效。

## Open Questions

- Spring AI、Spring AI Alibaba 和 AgentScope 2 Java 在目标 Java 17 / Spring Boot 3.x 版本组合上的实际兼容性，需由后续 change 的 SDK 版本探针确认；本次不对其发布坐标、JDK 或 Spring 基线作已验证承诺。
- AgentScope 2 Java 对嵌入和向量存储的原生支持范围需以实际 SDK 版本验证；未支持部分只能声明为不支持，不能复用其他框架实现。
- 旧 `fw-plus-ai-langchain4j-*` artifact 的弃用周期、是否增加新 artifact alias，以及中立配置的最终命名需在实现计划中结合下游使用情况确认。
