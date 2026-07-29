---
change: add-askdata-governance-permission-bridge
design-doc: docs/superpowers/specs/2026-07-29-add-askdata-governance-permission-bridge-design.md
base-ref: d9bc76941b3eee14277238dedaf3f7146fd78d60
---

# AskData Governance Permission Bridge Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 AskData Core/Starter 增加默认拒绝、可业务覆盖、贯穿元数据/SQL/观测的治理权限桥接。

**Architecture:** Core 新增无 Spring 依赖的治理授权模型和 SPI；Starter 自动配置默认拒绝实现并将业务 Bean 注入默认 Flow。授权摘要作为单一事实源贯穿元数据过滤、SQL 插件链、最终 SQL 校验、只读执行和管理员日志查询。

**Tech Stack:** Java 8, Maven Wrapper, Spring Boot 2.7.18 自动配置, JUnit Jupiter, AskData Core SPI, LangChain4j 默认 Starter 边界。

## Global Constraints

- Core 公共 API 必须 Java 8 兼容，不使用 Java 9+ API。
- Core 公共 API 不暴露 Spring、JustAuth、Sa-Token、Spring Security、LangChain4j、Spring AI 或宿主 IAM 类型。
- 默认治理实现必须拒绝受保护数据访问，不提供生产全局放行属性。
- SQL 插件只能收窄授权摘要，不能扩大、清空或替换项目、操作人、参数、授权对象和行级谓词。
- 外部企业 IAM、MCP、DataEase、Dify、Coze、n8n、企业微信、钉钉、飞书真实联调不属于本计划。

---

## File Structure

- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/security/AskDataAuthorizationRequest.java`：治理授权请求。
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/security/AskDataAuthorizationSummary.java`：不可变授权摘要。
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/security/AskDataAuthorizedObject.java`：数据源/schema/table/column 授权对象。
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/security/AskDataRowPredicate.java`：行级谓词和具名参数。
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/security/AskDataGovernanceProvider.java`：治理 SPI。
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/security/DefaultDenyAskDataGovernanceProvider.java`：默认拒绝实现。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/sql/AskDataSqlCommand.java`：携带授权摘要并保持不可变。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/sql/AskDataSqlInterceptorChain.java`：插件后验证授权摘要未扩大。
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/autoconfigure/AskDataAutoConfiguration.java`：装配治理 Bean 并注入 Flow/日志查询。
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/flow/DefaultAskDataFlowExecutor.java`：模型调用前解析授权摘要并贯穿执行。
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/observability/AskDataMonitoringLogQueryService.java`：管理员日志查询授权。
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/test/java/com/xiongx9527/fw/plus/ai/askdata/security/AskDataGovernanceProviderTest.java`。
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/test/java/com/xiongx9527/fw/plus/ai/askdata/sql/AskDataSqlPipelineTest.java`。
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/autoconfigure/AskDataAutoConfigurationTest.java`。
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/flow/DefaultAskDataFlowExecutorTest.java`。
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/observability/AskDataMonitoringLogQueryServiceTest.java`。
- Modify docs: `docs/askdata/starter/sql-safety.md`, `docs/askdata/starter/configuration.md`, `docs/askdata/starter/plugin-sdk.md`, `docs/askdata/starter/sqlbot-wrenai-feature-matrix.md`。

### Task 1: Core Governance Model

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/security/AskDataAuthorizationRequest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/security/AskDataAuthorizedObject.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/security/AskDataRowPredicate.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/security/AskDataAuthorizationSummary.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/test/java/com/xiongx9527/fw/plus/ai/askdata/security/AskDataGovernanceProviderTest.java`

**Interfaces:**
- Consumes: `AskDataProjectContext`.
- Produces: `AskDataAuthorizationSummary.isAllowed()`, `getAuthorizedObjects()`, `getRowPredicates()`, `containsObject(AskDataAuthorizedObject)`.

- [x] **Step 1: Write failing Core model tests**

Add tests asserting `AskDataAuthorizationSummary.deny(...)` rejects, `allow(...)` copies object and predicate lists defensively, and `containsObject` matches datasource/table/column.

Run: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-askdata -Dtest=AskDataGovernanceProviderTest test`
Expected: FAIL because the new classes do not exist.

- [x] **Step 2: Implement immutable request, object, predicate and summary classes**

Use final fields, constructor validation, defensive copies, `Collections.unmodifiableList`, and Java 8-compatible helper methods. `AskDataRowPredicate` must store SQL fragment plus `List<AskDataSqlParameter>` rather than raw dynamic values.

- [x] **Step 3: Run Core model tests**

Run: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-askdata -Dtest=AskDataGovernanceProviderTest test`
Expected: PASS.

- [x] **Step 4: Commit Task 1**

Run: `git add fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/security fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/test/java/com/xiongx9527/fw/plus/ai/askdata/security/AskDataGovernanceProviderTest.java && git commit -m "feat(askdata): add governance authorization model"`

### Task 2: Core Governance Provider and SQL Boundary

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/security/AskDataGovernanceProvider.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/security/DefaultDenyAskDataGovernanceProvider.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/sql/AskDataSqlCommand.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/sql/AskDataSqlInterceptorChain.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/test/java/com/xiongx9527/fw/plus/ai/askdata/sql/AskDataSqlPipelineTest.java`

**Interfaces:**
- Consumes: Task 1 model classes.
- Produces: `AskDataGovernanceProvider.authorize(AskDataAuthorizationRequest)`, `AskDataSqlCommand.getAuthorizationSummary()`, `AskDataSqlCommand.withAuthorizationSummary(...)`.

- [x] **Step 1: Write failing SQL authorization tests**

Add tests for default deny provider, command carrying authorization summary, interceptor attempting to replace summary with broader objects, and final chain rejecting the broader result.

Run: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-askdata -Dtest='AskDataGovernanceProviderTest,AskDataSqlPipelineTest' test`
Expected: FAIL for missing provider and command authorization APIs.

- [x] **Step 2: Implement provider and command authorization APIs**

`DefaultDenyAskDataGovernanceProvider.authorize` returns a deny summary with stable code such as `ASKDATA_GOVERNANCE_DENIED`. `AskDataSqlCommand` constructors retain binary compatibility by adding overloaded constructors and defaulting missing summary to `null` only for legacy tests that do not require authorization.

- [x] **Step 3: Harden interceptor chain**

After each plugin, rebuild from the original command preserving context, parameters and authorization summary. If a plugin returns a command whose summary contains objects not present in the original summary or removes required row predicates, throw `IllegalArgumentException` before final validation.

- [x] **Step 4: Run Core SQL tests**

Run: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-askdata -Dtest='AskDataGovernanceProviderTest,AskDataSqlPipelineTest' test`
Expected: PASS.

- [x] **Step 5: Commit Task 2**

Run: `git add fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/security fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/main/java/com/xiongx9527/fw/plus/ai/askdata/sql fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata/src/test/java/com/xiongx9527/fw/plus/ai/askdata && git commit -m "fix(askdata): enforce governance boundaries in sql pipeline"`

### Task 3: Starter Autoconfiguration and Flow Integration

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/autoconfigure/AskDataAutoConfiguration.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/flow/DefaultAskDataFlowExecutor.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/CoreAskDataServiceAdapter.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/autoconfigure/AskDataAutoConfigurationTest.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/flow/DefaultAskDataFlowExecutorTest.java`

**Interfaces:**
- Consumes: `AskDataGovernanceProvider` from Task 2.
- Produces: default deny Bean, Flow constructor overload accepting governance provider, authorization failure before model call.

- [x] **Step 1: Write failing Starter tests**

Add tests asserting default governance provider Bean exists, custom Bean wins, missing authorization rejects before model registry is invoked, and authorized summary reaches SQL command creation.

Run: `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest='AskDataAutoConfigurationTest,DefaultAskDataFlowExecutorTest' test`
Expected: FAIL until autoconfiguration and Flow constructor are updated.

- [x] **Step 2: Wire default and custom governance providers**

Add an `@Bean @ConditionalOnMissingBean` returning `DefaultDenyAskDataGovernanceProvider`. Update `DefaultAskDataFlowExecutor` Bean construction to pass the provider. Preserve existing constructor overloads by delegating to the new full constructor.

- [x] **Step 3: Reject unauthorized requests before model invocation**

At the start of `DefaultAskDataFlowExecutor.execute`, create `AskDataAuthorizationRequest`, call provider, and return controlled `AskDataResult` failure when denied. Record a step log or event summary without credentials or raw parameter values.

- [x] **Step 4: Run Starter flow tests**

Run: `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest='AskDataAutoConfigurationTest,DefaultAskDataFlowExecutorTest' test`
Expected: PASS.

- [x] **Step 5: Commit Task 3**

Run: `git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata && git commit -m "feat(askdata): wire governance provider into starter flow"`

### Task 4: Metadata, Row Predicate and Observability Access

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/metadata/AskDataMetadataProvider.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/metadata/EmptyAskDataMetadataProvider.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/observability/AskDataMonitoringLogQueryService.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/observability/AskDataMonitoringLogQueryServiceTest.java`

**Interfaces:**
- Consumes: `AskDataAuthorizationSummary`.
- Produces: authorization-aware metadata overload and admin monitoring log access checks.

- [x] **Step 1: Write failing metadata and monitoring tests**

Add tests asserting unauthorized columns are filtered before Prompt metadata, project admin can query only its project, platform admin receives only explicit scope, and ordinary operator is denied.

Run: `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest='AskDataMonitoringLogQueryServiceTest,DefaultAskDataFlowExecutorTest' test`
Expected: FAIL for missing authorization-aware metadata/log behavior.

- [x] **Step 2: Add metadata overload without breaking existing providers**

Add a default adapter path so existing `AskDataMetadataProvider.load(AskDataRequest)` implementations still work, while Flow can call an authorization-aware helper that filters returned metadata against `AskDataAuthorizationSummary`.

- [x] **Step 3: Wire monitoring log authorization**

Make `AskDataMonitoringLogQueryService` consult governance access scope before returning logs. Ordinary operators receive an empty or denied result according to existing service error style; administrator queries are audited through monitoring events or audit store.

- [x] **Step 4: Run observability tests**

Run: `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest='AskDataMonitoringLogQueryServiceTest,DefaultAskDataFlowExecutorTest' test`
Expected: PASS.

- [x] **Step 5: Commit Task 4**

Run: `git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata && git commit -m "fix(askdata): apply governance to metadata and monitoring access"`

### Task 5: Documentation and OpenSpec Task Evidence

**Files:**
- Modify: `docs/askdata/starter/sql-safety.md`
- Modify: `docs/askdata/starter/configuration.md`
- Modify: `docs/askdata/starter/plugin-sdk.md`
- Modify: `docs/askdata/starter/sqlbot-wrenai-feature-matrix.md`
- Modify: `openspec/changes/add-askdata-governance-permission-bridge/tasks.md`

**Interfaces:**
- Consumes: completed Tasks 1-4 behavior.
- Produces: updated docs and checked OpenSpec implementation tasks.

- [x] **Step 1: Update docs with exact integration guidance**

Document `AskDataGovernanceProvider`, default deny behavior, development test Bean pattern, row predicate named parameters, admin monitoring log scope, and explicit non-goals for external systems.

- [x] **Step 2: Update feature matrix statuses**

Move governance-related rows to “默认拒绝 + 业务桥接可验证” or equivalent wording. Keep enterprise platform, DataEase, Dify, Coze, n8n and MCP real integrations marked as business integration contract or future change scope.

- [x] **Step 3: Check OpenSpec tasks that have completed implementation evidence**

Mark tasks 1.1 through 5.3 only after the corresponding code/doc tests pass. Do not check verification tasks before running commands in Task 6.

- [x] **Step 4: Run docs diff check**

Run: `git diff --check -- docs/askdata/starter openspec/changes/add-askdata-governance-permission-bridge`
Expected: exit code 0.

- [x] **Step 5: Commit Task 5**

Run: `git add docs/askdata/starter openspec/changes/add-askdata-governance-permission-bridge/tasks.md && git commit -m "docs(askdata): document governance permission bridge"`

### Task 6: Final Verification

**Files:**
- Modify: `openspec/changes/add-askdata-governance-permission-bridge/tasks.md`
- Create or Modify: `docs/superpowers/reports/2026-07-29-add-askdata-governance-permission-bridge-verify.md`

**Interfaces:**
- Consumes: all implementation and docs.
- Produces: verification report and fully checked tasks.

- [x] **Step 1: Run OpenSpec validation**

Run: `openspec validate add-askdata-governance-permission-bridge --strict`
Expected: `Change 'add-askdata-governance-permission-bridge' is valid`.

- [x] **Step 2: Run Core targeted tests**

Run: `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-askdata test`
Expected: BUILD SUCCESS with all AskData Core tests passing.

- [x] **Step 3: Run AI AskData targeted tests**

Run: `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest='*AskData*Test' test`
Expected: BUILD SUCCESS with AskData autoconfiguration tests passing.

- [x] **Step 4: Run Starter package check**

Run: `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -DskipTests package`
Expected: BUILD SUCCESS. Existing local POM warnings may be recorded if they do not fail the build.

- [x] **Step 5: Run static scans**

Run: `git diff --check -- fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-askdata fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure docs/askdata openspec/changes/add-askdata-governance-permission-bridge`
Expected: exit code 0.

Run: `! rg -n "TBD|TODO|后续实现|未实现|外部集成预留" docs/askdata/starter openspec/changes/add-askdata-governance-permission-bridge docs/superpowers/specs/2026-07-29-add-askdata-governance-permission-bridge-design.md`
Expected: exit code 0.

- [x] **Step 6: Write verification report and check final tasks**

Record commands, results, test counts, remaining external integration non-goals, and any accepted warnings in `docs/superpowers/reports/2026-07-29-add-askdata-governance-permission-bridge-verify.md`. Mark tasks 6.1 through 6.4 complete only after evidence is recorded.

- [x] **Step 7: Commit Task 6**

Run: `git add openspec/changes/add-askdata-governance-permission-bridge/tasks.md docs/superpowers/reports/2026-07-29-add-askdata-governance-permission-bridge-verify.md && git commit -m "chore(askdata): record governance bridge verification"`

## Self-Review

- Spec coverage: governance bridge, Core contracts, query engine authorization, observability denial/admin access, docs and verification are covered by Tasks 1-6.
- Placeholder scan: this plan intentionally avoids deferred implementation markers and names exact files, commands and expected outcomes.
- Type consistency: `AskDataGovernanceProvider`, `AskDataAuthorizationRequest`, `AskDataAuthorizationSummary`, `AskDataAuthorizedObject`, `AskDataRowPredicate` are introduced in Tasks 1-2 and consumed by Tasks 3-4.
