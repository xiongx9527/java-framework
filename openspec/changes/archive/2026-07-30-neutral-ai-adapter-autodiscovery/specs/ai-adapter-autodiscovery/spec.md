## ADDED Requirements

### Requirement: 中立 AI adapter 的自动发现与确定性选择
系统 SHALL 从应用 classpath 与 Spring 容器发现已安装的 `AiModelAdapter` 和 `AiEmbeddingStoreAdapter`，并对每个模型或向量 store 定义独立选择 adapter。

#### Scenario: 显式 framework 匹配已安装 adapter
- **WHEN** 定义给出 `framework` 且同名 adapter 支持其 provider 与 type
- **THEN** 系统 SHALL 使用该 adapter 创建中立门面。
- **AND** 注册表元数据 SHALL 保留显式 framework。

#### Scenario: 显式 framework 没有匹配 adapter
- **WHEN** 定义给出 `framework` 但 adapter 不存在、SDK 缺失或不支持 provider/type
- **THEN** 应用启动 SHALL 失败。
- **AND** 错误 SHALL 包含 key、framework、provider、type 和已发现 framework，且不得包含凭证。

#### Scenario: 缺省 framework 只有一个候选
- **WHEN** 定义未给出 `framework` 且仅一个已发现 adapter 支持其 provider 与 type
- **THEN** 系统 SHALL 自动选择该 adapter。
- **AND** LangChain4j 是仅安装 LangChain4j adapter 时的默认实现。

#### Scenario: 缺省 framework 没有或存在多个候选
- **WHEN** 定义未给出 `framework` 且候选数量为零或大于一
- **THEN** 应用启动 SHALL 失败而不得按排序或 Bean 名称选择。
- **AND** 错误 SHALL 指示用户安装 adapter 或显式配置 framework。
