# Task 1 报告：依赖与构建基线

## 状态

DONE_WITH_CONCERNS

## 基线

- Maven：`3.8.8`
- 实际运行 JDK：`24.0.2`（Azul Zulu）
- Core 声明基线：Java 8 / Spring Boot `2.7.18`
- 本任务没有将 Spring Boot 3、Spring AI、Spring AI Alibaba 或 AgentScope 2 Java 依赖、版本写入 Java 8 Core BOM。

## RED：变更前探针

1. `./fw-plus-core/mvnw -f fw-plus-core/pom.xml help:effective-pom -Doutput=/tmp/fw-plus-core-effective-pom.xml`
   - 退出码 `0`；写入 `/tmp/fw-plus-core-effective-pom.xml`。
2. `./fw-plus-core/mvnw -f fw-plus-ai/fw-plus-ai-project/pom.xml help:effective-pom -Doutput=/tmp/fw-plus-ai-effective-pom.xml`
   - 退出码 `0`；写入 `/tmp/fw-plus-ai-effective-pom.xml`。
3. `./fw-plus-core/mvnw -f fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml dependency:tree -Dincludes=com.xiongx9527.fw.plus:fw-plus-ai-model,com.xiongx9527.fw.plus:fw-plus-ai-store-embedding -Dverbose`
   - 退出码 `0`，但没有输出这两个中立 artifact；结合变更前 effective POM 搜索，确认 Core dependencyManagement 尚未管理它们。

## GREEN：变更后验证

1. `./fw-plus-core/mvnw -f fw-plus-core/pom.xml validate`
   - 退出码 `0`，`BUILD SUCCESS`，57 个 Core reactor 项目均通过 validate。
2. `./fw-plus-core/mvnw -f fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml help:effective-pom -Doutput=/tmp/fw-plus-dependencies-effective-pom.xml`
   - 退出码 `0`；effective POM 含 `fw-plus-ai-model` 和 `fw-plus-ai-store-embedding`，版本均为 `0.1.1-SNAPSHOT`。
3. `! rg -ni 'spring-ai|agentscope|spring-boot-dependencies.*3\\.' fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`
   - 退出码 `0`；确认 Java 8 Core BOM 未加入目标 Java 17 SDK 或 Boot 3 版本。
4. `git diff --check`
   - 退出码 `0`。

## 变更

- `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`：以 `${project.version}` 管理两个 Java 8 中立 artifact。
- `docs/ai-framework-migration/compatibility-matrix.md`：记录 Java 8 / Java 17 线路、隔离规则、当前不可执行范围及后续验证目标。

## 提交

- `54c62463c892f5bfda92531cfd890a1fbf0e3301` — `fw-plus-ai: add abstraction dependency baseline`

## 风险信号与交接

- `./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-model -am install -DskipTests` 当前按预期失败：`Could not find the selected project in the reactor`。该模块由 Task 2 创建后，必须重新执行此命令并安装产物，供 Java 17 独立 reactor 解析。
- `fw-plus-ai-j17` 尚未创建，因此 Spring AI、Spring AI Alibaba、AgentScope 2 Java starter 目前不可执行；矩阵没有宣称它们已编译、可装配或具备真实 adapter。
- 有限风险：验证命令实际运行于 JDK 24，未替代后续 Java 8 target 与 Java 17 target 的独立编译验证。

## Self-review

- 仅提交允许范围内的两个文件；未修改 AI Boot BOM、源代码、OpenSpec、计划或进度文件。
- 两个新增坐标位于现有 Core 内部 artifact dependencyManagement 区域，使用既有 `${project.version}` 约定。
- 矩阵明确隔离 Java 17 依赖，且没有将后续骨架/真实适配功能表述为已交付。

## 审查修复补充

### RED

- 文档断言预期失败：矩阵未包含“计划目标 + Task 6 Java 17 独立 reactor SDK 依赖树与编译探针”、Core effective POM/`rg` 检查，以及 Task 2/Task 3 创建中立模块前不得 `install` 的边界。

### GREEN

- 文档断言通过：三类 Java 17 SDK 均标均标注为计划目标，且明确由 Task 6 进行 Java 17 独立 reactor 依赖树与编译探针。
- `./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-dependencies validate`：退出码 `0`，`BUILD SUCCESS`。
- `git diff --check`：退出码 `0`。

### 风险信号

- 当前尚无 `fw-plus-ai-j17` reactor 或真实 SDK 依赖树；Java 17+/Spring Boot 3+ 仅为计划目标，不能视为本任务已验证的 SDK 兼容结论。
- Maven 在 JDK 24 下输出 Jansi/Unsafe 弃用警告，不影响本次 `validate` 成功，但不替代 Task 6 的 Java 17 编译探针。

### 提交

- `6601c356295fb7dc6781c30fb10cc13c7c471430` — `docs: clarify Java 17 AI SDK validation`
