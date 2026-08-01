## MODIFIED Requirements

### Requirement: 提供 Spring Boot model starter 和自动配置
系统 SHALL 提供 `fw-plus-ai-langchain4j-model-spring-boot-starter`，并在 AI Boot 自动配置层把应用配置转换为模型 registry。

#### Scenario: starter 注册到 AI Boot 聚合
- **WHEN** 检查 `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters` 聚合 POM
- **THEN** 它列出 `fw-plus-ai-langchain4j-model-spring-boot-starter` 子模块。

#### Scenario: AI Boot BOM 管理 starter artifact
- **WHEN** 检查 `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies` 的 dependencyManagement
- **THEN** 它包含 `com.xiongx9527.fw.plus.ai:fw-plus-ai-langchain4j-model-spring-boot-starter`。
- **AND** 该依赖使用 `${project.version}` 作为版本。

#### Scenario: starter 只传递抽象和自动配置
- **WHEN** 检查 `fw-plus-ai-langchain4j-model-spring-boot-starter/pom.xml`
- **THEN** 它依赖 `fw-plus-ai-boot-starter` 和 `fw-plus-ai-langchain4j-model`。
- **AND** 它不默认传递所有具体 provider SDK。

#### Scenario: 自动配置创建模型 registry
- **WHEN** 应用配置了启用状态的 LangChain4j model 定义
- **THEN** 自动配置使用模型工厂创建模型并注册到 `LangChain4jModelRegistry`。
- **AND** 默认模型 key 配置存在时，registry 的默认模型指向对应模型。

#### Scenario: 自动配置校验默认模型 key
- **WHEN** 应用配置了不存在的默认模型 key
- **THEN** Spring Boot 启动阶段抛出明确异常。
- **AND** 异常信息包含无效默认模型 key。

#### Scenario: 同一配置节点注册多个模型名
- **WHEN** 应用在同一个模型配置节点下把 `model-name` 配置为多个模型名
- **THEN** 自动配置 MUST 为每个模型名创建独立模型实例。
- **AND** 每个实例 MUST 使用 `<配置 key>.<modelName>` 作为 registry key。
- **AND** 自动配置 MUST 把 `<配置 key>` 注册为该节点默认模型名的别名。

#### Scenario: 同一配置节点指定默认模型名
- **WHEN** 应用在多个 `model-name` 的配置节点下配置 `default-model-name`
- **THEN** `<配置 key>` 别名 MUST 指向 `default-model-name` 对应的模型实例。
- **AND** 未配置 `default-model-name` 时，`<配置 key>` 别名 MUST 指向 `model-name` 列表中的第一个模型名。

#### Scenario: 单个模型名覆盖共享配置
- **WHEN** 应用在模型配置节点下同时配置共享 `properties` 和 `model-configs.<modelName>.properties`
- **THEN** 自动配置 MUST 先应用共享 `properties`。
- **AND** 自动配置 MUST 再应用该 `modelName` 自己的 `properties`。
- **AND** 单个模型名自己的同名 `properties` MUST 覆盖共享 `properties`。

#### Scenario: 单个模型名覆盖通用连接配置
- **WHEN** 应用在 `model-configs.<modelName>` 下配置 `provider`、`model-type`、`base-url` 或 `api-key`
- **THEN** 自动配置 MUST 对该模型名使用覆盖后的通用配置。
- **AND** 未覆盖的字段 MUST 继续继承父级模型配置节点。

#### Scenario: 单模型配置保持兼容
- **WHEN** 应用继续使用 `models.<key>.provider` 加单个 `model-name` 的旧配置方式
- **THEN** 自动配置 MUST 继续把该模型注册为 `<key>`。
- **AND** 该配置 MUST 不要求额外配置 `model-configs`。

### Requirement: 替换旧 Magic LangChain4j 插件实现并保持非 Magic 行为兼容
系统 SHALL 在新增模型模块和 Magic model 插件能力时删除或替换旧 Magic-API LangChain4j 插件实现，同时保持已有 LangChain4j starter 模块和 `fw-plus-ai-langchain4j-store-embeding` 模块行为兼容。

#### Scenario: 向量存储模块行为不变
- **WHEN** 构建并运行现有 `fw-plus-ai-langchain4j-store-embeding` 相关测试
- **THEN** 现有 EmbeddingStoreFactory、EmbeddingStoreRegistry 和 Magic vector 插件测试继续通过。

#### Scenario: 旧 Magic LangChain4j 实现被删除或替换
- **WHEN** 检查 AI Boot 自动配置和 Magic 插件源码
- **THEN** 旧 `com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j` 下的重复模型工厂、动态数据源、资源存储、控制器和脚本模块实现已被删除或替换。
- **AND** Magic 插件自动配置导入新的 model 插件配置，而不是继续导入旧 LangChain4j 插件配置。

#### Scenario: 旧 Magic 资源和样例迁移到 model 插件
- **WHEN** 检查 Magic 前端插件资源、starter POM 和示例项目依赖
- **THEN** 它们使用新的 model 插件 artifact、`model-datasource` 资源类型和 model 脚本模块。
- **AND** 它们不再要求旧 `fw-plus-ai-magicapi-plugin-langchain4j` 作为运行入口。

#### Scenario: 分层 Maven 构建通过
- **WHEN** 按依赖顺序使用 Maven Wrapper 构建 core 新模型模块和 AI Boot 相关模块
- **THEN** 编译和相关测试均通过。

#### Scenario: 文档说明多模型名和单模型名配置
- **WHEN** 检查 README 或迁移文档
- **THEN** 它说明同一个模型 key 下多个模型名通过 `model-name` 列表配置。
- **AND** 它说明不要在该模型 key 下再嵌套一层 `models` 表达多个模型。
- **AND** 它说明每个模型名自己的参数通过 `model-configs.<modelName>.properties` 配置。

#### Scenario: 文档说明 LangChain4j 版本差异和多模态边界
- **WHEN** 检查 README 或迁移文档
- **THEN** 它说明当前实现以 LangChain4j 0.35.0 编译，Chat 类型使用 `ChatLanguageModel`，Streaming Chat 类型使用 `StreamingChatLanguageModel`。
- **AND** 它说明高版本 LangChain4j 使用 `ChatModel` / `StreamingChatModel`，这些类名不得在 0.35.0 编译代码中使用。
- **AND** 它说明 `ImageContent`、`AudioContent`、`VideoContent`、`PdfFileContent` 等是 Chat 多模态输入内容，不是 LangChain4j 0.35.0 或当前 1.x 的独立 `AudioModel` / `VideoModel` / `PdfModel` 统一接口。
- **AND** 它说明 `AUDIO`、`VIDEO`、`PDF` 是 fw-plus 为 Magic 资源和业务适配器保留的扩展 modelType，使用时需要自定义 `LangChain4jModelProviderAdapter` 接入厂商 SDK 或业务封装。
