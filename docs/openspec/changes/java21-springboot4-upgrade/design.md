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

### D3 推进次序：分层闭环 + 根聚合收口

Core → Boot → Cloud / AI（两者可交替）→ Sample → 根聚合全量。

- 每层完成"升级版本 → 全量构建 → 适配修复 → 通过"后才进入下一层；层间依赖经本地仓库（`~/.m2`）传递。
- 收口用根聚合一把构建验证全仓（含样例编译与测试编译）。
- Rationale：错误面可控、定位成本低；便于升级说明逐层记录适配点。
- Alternative：一次全改再统一修 —— 首轮错误爆炸（四层 × BOM × 源码 × 样例），反馈环过长，不采用。

### D4 Boot 4 破坏性适配策略：编译/测试驱动、逐点记录

- 只改构建与实际测试暴露的适配点，不预判性全量替换。初步适配清单：
  - starter / 模块坐标：`spring-boot-starter-web` → `spring-boot-starter-webmvc` 等拆包后坐标；第三方 starter 切 Boot4 专用坐标（见 D1）；排查时可用 `spring-boot-starter-classic` 作过渡；
  - 自动配置包名重组（如 `spring-boot-autoconfigure-web` → `spring-boot-webmvc-autoconfigure`）；
  - Jackson 3：框架自身代码 `com.fasterxml.jackson` → `tools.jackson`；核对 `AutoConfiguration.imports` 与配置元数据；
  - JSpecify 空性注解替换（`org.springframework.lang.Nullable` → `org.jspecify.annotations.Nullable`）；
  - JUnit 6 / 测试 API 适配（仅显式开启测试的模块）；
  - 配置属性重命名/移除与 `spring.factories` 残留清理。
- 每处适配记录到升级说明的"代码层改动"清单；归类不了的写"编译驱动适配"。
- Rationale：Boot 4 变化面大且部分破坏点难以静态预判（Spring Boot Admin 4 迁移即为实例），实际错误驱动最稳。
- Alternative：先全仓静态扫描再统一改 —— 扫描覆盖不了运行期行为（bean 装配、自动配置顺序），仍需构建驱动，收益有限。

### D5 生态组件处置工作流

对每个高风险组件执行：① 查可用版本线 → ② 取 Boot 4.1 兼容线 → ③ 构建/装配/冒烟实测 → ④ 不可用则给出"冻结 / 替换 / 降级边界"处置并记录。

- 已核实可用线：Spring Cloud Tencent 2.1.2.0-2025.1.1、knife4j-next 5.7.8（fork）、D1 表中各 starter 坐标切换项（MP / MF / Sa-Token / Druid / dynamic-datasource / easy-trans / mica-mqtt）。
- 待核验清单（构建期实测）：jasypt、dynamic-tp、plumelog、Spring AI Alibaba 2.0 线、LangChain4j 具体版本、seata，以及 InfluxDB / Elasticsearch / Kafka / Pulsar / RocketMQ / RabbitMQ 客户端等。
- 处置结果以表格固化到升级说明；spec 要求"不得静默保留不兼容版本"。

### D6 样例策略与冒烟选型标准

- 15 个样例（Boot 6 / Cloud 4 / AI 5）统一经 `fw-plus-sample` 聚合器构建；样例内零散版本覆盖收敛至 BOM。
- 冒烟标准：可离线启动、无强外部中间件依赖、覆盖代表性装配路径。
- 候选（按标准在构建期确认）：`fw-plus-ws-spring-boot-starter-sample`、`fw-plus-jdbc-spring-boot-starter-sample`、`fw-plus-mp-spring-boot-starter-sample`。
- 其余样例验收 = 编译与测试编译通过。

### D7 验收与文档同步

- 验收：根聚合构建（含样例编译与测试编译）＋ 核心模块显式开启测试（`-Dmaven.test.skip=false`，重点 `fw-plus-boot-autoconfigure`）＋ 代表性样例启动冒烟。
- 文档：`docs/springboot4-upgrade-notes.md` 结构对齐 `springboot3-upgrade-notes.md`（版本矩阵、破坏性清单、代码层适配、生态组件处置、待办）；AGENTS.md / README 基线描述在收口阶段统一更新。

## Test Strategy

- 分层构建门禁：每层 `./mvnw clean install -DskipTests` 通过后才进入下一层；收口用根聚合 `./mvnw clean install -DskipTests` 全仓（含样例编译与测试编译）。
- 测试回归：Core 层默认跑测试；Boot / Cloud / AI 层显式 `-Dmaven.test.skip=false` 开启，重点 `fw-plus-boot-autoconfigure` 自动配置基线测试与 `fw-plus-ai-boot-autoconfigure` 依赖基线校验用例。
- 样例验收：15 个样例经 `fw-plus-sample` 聚合器 test-compile 全通过；按 D6 标准选代表性样例启动冒烟（候选 ws / jdbc / mp），冒烟命令与结果记录在案。
- 收口核对：对照 `specs/platform-baseline/spec.md` 五个 Requirement 的场景逐项核对（无 17 编译声明残留、无 3.5.8 / 2025.0.0 残留、组件处置可追溯、核心测试通过、语义差异仅限平台基线）。
- 文档同步：升级说明五节结构 + AGENTS/README 基线描述（详见 D7）。

## Risks / Trade-offs

- [jasypt / dynamic-tp / plumelog 无 Boot 4 兼容版本] → 按 D5 处置（冻结并记录影响 / 评估替代）；jasypt 为 Boot 层 optional 依赖 + magicapi 样例使用，dynamic-tp / plumelog 为 Boot 层 optional 依赖，影响面以实测界定，不阻塞其他层与样例主线。
- [knife4j 切换社区 fork（knife4j-next）带来供应链信任成本] → fork 仅改 groupId、API 与上游保持一致；若装配/启动异常，回退评估"暂缓 knife4j + 仅保留 springdoc"的最小方案并记录。
- [Magic-API 2.2.2 自维护线与 Boot 4 / Servlet 6.1 有实质不适配] → 仓内可自行修复；若工作量超出升级边界，作为显式项上报用户决策（约束：不改版本号）。
- [EasyPoi fork 与 Boot 4 交互问题] → 同上，仓内 fork 可改。
- [Jackson 2/3 共存与 JSpecify 注解替换面遗漏] → 两者命名空间不同可共存；以依赖树核验 + 启动冒烟验证；AI BOM 中 AgentScope 相关注释需重验更新。
- [内嵌 Tomcat 安全面：Boot 4.1.1 管理 Tomcat 11.0.24，公开信息称 11.0.25/26 含 CVE-2026-76183 修复] → 构建期核验新补丁是否已纳入；如需在 BOM 钉 `tomcat.version` 并在升级说明记录（最小干预）。
- [样例离线冒烟覆盖率有限（多数样例依赖外部中间件）] → 冒烟只覆盖可离线样例；其余以测试编译 + `fw-plus-boot-autoconfigure` 基线测试补偿。
- [内网 Nexus 可达性影响 Magic-API 等依赖解析] → 若不可达，记录并推迟对应验证；不因此改动依赖定义。
- [升级改动面大、单次评审困难] → 分层推进，每层保留可构建状态；最终按层拆分提交或单次收口提交。

## Migration Plan

1. 准备（当前）：基线勘察（见 Context）、分支 `springboot4` 就位。
2. Core 层：`jdk.version=21` + `fw-plus-dependencies` 矩阵升级 → 构建 → 适配 → Core 测试通过。
3. Boot 层：BOM 与自动配置适配（Jackson 3、starter 拆包、SBA 4.1、JSpecify、jasypt/dynamic-tp/plumelog 处置）→ 构建 → 显式开启测试。
4. Cloud 层：Cloud 2025.1.3 + Alibaba 2025.1.0.0 + Tencent 2.1.2.0-2025.1.1（构建期实测）→ 构建。
5. AI 层：Spring AI 2.0 线 / LangChain4j / AgentScope 适配 → 构建 → 相关测试。
6. Sample：15 样例适配 → 编译与测试编译 → 冒烟。
7. 收口：根聚合全量构建 + 显式测试 + 冒烟复跑；升级说明与 AGENTS/README 同步；待办清单定稿。

回滚策略：

- 变更只存在于 `springboot4` 分支；`springboot3` 线不受影响（无共享提交）。
- 单层回退：还原该层改动即可（层间经本地仓库传递，不交叉污染源码）。
- 整体放弃 = 丢弃分支变更；不发版，无发布回滚需求。

## Open Questions

以下均为构建期核验项，不影响本设计、规格与任务拆分：

- jasypt / dynamic-tp / plumelog 的最终处置（冻结 / 替换 / 降级边界）——取决于实测结果。
- Spring AI Alibaba 2.0 适配线的具体里程碑版本与 LangChain4j 最终稳定版本——取决于构建态依赖解析与装配实测。
- 冒烟样例最终清单——取决于样例离线启动的实际可行性（标准见 D6）。
- 是否在 BOM 钉 `tomcat.version` 以覆盖公开披露的 Tomcat CVE——取决于 Boot 4.1.x 最新补丁管理的版本。
