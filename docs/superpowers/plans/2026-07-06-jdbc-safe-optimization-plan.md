# JDBC Safe Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 严格优化 JDBC 自动配置目录的空值边界、默认数据源注入和 AOP 属性防御，并补齐关键测试。

**Architecture:** 保持现有自动配置入口和 dynamic-datasource 集成方式不变，只在边界处理处增加兼容性防御。测试以单元级行为验证为主，避免引入完整 Spring Boot 上下文的高成本启动。

**Tech Stack:** Java 8, Spring Boot 2.7.18, dynamic-datasource, Druid, JUnit 5, Spring Mock Servlet API.

## Global Constraints

- 全程保持 Java 8 兼容，不使用 Java 9+ API。
- 不修改依赖版本、不新增第三方依赖。
- 不改变公开配置前缀、Bean 名称、包名和 starter 使用方式。
- 所有新增/修改源码保持 UTF-8 编码和现有代码风格。

---

### Task 1: Processor 边界测试与实现

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc/dynamic/datasource/processor/DsHeaderProcessor.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc/dynamic/datasource/processor/DsSessionProcessor.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc/dynamic/datasource/processor/DsHeaderProcessorTest.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc/dynamic/datasource/processor/DsSessionProcessorTest.java`

**Interfaces:**
- Consumes: `DsProcessor.matches(String)` and `DsProcessor.doDetermineDatasource(MethodInvocation, String)`.
- Produces: processor 在缺少 Web 上下文或值不存在时返回 `null`，正常场景返回数据源名称。

- [ ] **Step 1: Write failing tests**

覆盖 `matches(null)`、非 Web 上下文、空后缀、正常 Header/Session 值解析。

- [ ] **Step 2: Run tests and verify RED**

Run: `mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='DsHeaderProcessorTest,DsSessionProcessorTest' test -q`
Expected: FAIL，现有实现因 NPE 或断言不满足失败。

- [ ] **Step 3: Implement minimal processor guards**

使用 `StringUtils.startsWith` / `StringUtils.substringAfter` 和 `RequestContextHolder` 类型判断；Session 使用 `getSession(false)`。

- [ ] **Step 4: Run tests and verify GREEN**

Run: `mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='DsHeaderProcessorTest,DsSessionProcessorTest' test -q`
Expected: PASS。

### Task 2: 默认数据源注入测试与实现

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc/DynamicDataSourceAutoConfiguration.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc/DynamicDataSourceAutoConfigurationTest.java`

**Interfaces:**
- Consumes: `DynamicDataSourceProperties`, `DefaultDataSourceProperty`, `ObjectProvider<List<DynamicDataSourcePropertiesCustomizer>>`。
- Produces: 构造阶段只在默认 URL 非空白且未显式配置 primary 时补入默认数据源。

- [ ] **Step 1: Write failing tests**

覆盖空白 URL 不注入、有效 URL 注入 `master`、已有 `master` 不覆盖。

- [ ] **Step 2: Run tests and verify RED**

Run: `mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='DynamicDataSourceAutoConfigurationTest' test -q`
Expected: FAIL，现有实现会把空白 URL 当作有效值。

- [ ] **Step 3: Implement minimal default datasource guard**

抽取 `resolvePrimary` 和 `hasDefaultDatasourceUrl` 私有方法，使用 `StringUtils.isNotBlank`。

- [ ] **Step 4: Run tests and verify GREEN**

Run: `mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='DynamicDataSourceAutoConfigurationTest' test -q`
Expected: PASS。

### Task 3: AOP 属性防御测试与实现

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc/dynamic/datasource/DynamicDataSourceAopConfiguration.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc/dynamic/datasource/DynamicDataSourceAopConfigurationTest.java`

**Interfaces:**
- Consumes: `DynamicDataSourceProperties` and `DsProcessor`。
- Produces: 当 `properties.getAop()` 为 `null` 时仍能创建注解 Advisor 与事务 Advisor。

- [ ] **Step 1: Write failing tests**

覆盖 `dynamicDatasourceAnnotationAdvisor` 和 `dynamicTransactionAdvisor` 在 AOP 属性为空时不抛异常。

- [ ] **Step 2: Run tests and verify RED**

Run: `mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='DynamicDataSourceAopConfigurationTest' test -q`
Expected: FAIL，现有实现因 `properties.getAop()` 为空抛 NPE。

- [ ] **Step 3: Implement minimal AOP default fallback**

新增 `private DynamicDatasourceAopProperties getAopProperties()`，为空时返回新默认对象。

- [ ] **Step 4: Run tests and verify GREEN**

Run: `mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='DynamicDataSourceAopConfigurationTest' test -q`
Expected: PASS。

### Task 4: Final verification

**Files:**
- Check: all modified Java files and new docs.

**Interfaces:**
- Consumes: Tasks 1-3 completed implementation.
- Produces: Verified JDBC optimization patch.

- [ ] **Step 1: Run focused JDBC tests**

Run: `mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='*Jdbc*Test,*DataSource*Test,*Ds*Test' test -q`
Expected: PASS。

- [ ] **Step 2: Run whitespace check**

Run: `git diff --check`
Expected: no output。

- [ ] **Step 3: Review diff**

Run: `git diff -- fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc docs/superpowers/specs/2026-07-06-jdbc-safe-optimization-design.md docs/superpowers/plans/2026-07-06-jdbc-safe-optimization-plan.md`
Expected: diff matches design and no unrelated changes。
