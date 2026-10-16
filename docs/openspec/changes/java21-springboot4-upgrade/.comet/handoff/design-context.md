# Comet Design Handoff

- Change: java21-springboot4-upgrade
- Phase: design
- Mode: compact
- Context hash: 4c6faca0bdf1a1cd62d176460c9a75244a8953237a537a7498b8440d4f39f445

Generated-by: comet-handoff.sh
Task hash policy: task-content-v1. Read tasks.md for live completion; excerpts are design-time context.

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## docs/openspec/changes/java21-springboot4-upgrade/proposal.md

- Source: docs/openspec/changes/java21-springboot4-upgrade/proposal.md
- Lines: 1-33
- SHA256: 3cce69e9b4096ad353e475a5b12880472ebb7dbd452dcd80bbc605edff4f2a42

```md
# 提案：升级 Java 21 与 Spring Boot 4.1 基线

## Why

fw-plus 当前基线为 Spring Boot 3.5.8 / Spring Cloud 2025.0.0 / JDK 17。Spring Boot 3.5.x 的 OSS 支持已于 2026-06 结束，继续停留在旧基线将缺少安全补丁，并与生态组件的新版本线逐渐脱节。需要把整仓升级到 Spring Boot 4.1.x / Spring Cloud 2025.1.x（Oakwood）/ JDK 21，并在新基线下保持既有框架能力可用、样例可构建运行。

## What Changes

- 构建与运行基线从 Spring Boot 3.5.8 / Spring Cloud 2025.0.0 / JDK 17 升级到 Spring Boot 4.1.x / Spring Cloud 2025.1.x / JDK 21（四层 `jdk.version=21`、统一 BOM 版本矩阵）。**BREAKING**：框架最低运行环境变为 JDK 21 + Spring Boot 4.1（Jakarta EE 11 / Servlet 6.1 / Spring Framework 7）。
- 统一更新 Core/Boot/Cloud/AI 四层 BOM 中的第三方依赖到其 Boot 4.1 兼容线；对没有兼容版本的组件在 Design/Build 中给出替换或暂缓方案。**BREAKING**：依赖坐标与传递依赖矩阵变化。
- 适配 Spring Boot 4 / Spring Framework 7 的破坏性变化：模块化拆包后的坐标与包名调整、Jackson 2 → 3（`com.fasterxml.jackson` → `tools.jackson`）、JUnit 6、配置属性重命名、`spring.factories` 清理等，以构建与测试实际暴露的问题为准。**BREAKING**：主源码适配点。
- 适配 `fw-plus-sample` 全部样例（Boot 6 + Cloud 4 + AI 5 共 15 个）到新基线，保证可构建；选取可离线启动的代表性样例做启动冒烟。
- 更新文档：新增 `docs/springboot4-upgrade-notes.md`（版本矩阵、代码层改动、待办），同步 AGENTS.md / README 的基线描述。
- 不新增功能、不重构前端控制台源码、不做 Boot 3/Boot 4 双栈并存；既有 7 个 capability 规格的行为语义保持不变；本次不发版、不调整版本号策略（维持 0.3.0-SNAPSHOT）。

## Capabilities

### New Capabilities

- `platform-baseline`：定义 fw-plus 的构建与运行基线（JDK 21、Spring Boot 4.1.x / Spring Cloud 2025.1.x 依赖矩阵）及其约束——既有能力在新基线下不回归、样例可构建运行。

### Modified Capabilities

<!-- 无：既有 capability 的行为需求不变，升级只改变运行与依赖基线。 -->

## Impact

- 构建：根聚合 5 层（Core/Boot/Cloud/AI/Sample）pom、四个 BOM、样例聚合与各样例 pom。
- 代码：四层主源码中受 Boot 4 / Framework 7 / Jackson 3 / JUnit 6 影响的适配点；Magic-API 内部维护版与 EasyPoi fork 的兼容确认。
- 依赖矩阵：Spring Cloud Alibaba/Tencent、MyBatis-Plus/Flex、Sa-Token、knife4j/springdoc、Druid、dynamic-datasource/dynamic-tp、Spring AI 2.0 线、LangChain4j 等生态组件逐一确认 Boot 4.1 兼容版本。
- 样例：fw-plus-sample 下全部 15 个样例。
- 文档：新增升级说明；AGENTS.md / README 基线描述。
- OpenSpec：新增 `platform-baseline` 规格。

```

## docs/openspec/changes/java21-springboot4-upgrade/design.md

- Source: docs/openspec/changes/java21-springboot4-upgrade/design.md
- Lines: 1-167
- SHA256: 43e500652333680777d5796cae2543ebd554d358ede2d7738b50d5e44f766c52

[TRUNCATED]

```md
---
comet_change: java21-springboot4-upgrade
role: technical-design
canonical_spec: openspec
---

# 设计：Java 21 与 Spring Boot 4.1 基线升级

## Context

- 工作分支：`springboot4`（自 `springboot3` 迁出）。动机见 `proposal.md` - Why；本文只记录决定"怎么做"的现状与约束。
- 版本单点：`fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml` 是全局版本汇聚点，统一管理 spring-boot-dependencies 3.5.8、spring-cloud-dependencies 2025.0.0、spring-cloud-alibaba-dependencies 2025.0.0.0、spring-cloud-tencent-dependencies 1.14.0-2023.0.0-RC2、MyBatis-Plus 3.5.16、MyBatis-Flex 1.11.4、Sa-Token 1.46.0、Magic-API 2.2.2、Druid 1.2.28、knife4j 4.5.0、springdoc 2.8.17、easy-trans 3.0.6、LangChain4j 1.22.0、Spring AI 1.1.8 等；Boot 层 BOM（jasypt 3.0.5、seata 2.0.0、jackson 2.20.1、spring-boot-admin 3.5.6、mica-mqtt 2.3.9、dynamic-tp 1.2.2、plumelog 3.5.3 等）与 AI 层 BOM（AgentScope 2.0.4、LangGraph4j 1.9.3、reactor 3.8.2、jackson 2.21.1、opentelemetry 1.61.0 等）逐层 import 扩展。
- 编译基线：四层顶层 pom `jdk.version=17`（core:29 / boot:21 / cloud:21 / ai:29），编译插件以 source/target 方式编译。
- 既定约束（已在沟通中确认，不再重开）：
  - 目标线固定为 Spring Boot 4.1.x + Spring Cloud 2025.1.x（Oakwood）+ JDK 21，AI 层跟随 Spring AI 2.0 线；
  - Magic-API 为仓内自维护 2.2.2（jakarta 线，经内网 Nexus 解析），版本号不随意变更；EasyPoi 为仓内 fork；
  - 不发版、不调版本号（维持 0.3.0-SNAPSHOT）；不做 Boot 3/4 双栈；
  - 样例聚合器固定 0.3.0-SNAPSHOT、无 flatten；`.worktrees/` 不参与根构建。
- 勘察已核实的关键事实（2026-10）：
  - Spring Boot 4.1.1 为 4.1 线最新补丁（2026-08-20，OSS 支持至 2027-07-31）；4.1.1 管理 Spring Framework 7.0.9 / Spring Security 7.1.1 / Tomcat 11.0.24；
  - Spring Cloud 2025.1.3（Oakwood，2026-08-20）；官方兼容表：2025.1.x 自 2025.1.2 起兼容 Boot 4.1.x；
  - Spring Cloud Alibaba 2025.1.0.0（2026-02-06，适配 Boot 4.x / Cloud 2025.1.x）；Spring Cloud Tencent 2.1.2.0-2025.1.1（2026-09-01，支持 Boot >=4.0.0 <4.2.0-M1）；Spring Boot Admin 4.1.4（版本映射 Boot 4.1.Y → SBA 4.1.c）；
  - Spring AI 2.0.0 GA（2026-06-12，面向 Boot 4.0/4.1）；Spring AI Alibaba 2.0 适配线为 2.0.0-M1.x 里程碑（构建期核验具体版本）；
  - 生态 starter 已有 Boot4 专用坐标：MyBatis-Plus 3.5.17（`mybatis-plus-spring-boot4-starter`，自 3.5.13 起）、MyBatis-Flex 1.11.8（`mybatis-flex-spring-boot4-starter`，自 1.11.5 起）、Sa-Token 1.46.0（`sa-token-spring-boot4-starter`，自 v1.45.0 起）、Druid 1.2.28（`druid-spring-boot-4-starter`）、dynamic-datasource 4.6.0（`dynamic-datasource-spring-boot4-starter`，自 4.5.0 起）、easy-trans 4.0.2（4.0.x 为 Boot4 线，3.1.x 为 Boot3 线）、mica-mqtt 2.5.9+（Boot 4 / Jackson 3 支持）；
  - knife4j 上游（xiaoymin）无 Boot4 支持（issues #994/#995 未修复）；社区 fork knife4j-next（groupId `com.baizhukui`）提供 `knife4j-openapi3-boot4-spring-boot-starter` 5.7.8（springdoc 3.0.3），迁移仅需改 groupId；
  - Boot 4 破坏性变化要点：starter 模块化拆包（如 `spring-boot-starter-web` → `spring-boot-starter-webmvc`、自动配置模块重组）、Jackson 3（`tools.jackson`）、JSpecify 空性注解、JUnit 6、Jakarta EE 11 / Servlet 6.1、配置属性重命名；可用 `spring-boot-starter-classic` 作迁移过渡；
  - 无 Boot 4.1 兼容线的风险组件：jasypt（4.0.4 在 Boot 4 下 `NoSuchMethodError`，issue #425；Boot4 发布版缺失，issue #429 open）、dynamic-tp（官方仅称支持 Boot 2.x/3.x）、plumelog（3.5.3 无 Boot4 证据）——处置进入 D5；
  - 遗留项：`fw-plus-project/fw-plus-auth/fw-plus-auth-core/pom.xml` 仍声明 `maven.compiler.source/target=1.8`，需一并收敛。

## Goals / Non-Goals

**Goals:**

- 四层（Core/Boot/Cloud/AI）与全部 15 个样例在 JDK 21 / Boot 4.1.x / Cloud 2025.1.x 下构建与测试编译通过。
- 显式开启的核心模块测试通过；代表性样例启动冒烟通过。
- 每个高风险生态组件有明确处置（升级到兼容线 / 冻结记录 / 替换），并固化到 `docs/springboot4-upgrade-notes.md`。
- 版本矩阵集中可查；分层推进路径可回退。

**Non-Goals（设计层边界，不重复 proposal 范围）：**

- 不引入条件化双栈兼容（不为 Boot 3 保留并行分支）。
- 不为"以后可能用到"预升级与本次无关的组件。
- 不改模块结构与包组织（除 Boot 4 强制要求的坐标/包名迁移）。
- 不解决与平台升级无因果关系的既有缺陷；遇到即记录为待办。

## Decisions

### D1 版本矩阵：单点定义、分层传递

| 组件 | 当前 | 目标 | 依据 / 处置 |
| --- | --- | --- | --- |
| JDK | 17 | 21 | 用户已定；四层 `jdk.version` + auth-core 遗留收敛 |
| Spring Boot | 3.5.8 | 4.1.1（4.1 线最新补丁） | 构建期若 4.1.x 出新补丁可就地取新，不改方案 |
| Spring Cloud | 2025.0.0 | 2025.1.3 | Oakwood；2025.1.2+ 兼容 Boot 4.1.x |
| Spring Cloud Alibaba | 2025.0.0.0 | 2025.1.0.0 | 已发布适配线；构建期实测 nacos/sentinel 装配 |
| Spring Cloud Tencent | 1.14.0-2023.0.0-RC2 | 2.1.2.0-2025.1.1 | 支持 Boot >=4.0.0 <4.2.0-M1；构建期实测 |
| Spring AI | 1.1.8 | 2.0 线（2.0.0 GA 起） | 用户已定；2.0 面向 Boot 4.0/4.1 |
| Spring AI Alibaba | 1.1.2.4-security-fix | 2.0.0-M1.x 适配线 | 构建期核验具体里程碑版本 |
| LangChain4j | 1.22.0 | 最新稳定线 | 对 Boot 无强绑定，风险低，验证后定版 |
| Spring Boot Admin | 3.5.6 | 4.1.x 线（4.1.4） | 官方映射 Boot 4.1.Y → SBA 4.1.c |
| MyBatis-Plus | 3.5.16 | 3.5.17 | 坐标切 `mybatis-plus-spring-boot4-starter` |
| MyBatis-Flex | 1.11.4 | 1.11.8 | 坐标切 `mybatis-flex-spring-boot4-starter`；补 `spring-boot-starter-jdbc`（Boot4 autoconfigure 不再默认含 jdbc） |
| Sa-Token | 1.46.0 | 1.46.0 | 坐标切 `sa-token-spring-boot4-starter`（版本号不变） |
| Druid | 1.2.28 | 1.2.28 | 坐标切 `druid-spring-boot-4-starter`（版本号不变） |
| dynamic-datasource | 4.3.1 | 4.6.0 | 坐标切 `dynamic-datasource-spring-boot4-starter` |
| easy-trans | 3.0.6 | 4.0.2 | 4.0.x=Boot4 线（3.1.x 为 Boot3 线，不可用） |
| knife4j / springdoc | 4.5.0 / 2.8.17 | knife4j-next 5.7.8 / springdoc 3.0.3 | 上游无 Boot4 支持，切社区 fork（groupId `com.baizhukui`，仅改 groupId） |
| mica-mqtt | 2.3.9 | 2.5.9+ | Boot 4 / Jackson 3 支持线 |
| Magic-API / EasyPoi | 2.2.2 / fork | 不变版本号 | 仓内自维护/fork，兼容性以实测为准 |
| 其余组件 | 现状 | 按需升级 | 升级到 Boot 4.1 兼容线；无兼容线走 D5 处置 |

- Rationale：延续仓库既有"依赖管理单点 + 分层 import"模式，保证每个依赖只有一处版本定义；分层 import 顺序敏感（AI BOM 现有注释已说明"Maven 同坐标首次声明优先"），升级时保持该逻辑并重验。
- Alternative：各层各自钉版本 → 版本漂移、验证困难；一次性全量升级 → 错误面不可控，均不采用。

### D2 JDK 21 落点

- 四层顶层 pom `jdk.version` 17 → 21；编译插件保持 source/target 形式（最小变更面）。
- 删除 auth-core 的 `maven.compiler.source/target=1.8` 遗留配置，让其继承层基线。
- 样例不新增独立编译目标；构建期核对样例中零散的 compiler 配置（如 `fw-plus-ai-chat-sample`）是否需同步。
- Alternative：改用 `maven.compiler.release=21` —— 更严格但改变既有插件形态，本次不引入。

```

Full source: docs/openspec/changes/java21-springboot4-upgrade/design.md

## docs/openspec/changes/java21-springboot4-upgrade/tasks.md

- Source: docs/openspec/changes/java21-springboot4-upgrade/tasks.md
- Lines: 1-50
- SHA256: a822efdd48dcc94ce43b038a7529ac69a4ee8f1f2668e86c7999da11211bd174

```md
# 任务：Java 21 与 Spring Boot 4.1 基线升级

## 1. Core 层：JDK 21 与全局版本矩阵

- [ ] 1.1 将 `fw-plus-core/pom.xml` 的 `jdk.version` 升至 21，并清理 `fw-plus-auth-core` 中 `maven.compiler.source/target=1.8` 遗留；验证：`fw-plus-core` 内无 17 / 1.8 编译声明残留（`rg "jdk\.version|1\.8" fw-plus-core -g pom.xml`），且 `cd fw-plus-core && ./mvnw clean install -DskipTests` 在 JDK 21 下成功（旧依赖矩阵下的可编译基线）。
- [ ] 1.2 按 design D1 升级 `fw-plus-dependencies` 版本矩阵到目标线（spring-boot-dependencies → 4.1 线最新补丁、spring-cloud-dependencies → 2025.1.3、spring-cloud-alibaba → 2025.1.0.0、Spring AI → 2.0 线、LangChain4j → 最新稳定线，MyBatis-Plus/Flex、Sa-Token、Druid、knife4j/springdoc、easy-trans 等按 D5 取兼容线；Magic-API 与 EasyPoi 版本号不动；Tencent 行留待 Cloud 组处置）；同时核验 Boot 管理的 Tomcat 版本与公开 CVE 修复面，如需钉 `tomcat.version` 一并记录；验证：`./mvnw help:effective-pom`（抽查上述版本生效）与 `cd fw-plus-core && ./mvnw dependency:resolve` 通过。
- [ ] 1.3 适配 Core 层源码至 Boot 4 / Framework 7 / Jackson 3 / JSpecify（以编译与测试暴露点为准，逐点记录）；验证：`cd fw-plus-core && ./mvnw clean install -DskipTests` 成功。
- [ ] 1.4 运行 Core 层测试并修复回归；验证：`cd fw-plus-core && ./mvnw clean test` 全绿。
- [ ] 1.5 创建 `docs/springboot4-upgrade-notes.md`（结构对齐 `docs/springboot3-upgrade-notes.md`：版本矩阵、破坏性清单、代码层适配、生态组件处置、待办）并写入 Core 层改动记录；验证：文件存在且含目标版本矩阵与 Core 层适配条目。

## 2. Boot 层：Boot 4.1 适配

- [ ] 2.1 `fw-plus-boot/pom.xml` 的 `jdk.version` 升至 21；升级 `fw-plus-boot-dependencies`（jackson、seata、spring-boot-admin → 4.1.x、mica-mqtt、dynamic-tp、plumelog 等）至 Boot 4.1 兼容线或按 D5 记录处置；验证：`./mvnw help:effective-pom` 抽查新版本生效、构建可推进。
- [ ] 2.2 适配 Boot 层源码与自动配置（starter 拆包坐标、自动配置包重组、Jackson 3、JSpecify、配置属性重命名等，以构建与测试暴露点为准逐点记录）；验证：`cd fw-plus-boot && ./mvnw clean install -DskipTests` 成功。
- [ ] 2.3 显式开启 Boot 层测试并修复回归（重点 `fw-plus-boot-autoconfigure` 的自动配置基线用例）；验证：`cd fw-plus-boot && ./mvnw clean test -Dmaven.test.skip=false` 全绿。
- [ ] 2.4 升级说明追加 Boot 层记录（适配点与组件处置）；验证：文档含 Boot 节且列出对应条目。

## 3. Cloud 层：Cloud 2025.1 与 Alibaba/Tencent 处置

- [ ] 3.1 `fw-plus-cloud/pom.xml` 的 `jdk.version` 升至 21；升级 `fw-plus-cloud-dependencies`（servo-core、feign 等）与 Cloud starter 依赖至 2025.1.x 兼容线；验证：`./mvnw help:effective-pom` 抽查新版本生效、构建可推进。
- [ ] 3.2 实测 Spring Cloud Alibaba 2025.1.0.0（nacos/sentinel 装配），并核验 Spring Cloud Tencent 是否存在 Boot 4 兼容线；无兼容线则按 D5 给出处置（必要时更新 `fw-plus-dependencies` 中 Tencent 行）；验证：Cloud 层构建成功，Alibaba/Tencent 结论（升级 / 冻结 / 替换）写入升级说明。
- [ ] 3.3 适配 Cloud 层源码至构建通过（逐点记录）；验证：`cd fw-plus-cloud && ./mvnw clean install -DskipTests` 成功。
- [ ] 3.4 显式开启 Cloud 层测试并修复回归；验证：`cd fw-plus-cloud && ./mvnw clean test -Dmaven.test.skip=false` 全绿。
- [ ] 3.5 升级说明追加 Cloud 层记录（适配点清单）；验证：文档含 Cloud 节。

## 4. AI 层：Spring AI 2.0 / LangChain4j / AgentScope

- [ ] 4.1 `fw-plus-ai/pom.xml` 的 `jdk.version` 升至 21；复核并调整 Core BOM 中 spring-ai / spring-ai-alibaba / langchain4j 行与 AI BOM 中 agentscope / langgraph4j / reactor / jackson / opentelemetry 行（含"Maven 同坐标首次声明优先"注释逻辑重验）至 Boot 4.1 兼容组合；验证：`./mvnw help:effective-pom` 抽查生效、构建可推进。
- [ ] 4.2 适配 AI 层源码（Jackson 2/3 共存边界、AgentScope 自动配置抄录线在 Boot 4 下的行为、JSpecify 等，逐点记录）；验证：`cd fw-plus-ai && ./mvnw clean install -DskipTests` 成功。
- [ ] 4.3 显式开启 AI 层测试并修复回归（重点 `fw-plus-ai-boot-autoconfigure`，含依赖基线校验用例）；验证：`cd fw-plus-ai && ./mvnw clean test -Dmaven.test.skip=false` 全绿。
- [ ] 4.4 升级说明追加 AI 层记录（含 Jackson 2/3 共存与 AgentScope 处置）；验证：文档含 AI 节。

## 5. Sample：15 个样例适配与冒烟

- [ ] 5.1 收敛样例 pom 中零散版本覆盖至 BOM（含 `fw-plus-ai-chat-sample` 的 compiler 配置核对），完成 15 个样例的 Boot 4 适配；验证：`./mvnw -f fw-plus-sample/pom.xml clean test-compile -Dmaven.test.skip=false` 成功（15 样例全部通过测试编译）。
- [ ] 5.2 按 design D6 标准（可离线、无强外部中间件、覆盖代表性装配路径）选定并启动代表性样例冒烟（候选 ws / jdbc / mp）；验证：样例启动成功、关键端点可访问，冒烟命令与结果记录在案。
- [ ] 5.3 升级说明追加 Sample 节与冒烟选型结论；验证：文档含样例节与冒烟结果。

## 6. 集成收口：全仓验证与基线描述同步

- [ ] 6.1 根聚合全量构建（干净环境一次通过）；验证：根目录 `./mvnw clean install -DskipTests` 成功，含全部样例编译。
- [ ] 6.2 关键回归与冒烟复跑（集成态）；验证：Boot 层 `./mvnw clean test -Dmaven.test.skip=false` 全绿、冒烟样例再次启动成功。
- [ ] 6.3 对照 `specs/platform-baseline/spec.md` 场景逐项核对：全仓无 17 编译声明残留、BOM 无 3.5.8 / 2025.0.0 残留、组件处置可追溯、核心测试通过、行为语义差异仅限平台基线（抽查对外 API 与配置元数据 diff）；验证：核对记录（逐场景结论）产出，全部满足。
- [ ] 6.4 同步 `AGENTS.md` 与 `README.md` 基线描述（JDK 21 / Spring Boot 4.1.x / `springboot4` 分支）；验证：两文件中无过时基线描述（JDK 17 / Boot 3.5.8）残留。
- [ ] 6.5 升级说明定稿核对；验证：`docs/springboot4-upgrade-notes.md` 含版本矩阵、破坏性清单、代码层适配、生态组件处置与待办五节并按 spec「升级说明产出」场景检查通过。

## Workflow follow-up

- 在验证与评审要求满足后归档本 change（Comet Classic archive 阶段）。
- 归档后复核归档结果（change 移入归档目录、状态闭合）。

```

## docs/openspec/changes/java21-springboot4-upgrade/.openspec.yaml

- Source: docs/openspec/changes/java21-springboot4-upgrade/.openspec.yaml
- Lines: 1-2
- SHA256: 9cfa2ab963a8c9b58eecdb9fd5b55544e988fee8de8b887226ffc32d27744459

```md
schema: spec-driven
created: 2026-10-16

```

## docs/openspec/changes/java21-springboot4-upgrade/specs/platform-baseline/spec.md

- Source: docs/openspec/changes/java21-springboot4-upgrade/specs/platform-baseline/spec.md
- Lines: 1-77
- SHA256: 27d6eff999588ba20574e715ae8b5da294a49434d1ab3a5a90a788bb37fdecbd

```md
# Spec Delta

## Purpose

定义 fw-plus 的构建与运行基线：框架各层与样例在 JDK 21 与 Spring Boot 4.1.x / Spring Cloud 2025.1.x 依赖矩阵下构建、装配与运行，并保证既有能力在不同平台基线下行为语义一致、升级过程可追溯。

## ADDED Requirements

### Requirement: JDK 21 构建与运行基线

fw-plus 四个库层与全部样例 SHALL 以 JDK 21 作为构建与运行基线：Maven 编译目标 SHALL 声明为 21，不再声明 17；仓库 SHALL 能在 JDK 21 环境下完成全仓构建。

#### Scenario: 基线声明

- **WHEN** 检查四个库层的构建配置
- **THEN** Java 编译目标（`jdk.version`）为 21，且不存在仍声明 17 的库层或样例

#### Scenario: 全仓构建通过

- **WHEN** 在 JDK 21 环境执行根聚合的全仓构建（含 `fw-plus-sample` 全部样例的编译与测试编译）
- **THEN** 四个库层与全部样例构建成功，无编译或打包错误

### Requirement: Spring Boot 4.1 / Spring Cloud 2025.1 依赖矩阵

仓库各层 SHALL 以 Spring Boot 4.1.x 与 Spring Cloud 2025.1.x（Oakwood）为统一基线，相关版本 SHALL 由各层 BOM 集中管理；消费方应用引入 fw-plus starter 后 SHALL 能在 Spring Boot 4.1 上完成自动配置装配并正常启动。

#### Scenario: BOM 版本统一

- **WHEN** 检查 Core/Boot/Cloud/AI 各层 BOM 与依赖管理配置
- **THEN** Spring Boot 版本统一为 4.1.x 线、Spring Cloud 版本统一为 2025.1.x 线，且不再存在 3.5.8 / 2025.0.0 的残留声明

#### Scenario: 消费方装配与启动

- **WHEN** 代表性样例（Spring Boot 4.1 应用）引入对应 fw-plus starter 并启动
- **THEN** 自动配置按预期装配，应用启动成功，关键功能端点可访问

### Requirement: 生态组件 Boot 4.1 兼容处置

依赖矩阵内的第三方组件 SHALL 使用其 Spring Boot 4.1 兼容版本线；对不存在 Boot 4.1 兼容版本的组件，SHALL 给出明确处置（替换、降级边界或暂缓）并记录影响，不得静默保留与 Boot 4.1 不兼容的版本。

#### Scenario: 组件版本就位

- **WHEN** 构建全仓并检查受管依赖版本
- **THEN** 所有第三方依赖均为其 Boot 4.1 兼容线版本，或属于已记录处置的例外清单

#### Scenario: 例外可追溯

- **WHEN** 某组件缺少 Boot 4.1 兼容版本
- **THEN** 升级说明中记录该组件的处置方式、影响范围与后续动作，且全仓构建不受阻塞

### Requirement: 既有能力不回归

本次升级 SHALL 保持既有 capability 的对外行为语义不变：升级只改变构建与运行基线，不改变既有功能契约；核心模块测试 SHALL 在新基线下通过。

#### Scenario: 核心回归通过

- **WHEN** 显式开启核心模块测试（如 `fw-plus-boot-autoconfigure` 的自动配置基线测试）并在新基线下运行
- **THEN** 测试全部通过

#### Scenario: 行为语义保持不变

- **WHEN** 对比升级前后既有能力的对外 API、配置契约与事件协议
- **THEN** 无语义变化；破坏性差异仅限平台基线本身（JDK 21、Spring Boot 4.1 / Jakarta EE 11 / Jackson 3 等），且已在升级说明中列明

### Requirement: 升级说明与基线描述同步

仓库 SHALL 产出升级说明文档（`docs/springboot4-upgrade-notes.md`），记录目标版本矩阵、代码层改动与待办；AGENTS.md 与 README 的基线描述 SHALL 同步为新的 JDK 与 Spring Boot 版本。

#### Scenario: 升级说明产出

- **WHEN** 检查 `docs/springboot4-upgrade-notes.md`
- **THEN** 文档包含目标版本矩阵、破坏性改动清单、代码层适配说明与待办事项

#### Scenario: 基线描述同步

- **WHEN** 检查 AGENTS.md 与 README
- **THEN** 其描述的当前基线为 JDK 21 与 Spring Boot 4.1.x，不再声称 JDK 17 / Spring Boot 3.5.x

```
