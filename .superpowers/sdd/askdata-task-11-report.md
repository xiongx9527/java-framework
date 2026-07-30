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

## 第二轮 Reviewer 修复 RED/GREEN

### RED

新增三项回归测试后，执行：

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -Dtest=AskDataConversationStoreTest,AskDataMonitoringLogStoreTest,DefaultAskDataFlowExecutorTest,AskDataAutoConfigurationTest,AskDataPropertiesBindingTest test
```

结果：38 tests，2 failures，1 error。集群模式在三个 Core 状态 SPI、向量和旧会话均由业务提供但缺少 `AskDataMonitoringLogStore` 时仍可启动；业务自定义 `AskDataEventPublisher` 会使基础监控日志为空；只替换一个 Core 状态 SPI 时其余默认 SPI 因依赖未创建的 `InMemoryAskDataStores` 无法启动。修正测试 Bean 名冲突后，三项均稳定为预期失败断言。

### GREEN

- 三个 Core 状态 SPI 以独立的单机内存默认工厂补齐，因此任意一个 SPI 被业务替换时，其余缺失 SPI 仍可装配；集群模式下任一默认工厂仍拒绝本地状态。
- 默认监控日志存储在集群模式启动期拒绝，要求业务提供共享 `AskDataMonitoringLogStore`，不再因其余状态 SPI 已存在而静默落到节点内存。
- 基础监控日志发布器固定为主发布器；默认 flow 会先写入该发布器，再调用业务自定义发布器，外部导出不能替代项目与操作人隔离的脱敏日志。

验证：

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -Dtest=AskDataConversationStoreTest,AskDataMonitoringLogStoreTest,DefaultAskDataFlowExecutorTest,AskDataAutoConfigurationTest,AskDataPropertiesBindingTest test
```

结果：38 tests，0 failures，0 errors。

## 第三轮 Reviewer 修复 RED/GREEN

### RED

先新增两项回归：默认 flow 在缺少 `projectContext`、`projectId` 空白或 `operatorId` 空白时必须在元数据、模型和 SQL 主流程之前受控失败；外部 `AskDataEventPublisher` 抛出运行时异常时，基础监控日志仍需记录开始与完成事件，且主对话必须成功完成。

执行：

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -Dtest=AskDataConversationStoreTest,AskDataMonitoringLogStoreTest,DefaultAskDataFlowExecutorTest,AskDataAutoConfigurationTest,AskDataPropertiesBindingTest test
```

结果：39 tests，1 failure，1 error。无效上下文请求仍进入元数据加载器（3 次）；外部发布器异常直接中断对话。两项失败均与复审问题一致。

### GREEN

- `DefaultAskDataFlowExecutor` 在生成会话、发布生命周期事件和进入恢复/主流程之前，强制校验 `projectContext`、`projectId`、`operatorId`。缺失或只包含 Unicode 空白的标识返回既有 `MODEL_RESPONSE_INVALID` 受控失败，不产生不可审计对话。
- `combineEventPublishers` 保持基础日志先写入，对每个外部发布器的运行时异常做隔离，外部导出失败不会中断主流程或阻断后续生命周期日志。
- 原有默认 flow 单测夹具补充有效项目与操作人上下文，以符合新的安全入口契约。

执行同一命令通过：39 tests，0 failures，0 errors。

## 第三轮自查

- Core 未新增 Spring、模型 SDK 或存储厂商类型；实现保持 Java 8 兼容。
- 有效请求仍按 `projectId + operatorId` 分区写入脱敏生命周期日志；无有效分区的请求在主流程前拒绝。
- 集群模式的本地状态、向量和监控日志拒绝策略未改变。
