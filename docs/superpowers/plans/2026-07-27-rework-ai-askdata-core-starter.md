---
change: rework-ai-askdata-core-starter
design-doc: docs/superpowers/specs/2026-07-27-rework-ai-askdata-core-starter-design.md
base-ref: 18be86e44a394e943c7177aa1cb7a1a27d8bcfb4
---

# fw-plus AskData Core Starter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将现有 `fw-plus-ai-askdata-spring-boot-starter` 重构为可二次开发、可扩展、可观测、可恢复的 SQLBot/WrenAI 问数后端 Starter。

**Architecture:** 采用渐进式六边形架构：先在 Core 层新增无 Spring 依赖的 AskData 契约和 SPI，再将现有自动配置中的 AskData 类型迁移为兼容桥接，最后补齐默认 LangChain4j/Ollama、状态存储、SQL 安全、监控日志、Remote Runtime 和文档。Core 只暴露 AskData 自有 DTO/SPI；Spring AI、Spring AI Alibaba、AgentScope Java 2.0+ 若不兼容 Java 8 / Spring Boot 2.7.18，则通过 Java 17 独立适配服务对接。

**Tech Stack:** Java 8, Spring Boot 2.7.18, Maven Wrapper, LangChain4j 0.35.0, JUnit, Spring Boot Test, MyBatis 风格 SQL 插件链, Mermaid 文档。

## Global Constraints

- 产物语言使用简体中文。
- Core AskData 契约必须兼容 Java 8，禁止 Java 9+ API。
- 默认 Starter 必须兼容 Spring Boot 2.7.18。
- Core 不暴露 Spring、LangChain4j、Ollama、LangGraph、Spring AI、Spring AI Alibaba、AgentScope Java 或存储厂商类型。
- 默认模型和向量厂商为 Ollama：聊天 `qwen3:32b`，向量 `bge-m3`。
- 项目是第一分区，认证操作人是第二分区；会话、记忆、推荐、审计和监控日志均绑定 `projectId + operatorId`。
- 单机默认内存状态；集群模式必须拒绝本地内存状态和本地内存向量。
- SQL 插件不得获得裸连接，不得绕过最终 SQL 安全校验。
- 每次对话必须记录脱敏监控日志，管理员访问受授权策略控制。
- SQLBot 功能优先，WrenAI 企业级 GenBI 能力必须覆盖；功能状态以 `openspec/changes/rework-ai-askdata-core-starter/feature-list.md` 和正式文档追踪矩阵为准。
- 源码公开接口、SPI、安全、恢复、权限、扩展逻辑必须写详细中文注释。
- 每个任务完成后运行对应测试并只勾选已验证任务。

---

## File Structure

### 新增 Core 模块

- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/pom.xml`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/pom.xml`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`

### Core 包结构

- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/api/`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/context/`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/capability/`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/model/`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/runtime/`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/storage/`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/sql/`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/agent/`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/memory/`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/observability/`

### AI 自动配置迁移

- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/autoconfigure/AskDataAutoConfiguration.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/autoconfigure/LangChain4jModelAskDataAutoConfiguration.java`
- Modify: existing files under `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/`
- Modify: tests under `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/`

### Starter 与 BOM

- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter/pom.xml`

### 文档

- Create: `docs/askdata/starter/index.md`
- Create: `docs/askdata/starter/configuration.md`
- Create: `docs/askdata/starter/runtime-replacement.md`
- Create: `docs/askdata/starter/java17-remote-runtime.md`
- Create: `docs/askdata/starter/sql-safety.md`
- Create: `docs/askdata/starter/storage-matrix.md`
- Create: `docs/askdata/starter/database-design.md`
- Create: `docs/askdata/starter/flow-diagrams.md`
- Create: `docs/askdata/starter/test-cases.md`
- Create: `docs/askdata/starter/sqlbot-wrenai-feature-matrix.md`

---

### Task 1: 新增 Core AskData Maven 模块

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/pom.xml`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/pom.xml`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`

**Interfaces:**
- Produces: Maven artifact `com.xiongx9527.fw.plus:fw-plus-ai-askdata`
- Consumes: existing Core parent and dependency management

- [x] **Step 1: 添加失败验证**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-askdata -DskipTests compile
```

Expected: FAIL，因为模块尚不存在或未纳入聚合。

- [x] **Step 2: 创建模块 POM**

Create `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/pom.xml` with parent consistent with sibling modules and no Spring/LangChain4j dependencies. The module must only depend on Java 8-compatible Core utilities when needed.

- [x] **Step 3: 注册聚合与 BOM**

Modify `fw-plus-core/fw-plus-project/fw-plus-ai/pom.xml` to include `fw-plus-ai-askdata`.

Modify `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml` to expose the new artifact version using `${revision}`.

- [x] **Step 4: 验证模块可编译**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-askdata -DskipTests compile
```

Expected: PASS.

- [x] **Step 5: Commit Task 1**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai/pom.xml \
  fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/pom.xml \
  fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml
git commit -m "feat: add askdata core module"
```

### Task 2: 定义 Core API 与上下文对象

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/api/AskDataService.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/api/AskDataRequest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/api/AskDataResult.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/api/AskDataError.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/context/AskDataProjectContext.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/test/java/com/xiongx9527/fw/plus/ai/askdata/api/AskDataApiContractTest.java`

**Interfaces:**
- Produces: `AskDataService#ask(AskDataRequest)`, `AskDataRequest`, `AskDataResult`, `AskDataError`, `AskDataProjectContext`
- Consumes: no Spring types

- [x] **Step 1: 写失败测试 Task 2**

Create `AskDataApiContractTest` asserting:

- `AskDataRequest` rejects blank `question`.
- `AskDataProjectContext` requires non-blank `projectId` and `operatorId`.
- Public API class names do not start with `org.springframework`, `dev.langchain4j`, `io.agentscope`, or `org.springframework.ai`.

- [x] **Step 2: 运行失败测试 Task 2**

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-askdata -Dtest=AskDataApiContractTest test
```

Expected: FAIL because classes do not exist.

- [x] **Step 3: 实现最小 API Task 2**

Implement Java 8 POJOs with explicit constructors, getters, validation helpers, and detailed Chinese Javadoc. Avoid Lombok and Java 9+ collection factories.

- [x] **Step 4: 运行测试 Task 2**

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-askdata -Dtest=AskDataApiContractTest test
```

Expected: PASS.

- [x] **Step 5: Commit Task 2**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata
git commit -m "feat: define askdata core api contracts"
```

### Task 3: 定义能力注册表与版本协商

**Files:**
- Create: `.../askdata/capability/AskDataCapabilityType.java`
- Create: `.../askdata/capability/AskDataCapabilityDescriptor.java`
- Create: `.../askdata/capability/AskDataCapabilityProvider.java`
- Create: `.../askdata/capability/AskDataCapabilityRegistry.java`
- Create: `.../askdata/capability/DefaultAskDataCapabilityRegistry.java`
- Test: `.../askdata/capability/AskDataCapabilityRegistryTest.java`

**Interfaces:**
- Produces: deterministic selection for models, vectors, SQL plugins, stores, runtime adapters, observability exporters
- Consumes: Core API from Task 2

- [x] **Step 1: 写失败测试 Task 3**

Test duplicate key rejection, incompatible version rejection, priority ordering, unknown key diagnostic, and cluster-incompatible capability rejection.

- [x] **Step 2: 运行失败测试 Task 3**

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-askdata -Dtest=AskDataCapabilityRegistryTest test
```

Expected: FAIL.

- [x] **Step 3: 实现能力注册表 Task 3**

Use immutable defensive copies, Java 8 collections, and Chinese Javadoc explaining why Core only sees descriptors and not vendor clients.

- [x] **Step 4: 验证 Task 3**

Run the same test. Expected: PASS.

- [x] **Step 5: Commit Task 3**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/capability \
  fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/test/java/com/xiongx9527/fw/plus/ai/askdata/capability
git commit -m "feat: add askdata capability registry"
```

### Task 4: 定义模型路由与 Remote Runtime SPI

**Files:**
- Create: `.../askdata/model/AskDataModelRequest.java`
- Create: `.../askdata/model/AskDataModelResult.java`
- Create: `.../askdata/model/AskDataModelRouter.java`
- Create: `.../askdata/model/AskDataModelRoutePolicy.java`
- Create: `.../askdata/runtime/AskDataRuntimeClient.java`
- Create: `.../askdata/runtime/AskDataRemoteRuntimeRequest.java`
- Create: `.../askdata/runtime/AskDataRemoteRuntimeResult.java`
- Test: `.../askdata/model/AskDataModelRouterTest.java`
- Test: `.../askdata/runtime/AskDataRemoteRuntimeContractTest.java`

**Interfaces:**
- Produces: model routing SPI and Java 17 remote runtime boundary
- Consumes: `AskDataProjectContext`, capability registry

- [x] **Step 1: 写失败测试 Task 4**

Cover route precedence: request model key, project key, datasource key, agent task key, default key. Cover infrastructure failure fallback and non-retryable rejection. Cover Remote Runtime payload includes `projectId`, `operatorId`, `conversationId`, `requestId`, `capabilityKey`, and audit correlation ID.

- [x] **Step 2: 运行失败测试 Task 4**

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-askdata -Dtest=AskDataModelRouterTest,AskDataRemoteRuntimeContractTest test
```

Expected: FAIL.

- [x] **Step 3: 实现 SPI Task 4**

Implement interfaces and default route value objects only. Do not introduce HTTP client yet; Remote Runtime is a Core contract.

- [x] **Step 4: 验证 Task 4**

Run the same tests. Expected: PASS.

- [x] **Step 5: Commit Task 4**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/model \
  fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/runtime \
  fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/test/java/com/xiongx9527/fw/plus/ai/askdata/model \
  fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/test/java/com/xiongx9527/fw/plus/ai/askdata/runtime
git commit -m "feat: add askdata model routing contracts"
```

### Task 5: 定义存储、检查点、Agent 状态和监控日志 SPI

**Files:**
- Create: `.../askdata/storage/AskDataConversationStore.java`
- Create: `.../askdata/storage/AskDataCheckpointStore.java`
- Create: `.../askdata/storage/AskDataAgentStateStore.java`
- Create: `.../askdata/storage/AskDataMonitoringLogStore.java`
- Create: `.../askdata/storage/InMemoryAskDataStores.java`
- Test: `.../askdata/storage/AskDataStoreContractTest.java`

**Interfaces:**
- Produces: memory default for single-node, explicit cluster rejection contract
- Consumes: `AskDataProjectContext`

- [x] **Step 1: 写失败测试 Task 5**

Test stores isolate by `projectId + operatorId`, reject cluster mode with memory implementation, preserve checkpoint version, and never mark uncertain SQL execution as replayable.

- [x] **Step 2: 运行失败测试 Task 5**

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-askdata -Dtest=AskDataStoreContractTest test
```

Expected: FAIL.

- [x] **Step 3: 实现 SPI 与内存实现 Task 5**

Use synchronized maps or concurrent maps with bounded cleanup where needed. Add Chinese Javadoc explaining memory is for single-node only.

- [x] **Step 4: 验证 Task 5**

Run the same test. Expected: PASS.

- [x] **Step 5: Commit Task 5**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/storage \
  fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/test/java/com/xiongx9527/fw/plus/ai/askdata/storage
git commit -m "feat: add askdata state storage contracts"
```

### Task 6: 定义 SQL 命令、插件链和安全校验契约

**Files:**
- Create: `.../askdata/sql/AskDataSqlCommand.java`
- Create: `.../askdata/sql/AskDataSqlParameter.java`
- Create: `.../askdata/sql/AskDataSqlInterceptor.java`
- Create: `.../askdata/sql/AskDataSqlInterceptorChain.java`
- Create: `.../askdata/sql/AskDataSqlValidator.java`
- Create: `.../askdata/sql/AskDataSqlAudit.java`
- Test: `.../askdata/sql/AskDataSqlPipelineTest.java`

**Interfaces:**
- Produces: MyBatis-like SQL plugin SPI with final safety boundary
- Consumes: project/operator context and metadata descriptors

- [x] **Step 1: 写失败测试 Task 6**

Cover plugin ordering, named parameter binding, multi-statement rejection, write SQL rejection, unauthorized object rejection, and final validation after plugin rewrite.

- [x] **Step 2: 运行失败测试 Task 6**

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-askdata -Dtest=AskDataSqlPipelineTest test
```

Expected: FAIL.

- [x] **Step 3: 实现管线契约 Task 6**

Implement value objects and chain orchestration. Keep actual SQL parser pluggable; define validator result and failure categories now.

- [x] **Step 4: 验证 Task 6**

Run the same test. Expected: PASS.

- [x] **Step 5: Commit Task 6**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/sql \
  fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/test/java/com/xiongx9527/fw/plus/ai/askdata/sql
git commit -m "feat: add askdata sql pipeline contracts"
```

### Task 7: 定义多提问、长期记忆和按人推荐契约

**Files:**
- Create: `.../askdata/agent/AskDataQuestionPlan.java`
- Create: `.../askdata/agent/AskDataQuestionPlanner.java`
- Create: `.../askdata/memory/AskDataLongTermMemory.java`
- Create: `.../askdata/memory/AskDataMemoryEntry.java`
- Create: `.../askdata/memory/AskDataRecommendationService.java`
- Test: `.../askdata/memory/AskDataMemoryAndRecommendationTest.java`

**Interfaces:**
- Produces: multi-question plan, long-term memory and operator-scoped recommendation contracts
- Consumes: `AskDataProjectContext`

- [x] **Step 1: 写失败测试 Task 7**

Cover single question, explicit multi-question list, compound question plan, memory filtering by project/operator/datasource/semantic version, and recommendation isolation between operators.

- [x] **Step 2: 运行失败测试 Task 7**

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-askdata -Dtest=AskDataMemoryAndRecommendationTest test
```

Expected: FAIL.

- [x] **Step 3: 实现契约 Task 7**

Implement interfaces and simple in-memory reference implementation for contract tests. Document that recommendation may return generic non-personal suggestions only when operator identity is missing and policy permits it.

- [x] **Step 4: 验证 Task 7**

Run the same test. Expected: PASS.

- [x] **Step 5: Commit Task 7**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/agent \
  fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/memory \
  fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/test/java/com/xiongx9527/fw/plus/ai/askdata/memory
git commit -m "feat: add askdata memory and recommendation contracts"
```

### Task 8: 定义观测事件和管理员监控日志契约

**Files:**
- Create: `.../askdata/observability/AskDataEvent.java`
- Create: `.../askdata/observability/AskDataEventPublisher.java`
- Create: `.../askdata/observability/AskDataMonitoringLog.java`
- Create: `.../askdata/observability/AskDataMonitoringLogAccessPolicy.java`
- Test: `.../askdata/observability/AskDataObservabilityContractTest.java`

**Interfaces:**
- Produces: vendor-neutral observability contracts and admin access policy
- Consumes: storage SPI

- [x] **Step 1: 写失败测试 Task 8**

Assert lifecycle events exist for conversation start, question planning, model route, agent node, SQL validation/execution, checkpoint, recovery, completion and failure. Assert project admin can read project logs and normal operator cannot read admin logs by default.

- [x] **Step 2: 运行失败测试 Task 8**

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-askdata -Dtest=AskDataObservabilityContractTest test
```

Expected: FAIL.

- [x] **Step 3: 实现观测契约 Task 8**

Implement event DTOs and access policy interfaces. Do not depend on LangSmith, Langfuse or OpenTelemetry in Core.

- [x] **Step 4: 验证 Task 8**

Run the same test. Expected: PASS.

- [x] **Step 5: Commit Task 8**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/observability \
  fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/test/java/com/xiongx9527/fw/plus/ai/askdata/observability
git commit -m "feat: add askdata observability contracts"
```

### Task 9: 将现有自动配置桥接到 Core 契约

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/autoconfigure/AskDataAutoConfiguration.java`
- Modify: files under `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/`
- Modify: tests under `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/`

**Interfaces:**
- Consumes: Core contracts from Tasks 2-8
- Produces: Spring Boot auto-configured beans that implement Core SPI

- [x] **Step 1: 写/调整失败测试 Task 9**

Update existing tests so they import Core API where appropriate. Add a test asserting business-provided `AskDataService` still wins over default auto-configuration.

- [x] **Step 2: 运行失败测试 Task 9**

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=AskDataAutoConfigurationTest,DefaultAskDataServiceTest test
```

Expected: FAIL while imports and adapters are not migrated.

- [x] **Step 3: 添加 Core 依赖并迁移桥接 Task 9**

Add dependency on `fw-plus-ai-askdata`. Move or wrap existing auto-config DTOs so external consumers see Core types. Keep compatibility constructors or adapters for old tests when needed.

- [x] **Step 4: 验证 Task 9**

Run all AskData auto-configure tests:

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest='*AskData*Test' test
```

Expected: PASS.

- [x] **Step 5: Commit Task 9**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure
git commit -m "refactor: bridge askdata autoconfigure to core contracts"
```

### Task 10: 实现默认模型路由、兜底和 Ollama 配置

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/AskDataProperties.java`
- Create/Modify: model routing classes under auto-configure `askdata/model/`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/model/LangChain4jAskDataModelRegistryTest.java`

**Interfaces:**
- Consumes: Core model routing SPI
- Produces: default LangChain4j/Ollama route implementation

- [x] **Step 1: 写失败测试 Task 10**

Test default Ollama chat and embedding logical keys, explicit missing key error, infrastructure fallback, non-retryable no-switch behavior, and audit route record.

- [x] **Step 2: 运行失败测试 Task 10**

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=LangChain4jAskDataModelRegistryTest test
```

Expected: FAIL.

- [x] **Step 3: 实现默认配置和路由 Task 10**

Add properties for model route policy, candidates, fallback, timeout classification, and default Ollama labels. Keep provider-specific classes outside Core.

- [x] **Step 4: 验证 Task 10**

Run the same test and AskData property binding tests. Expected: PASS.

- [x] **Step 5: Commit Task 10**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure
git commit -m "feat: add askdata model routing defaults"
```

### Task 11: 实现默认状态、监控日志和集群保护

**Files:**
- Modify: `AskDataAutoConfiguration.java`
- Create/Modify: auto-configure storage adapters under `askdata/conversation/` and `askdata/observability/`
- Test: `AskDataConversationStoreTest.java`
- Test: new `AskDataMonitoringLogStoreTest.java`

**Interfaces:**
- Consumes: Core storage and observability contracts
- Produces: single-node memory defaults and cluster rejection diagnostics

- [ ] **Step 1: 写失败测试**

Test memory default in single-node mode, startup failure in cluster mode with memory state, monitoring log lifecycle event persistence, and admin access policy.

- [ ] **Step 2: 运行失败测试**

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=AskDataConversationStoreTest,AskDataMonitoringLogStoreTest test
```

Expected: FAIL.

- [ ] **Step 3: 实现默认存储和诊断**

Wire memory stores with bounded retention properties. Add startup validator that fails when cluster mode uses local memory for required state categories.

- [ ] **Step 4: 验证**

Run the same tests. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure
git commit -m "feat: add askdata state and monitoring defaults"
```

### Task 12: 实现 SQL 插件链桥接与安全测试

**Files:**
- Modify: existing auto-configure SQL validator/executor classes under `askdata/sql/`
- Test: `DefaultAskDataSqlValidatorTest.java`
- Test: `JdbcAskDataSqlExecutorTest.java`
- Test: new `AskDataSqlInterceptorAutoConfigurationTest.java`

**Interfaces:**
- Consumes: Core SQL pipeline
- Produces: Spring-discovered SQL interceptors and final safety validation

- [ ] **Step 1: 写失败测试**

Test custom interceptor adds tenant predicate through named parameters, final validator rejects unsafe plugin output, and audit records interceptor key and phase.

- [ ] **Step 2: 运行失败测试**

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=DefaultAskDataSqlValidatorTest,JdbcAskDataSqlExecutorTest,AskDataSqlInterceptorAutoConfigurationTest test
```

Expected: FAIL.

- [ ] **Step 3: 实现桥接**

Use `ObjectProvider<AskDataSqlInterceptor>` to collect ordered interceptors. Ensure final SQL always calls validator after plugins.

- [ ] **Step 4: 验证**

Run the same tests. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/sql \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/sql
git commit -m "feat: wire askdata sql interceptor pipeline"
```

### Task 13: 实现多提问、记忆和推荐默认桥接

**Files:**
- Modify: `DefaultAskDataFlowExecutor` and related flow classes
- Create/Modify: auto-configure memory/recommendation adapters
- Test: `DefaultAskDataFlowExecutorTest.java`
- Test: `AskDataResumeFlowTest.java`
- Test: new `AskDataMultiQuestionFlowTest.java`

**Interfaces:**
- Consumes: Core planner, memory, recommendation contracts
- Produces: flow support for single, explicit multi-question, compound question plan

- [ ] **Step 1: 写失败测试**

Test explicit two-question request creates two audited child results. Test partial failure returns one success and one failure. Test memory recall filters another operator's entry.

- [ ] **Step 2: 运行失败测试**

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=DefaultAskDataFlowExecutorTest,AskDataResumeFlowTest,AskDataMultiQuestionFlowTest test
```

Expected: FAIL.

- [ ] **Step 3: 实现默认桥接**

Add planner and aggregation hooks without changing default single-question behavior. Use project/operator context in all store calls.

- [ ] **Step 4: 验证**

Run the same tests. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata
git commit -m "feat: add askdata multi-question memory flow"
```

### Task 14: Starter POM、BOM 和兼容验证

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/pom.xml` if module ordering requires it

**Interfaces:**
- Produces: one starter that includes Core contract, LangChain4j default adapter, and autoconfiguration
- Consumes: all previous tasks

- [ ] **Step 1: 写失败验证**

Run:

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -DskipTests package
```

Expected before wiring: FAIL if dependency is missing.

- [ ] **Step 2: 更新依赖**

Add Core `fw-plus-ai-askdata` dependency to BOM and starter dependency tree. Do not add Spring AI, Spring AI Alibaba, AgentScope Java or Java 17-only dependencies.

- [ ] **Step 3: 验证**

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -DskipTests package
```

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter/pom.xml
git commit -m "build: wire askdata starter dependencies"
```

### Task 15: 编写 Starter 说明文档、流程图和数据库设计

**Files:**
- Create: `docs/askdata/starter/index.md`
- Create: `docs/askdata/starter/configuration.md`
- Create: `docs/askdata/starter/runtime-replacement.md`
- Create: `docs/askdata/starter/java17-remote-runtime.md`
- Create: `docs/askdata/starter/sql-safety.md`
- Create: `docs/askdata/starter/storage-matrix.md`
- Create: `docs/askdata/starter/database-design.md`
- Create: `docs/askdata/starter/flow-diagrams.md`

**Interfaces:**
- Consumes: Design Doc and implemented public API
- Produces: detailed Chinese documentation required by the spec

- [ ] **Step 1: 写文档结构**

Create the listed files with Chinese sections matching the Design Doc. Include Mermaid diagrams for business flow, technical flow, model fallback, Java 17 remote runtime, SQL safety, recovery, monitoring and external integrations.

- [ ] **Step 2: 写数据库设计**

In `database-design.md`, document logical tables for project, member, datasource, metadata, MDL, training assets, conversation, memory, recommendation, SQL audit, monitoring log, agent state, model config, MCP/API, enterprise integration and plugin registry.

- [ ] **Step 3: 写 Java 17 对接文档**

In `java17-remote-runtime.md`, document official compatibility verification, remote deployment, capability registration, HTTP/SSE, MCP, A2A, gRPC, message queue options, auth, context payload, error mapping, audit and rollback.

- [ ] **Step 4: 检查文档未完成标记**

```bash
rg -n "T[B]D|T[O]DO|待[补]|占[位]" docs/askdata/starter
```

Expected: no output.

- [ ] **Step 5: Commit**

```bash
git add docs/askdata/starter
git commit -m "docs: add askdata starter documentation"
```

### Task 16: 编写 SQLBot/WrenAI 功能矩阵和测试用例文档

**Files:**
- Create: `docs/askdata/starter/sqlbot-wrenai-feature-matrix.md`
- Create: `docs/askdata/starter/test-cases.md`
- Modify: `openspec/changes/rework-ai-askdata-core-starter/feature-list.md` if implementation status changes

**Interfaces:**
- Consumes: `feature-list.md`
- Produces: formal implementation and verification matrix

- [ ] **Step 1: 生成矩阵**

Transform every item in `feature-list.md` into a row with fields: product, category, feature, source, priority, duplicatePolicy, change, module, apiOrConfig, storage, docs, tests, status, limitation.

- [ ] **Step 2: 写测试用例**

Document unit, contract, auto-configuration, SQL safety, storage consistency, model fallback, multi-agent/recovery, memory/recommendation permissions, monitoring audit, Java 17 remote runtime and frontend integration test cases.

- [ ] **Step 3: 检查缺项**

```bash
rg -n "SQLBot|WrenAI|Java 17|DataEase|MCP|Dify|Coze|n8n|企业微信|钉钉|飞书" docs/askdata/starter/sqlbot-wrenai-feature-matrix.md
```

Expected: output includes all required product/integration names.

- [ ] **Step 4: Commit**

```bash
git add docs/askdata/starter/sqlbot-wrenai-feature-matrix.md \
  docs/askdata/starter/test-cases.md \
  openspec/changes/rework-ai-askdata-core-starter/feature-list.md
git commit -m "docs: add askdata feature matrix and test cases"
```

### Task 17: 运行聚合验证并更新 OpenSpec 任务状态

**Files:**
- Modify: `openspec/changes/rework-ai-askdata-core-starter/tasks.md`
- Modify: documentation status rows if implementation evidence changes

**Interfaces:**
- Consumes: all previous task deliverables
- Produces: checked tasks and verification evidence

- [ ] **Step 1: 运行 Core 测试**

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-askdata test
```

Expected: PASS.

- [ ] **Step 2: 运行 AI 自动配置 AskData 测试**

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest='*AskData*Test' test
```

Expected: PASS.

- [ ] **Step 3: 运行 Starter 打包**

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -DskipTests package
```

Expected: PASS.

- [ ] **Step 4: 运行文档检查**

```bash
rg -n "T[B]D|T[O]DO|待[补]|占[位]" docs/askdata/starter docs/superpowers/specs/2026-07-27-rework-ai-askdata-core-starter-design.md
```

Expected: no output.

- [ ] **Step 5: 勾选 OpenSpec tasks**

Only check tasks in `openspec/changes/rework-ai-askdata-core-starter/tasks.md` that have matching code, docs and test evidence.

- [ ] **Step 6: Commit**

```bash
git add openspec/changes/rework-ai-askdata-core-starter/tasks.md docs/askdata/starter
git commit -m "chore: record askdata implementation verification"
```

## Self-Review

- Spec coverage: The plan maps Core contracts, storage, runtime replacement, Java 17 remote runtime, SQL safety, monitoring, docs, matrix and tests to explicit tasks.
- Placeholder scan: This plan contains no unresolved placeholder entries.
- Type consistency: Public names use `AskData*` Core contracts consistently across tasks.
- Scope control: This plan implements the backend Starter first and records frontend/API/MCP/enterprise items in matrices and docs without claiming they are complete in this change.
