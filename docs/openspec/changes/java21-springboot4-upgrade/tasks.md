# 任务：Java 21 与 Spring Boot 4.1 基线升级

## 1. Core 层：JDK 21 与全局版本矩阵

- [x] 1.1 将 `fw-plus-core/pom.xml` 的 `jdk.version` 升至 21，并清理 `fw-plus-auth-core` 中 `maven.compiler.source/target=1.8` 遗留；验证：`fw-plus-core` 内无 17 / 1.8 编译声明残留（`rg "jdk\.version|1\.8" fw-plus-core -g pom.xml`），且 `cd fw-plus-core && ./mvnw clean install -DskipTests` 在 JDK 21 下成功（旧依赖矩阵下的可编译基线）。 <!-- comet-task:c6a5871d-a930-4de5-aec9-69e72904e095 -->
- [x] 1.2 按 design D1 升级 `fw-plus-dependencies` 版本矩阵到目标线（spring-boot-dependencies → 4.1 线最新补丁、spring-cloud-dependencies → 2025.1.3、spring-cloud-alibaba → 2025.1.0.0、Spring AI → 2.0 线、LangChain4j → 最新稳定线，MyBatis-Plus/Flex、Sa-Token、Druid、knife4j/springdoc、easy-trans 等按 D5 取兼容线；Magic-API 与 EasyPoi 版本号不动；Tencent 行留待 Cloud 组处置）；同时核验 Boot 管理的 Tomcat 版本与公开 CVE 修复面，如需钉 `tomcat.version` 一并记录；验证：`./mvnw help:effective-pom`（抽查上述版本生效）与 `cd fw-plus-core && ./mvnw dependency:resolve` 通过。 <!-- comet-task:d0b9710c-a1e0-41d4-8d5d-0476db7c385e -->
- [x] 1.3 适配 Core 层源码至 Boot 4 / Framework 7 / Jackson 3 / JSpecify（以编译与测试暴露点为准，逐点记录）；验证：`cd fw-plus-core && ./mvnw clean install -DskipTests` 成功。 <!-- comet-task:5516b38f-c873-469f-8f9f-c4479b80d01f -->
- [x] 1.4 运行 Core 层测试并修复回归；验证：`cd fw-plus-core && ./mvnw clean test` 全绿。 <!-- comet-task:657fbdf1-8150-4a73-91f0-727b6254f5c0 -->
- [x] 1.5 创建 `docs/springboot4-upgrade-notes.md`（结构对齐 `docs/springboot3-upgrade-notes.md`：版本矩阵、破坏性清单、代码层适配、生态组件处置、待办）并写入 Core 层改动记录；验证：文件存在且含目标版本矩阵与 Core 层适配条目。 <!-- comet-task:81b080a8-966c-4096-9c5e-512a33babaa8 -->

## 2. Boot 层：Boot 4.1 适配

- [ ] 2.1 `fw-plus-boot/pom.xml` 的 `jdk.version` 升至 21；升级 `fw-plus-boot-dependencies`（jackson、seata、spring-boot-admin → 4.1.x、mica-mqtt、dynamic-tp、plumelog 等）至 Boot 4.1 兼容线或按 D5 记录处置；验证：`./mvnw help:effective-pom` 抽查新版本生效、构建可推进。 <!-- comet-task:3cea7bc8-853e-497d-a0d2-24d5e8df6e84 -->
- [ ] 2.2 适配 Boot 层源码与自动配置（starter 拆包坐标、自动配置包重组、Jackson 3、JSpecify、配置属性重命名等，以构建与测试暴露点为准逐点记录）；验证：`cd fw-plus-boot && ./mvnw clean install -DskipTests` 成功。 <!-- comet-task:4222e636-b09a-4094-b4f4-89457ff801aa -->
- [ ] 2.3 显式开启 Boot 层测试并修复回归（重点 `fw-plus-boot-autoconfigure` 的自动配置基线用例）；验证：`cd fw-plus-boot && ./mvnw clean test -Dmaven.test.skip=false` 全绿。 <!-- comet-task:6954a026-6e54-467f-9a11-c5d5fdefc933 -->
- [ ] 2.4 升级说明追加 Boot 层记录（适配点与组件处置）；验证：文档含 Boot 节且列出对应条目。 <!-- comet-task:8509d85f-3fb5-44c0-8a8e-13a9f88bdf83 -->

## 3. Cloud 层：Cloud 2025.1 与 Alibaba/Tencent 处置

- [ ] 3.1 `fw-plus-cloud/pom.xml` 的 `jdk.version` 升至 21；升级 `fw-plus-cloud-dependencies`（servo-core、feign 等）与 Cloud starter 依赖至 2025.1.x 兼容线；验证：`./mvnw help:effective-pom` 抽查新版本生效、构建可推进。 <!-- comet-task:fe1c64dc-d4c3-4d69-95da-0afaad11e6bf -->
- [ ] 3.2 实测 Spring Cloud Alibaba 2025.1.0.0（nacos/sentinel 装配），并核验 Spring Cloud Tencent 是否存在 Boot 4 兼容线；无兼容线则按 D5 给出处置（必要时更新 `fw-plus-dependencies` 中 Tencent 行）；验证：Cloud 层构建成功，Alibaba/Tencent 结论（升级 / 冻结 / 替换）写入升级说明。 <!-- comet-task:7e14a3b7-98b4-40aa-80d2-9c6c805af8d3 -->
- [ ] 3.3 适配 Cloud 层源码至构建通过（逐点记录）；验证：`cd fw-plus-cloud && ./mvnw clean install -DskipTests` 成功。 <!-- comet-task:dfb1b512-9b4d-489a-9409-edeffe4877c0 -->
- [ ] 3.4 显式开启 Cloud 层测试并修复回归；验证：`cd fw-plus-cloud && ./mvnw clean test -Dmaven.test.skip=false` 全绿。 <!-- comet-task:ce5ef991-9a9a-41cf-9355-73b5983ef32f -->
- [ ] 3.5 升级说明追加 Cloud 层记录（适配点清单）；验证：文档含 Cloud 节。 <!-- comet-task:4723bc02-a4c8-4750-adaa-1a0504d7dea9 -->

## 4. AI 层：Spring AI 2.0 / LangChain4j / AgentScope

- [ ] 4.1 `fw-plus-ai/pom.xml` 的 `jdk.version` 升至 21；复核并调整 Core BOM 中 spring-ai / spring-ai-alibaba / langchain4j 行与 AI BOM 中 agentscope / langgraph4j / reactor / jackson / opentelemetry 行（含"Maven 同坐标首次声明优先"注释逻辑重验）至 Boot 4.1 兼容组合；验证：`./mvnw help:effective-pom` 抽查生效、构建可推进。 <!-- comet-task:88afe2c1-2c35-4cb3-b5bc-ab98538ab66c -->
- [ ] 4.2 适配 AI 层源码（Jackson 2/3 共存边界、AgentScope 自动配置抄录线在 Boot 4 下的行为、JSpecify 等，逐点记录）；验证：`cd fw-plus-ai && ./mvnw clean install -DskipTests` 成功。 <!-- comet-task:4ea20c41-92e1-4915-b5a8-15c15f5a1bcb -->
- [ ] 4.3 显式开启 AI 层测试并修复回归（重点 `fw-plus-ai-boot-autoconfigure`，含依赖基线校验用例）；验证：`cd fw-plus-ai && ./mvnw clean test -Dmaven.test.skip=false` 全绿。 <!-- comet-task:0e09fadb-0d46-4828-bd49-46eee050e90c -->
- [ ] 4.4 升级说明追加 AI 层记录（含 Jackson 2/3 共存与 AgentScope 处置）；验证：文档含 AI 节。 <!-- comet-task:f77e48fa-d0bc-4849-a72d-31eba4f03d80 -->

## 5. Sample：15 个样例适配与冒烟

- [ ] 5.1 收敛样例 pom 中零散版本覆盖至 BOM（含 `fw-plus-ai-chat-sample` 的 compiler 配置核对），完成 15 个样例的 Boot 4 适配；验证：`./mvnw -f fw-plus-sample/pom.xml clean test-compile -Dmaven.test.skip=false` 成功（15 样例全部通过测试编译）。 <!-- comet-task:96f61448-bee5-43be-a7e6-1b9dcd544784 -->
- [ ] 5.2 按 design D6 标准（可离线、无强外部中间件、覆盖代表性装配路径）选定并启动代表性样例冒烟（候选 ws / jdbc / mp）；验证：样例启动成功、关键端点可访问，冒烟命令与结果记录在案。 <!-- comet-task:be7ca1a2-d6ef-48fd-85e4-97719713c2a5 -->
- [ ] 5.3 升级说明追加 Sample 节与冒烟选型结论；验证：文档含样例节与冒烟结果。 <!-- comet-task:7c2c3fc0-ff6e-4452-a1cb-6d335c6c19f8 -->

## 6. 集成收口：全仓验证与基线描述同步

- [ ] 6.1 根聚合全量构建（干净环境一次通过）；验证：根目录 `./mvnw clean install -DskipTests` 成功，含全部样例编译。 <!-- comet-task:c81078db-1df5-43c2-a730-25515f945cec -->
- [ ] 6.2 关键回归与冒烟复跑（集成态）；验证：Boot 层 `./mvnw clean test -Dmaven.test.skip=false` 全绿、冒烟样例再次启动成功。 <!-- comet-task:0bc034ae-4254-45b5-a114-c4ddd895581b -->
- [ ] 6.3 对照 `specs/platform-baseline/spec.md` 场景逐项核对：全仓无 17 编译声明残留、BOM 无 3.5.8 / 2025.0.0 残留、组件处置可追溯、核心测试通过、行为语义差异仅限平台基线（抽查对外 API 与配置元数据 diff）；验证：核对记录（逐场景结论）产出，全部满足。 <!-- comet-task:ff55c5c5-8490-4ec7-b216-b0275da59ef7 -->
- [ ] 6.4 同步 `AGENTS.md` 与 `README.md` 基线描述（JDK 21 / Spring Boot 4.1.x / `springboot4` 分支）；验证：两文件中无过时基线描述（JDK 17 / Boot 3.5.8）残留。 <!-- comet-task:e51fe3c3-7de8-4a06-8989-b81b00a6a4d4 -->
- [ ] 6.5 升级说明定稿核对；验证：`docs/springboot4-upgrade-notes.md` 含版本矩阵、破坏性清单、代码层适配、生态组件处置与待办五节并按 spec「升级说明产出」场景检查通过。 <!-- comet-task:db89bbf5-2e3b-450a-8c87-bb2ba2abdbd8 -->

## Workflow follow-up

- 在验证与评审要求满足后归档本 change（Comet Classic archive 阶段）。
- 归档后复核归档结果（change 移入归档目录、状态闭合）。
