## 1. 中立模型与向量契约

- [ ] 1.1 为 `fw-plus-ai-model` 编写失败契约测试，覆盖类型化 Chat/Embedding、批量输入、实际模型 key、超时、参数、用量摘要和不可变复制。
- [ ] 1.2 增强 `AiModel` 类型化能力及请求/结果对象，保持 Java 8 和现有 `AiModelAdapter`/`AiModelRegistry` 兼容。
- [ ] 1.3 为 `fw-plus-ai-store-embedding` 编写失败测试，覆盖查询过滤不可变复制、过滤能力拒绝和参数校验。
- [ ] 1.4 增强 `AiEmbeddingQuery` 与 Store Adapter 能力诊断，确保权限相关过滤条件不得静默丢弃。
- [ ] 1.5 运行模型和 Embedding Store 模块测试并检查 Core 运行依赖树。

## 2. AskData Core SPI 与自动配置接线

- [ ] 2.1 编写 Core SPI 替换测试，证明业务模型、Embedding、元数据、会话、检查点和 Agent 状态实现能接管默认 Flow。
- [ ] 2.2 将 AskData 业务级模型/Embedding 请求结果桥接到中立模型注册表，保留旧构造和调用兼容。
- [ ] 2.3 在自动配置中发现并构造唯一 Core 能力注册表，校验未知 key、重复实现和契约版本冲突。
- [ ] 2.4 将默认 Flow 改为消费 Core 元数据、会话、检查点、状态和事件 SPI；为旧内部接口提供单向 deprecated Adapter。
- [ ] 2.5 运行 Core 与自动配置替换/上下文测试，确认 Starter 仍为纯依赖聚合模块。

## 3. LangChain4j Java 8 Adapter

- [ ] 3.1 编写 LangChain4j Chat/Embedding Adapter 失败测试，覆盖请求转换、批量 Embedding、模型身份、超时和异常分类。
- [ ] 3.2 在现有 `fw-plus-ai-langchain4j-adapter` 实现中立类型化模型能力和 AskData Core 桥接。
- [ ] 3.3 调整 LangChain4j 自动配置，按显式 Adapter 依赖和模型 key 注册能力，不设置默认 Ollama Provider。
- [ ] 3.4 验证现有 LangChain4j 模型注册信息可被 AskData 复用，业务 Core Bean 仍优先。
- [ ] 3.5 运行 LangChain4j Adapter、自动配置和 Starter 依赖树测试。

## 4. 模型路由与多问题执行

- [ ] 4.1 编写默认 Flow 模型路由测试，覆盖请求/项目/数据源/Agent/default 优先级、可重试切换、不可重试拒绝和候选耗尽。
- [ ] 4.2 将 Core `AskDataModelRouter` 接入模型调用，记录每次尝试、故障分类、路由原因和最终模型 key 到结果、事件与检查点。
- [ ] 4.3 编写多问题 DAG 失败测试，覆盖稳定 ID、依赖顺序、并发上限、超时、取消、依赖失败和部分结果。
- [ ] 4.4 实现基于 Core `AskDataMultiQuestionRequest`/`AskDataSubQuestion` 的调度器，单问题继续使用现有流程。
- [ ] 4.5 运行模型路由、多问题和恢复测试，确认 SQL/工具副作用开始后不重放模型节点。

## 5. SQL 插件与 Agent 步骤替换

- [ ] 5.1 编写 SQL 管线失败测试，覆盖插件新增具名参数、参数冲突、阶段顺序、结果映射、审计摘要和插件后完整复检。
- [ ] 5.2 扩展不可变 SQL 命令与拦截器阶段，保留插件安全输出并禁止扩大授权摘要。
- [ ] 5.3 将扩展 SQL 管线接入默认 Flow 的组装、校验、执行和结果映射节点。
- [ ] 5.4 编写 Agent 步骤替换失败测试，覆盖 SQL 生成替换后跳过默认生成、重复替换启动失败和默认后续步骤复用。
- [ ] 5.5 实现稳定步骤 key、类型化 Agent 输出和唯一替换装配。
- [ ] 5.6 运行 SQL 安全、治理桥和 Agent 流程测试。

## 6. 观测、压缩与恢复

- [ ] 6.1 编写完整事件序列测试，覆盖计划、子问题、模型尝试、Agent、SQL、记忆、压缩、检查点、恢复、取消、完成和失败。
- [ ] 6.2 扩展 Core 事件类型和关联字段，并在默认 Flow 各稳定节点发布脱敏事件。
- [ ] 6.3 编写监控存储/导出失败测试，证明异常只进入本地固定容量环形日志且不改变对话结果。
- [ ] 6.4 实现非阻断组合发布器与本地 fallback，保留管理员查询的项目/操作人边界。
- [ ] 6.5 编写上下文压缩测试，覆盖结构化字段保留、LangChain4j 语义压缩选择和失败回退。
- [ ] 6.6 实现默认确定性压缩器及可选 LangChain4j 语义压缩 Adapter。
- [ ] 6.7 运行观测、会话、检查点和恢复测试。

## 7. 文档、兼容性与最终验证

- [ ] 7.1 更新 AskData Starter 文档，明确 Adapter 由业务选择，LangChain4j 是当前 Java 8 实现但不是传递默认依赖。
- [ ] 7.2 编写 Spring AI、Spring AI Alibaba、AgentScope 的 Java 17 / Spring Boot 3 Adapter 契约文档，包含模块边界、能力映射、错误分类、鉴权、观测、回滚和验证清单。
- [ ] 7.3 更新迁移文档，说明旧自动配置接口、模型配置和会话存储如何迁移到 Core SPI。
- [ ] 7.4 使用 JDK 8 运行 Core Reactor、LangChain4j Adapter、AI 自动配置、Starter 和样例聚合验证。
- [ ] 7.5 检查 Core 无 Spring/LangChain4j 运行依赖、Starter 无具体 Provider/Java 17 运行时依赖、class major version 为 52。
- [ ] 7.6 运行 OpenSpec 严格校验、`git diff --check` 和 Java 9+ API 扫描，并记录所有验证结果。
