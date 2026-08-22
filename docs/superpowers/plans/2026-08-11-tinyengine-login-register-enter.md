# TinyEngine 登录与注册页 Enter 提交 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 TinyEngine 登录和注册页在输入框聚焦时按 Enter 触发对应操作，并在表单不满足现有可提交条件时阻止请求。

**Architecture:** 在两个 `TinyForm` 根组件监听冒泡的 `keyup.enter`，分别复用现有 `handleLogin` 和 `handleRegister`。两个处理函数在请求前检查各自的 `isReady`，使键盘入口和按钮入口共享同一保护逻辑。

**Tech Stack:** Vue 3、OpenTiny Vue、Node.js `node:test`、pnpm、Maven、Spring Boot 2.7.18、浏览器端到端验证。

## Global Constraints

- 不改变忘记密码、后端接口、登录成功或注册成功后的行为。
- 登录仅在用户名和密码均有值时提交。
- 注册仅在三项完整、密码合规且两次密码一致时提交。
- Enter 与对应按钮必须调用同一个处理函数。

---

### Task 1: 登录与注册表单键盘提交

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console/packages/design-core/src/login/Login.vue`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console/packages/design-core/src/login/Register.vue`

**Interfaces:**
- Consumes: 两个页面现有的 `isReady`、`handleLogin()` 和 `handleRegister()`。
- Produces: 两个表单的 `keyup.enter` 入口，以及处理函数内的可提交条件保护。

- [ ] **Step 1: 在当前旧页面执行真实浏览器 RED 验证**

退出当前账号，输入有效登录凭据并在密码框按 Enter。

Expected: 未调用登录接口或仍停留在登录页，证明旧页面缺少 Enter 提交行为。

- [ ] **Step 2: 实现最小键盘提交行为**

登录表单：

```vue
<tiny-form ref="ruleFormRef" :model="state.loginData" @keyup.enter="handleLogin">
```

```ts
const handleLogin = () => {
  if (!isReady.value) {
    return
  }
```

注册表单：

```vue
<tiny-form :model="state.registerData" @keyup.enter="handleRegister">
```

```ts
const handleRegister = () => {
  if (!isReady.value) {
    return
  }
```

- [ ] **Step 3: 运行前端资源回归测试和差异检查**

Run: `node --test fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/test/js/tiny-engine-frontend-resources.test.mjs`

Run: `git diff --check -- fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console/packages/design-core/src/login/Login.vue fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console/packages/design-core/src/login/Register.vue`

Expected: Node 测试全部通过，差异检查无输出。

### Task 2: 构建、部署和运行验证

**Files:**
- Verify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/pom.xml`
- Verify: `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-tiny-engine-sample/pom.xml`

**Interfaces:**
- Consumes: Task 1 修改后的控制台源码。
- Produces: 本地 Maven Starter 和运行于 `http://127.0.0.1:19080/tiny-engine/index.html` 的更新页面。

- [ ] **Step 1: 构建并安装 TinyEngine Starter**

Run: `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/pom.xml install -DskipTests`

Expected: pnpm 生产构建和 Maven 安装成功。

- [ ] **Step 2: 重启 TinyEngine 示例服务**

停止当前 19080 服务，使用原有 Spring Boot 参数重新启动。

Expected: 日志出现 `Tomcat started on port(s): 19080`。

- [ ] **Step 3: 执行真实浏览器 GREEN 验证**

验证登录有效输入按 Enter 成功、登录空值按 Enter 不请求、注册有效输入按 Enter发起注册、注册无效输入按 Enter 不请求，并确认两个按钮仍正常工作。

- [ ] **Step 4: 最终新鲜验证**

Run: `node --test fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/test/js/tiny-engine-frontend-resources.test.mjs`

Run: `git diff --check`

Run: `lsof -nP -iTCP:19080 -sTCP:LISTEN`

Expected: 测试通过，差异检查无错误，19080 端口由更新后的 Java 进程监听。
