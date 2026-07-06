# NoSQL Redis Safe Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Safely optimize the NoSQL Redis autoconfiguration package without changing public API behavior.

**Architecture:** Use small TDD-backed patches around existing classes. Keep current Spring Boot auto-configuration, Redis dynamic data-source, helper, and condition contracts intact while adding defensive checks and regression tests.

**Tech Stack:** Java 8, Spring Boot 2.7.18, Spring Data Redis, JUnit 5, Mockito, AssertJ, Maven Surefire.

## Global Constraints

- Keep existing configuration prefixes, Bean names, public classes, and public method signatures compatible.
- Do not introduce Java 9+ APIs.
- Do not require a real Redis server for tests.
- Do not change `RedisTemplate` serialization strategy or dynamic routing semantics.
- Use targeted Maven tests under `fw-plus-boot-project/fw-plus-boot-autoconfigure`.

---

## File Structure

- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/nosql/redis/condition/OnExistingPropertyCondition.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/nosql/redis/condition/OnMissingPropertyCondition.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/nosql/redis/condition/PropertyConditionTest.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/nosql/redis/RedisDataSourceContext.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/nosql/redis/client/RedisMultiSourceClient.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/nosql/redis/RedisDataSourceContextTest.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/nosql/redis/client/RedisMultiSourceClientTest.java`
- Modify selectively after inspection: `redis/config/DynamicRedisTemplateFactory.java`, `redis/config/RedisConnectionConfiguration.java`, and helper classes only when tests expose low-risk null/argument issues.

---

### Task 1: Harden property conditions

**Files:**
- Modify: `.../redis/condition/OnExistingPropertyCondition.java`
- Modify: `.../redis/condition/OnMissingPropertyCondition.java`
- Create: `.../src/test/java/.../nosql/redis/condition/PropertyConditionTest.java`

**Interfaces:**
- Consumes: `ConditionalOnExistingProperty.value()` and `ConditionalOnMissingProperty.value()`.
- Produces: deterministic `ConditionOutcome` for present, missing, blank, and metadata-missing cases.

- [x] **Step 1: Write failing tests**
  - Test existing condition matches when all configured properties have text.
  - Test existing condition does not match when any configured property is missing or blank.
  - Test missing condition matches only when all configured properties are missing or blank.
  - Test metadata without the expected annotation returns noMatch instead of relying on `assert`.

- [x] **Step 2: Run tests to verify failure**
  - Run: `mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='PropertyConditionTest' test -q`
  - Expected: at least the metadata-missing test fails or errors on current implementation.

- [x] **Step 3: Implement minimal hardening**
  - Replace `assert annotationAttributes != null` with explicit null/empty checks.
  - Use `StringUtils.hasText` consistently for existing/missing decisions.
  - Use matching condition class in `ConditionMessage.forCondition(...)`.

- [x] **Step 4: Run tests to verify pass**
  - Run: `mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='PropertyConditionTest' test -q`
  - Expected: pass.

---

### Task 2: Stabilize Redis data-source context and client boundaries

**Files:**
- Modify: `.../redis/RedisDataSourceContext.java`
- Modify: `.../redis/client/RedisMultiSourceClient.java`
- Create: `.../src/test/java/.../nosql/redis/RedisDataSourceContextTest.java`
- Create: `.../src/test/java/.../nosql/redis/client/RedisMultiSourceClientTest.java`

**Interfaces:**
- Consumes: existing context/client public methods.
- Produces: same public API with clearer null/key behavior and no accidental NPE for invalid inputs.

- [x] **Step 1: Inspect current methods and write failing tests**
  - Cover blank key handling.
  - Cover unknown data-source key behavior.
  - Cover register/get happy path using mocks/fakes.

- [x] **Step 2: Run tests to verify failure**
  - Run: `mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='RedisDataSourceContextTest,RedisMultiSourceClientTest' test -q`
  - Expected: current implementation fails at least one invalid-input assertion.

- [x] **Step 3: Implement minimal defensive checks**
  - Add explicit argument validation only where current code would NPE or silently misbehave.
  - Preserve existing normal return values and existing method signatures.
  - Improve exception messages with the data-source key when applicable.

- [x] **Step 4: Run tests to verify pass**
  - Run same command.
  - Expected: pass.

---

### Task 3: Apply low-risk factory/helper cleanup only where justified

**Files:**
- Inspect/modify as needed: `.../redis/config/DynamicRedisTemplateFactory.java`
- Inspect/modify as needed: `.../redis/config/RedisConnectionConfiguration.java`
- Inspect/modify as needed: `.../redis/helper/RedisHelper.java`
- Inspect/modify as needed: `.../redis/helper/EasyRedisHelper.java`
- Tests: adjacent focused tests if behavior can be tested without Redis.

**Interfaces:**
- Consumes: existing factory/helper public methods.
- Produces: same behavior with clearer validation or generic typing.

- [x] **Step 1: Identify one or two low-risk issues**
  - Prefer raw-type cleanup, null dependency guard, or duplicated small helper extraction.
  - Skip any behavior requiring real Redis integration.

- [x] **Step 2: Write failing tests for selected issue**
  - Keep tests focused on pure Java behavior or constructor/factory validation.

- [x] **Step 3: Implement minimal cleanup**
  - Do not change serialization, routing, or external method signatures.

- [x] **Step 4: Run selected tests**
  - Run targeted Maven `-Dtest=...` command.

---

### Task 4: Final verification and commit

**Files:**
- All modified source/test files.
- Plan/spec docs only if updated during implementation.

**Interfaces:**
- Produces: verified commit containing nosql safe optimization.

- [x] **Step 1: Run focused nosql tests**
  - Run: `mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='*Redis*Test,*PropertyConditionTest' test -q`

- [x] **Step 2: Run formatting/whitespace check**
  - Run: `git diff --check`

- [x] **Step 3: Review diff**
  - Run: `git diff --stat`
  - Ensure no unrelated modules changed.

- [x] **Step 4: Commit**
  - Commit message: `refactor: harden nosql redis autoconfiguration`
