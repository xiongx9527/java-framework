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
| Boot | `mvn -f fw-plus-boot/pom.xml clean install` | 49/49 | 447 | `BUILD SUCCESS` | 05:24 |
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
- Boot、Cloud、AI 的前端构建报告 npm audit 中 moderate/high 漏洞和 allow-scripts 待审核项；Vite 构建本身成功。
- 部分负向测试会按预期打印 Spring 上下文 WARN 或控制器 ERROR，Surefire 汇总仍为零失败、零错误。

上述警告未被本任务掩盖，也未在 Task 3 中修改源码或依赖配置。
