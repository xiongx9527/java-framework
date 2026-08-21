## ADDED Requirements

### Requirement: 中立 starter 与公共运行时保持具体框架零痕迹
系统 SHALL 使 `fw-plus-ai-model`、`fw-plus-ai-store-embedding`、其所有中立 starter、公共自动配置和 Magic-API model/vector 实现只依赖中立 API 与 registry。

#### Scenario: 中立 artifact 没有具体框架依赖或公开类型
- **WHEN** 检查中立 artifact 的 POM、编译依赖、公开签名和自动配置
- **THEN** 它们不得包含 LangChain4j、Spring AI、Spring AI Alibaba 或 AgentScope 类型、依赖、包引用或配置前缀。
- **AND** 它们 SHALL 保持 Java 8 兼容。

#### Scenario: 应用显式安装 LangChain4j adapter
- **WHEN** 应用需要 LangChain4j 实现
- **THEN** 它 SHALL 显式引入 `fw-plus-ai-langchain4j-adapter-spring-boot-starter` 或等价 adapter artifact。
- **AND** 中立 starter 本身不得隐式传递该 adapter。

#### Scenario: Magic-API 使用中立 registry
- **WHEN** Magic-API 创建或访问 model/vector 动态资源
- **THEN** 它 SHALL 仅通过中立 registry 与门面执行操作。
- **AND** 它不得创建或暴露框架原生 model、document 或 embedding store 类型。
