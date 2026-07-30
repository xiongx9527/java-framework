# Task 1 实施报告：模块注册与依赖边界

## 状态

`DONE_WITH_CONCERNS`

提交：`495a10bb feat(ai): add tiny engine ai starter module`

## 实现内容

- 新增 `fw-plus-ai-tiny-engine-spring-boot-starter`，直接依赖 AI 自动配置、`fw-plus-tiny-engine-core`、中立模型 starter、中立向量 store starter 与统一 LangChain4j adapter starter。
- 将新 starter 注册到 AI Boot starter 聚合 POM，并在 AI Boot BOM 中按 `${project.version}` 管理。
- 新增 TinyEngine 依赖边界测试，确认中立 starter 和统一 adapter 存在，旧 LangChain4j model/store starter 不存在。
- 扩展 AskData 依赖边界测试，确认旧 starter 不存在，且不直接要求 Spring AI、Spring AI Alibaba 或 AgentScope2 的 groupId。

## RED

按任务规定先尝试：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home ./fw-plus-core/mvnw -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter/pom.xml test -Dtest=AskDataStarterDependencyTest
```

结果：未进入 Maven，失败原因为 worktree 缺少根 `.mvn/wrapper/maven-wrapper.properties` 和 `maven-wrapper.jar`。

使用系统 Maven 运行等价 AskData 命令后，`AskDataStarterDependencyTest` 通过（1 run, 0 failures）。随后在尚未创建 TinyEngine POM 时运行等价命令：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-spring-boot-starter/pom.xml test -Dtest=TinyEngineAiStarterDependencyTest
```

结果：按预期失败，`POM file .../fw-plus-ai-tiny-engine-spring-boot-starter/pom.xml ... does not exist`。

## GREEN

按任务规定先尝试：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home ./fw-plus-core/mvnw -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/pom.xml -pl fw-plus-ai-tiny-engine-spring-boot-starter,fw-plus-ai-askdata-spring-boot-starter test
```

结果：同样因缺少根 `.mvn/wrapper` 文件而无法启动。

系统 Maven 等价最终验证命令：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/pom.xml -pl fw-plus-ai-tiny-engine-spring-boot-starter,fw-plus-ai-askdata-spring-boot-starter test
```

结果：`BUILD SUCCESS`；AskData 与 TinyEngine 各执行 1 个测试，均为 0 failures、0 errors。未使用 `-am`，所需快照依赖已在本地 Maven 仓库可解析。

## 变更文件

- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-spring-boot-starter/pom.xml`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/tinyengine/starter/TinyEngineAiStarterDependencyTest.java`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/pom.xml`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/askdata/starter/AskDataStarterDependencyTest.java`

## 自审结果

- `git diff --check` 通过。
- 已检查新增源码，没有 Java 9+ API。
- 新 starter 不包含旧 LangChain4j model/store starter，也没有 Spring AI、Spring AI Alibaba 或 AgentScope2 的必需依赖。
- 提交仅包含上述 5 个授权文件，未暂存或提交 OpenSpec、Docs 或 Comet 现有脏改。
- 单任务代码 diff 为 138 行，未超过 200 行。

## 风险信号

- 跨模块/跨子系统协调改动：是。涉及 Core 的 TinyEngine core artifact 与 AI Boot 的 starter 聚合、BOM 和 AskData 依赖边界。
- 公共 API 契约或对外接口变更：是。新增公开 Maven starter 坐标。
- 安全敏感面：否。
- 并发/共享状态：否。
- 数据/schema 迁移：否。
- `DONE_WITH_CONCERNS`：是。
- 单任务 diff 超过 200 行：否。

## 顾虑

- 规定的 Maven Wrapper 命令无法启动，因为当前 worktree 和父工作区均缺少根 `.mvn/wrapper` 文件；已使用相同 JDK 的系统 Maven 完成等价 GREEN 验证。
- Maven 输出显示本地 `fw-plus-dependencies:0.1.1-SNAPSHOT` 已存在 `fw-plus-ai-model` 与 `fw-plus-ai-store-embedding` 的重复 dependencyManagement 警告；本任务未修改该 BOM，测试仍通过。

---

## Task 1 review fix：完整 Maven 坐标依赖边界

## 修复内容

- TinyEngine 与 AskData starter 依赖测试现在均解析直接依赖的 `groupId:artifactId`，而不是仅解析 `artifactId`。
- 两份测试的正向依赖与旧 LangChain4j starter 禁止项均使用完整 Maven 坐标断言，避免同名 artifact 来自错误 groupId 时被误判为正确。

## RED

先仅把测试期望改为完整坐标、保留旧的 artifactId 解析器，运行：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/pom.xml -pl fw-plus-ai-tiny-engine-spring-boot-starter,fw-plus-ai-askdata-spring-boot-starter test -Dtest=TinyEngineAiStarterDependencyTest,AskDataStarterDependencyTest
```

结果：预期 `BUILD FAILURE`；`AskDataStarterDependencyTest` 在 `AskData starter must preserve the AskData Java 8 module` 断言失败，证明裸 artifactId 解析不能满足完整坐标断言。

为实际执行 TinyEngine 新增边界断言，临时将其 `fw-plus-ai-model-spring-boot-starter` 依赖替换为可解析但错误的 `junit:junit:4.13.2`，运行：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/pom.xml -pl fw-plus-ai-tiny-engine-spring-boot-starter test -Dtest=TinyEngineAiStarterDependencyTest
```

结果：预期 `BUILD FAILURE`；测试在 `TinyEngine starter must include the neutral AI model starter` 断言失败。随后立即恢复正确的 `com.xiongx9527.fw.plus.ai:fw-plus-ai-model-spring-boot-starter` POM 坐标，错误依赖未保留。

## GREEN

实现 `groupId:artifactId` 解析并恢复正确 POM 后，使用系统 Maven（worktree 中 Maven Wrapper 仍因缺少根 `.mvn/wrapper` 文件不可用）运行：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/pom.xml -pl fw-plus-ai-tiny-engine-spring-boot-starter,fw-plus-ai-askdata-spring-boot-starter test
```

结果：`BUILD SUCCESS`；AskData 与 TinyEngine 各执行 1 个测试，均为 0 failures、0 errors。

## 变更文件

- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/tinyengine/starter/TinyEngineAiStarterDependencyTest.java`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/askdata/starter/AskDataStarterDependencyTest.java`
- `.superpowers/sdd/task-1-report.md`

## 自审

- 两份测试均对正向与旧 starter 负向依赖使用完整 `groupId:artifactId` 坐标。
- 已核对 TinyEngine starter POM 相对 `495a10bb` 无差异，临时错误依赖已恢复。
- 提交前仅暂存上述两份测试与本报告，不暂存 `openspec/**`、`docs/superpowers/**` 或 `.comet/**`。

## 风险信号

- Maven 继续报告既有 `fw-plus-dependencies` 中重复 dependencyManagement 警告；本次未触及该 BOM，指定测试通过。
- Maven Wrapper 在该 worktree 不可用，最终验证使用指定 JDK 的系统 Maven。
