# TinyEngine Template Clone Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复模板创建应用时编码冲突、源模板角色校验失败和重复错误提示，使真实模板创建请求稳定返回成功。

**Architecture:** 模板创建表单要求用户输入新应用编码；后端在写库前执行稳定业务校验，并将源模板页面读取限定为已验证同租户模板后的内部 Mapper 操作。目标应用写入仍走现有服务权限，前端失败提示统一交给全局 HTTP 层。

**Tech Stack:** Java 8、Spring Boot 2.7.18、MyBatis、JUnit 5、Mockito、Vue 3、Vitest、Vue Test Utils、pnpm。

## Global Constraints

- Java 源码必须兼容 JDK 1.8，不使用 Java 9+ API。
- 同一租户内应用编码必须唯一，编码只允许英文、数字、下划线和中划线，最大长度 20。
- 仅在验证源应用是当前租户模板后，模板复制服务才能绕过源应用角色读取模板数据。
- 目标应用必须为当前用户创建唯一 `app_owner` 角色，复制失败时事务整体回滚。
- 请求失败只显示一次全局错误提示，不在组件层重复 Notify。
- 不修改 AI 鉴权与应用成员入口，不提交构建产物或其他工作区改动。

---

## File Map

- `fw-plus-core/.../service/impl/AppTemplateServiceImpl.java`：校验新应用编码、内部读取模板页面并驱动事务复制。
- `fw-plus-core/.../service/TinyEngineServiceContractTest.java`：覆盖编码校验、模板内部读取和 Owner 创建契约。
- `fw-plus-boot/.../workspace/template-center/src/TemplateDialog.vue`：收集并校验新应用编码。
- `fw-plus-boot/.../workspace/template-center/src/TemplateDetail.vue`：收敛失败 Promise，不重复提示。
- `fw-plus-boot/.../design-core/tests/TemplateClone.test.ts`：覆盖模板表单提交和单一错误提示。

### Task 1: 后端模板复制契约

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/test/java/com/xiongx9527/fw/plus/tinyengine/service/TinyEngineServiceContractTest.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/service/impl/AppTemplateServiceImpl.java`

**Interfaces:**
- Consumes: `AppMapper.queryAppTemplateById(Integer, String)`、`AppMapper.queryAppByCondition(App)`、`PageMapper.queryPageByApp(Integer)`、`AppRoleService.createOwner(Integer, String, String)`。
- Produces: `AppTemplateServiceImpl.createAppByTemplate(App)` 在写库前拒绝无效或重复编码，并只通过 `PageMapper` 读取已验证模板的页面。

- [ ] **Step 1: 写模板编码与内部读取失败测试**

在现有 `templateCloneCopiesCoreDataAndUsesTenantCondition` 中加入 `PageMapper` mock，令模板编码为 `template-code`、请求编码为 `new-app-code`，并断言：

```java
verify(pageMapper).queryPageByApp(Integer.valueOf(3));
verify(pageService, never()).queryAllPage(Integer.valueOf(3));
assertEquals("new-app-code", request.getCode());
verify(appRoleService).createOwner(Integer.valueOf(22), "tenant-9", "user-7");
```

新增重复编码测试：

```java
@Test
void templateCloneRejectsDuplicateApplicationCodeBeforeInsert() {
    AppMapper mapper = mock(AppMapper.class);
    AppTemplateServiceImpl service = new AppTemplateServiceImpl(context("user-7", "tenant-9"));
    setBaseMapper(service, mapper);
    App request = new App();
    request.setId(3);
    request.setCode("duplicate-code");
    App template = new App();
    template.setId(3);
    when(mapper.queryAppTemplateById(3, "tenant-9")).thenReturn(template);
    when(mapper.queryAppByCondition(any(App.class))).thenReturn(Collections.singletonList(new App()));

    assertThrows(ServiceException.class, () -> service.createAppByTemplate(request));

    verify(mapper, never()).createApp(any(App.class));
}
```

- [ ] **Step 2: 运行测试并确认 RED**

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/pom.xml \
  -Dtest=TinyEngineServiceContractTest#templateCloneCopiesCoreDataAndUsesTenantCondition+templateCloneRejectsDuplicateApplicationCodeBeforeInsert test
```

Expected: FAIL，因为当前实现仍调用 `pageService.queryAllPage`，且没有写库前编码唯一校验。

- [ ] **Step 3: 实现最小后端修复**

在 `AppTemplateServiceImpl` 注入 `PageMapper`，并在模板验证后、清空 ID 前调用：

```java
private void validateNewAppCode(App app) {
    String code = app == null ? null : app.getCode();
    if (code == null || !code.trim().matches("^[A-Za-z0-9_-]{1,20}$")) {
        throw error(ExceptionEnum.CM002);
    }
    App condition = new App();
    condition.setCode(code.trim());
    condition.setTenantId(getTenantId());
    if (!baseMapper.queryAppByCondition(condition).isEmpty()) {
        throw error(ExceptionEnum.CM003);
    }
    app.setCode(code.trim());
}
```

将源模板页面读取改为：

```java
List<Page> pages = pageMapper.queryPageByApp(templateId);
```

保留 `pageService.createPage(page)` 写入目标应用，确保目标 Owner 权限仍被校验。

- [ ] **Step 4: 运行后端测试并确认 GREEN**

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/pom.xml \
  -Dtest=TinyEngineServiceContractTest,AppRoleBoundaryRegressionTest test
```

Expected: PASS。

- [ ] **Step 5: 单独提交后端修复**

```bash
git commit --only -m "fix: 修复 TinyEngine 模板应用复制契约" -- \
  fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/service/impl/AppTemplateServiceImpl.java \
  fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/test/java/com/xiongx9527/fw/plus/tinyengine/service/TinyEngineServiceContractTest.java
```

### Task 2: 模板创建表单与单一错误提示

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console/packages/design-core/tests/TemplateClone.test.ts`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console/packages/workspace/template-center/src/TemplateDialog.vue`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console/packages/workspace/template-center/src/TemplateDetail.vue`

**Interfaces:**
- Consumes: `TemplateDialog` 的 `confirm` 事件、`createAppFromTemplate(params)`、全局 HTTP 错误提示。
- Produces: `confirm` payload 必含用户输入的 `code`；`TemplateDetail.confirmCreateApp` 失败时不调用局部 `Notify`。

- [ ] **Step 1: 写前端失败测试**

在 `TemplateClone.test.ts` 使用 Vue Test Utils 挂载真实组件、stub OpenTiny 表单，并覆盖：

```ts
it('submits a user supplied application code instead of the template code', async () => {
  // template.code = 'template-code'
  // formState.code = 'new-app-code'
  // 调用 confirm 后断言 emitted('confirm')[0][0].code === 'new-app-code'
})

it('does not show a second notification when the global HTTP layer rejects creation', async () => {
  createAppFromTemplate.mockRejectedValueOnce(new Error('request failed'))
  wrapper.vm.confirmCreateApp({ code: 'new-app-code' })
  await flushPromises()
  expect(notify).not.toHaveBeenCalled()
})
```

- [ ] **Step 2: 运行测试并确认 RED**

Run:

```bash
cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console
pnpm --filter @opentiny/tiny-engine test -- TemplateClone.test.ts
```

Expected: FAIL，因为表单没有 `code`，失败分支会调用 `Notify`。

- [ ] **Step 3: 实现最小前端修复**

在 `TemplateDialog.vue` 的 `formState` 增加：

```ts
code: ''
```

增加“应用编码”输入框和规则：

```ts
code: [
  { required: true, message: '应用编码必填', trigger: 'blur' },
  { max: 20, message: '应用编码最长20个字符', trigger: 'change' },
  {
    trigger: 'blur',
    validator: (_rule, value, callback) =>
      /^[A-Za-z0-9_-]+$/.test(value) ? callback() : callback(new Error('应用编码只能包括英文、数字、下划线和中划线'))
  }
]
```

在 `TemplateDetail.vue` 将失败分支改为只收敛 Promise：

```ts
.catch(() => undefined)
```

- [ ] **Step 4: 运行前端测试并确认 GREEN**

Run:

```bash
cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console
pnpm --filter @opentiny/tiny-engine test
```

Expected: PASS。

- [ ] **Step 5: 运行 Starter 资源测试**

Run:

```bash
node --test fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/test/js/tiny-engine-frontend-resources.test.mjs
```

Expected: 现有全部资源测试 PASS。

- [ ] **Step 6: 单独提交前端修复**

```bash
git add -- fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console/packages/design-core/tests/TemplateClone.test.ts
git commit --only -m "fix: 修复 TinyEngine 模板创建交互" -- \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console/packages/design-core/tests/TemplateClone.test.ts \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console/packages/workspace/template-center/src/TemplateDialog.vue \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console/packages/workspace/template-center/src/TemplateDetail.vue
```

### Task 3: 构建、部署与真实验收

**Files:**
- No source changes.

**Interfaces:**
- Consumes: Task 1 安装到 Maven 本地仓库的 Core，以及 Task 2 构建后的 TinyEngine Starter。
- Produces: 19080 上可创建模板应用的运行实例。

- [ ] **Step 1: 安装 Core 模块**

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home \
./fw-plus-core/mvnw -f fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/pom.xml install -DskipTests
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 2: 构建并安装 TinyEngine Starter**

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home \
mvn -f fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/pom.xml install -DskipTests
```

Expected: `BUILD SUCCESS`，前端产物复制到 Starter JAR。

- [ ] **Step 3: 重启 19080 样例服务**

停止当前进程后运行：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home \
mvn -f fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-tiny-engine-sample/pom.xml spring-boot:run \
'-Dspring-boot.run.arguments=--server.port=19080 --fw.plus.tiny-engine.security.enabled=true --fw.plus.tiny-engine.ai.enabled=false --fw.plus.tiny-engine.ai.plugin.langchain4j.enabled=false --fw.plus.ai.model.enabled=false'
```

Expected: `Tomcat started on port(s): 19080`。

- [ ] **Step 4: 在线 API 验收并清理测试应用**

登录后读取真实模板，使用唯一测试名称和新编码调用 `/app-center/api/app-template/create`，断言：

- HTTP 200；
- 返回应用编码等于请求编码；
- `r_auth_users_units_roles` 中存在且仅存在一个 `app_owner`；
- 源模板页面成功复制；
- 验收结束后通过正式删除应用接口删除该测试应用及角色数据。

- [ ] **Step 5: 页面验收**

强制刷新 `http://127.0.0.1:19080/tiny-engine/index.html?tenant=1`，确认：

- 模板创建表单包含应用编码；
- 创建成功后只出现既有成功交互；
- 失败场景只出现一个明确错误提示，不出现空白框。

## Plan Self-Review

- Spec coverage: 编码输入与校验、模板内部读取、Owner 与事务、单一错误提示、构建部署均有对应任务。
- Placeholder scan: 无占位标记、待定项或省略实现步骤。
- Type consistency: 前端字段统一为 `code`；后端使用 `App.getCode/setCode`；页面读取统一为 `PageMapper.queryPageByApp(Integer)`。
