## Context

上一轮抽象已将模型和向量存储的业务契约放入 Java 8 的 `fw-plus-ai-model` 与 `fw-plus-ai-store-embedding`。但当前 AI Boot 自动配置仍在同一装配路径中直接创建 LangChain4j registry、factory 与原生 `EmbeddingStore<TextSegment>`，中立 starter 也传递旧 LangChain4j 模块。四个旧 artifact 仍被 BOM、聚合 POM、Magic-API 和样例引用。

这使中立模块的依赖边界与运行时行为不一致：声明 `framework` 时不能明确验证适配器，未声明时也不能安全决定默认实现。未来的 Spring AI、Spring AI Alibaba 与 AgentScope 2 Java 需要 Java 17 / Spring Boot 3.x；把它们加入当前 Java 8 / Boot 2.7 reactor 会破坏基线。

## Goals / Non-Goals

**Goals:**

- 删除 4 个旧 LangChain4j model/store artifact 及其全部聚合、BOM、starter、Magic-API 和样例引用。
- 提供 `fw-plus-ai-langchain4j-adapter`，作为唯一包含 LangChain4j 类型的 Java 8 默认实现，并通过独立 adapter starter 显式安装。
- 使模型与向量 store 的中立配置支持 adapter 发现：显式 framework 精确匹配；缺省 framework 时仅唯一候选可用；零或多候选必须失败。
- 保证中立 Core、中立 starter、公共自动配置和 Magic-API 只依赖中立 registry/SPI，不出现具体 AI 框架的依赖、类型、包名或配置前缀。
- 以可执行测试和中文迁移文档覆盖移除、自动选择、错误分支、Magic-API 与回滚；记录 Java 17 / Boot 3.x 的后续实现蓝图。

**Non-Goals:**

- 本次不实现 Spring AI、Spring AI Alibaba 或 AgentScope 2 Java adapter，不升级 Java 8 / Spring Boot 2.7 基线，也不引入跨进程 AgentScope 协议。
- 不兼容旧 artifact 或 `fw-plus.ai.langchain4j.*` 配置；这是一次明确的破坏性迁移。
- 不改变具体 LangChain4j provider SDK 的现有职责，也不重写 Magic-API 的脚本语义。
- 不迁移 AskData 的依赖、源码或测试；该模块留给独立 change。

## Decisions

### 1. 统一 LangChain4j adapter 替代两个旧 Core artifact

在 `fw-plus-ai-langchain4j` 聚合下新增 `fw-plus-ai-langchain4j-adapter`，承载原 `fw-plus-ai-langchain4j-model` 与 `fw-plus-ai-langchain4j-store-embeding` 的 LangChain4j 对象转换、provider 构造和中立 SPI 实现。新增 `fw-plus-ai-langchain4j-adapter-spring-boot-starter` 显式引入 adapter 与其隔离的自动配置；应用不再通过中立 starter 隐式获得 LangChain4j。

选择该方案而不是保留兼容 POM，是因为保留旧坐标会持续给调用方传递 LangChain4j 依赖，并无法满足“旧模块不应存在”的发布边界。选择单一 adapter 而不是 model/store 两个 adapter，可将一套框架生命周期、条件装配与版本管理集中维护。

### 2. 在中立 SPI 中定义可发现 framework，而非以 provider 推断框架

`AiModelAdapter` 与 `AiEmbeddingStoreAdapter` 扩展为能够声明其 framework 标识和支持能力的 SPI；中立工厂/选择器接收当前 classpath 已发现的 adapter 集合。选择算法对模型和 store 一致：

1. 定义显式给出 `framework` 时，只保留同名 adapter；不存在或无法支持 provider/type 时以包含 key、framework、provider、type 的受控异常失败。
2. 定义未给出 `framework` 时，先按 provider/type 计算候选；恰好一个候选时使用该 adapter，并将实际 framework 写入诊断元数据；零个或多个候选时启动失败，错误列出候选 framework。
3. 不以 adapter 排序、Bean 名称或 LangChain4j 作为隐式优先级，不回退到空实现。

这比“始终默认 LangChain4j”更安全：LangChain4j 仍是只安装一个 adapter 时的默认实现，但应用显式安装多个 adapter 后必须消除歧义。

### 3. 拆分中立与框架特定 Spring Boot 装配

中立 properties、registry、factory、动态 Magic-API 数据源只保留 `fw-plus.ai.model`、`fw-plus.ai.embedding-store` 与中立 API。LangChain4j adapter 的 properties、原生 Bean 适配和 provider 逻辑放入 adapter starter 的自动配置，条件为 adapter 类与 LangChain4j SDK 同时存在。公共自动配置不得导入、反射加载或以字符串检测任何具体 AI 框架。

替代方案是在现有 LangChain4j 自动配置中继续桥接旧配置。该方案被拒绝，因为会令中立装配仍需 LangChain4j，并使之后的 Java 17 adapter 复制桥接代码。

### 4. Magic-API 只消费中立 registry

model 与 vector 脚本模块、动态资源、控制器和 legacy support 一律使用 `AiModelRegistry`、`AiEmbeddingStoreRegistry` 及中立门面。资源定义中的 `framework` 交给选择器处理；Magic-API 不创建 LangChain4j 原生对象，也不依赖 adapter 包。无 framework 的资源仅在唯一候选时可创建。

### 5. 将 Java 17 / Boot 3 作为独立后续 reactor

文档定义未来 `java17` / Boot 3.x 线：独立 parent/BOM/reactor，使用 `jakarta.*`、Spring Boot 3 自动配置注册方式和 Java 17 编译级别；每个 Spring AI、Spring AI Alibaba、AgentScope 2 Java adapter 都实现相同中立 SPI。AgentScope 2 采用进程内 Java SDK，业务层决定是否启用其 agent/workflow 能力。该路线不向当前 Java 8 artifact 回传依赖。

## Risks / Trade-offs

- [删除 artifact 会导致旧依赖立即解析失败] → 文档给出逐项坐标替换、配置替换、回滚至前一发布版本和 Maven 依赖树诊断命令。
- [多个 adapter 让缺省 framework 的历史配置失败] → 错误列出候选，文档要求在定义中显式写 framework；单 adapter 部署保持自动选择。
- [provider 构造逻辑从旧模块迁移时遗漏能力] → 先补充 RED 测试，逐项迁移 model/store provider、默认 key、多实例和异常上下文，再删除旧目录。
- [Magic-API 仍隐式持有原生类型] → 编译依赖、源码导入和集成测试三层检查；测试从中立 registry 注入假实现。
- [未来 Java 17 adapter 与 Java 8 二进制混用] → 文档明确分仓库 reactor 与版本线，不承诺当前 Boot 2 应用可直接加载 Java 17 adapter。

## Migration Plan

1. 在 Core BOM、根 AI 聚合与 LangChain4j 聚合中注册统一 adapter，先建立中立 SPI 自动选择测试。
2. 将 LangChain4j model/store 的 provider 实现与自动配置迁入 adapter 与 adapter starter，迁移 Magic-API、BOM 和样例依赖。
3. 删除四个旧模块目录和所有坐标、旧配置绑定、兼容桥接代码；在构建中添加禁止旧 artifact/前缀/导入的断言。
4. 发布迁移文档：应用显式添加 adapter starter；把旧 `fw-plus.ai.langchain4j.*` 配置改为中立 key；多 adapter 时指定 framework。
5. 先运行 Core reactor 测试，再运行 AI Boot 自动配置与 Magic-API 目标测试；在新版本不可用时回退到包含旧模块的上一发行版本，而不是混装新旧 artifact。

## Open Questions

- Spring AI、Spring AI Alibaba 与 AgentScope 2 Java adapter 的 artifact 命名、BOM 版本和 provider 能力矩阵在 Java 17 / Boot 3 change 中确定。
- AskData 直接使用 LangChain4j 的内部路径和对旧 artifact 的依赖迁移由独立 change 决定；本次不修改 AskData。
