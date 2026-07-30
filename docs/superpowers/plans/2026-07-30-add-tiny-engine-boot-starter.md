---
change: add-tiny-engine-boot-starter
design-doc: docs/superpowers/specs/2026-07-30-add-tiny-engine-boot-starter-design.md
base-ref: 0212ccdebe60b458811d8ed0e3d20c47310a54d7
archived-with: 2026-07-30-add-tiny-engine-boot-starter
---

# TinyEngine Boot Starter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 在 `fw-plus-boot` 中新增 TinyEngine Spring Boot starter，让 Boot 2.7.18 宿主通过 `fw.plus.tiny-engine` 属性启用 TinyEngine Controller、Service 装配、Mapper/XML 与 SQL 资源发现。

**Architecture:** Boot 层新增一个受条件保护的 `TinyEngineAutoConfiguration`，只装配 core TinyEngine 能力和 Web 薄适配，不复制领域逻辑。starter 模块只聚合 `fw-plus-boot-starter`、`fw-plus-boot-autoconfigure`、MyBatis-Plus/Web 依赖与 `fw-plus-tiny-engine-core`；样例和测试验证启用、禁用、资源发现、classpath 缺失、宿主覆盖和 AI 依赖边界。

**Tech Stack:** Java 8、Spring Boot 2.7.18、Spring MVC、MyBatis-Plus、Maven Wrapper、OpenSpec/Comet、ApplicationContextRunner。

## Global Constraints

- 所有源码保持 Java 8，不使用 Java 9+ API。
- 不升级 Spring Boot 2.7.18，不引入 Spring Boot 3 或 JDK 17 专属依赖。
- 不修改 Magic-API 2.2.2 版本或 Magic-API 插件行为。
- TinyEngine 领域模型、Service、Mapper、动态 SQL、Mapper XML 和 SQL 脚本以 `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core` 为事实源。
- Boot starter 不新增旧 LangChain4j 专用 model/store artifact、旧 starter 坐标或直接 AI provider SDK。
- 后续 TinyEngine AI/RAG 只能通过 `fw-plus-ai-model` 与 `fw-plus-ai-store-embedding` 中立抽象衔接。
- 修改前保留当前工作树中已有未提交改动，不回滚 `.superpowers/sdd/*`、`add-tiny-engine-ai-starter` 或其他无关文件。
- 新增 POM 继续使用 `${revision}`，缩进风格跟随所在 POM。

---

## File Structure

- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/TinyEngineProperties.java`
  - 负责 `fw.plus.tiny-engine` 属性绑定和默认值。
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/TinyEngineAutoConfiguration.java`
  - 顶层自动配置，启用属性类，导入 Web、Mapper、Resource 子配置。
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/TinyEngineCoreConfiguration.java`
  - 受控导入 core Service/support Bean 或提供默认扩展 Bean。
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/TinyEngineMapperConfiguration.java`
  - 注册 Mapper 扫描和 mapper XML 追加能力。
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/TinyEngineResourceConfiguration.java`
  - 暴露 SQL manifest/resource 发现 Bean，默认不执行 SQL。
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/web/TinyEngineResourceController.java`
  - 首批 REST 薄适配 Controller，委托 core `ResourceService`。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/resources/META-INF/spring.factories`
  - 追加 TinyEngine 自动配置类。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
  - 追加 TinyEngine 自动配置类。
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/TinyEngineAutoConfigurationTest.java`
  - 覆盖启用/禁用、缺 classpath、宿主覆盖、SQL 默认不执行。
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/TinyEngineResourceDiscoveryTest.java`
  - 覆盖 mapper XML 与 SQL manifest/resource 可发现。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/pom.xml`
  - 增加 `fw-plus-tiny-engine-spring-boot-starter` module。
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/pom.xml`
  - 新 starter POM。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-dependencies/pom.xml`
  - 如现有 BOM 需要，托管 tiny-engine starter 与 core 依赖。
- Modify: `fw-plus-sample/pom.xml` and `fw-plus-sample/fw-plus-boot-samples/pom.xml`
  - 接入 TinyEngine 样例模块。
- Create: `fw-plus-sample/fw-plus-boot-samples/fw-plus-tiny-engine-spring-boot-starter-sample/pom.xml`
  - 样例模块 POM。
- Create: `fw-plus-sample/fw-plus-boot-samples/fw-plus-tiny-engine-spring-boot-starter-sample/src/test/java/com/xiongx9527/fw/plus/sample/tinyengine/TinyEngineStarterSampleTest.java`
  - 样例级启用/禁用验证。
- Create: `fw-plus-sample/fw-plus-boot-samples/fw-plus-tiny-engine-spring-boot-starter-sample/src/test/resources/application.yml`
  - 样例基础配置，使用 H2 或上下文级资源发现，不连接真实库。
- Create: `docs/superpowers/reports/2026-07-30-add-tiny-engine-boot-starter-build.md`
  - build 阶段证据草稿，记录命令和边界扫描结果。

---

### Task 1: Core 依赖与边界确认

**Files:**
- Modify: `openspec/changes/add-tiny-engine-boot-starter/tasks.md`
- Create: `docs/superpowers/reports/2026-07-30-add-tiny-engine-boot-starter-build.md`

**Interfaces:**
- Consumes: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/pom.xml`、core package/resource layout。
- Produces: 已确认的 artifact、包名、Mapper XML、SQL manifest 和 AI 边界证据，供后续任务使用。

- [x] **Step 1: 记录当前 core artifact 与资源路径**

Run:

```bash
sed -n '1,180p' fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/pom.xml
find fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/resources -maxdepth 4 -type f | sort
```

Expected: POM artifactId 为 `fw-plus-tiny-engine-core`；存在 `mapper/tinyengine/*.xml` 和 `META-INF/tiny-engine/sql/manifest.properties`。

- [x] **Step 2: 扫描 core Java 8 与 AI 边界**

Run:

```bash
rg -n "List\.of\(|Map\.of\(|Optional\.orElseThrow\(|var |record |sealed " fw-plus-core/fw-plus-project/fw-plus-tiny-engine || true
rg -n "fw-plus-ai-langchain4j-model|fw-plus-ai-langchain4j-store-embeding|langchain4j-.*starter|openai|dashscope|qianfan|milvus|qdrant|weaviate" fw-plus-boot fw-plus-sample || true
```

Expected: 第一条无 Java 9+ API 命中；第二条在 TinyEngine Boot starter 新增范围内无旧 LangChain4j 专用坐标或直接 provider SDK。

- [x] **Step 3: 写 build 证据草稿**

Create `docs/superpowers/reports/2026-07-30-add-tiny-engine-boot-starter-build.md`:

```markdown
# add-tiny-engine-boot-starter Build Evidence

## Core Boundary

- Core artifact: `com.xiongx9527.fw.plus:fw-plus-tiny-engine-core:${revision}`
- Core package: `com.xiongx9527.fw.plus.tinyengine`
- Mapper package: `com.xiongx9527.fw.plus.tinyengine.mapper`
- Mapper XML: `classpath*:mapper/tinyengine/**/*.xml`
- SQL manifest: `META-INF/tiny-engine/sql/manifest.properties`
- SQL resources: `META-INF/tiny-engine/sql/h2/`, `META-INF/tiny-engine/sql/mysql/`, `META-INF/tiny-engine/sql/postgresql/`

## AI Boundary

TinyEngine Boot starter does not add legacy LangChain4j model/store artifacts, old starter coordinates, or direct AI provider SDKs. Future AI/RAG integration must use `fw-plus-ai-model` and `fw-plus-ai-store-embedding`.

## Commands

Commands will be appended as implementation progresses.
```

- [x] **Step 4: 勾选 OpenSpec task 1.1-1.4**

Modify `openspec/changes/add-tiny-engine-boot-starter/tasks.md` so tasks 1.1 through 1.4 are checked only after Step 1-3 evidence is present.

- [x] **Step 5: 提交任务 1**

Run:

```bash
git add openspec/changes/add-tiny-engine-boot-starter/tasks.md docs/superpowers/reports/2026-07-30-add-tiny-engine-boot-starter-build.md
git commit -m "docs: record tiny engine boot starter boundaries"
```

Expected: commit succeeds without staging unrelated `.superpowers/sdd/*` or `add-tiny-engine-ai-starter` changes.

---

### Task 2: 自动配置、属性与 Web 薄适配

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/TinyEngineProperties.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/TinyEngineAutoConfiguration.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/TinyEngineCoreConfiguration.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/web/TinyEngineResourceController.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/TinyEngineAutoConfigurationTest.java`
- Modify: `openspec/changes/add-tiny-engine-boot-starter/tasks.md`

**Interfaces:**
- Consumes: core `ResourceService`, `TinyEngineUserContext`, `SystemTinyEngineUserContext`, `TinyEngineSaveHook`, `NoopTinyEngineSaveHook`.
- Produces: `TinyEngineProperties`, `TinyEngineAutoConfiguration`, default user context/hook beans, and `TinyEngineResourceController`.

- [x] **Step 1: 写失败测试覆盖禁用、启用和覆盖**

Create `TinyEngineAutoConfigurationTest.java` with tests equivalent to:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineResourceController;
import com.xiongx9527.fw.plus.tinyengine.service.ResourceService;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class TinyEngineAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TinyEngineAutoConfiguration.class));

    @Test
    void disabledByDefaultDoesNotRegisterTinyEngineWebBeans() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(TinyEngineResourceController.class);
            assertThat(context).doesNotHaveBean(TinyEngineUserContext.class);
        });
    }

    @Test
    void enabledRegistersDefaultSupportBeans() {
        contextRunner.withPropertyValues("fw.plus.tiny-engine.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(TinyEngineUserContext.class));
    }

    @Test
    void webDisabledDoesNotRegisterController() {
        contextRunner.withPropertyValues(
                "fw.plus.tiny-engine.enabled=true",
                "fw.plus.tiny-engine.web.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(TinyEngineResourceController.class));
    }

    @Test
    void missingCoreClassDoesNotRegisterTinyEngineBeans() {
        contextRunner.withClassLoader(new FilteredClassLoader(ResourceService.class))
                .withPropertyValues("fw.plus.tiny-engine.enabled=true")
                .run(context -> assertThat(context).doesNotHaveBean(TinyEngineUserContext.class));
    }

    @Test
    void hostUserContextWins() {
        contextRunner.withUserConfiguration(HostTinyEngineConfiguration.class)
                .withPropertyValues("fw.plus.tiny-engine.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(TinyEngineUserContext.class));
    }

    @Configuration
    static class HostTinyEngineConfiguration {
        @Bean
        TinyEngineUserContext tinyEngineUserContext() {
            return new TinyEngineUserContext() {
                @Override
                public com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser getCurrentUser() {
                    return null;
                }
            };
        }
    }
}
```

- [x] **Step 2: 运行失败测试**

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=TinyEngineAutoConfigurationTest test
```

Expected: FAIL because `TinyEngineAutoConfiguration` and related classes do not exist.

- [x] **Step 3: 实现 `TinyEngineProperties`**

Create `TinyEngineProperties.java` with Java 8-compatible nested classes:

```java
@ConfigurationProperties(prefix = "fw.plus.tiny-engine")
public class TinyEngineProperties {
    private boolean enabled = false;
    private Web web = new Web();
    private Mapper mapper = new Mapper();
    private Resources resources = new Resources();
    private Sql sql = new Sql();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Web getWeb() { return web; }
    public void setWeb(Web web) { this.web = web; }
    public Mapper getMapper() { return mapper; }
    public void setMapper(Mapper mapper) { this.mapper = mapper; }
    public Resources getResources() { return resources; }
    public void setResources(Resources resources) { this.resources = resources; }
    public Sql getSql() { return sql; }
    public void setSql(Sql sql) { this.sql = sql; }

    public static class Web {
        private boolean enabled = true;
        private String basePath = "";
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getBasePath() { return basePath; }
        public void setBasePath(String basePath) { this.basePath = basePath; }
    }

    public static class Mapper {
        private boolean enabled = true;
        private String basePackage = "com.xiongx9527.fw.plus.tinyengine.mapper";
        private String[] mapperLocations = new String[] { "classpath*:mapper/tinyengine/**/*.xml" };
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getBasePackage() { return basePackage; }
        public void setBasePackage(String basePackage) { this.basePackage = basePackage; }
        public String[] getMapperLocations() { return mapperLocations; }
        public void setMapperLocations(String[] mapperLocations) { this.mapperLocations = mapperLocations; }
    }

    public static class Resources {
        private boolean enabled = true;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class Sql {
        private boolean enabled = true;
        private boolean autoInitialize = false;
        private String database;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isAutoInitialize() { return autoInitialize; }
        public void setAutoInitialize(boolean autoInitialize) { this.autoInitialize = autoInitialize; }
        public String getDatabase() { return database; }
        public void setDatabase(String database) { this.database = database; }
    }
}
```

- [x] **Step 4: 实现自动配置与默认 support Bean**

Create `TinyEngineAutoConfiguration.java` and `TinyEngineCoreConfiguration.java` with:

```java
@Configuration
@ConditionalOnClass(name = "com.xiongx9527.fw.plus.tinyengine.service.ResourceService")
@ConditionalOnProperty(prefix = "fw.plus.tiny-engine", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(TinyEngineProperties.class)
@Import({TinyEngineCoreConfiguration.class, TinyEngineMapperConfiguration.class, TinyEngineResourceConfiguration.class})
public class TinyEngineAutoConfiguration {
}
```

```java
@Configuration
@ConditionalOnClass(name = "com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext")
public class TinyEngineCoreConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TinyEngineUserContext tinyEngineUserContext() {
        return new SystemTinyEngineUserContext();
    }

    @Bean
    @ConditionalOnMissingBean
    public TinyEngineSaveHook tinyEngineSaveHook() {
        return new NoopTinyEngineSaveHook();
    }
}
```

If core Service implementations are not otherwise scanned by the host, add a narrowly scoped `@ComponentScan(basePackages = "com.xiongx9527.fw.plus.tinyengine.service.impl")` to this configuration and cover it in tests.

- [x] **Step 5: 实现首批 Resource Controller 薄适配**

Create `TinyEngineResourceController.java` with route methods that delegate only to `ResourceService`:

```java
@RestController
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.web", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(ResourceService.class)
@ConditionalOnMissingBean(name = "tinyEngineResourceController")
public class TinyEngineResourceController {
    private final ResourceService resourceService;

    public TinyEngineResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping("${fw.plus.tiny-engine.web.base-path:}/tiny-engine/resources")
    public Result<List<Resource>> listResources() {
        return Result.ok(resourceService.queryAllResource());
    }
}
```

Adjust only to match the actual `Result` factory names in core. Do not add new business logic in the controller.

- [x] **Step 6: 运行测试并修复编译差异**

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=TinyEngineAutoConfigurationTest test
```

Expected: PASS.

- [x] **Step 7: 勾选 OpenSpec task 2.1-2.4**

Update `openspec/changes/add-tiny-engine-boot-starter/tasks.md` tasks 2.1 through 2.4 after the tests pass.

- [x] **Step 8: 提交任务 2**

Run:

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine openspec/changes/add-tiny-engine-boot-starter/tasks.md
git commit -m "feat: add tiny engine boot autoconfiguration"
```

Expected: commit succeeds and contains only TinyEngine Boot starter change files.

---

### Task 3: Mapper/XML、SQL 资源发现与自动配置注册

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/TinyEngineMapperConfiguration.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/TinyEngineResourceConfiguration.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/TinyEngineResourceDiscoveryTest.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/resources/META-INF/spring.factories`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Modify: `openspec/changes/add-tiny-engine-boot-starter/tasks.md`

**Interfaces:**
- Consumes: `TinyEngineProperties.Mapper`, `TinyEngineProperties.Sql`, `TinyEngineSqlScriptManifest`.
- Produces: Mapper scan registration, mapper XML pattern exposure, SQL manifest/resource discovery Bean, auto-configuration registration metadata.

- [x] **Step 1: 写失败测试覆盖 XML 与 SQL 发现**

Create `TinyEngineResourceDiscoveryTest.java` with tests equivalent to:

```java
class TinyEngineResourceDiscoveryTest {
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TinyEngineAutoConfiguration.class))
            .withPropertyValues("fw.plus.tiny-engine.enabled=true");

    @Test
    void exposesMapperLocations() {
        contextRunner.run(context -> {
            TinyEngineProperties properties = context.getBean(TinyEngineProperties.class);
            assertThat(properties.getMapper().getMapperLocations())
                    .contains("classpath*:mapper/tinyengine/**/*.xml");
        });
    }

    @Test
    void exposesSqlManifestWithoutAutoInitializingDatabase() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(TinyEngineSqlScriptManifest.class);
            TinyEngineProperties properties = context.getBean(TinyEngineProperties.class);
            assertThat(properties.getSql().isAutoInitialize()).isFalse();
        });
    }
}
```

- [x] **Step 2: 运行失败测试**

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=TinyEngineResourceDiscoveryTest test
```

Expected: FAIL because mapper/resource configuration classes are absent.

- [x] **Step 3: 实现 Mapper 扫描配置**

Create `TinyEngineMapperConfiguration.java` using `MapperScannerConfigurer` or the repository's existing MyBatis extension point:

```java
@Configuration
@ConditionalOnClass(name = "org.mybatis.spring.mapper.MapperScannerConfigurer")
@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.mapper", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TinyEngineMapperConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "tinyEngineMapperScannerConfigurer")
    public MapperScannerConfigurer tinyEngineMapperScannerConfigurer(TinyEngineProperties properties) {
        MapperScannerConfigurer configurer = new MapperScannerConfigurer();
        configurer.setBasePackage(properties.getMapper().getBasePackage());
        return configurer;
    }
}
```

If the project already has a safer mapper location customizer, use that pattern and add an assertion that existing mapper locations are preserved.

- [x] **Step 4: 实现 SQL 资源发现配置**

Create `TinyEngineResourceConfiguration.java`:

```java
@Configuration
@ConditionalOnClass(name = "com.xiongx9527.fw.plus.tinyengine.support.sql.TinyEngineSqlScriptManifest")
@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.resources", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TinyEngineResourceConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TinyEngineSqlScriptManifest tinyEngineSqlScriptManifest() {
        return new TinyEngineSqlScriptManifest();
    }
}
```

Do not add automatic SQL execution unless `sql.auto-initialize=true` is explicitly supported and tested with H2.

- [x] **Step 5: 注册 AutoConfiguration 元数据**

Append `com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.TinyEngineAutoConfiguration` to both:

```text
fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/resources/META-INF/spring.factories
fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

Keep the continuation comma style in `spring.factories`.

- [x] **Step 6: 运行 autoconfigure 测试**

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=TinyEngineAutoConfigurationTest,TinyEngineResourceDiscoveryTest test
```

Expected: PASS.

- [x] **Step 7: 勾选 OpenSpec task 2.5-2.7**

Update `openspec/changes/add-tiny-engine-boot-starter/tasks.md` tasks 2.5 through 2.7 after tests pass.

- [x] **Step 8: 提交任务 3**

Run:

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/resources/META-INF/spring.factories fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports openspec/changes/add-tiny-engine-boot-starter/tasks.md
git commit -m "feat: register tiny engine mapper and resources"
```

Expected: commit succeeds.

---

### Task 4: Starter、BOM 与样例模块

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/pom.xml`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/pom.xml`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-dependencies/pom.xml`
- Modify: `fw-plus-sample/pom.xml`
- Modify: `fw-plus-sample/fw-plus-boot-samples/pom.xml`
- Create: `fw-plus-sample/fw-plus-boot-samples/fw-plus-tiny-engine-spring-boot-starter-sample/pom.xml`
- Create: `fw-plus-sample/fw-plus-boot-samples/fw-plus-tiny-engine-spring-boot-starter-sample/src/test/java/com/xiongx9527/fw/plus/sample/tinyengine/TinyEngineStarterSampleTest.java`
- Create: `fw-plus-sample/fw-plus-boot-samples/fw-plus-tiny-engine-spring-boot-starter-sample/src/test/resources/application.yml`
- Modify: `openspec/changes/add-tiny-engine-boot-starter/tasks.md`

**Interfaces:**
- Consumes: autoconfigure classes from Tasks 2-3.
- Produces: Maven starter module, BOM-managed dependency path, sample module and sample tests.

- [x] **Step 1: 写 starter POM**

Create `fw-plus-tiny-engine-spring-boot-starter/pom.xml` following adjacent starter style:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.xiongx9527.fw.plus.boot</groupId>
        <artifactId>fw-plus-boot-starters</artifactId>
        <version>${revision}</version>
    </parent>
    <artifactId>fw-plus-tiny-engine-spring-boot-starter</artifactId>
    <properties>
        <main.basedir>${basedir}/../../..</main.basedir>
    </properties>
    <dependencies>
        <dependency>
            <groupId>com.xiongx9527.fw.plus.boot</groupId>
            <artifactId>fw-plus-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.xiongx9527.fw.plus.boot</groupId>
            <artifactId>fw-plus-boot-autoconfigure</artifactId>
        </dependency>
        <dependency>
            <groupId>com.xiongx9527.fw.plus</groupId>
            <artifactId>fw-plus-tiny-engine-core</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>
</project>
```

Do not add any `fw-plus-ai-langchain4j-*` dependency.

- [x] **Step 2: 接入 starter 聚合与 BOM**

Add module to `fw-plus-boot-starters/pom.xml`:

```xml
<module>fw-plus-tiny-engine-spring-boot-starter</module>
```

If `fw-plus-boot-dependencies/pom.xml` has dependency management entries for starters, add:

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.boot</groupId>
    <artifactId>fw-plus-tiny-engine-spring-boot-starter</artifactId>
    <version>${revision}</version>
</dependency>
<dependency>
    <groupId>com.xiongx9527.fw.plus</groupId>
    <artifactId>fw-plus-tiny-engine-core</artifactId>
    <version>${revision}</version>
</dependency>
```

- [x] **Step 3: 创建样例 POM 与配置**

Create sample POM that depends on `fw-plus-tiny-engine-spring-boot-starter`, H2 test dependency, and Spring Boot test. Add sample module to both sample aggregators if present. Create `application.yml`:

```yaml
fw:
  plus:
    tiny-engine:
      enabled: true
      sql:
        auto-initialize: false
spring:
  datasource:
    url: jdbc:h2:mem:tiny_engine_sample;MODE=MySQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
```

- [x] **Step 4: 写样例测试**

Create `TinyEngineStarterSampleTest.java` with two context runners or Spring tests:

```java
class TinyEngineStarterSampleTest {

    @Test
    void enabledSampleFindsTinyEngineResources() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        assertThat(resolver.getResources("classpath*:mapper/tinyengine/**/*.xml")).isNotEmpty();
        assertThat(resolver.getResource("classpath:META-INF/tiny-engine/sql/manifest.properties").exists()).isTrue();
    }

    @Test
    void disabledSampleCanStartWithoutTinyEngineWebBeans() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(TinyEngineAutoConfiguration.class))
                .withPropertyValues("fw.plus.tiny-engine.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean("tinyEngineResourceController"));
    }
}
```

- [x] **Step 5: 运行 Maven 解析和样例测试**

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter -am validate
./fw-plus-core/mvnw -f fw-plus-sample/pom.xml -pl fw-plus-boot-samples/fw-plus-tiny-engine-spring-boot-starter-sample test
```

Expected: both commands PASS.

- [x] **Step 6: 勾选 OpenSpec task 3.1-3.4 与 4.1-4.4**

Update `openspec/changes/add-tiny-engine-boot-starter/tasks.md` after starter and sample verification pass.

- [x] **Step 7: 提交任务 4**

Run:

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters fw-plus-boot/fw-plus-boot-project/fw-plus-boot-dependencies/pom.xml fw-plus-sample openspec/changes/add-tiny-engine-boot-starter/tasks.md
git commit -m "feat: add tiny engine boot starter sample"
```

Expected: commit succeeds; inspect staged diff before committing to avoid unrelated files.

---

### Task 5: 全量验证、边界扫描与 build 收口

**Files:**
- Modify: `docs/superpowers/reports/2026-07-30-add-tiny-engine-boot-starter-build.md`
- Modify: `openspec/changes/add-tiny-engine-boot-starter/tasks.md`

**Interfaces:**
- Consumes: all implementation from Tasks 1-4.
- Produces: completed task checklist and build evidence ready for Comet build guard.

- [x] **Step 1: 运行 Boot 聚合校验**

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-boot/pom.xml validate
```

Expected: BUILD SUCCESS.

- [x] **Step 2: 运行 TinyEngine 相关测试**

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure,fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter -am test
```

Expected: BUILD SUCCESS with TinyEngine autoconfigure tests passing.

- [x] **Step 3: 运行样例聚合校验**

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-sample/pom.xml validate
```

Expected: BUILD SUCCESS.

- [x] **Step 4: 运行版本与 AI 边界扫描**

Run:

```bash
rg -n "<spring-boot.version>|3\.1\.9|17</java.version>|<jdk.version>17|Magic-API 2\.2\.2" fw-plus-boot fw-plus-sample openspec/changes/add-tiny-engine-boot-starter docs/superpowers/specs/2026-07-30-add-tiny-engine-boot-starter-design.md || true
rg -n "fw-plus-ai-langchain4j-model|fw-plus-ai-langchain4j-store-embeding|fw-plus-ai-langchain4j-.*spring-boot-starter|dev.langchain4j|spring-ai|agentscope|dashscope|openai|qdrant|milvus|weaviate" fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter fw-plus-sample/fw-plus-boot-samples/fw-plus-tiny-engine-spring-boot-starter-sample || true
```

Expected: no TinyEngine Boot starter or sample dependency on old LangChain4j model/store artifacts, old starter coordinates, direct AI provider SDKs, Spring Boot 3, or JDK 17-only configuration.

- [x] **Step 5: 更新 build evidence**

Append command results to `docs/superpowers/reports/2026-07-30-add-tiny-engine-boot-starter-build.md`:

```markdown
## Final Build Checks

| Check | Result |
| --- | --- |
| `./fw-plus-core/mvnw -f fw-plus-boot/pom.xml validate` | PASS |
| TinyEngine autoconfigure/starter tests | PASS |
| `./fw-plus-core/mvnw -f fw-plus-sample/pom.xml validate` | PASS |
| Magic-API/Spring Boot/JDK boundary scan | PASS |
| Neutral AI boundary scan | PASS |
```

- [x] **Step 6: 勾选 OpenSpec task 5.1-5.5**

Update `openspec/changes/add-tiny-engine-boot-starter/tasks.md` so all task checkboxes are complete only after the commands pass.

- [x] **Step 7: 提交最终 build 证据**

Run:

```bash
git add openspec/changes/add-tiny-engine-boot-starter/tasks.md docs/superpowers/reports/2026-07-30-add-tiny-engine-boot-starter-build.md
git commit -m "test: verify tiny engine boot starter"
```

Expected: commit succeeds.

- [x] **Step 8: 运行 Comet build guard**

Run:

```bash
comet guard add-tiny-engine-boot-starter build --apply
```

Expected: all build guard checks PASS and `.comet.yaml` phase becomes `verify`.

---

## Self-Review

- Spec coverage: Task 1 covers core dependency confirmation and neutral AI boundary. Tasks 2-3 cover autoconfiguration, properties, Controller, Mapper/XML, SQL resources and registration. Task 4 covers starter, BOM and sample. Task 5 covers build checks, Magic-API/JDK/Spring Boot boundaries and final task completion.
- Placeholder scan: no `TBD`, `TODO`, `implement later`, or unspecified file paths remain.
- Type consistency: property prefix is consistently `fw.plus.tiny-engine`; default mapper package is `com.xiongx9527.fw.plus.tinyengine.mapper`; default XML pattern is `classpath*:mapper/tinyengine/**/*.xml`; SQL manifest is `META-INF/tiny-engine/sql/manifest.properties`.
