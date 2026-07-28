# Comet Design Handoff

- Change: fix-magicapi-plugin-sample-integration
- Phase: design
- Mode: compact
- Context hash: 76e01409ee42da681feaa28ac83c44f0416474a33589ffef75e56be19ea26145

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/fix-magicapi-plugin-sample-integration/proposal.md

- Source: openspec/changes/fix-magicapi-plugin-sample-integration/proposal.md
- Lines: 1-40
- SHA256: 4a3fc846b788d28d03558a255a62604718612de9acea3f120178910fbc2ec595

```md
## Why

当前 Magic-API Boot sample 只引入 25 个 Boot 插件中的 17 个，Mongo 依赖被注释，AI 的 2 个插件位于另一 sample；因此现有 sample 启动无法证明宿主与全部插件能够在同一应用中完成依赖解析、自动配置和启动。首次统一校验还暴露了本地旧 AI BOM 阻断 sample reactor 的问题，需要建立可重复的分层构建与整体启动证据。

## Root Cause

- Magic-API 插件模块持续增加，但 sample 依赖清单没有与 Boot/AI 插件聚合器同步，缺少 Admin、Druid、DTP、Influx、MQTT、MQTT Client、Task，并注释了 Mongo。
- Boot 与 AI 插件分别放在两个 sample 中，没有一个宿主同时覆盖全部插件 classpath。
- sample 依赖本地 `0.1.1-SNAPSHOT` BOM 和库产物；未先按 Core -> Boot -> Cloud/AI 安装时，旧 BOM 会产生与源码不一致的依赖解析结果。
- 现有 application 配置依赖 Apollo、Redis、Mongo 等外部服务和硬编码绝对资源路径，无法作为稳定、可重复的本地整体启动入口。

## What Changes

- 以 Magic-API Boot sample 作为唯一整体测试宿主，补齐当前 Boot 与 AI 全部 Magic-API 插件依赖。
- 提供不依赖外部中间件的整体测试启动配置，同时保持生产代码和默认插件行为不变。
- 增加可自动化的插件 classpath、Spring 装配与关键入口检查，确保遗漏插件或启动冲突能直接失败。
- 按 Core -> Boot -> Cloud/AI -> sample 顺序构建，并实际启动 sample 验证健康状态和 Magic-API 宿主入口。
- 对测试过程中复现的源码缺陷逐项建立 RED 证据后修复并回归。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

无。本变更只修复 sample 集成和验证覆盖，不改变生产接口、插件行为或已有验收契约。

## Impact

- 影响 `fw-plus-sample` 中 Magic-API sample 的依赖和测试/启动配置。
- 可能影响测试发现存在冲突的 Boot/AI Magic-API 插件实现；任何源码修复都必须先有对应 RED 证据。
- 不升级 Magic-API 2.2.2，不修改内部 Nexus 坐标，不引入 Java 9+ API。

## Failure Evidence

- 命令：`./fw-plus-core/mvnw -f fw-plus-sample/pom.xml validate`
- 结果：失败，Maven 报 `fw-plus-ai-magicapi-plugin-model` 缺少版本。
- 定位：源码 AI BOM 已管理该版本，本地 `~/.m2` 中旧 BOM 未包含该条目；这是分层构建前置条件缺失的环境证据，不作为源码缺陷修复。

```

## Task 6 Verification Evidence (2026-07-28)

- Gate: worktree `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/.worktrees/fix-magicapi-plugin-sample-integration`; `HEAD=ba5805a022ebb8aa422d6af19a8a7be9eba20b77`; branch `hotfix/20260727/fix-magicapi-plugin-sample-integration`; Java `1.8.0_432`; Maven Wrapper `3.8.8`.
- `./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/pom.xml -Dtest=MyBatisFlexConfigurationTest,MagicAPIMongoConfigurationTest,MagicAPIMqttClientConfigurationTest,MagicAPIRedisConfigurationTest,MagicAPIMcpConfigurationTest test`: exit `0`; 19 tests, failures `0`, errors `0`; `BUILD SUCCESS`; log `/tmp/fw-plus-magicapi-task6-boot-directed.log`.
- `./fw-plus-ai/mvnw -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml -Dtest=EmbeddingStoreAutoConfigurationTest,EmbeddingStoreAutoConfigurationMetadataTest test`: exit `0`; 11 tests, failures `0`, errors `0`; `BUILD SUCCESS`; log `/tmp/fw-plus-magicapi-task6-ai-directed.log`.
- `./fw-plus-core/mvnw -f fw-plus-sample/fw-plus-boot-samples/fw-plus-magicapi-spring-boot-starter-sample/pom.xml test`: exit `0`; 6 tests, failures `0`, errors `0`; `BUILD SUCCESS`; log `/tmp/fw-plus-magicapi-task6-sample-directed-preinstall-rerun.log`.
- `./fw-plus-core/mvnw -f fw-plus-core/pom.xml clean install`: exit `0`; 57 reactor entries; `BUILD SUCCESS`; log `/tmp/fw-plus-magicapi-task6-core-clean-install.log`.
- `./fw-plus-boot/mvnw -f fw-plus-boot/pom.xml clean install`: exit `0`; 48 reactor entries; `BUILD SUCCESS`; boot-autoconfigure 373 tests, failures `0`, errors `0`; log `/tmp/fw-plus-magicapi-task6-boot-clean-install.log`.
- `./fw-plus-cloud/mvnw -f fw-plus-cloud/pom.xml clean install`: exit `0`; 13 reactor entries; `BUILD SUCCESS`; log `/tmp/fw-plus-magicapi-task6-cloud-clean-install.log`.
- `./fw-plus-ai/mvnw -f fw-plus-ai/pom.xml clean install`: exit `1`; reactor `fw-plus-ai-boot-autoconfigure`; 92 tests, failures `1`, errors `0`; failed test `LangChain4jAutoConfigurationRegistrationTest.shouldKeepModelStarterProviderSdkDependenciesOptIn`; message `Model starter should depend on fw-plus-ai-boot-autoconfigure`; log `/tmp/fw-plus-magicapi-task6-ai-clean-install.log`.
- Not run after the AI build exit `1`: `./fw-plus-core/mvnw -f fw-plus-sample/pom.xml clean verify`; post-install target sample test; standalone `MagicapiSampleApplication`; health/editor HTTP checks; negative log scan; process termination; port release check.

## openspec/changes/fix-magicapi-plugin-sample-integration/design.md

- Source: openspec/changes/fix-magicapi-plugin-sample-integration/design.md
- Lines: 1-23
- SHA256: 1dd3467f682fc085ee1e46de8f182b30df405b68df2e1e7cc67907d6a5a0aeb2

```md
## Context

Boot 层当前有 25 个 Magic-API 插件模块，AI 层当前有 vector、model 2 个插件。Boot sample 只覆盖其中一部分，AI sample 又是独立应用，现有配置还会主动连接 Apollo、Redis、Mongo 等环境，整体启动结果不可重复。

## Root Cause

插件聚合器、sample 依赖和启动验证之间没有单一同步点。新增插件可以在各自模块内编译，但不会因为 sample 缺失或共同装配冲突而使构建失败。

## Fix

采用一个整体测试宿主：

1. 继续使用 `fw-plus-magicapi-spring-boot-starter-sample`，在其测试/验证 classpath 中显式纳入 Boot 25 个插件和 AI 2 个插件。
2. 为整体测试提供独立配置，使用随机端口和 worktree 内相对/临时资源目录，关闭 Apollo 启动拉取及需要真实连接的动态数据源，避免依赖外部服务。
3. 自动校验每个插件 artifact 均进入依赖树，并通过 Spring 上下文、Magic-API 插件元数据或对应关键 bean 验证可装配插件；受显式条件控制的插件同时验证条件报告。
4. 严格按 Core -> Boot -> Cloud/AI 顺序安装本地快照，再构建并启动 sample；启动后检查 Actuator 健康、Magic-API Web 入口和插件静态资源/管理入口。
5. 若某插件启动失败，按 debug gate 先定位根因、增加最小失败测试，再做单一源码修复。

## Non-Goals

- 不要求在本机启动 Kafka、Pulsar、RocketMQ、RabbitMQ、Redis、Mongo、Elasticsearch、InfluxDB、MQTT、S3 或 Debezium 数据源。
- 不执行依赖真实凭证和外部服务的数据读写业务测试。
- 不改变插件公开 API、配置键或运行时语义。

```

## openspec/changes/fix-magicapi-plugin-sample-integration/tasks.md

- Source: openspec/changes/fix-magicapi-plugin-sample-integration/tasks.md
- Lines: 1-3
- SHA256: 3dbe749b2a588bccc90876baf66e2e4810cd54d4bc0375cfd6367c2eb0194da1

```md
- [ ] 1. 建立 Boot 25 个插件、AI 2 个插件、宿主入口和现有测试覆盖矩阵，记录 sample 聚合验证与首次启动 RED 证据。
- [ ] 2. 补齐 sample 整体测试 classpath 和可控启动配置；对发现的每个源码缺陷先补最小回归测试，再逐项修复并转绿。
- [ ] 3. 按 Core -> Boot -> Cloud/AI -> sample 完成分层构建，运行相关测试，并实际启动 sample 检查健康、Magic-API 宿主及全部插件装配结果。

```

## Task 6 Verification Recovery (2026-07-28)

- The initial AI RED evidence above remains historical. Commit `de9b09ac0a4da4f52496f052dec7ea421b53ccc4` removed the obsolete direct `fw-plus-ai-boot-autoconfigure` dependency expectation from `LangChain4jAutoConfigurationRegistrationTest`; current verification HEAD `d327f910dfa06f1eb8472819c443bc5336e903db` contains that commit.
- `./fw-plus-ai/mvnw -f fw-plus-ai/pom.xml clean install`: exit `0`; 15 reactor entries; `BUILD SUCCESS`; fw-plus-ai-boot-autoconfigure 92 tests, failures `0`, errors `0`; log `/tmp/fw-plus-magicapi-task6-ai-clean-install-resume.log`.
- `./fw-plus-core/mvnw -f fw-plus-sample/pom.xml clean verify`: exit `0`; 14 reactor entries; `BUILD SUCCESS`; target Magic-API sample 6 tests, failures `0`, errors `0`; log `/tmp/fw-plus-magicapi-task6-sample-clean-verify.log`.
- `./fw-plus-core/mvnw -f fw-plus-sample/fw-plus-boot-samples/fw-plus-magicapi-spring-boot-starter-sample/pom.xml test`: exit `0`; 6 tests, failures `0`, errors `0`; `BUILD SUCCESS`; log `/tmp/fw-plus-magicapi-task6-sample-directed-postinstall.log`.
- Standalone command: `./fw-plus-core/mvnw -f fw-plus-sample/fw-plus-boot-samples/fw-plus-magicapi-spring-boot-starter-sample/pom.xml org.springframework.boot:spring-boot-maven-plugin:2.7.18:run -Dspring-boot.run.profiles=all-plugins -Dspring-boot.run.arguments="--server.port=39089 --magic-api.resource.location=<mktemp-empty-dir>"`; sample PID `78195`; port `39089`; ready seconds `18`; health body `{"status":"UP"}`; editor title `magic-api`; log `/var/folders/p7/cg70fqfj1xz7rwplv4n19d_w0000gp/T//fw-plus-magicapi-task6.XXXXXX.log`.
- Standalone negative scan pattern had no match; sample stopped by `TERM` within the bounded wait; `lsof -nP -iTCP:39089 -sTCP:LISTEN` had no output; the temporary resource directory cleanup returned successfully.
