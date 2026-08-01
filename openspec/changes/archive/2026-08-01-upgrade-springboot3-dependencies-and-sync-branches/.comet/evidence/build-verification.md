# 构建验证证据

## 固定环境与版本边界

- 执行日期：`2026-07-30`
- JDK：Azul Zulu `1.8.0_432`，`JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home`
- Maven：系统 Maven `3.6.3`，路径 `/Users/xiongxiao/Workspaces/Maven/apache-maven-3.6.3/bin/mvn`
- Maven Wrapper 不可用时按已批准方案使用系统 Maven；未创建、修复或升级 Wrapper。
- 版本断言全部命中：Spring Boot `2.7.18`、Spring Cloud `2021.0.9`、Spring Cloud Alibaba `2021.0.5.0`、LangChain4j `0.35.0`、Magic-API `2.2.2`、Magic-Script `1.9.0`。
- `langchain4j-core:0.35.0` 首个 class 的 8 字节头为 `cafe babe 0000 0034`，class major `52`，满足 Java 8。
- `fw-plus-ai-boot-autoconfigure/target/java8-starter-tree.txt` 为空，未解析 Java 17 的 `langchain4j-spring-boot-starter`。

## 分层构建结果

以下命令均在固定的 JDK 8 环境中执行，最终退出码均为 `0`。

| 层级 | 命令 | Reactor | 测试 | 结果 | 耗时 |
| --- | --- | ---: | ---: | --- | ---: |
| Core | `mvn -f fw-plus-core/pom.xml clean install` | 64/64 | 325 | `BUILD SUCCESS` | 03:01 |
| Boot | `mvn -f fw-plus-boot/pom.xml clean install` | 49/49 | 444 | `BUILD SUCCESS` | 05:24 |
| Cloud | `mvn -f fw-plus-cloud/pom.xml clean install` | 13/13 | 2 | `BUILD SUCCESS` | 28.798 s |
| AI | `mvn -f fw-plus-ai/pom.xml clean install` | 16/16 | 299 | `BUILD SUCCESS` | 04:20 |
| Sample | `mvn -f fw-plus-sample/pom.xml validate` | 15/15 | validate 不执行测试 | `BUILD SUCCESS` | 0.952 s |

所有已执行测试均为 `Failures: 0, Errors: 0, Skipped: 0`。完整执行日志保存在本机 `/tmp/task3-core-build.log`、`/tmp/task3-boot-build.log`、`/tmp/task3-cloud-build.log`、`/tmp/task3-ai-build-final.log` 和 `/tmp/task3-sample-validate.log`。

## AI 边界测试环境清理

首次 AI 构建被 `LegacyAiArtifactBoundaryTest` 阻断。调查确认测试枚举的 4 个旧模块目录均无 Git 跟踪文件，每个目录只剩一个被 `.gitignore` 忽略的 `.flattened-pom.xml`；提交 `bf00e522` 已删除对应生产模块与源码。将这些可恢复的构建残留移动到 `/tmp/task3-legacy-ai-stale-20260730/` 后，从 AI 顶层重新执行完整 `clean install`，边界测试和全部 16 个 reactor 通过。

该处理未删除或修改生产源码、POM、测试或已跟踪文件。构建产生的 3 个 tracked Cloud flatten 变化已精确恢复，Cloud/AI 控制台产生的 untracked `package-lock.json`、`dist/` 和 `node_modules/` 已移动到 `/tmp/task3-build-generated-20260730/`；证据修改前工作区恢复干净。

## 已知非阻断警告

- 系统 Maven 与用户 Maven `settings.xml` 第 213 行均含无法识别的 `repository` 标签。
- Core BOM 中 `fw-plus-ai-model` 与 `fw-plus-ai-store-embedding` 各有重复的 dependencyManagement 声明；Maven 警告未来版本可能拒绝该模型。
- 编译日志包含既有 deprecated/unchecked 提示；AI 测试 classpath 包含多个 SLF4J binding。
- Boot、Cloud、AI 的前端构建报告 npm audit 共 14 项风险（4 moderate、9 high、1 critical）和 allow-scripts 待审核项；Vite 构建本身成功。
- 部分负向测试会按预期打印 Spring 上下文 WARN 或控制器 ERROR，Surefire 汇总仍为零失败、零错误。

上述警告未被本任务掩盖，也未在 Task 3 中修改源码或依赖配置。

## Task 8：Spring Boot 3 依赖基线 RED/GREEN 证据

- 执行日期：`2026-07-31`；JDK：Zulu `17`，`JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home`；Maven：系统 Maven `3.6.3`。
- 历史 BOM 基线：`29295a5c5926c7b7ab39590ae886319d7554c42d`；方案 A 结构提交：`0c44f8938d5a784876124b32670bc981554a574f`；当前验证 HEAD：`afa7a50f1f9062894c2aa14231c707a34d9722bd`。
- RED 在 `mktemp -d` 创建的 detached worktree 中执行：以当前 `afa7a50f` 保留已适配 LangChain4j API 的可编译源码，仅通过 `apply_patch` 将 Core 与 AI BOM 还原为 `29295a5c` 的 Task 8 契约状态。该隔离树未改动真实 `springboot3-upgrade`。

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml \
  -Dtest=SpringBoot3DependencyBaselineTest test
```

- RED 摘要：完整 main/test compile 后 Surefire 实际运行 `SpringBoot3DependencyBaselineTest` 1 项，`Failures: 1, Errors: 0, Skipped: 0`，进程退出码 `1`；预期断言为 `langchain4j.version ==> expected: <1.18.0> but was: <1.18.1>`。
- 历史 XML 核对：`29295a5c` 的 AI BOM 仍直接导入 `langchain4j-bom` 并直接声明 Redis、Qianfan、DashScope `0.36.2`，而方案 A 要求这些管理只留在 Core BOM、AI BOM import Core BOM。Core BOM 本身已有 BOM 与三项例外；RED 因首个 `1.18.0` 版本断言提前终止，结构差异由同一历史 POM 的静态核对确认。
- GREEN 在真实 `.worktrees/springboot3-upgrade` 的 `afa7a50f` 以完全相同命令执行：Surefire 运行 1 项，`Failures: 0, Errors: 0, Skipped: 0`，Maven `BUILD SUCCESS`，退出码 `0`。接管复核于 `2026-07-31T19:30:57+08:00` 再次执行该命令，结果仍为 1 项测试零失败、零错误、零跳过，`BUILD SUCCESS`，退出码 `0`；仅出现已记录的 Maven `settings.xml` 第 213 行 `repository` 标签警告。
- Fix round 1/1 清理：先用 `git worktree list --porcelain`、逐路径 `git status --short` 与 `git rev-parse HEAD` 核对 Task 8 自建的两个 detached worktree。`current-with-old-boms` 位于 `/private/var/folders/p7/cg70fqfj1xz7rwplv4n19d_w0000gp/T/fw-plus-task8-baseline-current.Tmr05HO9qB/current-with-old-boms`，HEAD 为 `afa7a50f1f9062894c2aa14231c707a34d9722bd`，dirty 内容仅为历史 RED 的 Core/AI BOM 支架；`old-baseline` 位于 `/private/var/folders/p7/cg70fqfj1xz7rwplv4n19d_w0000gp/T/fw-plus-task8-baseline.5Po0PhTMO2/old-baseline`，HEAD 为 `29295a5c5926c7b7ab39590ae886319d7554c42d`，dirty 内容仅为 Task 8 基线测试及编译筛选支架。两者均按精确路径执行 `git worktree remove --force`，退出码均为 `0`。
- 清理后复核：上述两个精确路径均不再出现在 `git worktree list --porcelain`，且文件系统检查均为 `missing`；未运行 `git worktree prune`，三个 `/private/tmp/fw-plus-task8-*` prunable 条目及其他 worktree 均未触碰。真实 `.worktrees/springboot3-upgrade` 仍为空状态，HEAD 保持 `afa7a50f1f9062894c2aa14231c707a34d9722bd`。

## springboot2 发布前复验

- 环境保持 Zulu JDK `1.8.0_432` 与系统 Maven `3.6.3`；固定版本断言再次命中 Boot `2.7.18`、Cloud `2021.0.9`、Alibaba `2021.0.5.0`、LangChain4j `0.35.0`，Spring AI 与 Spring AI Alibaba 均为 `1.0.1`。
- 分层构建在 springboot2 同步后的 worktree 中完成：Core `64/64`、Boot `49/49`、Cloud `13/13`、AI `16/16` 均为 `BUILD SUCCESS`；Sample `15/15` 的 `validate` 为 `BUILD SUCCESS`。所有已执行测试的汇总为零失败、零错误、零跳过。
- `fw-plus-ai-boot-autoconfigure` 的 `dependency:tree` 退出码 `0`；`langchain4j-core` 解析为 `0.35.0`，`langchain4j-spring-boot-starter` 计数为 `0`，维持 Java 8 依赖边界。
- 构建清理后恢复了被 Maven 修改或删除的 tracked `.flattened-pom.xml`；只删除本轮新生成的 AI model console `dist/`、`node_modules/`、`package-lock.json` 及 Cloud eureka console `package-lock.json`。无法归因的其它 ignored 前端文件未触碰，最终 springboot2 worktree 干净。

## springboot3 发布前复验

- 环境使用 Zulu JDK `17` 与系统 Maven `3.6.3`；固定版本断言命中 Boot `3.5.8`、Cloud `2025.0.0`、Alibaba `2025.0.0.0`、LangChain4j 主版本 `1.18.0`。
- Core BOM 管理 `langchain4j-bom:${langchain4j.version}`，并单独固定 `langchain4j-redis`、`langchain4j-qianfan`、`langchain4j-dashscope` 为 `0.36.2`；AI BOM 仅 import Core BOM，不再重复管理 `dev.langchain4j` 坐标。
- Spring AI 与 Spring AI Alibaba 均保持 `1.0.1`；Magic-API / Magic-Script 固定版本未作为本轮变更点修改。
- 分层构建完成：Core `64/64`、Boot `49/49`、AI `17/17`、Cloud `13/13` 均为 `BUILD SUCCESS`；`git diff --check` 无输出，退出码 `0`。
- AI 构建中修复 LangChain4j 1.18.x API 迁移点：Vertex AI Gemini 包名、EmbeddingStore `search(...)`、Weaviate `metadataFieldName`、测试桩和 Spring 6 `ResponseStatusException#getStatusCode()`。
- 构建清理后恢复 Maven 生成的 tracked `.flattened-pom.xml`，并删除本轮新生成的 untracked 前端 `dist/`、`node_modules/`、`package-lock.json`，最终 `springboot3` worktree 干净且与 `origin/springboot3` 同步。

## Task 11：Spring Boot 3 最终全层与 Sample 证据

- 最终验证提交：`d3f1ad3110c525bbd76db41ac80454b778b8c5fb`；从发布前远端基线 `afa7a50f1f9062894c2aa14231c707a34d9722bd` 的提交链包含 `c26099f2`（Vertex AI Gemini 断言）、`dfb6eb35`（Task 9 的 Boot 3 兼容依赖）、`800ea7df`（Task 10 的 Feign 平台兼容）、`93bb0e02`（MyBatis-Spring 与 AI 边界）和 `d3f1ad31`（AI starter 契约）。
- 环境：Zulu OpenJDK `17.0.13`，`JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home`；系统 Maven `3.6.3`。未使用 Wrapper。
- 已有且与最终提交内容对应的 Task 11 重型命令均退出 0：Core `mvn -nsu -f fw-plus-core/pom.xml clean install` 为 `64/64`；Boot 同命令为 `49/49`，Surefire `444` tests 零 failure/error/skipped；Cloud 为 `14/14`，Surefire `1` test 零 failure/error/skipped；AI 为 `17/17`，autoconfigure `332` tests 和修复定向 `3/3`、`13/13` 均零 failure/error。有效 POM/AI 依赖树确认 Boot `3.5.8`、LangChain4j `1.18.0`、三项 `0.36.2` 覆盖、Spring AI 与 Spring AI Alibaba `1.0.1`；Enforcer `3.4.1` 的 `dependencyConvergence` 和 `banDuplicatePomDependencyVersions` 均通过 17/17 reactor。
- 本轮补齐且新鲜执行：`mvn -nsu -f .worktrees/springboot3-upgrade/fw-plus-sample/pom.xml validate` 退出 0，15/15 reactor `SUCCESS`，`BUILD SUCCESS`，耗时 `1.119 s`；`validate` 不执行测试。`-nsu` 只禁止远程 SNAPSHOT 元数据更新，不跳过解析或验证；命令首次成功，无重试。
- 前后复核：`git -C .worktrees/springboot3-upgrade status --short --branch` 为空，`git -C .worktrees/springboot3-upgrade diff --check` 无输出、退出 0。未改动 Spring Boot 3 源码、测试或 POM。
- 已知非阻断风险：系统和用户 Maven settings.xml 的未知 `<repository>` 标签警告；此前测试的既有 SLF4J/Commons Logging、多种负向上下文日志及前端审计告警。它们均未使上述命令失败。

## Task 11：审查修复与最终发布校正

- 审查修复提交：`bca53b69bba9ddc8f33ec848795fe1cc6c58e989`（`test(ai): 收紧中立边界契约`）。独立 scoped re-review 已确认两个 Important 均解决，AI `17/17` 为 GREEN。
- 发布前在 `.worktrees/springboot3-upgrade` 完成工作树空状态和 `git diff --check` 核验；以显式 refspec 刷新 `refs/remotes/origin/springboot3` 后，`d3f1ad31` 是 `bca53b69` 的祖先。
- 普通非强制 push 成功。独立 `git ls-remote`、本地 HEAD 与 tracking `origin/springboot3` 均为 `bca53b69bba9ddc8f33ec848795fe1cc6c58e989`，`git rev-list --left-right --count springboot3...origin/springboot3` 为 `0 0`。

## Task 12：派生分支同步后的定向复验

### springboot2 / Java 8

- `java -version`：Zulu OpenJDK `1.8.0_432`（Zulu 8.82.0.21-CA）。两个 worktree 的 Maven 命令严格串行执行。
- AskData Starter 依赖测试：`AskDataStarterDependencyTest` 1/1，通过。
- AskData Starter `validate -DskipTests`：`BUILD SUCCESS`。
- 受影响自动配置：`AskDataAutoConfigurationTest` 19/19、`NeutralAiAutoConfigurationBoundaryTest` 2/2，共 21/21，通过。

### springboot3 / Java 17

- `java -version`：Zulu OpenJDK `17.0.13`（Zulu17.54+21-CA）。所有 Maven 命令在 springboot2 完成后严格串行执行。
- AskData Starter 依赖测试：`AskDataStarterDependencyTest` 1/1，通过。
- AskData Starter `validate -DskipTests`：`BUILD SUCCESS`。
- 受影响自动配置：`AskDataAutoConfigurationTest` 19/19、`NeutralAiAutoConfigurationBoundaryTest` 3/3、`SpringBoot3DependencyBaselineTest` 1/1，共 23/23，通过；结果由最新 Surefire XML 的 `tests/failures/errors/skipped` 属性复核。
- 实际 `fw-plus-ai-boot-autoconfigure/pom.xml` 下的 API 适配测试：`LangChain4jAutoConfigurationRegistrationTest` 7/7、`LangChain4jAdapterAutoConfigurationTest` 6/6，共 13/13，`BUILD SUCCESS`。

### 非阻断风险

- 系统级和用户级 Maven `settings.xml` 第 213 行仍有无法识别的 `repository` 标签警告。
- 两个分支均提示部分 `exec-maven-plugin` 消费模块缺少可见版本；这些文件来自已发布 dev 历史，本轮未扩大范围修改。
- 测试 classpath 仍报告多个 SLF4J provider；Spring Boot 3 LangChain4j 测试另有 Commons Logging 与 `spring-jcl` 并存提示。所有上述测试汇总仍为零失败、零错误、零跳过。

## Task 13：构建证据复用边界

- Task 12 已在当前生产树上串行完成 Java 8 / Java 17 定向验证：springboot2 的 AskData Starter 1/1、starter validate、AskData 19/19、Neutral 2/2；springboot3 的 AskData Starter 1/1、starter validate、AskData 19/19、Neutral 3/3、基线 1/1、LangChain4j API 适配 13/13，全部通过。
- Task 13 重新执行三组生产树差异断言：`9837ccbe..a25ab807`、`405c1ddb..34de9413`、`02d6addd..5be07b08` 在所有 `src/main`、`src/test` 和 `pom.xml` 路径上均无差异；每组仅修改计划、OpenSpec tasks 和 subagent progress 文档。
- 因当前生产源码、测试和 POM 与 Task 12 已验证树相同，本任务按计划不并行或重复运行 Maven。build guard 的 RED 仅为 OpenSpec 6.1-6.4、Plan Task 13 四步未勾，以及尚未记录 build check，不是构建失败。
