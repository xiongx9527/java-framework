# Task 4 报告：Tinyflow 执行服务与状态边界

## 结果

- 新增框架无关的 `TinyflowService` 门面，支持工作流注册、删除、同步/异步执行及同步/异步恢复。
- 新增不可变执行请求和异步结果边界；变量在进入 Tinyflow 前复制，异步调用返回稳定实例 ID。
- 新增进程内 `ChainDefinitionRepository`，注册前先完成工作流校验，非法替换不会覆盖已有工作流。
- 同步调用使用构造器提供的正 `Duration`，调用 Tinyflow 原生有限超时重载；超时和中断转换为不含变量内容的 `TinyflowException`。
- 挂起通过 `ChainSuspendException.getChainStateId()` 转换为 `TinyflowExecutionResult(suspended=true)`；恢复保持同一实例 ID。
- 返回结果先复制，再通过 `ChainExecutor.clearDefaultStates` 移除 Tinyflow 内部状态键，不暴露 repository/state Map。

## 真实 Tinyflow 2.0.7 API 核对

使用本机 `tinyflow-core-2.0.7.jar` 的 `javap` 和 Maven Central source JAR 核对，未采用计划伪代码：

- `ChainExecutor.execute(String, Map, Map, long, TimeUnit)` 返回 `Map<String,Object>`。
- `ChainExecutor.executeAsync(String, Map)` 返回 `String instanceId`。
- `ChainExecutor.resume(String, Map, Map, long, TimeUnit)` 返回 `Map<String,Object>`。
- `ChainExecutor.resumeAsync(String, Map)` 返回 `void`，门面返回传入的同一实例 ID。
- `ChainSuspendException` 提供 `getChainStateId()` 和 `getSuspendParameters()`。
- `ChainDefinitionRepository` 只有 `getChainDefinitionById(String)`，因此新增仓库用显式 `register/remove` 管理定义。
- `ChainStateRepository` 提供 `load/tryUpdate/getLock`；默认 `InMemoryChainStateRepository` 使用静态进程内 Map，且 `load` 对未知 ID 创建空状态。门面通过空 `chainDefinitionId` 拒绝未知恢复实例，不承诺跨进程恢复。
- `ChainExecutor.clearDefaultStates` 删除 `SCHEDULE_NEXT_NODE_DISABLED_KEY`、`NODE_STATE_STATUS_KEY`、`CHAIN_STATE_STATUS_KEY`、`CHAIN_STATE_MESSAGE_KEY`。

## TDD 证据

### RED

命令：

```bash
mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core -Dtest=DefaultTinyflowServiceTest test
```

结果：退出码 `1`，`testCompile` 明确报告缺少 `InMemoryChainDefinitionRepository` 和 `TinyflowService`。失败原因是执行门面尚不存在，不是测试语法或环境错误。

### 首轮 GREEN 调试证据

最小实现后的首次同命令结果为 `Tests run: 8, Failures: 3`：

1. 两个模板输出断言实际得到 `Hello `。对照 Tinyflow `TemplateNode.execute` 和 `ChainState.resolveParameters` 后确认：模板只接收节点显式声明的 `parameters`。测试工作流补充 `name`、`refType=input` 后，保留原始结果断言，focused 命令结果 `Tests run: 2, Failures: 0`。
2. 预先中断测试收到 generic failure。对照 `LocalChainLock` 源码确认：`ChainExecutor` 在进入 `future.get` 前先调用可中断 `tryLock`，它捕获 `InterruptedException`、恢复中断标记，但只返回未获取锁。同步门面增加入口中断检查后，未修改原失败测试，focused 命令结果 `Tests run: 1, Failures: 0`。

### 最终 GREEN

命令：

```bash
mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core -Dtest=DefaultTinyflowServiceTest test
```

结果：退出码 `0`，`Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`。覆盖真实 start -> template -> end、异步实例 ID、同步挂起/恢复、异步恢复、超时、中断标记、缺失/删除工作流、非法替换和内部状态键清理。

## 完整验证

Tinyflow Core 模块全测试：

```bash
mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core test
```

结果：退出码 `0`，`Tests run: 31, Failures: 0, Errors: 0, Skipped: 0`。

整个 Core reactor：

```bash
mvn -f fw-plus-core/pom.xml test
```

结果：退出码 `1`。Tinyflow Core 及此前模块均为 `SUCCESS`，在无关的 `fw-plus-nebula-jdbc` 停止：`NebulaSqlParserMatrixTest` 依赖仓库根的 `openspec/` 目录，但当前工作区不存在该目录，报 `Cannot locate repository root`；该模块 `Tests run: 244, Failures: 0, Errors: 1, Skipped: 2`，后三个模块因 fail-fast 跳过。该测试与 Task 4 文件无调用或依赖关系，且不在允许修改范围。

为补齐被跳过的 reactor 尾部，运行：

```bash
mvn -f fw-plus-core/pom.xml -rf :fw-plus-nebula-jdbc '-Dtest=*,!NebulaSqlParserMatrixTest' -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：退出码 `0`；Nebula 其余 `243` 个测试（另跳过 live 测试 `2` 个）、Neo4j `27` 个测试（跳过 live 测试 `3` 个）、Tiny Engine `71` 个测试均无失败或错误，四个尾部 reactor 模块全部 `SUCCESS`。

## 自审

- 只新增 brief 指定的五个生产文件、一个测试文件及本报告；未修改 POM、其他模块、`.comet`、`docs/comet`、`.gitignore` 或 README/资产并发改动。
- 生产代码仅依赖 Java 8 标准库、现有 Tinyflow Core 和现有 Core 契约；未引入 Spring、Servlet、Magic-API 或 Java 9+ API。
- 测试使用真实 `ChainParser`、`ChainExecutor`、默认节点和状态仓库；仅用测试内阻塞节点控制超时/中断，不依赖模型或外部服务，也不对 mock 行为做断言。
- 同步成功调用的上游 `execute` 不暴露实例 ID，因此完成结果的 `instanceId` 为 `null`；异步执行、挂起同步执行和所有恢复结果均提供实例 ID。这保持现有 `TinyflowExecutionResult` 契约，不修改 brief 未列出的既有类型。
