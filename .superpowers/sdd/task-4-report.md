# Task 4 实施报告

## 状态

DONE_WITH_CONCERNS

## RED

1. `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest='AskDataMonitoringLogQueryServiceTest,DefaultAskDataFlowExecutorTest' test`
   - 未进入测试执行：范围外 `LegacyEmbeddingStoreAiAdapter` 未实现 `AiEmbeddingStoreAdapter#getFramework()`，Maven 主编译失败。
2. 聚焦编译后执行：
   `../../mvnw surefire:test -Dtest='AskDataMonitoringLogQueryServiceTest,DefaultAskDataFlowExecutorTest'`
   - 预期失败：普通操作人查询日志返回 1 条；未授权 `secret` 列进入传给 Prompt 的 metadata。

## GREEN

1. 生成测试依赖类路径：
   `../../mvnw dependency:build-classpath -Dmdep.includeScope=test -Dmdep.pathSeparator=: -Dmdep.outputFile=target/task-4-test.classpath`
2. 聚焦编译 Task 4 生产类与测试类：
   `javac -source 8 -target 8 -cp "target/classes:$(tr -d '\\n' < target/task-4-test.classpath)" -d target/classes ...`
   `javac -source 8 -target 8 -cp "target/classes:$(tr -d '\\n' < target/task-4-test.classpath)" -d target/test-classes ...`
3. 聚焦 Surefire：
   `../../mvnw surefire:test -Dtest='AskDataMonitoringLogQueryServiceTest,DefaultAskDataFlowExecutorTest'`
   - 结果：22 tests，0 failures，0 errors。

## 实施内容

- `AskDataMetadataProvider` 新增 Java 8 default overload，兼容已有 `load(AskDataRequest)` 实现，并按 datasource、表、列或 `*` 授权对象收窄 metadata。
- Flow 的新建与恢复路径均调用 authorization-aware overload，Prompt 和后续 SQL 校验使用已收窄 metadata。
- `EmptyAskDataMetadataProvider` 显式兼容新 overload。
- 监控日志普通操作人默认拒绝；项目管理员保持同项目限制；平台管理员仅查询调用方显式传入的 scope。
- 新增列级 Prompt 过滤、项目管理员跨项目拒绝、平台管理员显式 scope 和普通操作人拒绝测试。

## 风险与关注项

- 完整 Maven 生命周期被范围外 `LegacyEmbeddingStoreAiAdapter` 编译缺陷阻断，未修改该文件。
- 监控日志管理员角色由宿主调用方传入；平台管理员可读取的边界仍由显式 `scope` 参数限定，宿主 IAM 必须负责授予该角色。

---

## 第 1 轮 Review 修复（Task 4）

### 状态

DONE_WITH_CONCERNS

### RED

1. `cd fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure && ../../mvnw dependency:build-classpath -Dmdep.includeScope=test -Dmdep.pathSeparator=: -Dmdep.outputFile=target/task-4-test.classpath && javac -source 8 -target 8 -cp "target/classes:$(tr -d '\n' < target/task-4-test.classpath)" -d target/test-classes src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/observability/AskDataMonitoringLogQueryServiceTest.java`
   - 预期失败：新增测试使用的 `AskDataMonitoringLogQueryService(store, policy)` 构造器不存在，证明平台管理员角色此前可以绕过访问策略。
2. 聚焦编译访问策略修复后，执行：
   `../../mvnw surefire:test -Dtest='AskDataMonitoringLogQueryServiceTest'`
   - 预期失败：`shouldWriteSanitizedAuditLogWhenAdministratorReadsMonitoringLogs` 断言管理员分区存在 1 条审计日志，实际为 0。
3. 聚焦编译自动配置与测试：
   `javac -source 8 -target 8 -cp "target/classes:$(tr -d '\n' < target/task-4-test.classpath)" -d target/classes src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/autoconfigure/AskDataAutoConfiguration.java && javac -source 8 -target 8 -cp "target/classes:$(tr -d '\n' < target/task-4-test.classpath)" -d target/test-classes src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/observability/AskDataMonitoringLogQueryServiceTest.java`
   - 预期失败：自动配置的查询服务 Bean 方法只接收 store，不能传入已配置的访问策略。

### GREEN

1. 聚焦编译并执行：
   `javac -source 8 -target 8 -cp "target/classes:$(tr -d '\n' < target/task-4-test.classpath)" -d target/classes src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/observability/AskDataMonitoringLogQueryService.java src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/autoconfigure/AskDataAutoConfiguration.java && javac -source 8 -target 8 -cp "target/classes:$(tr -d '\n' < target/task-4-test.classpath)" -d target/test-classes src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/observability/AskDataMonitoringLogQueryServiceTest.java src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/flow/DefaultAskDataFlowExecutorTest.java && ../../mvnw surefire:test -Dtest='AskDataMonitoringLogQueryServiceTest,DefaultAskDataFlowExecutorTest'`
   - 结果：24 tests，0 failures，0 errors。

### 修复内容

- 查询服务新增 `AskDataMonitoringLogAccessPolicy` 依赖，并逐条将存储日志包装为 Core `AskDataMonitoringLog` 后调用策略判定；角色仅用于向策略传递管理员授权状态，不再直接越过项目边界。
- 保留单参数构造器兼容性，其默认策略为 `projectAdministratorOnly()`；自动配置改为注入容器中可替换的策略 Bean。
- 管理员查询完成后，在管理员自身的隔离分区追加 `MONITORING_LOG_QUERY` 审计日志。审计消息只含 scope 项目和 trace 筛选是否存在，不含原始 trace 值、参数或凭据；返回快照在写审计前已完成筛选，因此不会包含本次审计记录。

### 风险与关注项

- 完整 Maven 测试命令仍在编译阶段被范围外 `LegacyEmbeddingStoreAiAdapter` 未实现 `AiEmbeddingStoreAdapter#getFramework()` 阻断，未修改该范围外文件。

---

## 第 2 轮 Review 修复（Task 4）

### 状态

DONE_WITH_CONCERNS

### RED

1. 完整命令：
   `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest='AskDataMonitoringLogQueryServiceTest,DefaultAskDataFlowExecutorTest' test`
   - 未进入测试执行：范围外 `LegacyEmbeddingStoreAiAdapter` 未实现 `AiEmbeddingStoreAdapter#getFramework()`，主编译失败。
2. 聚焦测试：
   `cd fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure && ../../mvnw dependency:build-classpath -Dmdep.includeScope=test -Dmdep.pathSeparator=: -Dmdep.outputFile=target/task-4-test.classpath && javac -source 8 -target 8 -cp "target/classes:$(tr -d '\n' < target/task-4-test.classpath)" -d target/test-classes src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/observability/AskDataMonitoringLogQueryServiceTest.java && ../../mvnw surefire:test -Dtest='AskDataMonitoringLogQueryServiceTest'`
   - 预期失败：`shouldRejectForgedProjectAdministratorRoleByDefault` 期望 0 条日志，实际返回 1 条，证明旧实现直接信任调用方传入的管理员枚举。

### GREEN

1. 聚焦编译并执行：
   `cd fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure && javac -source 8 -target 8 -cp "target/classes:$(tr -d '\n' < target/task-4-test.classpath)" -d target/classes src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/observability/AskDataMonitoringLogAccessAuthorizer.java src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/observability/AskDataMonitoringLogQueryService.java src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/autoconfigure/AskDataAutoConfiguration.java && javac -source 8 -target 8 -cp "target/classes:$(tr -d '\n' < target/task-4-test.classpath)" -d target/test-classes src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/observability/AskDataMonitoringLogQueryServiceTest.java src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/flow/DefaultAskDataFlowExecutorTest.java && ../../mvnw surefire:test -Dtest='AskDataMonitoringLogQueryServiceTest,DefaultAskDataFlowExecutorTest'`
   - 结果：28 tests，0 failures，0 errors。

### 修复内容

- 新增 Starter-local `AskDataMonitoringLogAccessAuthorizer`。调用方角色仅作为解析输入，授权结果必须由宿主根据 requester、显式 scope 与角色确认。
- 默认和旧构造器均使用 fail-closed 的拒绝授权解析器；未接入业务 IAM 时，伪造 `PROJECT_ADMINISTRATOR` 或 `PLATFORM_ADMINISTRATOR` 均不能读取日志或写查询审计。
- 自动配置以 `@ConditionalOnMissingBean` 暴露默认拒绝解析器，并将可替换 Bean 注入查询服务。
- 逐条日志边界仍由 `AskDataMonitoringLogAccessPolicy` 校验；传入的管理员布尔值来自授权解析器。测试覆盖自定义项目管理员、平台管理员显式 scope、策略边界和审计副作用。

### 风险与关注项

- 完整 Maven 生命周期仍由范围外 `LegacyEmbeddingStoreAiAdapter#getFramework()` 编译缺陷阻断，未修改范围外文件。

---

## AskData 模型注册中立化（本次实施）

### 状态

DONE_WITH_CONCERNS

### 实现内容

- 新增 `AiModelAskDataModelRegistry`，通过 `AiModelRegistry` 按配置候选和默认 key 解析 `CHAT`、`EMBEDDING` 能力。
- 中立 `AiModel` 可直接实现 AskData chat/embedding 门面；仅在该默认实现内部对 `LangChain4jAiModel` 解包为当前默认运行时。
- `AskDataAutoConfiguration#askDataModelRegistry` 改为注入 `ObjectProvider<AiModelRegistry>`，移除默认路径中 `Map<String, ChatLanguageModel>`、`Map<String, EmbeddingModel>` 和旧 source 的收集。
- 保持 `AskDataModelRegistry` 接口不变，并以 `@ConditionalOnMissingBean` 验证业务自定义 Bean 覆盖默认实现。
- 旧的直接 LangChain4j Spring Bean 测试改为断言其不再是默认 AskData 模型路径。

### RED

1. `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml test -Dtest=AiModelAskDataModelRegistryTest`
   - 预期失败：测试编译提示 `AiModelAskDataModelRegistry` 不存在，证明新增中立注册表测试先于实现。
2. 默认实现落地后的指定组合测试首次执行：
   `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml test -Dtest=AiModelAskDataModelRegistryTest,AskDataAutoConfigurationTest,LangChain4jAskDataModelRegistryTest`
   - 预期失败：旧测试仍要求无 `AiModelRegistry` 时直接收集 `ChatLanguageModel`/`EmbeddingModel`，与本任务的新主路径契约冲突。

### GREEN

1. `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml test -Dtest=AiModelAskDataModelRegistryTest`
   - 结果：2 tests，0 failures，0 errors。
2. `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml test -Dtest=AiModelAskDataModelRegistryTest,AskDataAutoConfigurationTest,LangChain4jAskDataModelRegistryTest`
   - 结果：35 tests，0 failures，0 errors。

### 变更文件

- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/model/AiModelAskDataModelRegistry.java`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/autoconfigure/AskDataAutoConfiguration.java`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/model/AiModelAskDataModelRegistryTest.java`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/autoconfigure/AskDataAutoConfigurationTest.java`
- `.superpowers/sdd/task-4-report.md`

### 自审结果

- `git diff --check` 通过。
- 默认 Bean 的签名不再包含 `Map<String, ChatLanguageModel>` 或 `Map<String, EmbeddingModel>`。
- `LangChain4jAskDataModelRegistry` 没有默认构造入口；新默认实现只在本地兼容分支引用 `LangChain4jAiModel`。
- 未修改 TinyEngine、AskData vector、Task 5+、`openspec/**`、`docs/superpowers/**` 或 `.comet/**`。

### 风险信号与顾虑

- Maven 输出仍报告本地已安装 BOM 存在重复 dependencyManagement 项，以及测试类路径存在双 SLF4J binding；两者均为范围外环境警告，指定测试已实际执行并通过。
- `AiModel` 本身只定义元数据，非 LangChain4j provider 需提供 AskData chat/embedding 门面或后续专用适配器，才能被 AskData 调用；当前行为会返回明确的 `AskDataModelException`，不会退回到直接收集 LangChain4j Bean。
