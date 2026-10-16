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
