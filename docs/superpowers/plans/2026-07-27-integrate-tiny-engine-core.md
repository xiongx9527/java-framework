---
change: integrate-tiny-engine-core
design-doc: docs/superpowers/specs/2026-07-27-integrate-tiny-engine-core-design.md
base-ref: 9c48128246f99024d263c71136e9f0e5186cefc6
archived-with: 2026-07-28-integrate-tiny-engine-core
---

# TinyEngine Core Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `tiny-engine-backend-java` 的低代码后端基础能力提炼为 `fw-plus-core` 下的 `fw-plus-tiny-engine-core` artifact，供后续 Boot starter 和 AI starter 复用。

**Architecture:** 在 `fw-plus-core/fw-plus-project` 下新增 `fw-plus-tiny-engine` 聚合模块，首版只包含 `fw-plus-tiny-engine-core`。Core 模块只承载领域模型、DTO、Mapper/XML、SQL 资源清单、核心服务、动态模型和安全边界；Web 暴露、自动配置、starter、AI/RAG、Docker 和运行 profile 留给后续 change。

**Tech Stack:** Java 8, Spring Boot 2.7.18, Maven Wrapper, MyBatis 3.5.19, MyBatis-Plus 3.3.2, Hutool 5.8.34, Fastjson/Fastjson2 2.0.53, JUnit, MyBatis XML, classpath SQL resources.

## Global Constraints

- 以 Design Doc `docs/superpowers/specs/2026-07-27-integrate-tiny-engine-core-design.md` 为主，`openspec/changes/integrate-tiny-engine-core/tasks.md` 只作为任务边界。
- 只能新增和修改 TinyEngine core 所需文件；不得实现 Controller、Spring Boot 自动配置、starter、启动类、OpenAPI、Servlet Filter、profile 配置、Docker 部署、AI Chat、RAG、VectorStorage 或 LangChain4j 1.x。
- 新包名统一为 `com.xiongx9527.fw.plus.tinyengine`，不得保留 `com.tinyengine.it`。
- Core 层保持 Java 8 / Spring Boot 2.7.18 兼容，禁止 `jakarta.*`、Java 9+ API、Spring Boot 3-only API、Springdoc OpenAPI 2.x。
- MyBatis-Plus 版本收敛到 core BOM 当前 `3.3.2`，Magic-API `2.2.2` 相关依赖版本不得修改。
- SQL 初始化资源只作为可发现资源分发，不在 core 中自动执行。
- 动态 SQL 的表名、字段名、条件 key、排序字段、排序方向、DDL 列名、批量字段必须先经 `TinyEngineSqlIdentifierValidator` 校验，用户值必须通过 MyBatis 参数绑定。
- 每个任务完成后运行本任务列出的验证命令；只有实际实现阶段验证通过后才允许勾选本计划中的步骤或 OpenSpec `tasks.md`。

---

## File Structure

### Maven 与 BOM

- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/pom.xml`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/pom.xml`
- Modify: `fw-plus-core/fw-plus-project/pom.xml`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`

### Core Java 包结构

- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/common/`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/model/entity/`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/model/dto/`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/mapper/`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/service/`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/service/impl/`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/dynamic/`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/support/`

### Core 资源结构

- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/resources/mapper/tinyengine/`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/resources/META-INF/tiny-engine/sql/mysql/`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/resources/META-INF/tiny-engine/sql/h2/`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/resources/META-INF/tiny-engine/sql/postgresql/`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/resources/META-INF/tiny-engine/sql/manifest.properties`

### Tests

- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/test/java/com/xiongx9527/fw/plus/tinyengine/common/`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/test/java/com/xiongx9527/fw/plus/tinyengine/mapper/`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/test/java/com/xiongx9527/fw/plus/tinyengine/dynamic/`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/test/java/com/xiongx9527/fw/plus/tinyengine/support/`

---

### Task 1: 新增 TinyEngine Core Maven 模块

**Goal:** 建立空的 `fw-plus-tiny-engine` 聚合模块和 `fw-plus-tiny-engine-core` 子模块，并接入 core 聚合 POM 与 BOM。

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/pom.xml`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/pom.xml`
- Modify: `fw-plus-core/fw-plus-project/pom.xml`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`

**Interfaces:**
- Produces: Maven artifact `com.xiongx9527.fw.plus:fw-plus-tiny-engine-core:${revision}`
- Consumes: parent `com.xiongx9527.fw.plus:fw-plus-parent:${revision}`

**Dependencies:** 无前置任务。

**Validation:**
- `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -DskipTests compile`
- `cd fw-plus-core && ./mvnw -f fw-plus-project/pom.xml -pl fw-plus-tiny-engine -am -DskipTests validate`

- [x] **Step 1: 记录失败基线**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -DskipTests compile
```

Expected: FAIL，因为模块尚不存在或尚未接入聚合。

- [x] **Step 2: 创建聚合 POM**

Create `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/pom.xml` with parent `fw-plus-parent`, packaging `pom`, module `fw-plus-tiny-engine-core`, and `main.basedir` matching sibling aggregators.

- [x] **Step 3: 创建 core 子模块 POM**

Create `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/pom.xml` with parent `fw-plus-tiny-engine`, artifactId `fw-plus-tiny-engine-core`, Java 8 compatible dependencies only. Initial dependencies should be limited to existing managed dependencies needed for compile, such as `fw-plus-common`, MyBatis/MyBatis-Plus, Spring context/tx annotations, Hutool, Fastjson/Fastjson2, and test dependencies inherited or managed by the parent.

- [x] **Step 4: 注册聚合与 BOM**

Modify `fw-plus-core/fw-plus-project/pom.xml` to add `<module>fw-plus-tiny-engine</module>`. Modify `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml` to add dependency management for `fw-plus-tiny-engine-core` with version `${project.version}` unless surrounding fw-plus entries require `${revision}` for consistency.

- [x] **Step 5: 验证模块可编译**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -DskipTests compile
```

Expected: PASS.

- [x] **Step 6: Commit Task 1**

```bash
git add fw-plus-core/fw-plus-project/pom.xml \
  fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml \
  fw-plus-core/fw-plus-project/fw-plus-tiny-engine
git commit -m "feat: add tiny engine core module"
```

### Task 2: 迁移公共基础类型与上下文 SPI

**Goal:** 将 Result、基础实体、异常模型、分页对象和用户上下文改造成无 Web/OpenAPI 强依赖的 core 契约。

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/common/model/Result.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/common/model/BaseEntity.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/common/model/HistoryEntity.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/common/model/PageQueryVo.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/common/exception/IBaseError.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/common/exception/ExceptionEnum.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/common/exception/ServiceException.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/support/context/TinyEngineUser.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/support/context/TinyEngineUserContext.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/support/context/SystemTinyEngineUserContext.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/test/java/com/xiongx9527/fw/plus/tinyengine/common/TinyEngineCommonContractTest.java`

**Interfaces:**
- Produces: `TinyEngineUserContext#getCurrentUser()`, `TinyEngineUser`, `Result`, `ServiceException`
- Consumes: Task 1 module artifact

**Dependencies:** Task 1.

**Validation:**
- `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineCommonContractTest test`

- [x] **Step 1: 写失败测试**

Create `TinyEngineCommonContractTest` verifying `Result` success/failure shape, `ServiceException` carries error code/message, `PageQueryVo` defaults do not require Servlet/OpenAPI types, and `SystemTinyEngineUserContext#getCurrentUser()` returns a deterministic system user.

- [x] **Step 2: 运行失败测试**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineCommonContractTest test
```

Expected: FAIL because classes do not exist.

- [x] **Step 3: 迁移并改造公共类型**

Migrate the public fields and methods needed by TinyEngine services into the new package. Remove `@ResponseStatus`, Swagger/OpenAPI annotations, Servlet types, Boot 3 imports, and direct login implementation references. Keep constructors/getters/setters Java 8 compatible.

- [x] **Step 4: 实现用户上下文 SPI**

Define `TinyEngineUserContext` as a replaceable interface. Provide `SystemTinyEngineUserContext` as the default core implementation with stable system user values so service code can run before Boot starter binds real Sa-Token/Security context.

- [x] **Step 5: 运行测试**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineCommonContractTest test
```

Expected: PASS.

- [x] **Step 6: Commit Task 2**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core
git commit -m "feat: add tiny engine common contracts"
```

### Task 3: 迁移 Java 8 兼容工具与 TypeHandler

**Goal:** 迁移 TinyEngine 所需 JSON、Schema、Hash、文件安全工具和 MyBatis TypeHandler，并建立 Java 8 兼容保护。

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/common/util/TinyEngineJsonUtils.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/common/util/TinyEngineSchemaUtils.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/common/util/TinyEngineHashUtils.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/common/util/TinyEngineFileSecurityUtils.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/common/typehandler/JsonObjectTypeHandler.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/common/typehandler/JsonArrayTypeHandler.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/test/java/com/xiongx9527/fw/plus/tinyengine/common/TinyEngineUtilityTest.java`

**Interfaces:**
- Produces: utility methods used by model/services and XML typeHandler references
- Consumes: Task 2 exception and model types

**Dependencies:** Task 2.

**Validation:**
- `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineUtilityTest test`
- `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -DskipTests compile`

- [x] **Step 1: 写失败测试**

Create tests covering JSON object/array round trip, schema helper behavior required by migrated DTOs, hash determinism, path traversal rejection in file security utilities, and TypeHandler null/empty JSON handling.

- [x] **Step 2: 运行失败测试**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineUtilityTest test
```

Expected: FAIL because utilities and handlers do not exist.

- [x] **Step 3: 迁移工具类并替换不兼容 API**

Replace `String.isBlank()` with Java 8-compatible checks, replace `List.of()` / `Map.of()` with mutable construction plus defensive copies where needed, replace `InputStream.readAllBytes()` with buffered reads, and avoid `java.net.http.HttpClient`.

- [x] **Step 4: 迁移 TypeHandler**

Move MyBatis TypeHandler classes into `common.typehandler`, update package names, and ensure handler FQCNs are stable for mapper XML references.

- [x] **Step 5: 运行测试与编译**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineUtilityTest test
./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -DskipTests compile
```

Expected: PASS.

- [x] **Step 6: Commit Task 3**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core
git commit -m "feat: add tiny engine utilities"
```

### Task 4: 迁移领域实体与 DTO

**Goal:** 将 TinyEngine 主要领域实体和 service DTO 迁移到新包名，并保证 Java 8 / MyBatis-Plus 3.3.2 可编译。

**Files:**
- Create: files under `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/model/entity/`
- Create: files under `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/model/dto/`
- Test: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/test/java/com/xiongx9527/fw/plus/tinyengine/model/TinyEngineModelContractTest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/test/resources/tinyengine/model-domain-checklist.txt`

**Interfaces:**
- Produces: entities and DTOs for app, page, page history, page template, canvas, block, block group, material, material history, component, component library, resource, resource group, business category, i18n, platform, tenant, user, extension, task record, model, datasource, and relation tables
- Consumes: Task 2 base model types and Task 3 JSON TypeHandler FQCNs

**Dependencies:** Task 2, Task 3.

**Validation:**
- `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineModelContractTest test`
- `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -DskipTests compile`

- [x] **Step 1: 写领域核对清单测试**

Create `TinyEngineModelContractTest` that loads `tinyengine/model-domain-checklist.txt` and asserts every listed class exists under `com.xiongx9527.fw.plus.tinyengine.model.entity` or `model.dto`.

- [x] **Step 2: 运行失败测试**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineModelContractTest test
```

Expected: FAIL because migrated model classes do not exist.

- [x] **Step 3: 迁移实体**

Move entity classes by domain in small batches. Update package names, imports, table annotations, field annotations, inheritance from `BaseEntity` / `HistoryEntity`, and JSON field type references. Remove Web/OpenAPI annotations and any Controller-only validation assumptions.

- [x] **Step 4: 迁移 DTO**

Move request/response/service DTOs required by service interfaces. Keep DTOs free of Servlet/OpenAPI annotations; validation annotations may use `javax.validation.*` only when already managed and required by service code.

- [x] **Step 5: 运行测试与编译**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineModelContractTest test
./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -DskipTests compile
```

Expected: PASS.

- [x] **Step 6: Commit Task 4**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core
git commit -m "feat: migrate tiny engine models"
```

### Task 5: 迁移 Mapper 接口、XML 与 SQL 资源清单

**Goal:** 将 TinyEngine 持久化层迁移到 core classpath，并保证 Mapper XML namespace、resultMap、typeHandler 与新包名一致。

**Files:**
- Create: files under `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/mapper/`
- Create: files under `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/resources/mapper/tinyengine/`
- Create: SQL files under `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/resources/META-INF/tiny-engine/sql/mysql/`
- Create: SQL files under `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/resources/META-INF/tiny-engine/sql/h2/`
- Create: SQL files under `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/resources/META-INF/tiny-engine/sql/postgresql/`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/resources/META-INF/tiny-engine/sql/manifest.properties`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/support/sql/TinyEngineSqlScriptManifest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/test/java/com/xiongx9527/fw/plus/tinyengine/mapper/TinyEngineMapperResourceTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/test/java/com/xiongx9527/fw/plus/tinyengine/support/TinyEngineSqlScriptManifestTest.java`

**Interfaces:**
- Produces: Mapper interfaces, mapper XML resources under `classpath*:mapper/tinyengine/**/*.xml`, SQL resources under `META-INF/tiny-engine/sql/`, manifest reader `TinyEngineSqlScriptManifest`
- Consumes: Task 4 entities/DTOs and Task 3 TypeHandlers

**Dependencies:** Task 3, Task 4.

**Validation:**
- `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineMapperResourceTest,TinyEngineSqlScriptManifestTest test`

- [x] **Step 1: 写 Mapper/XML 失败测试**

Create `TinyEngineMapperResourceTest` to scan `mapper/tinyengine/**/*.xml`, parse XML, and assert namespace starts with `com.xiongx9527.fw.plus.tinyengine.mapper.` and does not contain `com.tinyengine.it`.

- [x] **Step 2: 写 SQL manifest 失败测试**

Create `TinyEngineSqlScriptManifestTest` verifying `manifest.properties` exposes ordered scripts for `mysql`, `h2`, and `postgresql`, and every referenced resource exists on the classpath.

- [x] **Step 3: 运行失败测试**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineMapperResourceTest,TinyEngineSqlScriptManifestTest test
```

Expected: FAIL because mapper and SQL resources do not exist.

- [x] **Step 4: 迁移 Mapper 接口**

Move Mapper interfaces into `com.xiongx9527.fw.plus.tinyengine.mapper`, update imports/generics for MyBatis-Plus 3.3.2, and keep mapper interfaces free of Boot starter concerns.

- [x] **Step 5: 迁移 Mapper XML**

Move XML files into `src/main/resources/mapper/tinyengine/`, update namespace, resultMap type, parameterType, resultType, typeHandler FQCN, SQL fragments, and entity/DTO FQCNs.

- [x] **Step 6: 迁移 SQL 初始化资源**

Place SQL files under the three database directories. Implement `manifest.properties` with explicit ordered keys per database, for example `mysql.001=classpath:META-INF/tiny-engine/sql/mysql/001_schema.sql`. Do not execute SQL in core.

- [x] **Step 7: 运行测试**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineMapperResourceTest,TinyEngineSqlScriptManifestTest test
```

Expected: PASS.

- [x] **Step 8: Commit Task 5**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core
git commit -m "feat: migrate tiny engine persistence resources"
```

### Task 6: 迁移核心服务接口、实现与扩展点

**Goal:** 迁移 app/page/material/resource/i18n/platform/tenant/user/task/model/datasource 核心服务，并剥离 Controller、OpenAPI、启动类、过滤器、定时任务和 profile 配置依赖。

**Files:**
- Create: files under `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/service/`
- Create: files under `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/service/impl/`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/support/hook/TinyEngineSaveHook.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/support/hook/NoopTinyEngineSaveHook.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/test/java/com/xiongx9527/fw/plus/tinyengine/service/TinyEngineServiceContractTest.java`

**Interfaces:**
- Produces: core service interfaces and Spring `@Service` implementations that can be imported by a future Boot starter
- Consumes: Task 2 context SPI, Task 4 models, Task 5 mappers/resources

**Dependencies:** Task 2, Task 4, Task 5.

**Validation:**
- `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineServiceContractTest test`
- `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -DskipTests compile`

- [x] **Step 1: 写服务契约失败测试**

Create `TinyEngineServiceContractTest` verifying every public service interface has an implementation class, implementations do not depend on controller packages, and extension points default to no-op behavior.

- [x] **Step 2: 运行失败测试**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineServiceContractTest test
```

Expected: FAIL because services do not exist.

- [x] **Step 3: 迁移服务接口**

Move service interfaces into `service`, keeping method signatures expressed in TinyEngine DTO/entity types only. Remove controller-specific response wrappers where service consumers should receive DTOs or domain objects.

- [x] **Step 4: 迁移服务实现**

Move service implementations into `service.impl`, keep Spring `@Service` and `org.springframework.transaction.annotation.Transactional` where needed, inject mapper interfaces and SPI interfaces, and remove Controller/OpenAPI/Servlet/Filter/profile/startup dependencies.

- [x] **Step 5: 增加 Hook 与上下文扩展点**

Implement `TinyEngineSaveHook` and `NoopTinyEngineSaveHook` for save-after extension behavior. Service implementations should depend on the interface so future Boot starter or business modules can override by Bean.

- [x] **Step 6: 运行测试与编译**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineServiceContractTest test
./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -DskipTests compile
```

Expected: PASS.

- [x] **Step 7: Commit Task 6**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core
git commit -m "feat: migrate tiny engine services"
```

### Task 7: 建立动态 SQL 标识符校验器

**Goal:** 先建立动态模型的集中式 SQL 安全边界，阻断标识符拼接注入。

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/dynamic/sql/TinyEngineSqlIdentifierValidator.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/dynamic/sql/TinyEngineSortDirection.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/dynamic/sql/TinyEngineSqlIdentifierException.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/test/java/com/xiongx9527/fw/plus/tinyengine/dynamic/TinyEngineSqlIdentifierValidatorTest.java`

**Interfaces:**
- Produces: `TinyEngineSqlIdentifierValidator#validateIdentifier(String, String)`, `#validateTableName(String)`, `#validateFieldName(String)`, `#validateOrderBy(String)`, `#validateSortDirection(String)`, `#validateFieldList(Collection<String>, String)`
- Consumes: Task 2 exception style

**Dependencies:** Task 2.

**Validation:**
- `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineSqlIdentifierValidatorTest test`

- [x] **Step 1: 写危险输入失败测试**

Create tests rejecting semicolon, SQL comment, whitespace injection, quoted identifiers, function calls, expressions, parentheses, keyword concatenation, multi-statement fragments, invalid sort direction, invalid DDL column name, invalid batch field, and invalid condition key.

- [x] **Step 2: 写合法输入失败测试**

In the same test class, add legal cases for `app`, `page_template`, `tenant_user_01`, controlled multi-part names when explicitly allowed, and sort directions `ASC` / `DESC` in mixed case.

- [x] **Step 3: 运行失败测试**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineSqlIdentifierValidatorTest test
```

Expected: FAIL because validator does not exist.

- [x] **Step 4: 实现校验器**

Implement the default regex `^[A-Za-z_][A-Za-z0-9_]*$`, normalize sort direction to uppercase, reject blank/null identifiers, and provide collection validation for field lists. Do not allow user-provided aggregate expressions such as `COUNT(*) as count`.

- [x] **Step 5: 运行测试**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineSqlIdentifierValidatorTest test
```

Expected: PASS.

- [x] **Step 6: Commit Task 7**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core
git commit -m "feat: add tiny engine sql identifier guard"
```

### Task 8: 迁移动态模型 DAO、服务与 SQL Provider

**Goal:** 在 Task 7 安全边界之上迁移动态模型查询、写入、结构读取和表生命周期能力。

**Files:**
- Create: files under `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/dynamic/model/`
- Create: files under `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/dynamic/mapper/`
- Create: files under `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/dynamic/service/`
- Create: files under `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/dynamic/service/impl/`
- Create: files under `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/dynamic/sql/`
- Test: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/test/java/com/xiongx9527/fw/plus/tinyengine/dynamic/TinyEngineDynamicSqlProviderTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/test/java/com/xiongx9527/fw/plus/tinyengine/dynamic/TinyEngineDynamicServiceContractTest.java`

**Interfaces:**
- Produces: dynamic CRUD service, dynamic table lifecycle service, dynamic SQL provider, dynamic DAO/mapper contracts
- Consumes: Task 7 `TinyEngineSqlIdentifierValidator`, Task 5 mapper infrastructure, Task 6 services where model metadata is required

**Dependencies:** Task 5, Task 6, Task 7.

**Validation:**
- `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineDynamicSqlProviderTest,TinyEngineDynamicServiceContractTest test`

- [x] **Step 1: 写动态 SQL 安全失败测试**

Create `TinyEngineDynamicSqlProviderTest` asserting query, insert, update, delete, create table, alter table, drop table, initialize table, describe structure, and batch insert paths call the validator before SQL string assembly.

- [x] **Step 2: 写动态服务契约失败测试**

Create `TinyEngineDynamicServiceContractTest` covering legal dynamic pagination query, legal insert/update/delete requests, table structure read request, and table lifecycle request using safe identifiers and bound values.

- [x] **Step 3: 运行失败测试**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineDynamicSqlProviderTest,TinyEngineDynamicServiceContractTest test
```

Expected: FAIL because dynamic model implementation does not exist.

- [x] **Step 4: 迁移动态模型 DTO 与 DAO**

Move dynamic model request/response DTOs, DAO contracts, and metadata types into `dynamic.model` and `dynamic.mapper`. Keep all user supplied table/field/order values as data until they reach the validator.

- [x] **Step 5: 迁移并改造 SQL Provider**

Move SQL provider code into `dynamic.sql`. Every SQL path must validate identifiers through `TinyEngineSqlIdentifierValidator`; every user value must remain a MyBatis parameter binding such as `#{}` rather than direct string concatenation.

- [x] **Step 6: 迁移动态服务**

Move dynamic CRUD and table lifecycle services into `dynamic.service` and `dynamic.service.impl`. Services should compose model metadata, validator, and mapper/provider contracts without depending on Controller or starter code.

- [x] **Step 7: 运行测试**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineDynamicSqlProviderTest,TinyEngineDynamicServiceContractTest test
```

Expected: PASS.

- [x] **Step 8: Commit Task 8**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core
git commit -m "feat: migrate tiny engine dynamic model core"
```

### Task 9: 范围排除、聚合验证与实现说明

**Goal:** 确认 TinyEngine core 只包含本 change 范围内的底层库能力，并记录后续 starter 接入边界。

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/README.md`
- Test: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/test/java/com/xiongx9527/fw/plus/tinyengine/support/TinyEngineScopeBoundaryTest.java`
- Modify: only if needed for documentation links, `docs/`

**Interfaces:**
- Produces: module README documenting artifact, package root, mapper XML path, SQL manifest path, SPI extension points, and excluded scopes
- Consumes: all previous tasks

**Dependencies:** Task 1 through Task 8.

**Validation:**
- `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core test`
- `cd fw-plus-core && ./mvnw -f fw-plus-project/pom.xml -pl fw-plus-tiny-engine -am -DskipTests install`
- `cd fw-plus-core && ./mvnw -f fw-plus-project/pom.xml -DskipTests validate`

- [x] **Step 1: 写范围排除测试**

Create `TinyEngineScopeBoundaryTest` scanning compiled classes/resources and source paths to assert the module does not contain `Controller`, `OpenAPI`, `Swagger`, `ServletFilter`, `SpringBootApplication`, `Docker`, `LangChain4j`, `VectorStorage`, `RAG`, `jakarta.`, or `com.tinyengine.it`.

- [x] **Step 2: 运行范围测试**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=TinyEngineScopeBoundaryTest test
```

Expected: PASS after previous tasks are complete; if it fails, remove the out-of-scope dependency or class from core.

- [x] **Step 3: 编写 README**

Document `fw-plus-tiny-engine-core` artifact coordinates, package root `com.xiongx9527.fw.plus.tinyengine`, mapper XML path `classpath*:mapper/tinyengine/**/*.xml`, SQL resource root `META-INF/tiny-engine/sql/`, `manifest.properties` ordering contract, `TinyEngineUserContext`, `TinyEngineSaveHook`, and dynamic SQL validation requirements.

- [x] **Step 4: 运行新增模块测试**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core test
```

Expected: PASS.

- [x] **Step 5: 运行 TinyEngine 聚合安装验证**

Run:

```bash
cd fw-plus-core
./mvnw -f fw-plus-project/pom.xml -pl fw-plus-tiny-engine -am -DskipTests install
```

Expected: PASS.

- [x] **Step 6: 运行 core 聚合 validate**

Run:

```bash
cd fw-plus-core
./mvnw -f fw-plus-project/pom.xml -DskipTests validate
```

Expected: PASS. If full validation is blocked by an unrelated existing module or external dependency, record the failing module, command, and first actionable error in the implementation handoff.

- [x] **Step 7: Commit Task 9**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-tiny-engine
git commit -m "docs: document tiny engine core boundaries"
```

---

## Self-Review Checklist

- [x] Spec coverage: Tasks 1-9 cover module/BOM, common types, utilities, model/DTO, mapper/XML/SQL resources, services, dynamic SQL guard, dynamic model migration, and final scope verification.
- [x] Scope check: No task implements Controller, starter, auto-configuration, AI/RAG, Docker, profile config, or Servlet filter.
- [x] Java compatibility: Every implementation task explicitly requires Java 8 and Spring Boot 2.7.18 compatibility.
- [x] SQL safety: Task 7 establishes `TinyEngineSqlIdentifierValidator` before Task 8 migrates dynamic SQL providers.
- [x] Resource contract: Task 5 fixes mapper XML path and SQL manifest path for future Boot starter consumption.
- [x] Execution boundary: Do not mark OpenSpec `tasks.md` checkboxes until implementation and verification are actually complete.
