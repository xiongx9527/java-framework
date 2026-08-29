## Context

见 `proposal.md`。中立 Store 层已经提供 Definition、Factory、Adapter、Registry 和统一异常；LangChain4j Store 层仍复制了一整套 legacy 类型，并在 `DefaultEmbeddingStoreFactory` 内执行 neutral/legacy 双向转换。AutoConfigure 的 `adapter.store` 同时包含 23 个厂商实现类、一个空默认映射的通用 Adapter 和一个二次转发基类。MagicAPI 与 Tinyflow 已经主要依赖中立 `AiEmbeddingStore*`，因此 legacy 链可以完整删除而无需保留兼容壳。

约束：Java 8、Spring Boot 2.7.18、LangChain4j 0.35.0；内置 Provider SDK 为可选依赖；现有 `fw-plus.ai.embedding-store` 配置、23 个具名 Bean 覆盖点和 provider alias 必须保持。

## Goals / Non-Goals

**Goals:**

- 只保留一套 Store Definition、Factory、Registry 和异常体系。
- 让 framework 与 provider 两级 Adapter 选择都确定性地处理零/一/多匹配。
- 将 `adapter.store` 从 27 个主类收敛为一个自动配置、一个声明式 Spec、一个通用反射 Adapter 和一个 Support。
- 用同一反射执行器覆盖所有现有厂商，包括 Tablestore 条件构造器和 Oracle 多参数重载调用。
- 保持中立 Store CRUD/search 行为、Provider key/alias、配置属性和具名 Bean 覆盖能力。
- 删除职责错误、重复或仅为兼容存在的类型，不保留 deprecated 转发层。

**Non-Goals:**

- 不修改 Model Definition、Factory、Registry、Provider Adapter 或自动配置。
- 不升级 LangChain4j、Spring Boot 或 Provider SDK 版本。
- 不改变 MagicAPI `vector-datasource` 格式、Tinyflow Vector Interface 或外部向量存储数据 schema。
- 不通过 classpath 扫描自动发现未知 Provider；内置支持仍显式声明。

## Decisions

### 1. 中立 `AiEmbeddingStore*` 是唯一外部 Store 体系

删除 Core LangChain4j Adapter 模块中的：

- `EmbeddingStoreDefinition`
- `EmbeddingStoreProperties`
- `EmbeddingStoreFactory`
- `EmbeddingStoreRegistry`
- `DefaultEmbeddingStoreRegistry`
- `EmbeddingStoreException`
- `EmbeddingStoreProviderAdapter`
- `DefaultEmbeddingStoreFactory`

对应职责由 `AiEmbeddingStoreDefinition`、`AiEmbeddingStoreFactory`、`AiEmbeddingStoreRegistry`、`AiEmbeddingStoreException` 和新的 LangChain4j Adapter 实现承担。删除 `toNeutral`、`toLegacy`、failure-capture 和 legacy Registry 包装逻辑。

Core `adapter.store` 最终保留：

```text
DefaultLangChain4jEmbeddingStoreAdapter
LangChain4jEmbeddingStoreProviderAdapter
LangChain4jAiEmbeddingStore
```

`LangChain4jAiEmbeddingStore` 继续作为 package-private 中立 facade，内部保存原生 `EmbeddingStore<TextSegment>`。

### 2. 新 Provider seam 直接消费中立 Definition

新公开 Interface：

```java
public interface LangChain4jEmbeddingStoreProviderAdapter {
    boolean supports(AiEmbeddingStoreDefinition definition);
    EmbeddingStore<TextSegment> create(AiEmbeddingStoreDefinition definition);
}
```

`supports` 接收完整 Definition，使自定义 Adapter 可以基于 provider、type、properties 和 SDK 可用性判断，而不是只接收字符串。`create` 返回原生 LangChain4j Store，由 framework Adapter 包装成中立 Store。

### 3. framework Adapter 严格选择 Provider Adapter

`DefaultLangChain4jEmbeddingStoreAdapter` 实现 `AiEmbeddingStoreAdapter`：

- `getFramework()` 返回 `langchain4j`；
- 只支持 `VECTOR` 类型和匹配 framework 的 Definition；
- 对 Provider Adapter 去除相同对象重复项；
- 收集所有 `supports(definition)` 为 true 的候选；
- 一个候选时创建；零个或多个候选时抛出带完整 Store 上下文的中立异常；
- `supports` 中发现多个候选也 fail-fast，不把冲突隐藏成 false；
- Provider 的支持检查或创建异常统一由中立 Factory 再包装，但保留根因。

自动配置向构造器注入 `Map<String, LangChain4jEmbeddingStoreProviderAdapter>`，使冲突诊断能够报告 Bean 名，而不依赖相同实现类的类名。

### 4. AutoConfigure 包只保留四个主类

最终结构：

```text
LangChain4jEmbeddingStoreAdapterAutoConfiguration
LangChain4jEmbeddingStoreProviderSpec
ReflectiveLangChain4jEmbeddingStoreProviderAdapter
ProviderAdapterSupport
```

删除 23 个厂商类、当前 `LangChain4jEmbeddingStoreProviderAdapter` 空通用实现和旧 `ReflectiveEmbeddingStoreProviderAdapter` 二次转发基类，共 25 个旧主类。新的反射 Adapter 是 package-private `final` Implementation，Core 中的新 Interface 是唯一 public provider seam。

自动配置保留 23 个原 Bean 方法名及 `@ConditionalOnMissingBean(name=...)`，每个方法创建：

```java
new ReflectiveLangChain4jEmbeddingStoreProviderAdapter(spec, applicationContext)
```

最终 `langChain4jAiEmbeddingStoreAdapter` Bean 构造 `DefaultLangChain4jEmbeddingStoreAdapter`。

### 5. 不可变 `ProviderSpec` 表达厂商规则

`LangChain4jEmbeddingStoreProviderSpec` 为 package-private 不可变值对象，构造时 fail-fast 校验：

- 至少一个规范化 provider alias；
- Store 类名和全部必要依赖类名非空；
- 创建计划非空且条件不重叠；
- 属性目标、fallback、Bean 引用和调用参数定义无冲突；
- 必填组和调用计划引用的属性存在有效来源。

Spec 支持以下声明元素：

- provider aliases 与 required class names；
- `require(...)`、`requireAny(...)`、条件必填规则；
- source property、constant、Definition key/provider/type、fallback chain；
- target setter 重命名，例如 `uri -> serverUrl`、`username -> userName`；
- Bean 引用及目标类型；
- 数字、布尔、枚举、Duration、Collection 和普通对象转换；
- builder、默认构造器、显式构造器参数计划；
- 条件创建计划；
- 单参数或多参数方法调用；
- 可选调用与显式值优先语义。

Spec 不公开给用户，也不是第二套扩展 seam。用户扩展只实现 Core 的 Provider Adapter Interface。

### 6. 通用反射 Adapter 是深模块

`ReflectiveLangChain4jEmbeddingStoreProviderAdapter` 负责：

1. provider alias 与 required class 可用性检查，使用 `Class.forName(name, false, loader)`，不初始化类；
2. 校验 required/required-any/conditional 规则；
3. 选择且仅选择一个创建计划；
4. 解析属性、fallback、Bean 引用和常量；
5. 确定性解析 builder setter、构造器与方法重载；
6. 转换 Java 8 支持的基础类型、枚举、Duration、Collection 与 Bean；
7. 调用 `build()` 或构造器并校验结果类型；
8. 用中立异常报告 provider、key、失败规则和根因。

重载解析必须拒绝多个同等匹配方法/构造器，不按反射返回顺序选择。

### 7. 复杂 Provider 也使用 Spec，不保留专用类

- Milvus：`host|uri` 任一必填、`collectionName/dimension` 必填、`token <- apiKey`、枚举和布尔 setter。
- Qdrant：必填 host/port/collectionName、`useTls <- utls`。
- Elasticsearch：`uri -> serverUrl`、`username -> userName`。
- Azure AI Search：`dimensions <- dimension`、三个可选 Bean 引用。
- Azure Cosmos NoSQL：四个 `*Bean -> builder property` Bean alias。
- Neo4j：dimension 必填；driver 缺失时 uri/username/password 条件必填。
- Redis：两个 provider alias、metadataKeys 来源。
- Tablestore：根据 tableName 与 metadata schema 选择唯一构造器参数计划。
- Oracle：DataSource Bean、embeddingTable 的单/双参数重载和 CreateOption 枚举调用。
- Weaviate：显式长构造器参数计划。

任何规则若现有通用机制无法正确表达，应扩展 Spec/执行器，而不是恢复厂商专用公开类。

### 8. 可选 SDK Bean 始终可注册，支持能力按 classpath 判断

参照 `adapter.model`：23 个具名 Provider Bean 始终可由自动配置声明；对应 SDK 缺失时 `supports` 返回 false，不触发类初始化，也不阻止应用启动。只保留顶层 Adapter 模块存在条件，不再为直接链接类设置逐 Bean `@ConditionalOnClass`。

这会改变“缺少 SDK 时某些 Provider Bean 不存在”的内部观测，但外部契约改为“Bean 可作为覆盖点存在、Provider 不可选择”。

### 9. 删除绕过中立链的上游 Store 自动配置

删除直接导入上游原生 Store 自动配置的 Elasticsearch、Redis、Milvus 三个顶层配置，它们绕过中立 Definition/Factory/Registry。Azure AI Search 的自定义 AutoConfig 同时提供 Retriever 与原生 Store：保留并重命名为 ContentRetriever-only 顶层配置，删除其原生 Store Bean 和 `embeddingStore` 配置字段。

AutoConfigure POM 删除三个已无调用者的 wrapper 模块依赖；Elasticsearch、Milvus、Qdrant、Redis、Weaviate 与 Milvus SDK 仅作为内置规则执行测试的 test scope，Azure SDK 保持 compile optional 以支持 Retriever。

### 10. 同步所有调用方和文档

MagicAPI 与 Tinyflow 保持只依赖 `AiEmbeddingStoreFactory`、`AiEmbeddingStoreRegistry` 和 `AiEmbeddingStore`。删除测试中的 legacy factory/definition 构造，改用新的 framework/provider Adapter。修正仍展示 `fw-plus.ai.langchain4j.embedding-store` 的 README 示例为 `fw-plus.ai.embedding-store`，并将 dimension/metadataKeys 放入中立 properties 结构。

### 11. 测试 Interface 和规则，不测试删除的类

- Core：framework Adapter 零/一/多候选、去重、异常传播、原生 Store 包装与中立 CRUD/search。
- AutoConfigure：23 个 Bean 名、全部 provider alias、SDK 缺失、具名覆盖、最终 Adapter 单例。
- ProviderSpec：不变量与冲突检测。
- 反射 Adapter：builder、constructor、重载歧义、Bean 引用、fallback、类型转换和全部复杂 Provider 规则。
- 集成：Spring 配置、Magic 动态资源、Tinyflow、完整 Reactor。

删除原 `DefaultEmbeddingStoreFactoryTest`、`DefaultEmbeddingStoreRegistryTest`、`EmbeddingStorePropertiesTest`；行为迁移到新测试，不保留 legacy 测试壳。

## Risks / Trade-offs

- [删除 public legacy FQCN 导致外部源码不兼容] → 明确 BREAKING，不提供兼容层；README 给出中立类型迁移映射。
- [通用规则执行器过度抽象] → Spec 只覆盖当前已验证需求；以 Tablestore/Oracle 为复杂度上界，并用 Interface 级测试锁定每类机制。
- [反射重载选择不稳定] → 评分后必须唯一匹配；同分多候选直接失败并列出方法/构造器。
- [可选 SDK 缺失时类加载失败] → AutoConfigure 不直接 import 厂商类型，Spec 只保存类名并使用不初始化的存在检查。
- [配置属性映射回归] → 为每个现有 provider 建立参数化契约测试，并保留 provider alias 与 Bean 名。
- [大范围删除遗漏调用方] → 全仓搜索 legacy FQCN、编译 Core/AI Reactor，并验证源码目录中不再存在被删除类型。

## Migration Plan

1. 建立 legacy 类型存在、逐厂商类数量和 Provider first-match 的 RED 证据。
2. 在 Core 新建 Provider Adapter seam 和 `DefaultLangChain4jEmbeddingStoreAdapter`，迁移行为测试。
3. 删除 8 个 legacy 类型及其测试，消除所有 neutral/legacy 转换。
4. 在 AutoConfigure 建立 Spec、通用反射 Adapter 和 Support 的完整规则执行能力。
5. 将 23 个厂商行为逐项迁入 Spec Bean，删除 25 个旧厂商/通用/转发实现类。
6. 迁移 Spring、MagicAPI、Tinyflow、测试和文档引用。
7. 运行结构 GREEN、参数化 Provider 测试、Core/AI Reactor 验证和两路独立审查。
8. 分别创建实现提交与归档提交；回滚时整体回退实现提交，不支持 legacy 与新链并存。
