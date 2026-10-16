---
change: java21-springboot4-upgrade
design-doc: docs/openspec/changes/java21-springboot4-upgrade/design.md
base-ref: 64a27a973763d8ed202b47dbfb671f3210063208
---

<!-- comet-task-authority: docs/openspec/changes/java21-springboot4-upgrade/tasks.md -->

# 实施计划：Java 21 与 Spring Boot 4.1 基线升级

## 工作区与执行前提

- 工作分支 `springboot4`；当前工作树未提交内容仅为本 change 的 OpenSpec / Comet 产物（`docs/openspec/changes/java21-springboot4-upgrade/`）。base-ref = `64a27a973763d8ed202b47dbfb671f3210063208`。
- 构建必须使用 JDK 21：每个构建 shell 先 `export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home` 且 `PATH` 含其 `bin`（当前交互 shell 默认 JDK 8），再执行 `./mvnw`。
- 根目录 wrapper 构建全仓；`fw-plus-sample` 无独立 wrapper，用根 `./mvnw -f fw-plus-sample/pom.xml ...`。
- boot / cloud / ai 三层默认 `maven.test.skip=true`，跑测试必须 `-Dmaven.test.skip=false`；Core 默认执行测试。
- Magic-API（`org.ssssssss`）依赖经内网 Nexus（jugui-snapshot / jugui-release）解析；不可达时记录并推迟对应验证，不改依赖定义。
- 不改分支、不 push；提交按层推进（见「执行顺序与提交策略」）。

## 执行策略与证据规则

- 已确认配置：`build_mode=autonomous`、`tdd_mode=tdd`、`review_mode=standard`、`isolation=current`。
- 分层闭环：Core → Boot → Cloud / AI → Sample → 根聚合收口；每层构建门禁通过后才进入下一层（Cloud 与 AI 层间无依赖，可交替）。
- RED/GREEN（tdd）：实现/适配任务在修复前运行其验收命令（或前置构建）并保存真实失败输出为 RED（失败原因必须是对应破坏点或回归），适配后同一命令通过为 GREEN；声明/文档类任务无待实现行为时如实标注「无 RED（声明型任务）」，以验收命令与产物为准。不得回退代码伪造 RED；历史证据缺失时如实记录缺口。
- 任务级审查（standard）：独立 reviewer 子代理审查高风险任务的实际 diff 与证据（规格符合性 + 代码质量），修复后复查上限 1 轮；CRITICAL/IMPORTANT 必须解决。最终集成审查只在 Verify 做一次，Build 不重复。
- 验收与勾选：核对实际改动与证据后，用 `comet state task-complete java21-springboot4-upgrade <task-id> --expect <revision> --json` 逐 ID 勾选；协调与决策记录用 `comet state checkpoint` 保存。
- 证据录制：完整构建/测试跑绿后以 `comet check run java21-springboot4-upgrade build --local -- <命令>` 录制（cwd 为项目根）；最后 `comet guard java21-springboot4-upgrade build --apply`。
- 版本口径补充（用户确认，2026-10-16）：除 D1 矩阵外，「其余组件」版本也可在 Boot 4.1 兼容线内就地升级（含各层 BOM 中既有显式钉版）。执行方式：先修完当前层编译基线，再对该层 BOM 其余钉版做一轮兜底复核与升级，重建回归验证；所有升级逐条记入 `docs/springboot4-upgrade-notes.md`。Magic-API 2.2.2 与仓内 EasyPoi fork 版本号仍不动。

## 实施任务

### 1. Core 层：JDK 21 与全局版本矩阵

#### 1.1 JDK 21 编译基线

<!-- comet-task-ref:c6a5871d-a930-4de5-aec9-69e72904e095 -->

- 范围：`fw-plus-core/pom.xml` 的 `jdk.version` 17→21；删除 `fw-plus-auth-core` 的 `maven.compiler.source/target=1.8` 遗留。
- 依赖：无；约束：只动编译目标声明，不动依赖矩阵；声明型任务（无自然 RED 时如实标注）。
- 验收：`rg "jdk\.version|1\.8" fw-plus-core -g pom.xml` 无 17 / 1.8 残留声明；`cd fw-plus-core && ./mvnw clean install -DskipTests`（JDK 21）成功。

#### 1.2 全局版本矩阵升级（Core BOM）

<!-- comet-task-ref:d0b9710c-a1e0-41d4-8d5d-0476db7c385e -->

- 范围：`fw-plus-dependencies` 按 design D1 升级——spring-boot-dependencies → 4.1.x 最新补丁、spring-cloud-dependencies → 2025.1.3、alibaba → 2025.1.0.0、Spring AI → 2.0 线、LangChain4j → 最新稳定线、MyBatis-Plus 3.5.17（boot4 坐标）、MyBatis-Flex 1.11.8（boot4 坐标 + jdbc starter）、Sa-Token / Druid boot4 坐标、dynamic-datasource 4.6.0、knife4j-next 5.7.8 + springdoc 3.0.3、easy-trans 4.0.2；Magic-API / EasyPoi 版本号不动；Tencent 行留待任务 3.2 处置；核验 Boot 管理 Tomcat 版本与 CVE 修复面，必要时钉 `tomcat.version` 并记录。
- 依赖：1.1；约束：坐标切换仅替换 groupId/artifactId，不新增无关依赖；每行版本在升级说明中可追溯。
- 验收：`./mvnw help:effective-pom`（抽查上述版本生效）与 `cd fw-plus-core && ./mvnw dependency:resolve` 通过。
- 注意：BOM 升级后 Core 构建必然暴露兼容问题，失败输出作为 1.3 的 RED 证据保存。

#### 1.3 Core 层源码适配

<!-- comet-task-ref:5516b38f-c873-469f-8f9f-c4479b80d01f -->

- 范围：适配 Core 源码至 Boot 4 / Framework 7 / Jackson 3 / JSpecify（compile error 驱动，逐点记录，含加密、认证、JDBC、图数据库等模块）。
- 依赖：1.2；约束：只改构建/测试暴露的适配点；每处记录到升级说明候选条目。
- 验收：`cd fw-plus-core && ./mvnw clean install -DskipTests` 成功（RED = 1.2 后的构建失败输出，GREEN = 本任务通过）。

#### 1.4 Core 层测试回归

<!-- comet-task-ref:657fbdf1-8150-4a73-91f0-727b6254f5c0 -->

- 范围：运行 Core 测试并修复回归（Jackson 3 序列化、JSpecify、JUnit 6 等）。
- 依赖：1.3；验收：`cd fw-plus-core && ./mvnw clean test` 全绿；每处回归修复记录 RED/GREEN。

#### 1.5 升级说明创建（Core 节）

<!-- comet-task-ref:81b080a8-966c-4096-9c5e-512a33babaa8 -->

- 范围：创建 `docs/springboot4-upgrade-notes.md`（对齐 `springboot3-upgrade-notes.md` 五节结构），写入目标版本矩阵与 Core 层适配/处置记录。
- 依赖：1.2–1.4；约束：文档型任务（无 RED 依据）。
- 验收：文件存在，含目标版本矩阵（D1 表）与 Core 层适配条目。

### 2. Boot 层：Boot 4.1 适配

#### 2.1 Boot 层 JDK 与 BOM 升级

<!-- comet-task-ref:3cea7bc8-853e-497d-a0d2-24d5e8df6e84 -->

- 范围：`fw-plus-boot/pom.xml` `jdk.version` → 21；`fw-plus-boot-dependencies` 升级——Spring Boot Admin → 4.1.x（4.1.4）、mica-mqtt → 2.5.9+、jasypt / dynamic-tp / plumelog 按 D5 处置、seata 与 jackson 行复核（Boot 4 的 Jackson 3 为 `tools.jackson`，Boot 层 jackson 2.x 属性需按口径重定义，必要时走 `spring-boot-jackson2` 过渡模块）。
- 依赖：1.x（Core 已安装到本地仓库）；验收：`./mvnw help:effective-pom -f fw-plus-boot/pom.xml` 抽查生效、构建可推进。

#### 2.2 Boot 层源码与自动配置适配

<!-- comet-task-ref:4222e636-b09a-4094-b4f4-89457ff801aa -->

- 范围：starter 拆包坐标（`spring-boot-starter-webmvc` 等）、自动配置包重组、Jackson 3（`tools.jackson`）、JSpecify、配置属性重命名、`spring.factories` / `AutoConfiguration.imports` 清理，以构建与测试暴露点为准逐点记录（含 magicapi、dtp、plumelog、mqtt、ws 等装配路径）。
- 依赖：2.1；验收：`cd fw-plus-boot && ./mvnw clean install -DskipTests` 成功。

#### 2.3 Boot 层测试回归

<!-- comet-task-ref:6954a026-6e54-467f-9a11-c5d5fdefc933 -->

- 范围：显式开启测试（重点 `fw-plus-boot-autoconfigure` 自动配置基线用例，含 MagicAPI 隔离、plumelog、dtp 相关测试）。
- 依赖：2.2；验收：`cd fw-plus-boot && ./mvnw clean test -Dmaven.test.skip=false` 全绿；回归修复记录 RED/GREEN。

#### 2.4 升级说明追加 Boot 节

<!-- comet-task-ref:8509d85f-3fb5-44c0-8a8e-13a9f88bdf83 -->

- 依赖：2.1–2.3；验收：文档含 Boot 节，列出适配点与组件处置（jasypt/dynamic-tp/plumelog 结论、SBA 4.1、mica-mqtt）。

### 3. Cloud 层：Cloud 2025.1 与 Alibaba/Tencent 处置

#### 3.1 Cloud 层 JDK 与 BOM 升级

<!-- comet-task-ref:fe1c64dc-d4c3-4d69-95da-0afaad11e6bf -->

- 范围：`fw-plus-cloud/pom.xml` `jdk.version` → 21；`fw-plus-cloud-dependencies`（servo-core、feign 等）与 Cloud starter 依赖升级至 2025.1.x 兼容线。
- 依赖：Boot 层通过（Cloud 依赖 Boot）；验收：`./mvnw help:effective-pom -f fw-plus-cloud/pom.xml` 抽查生效、构建可推进。

#### 3.2 Alibaba / Tencent 实测与处置

<!-- comet-task-ref:7e14a3b7-98b4-40aa-80d2-9c6c805af8d3 -->

- 范围：实测 Alibaba 2025.1.0.0（nacos / sentinel 装配）与 Tencent 2.1.2.0-2025.1.1（支持 Boot >=4.0.0 <4.2.0-M1）；结论（升级 / 冻结 / 替换）写入升级说明，必要时更新 `fw-plus-dependencies` Tencent 行。
- 依赖：3.1；约束：不改 Cloud starter 的对外契约。
- 验收：Cloud 层构建成功；Alibaba/Tencent 结论可追溯。

#### 3.3 Cloud 层源码适配

<!-- comet-task-ref:dfb1b512-9b4d-489a-9409-edeffe4877c0 -->

- 依赖：3.1 / 3.2；验收：`cd fw-plus-cloud && ./mvnw clean install -DskipTests` 成功（逐点记录适配）。

#### 3.4 Cloud 层测试回归

<!-- comet-task-ref:ce5ef991-9a9a-41cf-9355-73b5983ef32f -->

- 依赖：3.3；验收：`cd fw-plus-cloud && ./mvnw clean test -Dmaven.test.skip=false` 全绿。

#### 3.5 升级说明追加 Cloud 节

<!-- comet-task-ref:4723bc02-a4c8-4750-adaa-1a0504d7dea9 -->

- 依赖：3.1–3.4；验收：文档含 Cloud 节（适配点与 Alibaba/Tencent 结论）。

### 4. AI 层：Spring AI 2.0 / LangChain4j / AgentScope

#### 4.1 AI 层 JDK 与 BOM 升级

<!-- comet-task-ref:88afe2c1-2c35-4cb3-b5bc-ab98538ab66c -->

- 范围：`fw-plus-ai/pom.xml` `jdk.version` → 21；复核 Core BOM 中 spring-ai → 2.0 线、spring-ai-alibaba → 2.0.0-M1.x 线、langchain4j → 最新稳定线；AI BOM（agentscope / langgraph4j / reactor / jackson / opentelemetry）调整至兼容组合，含「Maven 同坐标首次声明优先」注释逻辑重验。
- 依赖：Core 层通过；验收：`./mvnw help:effective-pom -f fw-plus-ai/pom.xml` 抽查生效、构建可推进。

#### 4.2 AI 层源码适配

<!-- comet-task-ref:4ea20c41-92e1-4915-b5a8-15c15f5a1bcb -->

- 范围：Jackson 2/3 共存边界（AgentScope 对 Jackson 2 的依赖与框架 Jackson 3）、AgentScope 自动配置抄录线在 Boot 4 下行为、JSpecify；langchain4j-* 模块注意显式声明 lombok 与 langchain4j-core（上游 optional 不传递）。
- 依赖：4.1；验收：`cd fw-plus-ai && ./mvnw clean install -DskipTests` 成功（逐点记录）。

#### 4.3 AI 层测试回归

<!-- comet-task-ref:0e09fadb-0d46-4828-bd49-46eee050e90c -->

- 范围：显式开启测试（重点 `fw-plus-ai-boot-autoconfigure`，含依赖基线校验用例，原 `SpringBoot3DependencyBaselineTest` 需同步为 Boot 4 基线断言）。
- 依赖：4.2；验收：`cd fw-plus-ai && ./mvnw clean test -Dmaven.test.skip=false` 全绿。

#### 4.4 升级说明追加 AI 节

<!-- comet-task-ref:f77e48fa-d0bc-4849-a72d-31eba4f03d80 -->

- 依赖：4.1–4.3；验收：文档含 AI 节（Jackson 2/3 共存与 AgentScope 处置）。

### 5. Sample：15 个样例适配与冒烟

#### 5.1 样例适配与测试编译

<!-- comet-task-ref:96f61448-bee5-43be-a7e6-1b9dcd544784 -->

- 范围：收敛样例 pom 零散版本覆盖至 BOM（含 `fw-plus-ai-chat-sample` compiler 配置核对），15 个样例（Boot 6 + Cloud 4 + AI 5）Boot 4 适配。
- 依赖：Cloud / AI 层通过；验收：`./mvnw -f fw-plus-sample/pom.xml clean test-compile -Dmaven.test.skip=false` 成功。

#### 5.2 代表性样例冒烟

<!-- comet-task-ref:be7ca1a2-d6ef-48fd-85e4-97719713c2a5 -->

- 范围：按 design D6 标准（可离线、无强外部中间件、覆盖代表性装配路径）选定并启动冒烟，候选 ws / jdbc / mp。
- 依赖：5.1；验收：样例启动成功、关键端点可访问，冒烟命令与结果记录在案（外部中间件依赖不可满足时按标准换选并记录依据）。

#### 5.3 升级说明追加 Sample 节

<!-- comet-task-ref:7c2c3fc0-ff6e-4452-a1cb-6d335c6c19f8 -->

- 依赖：5.1–5.2；验收：文档含样例节与冒烟选型结论。

### 6. 集成收口：全仓验证与基线描述同步

#### 6.1 根聚合全量构建

<!-- comet-task-ref:c81078db-1df5-43c2-a730-25515f945cec -->

- 依赖：1–5 全部；验收：根目录 `./mvnw clean install -DskipTests` 成功（含全部样例编译），作为 build 证据录制。

#### 6.2 关键回归与冒烟复跑

<!-- comet-task-ref:0bc034ae-4254-45b5-a114-c4ddd895581b -->

- 依赖：6.1；验收：`cd fw-plus-boot && ./mvnw clean test -Dmaven.test.skip=false` 全绿、冒烟样例再次启动成功（集成态）。

#### 6.3 Spec 场景逐项核对

<!-- comet-task-ref:ff55c5c5-8490-4ec7-b216-b0275da59ef7 -->

- 范围：对照 `specs/platform-baseline/spec.md` 五条 Requirement 逐场景核对——无 17 编译声明残留、BOM 无 3.5.8 / 2025.0.0 残留、组件处置可追溯、核心测试通过、行为语义差异仅限平台基线（抽查对外 API 与配置元数据 diff）。
- 依赖：6.1–6.2；验收：逐场景结论核对记录产出，全部满足。

#### 6.4 AGENTS / README 基线同步

<!-- comet-task-ref:e51fe3c3-7de8-4a06-8989-b81b00a6a4d4 -->

- 范围：`AGENTS.md` 与 `README.md` 基线描述同步为 JDK 21 / Spring Boot 4.1.x / `springboot4` 分支。
- 依赖：6.3；约束：文档型任务；避免覆盖用户无关改动。
- 验收：两文件无过时基线描述（JDK 17 / Boot 3.5.8）残留。

#### 6.5 升级说明定稿核对

<!-- comet-task-ref:db89bbf5-2e3b-450a-8c87-bb2ba2abdbd8 -->

- 依赖：6.4；验收：`docs/springboot4-upgrade-notes.md` 含五节（版本矩阵 / 破坏性清单 / 代码层适配 / 生态组件处置 / 待办），按 spec「升级说明产出」场景检查通过。

## 执行顺序与提交策略

1. Core（1.1 → 1.5）：1.1 建基线，1.2 升矩阵（失败输出为 RED），1.3 适配至构建过，1.4 测试全绿，1.5 建文档。
2. Boot（2.1 → 2.4）：BOM → 适配 → 测试（显式开启）→ 文档。
3. Cloud（3.1 → 3.5）与 AI（4.1 → 4.4）：Cloud 依赖 Boot，AI 依赖 Core；两者间无依赖可交替，默认按 Cloud → AI 推进。
4. Sample（5.1 → 5.3）：适配 test-compile → 冒烟 → 文档。
5. 收口（6.1 → 6.5）：全量构建 → 回归复跑 → spec 核对 → 基线文档同步 → 升级说明定稿。
6. 提交：每层通过后提交一次（层内任务合并；commit message 带模块前缀，如 `fw-plus-core: 升级 JDK 21 与 Spring Boot 4.1 依赖矩阵`）；收口后一次集成提交；`tasks.md` 勾选与 `.comet` 状态随对应提交一并保存。不 push、不改分支。

## 风险与回退

- jasypt / dynamic-tp / plumelog 无 Boot 4 兼容版本 → 按 design D5 冻结并记录影响；若阻塞装配，退到「optional 依赖保持冻结 + 样例不使用」的边界并记录。
- knife4j 切 fork（knife4j-next）异常 → 回退评估「暂缓 knife4j + 仅保留 springdoc」最小方案并记录。
- Magic-API / EasyPoi 兼容问题 → 仓内修复；超出升级边界时显式上报（不改版本号）。
- Tomcat 11.0.24 CVE → 构建期核验 4.1.x 最新补丁；必要时在 BOM 钉 `tomcat.version`（最小干预）并记录。
- 内网 Nexus 不可达 → 记录并推迟对应验证，不改依赖定义。
- 样例离线冒烟不可行 → 按 D6 换选候选；均不可行时以测试编译 + autoconfigure 基线测试补偿并记录。
- 单层回退：还原该层改动即可（层间经 `~/.m2` 传递，不交叉污染源码）；整体放弃 = 丢弃 `springboot4` 分支变更。
