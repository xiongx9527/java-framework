## Why

上一轮对 `add-askdata-spring-boot-starter`、`fix-askdata-starter-layering`、`rework-ai-askdata-core-starter` 和治理权限桥的复核表明：现有测试虽然通过，但 Core SPI、能力注册表、多问题计划、模型路由、SQL 插件、Agent 步骤、监控事件和上下文压缩没有完整进入默认执行链。当前自动配置还保留了与 Core 同名但不兼容的模型、Embedding、元数据和会话接口，导致业务实现 Core SPI 后不能可靠接管运行流程。

`fw-plus-ai-model` 已具备模型定义、Adapter 工厂和注册表，但 `AiModel` 只描述模型身份，缺少可供不同 AI 框架统一实现的类型化执行契约。`fw-plus-ai-store-embedding` 的存储抽象总体可复用，但查询过滤和 Adapter 能力诊断不足。需要在不破坏 Java 8 / Spring Boot 2.7.18 基线的前提下增强这两个中立模块，并让 AskData 只消费公开 Core 契约。

当前分支只实现 Java 8 兼容的 LangChain4j Adapter。Spring AI、Spring AI Alibaba 和 AgentScope 的正式 Adapter 由 Java 17 / Spring Boot 3 分支实现；本 change 只定义其必须遵守的中立契约、错误映射、远程运行边界和验证要求，不向 Java 8 Starter 传递这些依赖。

## What Changes

- 增强 `fw-plus-ai-model` 的厂商无关类型化聊天和 Embedding 请求/结果契约，保留 `AiModelDefinition`、`AiModelAdapter`、`AiModelRegistry` 的模型发现与工厂职责。
- 增强 `fw-plus-ai-store-embedding` 的中立查询过滤、能力诊断和参数校验，使 AskData 检索不丢失项目授权过滤条件。
- 保留 AskData Core 的业务级模型和 Embedding SPI，并通过桥接器消费中立模型注册表；自动配置内部同名接口进入兼容迁移路径，不再作为默认执行链的事实契约。
- 在 Java 8 分支实现 LangChain4j 对中立模型、Embedding 和 AskData Core SPI 的 Adapter，并保持 Adapter 由业务显式引入；AskData Starter 不默认绑定 Ollama 或任何 AI 框架。
- 将 Core 能力注册表、元数据、会话、检查点、Agent 状态和事件 SPI 接入默认自动配置，业务 Bean 优先，未知 key、重复实现和版本不兼容在启动期失败。
- 实现带稳定子问题 ID、依赖关系、受限并发、超时、取消、部分失败和结构化聚合结果的多问题执行器。
- 将 Core 模型路由和基础设施故障切换接入默认模型调用链，记录每次尝试、故障分类、路由原因和最终模型 key。
- 扩展 SQL 插件链以保留插件新增的具名参数，支持明确的组装、改写、执行准备、结果映射和审计阶段，并在插件后重新执行完整权限与 SQL 安全校验。
- 使业务 Agent 能按稳定步骤 key 替换默认选表、SQL 生成、SQL 校验、SQL 执行或结果解释步骤，而不是只能追加旁路输出。
- 补齐多问题、模型、Agent、SQL、检查点、压缩、长期记忆、取消和失败事件；监控存储或导出失败时回退到本地脱敏环形日志，且不得改变对话结果。
- 将默认上下文压缩改为不依赖具体 AI 框架的结构化压缩器，并允许业务选择 LangChain4j 语义摘要实现；压缩失败时保留安全字段并使用确定性降级结果。
- 更新文档，明确 Spring AI、Spring AI Alibaba、AgentScope Adapter 由 Java 17 / Spring Boot 3 分支实现，并复用当前 Core 契约或 Remote Runtime 协议。

## Capabilities

### Modified Capabilities

- `askdata-core-contracts`: 统一模型、Embedding、能力注册、SQL 插件和业务替换 SPI 的实际消费边界。
- `askdata-query-engine`: 修复模型路由、多问题、Agent 步骤、SQL 管线、上下文压缩和 Starter Adapter 选择行为。
- `askdata-agent-observability-contracts`: 补齐生命周期事件并隔离监控存储和导出失败。

## Impact

- 影响 Core 层 `fw-plus-ai-model`、`fw-plus-ai-store-embedding`、`fw-plus-ai-askdata` 和 `fw-plus-ai-langchain4j-adapter`。
- 影响 AI 层 AskData 自动配置、LangChain4j Adapter 自动配置、AskData Starter 文档和相关 BOM/POM。
- 保持 Java 8、Spring Boot 2.7.18、现有单问题 API 和 Starter 纯依赖聚合分层。
- 不实现 Spring AI、Spring AI Alibaba、AgentScope 的 Java 8 内嵌 Adapter；不修改 `magicapi-dynamic-entity-plugin`。
