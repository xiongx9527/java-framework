## ADDED Requirements

### Requirement: 中立抽象、默认 adapter 与 Java 17 后续实施文档必须具备可验证证据
系统 SHALL 为中立模型/store 核心、LangChain4j 默认 adapter 提供不依赖真实 API key 或远程向量库的自动化测试，并为 Spring AI、Spring AI Alibaba、AgentScope 2 Java 提供经脚本校验的后续实施文档。

#### Scenario: 核心契约测试覆盖注册与失败路径
- **WHEN** 执行中立核心模块测试
- **THEN** 测试 SHALL 覆盖按 key/default 获取、重复 key、无效默认 key、adapter 不支持能力和异常上下文。

#### Scenario: LangChain4j 默认 adapter 验证完整声明能力
- **WHEN** 执行 LangChain4j adapter 测试
- **THEN** 测试 SHALL 使用 SDK mock 或本地 fake 验证其声明支持的模型和向量能力。
- **AND** 测试 SHALL 验证未声明能力被明确拒绝。

#### Scenario: Java 17 后续实施文档验证发布边界
- **WHEN** 执行 Spring AI、Spring AI Alibaba 或 AgentScope 2 Java 的文档校验
- **THEN** 校验 SHALL 验证 Java 17 / Spring Boot 3.x 前置条件、未来验证步骤、Java 8 隔离和未实现 adapter 的明确状态。
- **AND** 文档 SHALL 不把真实 SDK model/store 契约或 Java 17 artifact 伪装为当前已交付。

### Requirement: 自动配置和 Magic-API 兼容性必须回归验证
系统 SHALL 对配置兼容层、starter 条件装配和 Magic-API model/vector 插件建立集成测试。

#### Scenario: 旧配置和中立配置优先级受测试保护
- **WHEN** 分别加载旧 LangChain4j 配置、仅中立配置和同 key 混合配置
- **THEN** 测试 SHALL 验证对应 registry 内容和中立配置优先级。

#### Scenario: Magic-API 使用中立 registry
- **WHEN** Magic-API 注册模型或向量动态资源并通过 key/default 调用插件
- **THEN** 测试 SHALL 验证插件只依赖中立 registry/factory。
- **AND** 错误响应和日志 SHALL 不包含密钥。

#### Scenario: 分层 Maven 构建通过
- **WHEN** 按 Core 后 AI Boot 的依赖顺序执行 Maven Wrapper 测试
- **THEN** 新增模块、LangChain4j 兼容路径和相关自动配置测试 SHALL 编译并通过。
