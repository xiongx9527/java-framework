# AskData Task 11 执行报告

## 范围

- 自动配置层为 Core AskData 提供单机内存会话、检查点、智能体状态和监控日志存储。
- 自动装配生命周期事件发布器，强制将已脱敏事件写入 Core 监控日志存储。
- 自动装配项目管理员日志访问策略；同项目已授权管理员允许读取，普通操作人和跨项目管理员默认拒绝。
- 新增 `fw.plus.ai.askdata.state.cluster-mode`、`state.store` 与 `vector.store` 配置；集群模式使用默认本地内存状态或向量时在启动期拒绝。

## RED

先新增单机默认状态、集群内存拒绝、生命周期日志持久化和管理员策略测试。

首次按任务命令执行时，原有 `AskDataConversationStoreTest` 使用 JUnit 4，而当前 Surefire 只发现 JUnit 5，结果为 0 tests；已将该目标测试切换至项目现用的 JUnit 5。

再次执行：

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -Dtest=AskDataConversationStoreTest,AskDataMonitoringLogStoreTest test
```

结果为 5 tests，3 项按预期失败：缺少 Core 会话/检查点/智能体状态 Bean、缺少事件发布器、集群内存模式未拒绝。

## GREEN

实现后同一命令通过：5 tests，0 failures，0 errors。

额外回归：

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -Dtest=AskDataAutoConfigurationTest,AskDataPropertiesBindingTest test
```

结果通过：15 tests，0 failures，0 errors。

## 自查

- 生产接口仍只引用 Core 的存储与观测契约；未向 Core 引入 Spring、LangChain4j、Ollama、Spring AI、AgentScope 或存储厂商类型。
- 默认内存实现由 Core 按 `projectId + operatorId` 分区；监控日志只通过 `AskDataEventPublisher.toStore` 写入已脱敏消息。
- 新增公开配置和自动装配入口带有中文注释。
- 所有新增实现保持 Java 8 兼容，未使用 Java 9+ API。
- 本任务路径 `git diff --check` 通过。仓库中其他任务文件已有尾随空行告警，未修改。

## Reviewer 修复 RED/GREEN

### RED

新增检查点有界保留测试后，首次执行目标测试在编译期失败：`InMemoryAskDataConversationStore(int)` 不存在，证明容量契约尚未实现。补充集群伪装配置测试后，原实现会在 `cluster-mode=true,state.store=redis,vector.store=redis` 下启动，证明仅检查配置字符串可以绕过实际默认内存状态。默认 flow 监控断言也未发现生命周期日志。

### GREEN

- 默认 `InMemoryAskDataStores` 仅在三个 Core 状态 SPI 都不存在时创建，且集群模式一律拒绝创建；状态/向量的 `store` 文本不再作为共享能力的依据。
- 默认向量检索工厂在集群模式直接拒绝，业务必须提供实际 `AskDataVectorRetriever` SPI；默认旧会话存储也在集群模式拒绝。
- `DefaultAskDataFlowExecutor` 注入可选发布器，按具有 `projectContext` 的请求写入不含问题、SQL、参数或模型输出的 `CONVERSATION_STARTED`、`CONVERSATION_COMPLETED`、`CONVERSATION_FAILED`，恢复时另写 `RECOVERY_ATTEMPTED`。
- `conversation.max-checkpoints-per-conversation` 和 `state.monitoring-log-capacity` 分别约束默认旧检查点、默认监控日志的每范围保留量；Core 固定内存集合未修改。

验证命令：

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -Dtest=AskDataConversationStoreTest,AskDataMonitoringLogStoreTest,DefaultAskDataFlowExecutorTest,AskDataAutoConfigurationTest,AskDataPropertiesBindingTest test
```

结果：34 tests，0 failures，0 errors。
