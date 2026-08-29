# langchain4j-embedding-store-adapter Specification

## Purpose
定义 LangChain4j Embedding Store 通过中立定义、确定性 Provider 选择和可扩展厂商创建规则对外提供一致向量存储行为的契约。

## Requirements

### Requirement: 使用中立 Store 定义
系统 SHALL 仅使用中立 Embedding Store Definition 作为 LangChain4j Store 创建输入，并 SHALL 通过中立 Store Interface 和 Registry 暴露创建结果。

#### Scenario: 由 Spring Boot 配置创建 Store
- **WHEN** 用户在 `fw-plus.ai.embedding-store.stores.<key>` 下配置启用的 LangChain4j Store
- **THEN** 系统使用该中立定义创建并以相同 key 注册 Store

#### Scenario: 动态资源创建 Store
- **WHEN** MagicAPI 动态向量资源提交等价的中立 Store 定义
- **THEN** 系统通过同一 Store Factory 与 Adapter 链创建 Store

### Requirement: 确定性选择 Framework Adapter
系统 MUST 按 framework 和支持能力选择且仅选择一个 Store Adapter，不得依赖 Adapter 集合的迭代顺序。

#### Scenario: 明确指定 LangChain4j framework
- **WHEN** 定义的 framework 为 `langchain4j` 且恰好一个 LangChain4j Adapter 支持该定义
- **THEN** 系统使用该 Adapter 创建 Store

#### Scenario: 未指定 framework 且只有一个候选
- **WHEN** 定义未指定 framework 且所有已安装 Adapter 中恰好一个支持该定义
- **THEN** 系统使用唯一候选创建 Store

#### Scenario: 多个 Framework Adapter 支持定义
- **WHEN** 两个或更多 Framework Adapter 支持同一定义
- **THEN** 系统拒绝创建并报告所有候选 framework

### Requirement: 确定性选择 Provider Adapter
LangChain4j Store Adapter MUST 对同一 Provider Definition 执行零个、一个和多个匹配的严格判定，不得选择第一个匹配项。

#### Scenario: 一个 Provider Adapter 匹配
- **WHEN** 恰好一个 Provider Adapter 支持定义中的 provider
- **THEN** 系统使用该 Adapter 创建原生 Store

#### Scenario: 没有 Provider Adapter 匹配
- **WHEN** 没有 Provider Adapter 支持定义中的 provider 或所需 SDK 不可用
- **THEN** 系统拒绝创建并在错误中包含 key、framework、provider 和 type 上下文

#### Scenario: 多个 Provider Adapter 匹配
- **WHEN** 两个或更多 Provider Adapter 支持同一 provider
- **THEN** 系统拒绝创建并报告 Provider Adapter 冲突，不得按注册顺序静默选择

### Requirement: 保持内置 Provider 兼容性
系统 SHALL 支持重构前已有的内置 Provider key、provider alias、配置属性语义和具名 Bean 覆盖点。

#### Scenario: In-memory provider alias
- **WHEN** provider 为 `in-memory`、`inmemory` 或 `memory`
- **THEN** 系统将其映射到同一种 In-memory Store 创建规则

#### Scenario: Redis provider alias
- **WHEN** provider 为 `redisstack` 或 `redis`
- **THEN** 系统将其映射到同一种 Redis Store 创建规则

#### Scenario: PgVector provider alias
- **WHEN** provider 为 `pgvector`、`postgres` 或 `postgresql`
- **THEN** 系统将其映射到同一种 PgVector Store 创建规则

#### Scenario: MongoDB provider alias
- **WHEN** provider 为 `mongodb-atlas` 或 `mongodb`
- **THEN** 系统将其映射到同一种 MongoDB Store 创建规则

#### Scenario: 自定义具名 Provider Bean
- **WHEN** 用户提供与某个内置厂商覆盖点同名的 Provider Adapter Bean
- **THEN** 自动配置 SHALL 回退该内置 Bean，并由用户 Bean 参与确定性 Provider 选择

### Requirement: 声明式执行厂商创建规则
系统 MUST 在不要求每个内置厂商拥有独立公开实现类的前提下，完整执行厂商所需的校验、属性映射和原生对象创建规则。

#### Scenario: Builder 型 Store
- **WHEN** Provider 规则声明使用 builder 且输入满足必填条件
- **THEN** 系统转换属性类型、调用匹配 setter 并通过 `build` 创建 Store

#### Scenario: Constructor 型 Store
- **WHEN** Provider 规则声明使用构造器及条件参数计划
- **THEN** 系统选择唯一匹配计划、解析参数并调用对应构造器创建 Store

#### Scenario: Spring Bean 引用
- **WHEN** Provider 属性声明引用 Spring Bean 名称
- **THEN** 系统从 ApplicationContext 解析类型兼容的 Bean 并传入原生创建调用

#### Scenario: 属性 fallback
- **WHEN** 目标属性缺失但规则声明的 fallback 属性存在
- **THEN** 系统使用 fallback 值且显式目标值始终优先

#### Scenario: 必填配置缺失
- **WHEN** 定义缺失厂商规则要求的属性或任一必填组全部为空
- **THEN** 系统拒绝创建并报告缺失属性及 Store 上下文

### Requirement: 可选 Provider SDK 隔离
内置 Provider 的可选 SDK 不可用时，系统 MUST 保持应用上下文可启动，并 MUST 仅使对应 Provider 不可选择。

#### Scenario: 可选 SDK 不在 classpath
- **WHEN** 某个内置 Provider 的原生 Store 类或必要依赖类不在 classpath
- **THEN** 该 Provider Adapter 不声明支持且其他 Provider 和最终 Store Adapter 仍可正常注册

#### Scenario: SDK 可用
- **WHEN** Provider 所需的原生类和必要依赖类均在 classpath
- **THEN** 该 Provider Adapter 参与该 provider 的确定性选择

### Requirement: 禁止原生 Store 自动配置旁路
系统 MUST 仅通过中立 Definition、Factory、Adapter 和 Registry 创建及暴露 Embedding Store，不得同时启用直接创建原生 LangChain4j Store Bean 的旁路。

#### Scenario: Provider SDK 自带 Spring 自动配置
- **WHEN** classpath 包含 Redis、Milvus、Elasticsearch 或其他 Provider 的原生 Spring 自动配置
- **THEN** fw-plus 不导入该原生 Store 自动配置，Store 仍只通过中立链创建

#### Scenario: Azure AI Search Retriever 与 Store 共用模块
- **WHEN** Azure AI Search 模块同时具备 ContentRetriever 与 Embedding Store 能力
- **THEN** 系统保留 ContentRetriever 自动配置但不创建原生 Embedding Store Bean

### Requirement: 保持中立 Store 运行行为
通过 LangChain4j 创建的 Store SHALL 继续实现中立 Store 的添加、批量添加、删除、批量删除和搜索行为，并 SHALL 将不受底层版本支持的操作转换为中立异常。

#### Scenario: 添加和搜索向量文档
- **WHEN** 调用方通过中立 Store 添加文档并使用向量搜索
- **THEN** 系统在 LangChain4j 原生 Store 上执行操作并返回中立文档、匹配项和得分

#### Scenario: 不受支持的过滤器
- **WHEN** 搜索请求包含当前 LangChain4j 版本不支持的过滤器
- **THEN** 系统拒绝请求并抛出包含 Store key 的中立 Store 异常

#### Scenario: 显式 id 携带不可保留内容
- **WHEN** 调用方使用显式 id 添加当前底层版本无法同时保留的内容或元数据
- **THEN** 系统拒绝写入而不是静默丢失内容或元数据
