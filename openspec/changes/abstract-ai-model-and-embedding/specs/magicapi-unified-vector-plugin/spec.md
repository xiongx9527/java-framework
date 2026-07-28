## MODIFIED Requirements

### Requirement: 统一插件复用中立多向量 registry
系统 SHALL 让统一 Magic-API vector 插件委托框架无关的多向量 registry/factory 获取向量存储实例，而不直接依赖 LangChain4j `EmbeddingStore<TextSegment>` 或其 factory/registry。

#### Scenario: 插件不重复构造 provider 或框架连接
- **WHEN** 插件需要访问 Milvus、Qdrant、RedisStack、Weaviate 或其他已注册向量 store
- **THEN** 插件 SHALL 通过中立 registry/factory 获取中立 store 门面。
- **AND** 插件自身不得复制 provider 或框架原生连接构建逻辑。

#### Scenario: 旧脚本按 key 和默认 store 继续访问
- **WHEN** Magic-API 脚本使用 `vector['knowledge']` 或未指定 key 的默认访问方式
- **THEN** 插件 SHALL 使用相同 key 或默认 store 返回对应中立操作对象。
- **AND** 资源定义未指定 `framework` 时 SHALL 保持已解析默认 store 的行为。

#### Scenario: provider 配置异常
- **WHEN** 脚本访问的向量 store key 未注册、framework adapter 缺失或 provider 配置无效
- **THEN** 插件 SHALL 返回包含缺失 key、framework 或 provider 的明确 Magic-API 运行时错误。
- **AND** 错误不得暴露凭证。
