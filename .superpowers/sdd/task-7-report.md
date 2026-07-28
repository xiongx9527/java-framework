# Task 7 实施报告

## 状态

DONE_WITH_CONCERNS

## 实现内容

- 新增会话检查点模型、内存存储和上下文压缩接口/默认实现。
- 在稳定步骤 `metadata`、`generate-sql`、`validate-sql`、`execute-sql` 后保存检查点，并为检查点和恢复令牌生成 UUID。
- 支持按恢复令牌或会话 ID 加载检查点；版本不匹配、检查点缺失、SQL 未验证或状态不安全时分别返回既有错误码。
- 已验证 SQL 的恢复路径跳过元数据、提示词、模型和校验器；已执行检查点不会再次执行 SQL。
- 自动配置注册内存会话存储与默认压缩器，均使用 `@ConditionalOnMissingBean`。

## 变更文件

- `fw-plus-ai/.../askdata/conversation/AskDataConversationStore.java`
- `fw-plus-ai/.../askdata/conversation/AskDataContextCompressor.java`
- `fw-plus-ai/.../askdata/conversation/AskDataCheckpoint.java`
- `fw-plus-ai/.../askdata/conversation/AskDataCompressedContext.java`
- `fw-plus-ai/.../askdata/conversation/InMemoryAskDataConversationStore.java`
- `fw-plus-ai/.../askdata/conversation/DefaultAskDataContextCompressor.java`
- `fw-plus-ai/.../askdata/flow/AskDataStepContext.java`
- `fw-plus-ai/.../askdata/flow/DefaultAskDataFlowExecutor.java`
- `fw-plus-ai/.../askdata/autoconfigure/AskDataAutoConfiguration.java`
- `fw-plus-ai/.../askdata/conversation/AskDataConversationStoreTest.java`
- `fw-plus-ai/.../askdata/conversation/DefaultAskDataContextCompressorTest.java`
- `fw-plus-ai/.../askdata/flow/AskDataResumeFlowTest.java`

## TDD 证据

RED：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8); export PATH="$JAVA_HOME/bin:$PATH"; \
./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter \
  -Dtest=AskDataConversationStoreTest,DefaultAskDataContextCompressorTest,AskDataResumeFlowTest test
```

首次运行因缺少 `AskDataCheckpoint`、`AskDataConversationStore`、`AskDataContextCompressor` 及相关默认实现而在测试编译阶段失败。补齐初始实现后，为首次会话检查点与压缩异常新增测试，再运行 `-Dtest=AskDataResumeFlowTest test`，2 项断言按预期失败。

GREEN：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8); export PATH="$JAVA_HOME/bin:$PATH"; \
./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter \
  -Dtest=AskDataConversationStoreTest,DefaultAskDataContextCompressorTest,AskDataResumeFlowTest test
```

结果：8 项测试通过，0 failures，0 errors。

最终验证：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8); export PATH="$JAVA_HOME/bin:$PATH"; \
./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter test
git diff --check
```

结果：50 项测试通过，0 failures，0 errors；`git diff --check` 无输出。

## 风险信号

- 命中已有构建风险：`fw-plus-ai-boot-autoconfigure` 存在 3 条重复 LangChain4j 依赖 Maven 警告，位于本任务范围外，未修改。
- 命中已有测试环境风险：SLF4J 未加载 `StaticLoggerBinder`，回退到 NOP logger；测试仍全部通过。
- 未命中产品约束风险：未引入 Spring AI、AgentScope Java、LangGraph4j Java17 主线依赖；未发现 SQLBot 品牌、包名或配置前缀；通过 Java 8 `JAVA_HOME` 执行验证。

## Review Fix（2026-07-24）

### 状态

DONE_WITH_CONCERNS

### 修复内容

- `DefaultAskDataFlowExecutor.isSafeCheckpoint` 现在只接受 `metadata`、`metadata -> prompt -> generate-sql`、`metadata -> prompt -> generate-sql -> validate-sql`、`metadata -> prompt -> generate-sql -> validate-sql -> execute-sql` 的合法前缀，并校验 `currentStep` 必须与最后完成步骤相同。
- 按检查点阶段严格校验 SQL、`sqlValidated`、`sqlExecuted` 和 `rowCount` 的组合。任何无法确认的状态均在 SQL 执行前返回 `RESUME_FAILED`。
- metadata 检查点恢复时重新调用 `AskDataMetadataProvider.load(request)` 获取结构化 metadata，再继续提示词、模型、SQL 校验和执行流程；未把压缩摘要作为 metadata 的替代品。
- `InMemoryAskDataConversationStore` 保存时存放深度快照，`save`、`loadLatest`、`loadByResumeToken` 均返回独立快照；覆盖检查点字段、完成步骤、时间和逐项复制的步骤日志。
- 恢复时如历史检查点遗漏步骤日志，会从经过校验的 `completedSteps` 重建成功日志，确保下一稳定检查点继续保留合法前缀。

### 新增测试

- 已执行 SQL 的检查点恢复不再次执行 SQL。
- 非连续 `completedSteps` 返回 `RESUME_FAILED` 且 SQL 执行器调用次数为零。
- metadata 检查点重新加载结构化 metadata 并继续完成流程。
- 保存后修改原检查点、完成步骤、步骤日志或 `save` 返回对象不会污染已保存状态；修改读取结果同样不会污染后续读取。

### TDD 证据

RED：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -Dtest=AskDataResumeFlowTest,AskDataConversationStoreTest test
```

结果：11 项测试中 4 项按预期失败，分别暴露保存对象别名、保存后篡改、非连续步骤恢复执行和 metadata 恢复失败。

GREEN（聚焦）：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -Dtest=AskDataResumeFlowTest,AskDataConversationStoreTest test
```

结果：11 项测试通过，0 failures，0 errors。

GREEN（模块完整）：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter test
```

结果：54 项测试通过，0 failures，0 errors。`git diff --check` 无输出。

### 子 Agent 合并路径核查

- `SequentialAskDataAgentDispatcher.dispatch` 只执行并收集 `AskDataSubAgentResult`，不合并到 `DefaultAskDataFlowExecutor` 的结果或检查点流程。
- `DefaultAskDataFlowExecutor` 没有调用 `AskDataAgentDispatcher`、`AskDataSubAgent` 或 `AskDataDispatchResult`。因此当前 Task 7 实际执行路径不存在“子 Agent 合并后”可遗漏的检查点保存点，无需额外修改。

### 本次变更文件

- `fw-plus-ai/.../askdata/conversation/InMemoryAskDataConversationStore.java`
- `fw-plus-ai/.../askdata/flow/DefaultAskDataFlowExecutor.java`
- `fw-plus-ai/.../askdata/conversation/AskDataConversationStoreTest.java`
- `fw-plus-ai/.../askdata/flow/AskDataResumeFlowTest.java`
- `.superpowers/sdd/task-7-report.md`

### 风险信号

- 命中已有构建风险：`fw-plus-ai-boot-autoconfigure` 的 3 条重复 LangChain4j 依赖 Maven 警告，范围外未修改。
- 命中已有测试环境风险：SLF4J 未加载 `StaticLoggerBinder`，回退到 NOP logger；测试通过。
- 未命中产品约束风险：未引入 Spring AI、AgentScope、Java 17 LangGraph4j 主线依赖，验证命令使用 Java 8。

## Extra Review Fix（2026-07-24）

### 状态

DONE_WITH_CONCERNS

### 修复内容

- `DefaultAskDataFlowExecutor.isSafeCheckpoint` 对 `execute-sql` 检查点同时校验 `sqlExecuted` 与 `rowCount`：`sqlExecuted=false` 时必须没有行数，`sqlExecuted=true` 时必须有行数；不满足任一组合的恢复请求在执行 SQL 前返回 `RESUME_FAILED`。

### 新增测试

- 合法完整的 `execute-sql` 前缀中，`sqlExecuted=false` 但 `rowCount=3` 返回 `RESUME_FAILED`，且 SQL 执行器调用次数为零。

### TDD 证据

RED：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -Dtest=AskDataResumeFlowTest test
```

结果：10 项测试中 1 项按预期失败。新增的“不执行 SQL 却带行数”检查点被旧逻辑当作成功恢复，未返回 `RESUME_FAILED`。

GREEN（聚焦）：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -Dtest=AskDataResumeFlowTest test
```

结果：10 项测试通过，0 failures，0 errors。

GREEN（模块完整）：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter test
```

结果：55 项测试通过，0 failures，0 errors。

### 本次变更文件

- `fw-plus-ai/.../askdata/flow/DefaultAskDataFlowExecutor.java`
- `fw-plus-ai/.../askdata/flow/AskDataResumeFlowTest.java`
- `.superpowers/sdd/task-7-report.md`

### 风险信号

- 命中已有构建风险：`fw-plus-ai-boot-autoconfigure` 的 3 条重复 LangChain4j 依赖 Maven 警告，范围外未修改。
- 命中已有测试环境风险：SLF4J 未加载 `StaticLoggerBinder`，回退到 NOP logger；测试通过。
- 未命中产品约束风险：未引入 Spring AI、AgentScope、Java 17 LangGraph4j 主线依赖，验证命令使用 Java 8。
