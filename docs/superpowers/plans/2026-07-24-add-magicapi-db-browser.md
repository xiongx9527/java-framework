---
change: add-magicapi-db-browser
design-doc: docs/superpowers/specs/2026-07-24-add-magicapi-db-browser-design.md
base-ref: bf45ed49f309cca5d18c623a03dab70c72cbfa1e
archived-with: 2026-07-24-add-magicapi-db-browser
---

# Magic-API DB 只读浏览与插件 UI 归属实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Magic-API 中新增 JDBC 数据源只读 DB 浏览器，并把 DB/S3 管理 UI 都收敛到各自插件 UI 中。

**Architecture:** 后端在 `fw-plus-boot-autoconfigure` 增加 DB 浏览 DTO、Service、Controller 和插件配置注册，复用 Magic-API 已注册的 `MagicDynamicDataSource`。前端新增独立 `fw-plus-magicapi-plugin-db/src/console` 插件 UI，并将宿主控制台中的 S3 文件管理页面迁移到 `fw-plus-magicapi-plugin-s3/src/console`。宿主控制台只保留通用插件加载/挂载能力。

**Tech Stack:** Java 8、Spring Boot 2.7.18、Magic-API 2.2.2、JDBC `DatabaseMetaData`、Vue 3、Vite、Magic-API 控制台插件 IIFE。

## Global Constraints

- 所有 Java 代码必须兼容 JDK 1.8，不使用 Java 9+ API。
- 后端 DB 浏览接口只读，不接受任意 SQL，不执行 DDL/DML，不调用函数或存储过程。
- identifier 必须来自元数据白名单，并使用数据库 quote string 处理；过滤值必须使用 `PreparedStatement` 参数绑定。
- DB 插件 UI 必须位于独立 Magic-API 插件 `src/console`，不得写入宿主 `fw-plus-magicapi-spring-boot-starter/src/console` 业务页面。
- S3 文件管理 UI 必须迁入 `fw-plus-magicapi-plugin-s3/src/console`，宿主控制台不得保留 S3 业务页面、S3 专属状态栏/工具栏逻辑或 S3 专属 i18n。
- 不修改 datasource JSON 持久化格式，不改变 Magic-API 脚本侧 `db` API。
- 保持现有包名 `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi` 风格。

---

## 文件结构

### 后端新增文件

- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser/model/DbBrowserDataSourceInfo.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser/model/DbBrowserNamespace.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser/model/DbBrowserObjectInfo.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser/model/DbBrowserColumnInfo.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser/model/DbBrowserIndexInfo.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser/model/DbBrowserRoutineInfo.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser/model/DbBrowserObjectDetail.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser/model/DbBrowserPreviewRequest.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser/model/DbBrowserPreviewResult.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser/model/DbBrowserFilter.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser/service/DbBrowserService.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser/service/DefaultDbBrowserService.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser/service/DbBrowserSqlBuilder.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser/web/MagicDbBrowserController.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser/starter/MagicAPIDbBrowserConfiguration.java`

### 后端修改文件

- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/resources/META-INF/spring.factories`，注册 DB 浏览插件自动配置。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/pom.xml` 或同层聚合 POM，加入 DB 插件模块；若插件聚合 POM 已是唯一聚合入口，只修改该文件。
- Modify: Boot starter 依赖聚合文件，确保 DB 插件资源可进入最终包；若现有插件自动配置通过 `spring.factories` 和插件模块 POM 自动纳入，则在 Task 4 提交说明中记录无需修改的判断。

### 前端新增 DB 插件文件

- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-db/pom.xml`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-db/src/console/package.json`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-db/src/console/vite.config.js`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-db/src/console/src/index.js`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-db/src/console/src/service/magic-db-browser.js`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-db/src/console/src/components/magic-db-browser.vue`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-db/src/console/src/components/magic-db-object-detail.vue`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-db/src/console/src/i18n/zh-cn.js`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-db/src/console/src/i18n/en.js`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-db/src/console/src/icons/db.svg`

### S3 插件迁移文件

- Modify/Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/src/components/magic-s3-file-manager.vue`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/src/index.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/src/service/magic-s3-datasource.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/src/i18n/zh-cn.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/src/i18n/en.js`

### 宿主 UI 清理文件

- Delete: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/src/components/panel/main/magic-s3.vue`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/src/components/panel/footer/magic-toolbar.vue`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/src/components/panel/footer/magic-status-bar.vue`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/src/scripts/i18n/zh-cn.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/src/scripts/i18n/en.js`

### 测试与文档

- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser/service/DefaultDbBrowserServiceTest.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser/service/DbBrowserSqlBuilderTest.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser/web/MagicDbBrowserControllerTest.java`
- Create/Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/README.md`

---

### Task 1: 后端 DTO 与 SQL Builder 安全骨架

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser/model/*.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser/service/DbBrowserSqlBuilder.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser/service/DbBrowserSqlBuilderTest.java`

**Interfaces:**
- Produces: `DbBrowserPreviewRequest`、`DbBrowserPreviewResult`、`DbBrowserFilter`、`DbBrowserSqlBuilder.BuildResult`。
- Produces: `DbBrowserSqlBuilder.buildPreviewSql(Connection, DbBrowserPreviewRequest, Set<String> allowedColumns, Set<String> allowedObjects)` 返回 SQL 和参数列表。

- [x] **Step 1: 写 SQL Builder 失败测试**

测试覆盖：未知排序字段被拒绝、未知过滤字段被拒绝、pageSize 超限被截断、过滤值进入参数列表而不是 SQL 字符串。

Run:
```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=DbBrowserSqlBuilderTest test
```

Expected: FAIL，原因是 `DbBrowserSqlBuilder` 和 DTO 尚不存在。

- [x] **Step 2: 实现 DTO 和最小 SQL Builder**

实现 Java 8 POJO，使用 getter/setter，不用 `List.of()` 或 record。SQL Builder 只支持 SELECT 预览，操作符先实现 `eq`、`ne`、`like`、`gt`、`gte`、`lt`、`lte`、`isNull`、`isNotNull`。

- [x] **Step 3: 跑 SQL Builder 测试**

Run:
```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=DbBrowserSqlBuilderTest test
```

Expected: PASS。

- [x] **Step 4: 提交 Task 1**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser/service/DbBrowserSqlBuilderTest.java
git commit -m "feat: add magicapi db browser sql builder"
```

### Task 2: 后端元数据服务

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser/service/DbBrowserService.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser/service/DefaultDbBrowserService.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser/service/DefaultDbBrowserServiceTest.java`

**Interfaces:**
- Consumes: Task 1 DTO 和 `DbBrowserSqlBuilder`。
- Produces: `listDataSources()`、`listNamespaces(String key)`、`listObjects(String key, String catalog, String schema, String type)`、`getObjectDetail(...)`、`preview(DbBrowserPreviewRequest)`。

- [x] **Step 1: 写元数据服务失败测试**

使用 H2 或可控 DataSource 建表和视图，断言表/视图列表、字段、主键、索引和分页预览返回结构稳定。

Run:
```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=DefaultDbBrowserServiceTest test
```

Expected: FAIL，原因是服务不存在。

- [x] **Step 2: 实现 `DefaultDbBrowserService`**

从现有 `MagicDynamicDataSource` 获取已注册数据源连接；优先通过 `DatabaseMetaData` 读取 catalog、schema、tables、views、procedures、functions、columns、primary keys、indexes。驱动未返回的字段保持 null。

- [x] **Step 3: 接入只读预览**

预览前读取目标表/视图元数据，生成 allowed object/columns，调用 SQL Builder，`PreparedStatement` 绑定参数，请求 `pageSize + 1` 判断 `hasMore`。

- [x] **Step 4: 跑服务测试**

Run:
```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=DefaultDbBrowserServiceTest test
```

Expected: PASS。

- [x] **Step 5: 提交 Task 2**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser/service fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser/service
git commit -m "feat: add magicapi db metadata browser service"
```

### Task 3: 后端 Controller 与自动配置

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser/web/MagicDbBrowserController.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser/starter/MagicAPIDbBrowserConfiguration.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/resources/META-INF/spring.factories`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser/web/MagicDbBrowserControllerTest.java`

**Interfaces:**
- Consumes: Task 2 `DbBrowserService`。
- Produces endpoints under `/db/browser/**` or `/db/datasource/**` following existing plugin Controller style.

- [x] **Step 1: 写 Controller 失败测试**

覆盖 datasource 列表、对象列表、详情、预览、未知 datasource、未知列、未认证/无 VIEW 权限。

- [x] **Step 2: 实现 Controller 权限校验**

参照 `MagicS3Controller.platformOptions()` 的 `@Valid(requireLogin = true)` 和 `Authorization.VIEW` 检查方式，避免绕过控制台权限。

- [x] **Step 3: 实现 `MagicAPIDbBrowserConfiguration`**

提供 `DbBrowserService` Bean、`Plugin("db", "MagicDB", "magic-db.1.0.0.iife.js")`，并通过 `controllerRegister()` 注册 `MagicDbBrowserController`。

- [x] **Step 4: 注册自动配置并跑测试**

Run:
```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicDbBrowserControllerTest test
```

Expected: PASS。

- [x] **Step 5: 提交 Task 3**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/resources/META-INF/spring.factories fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/dbbrowser/web/MagicDbBrowserControllerTest.java
git commit -m "feat: expose magicapi db browser endpoints"
```

### Task 4: 新增 DB 插件 UI

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-db/**`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/pom.xml`

**Interfaces:**
- Consumes: Task 3 DB 浏览接口。
- Produces: `magic-db.1.0.0.iife.js` 插件资源，注册 DB 管理入口、服务、图标、i18n、页面组件。

- [x] **Step 1: 复制 S3 插件 UI 的最小结构**

按 `fw-plus-magicapi-plugin-s3/src/console` 创建 DB 插件 `package.json`、`vite.config.js`、`src/index.js`、i18n、service、components 和 icon。

- [x] **Step 2: 实现 service 封装**

`magic-db-browser.js` 封装 `listDataSources`、`listNamespaces`、`listObjects`、`getObjectDetail`、`preview`，统一使用插件运行时注入的 `request`。

- [x] **Step 3: 实现 DB 浏览页面**

左侧展示数据源和对象树；右侧展示对象详情。表/视图展示字段、主键、索引、分页预览；函数/存储过程只展示元数据。

- [x] **Step 4: 实现排序、过滤、分页和状态**

支持基础排序、简单过滤、加载态、空态、错误态；后端失败时保留当前数据源和已加载上下文。

- [x] **Step 5: 构建 DB 插件**

Run:
```bash
cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-db/src/console && npm install && npm run build
```

Expected: PASS，生成 `dist/magic-db.1.0.0.iife.js`。

- [x] **Step 6: 提交 Task 4**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-db fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/pom.xml
git commit -m "feat: add magicapi db browser console plugin"
```

### Task 5: S3 文件管理 UI 迁回插件 UI

**Files:**
- Modify/Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/src/components/magic-s3-file-manager.vue`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/src/index.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/src/service/magic-s3-datasource.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/src/i18n/zh-cn.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/src/i18n/en.js`

**Interfaces:**
- Consumes: existing `MagicS3Controller` endpoints `/s3/datasource/availableStorages`、`/s3/datasource/{key}/listFilesResult`、`/delete`、`/generateUploadPresignedUrl`。
- Produces: S3 插件 IIFE 自己注册文件管理入口和页面。

- [x] **Step 1: 从宿主页面迁移 S3 文件管理组件**

将宿主 `magic-s3.vue` 的文件浏览、搜索、存储选择、basePath 展示/编辑、列表、删除、预签名上传入口迁入 S3 插件组件。

- [x] **Step 2: 扩展 S3 service**

在 S3 插件 service 中封装文件管理接口，保持 datasource 表单能力不回退。

- [x] **Step 3: 扩展 S3 插件注册**

在 `src/index.js` 中注册 S3 数据源表单和 S3 文件管理入口。入口只由插件产物提供，不依赖宿主专属 toolbar/status-bar 分支。

- [x] **Step 4: 迁移 S3 i18n 和图标引用**

把宿主中的 S3 文件管理文案迁到 S3 插件 i18n；图标使用插件内 `s3.svg` 或 IIFE 注入 symbol。

- [x] **Step 5: 构建 S3 插件**

Run:
```bash
cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console && npm run build
```

Expected: PASS，生成 `dist/magic-s3.1.0.0.iife.js`。

- [x] **Step 6: 提交 Task 5**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console
git commit -m "feat: move magic s3 file manager into plugin ui"
```

### Task 6: 清理宿主控制台 S3 业务 UI

**Files:**
- Delete: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/src/components/panel/main/magic-s3.vue`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/src/components/panel/footer/magic-toolbar.vue`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/src/components/panel/footer/magic-status-bar.vue`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/src/scripts/i18n/zh-cn.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/src/scripts/i18n/en.js`

**Interfaces:**
- Consumes: Task 5 S3 插件入口。
- Produces: 宿主控制台无 S3 业务页面和专属入口，只保留通用插件加载能力。

- [x] **Step 1: 删除宿主 S3 页面组件**

移除 `magic-s3.vue`，并删除所有直接引用。

- [x] **Step 2: 清理 toolbar/status-bar S3 专属逻辑**

删除 `hasS3Plugin`、`openS3`、`s3-datasource` 专属判断和 `resolveDynamicComponent('magic-s3')` 入口。保留通用插件数据驱动的加载逻辑。

- [x] **Step 3: 清理宿主 S3 i18n**

删除宿主 `toolbars.s3` 和 `s3.*` 文件管理文案。不要删除插件通用文案。

- [x] **Step 4: 构建宿主控制台**

Run:
```bash
cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console && npm run build
```

Expected: PASS。

- [x] **Step 5: 源码检查**

Run:
```bash
rg -n "magic-s3|s3-datasource|toolbars\.s3|openS3|hasS3Plugin" fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/src
```

Expected: 无宿主 S3 业务页面、入口和专属文案残留。

- [x] **Step 6: 提交 Task 6**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console
git commit -m "refactor: remove s3 business ui from magicapi host console"
```

### Task 7: 集成构建、文档与 OpenSpec 任务收口

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/README.md`
- Modify: `openspec/changes/add-magicapi-db-browser/tasks.md`

**Interfaces:**
- Consumes: Tasks 1-6 全部产物。
- Produces: 验证证据和任务勾选。

- [x] **Step 1: 更新说明文档**

记录 DB 浏览只读范围、支持对象类型、数据预览限制、插件 UI 边界，以及 S3 文件管理 UI 已归属 S3 插件。

- [x] **Step 2: 运行后端目标测试**

Run:
```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=DbBrowserSqlBuilderTest,DefaultDbBrowserServiceTest,MagicDbBrowserControllerTest test
```

Expected: PASS。

- [x] **Step 3: 运行前端构建**

Run:
```bash
(cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-db/src/console && npm run build)
(cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console && npm run build)
(cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console && npm run build)
```

Expected: 三个构建均 PASS。

- [x] **Step 4: 运行 OpenSpec 和 diff 检查**

Run:
```bash
openspec validate add-magicapi-db-browser
git diff --check
```

Expected: PASS。

- [x] **Step 5: 勾选 OpenSpec tasks**

按实际完成情况勾选 `openspec/changes/add-magicapi-db-browser/tasks.md` 中 1.1-5.7。

- [x] **Step 6: 提交 Task 7**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/README.md openspec/changes/add-magicapi-db-browser/tasks.md
git commit -m "docs: document magicapi db browser plugin boundaries"
```

## 自检清单

- [x] 每个 OpenSpec requirement 都至少由一个任务覆盖。
- [x] DB/S3 插件 UI 都位于插件 `src/console`。
- [x] 宿主控制台没有 S3/DB 业务页面和专属入口。
- [x] DB 数据预览没有任意 SQL 输入，没有写操作入口。
- [x] 后端测试、前端构建、`openspec validate`、`git diff --check` 已通过。
