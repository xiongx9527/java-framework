# Cloud Eureka Plugin Layering Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 Cloud Eureka Magic-API 插件移动到独立插件聚合层下，并保持现有 Maven 坐标和依赖行为不变。

**Architecture:** 参照 Boot 和 AI 的 Maven reactor 层级，在 `fw-plus-cloud-starters` 与 Eureka 插件之间增加 `fw-plus-magicapi-plugin` 聚合 POM。现有插件目录整体下移一层，只更新 Maven parent、相对基准路径及层级契约测试。

**Tech Stack:** Maven multi-module reactor、Node.js `node:test`、XML

## Global Constraints

- 不修改 Eureka 插件业务源码或 artifactId。
- 不修改 Cloud BOM 中现有 Eureka 依赖管理。
- 不修改当前 Comet change 及其他未提交文件。
- 不创建分支或 worktree。

---

### Task 1: 修正 Cloud Eureka 插件聚合层级

**Files:**
- Create: `fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-starters/fw-plus-magicapi-plugin/pom.xml`
- Modify: `fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-starters/pom.xml`
- Move: `fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-starters/fw-plus-magicapi-plugin-eureka/` to `fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-eureka/`
- Modify: `fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-eureka/pom.xml`
- Test: `fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-eureka/src/console/test/maven-layering.test.mjs`

**Interfaces:**
- Consumes: `fw-plus-cloud-starters` 父 POM和 `${revision}`。
- Produces: `com.xiongx9527.fw.plus.cloud:fw-plus-magicapi-plugin` 聚合器，以及坐标不变的 `fw-plus-magicapi-plugin-eureka` 子模块。

- [x] **Step 1: 将层级测试改为断言新的聚合器和插件路径**

  测试必须读取 `fw-plus-cloud-starters/fw-plus-magicapi-plugin/pom.xml`，断言 starters 聚合器只引用 `fw-plus-magicapi-plugin`，插件聚合器引用 `fw-plus-magicapi-plugin-eureka`，且 Eureka 插件父 artifactId 为 `fw-plus-magicapi-plugin`。

- [x] **Step 2: 运行测试并确认因新聚合 POM 不存在而失败**

  Run: `node --test fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-starters/fw-plus-magicapi-plugin-eureka/src/console/test/maven-layering.test.mjs`

  Expected: FAIL，错误指出 `fw-plus-magicapi-plugin/pom.xml` 不存在。

- [x] **Step 3: 增加聚合 POM并移动 Eureka 插件**

  新聚合 POM继承 `fw-plus-cloud-starters`、使用 `pom` packaging，并只聚合 `fw-plus-magicapi-plugin-eureka`。移动后的子模块继承新聚合器，并将 `main.basedir` 改为 `${basedir}/../../../..`。

- [x] **Step 4: 运行层级测试和 Maven 验证**

  Run: `node --test fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-eureka/src/console/test/*.test.mjs`

  Expected: 所有测试通过。

  Run: `./fw-plus-cloud/mvnw -f fw-plus-cloud/pom.xml -pl fw-plus-cloud-project/fw-plus-cloud-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-eureka -am validate -DskipTests`

  Expected: `BUILD SUCCESS`。

- [x] **Step 5: 校验路径、XML 和差异**

  使用 `xmllint --noout` 检查三个相关 POM，确认旧目录不存在、新目录存在，并运行 `git diff --check`。
