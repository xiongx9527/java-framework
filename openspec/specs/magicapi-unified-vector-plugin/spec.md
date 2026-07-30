# magicapi-unified-vector-plugin Specification

## Purpose
TBD - created by archiving change unify-vector-magicapi-plugin. Update Purpose after archive.
## Requirements
### Requirement: 单一 Magic-API 向量插件
系统 SHALL 在 AI Magic-API 侧提供一个统一 vector 插件，用一个脚本模块聚合 Milvus、Qdrant、RedisStack、Weaviate 等向量存储能力。

#### Scenario: 只暴露统一 vector 插件
- **WHEN** 检查 AI Magic-API plugin starter 聚合
- **THEN** 它 SHALL 提供一个统一向量插件模块。
- **AND** Milvus、Qdrant、RedisStack、Weaviate 不 SHALL 作为推荐的一等独立向量插件继续暴露。

#### Scenario: 脚本侧通过 key 选择 store
- **WHEN** Magic-API 脚本使用统一 vector 模块访问 `vector['knowledge']` 或等价 key 选择方式
- **THEN** 插件 SHALL 返回 key 对应的向量存储操作对象。

#### Scenario: 脚本侧访问默认 store
- **WHEN** Magic-API 脚本未显式指定向量 store key
- **THEN** 插件 SHALL 使用 starter registry 中配置的默认向量存储。

### Requirement: 统一插件复用 Core 多向量 registry
系统 SHALL 让统一 Magic-API vector 插件委托中立 `AiEmbeddingStoreRegistry` 和中立 store 门面获取实例，而不得直接依赖 LangChain4j `EmbeddingStore<TextSegment>`、旧 factory/registry 或任何具体 AI framework 类型。

#### Scenario: 插件不重复构造 provider 连接
- **WHEN** 插件需要访问已注册向量 store
- **THEN** 插件 SHALL 通过中立 registry 获取中立 store 门面。
- **AND** 插件自身不得复制 provider 或框架原生连接构建逻辑。

#### Scenario: 脚本资源触发 adapter 自动发现
- **WHEN** Magic-API 资源定义给出 framework 或未给出 framework
- **THEN** 系统 SHALL 使用与普通 starter 相同的显式匹配或唯一候选选择规则。
- **AND** 零候选或多候选 SHALL 返回不含凭证的明确 Magic-API 错误。

#### Scenario: 旧脚本按 key 和默认 store 继续访问
- **WHEN** Magic-API 脚本使用 `vector['knowledge']` 或未指定 key 的默认访问方式
- **THEN** 插件 SHALL 使用相同 key 或默认 store 返回对应中立操作对象。
- **AND** 脚本侧不得获得框架原生对象。

#### Scenario: provider 配置异常
- **WHEN** 脚本访问的向量 store key 未注册、framework adapter 缺失或 provider 配置无效
- **THEN** 插件 SHALL 返回包含缺失 key、framework 或 provider 的明确 Magic-API 运行时错误。
- **AND** 错误不得暴露凭证。

### Requirement: 旧向量插件迁移策略
系统 SHALL 为现有 Milvus、Qdrant、RedisStack、Weaviate Magic-API 插件提供明确迁移策略，确保用户知道应迁移到统一 vector 插件。

#### Scenario: 旧插件入口被保留为兼容别名
- **WHEN** 实现选择保留旧插件入口
- **THEN** 旧入口 SHALL 委托统一 vector 插件或统一 registry。
- **AND** 旧入口 SHALL 避免继续维护独立 provider 构建逻辑。

#### Scenario: 旧插件入口被移除
- **WHEN** 实现选择移除旧插件入口
- **THEN** 聚合 POM 和 BOM SHALL 不再推荐独立 provider 插件。
- **AND** 迁移说明 SHALL 指向统一 vector 插件和按 key 访问方式。

