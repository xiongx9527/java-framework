# Task 2 执行报告

## 实现内容

- 新增 `AskDataGovernanceProvider` SPI 与默认拒绝实现 `DefaultDenyAskDataGovernanceProvider`，拒绝码为 `ASKDATA_GOVERNANCE_DENIED`。
- 为 `AskDataSqlCommand` 增加授权摘要字段、访问器、带摘要的兼容构造器和 `withAuthorizationSummary(...)`。
- SQL 拦截器链在每个插件返回后校验授权摘要，拒绝扩大已授权对象或移除既有行级谓词的结果；最终命令继续使用原始命令的上下文、参数和授权摘要。
- 新增默认拒绝、摘要携带、对象扩权拒绝和行级谓词移除拒绝测试。

## RED

命令：

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-askdata -Dtest='AskDataGovernanceProviderTest,AskDataSqlPipelineTest' test
```

失败摘要：测试编译失败，缺少 `AskDataGovernanceProvider`、`DefaultDenyAskDataGovernanceProvider`、`AskDataSqlCommand.getAuthorizationSummary()`、带授权摘要的构造器与 `withAuthorizationSummary(...)`。

## GREEN

命令：

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-askdata -Dtest='AskDataGovernanceProviderTest,AskDataSqlPipelineTest' test
```

通过摘要：`BUILD SUCCESS`；`AskDataGovernanceProviderTest` 4 项、`AskDataSqlPipelineTest` 21 项，共 25 项测试通过。

## 变更文件

- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/security/AskDataGovernanceProvider.java`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/security/DefaultDenyAskDataGovernanceProvider.java`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/sql/AskDataSqlCommand.java`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/sql/AskDataSqlInterceptorChain.java`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/test/java/com/xiongx9527/fw/plus/ai/askdata/security/AskDataGovernanceProviderTest.java`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/test/java/com/xiongx9527/fw/plus/ai/askdata/sql/AskDataSqlPipelineTest.java`

## 提交 SHA

`98e69494`

## 风险信号自报

- Maven/Jansi 和 Guava 在当前 JDK 输出未来兼容性警告；本次目标测试未受影响。
- `openspec/changes/add-askdata-governance-permission-bridge/.comet/subagent-progress.md` 是主会话协调检查点，保持未修改、未暂存、未提交。

## Task 2 Review 修复：禁止插件清空授权对象

### 根因

`AskDataSqlInterceptorChain.validateAuthorizationBoundary(...)` 原先使用
`original.getAuthorizedObjects().containsAll(rewritten.getAuthorizedObjects())` 校验对象范围。
插件将授权对象改为空集时，该表达式恒为 `true`；在原摘要没有行级谓词的场景下，谓词校验也会通过，导致插件清空授权对象未被显式拒绝。

### RED

命令：

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-askdata -Dtest='AskDataGovernanceProviderTest,AskDataSqlPipelineTest' test
```

失败摘要：新增 `shouldRejectPluginThatClearsAuthorizationSummaryObjectsWithoutRowPredicates` 后，
共 26 项测试中 1 项失败。失败信息为：预期抛出 `IllegalArgumentException`，实际未抛出；确认复现授权对象被清空而未拒绝的问题。

### GREEN

命令：

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-askdata -Dtest='AskDataGovernanceProviderTest,AskDataSqlPipelineTest' test
```

通过摘要：`BUILD SUCCESS`；`AskDataGovernanceProviderTest` 4 项、`AskDataSqlPipelineTest` 22 项，共 26 项测试通过。

### 变更文件

- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/sql/AskDataSqlInterceptorChain.java`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/test/java/com/xiongx9527/fw/plus/ai/askdata/sql/AskDataSqlPipelineTest.java`

### 提交 SHA

`14c08490dafd5f3f5179c745d0e7366631110cb6` — `fix(askdata): reject cleared sql authorization objects`

### 风险信号

- Maven 运行期间继续出现 Jansi native access 与 Guava `sun.misc.Unsafe` 的未来兼容性警告；目标测试已通过，未由本修复引入。
- 授权对象现在要求插件精确保持，当前 Task 2 的安全边界明确；未来若产品需要插件缩小授权范围，应先定义可审计的受限缩窄语义及对应测试。
