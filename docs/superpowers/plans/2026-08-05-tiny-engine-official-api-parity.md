# TinyEngine Official API Parity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 严格对齐 TinyEngine 官方前端与 v2.10 Java 后端的有效 API 契约，同时保留 `java-framework` 当前 `/tiny-engine/ai/**` 中立规范，并在 Java 8、MVC、WebFlux、MySQL 和打包前端环境中完成可重复验收。

**Architecture:** 先把官方前端候选 URL 归并成逐项契约清单，再按领域补齐 Core Service、共享 `TinyEngineApiFacade`、MVC 与 WebFlux 适配层。官方 `/app-center/api/**` AI 使用独立 OpenAI 兼容网关完成请求原样转发、SM4 `EKEY_` 和实时 SSE；当前 `/tiny-engine/ai/**` 继续走现有 Provider Registry/RAG，不改变公开类型。

**Tech Stack:** Java 8、Spring Boot 2.7.18、Spring MVC/WebFlux、MyBatis-Plus、MySQL/H2、Jackson、Bouncy Castle SM4、Apache HttpClient 4.x 或 Spring Boot 2.7 已管理的 Java 8 HTTP 能力、Vue 3、Vite、pnpm、Node test runner。

## Global Constraints

- 官方前端基线：`/Users/xiongxiao/Workspace/Idea/codes/TinyEngine`，HEAD `484898fd0af3b0561c47ced9e159f3a9a0d1bee0`；只读参考，不覆盖其本地修改。
- 官方后端基线：`/Users/xiongxiao/Workspace/Idea/codes/tiny-engine-backend-java`，HEAD/tag 线 `c116fa649122390baf212f53d82358617c63924d` / `v2.10.0`；只读参考。
- 目标工程基线：`f63c62f2d174a6a49db77791295a3dfd9decc991`，保留全部现有已修改和未跟踪文件。
- JDK 目标为 Java 8；禁止 `List.of()`、`Map.of()`、`String.isBlank()`、`InputStream.readAllBytes()`、`java.net.http.HttpClient` 等 Java 9+ API。
- 官方 `/app-center/api/**` 契约与当前 `/tiny-engine/ai/**` 契约并存，不能用其中一套替换另一套。
- 不新增或内置 `gpt-5.5`、AI Base URL、API Key；保留官方前端已有百炼、DeepSeek 空 Key 预设。
- 不记录 Authorization、API Key、完整提示词、工具参数或模型正文。
- 前端只能使用 `pnpm`。
- 不创建分支或 worktree；不提交、不推送。每个任务结束时执行范围检查和 `git diff --check`。
- 手工修改文件统一使用 `apply_patch`；不得回退用户现有工作。

---

### Task 1: 固化官方前端逐接口契约清单

**Files:**
- Create: `docs/tiny-engine/official-api-contracts.json`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/test/js/tiny-engine-official-api-contracts.test.mjs`
- Modify: `docs/tiny-engine/backend-source-coverage.md`
- Reference: `/Users/xiongxiao/Workspace/Idea/codes/TinyEngine/designer-demo/**`
- Reference: `/Users/xiongxiao/Workspace/Idea/codes/TinyEngine/packages/**`
- Reference: `/Users/xiongxiao/Workspace/Idea/codes/tiny-engine-backend-java/base/src/main/java/com/tinyengine/it/controller/**`

**Interfaces:**
- Produces: JSON 数组，每项固定字段为 `id`、`method`、`path`、`frontendCallSites`、`officialController`、`currentMvcHandler`、`currentWebFluxHandler`、`status`、`responseShape`。
- Produces: 后续全部 Controller 测试使用的唯一契约事实源。

- [ ] **Step 1: 写契约资源的失败结构测试**

```javascript
test('每个官方 API 契约都有调用点、官方处理器和双 Web 栈状态', () => {
  const contracts = JSON.parse(readFileSync(CONTRACT_FILE, 'utf8'))
  assert.ok(contracts.length > 0)
  for (const contract of contracts) {
    assert.match(contract.id, /^[a-z0-9-]+$/)
    assert.ok(['GET', 'POST', 'PUT', 'DELETE', 'PATCH'].includes(contract.method))
    assert.match(contract.path, /^\/(app-center|material-center|platform-center)\//)
    assert.ok(contract.frontendCallSites.length > 0)
    assert.ok(contract.officialController)
    assert.ok(['implemented', 'missing'].includes(contract.status))
  }
})
```

- [ ] **Step 2: 运行测试确认清单尚不存在**

Run:

```bash
node --test fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/test/js/tiny-engine-official-api-contracts.test.mjs
```

Expected: FAIL，指出 `docs/tiny-engine/official-api-contracts.json` 不存在。

- [ ] **Step 3: 从真实调用点建立清单**

清单中的每项必须采用如下完整结构；动态路径统一写成 `{id}`，查询参数记录在 `query` 字段，不把 `${getParams(...)}` 当成新路由：

```json
{
  "id": "app-extension-list",
  "method": "GET",
  "path": "/app-center/api/apps/extension/list",
  "query": ["app", "category"],
  "frontendCallSites": ["packages/common/js/http.ts"],
  "officialController": "AppExtensionController#findAppExtension",
  "currentMvcHandler": "TinyEngineAppMvcController#appExtensions",
  "currentWebFluxHandler": "TinyEngineAppWebFluxController#appExtensions",
  "status": "implemented",
  "responseShape": "Result<List<AppExtension>>"
}
```

逐项检查最初扫描到的 86 个候选字符串，记录重复归并依据；不得只保存缺失项。

- [ ] **Step 4: 更新后端覆盖矩阵为可验证状态**

在 `backend-source-coverage.md` 的 Controller 章节链接契约 JSON，并将“adapted”改成“逐接口状态由 `official-api-contracts.json` 判定”。不得继续用一个类别级状态代表所有路由已实现。

- [ ] **Step 5: 运行结构测试和范围检查**

Run:

```bash
node --test fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/test/js/tiny-engine-official-api-contracts.test.mjs
git diff --check -- docs/tiny-engine/official-api-contracts.json docs/tiny-engine/backend-source-coverage.md fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/test/js/tiny-engine-official-api-contracts.test.mjs
```

Expected: Node 测试 PASS；`git diff --check` 无输出。

---

### Task 2: 补齐应用生命周期、Schema、Canvas 与扩展业务

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/api/TinyEngineApiFacade.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/api/DefaultTinyEngineApiFacade.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/service/AppService.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/service/AppExtensionService.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/service/impl/AppServiceImpl.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/service/impl/AppExtensionServiceImpl.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEngineAppPageMvcApiContractTest.java`
- Test: create `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/test/java/com/xiongx9527/fw/plus/tinyengine/service/AppLifecycleServiceTest.java`

**Interfaces:**
- Consumes: Task 1 中 `app-*` 契约项。
- Produces: `updateApp`、`deleteApp`、`saveApp`、`publishApp`、`previewApp`、`appSchemaComponents`、`lockCanvas`、`createAppExtension`、`updateAppExtension`、`deleteAppExtension`。

- [ ] **Step 1: 写 Service 与 MVC 失败测试**

测试至少覆盖以下请求：

```java
mockMvc.perform(post("/app-center/api/apps/update/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"example-updated\"}"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.data.id").value(1));

mockMvc.perform(post("/app-center/api/apps/extension/create")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"app\":1,\"category\":\"utils\",\"content\":{}}"))
    .andExpect(status().isOk());
```

Service 测试必须验证保存/发布更新版本字段，删除应用时清理关联页面或走现有官方删除 Hook，扩展 CRUD 按 `app + category` 隔离。

- [ ] **Step 2: 运行聚焦测试确认缺少 Facade 方法或路由**

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -am -Dtest=AppLifecycleServiceTest test
./fw-plus-core/mvnw -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -am -Dtest=TinyEngineAppPageMvcApiContractTest test
```

Expected: FAIL，错误对应缺失方法或 404。

- [ ] **Step 3: 扩展 Facade 与 Core Service**

在 `TinyEngineApiFacade` 增加准确签名：

```java
App updateApp(Integer id, App app);
App deleteApp(Integer id);
Object saveApp(Integer id, Map<String, Object> request);
Object publishApp(Integer id, Map<String, Object> request);
Object previewApp(Integer id, Map<String, Object> request);
Object appSchemaComponents(Integer id);
Object lockCanvas(Integer appId, Map<String, Object> request);
AppExtension createAppExtension(AppExtension extension);
AppExtension updateAppExtension(AppExtension extension);
AppExtension deleteAppExtension(Integer id);
```

实现必须委托 Core Service；无法归入单表 Service 的 Schema/Preview 组合逻辑放在 Facade，不放 Controller。

- [ ] **Step 4: 实现 MVC 路由并对齐官方响应**

修改 `TinyEngineAppMvcController`，按照契约 JSON 添加官方路径。每个方法使用 `TinyEngineWebResponses.ok/error/notFound`，但最终 JSON 外形必须由官方对应接口测试断言决定。

- [ ] **Step 5: 运行 Core 与 MVC 测试**

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -am -Dtest=AppLifecycleServiceTest test
./fw-plus-core/mvnw -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -am -Dtest=TinyEngineAppPageMvcApiContractTest test
git diff --check -- fw-plus-core/fw-plus-project/fw-plus-tiny-engine fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure
```

Expected: 全部 PASS；无空扩展列表掩盖数据库错误。

---

### Task 3: 补齐页面、历史、国际化与任务状态

**Files:**
- Modify: `TinyEngineApiFacade.java` and `DefaultTinyEngineApiFacade.java`（Task 2 路径）
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/service/PageService.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/service/PageHistoryService.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/service/I18nEntryService.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/service/TaskRecordService.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/service/impl/PageServiceImpl.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/service/impl/PageHistoryServiceImpl.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/service/impl/I18nEntryServiceImpl.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/service/impl/TaskRecordServiceImpl.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEnginePageMvcController.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEngineHistoryMvcController.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEngineI18nMvcController.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEngineTaskMvcController.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEngineServletWebConfiguration.java`
- Test: create `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEnginePageHistoryI18nMvcApiContractTest.java`

**Interfaces:**
- Produces: 页面删除/复制、历史分页/详情/恢复、`/i18n/entries/bulk/delete`、任务状态查询。

- [ ] **Step 1: 写失败契约测试**

```java
mockMvc.perform(post("/app-center/api/i18n/entries/bulk/delete")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"host\":1,\"hostType\":\"app\",\"keys\":[\"home.title\"]}"))
    .andExpect(status().isOk());

mockMvc.perform(get("/app-center/api/tasks/status/1"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.data.id").value(1));
```

同时断言页面恢复产生新当前版本，不能只返回旧历史对象。

- [ ] **Step 2: 运行测试确认 404/缺失服务行为**

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -am -Dtest=TinyEnginePageHistoryI18nMvcApiContractTest test
```

Expected: FAIL with 404 或 Facade 方法不存在。

- [ ] **Step 3: 实现 Service 与 Facade 方法**

新增以下接口签名：

```java
Page deletePage(Integer id);
Page copyPage(Map<String, Object> request);
List<PageHistoryVo> pageHistories(Integer pageId, Integer currentPage, Integer pageSize);
PageHistoryVo pageHistoryDetail(Integer historyId);
Page restorePageHistory(Map<String, Object> request);
List<I18nEntryDto> bulkDeleteI18nEntries(OperateI18nBatchEntries request);
TaskRecord taskStatus(Integer id);
```

批量删除在一个事务中完成；任一数据库异常必须回滚。

- [ ] **Step 4: 添加领域 Controller 并注册 Bean**

Controller 路径严格来自 Task 1 清单；不要把历史、国际化和任务方法继续堆入 `TinyEnginePageMvcController`。

- [ ] **Step 5: 运行测试与事务回滚断言**

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -am -Dtest=TinyEnginePageHistoryI18nMvcApiContractTest test
git diff --check -- fw-plus-core/fw-plus-project/fw-plus-tiny-engine fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure
```

Expected: PASS；已报告的 bulk delete 不再 404。

---

### Task 4: 补齐物料、区块历史、模型和资源高级操作

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/service/BlockService.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/service/BlockHistoryService.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/service/ResourceService.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/service/ModelService.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/service/impl/BlockServiceImpl.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/service/impl/BlockHistoryServiceImpl.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/service/impl/ResourceServiceImpl.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/service/impl/ModelServiceImpl.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/api/TinyEngineApiFacade.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/api/DefaultTinyEngineApiFacade.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEngineMaterialMvcController.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEngineModelMvcController.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEngineMaterialModelMvcApiContractTest.java`

**Interfaces:**
- Produces: 区块历史、用户/租户筛选、批量资源创建、上传适配、模型官方别名及官方返回结构。

- [ ] **Step 1: 添加失败测试**

覆盖 `/block-history?block=1`、`/block/tenants`、`/block/users`、`/resource/create/batch`、`/resource/upload`、`/model` 和 `/model/detail/{id}`。

- [ ] **Step 2: 运行测试确认缺口**

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -am -Dtest=TinyEngineMaterialModelMvcApiContractTest test
```

Expected: 新增断言 FAIL。

- [ ] **Step 3: 实现 Service/Facade**

```java
List<BlockHistoryDto> blockHistories(Integer blockId);
List<Tenant> blockTenants();
List<User> blockUsers();
List<Resource> createResources(List<Resource> resources);
Object uploadResource(ResourceRequestDto request);
```

上传必须继续使用 `TinyEngineFileSecurityUtils` 校验路径和文件名；禁止引入任意文件写入。

- [ ] **Step 4: 实现官方路由别名与响应结构**

已有同义 Facade 方法时只添加路由别名，不复制业务逻辑。

- [ ] **Step 5: 运行测试和文件安全测试**

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -am -Dtest=TinyEngineMaterialModelMvcApiContractTest test
git diff --check -- fw-plus-core/fw-plus-project/fw-plus-tiny-engine fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure
```

Expected: PASS；路径穿越用例返回 `4xx`。

---

### Task 5: 补齐平台、动态模型、代码生成和外部能力

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/api/TinyEngineExternalCapability.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/api/DefaultTinyEngineExternalCapability.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEnginePlatformMvcController.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEngineCodegenMvcController.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEngineDynamicModelMvcController.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEngineServletWebConfiguration.java`
- Reference: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/dynamic/service/DynamicModelService.java`
- Reference: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/dynamic/service/DynamicService.java`
- Test: create `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEnginePlatformDynamicMvcApiContractTest.java`

**Interfaces:**
- Produces: 课程、动态模型数据、`schema2code`、`code`、`source_tpl` 的官方入口。
- Produces: 可覆盖的外部能力方法，不再只有 `unsupported(String)`。

- [ ] **Step 1: 写失败契约测试**

测试官方前端实际调用的方法与请求形状；动态 SQL 测试同时覆盖非法表名、列名和排序方向。

- [ ] **Step 2: 运行测试确认缺失 Controller**

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -am -Dtest=TinyEnginePlatformDynamicMvcApiContractTest test
```

Expected: FAIL with 404。

- [ ] **Step 3: 将外部能力改为明确接口**

```java
Object courses(Map<String, Object> query);
Object courseDetail(Integer id);
Object generateCode(SchemaCodeParam request);
Object schemaToCode(SchemaCodeParam request);
Object sourceTemplate(Map<String, Object> query);
```

默认实现必须返回 `TinyEngineUnsupportedCapabilityException`，宿主可以用 `@ConditionalOnMissingBean` 覆盖；官方后端已有可安全迁移的纯 Java 行为应直接迁移，不能用 unsupported 代替。

- [ ] **Step 4: 接入 DynamicModelService/DynamicService**

Controller 只调用现有安全动态服务，不能直接拼接 SQL。

- [ ] **Step 5: 运行平台与 SQL 安全测试**

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -am -Dtest=TinyEnginePlatformDynamicMvcApiContractTest test
git diff --check -- fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure
```

Expected: PASS；非法标识符测试继续 PASS。

---

### Task 6: 实现 WebFlux 全量对等与自动配置注册

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webflux/TinyEngineAppWebFluxController.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webflux/TinyEnginePageWebFluxController.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webflux/TinyEngineMaterialWebFluxController.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webflux/TinyEngineModelWebFluxController.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webflux/TinyEngineHistoryWebFluxController.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webflux/TinyEngineI18nWebFluxController.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webflux/TinyEngineTaskWebFluxController.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webflux/TinyEnginePlatformWebFluxController.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webflux/TinyEngineCodegenWebFluxController.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webflux/TinyEngineDynamicModelWebFluxController.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webflux/TinyEngineReactiveWebConfiguration.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webflux/TinyEngineWebFluxApiContractTest.java`

**Interfaces:**
- Consumes: Tasks 2-5 的共享 Facade。
- Produces: Task 1 清单中每个有效业务接口的 WebFlux 映射。

- [ ] **Step 1: 将契约 JSON 参数化到 WebFlux 测试**

```java
contracts.forEach(contract -> assertThat(mappingPaths).contains(contract.getPath()));
```

对需要请求体的接口增加 `WebTestClient` 行为断言，而非只检查注解字符串。

- [ ] **Step 2: 运行测试确认 WebFlux 缺口**

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -am -Dtest=TinyEngineWebFluxApiContractTest test
```

Expected: FAIL，列出未注册路径。

- [ ] **Step 3: 添加对等 Controller**

所有阻塞 Service 调用统一经 `TinyEngineReactiveExecutor` 执行；不得在事件循环直接调用 MyBatis。

- [ ] **Step 4: 注册条件 Bean**

在 `TinyEngineReactiveWebConfiguration` 中使用 `@ConditionalOnMissingBean` 注册新增 Controller，保持宿主覆盖能力。

- [ ] **Step 5: 运行 MVC/WebFlux 全量映射测试**

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -am -Dtest='TinyEngine*ApiContractTest' test
git diff --check -- fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure
```

Expected: MVC 与 WebFlux 均覆盖审核后的契约清单。

---

### Task 7: 实现官方 SM4 凭据契约

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/TinyEngineAiProperties.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/official/TinyEngineAiCredentialCipher.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/official/Sm4TinyEngineAiCredentialCipher.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/official/TinyEngineOfficialAiRequest.java`
- Test: create `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/official/Sm4TinyEngineAiCredentialCipherTest.java`

**Interfaces:**
- Produces: `String encrypt(String plainText)`、`String decrypt(String token)`。
- Produces: `TinyEngineOfficialAiRequest`，保存 `baseUrl`、`model`、解析后的 Authorization 和保留未知字段的 `Map<String,Object>`。

- [ ] **Step 1: 写官方兼容向量失败测试**

```java
String token = cipher.encrypt("secret-value");
assertThat(token).startsWith("EKEY_");
assertThat(cipher.decrypt(token)).isEqualTo("secret-value");
```

增加固定 Base64 SM4 Key 的跨实现测试，确保能解密官方 `SM4Utils` 产生的密文；缺失 Key、错误长度、错误 Token 均断言明确异常。

- [ ] **Step 2: 运行测试确认实现不存在**

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am -Dtest=Sm4TinyEngineAiCredentialCipherTest test
```

Expected: FAIL with missing classes。

- [ ] **Step 3: 增加配置和 SM4 实现**

在 `TinyEngineAiProperties` 增加 `official` 嵌套配置，仅包含：

```java
private boolean enabled = true;
private String sm4Key;
private int connectTimeoutMillis = 10000;
private int readTimeoutMillis = 120000;
private int maxResponseBytes = 10 * 1024 * 1024;
```

Key 解析优先显式属性，其次读取 `SM4KEY` 环境变量。使用 Core BOM 已管理的 Bouncy Castle Java 8 兼容版本，不引入 `bcprov-jdk18on` 到 Java 8 主路径。

- [ ] **Step 4: 实现请求解析优先级**

Authorization Bearer 值覆盖请求体 `apiKey`；`EKEY_` 才解密，普通 Key 原样使用。转发 JSON 必须删除 `baseUrl` 与 `apiKey`，保留所有其他键。

- [ ] **Step 5: 运行测试和依赖树检查**

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am -Dtest=Sm4TinyEngineAiCredentialCipherTest test
./fw-plus-core/mvnw -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure dependency:tree -Dincludes=org.bouncycastle
git diff --check -- fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure
```

Expected: PASS；依赖树只出现 Java 8 兼容 Bouncy Castle。

---

### Task 8: 实现官方非流式 OpenAI 兼容代理

**Files:**
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/official/TinyEngineOpenAiCompatibleGateway.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/official/DefaultTinyEngineOpenAiCompatibleGateway.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/official/TinyEngineOfficialAiResponse.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/official/TinyEngineOfficialAiController.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/TinyEngineAiAutoConfiguration.java`
- Test: create `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/official/TinyEngineOfficialAiControllerTest.java`

**Interfaces:**
- Produces: `TinyEngineOfficialAiResponse execute(TinyEngineOfficialAiRequest request)`。
- Produces: 官方 POST `/app-center/api/ai/chat` 和 `/app-center/api/chat/completions` 非流式行为。

- [ ] **Step 1: 使用本地假上游写失败测试**

测试请求必须包含 `response_format` 对象、`tools`、`thinking_budget` 和一个未知字段，断言假上游全部收到；断言 `baseUrl/apiKey` 未出现在上游 JSON，Authorization 正确。

- [ ] **Step 2: 运行测试确认官方路由不存在**

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am -Dtest=TinyEngineOfficialAiControllerTest test
```

Expected: FAIL with 404/missing bean。

- [ ] **Step 3: 实现 Java 8 HTTP 网关**

网关只允许 HTTP/HTTPS，按官方规则补齐 `/v1/chat/completions`；禁止跨主机重定向携带 Authorization。响应对象保存状态码、允许转发的响应头和原始 body bytes。

- [ ] **Step 4: 实现 MVC 非流式 Controller 与 encrypt-key**

`stream=false` 时返回上游状态码、Content-Type 和 body；`/encrypt-key` 请求/响应使用官方 `AiToken` 外形 `{ "token": "EKEY_..." }`。

- [ ] **Step 5: 验证上游错误映射**

覆盖上游 `401/429/500` 原状态转发、连接失败 `502`、超时 `504`、非法 URL/缺模型/缺 Key 的明确 `4xx`。

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am -Dtest=TinyEngineOfficialAiControllerTest test
git diff --check -- fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure
```

Expected: PASS；错误正文不包含密钥。

---

### Task 9: 实现官方实时 SSE 与 WebFlux 对等

**Files:**
- Modify: `TinyEngineOpenAiCompatibleGateway.java`
- Modify: `DefaultTinyEngineOpenAiCompatibleGateway.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/official/TinyEngineSseConsumer.java`
- Modify: `TinyEngineOfficialAiController.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/official/TinyEngineOfficialAiWebFluxController.java`
- Test: create `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/official/TinyEngineOfficialAiStreamingTest.java`
- Test: create `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/official/TinyEngineOfficialAiWebFluxControllerTest.java`

**Interfaces:**
- Produces: `void stream(TinyEngineOfficialAiRequest request, TinyEngineSseConsumer consumer)`。
- Produces: MVC `StreamingResponseBody` 与 WebFlux `Flux<DataBuffer>` 适配。

- [ ] **Step 1: 写有时间间隔的 SSE 失败测试**

假上游依次发送：

```text
data: {"choices":[{"delta":{"content":"A"}}]}

data: {"choices":[{"delta":{"tool_calls":[{"id":"call_1"}]}}]}

data: [DONE]

```

测试断言第一个事件在上游结束前到达，防止先收集为 List；同时验证客户端取消后上游连接关闭。

- [ ] **Step 2: 运行测试确认当前 List 流不兼容**

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am -Dtest='TinyEngineOfficialAiStreamingTest,TinyEngineOfficialAiWebFluxControllerTest' test
```

Expected: FAIL。

- [ ] **Step 3: 实现逐块转发**

网关按读取到的 SSE 原始帧调用 consumer，不解析后再重建 `choices.delta`。保留 `[DONE]`、reasoning、工具调用和厂商字段。

- [ ] **Step 4: 适配 MVC 与 WebFlux**

MVC 设置 `text/event-stream`、`Cache-Control: no-cache`、`X-Accel-Buffering: no`；WebFlux 在独立调度器执行阻塞上游读取，并把取消信号传给 HTTP 请求。

- [ ] **Step 5: 运行 SSE 双栈测试**

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am -Dtest='TinyEngineOfficialAiStreamingTest,TinyEngineOfficialAiWebFluxControllerTest' test
git diff --check -- fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure
```

Expected: PASS；首帧实时到达，取消释放资源。

---

### Task 10: 接入官方 RAG 并保护当前 AI 规范

**Files:**
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/official/TinyEngineOfficialRagGateway.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/official/DefaultTinyEngineOfficialRagGateway.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/official/TinyEngineOfficialAiController.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/official/TinyEngineOfficialAiWebFluxController.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/TinyEngineAiAutoConfiguration.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/TinyEngineAiControllerTest.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/TinyEngineAiWebAutoConfigurationTest.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/TinyEngineAiWebFluxAutoConfigurationTest.java`
- Test: create `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/official/TinyEngineOfficialRagGatewayTest.java`

**Interfaces:**
- Consumes: 现有 `TinyEngineRagService`。
- Produces: 官方 POST `/app-center/api/ai/search` 响应，字段为 `score`、`content`、`doc_name`。

- [ ] **Step 1: 写官方映射与旧契约回归测试**

官方测试断言字段名；旧测试继续断言 `/tiny-engine/ai/search` 返回 `TinyEngineRagSearchResult`，两者不得共用同一序列化外形。

- [ ] **Step 2: 运行测试确认官方 search 缺失**

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am -Dtest='TinyEngineOfficialRagGatewayTest,TinyEngineAiControllerTest,TinyEngineAiWebAutoConfigurationTest,TinyEngineAiWebFluxAutoConfigurationTest' test
```

Expected: 官方测试 FAIL，旧测试保持现状。

- [ ] **Step 3: 实现官方映射和不可用错误**

RAG 关闭或依赖缺失时返回明确不可用错误；不得用空结果伪装成功。

- [ ] **Step 4: 为官方 Bean 添加条件配置**

`official.enabled=false` 时不注册官方四个路由；现有 `web.enabled` 行为不受影响。所有默认 Bean 使用 `@ConditionalOnMissingBean`。

- [ ] **Step 5: 运行全部 AI 聚焦测试**

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am test
git diff --check -- fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure
```

Expected: 当前规范与官方规范测试同时 PASS。

---

### Task 11: 清理样例凭据并补齐前端/打包契约

**Files:**
- Modify: `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-tiny-engine-sample/src/main/resources/application.yml`
- Modify: `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-tiny-engine-sample/src/test/resources/application.yml`
- Modify: `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-tiny-engine-sample/src/test/java/com/xiongx9527/fw/plus/ai/tinyengine/TinyEngineAiSampleApplicationTest.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/test/js/tiny-engine-ai-frontend-contract.test.mjs`
- Modify: `docs/tiny-engine/starter.md`

**Interfaces:**
- Produces: 无硬编码模型、地址、Key 的可启动样例。
- Produces: 前端官方 AI URL 和 Robot 默认预设不被 Starter 改写的结构测试。

- [ ] **Step 1: 写前端与配置失败测试**

Node 测试扫描样例配置和前端资源，断言：

```javascript
assert.doesNotMatch(sampleYaml, /^\s+(baseUrl|apiKey|modelName):\s*\S+/m)
assert.match(robotApi, /app-center\/api\/chat\/completions/)
assert.match(robotApi, /app-center\/api\/ai\/chat/)
assert.match(modelConfig, /provider:\s*'bailian'/)
assert.match(modelConfig, /provider:\s*'deepseek'/)
```

- [ ] **Step 2: 运行测试确认当前样例泄漏内置配置**

Run:

```bash
node --test fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/test/js/tiny-engine-ai-frontend-contract.test.mjs
```

Expected: FAIL，命中现有硬编码 AI 配置。

- [ ] **Step 3: 清理配置并更新说明**

保留 TinyEngine 和数据库配置；移除具体 AI provider/model/base URL/API Key。文档说明 Robot 中添加自定义服务，以及可选 `SM4KEY`/`fw.plus.tiny-engine.ai.official.sm4-key` 的使用方式，但示例值只能是占位符。

- [ ] **Step 4: 运行前端结构测试和样例上下文测试**

Run:

```bash
node --test fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/test/js/*.test.mjs
./fw-plus-core/mvnw -f fw-plus-sample/pom.xml -pl fw-plus-ai-samples/fw-plus-ai-tiny-engine-sample -am test
```

Expected: PASS；样例启动不要求 AI Key。

- [ ] **Step 5: 使用 pnpm 构建前端并检查 JAR**

Run:

```bash
cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console
pnpm install --frozen-lockfile
pnpm run build
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework
./fw-plus-core/mvnw -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter -am package -DskipTests
jar tf fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/target/*.jar | rg '^META-INF/resources/tiny-engine/(index|preview)\.html$'
```

Expected: pnpm/Maven 成功；JAR 同时含 `index.html` 与 `preview.html`。

---

### Task 12: 全量回归、MySQL、浏览器与真实 AI 验收

**Files:**
- Modify: `docs/tiny-engine/official-api-contracts.json`（只把已验证项标记为 `implemented`）
- Modify: `docs/tiny-engine/backend-source-coverage.md`
- Create: `docs/tiny-engine/verification/2026-08-05-official-api-parity.md`
- No production changes unless a failing verification first receives a regression test.

**Interfaces:**
- Consumes: Tasks 1-11 全部产物。
- Produces: 可复核的命令、状态码、数据库行为、浏览器网络和 AI 证据。

- [ ] **Step 1: 执行 Java 8 分层构建**

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -am test
./fw-plus-core/mvnw -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure,fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter -am test
./fw-plus-core/mvnw -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure,fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-spring-boot-starter -am test
./fw-plus-core/mvnw -f fw-plus-sample/pom.xml -pl fw-plus-ai-samples/fw-plus-ai-tiny-engine-sample -am test
```

Expected: 四组均 `BUILD SUCCESS`。

- [ ] **Step 2: 启动 MySQL 样例并执行接口清单探针**

使用现有本机 `tiny_engine` 数据库。启动前验证端口 `18083` 和数据库连接；不得删除或重建用户数据库。对契约清单中的 GET 路由执行只读探针，对写路由使用专用 `example` 应用数据并记录清理范围。

Expected: 没有相关 404；错误输入返回契约定义的 `4xx`，不是 NPE/500。

- [ ] **Step 3: 浏览器验证核心工作流**

打开：

```text
http://127.0.0.1:18083/tiny-engine/index.html?type=app&id=1&tenant=system
```

依次验证扩展加载、页面保存、页面预览、应用预览、国际化批量删除、资源和历史操作。检查控制台和 Network，不得出现本任务范围内的 404、未处理 Promise、`Object.keys(null)` 或缺少 `defaultAvator.png`。

- [ ] **Step 4: 使用临时凭据验证真实 AI**

在 Robot UI 中添加用户指定的 OpenAI 兼容服务、模型和 Key，验证非流式与流式消息。凭据不得写入任何文件；验证文档只记录上游主机、模型名称、HTTP 状态、首帧耗时和完成状态，不记录 Key 或正文。

- [ ] **Step 5: 完成证据文档与最终安全扫描**

Run:

```bash
rg -n "sk-[A-Za-z0-9]{16,}|gpt\.xinshu\.ai" fw-plus-core fw-plus-boot fw-plus-ai fw-plus-sample docs || true
rg -n "List\.of\(|Map\.of\(|String\.isBlank\(|readAllBytes\(|java\.net\.http" fw-plus-core/fw-plus-project/fw-plus-tiny-engine fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure || true
git diff --check
git status --short
```

Expected: 无密钥；无新增 Java 9+ API；`git diff --check` 无输出；状态只包含任务拥有和用户原有文件。

- [ ] **Step 6: 逐项关闭契约清单**

只有同时具备自动化测试和实际行为证据的项目才能标记为 `implemented`。最终 `official-api-contracts.json` 不得存在 `missing`；归并掉的候选必须保留 `mergedInto` 依据，不能直接删除。
