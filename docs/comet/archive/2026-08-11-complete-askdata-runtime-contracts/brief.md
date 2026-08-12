# Outcome

补齐 AskData 默认运行链中尚未真正生效的 Core SPI、模型路由、多问题、SQL 插件、Agent 步骤、观测和上下文压缩契约。在当前 Java 8 / Spring Boot 2.7.18 分支完成 LangChain4j 首个正式 Adapter，同时保持 AskData 与通用模型、Embedding Store 对具体 AI 框架无依赖。

# Scope

- 增强 `fw-plus-ai-model` 的类型化 Chat/Embedding 请求、结果、用量、超时和错误契约。
- 增强 `fw-plus-ai-store-embedding` 的不可变过滤条件与过滤能力诊断，授权过滤必须 fail-closed。
- 实现 LangChain4j 对通用模型、Embedding 和 AskData Core SPI 的 Adapter；由业务显式选择框架与 Provider。
- 让默认 AskData Flow 直接消费公开 Core SPI 和唯一能力注册表，业务 Bean 优先，旧接口只作单向兼容桥。
- 接通治理授权、动态模型路由、多问题 DAG、阶段化 SQL 插件、Agent 步骤替换、检查点、观测事件和上下文压缩降级。
- 更新当前分支文档以及 Java 17 / Spring Boot 3 分支的 Spring AI、Spring AI Alibaba、AgentScope Adapter 实现边界。

# Non-goals

- 不在当前 Java 8 分支实现或引入 Spring AI、Spring AI Alibaba、AgentScope 运行时依赖。
- 不让公共 AskData Starter 默认引入 LangChain4j、Ollama、模型实例或凭据配置。
- 不删除仍有调用方的旧自动配置接口，只停止默认 Flow 把它们当作事实契约。
- 不修改 `magicapi-dynamic-entity-plugin`、TinyEngine 或其他 AskData 之外的业务模块。

# Acceptance examples

- A1：仅依赖 `fw-plus-ai-model` 的 Java 8 模块可调用类型化 Chat/批量 Embedding；请求和结果不可被外部修改，并返回实际模型 key、用量及中立错误语义。
- A2：向量检索携带项目、数据源或业务授权 filters 时，Store 必须明确支持并应用；无法证明过滤生效时拒绝请求，不得静默忽略。
- A3：业务显式引入 LangChain4j Adapter 后可复用已有模型注册完成 Chat 与 Embedding；只引入 AskData Starter 时不创建默认模型、Ollama 或 Provider Bean。
- A4：业务提供任一 Core SPI Bean 时默认 Flow 使用该实现；未知 key、重复实现和契约版本不兼容在启动期给出明确失败。
- A5：每次模型调用经过 Core Router；只有可重试基础设施故障可在无副作用的模型节点切换，结果、检查点和事件记录每次尝试及最终实际模型 key。
- A6：复合问题按稳定 ID 和依赖 DAG 在配置并发上限内执行；失败、超时、取消、跳过和部分结果保持独立终态且可恢复。
- A7：SQL 插件新增的具名参数传递至执行阶段，冲突参数被拒绝；组装、改写、准备、结果映射和审计按确定顺序执行，改写后重新完成治理与 SQL 安全校验。
- A8：业务 Agent 可按稳定步骤 key 替换选表、SQL 生成、校验、执行或解释；每步最多一个有效替换，未替换步骤保持默认行为。
- A9：完整生命周期发布脱敏事件；外部监控、导出器或本地 fallback 失败均不改变问数结果；语义压缩失败使用确定性压缩并保留恢复安全字段。
- A10：会话、检索、SQL、检查点和管理员日志始终校验 `projectId + operatorId` 及授权版本，默认治理桥对受保护资源 fail-closed。
- A11：Zulu JDK 8 下相关 Core、AI 自动配置和 Starter 测试通过；依赖树不泄漏 Spring AI、Spring AI Alibaba、AgentScope 或未显式选择的 LangChain4j Provider。
- A12：文档明确 Java 17 / Spring Boot 3 分支分别实现 Spring AI、Spring AI Alibaba、AgentScope Adapter，并复用当前 Core 契约或 Remote Runtime 边界。

# Constraints and invariants

- 当前代码保持 Java 8 与 Spring Boot 2.7.18 兼容，不使用 Java 9+ API。
- `fw-plus-ai-model`、`fw-plus-ai-store-embedding`、`fw-plus-ai-askdata` 不暴露 Spring 或具体 AI SDK 类型。
- Embedding 生成与向量存储职责分离；Adapter 不实现 AskData 权限、SQL 或跨节点重试策略。
- 业务实现和配置决定模型框架、Provider、模型 key 及存储能力；Starter 不替业务作默认运行时选择。
- 监控和压缩降级不得扩大权限、泄漏原始 Prompt、SQL 参数值、记忆正文或凭据。
- 保持现有单问题 API、Starter 纯依赖聚合分层和必要兼容重载。

# Decisions

- 通用模型层负责模型发现与类型化执行，AskData Core SPI 负责项目和操作人业务语义，两者通过单向桥接组合。
- 当前分支只正式实现 LangChain4j Adapter；其他三类 Adapter 在 Java 17 / Spring Boot 3 分支实现。
- 模型故障分为可重试基础设施失败与不可重试请求拒绝，只有前者允许候选切换。
- 多问题使用显式有界 DAG；SQL 使用阶段化不可变命令；Agent 使用稳定步骤 key 替换默认节点。
- 默认上下文压缩不依赖 AI 框架，LangChain4j 语义摘要为业务可选能力。

# Open questions

- 无。

# Verification expectations

- 严格按 TDD 执行，每项行为先观察聚焦测试因缺失能力失败，再实现并运行相关回归。
- 使用 Zulu JDK 8 验证 Core、LangChain4j Adapter、AI 自动配置、AskData Starter 和依赖树。
- 验证业务 Bean 优先、重复能力冲突、治理 fail-closed、路由尝试、DAG 并发/取消、SQL 参数传播、Agent 替换、监控隔离和压缩降级。
- 验证公共 Core/Starter 依赖树不包含被排除的可选 AI 运行时和默认 Provider。
