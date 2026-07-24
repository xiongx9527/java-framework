---
change: add-magicapi-sentinel-guards
design-doc: docs/superpowers/specs/2026-07-23-magicapi-sentinel-guards-design.md
base-ref: 277ace4426d9e97d381526b95c712efb7b7142aa
archived-with: 2026-07-24-add-magicapi-sentinel-guards
---

# Magic-API Sentinel Guards Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Magic-API HTTP `api` 接口实现基于 Sentinel 的热点数据保护与熔断降级保护。

**Architecture:** 在 `fw-plus-boot-autoconfigure` 中新增独立 `RequestInterceptor`，复用/抽取请求 key 解析能力，并通过合并式 Sentinel rule manager 注册 `FlowRule` 与 `ParamFlowRule`。被 Sentinel 阻断时返回 Magic-API 统一失败响应，成功进入的 Sentinel `Entry` 在请求完成后释放。

**Tech Stack:** Java 8、Spring Boot 2.7.18、Magic-API 2.2.2、Sentinel 1.8.6、JUnit、Mockito、Maven。

## Global Constraints

- 所有代码保持 JDK 1.8 兼容，不使用 Java 9+ API。
- 使用 `ParamConfig` 读取接口 option 与 `magic.api.<path>.options.*`，外部配置优先。
- `hot_data_count <= 0` 或 `fallback_count <= 0` 必须放行，不注册有效阻断规则。
- 不实现 Java `${methodName}Fallback`，不新增 fallback script/function，不修改 Magic-API core。
- Sentinel 规则加载必须保留外部规则和其他 Magic-API 接口规则。
- 任务完成后同步勾选 `openspec/changes/add-magicapi-sentinel-guards/tasks.md` 对应条目并提交。

---

## File Structure

- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-dependencies/pom.xml`，集中管理 Sentinel 版本。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/pom.xml`，声明 Sentinel 依赖。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/MagicAPIBaseAutoConfiguration.java`，注册默认 Bean。
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/provider/key/MagicApiRequestKeyResolver.java`，通用请求 key 解析。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/provider/repeat/RepeatSubmitKeyResolver.java`，复用通用 key 解析。
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/provider/sentinel/MagicApiSentinelResourceResolver.java`，接口资源命名。
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/provider/sentinel/MagicApiSentinelOptions.java`，配置解析结果。
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/provider/sentinel/MagicApiSentinelRuleManager.java`，合并式规则管理。
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/provider/sentinel/MagicApiSentinelGuardRequestInterceptor.java`，运行时拦截器。
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/provider/key/MagicApiRequestKeyResolverTest.java`。
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/provider/sentinel/MagicApiSentinelRuleManagerTest.java`。
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/provider/sentinel/MagicApiSentinelGuardRequestInterceptorTest.java`。
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/MagicAPIBaseAutoConfigurationSentinelGuardTest.java`。

### Task 1: Sentinel 依赖与自动配置骨架

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-dependencies/pom.xml`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/pom.xml`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/MagicAPIBaseAutoConfiguration.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/MagicAPIBaseAutoConfigurationSentinelGuardTest.java`

**Interfaces:**
- Consumes: existing `ResultProvider`, `ParamConfig`, `RequestInterceptor` registration style.
- Produces: Spring Bean placeholders for `MagicApiRequestKeyResolver`, `MagicApiSentinelRuleManager`, `MagicApiSentinelGuardRequestInterceptor`.

- [x] **Step 1: Write failing auto-configuration test**

Create `MagicAPIBaseAutoConfigurationSentinelGuardTest` using the same `ApplicationContextRunner` style as repeat submit tests. Assert the context contains `MagicApiSentinelGuardRequestInterceptor`, `MagicApiSentinelRuleManager`, `MagicApiRequestKeyResolver`, and honors a user-provided interceptor bean.

- [x] **Step 2: Run the focused test and verify it fails**

Run: `cd fw-plus-boot && mvn -f fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure -Dtest=MagicAPIBaseAutoConfigurationSentinelGuardTest test`

Expected: FAIL because the new classes/beans do not exist.

- [x] **Step 3: Add Sentinel dependencies**

Add `sentinel.version` with value `1.8.6` to dependency management and add `sentinel-core` plus `sentinel-parameter-flow-control` dependencies to autoconfigure.

- [x] **Step 4: Add minimal classes and bean methods**

Create skeletal classes with constructors only. Register beans in `MagicAPIBaseAutoConfiguration` with `@ConditionalOnMissingBean`.

- [x] **Step 5: Run test and compile**

Run the focused test again. Expected: PASS.

- [x] **Step 6: Commit**

Commit message: `feat(magicapi): add sentinel guard configuration skeleton`.

### Task 2: 通用请求 Key Resolver

**Files:**
- Create: `.../provider/key/MagicApiRequestKeyResolver.java`
- Modify: `.../provider/repeat/RepeatSubmitKeyResolver.java`
- Test: `.../provider/key/MagicApiRequestKeyResolverTest.java`
- Test: existing repeat submit key resolver tests

**Interfaces:**
- Produces: `String resolve(ApiInfo info, RequestEntity requestEntity, String configuredKey)` and `String fallbackFingerprint(ApiInfo info, RequestEntity requestEntity)`.
- Consumes: `ParamConfig` remains used by caller-specific resolver classes.

- [x] **Step 1: Write failing tests for SpEL, literal, fallback, long segment digest**

Cover variables `request`, `response`, `headers`, `params`, `pathVariables`, `body`, `api`, `requestEntity`; verify empty configured key returns fingerprint and long values become `sha256:*`.

- [x] **Step 2: Run resolver tests and verify failure**

Run: `cd fw-plus-boot && mvn -f fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure -Dtest=MagicApiRequestKeyResolverTest test`

Expected: FAIL because class does not exist.

- [x] **Step 3: Extract implementation from RepeatSubmitKeyResolver**

Move SpEL evaluation, interface key, fallback fingerprint, segment normalization and digest logic into `MagicApiRequestKeyResolver`. Keep error message generic for Magic-API key parsing.

- [x] **Step 4: Refactor RepeatSubmitKeyResolver to delegate**

Repeat submit should still build `magicapi:repeat:submit:<interface>:<main>:<suffix>` and preserve suffix provider behavior.

- [x] **Step 5: Run new and existing tests**

Run: `cd fw-plus-boot && mvn -f fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure -Dtest=MagicApiRequestKeyResolverTest,RepeatSubmitKeyResolverTest test`

Expected: PASS.

- [x] **Step 6: Commit**

Commit message: `refactor(magicapi): extract request key resolver`.

### Task 3: Sentinel 配置解析与规则管理

**Files:**
- Create: `.../provider/sentinel/MagicApiSentinelResourceResolver.java`
- Create: `.../provider/sentinel/MagicApiSentinelOptions.java`
- Create/Modify: `.../provider/sentinel/MagicApiSentinelRuleManager.java`
- Test: `.../provider/sentinel/MagicApiSentinelRuleManagerTest.java`

**Interfaces:**
- Produces: `String resolve(ApiInfo info)` resource name.
- Produces: `MagicApiSentinelOptions from(ApiInfo info)` with hot/fallback enabled flags, grade/count/duration.
- Produces: `void ensureRules(String resource, MagicApiSentinelOptions options)`.

- [x] **Step 1: Write failing rule manager tests**

Test `count<=0` does not create rules; QPS/THREAD grade maps correctly; updating resource A keeps resource B; external Sentinel rules remain after Magic-API rule updates.

- [x] **Step 2: Run tests and verify failure**

Run: `cd fw-plus-boot && mvn -f fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure -Dtest=MagicApiSentinelRuleManagerTest test`

Expected: FAIL because implementation is missing.

- [x] **Step 3: Implement resource resolver and options parser**

Use id if present, otherwise method + normalized path. Parse booleans with `ParamConfig.isEnabled`; parse numeric values defensively and use defaults from `Options`.

- [x] **Step 4: Implement merging rule manager**

Maintain synchronized maps for framework-owned `FlowRule` and `ParamFlowRule`. Before `loadRules`, merge current external rules with owned rule cache and replace only framework-owned resources.

- [x] **Step 5: Run rule manager tests**

Expected: PASS.

- [x] **Step 6: Commit**

Commit message: `feat(magicapi): add sentinel rule manager`.

### Task 4: Magic-API Sentinel Guard 拦截器

**Files:**
- Create/Modify: `.../provider/sentinel/MagicApiSentinelGuardRequestInterceptor.java`
- Test: `.../provider/sentinel/MagicApiSentinelGuardRequestInterceptorTest.java`

**Interfaces:**
- Consumes: `MagicApiRequestKeyResolver`, `MagicApiSentinelResourceResolver`, `MagicApiSentinelRuleManager`, `ResultProvider`.
- Produces: `RequestInterceptor` that short-circuits blocked requests and releases entries.

- [x] **Step 1: Write failing interceptor tests**

Cover disabled guard放行、`count<=0`放行、hot key隔离、热点阻断不执行脚本、fallback 阻断不执行脚本、正常和异常 afterCompletion 释放 entry、阻断请求不释放 unrelated entry。

- [x] **Step 2: Run interceptor tests and verify failure**

Run: `cd fw-plus-boot && mvn -f fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure -Dtest=MagicApiSentinelGuardRequestInterceptorTest test`

Expected: FAIL because interceptor is missing/incomplete.

- [x] **Step 3: Implement preHandle**

Build `RequestEntity` compatibility helper like repeat submit. Resolve options and resource. Ensure rules. Enter fallback FlowRule first, then hot data ParamFlowRule with resolved key.

- [x] **Step 4: Implement blocked response and entry cleanup**

On `BlockException`, release any entry already acquired in this request and return `ResultProvider` failure. Store successful entries in `MagicScriptContext`; afterCompletion releases in reverse order.

- [x] **Step 5: Run interceptor tests**

Expected: PASS.

- [x] **Step 6: Commit**

Commit message: `feat(magicapi): enforce sentinel guards for api requests`.

### Task 5: 集成验证与 OpenSpec 任务同步

**Files:**
- Modify: `openspec/changes/add-magicapi-sentinel-guards/tasks.md`
- Possibly Modify: implementation tests if integration reveals gaps

**Interfaces:**
- Consumes: all previous tasks.
- Produces: verified implementation and checked OpenSpec tasks.

- [x] **Step 1: Run focused test suite**

Run: `cd fw-plus-boot && mvn -f fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure -Dtest=MagicAPIBaseAutoConfigurationSentinelGuardTest,MagicApiRequestKeyResolverTest,RepeatSubmitKeyResolverTest,MagicApiSentinelRuleManagerTest,MagicApiSentinelGuardRequestInterceptorTest test`

Expected: PASS.

- [x] **Step 2: Run module test or compile gate**

Run: `cd fw-plus-boot && mvn -f fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure test`

Expected: PASS. If dependency resolution prevents full test, run the narrowest compilable Maven command and record the exact failure.

- [x] **Step 3: Update OpenSpec tasks**

Check off completed items in `openspec/changes/add-magicapi-sentinel-guards/tasks.md` only after implementation and tests pass.

- [x] **Step 4: Commit final task sync**

Commit message: `chore(magicapi): verify sentinel guard implementation`.

## Self-Review

- Spec coverage: per-interface config、hot data key、fallback resource、entry release、rule preservation all map to Tasks 2-4 and Task 5 verification.
- Placeholder scan: no TBD/TODO placeholders remain.
- Type consistency: resource resolver、options parser、rule manager、request interceptor interfaces are consistently named across tasks.
