# Mate Safe Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 严格优化 Mate 自动配置目录的条件装配、Validator ThreadLocal 清理和 EasyTrans Bean 让位行为。

**Architecture:** 保持现有自动配置入口不变，在自动配置类上补充 Spring Boot 条件注解，在 Advice 内修复边界行为，在 EasyTrans Bean 上增加用户自定义让位。测试使用 JUnit 5、ApplicationContextRunner 和直接 Advice 单元测试覆盖关键行为。

**Tech Stack:** Java 8, Spring Boot 2.7.18, Spring MVC, EasyTrans, JUnit 5, ApplicationContextRunner.

## Global Constraints

- 全程保持 Java 8 兼容，不使用 Java 9+ API。
- 不修改依赖版本、不新增第三方依赖。
- 不改变公开配置前缀、Bean 方法名、包名和默认启用行为。
- 所有新增/修改源码保持 UTF-8 编码和现有代码风格。

---

### Task 1: Validator Advice 行为修复

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mate/webmvc/validator/CommonsValidatorRequestBodyAdvice.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mate/webmvc/validator/CommonsValidatorResponseBodyAdvice.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/mate/webmvc/validator/CommonsValidatorRequestBodyAdviceTest.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/mate/webmvc/validator/CommonsValidatorResponseBodyAdviceTest.java`

**Interfaces:**
- Consumes: `CommonsValidatorHolder`, `RequestBodyAdvice`, `ResponseBodyAdvice`。
- Produces: 空 body 清理上下文，ResponseBodyAdvice 支持方法级和类级响应体注解。

- [ ] **Step 1: Write failing tests**

覆盖空 body 清理、handleEmptyBody 清理、`@RestController` supports、方法级 `@ResponseBody` supports。

- [ ] **Step 2: Run tests and verify RED**

Run: `mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='CommonsValidatorRequestBodyAdviceTest,CommonsValidatorResponseBodyAdviceTest' test -q`
Expected: FAIL，当前实现不清理空 body 且不识别常规响应体注解。

- [ ] **Step 3: Implement minimal Advice fixes**

空 body 路径调用 `CommonsValidatorHolder.clear()`；Response supports 使用 `AnnotatedElementUtils.hasAnnotation` 判断方法/类 `ResponseBody` 元注解。

- [ ] **Step 4: Run tests and verify GREEN**

Run: `mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='CommonsValidatorRequestBodyAdviceTest,CommonsValidatorResponseBodyAdviceTest' test -q`
Expected: PASS。

### Task 2: Validator 自动配置条件修复

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mate/webmvc/MateWebMvcValidatorAutoConfiguration.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/mate/webmvc/MateWebMvcValidatorAutoConfigurationTest.java`

**Interfaces:**
- Consumes: `fw.plus.mate.validator.enabled`。
- Produces: Servlet Web 环境默认装配，属性关闭时不装配，用户自定义 Advice 不被覆盖。

- [ ] **Step 1: Write failing tests**

使用 `ApplicationContextRunner` 覆盖用户自定义 Advice 不被覆盖。

- [ ] **Step 2: Run tests and verify RED**

Run: `mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='MateWebMvcValidatorAutoConfigurationTest' test -q`
Expected: FAIL，当前实现会注册同类型默认 Bean。

- [ ] **Step 3: Implement minimal auto-configuration conditions**

增加 `@Configuration(proxyBeanMethods = false)`、`@ConditionalOnWebApplication(type = SERVLET)` 和 `@ConditionalOnMissingBean`。

- [ ] **Step 4: Run tests and verify GREEN**

Run: `mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='MateWebMvcValidatorAutoConfigurationTest' test -q`
Expected: PASS。

### Task 3: EasyTrans 自动配置让位

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mate/MateEasyTransAutoConfiguration.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/mate/MateEasyTransAutoConfigurationTest.java`

**Interfaces:**
- Consumes: EasyTrans 服务类、`fw.plus.mate.easy-trans.*` 属性。
- Produces: 默认 Bean 正常创建，用户自定义同类型 Bean 时自动配置让位。

- [ ] **Step 1: Write failing tests**

使用 `ApplicationContextRunner` 覆盖自定义 `MateEasyTransTransService` 时默认 `transService` 不再额外创建。

- [ ] **Step 2: Run tests and verify RED**

Run: `mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='MateEasyTransAutoConfigurationTest' test -q`
Expected: FAIL，当前默认 Bean 会与用户 Bean 并存。

- [ ] **Step 3: Implement minimal missing-bean guards**

增加 `@Configuration(proxyBeanMethods = false)` 和核心 Bean 的 `@ConditionalOnMissingBean`。

- [ ] **Step 4: Run tests and verify GREEN**

Run: `mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='MateEasyTransAutoConfigurationTest' test -q`
Expected: PASS。

### Task 4: Final verification

**Files:**
- Check: all modified Java files and new docs.

**Interfaces:**
- Consumes: Tasks 1-3 completed implementation.
- Produces: Verified Mate optimization patch.

- [ ] **Step 1: Run focused Mate tests**

Run: `mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='*Mate*Test,*CommonsValidator*Test' test -q`
Expected: PASS。

- [ ] **Step 2: Run whitespace check**

Run: `git diff --check`
Expected: no output。

- [ ] **Step 3: Review diff**

Run: `git diff -- fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mate fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/mate docs/superpowers/specs/2026-07-06-mate-safe-optimization-design.md docs/superpowers/plans/2026-07-06-mate-safe-optimization-plan.md`
Expected: diff matches design and no unrelated changes。
