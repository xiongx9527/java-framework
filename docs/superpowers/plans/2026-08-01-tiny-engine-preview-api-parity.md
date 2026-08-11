# TinyEngine Preview API Parity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 补齐当前 TinyEngine 设计器初始化、页面预览和应用预览实际依赖但框架尚未暴露的后端 API。

**Architecture:** 保持 TinyEngine Core Service 为数据访问边界，由 Boot `TinyEngineApiFacade` 组合响应，再由 MVC 与 WebFlux Controller 提供一致路由。以 `/Users/xiongxiao/Workspace/Idea/codes/tiny-engine-backend-java` 的 Controller 契约为参考，但只实现当前打包前端实际阻断的接口。

**Tech Stack:** Java 8、Spring Boot 2.7.18、Spring MVC、Spring WebFlux、JUnit 5、Mockito、MockMvc、WebTestClient。

## Global Constraints

- 必须兼容 Java 8，不使用 Java 9+ API。
- 保留当前工作区已有改动与已暂存删除。
- 不修改独立后端仓库，不提交、不推送。
- MVC 与 WebFlux 必须暴露同等路由。

---

### Task 1: 应用扩展查询 API

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEngineAppPageMvcApiContractTest.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/api/TinyEngineApiFacade.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/api/DefaultTinyEngineApiFacade.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEngineAppMvcController.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webflux/TinyEngineAppWebFluxController.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/TinyEngineApiConfiguration.java`

**Interfaces:**
- Consumes: `AppExtensionService.findAppExtensionByCondition(AppExtension)`。
- Produces: `List<AppExtension> appExtensions(Integer app, String category)` 与 `GET /app-center/api/apps/extension/list`。

- [ ] 写 MockMvc 失败测试，断言响应列表并捕获 `app=1`、`category=utils`。
- [ ] 运行单测，确认因路由缺失得到 404。
- [ ] 在 Facade、MVC、WebFlux 与自动配置中补齐最小实现。
- [ ] 重跑单测，确认通过。

### Task 2: 国际化批量删除 API

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEngineMaterialModelMvcApiContractTest.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/api/TinyEngineApiFacade.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/api/DefaultTinyEngineApiFacade.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEngineModelMvcController.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webflux/TinyEngineModelWebFluxController.java`

**Interfaces:**
- Consumes: `I18nEntryService.deleteI18nEntriesByHostAndHostTypeAndKey(DeleteI18nEntry)`。
- Produces: `List<I18nEntryDto> deleteI18nEntries(DeleteI18nEntry)` 与 `POST /app-center/api/i18n/entries/bulk/delete`。

- [ ] 写 MockMvc 失败测试，使用 `host`、`host_type`、`key_in` 请求体并校验委托参数。
- [ ] 运行单测，确认因路由缺失得到 404。
- [ ] 补齐 Facade、MVC 与 WebFlux 路由。
- [ ] 重跑单测，确认通过。

### Task 3: 页面与应用预览元数据 API

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEngineAppPageMvcApiContractTest.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/api/TinyEngineApiFacade.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/api/DefaultTinyEngineApiFacade.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEnginePageMvcController.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webflux/TinyEnginePageWebFluxController.java`

**Interfaces:**
- Consumes: `AppService.queryAppById`、`DatasourceService.queryDatasourceByCondition`、`I18nEntryService.findI18nEntryByApp`、`AppExtensionService.findAppExtensionByCondition`。
- Produces: `PreviewDto previewMetadata(PreviewParam)` 与 `GET /app-center/api/preview/metadata`。

- [ ] 写 MockMvc 失败测试，断言 `dataSource.list`、`globalState`、`i18n`、`utils` 的响应结构。
- [ ] 运行单测，确认因路由缺失得到 404。
- [ ] 在 Facade 组合应用级预览元数据，并为 MVC/WebFlux 绑定 `PreviewParam`。
- [ ] 在 WebFlux 映射清单中加入三个新路由并重跑测试。

### Task 4: Java 8 构建与真实页面验证

**Files:**
- Verify only: `fw-plus-core`、`fw-plus-boot`、`fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-tiny-engine-sample`

**Interfaces:**
- Consumes: 本地 Maven 仓库中的 `0.1.1-SNAPSHOT` TinyEngine Core/Boot JAR。
- Produces: `http://127.0.0.1:18083/tiny-engine/index.html?type=app&id=1&tenant=system` 可用设计器与预览。

- [ ] 使用 Zulu Java 8 运行聚焦测试与 Maven install。
- [ ] 受控重启样例，curl 验证三个接口不再 404。
- [ ] 刷新设计器，分别点击页面预览和应用预览，检查新标签页及控制台。
- [ ] 根据真实页面日志补充遗漏的直接阻断接口，随后执行 `git diff --check`。
