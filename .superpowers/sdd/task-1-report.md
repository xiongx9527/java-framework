# Task 1 执行报告：Core Governance Model

## 实现内容

- 新增不可变的 `AskDataAuthorizationRequest`，封装并校验 `AskDataProjectContext`。
- 新增不可变的 `AskDataAuthorizedObject`，以数据源、表、列三元组标识授权对象，并提供值相等比较。
- 新增不可变的 `AskDataRowPredicate`，保存目标授权对象、SQL 片段和 `List<AskDataSqlParameter>` 参数化绑定值。
- 新增不可变的 `AskDataAuthorizationSummary`，提供 `allow(...)`、`deny(...)`、防御性列表副本、不可变列表访问和三元对象匹配。
- 新增 `AskDataGovernanceProviderTest`，覆盖拒绝结果、允许结果的防御性复制及三元对象匹配。

## RED

命令：

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-askdata -Dtest=AskDataGovernanceProviderTest test
```

失败摘要：测试编译失败，`AskDataAuthorizationSummary`、`AskDataAuthorizedObject` 和 `AskDataRowPredicate` 等新模型不存在，共报告 21 个“找不到符号”错误，符合先写测试的预期。

## GREEN

命令：

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-askdata -Dtest=AskDataGovernanceProviderTest test
```

通过摘要：Maven `BUILD SUCCESS`；`AskDataGovernanceProviderTest` 运行 3 个测试，失败 0、错误 0、跳过 0。

## 变更文件

- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/security/AskDataAuthorizationRequest.java`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/security/AskDataAuthorizedObject.java`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/security/AskDataRowPredicate.java`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/security/AskDataAuthorizationSummary.java`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/test/java/com/xiongx9527/fw/plus/ai/askdata/security/AskDataGovernanceProviderTest.java`

## 提交

`777b4c24 feat(askdata): add governance authorization model`

## 风险信号自报

- Maven 在当前 JDK 下输出了 `System::load` 和 `sun.misc.Unsafe` 的未来兼容性警告；本任务测试仍通过，且该警告来自 Maven/Jansi/Guava 依赖运行时，不由本次模型代码引入。
- 工作区保留了用户归属的 Comet 与无关 change 未提交改动；本提交仅包含 Task 1 的五个指定文件。
