## MODIFIED Requirements

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
