# LangChain4j 高版本兼容性规格

## 目的

fw-plus SHALL 在不破坏框架中立 AI API 的前提下，使用同一发布批次的 LangChain4j 1.18.0 稳定组件与 1.18.0-beta28 Community/实验性组件，并使模型、Embedding Store、Tinyflow、AskData 和 Spring Boot 自动配置能够在完整或裁剪类路径下安全运行。

## Requirements

### Requirement: 统一 LangChain4j 依赖基线

系统 SHALL 使用 LangChain4j `1.18.0` 管理稳定核心组件，并对 Community 或实验性 Provider 使用同批次 `1.18.0-beta28`。系统 SHALL 不解析任何 `dev.langchain4j:*:0.36.2` 依赖。

#### Scenario: 解析完整 AI 依赖树

- **WHEN** 构建 LangChain4j Core、Adapter、Boot AutoConfigure 和相关 Starter
- **THEN** 稳定 LangChain4j 组件版本为 `1.18.0`
- **AND** Community/实验性组件版本为 `1.18.0-beta28`
- **AND** 依赖树不包含 `0.36.2`
- **AND** 不包含旧 `langchain4j-dashscope`、`langchain4j-qianfan`、`langchain4j-redis` 坐标

### Requirement: 保持框架中立模型与向量存储边界

`fw-plus-ai-model` 与 `fw-plus-ai-store-embedding` SHALL 保持不依赖具体 AI 框架的公共契约。

#### Scenario: 检查公共依赖和公开签名

- **WHEN** 检查两个中立模块的 POM、编译依赖和公开类型
- **THEN** 不存在 `dev.langchain4j` 编译依赖
- **AND** 公开方法签名不暴露 LangChain4j 类型
- **AND** 中立工厂、注册表和异常行为保持现有语义

### Requirement: 使用 LangChain4j 现代聊天模型 API

生产代码 SHALL 使用 `ChatModel` 和 `StreamingChatModel`，不得继续引用已移除的 `ChatLanguageModel` 或 `StreamingChatLanguageModel`。

#### Scenario: Tinyflow 调用聊天模型

- **WHEN** Tinyflow 使用已注册的 LangChain4j 聊天模型执行请求
- **THEN** 请求通过 `ChatModel` API 发出
- **AND** 响应文本按现有 Tinyflow 契约返回
- **AND** 空响应和调用异常仍映射为受控 `TinyflowException`

#### Scenario: AskData 调用聊天模型

- **WHEN** AskData 从 LangChain4j registry 获取聊天模型
- **THEN** 返回并使用 `ChatModel`
- **AND** 既有模型 key、默认选择和失败语义保持不变

### Requirement: Adapter 自动配置安全加载

`LangChain4jAdapterAutoConfiguration` SHALL 在核心 adapter 与 LangChain4j Core 可用时导入模型和 Embedding Store adapter 配置，并在缺失可选 Provider 时安全回退。

#### Scenario: 完整类路径加载

- **WHEN** 核心 adapter、LangChain4j Core 和受支持 Provider 均存在
- **THEN** Spring 上下文启动成功
- **AND** 注册唯一的中立模型 adapter
- **AND** 注册唯一的中立 Embedding Store adapter
- **AND** 注册类路径中可用的 Provider adapter

#### Scenario: 可选 Provider 缺失

- **WHEN** 任一 Provider 实现类或其 SDK 不在类路径
- **THEN** Spring 上下文仍启动成功
- **AND** 仅缺失 Provider 对应的 bean 不注册
- **AND** 不抛出类加载异常

#### Scenario: 自动配置条件检查可选类型

- **WHEN** 自动配置声明 LangChain4j 或 Provider 可选类条件
- **THEN** 条件使用不会在配置类加载期强制解析缺失类型的形式
- **AND** 相关回归测试覆盖条件元数据和裁剪类路径启动

### Requirement: Community Provider 保持既有用户契约

DashScope、Qianfan、Zhipu AI 和 Redis SHALL 使用对应 `langchain4j-community-*` 坐标和 `dev.langchain4j.community.*` 包名，同时保持 fw-plus 对外 Provider key 和配置前缀。

#### Scenario: 创建 Qwen 模型

- **WHEN** 定义使用现有 Qwen Provider key 和配置
- **THEN** adapter 创建 Community DashScope 模型
- **AND** Chat、Streaming Chat、Embedding、Image 和仍被高版本支持的能力按既有类型映射

#### Scenario: 创建 Qianfan 或 Zhipu 模型

- **WHEN** 定义使用现有 Qianfan 或 Zhipu Provider key
- **THEN** adapter 使用对应 Community 模型类
- **AND** 不要求调用方修改既有 Provider key

#### Scenario: 创建 Redis Embedding Store

- **WHEN** Redis Provider 配置有效且 Community Redis 模块存在
- **THEN** adapter 创建 Community `RedisEmbeddingStore`
- **AND** 缺少 Community Redis 模块时仅 Redis Provider 不可用

### Requirement: 高版本兼容性可持续验证

系统 SHALL 提供自动化测试覆盖中立边界、现代聊天 API、Provider 类名、依赖树和裁剪类路径启动。

#### Scenario: 执行兼容性验证

- **WHEN** 在 JDK 17 上执行相关 Maven reactor 测试
- **THEN** model、store、adapter、Tinyflow、AskData 和 Boot AutoConfigure 相关测试通过
- **AND** 依赖树断言通过
- **AND** 不存在旧 Chat Language API 的生产代码引用
