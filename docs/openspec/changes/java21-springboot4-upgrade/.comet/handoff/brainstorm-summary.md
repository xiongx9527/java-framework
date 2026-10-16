# Brainstorm Summary

- Change: java21-springboot4-upgrade
- Date: 2026-10-16
- 状态：**已确认**（2026-10-16，Step 1c 用户选择「确认本设计」）

## 确认的技术方案

**总体路线：分层闭环推进 + 根聚合收口。**

Core → Boot → Cloud / AI（两者可交替）→ Sample → 根聚合全量。每层完成「升级版本 → 全量构建 → 适配修复 → 测试通过」后才进入下一层；层间依赖经本地仓库（`~/.m2`）传递；收口用根聚合一把构建验证全仓。

**版本矩阵目标**（构建期如出更新补丁可就地取新，不改方案）：

| 组件 | 当前 → 目标 | 备注 |
| --- | --- | --- |
| JDK | 17 → 21 | 四层 `jdk.version`；清理 auth-core `maven.compiler.source/target=1.8` 遗留 |
| Spring Boot | 3.5.8 → 4.1.1 | 4.1 线最新补丁（管理 Framework 7.0.9 / Security 7.1.1 / Tomcat 11.0.24） |
| Spring Cloud | 2025.0.0 → 2025.1.3 | Oakwood；2025.1.2+ 兼容 Boot 4.1.x |
| Spring Cloud Alibaba | 2025.0.0.0 → 2025.1.0.0 | 已发布适配线（2026-02-06） |
| Spring Cloud Tencent | 1.14.0-2023.0.0-RC2 → 2.1.2.0-2025.1.1 | 支持 Boot >=4.0.0 <4.2.0-M1（2026-09-01） |
| Spring AI | 1.1.8 → 2.0.0 GA | 面向 Boot 4.0/4.1（2026-06-12） |
| Spring AI Alibaba | 1.1.2.4-security-fix → 2.0 适配线 | 2.0.0-M1.x 里程碑线，构建期核验 |
| Spring Boot Admin | 3.5.6 → 4.1.4 | 版本映射 Boot 4.1.Y → SBA 4.1.c |
| LangChain4j | 1.22.0 → 最新稳定线 | 对 Boot 无强绑定，风险低 |
| MyBatis-Plus | 3.5.16 → 3.5.17 | 切 `mybatis-plus-spring-boot4-starter`（自 3.5.13 起） |
| MyBatis-Flex | 1.11.4 → 1.11.8 | 切 `mybatis-flex-spring-boot4-starter`（自 1.11.5 起；需补 `spring-boot-starter-jdbc`） |
| Sa-Token | 1.46.0 → 1.46.0 | 切 `sa-token-spring-boot4-starter`（版本号不变，自 v1.45.0 起有 boot4 坐标） |
| Druid | 1.2.28 → 1.2.28 | 切 `druid-spring-boot-4-starter`（版本号不变） |
| dynamic-datasource | 4.3.1 → 4.6.0 | 切 `dynamic-datasource-spring-boot4-starter`（自 4.5.0 起） |
| easy-trans | 3.0.6 → 4.0.2 | 4.0.x=Boot4 线；3.1.x=Boot3 线 |
| knife4j | 4.5.0 → knife4j-next 5.7.8 | 上游 xiaoymin 无 Boot4 支持（#994/#995），换社区 fork（groupId `com.baizhukui`），仅改 groupId |
| springdoc | 2.8.17 → 3.0.3 | Boot4 线，随 knife4j-next 传递，覆盖版本同步 |
| mica-mqtt | 2.3.9 → 2.5.9+ | 自 2.5.9 起支持 Boot 4 / Jackson 3 |
| Magic-API / EasyPoi | 2.2.2 / fork → 不变 | 仓内自维护/fork，兼容性以实测为准，可仓内修复 |

**Boot 4 适配核心模式**（编译/测试驱动，逐点记录）：

- starter / 模块坐标切换：`spring-boot-starter-web` → `spring-boot-starter-webmvc` 等拆包后坐标，自动配置模块重组（`spring-boot-autoconfigure-web` → `spring-boot-webmvc-autoconfigure`）；
- Jackson 3：`com.fasterxml.jackson` → `tools.jackson`（Jackson 2/3 命名空间不同，可共存）；
- JSpecify 空性注解替换（`org.springframework.lang.Nullable` → `org.jspecify.annotations.Nullable`）；
- 配置属性重命名、`spring.factories` 残留清理、JUnit 6 测试 API 适配（仅显式开启测试的模块）；
- 见 design.md D4。

**风险组件处置工作流**（design.md D5）：查可用版本线 → 取 Boot 4.1 兼容线 → 构建/装配/冒烟实测 → 不可用则给出「冻结 / 替换 / 降级边界」处置并记录到升级说明。

## 关键取舍与风险

- **jasypt**：上游 4.0.4 在 Boot 4 下 `NoSuchMethodError`（issue #425），Boot4 兼容发布版缺失（issue #429 open）；仓库现用 3.0.5（Boot 层 optional + magicapi 样例）。→ 按 D5 处置（冻结当前版本并记录影响 / 评估替代），不阻塞主线。
- **dynamic-tp / plumelog**：官方无 Boot 4 支持证据（dynamic-tp 文档仅称 2.x/3.x；plumelog 3.5.3 无 Boot4 证据）。→ 按 D5 处置（冻结 + 记录影响范围），相关样例/装配路径在无兼容版本时降级。
- **knife4j**：上游无 Boot4 支持，切换社区 fork knife4j-next 为已定取舍（仅 groupId 变化，迁移成本低）。
- **Tomcat CVE**：Boot 4.1.1 管理 Tomcat 11.0.24，11.0.25/26 含 CVE-2026-76183 修复。→ 构建期核验新补丁是否纳入；如需在 BOM 钉 `tomcat.version` 并在升级说明记录（最小干预）。
- **Magic-API 2.2.2 / EasyPoi fork**：与 Servlet 6.1 / Boot 4 的实质不适配可在仓内修复；若工作量超出升级边界，作为显式项上报用户决策（约束：不改版本号）。
- **AI 层 Jackson 2/3 共存**：AgentScope 等对 Jackson 2 的依赖与框架 Jackson 3 边界需重验；AI BOM「Maven 同坐标首次声明优先」注释逻辑保持并重验。
- **内网 Nexus 可达性**：影响 Magic-API 等依赖解析 → 不可达时记录并推迟对应验证，不改依赖定义。
- **样例离线冒烟覆盖率有限**：多数样例依赖外部中间件；冒烟只覆盖可离线样例，其余以测试编译 + `fw-plus-boot-autoconfigure` 基线测试补偿。
- **behavioral 语义不变**：纯基线升级，既有 capability 对外行为语义不变；破坏性差异仅限平台基线本身并在升级说明列明。

## 测试策略

- **分层构建门禁**：每层 `./mvnw clean install -DskipTests` 通过后才进入下一层；收口用根聚合 `./mvnw clean install -DskipTests` 全仓（含样例编译与测试编译）。
- **测试回归**：Core 层默认跑测试；Boot / Cloud / AI 显式 `-Dmaven.test.skip=false` 开启，重点 `fw-plus-boot-autoconfigure` 自动配置基线测试与 `fw-plus-ai-boot-autoconfigure` 依赖基线校验用例。
- **样例验收**：15 个样例经 `fw-plus-sample` 聚合器 test-compile 全通过；按 D6 标准（可离线、无强外部中间件、覆盖代表性装配路径）选代表性样例启动冒烟，候选：`fw-plus-ws` / `fw-plus-jdbc` / `fw-plus-mp`。
- **收口核对**：对照 `specs/platform-baseline/spec.md` 五个 Requirement 的场景逐项核对（无 17 残留、无 3.5.8/2025.0.0 残留、组件处置可追溯、核心测试通过、语义差异仅限平台基线）。
- **文档同步**：`docs/springboot4-upgrade-notes.md` 五节（版本矩阵 / 破坏性清单 / 代码层适配 / 生态组件处置 / 待办）；AGENTS.md / README 基线描述收口阶段统一更新。

## Spec Patch

无。现有 `platform-baseline` 5 条 Requirement 已覆盖本设计验收（JDK 21 基线、Boot 4.1/Cloud 2025.1 矩阵、生态组件处置、既有能力不回归、升级说明与基线描述同步），无需回写 delta spec。
