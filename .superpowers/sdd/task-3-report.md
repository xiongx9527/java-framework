# Task 3 实施报告：Starter Autoconfiguration and Flow Integration

## 实现内容

- 在 `AskDataAutoConfiguration` 注册 `@ConditionalOnMissingBean` 的 `AskDataGovernanceProvider`，默认使用 `DefaultDenyAskDataGovernanceProvider`；业务自定义 Bean 会覆盖默认实现。
- 将治理 Provider 注入 `DefaultAskDataFlowExecutor`；保留既有构造器并委托给新增的完整构造器，旧的手工构造路径保持兼容。
- 在单问题流程的模型、元数据访问前执行授权；拒绝、空结果或治理 SPI 异常均返回受控的 `ASKDATA_GOVERNANCE_DENIED`，且步骤日志不包含凭据或原始参数。
- 将允许的 `AskDataAuthorizationSummary` 放入本次执行的步骤上下文，并传入新建的 `AskDataSqlCommand`，使 SQL 插件链和最终校验保持授权边界。
- 新增 Starter 默认/自定义 Provider 覆盖测试，以及授权短路和授权摘要传递测试；原有成功流程测试显式注册允许 Provider。

## RED 证据

命令：

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest='AskDataAutoConfigurationTest,DefaultAskDataFlowExecutorTest' test
```

首次执行发现本地 Maven 快照缺少 Task 2 的 `security` 契约；安装当前 Core AskData 模块后，以相同命令重新执行。第二次在 `DefaultAskDataFlowExecutorTest.java:383` 失败：不存在接收 `AskDataGovernanceProvider` 的 `DefaultAskDataFlowExecutor` 构造器。这证明新测试在生产接线前为 RED。

## GREEN 证据

命令：

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest='AskDataAutoConfigurationTest,DefaultAskDataFlowExecutorTest' test
```

结果：`BUILD SUCCESS`；`AskDataAutoConfigurationTest` 17 项、`DefaultAskDataFlowExecutorTest` 14 项，共 31 项通过，0 failure、0 error、0 skipped。

## 变更文件

- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/autoconfigure/AskDataAutoConfiguration.java`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/flow/DefaultAskDataFlowExecutor.java`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/autoconfigure/AskDataAutoConfigurationTest.java`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/flow/DefaultAskDataFlowExecutorTest.java`

`CoreAskDataServiceAdapter.java` 位于允许范围但不需要修改：它委托的旧流程入口已在本任务中执行授权。

## 提交

`68d7bcb5 feat(askdata): wire governance provider into starter flow`

## 风险信号自报

- 初始 RED 受本地 Maven 缓存中缺少 Task 2 契约影响；已用当前工作区 Core AskData 模块安装到本地仓库后复测。该操作未修改仓库源码。
- Maven 输出仍有既有的 JDK restricted-method、SLF4J 多绑定和过时 API 警告；目标测试均通过，未将这些环境告警作为 Task 3 行为失败处理。
- 默认策略为拒绝，依赖 Starter 的既有流程需要由业务显式提供允许的 `AskDataGovernanceProvider`，这是设计要求带来的集成注意事项。

---

## Critical 修复报告：关闭 Starter 治理绕过

### 修复内容

- 所有 `DefaultAskDataFlowExecutor` 构造路径在未传入治理 Provider 时统一回退到 `DefaultDenyAskDataGovernanceProvider`，手工构造不再绕过治理。
- 仅接受当前数据源存在至少一个对象的允许授权摘要；`null`、拒绝、空对象或不匹配当前数据源的摘要都在模型和元数据访问前以 `ASKDATA_GOVERNANCE_DENIED` 拒绝。
- 从授权摘要生成命令 `authorizedObjects`，并以同一对象集合收窄普通 SQL 校验器使用的元数据。这样默认 SQL 校验器会拒绝授权摘要之外的表，插件链/恢复校验也得到同一边界。
- 成功流程测试显式注入 `orders` 允许对象；新增旧构造路径、空摘要、未授权 `payments` 表的回归测试。

### RED 证据

运行指定命令后共 34 项测试，其中 3 项失败：

- `shouldDenyOldConstructorPathWithoutGovernanceProviderBeforeModelInvocation`：实际返回 `MODEL_RESPONSE_INVALID`，证明 `null` Provider 绕过治理。
- `shouldRejectEmptyAuthorizedObjectsBeforeModelInvocation`：实际返回 `MODEL_RESPONSE_INVALID`，证明空允许摘要被放行。
- `shouldRejectTableOutsideGovernanceAuthorizedObjects`：实际成功，证明 `payments` 仍由全量元数据授权。

### GREEN 证据

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest='AskDataAutoConfigurationTest,DefaultAskDataFlowExecutorTest' test
```

结果：`BUILD SUCCESS`；`AskDataAutoConfigurationTest` 17 项、`DefaultAskDataFlowExecutorTest` 17 项，共 34 项通过，0 failure、0 error、0 skipped。

### 风险与剩余范围

- 本次未运行 Core 安装命令；本地已有所需 AskData snapshot。
- Maven 仍输出既有 JDK restricted-method、SLF4J 多绑定和过时 API 警告，目标测试不受影响。
- 行级谓词的 SQL 注入不属于 Task 3，本次未实现；该工作保留给 Task 4。列级语义仍由下游 Core SQL 安全校验器/业务 Provider 的对象策略负责，本次确保 Starter 不再以全量元数据扩大表级授权。

---

## Critical 修复报告（第 2 轮）：关闭未落实谓词与空元数据交集

### 修复内容

- 授权摘要包含任何行级谓词时，Task 3 流程在模型调用前 fail-closed，返回 `ASKDATA_GOVERNANCE_DENIED`，不再将尚未注入或可靠验证的谓词仅随 SQL 命令携带。
- 每个新流程和恢复流程的元数据加载路径均校验当前数据源的授权对象与实际元数据表存在交集；空交集统一按治理拒绝处理，不能将空表元数据交给默认 SQL 校验器形成不确定的授权边界。
- 新增两条回归：未落实的 `tenant_id = :tenantId` 谓词会在模型前拒绝；授权同数据源的未知表而模型请求现有 `payments` 表会以治理拒绝结束，且不调用 SQL 执行器。

### RED 证据

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest='AskDataAutoConfigurationTest,DefaultAskDataFlowExecutorTest' test
```

结果：`BUILD FAILURE`，36 项中 2 项失败，且均为预期行为缺失：

- `shouldDenyUnresolvedRowPredicateBeforeModelInvocation` 实际成功，证明含行级谓词的 SQL 仍可执行。
- `shouldDenyAuthorizedObjectsWithoutMetadataIntersectionBeforeSqlExecution` 实际返回 `SQL_REJECTED`，未以受控治理拒绝关闭空授权元数据交集。

### GREEN 证据

已先用模块解析出的编译类路径分别编译本轮修改的 Flow 类和两个目标测试类，再直接运行 Surefire：

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure surefire:test -Dtest='AskDataAutoConfigurationTest,DefaultAskDataFlowExecutorTest'
```

结果：`BUILD SUCCESS`；`AskDataAutoConfigurationTest` 17 项、`DefaultAskDataFlowExecutorTest` 19 项，共 36 项通过，0 failure、0 error、0 skipped。

### 风险信号

- 必跑的完整 Maven 生命周期在主源码重新编译时被范围外文件 `LegacyEmbeddingStoreAiAdapter.java` 缺少 `AiEmbeddingStoreAdapter#getFramework()` 实现阻断；随后跳过主源码重编译的测试编译还被范围外的 `EmbeddingStoreAutoConfiguration` 缺失阻断。本轮未修改这些文件。
- Maven/JDK 的 restricted-method、`Unsafe` 和 SLF4J 多绑定警告仍存在；未影响上述手工编译与目标 Surefire 回归。
