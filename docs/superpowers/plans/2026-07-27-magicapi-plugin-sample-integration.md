---
change: fix-magicapi-plugin-sample-integration
design-doc: docs/superpowers/specs/2026-07-27-magicapi-plugin-sample-integration-design.md
base-ref: 0cf101d618562f74db41dc5b7071e6714e1fd4fa
---

# Magic-API 全插件 Sample 整体集成 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 Boot 25 个与 AI 2 个 Magic-API 插件在真实 Boot sample 中完成离线联合装配、分层构建、实际进程启动和 HTTP 入口验证。

**Architecture:** `fw-plus-magicapi-spring-boot-starter-sample` 是唯一整体宿主。先修复 AI 元数据与 MQTT 创建环，再用 `all-plugins` profile 隔离 Apollo 和外部资源；测试从聚合 POM 清单、运行时 classpath、完整 Spring 上下文逐层收敛，最终启动独立 sample 进程验证健康和 Magic-API Web。

**Tech Stack:** JDK 8、Maven 3.6.3 / 项目 Maven Wrapper、Spring Boot 2.7.18、JUnit 5、AssertJ、Spring Boot `ApplicationContextRunner` / `@SpringBootTest`、Magic-API 2.2.2。

## Global Constraints

- Core、Boot、Cloud、AI 版本保持 `0.1.1-SNAPSHOT`；sample 固定使用 `0.1.1-SNAPSHOT`。
- 不修改 Magic-API 2.2.2、内部 Nexus 坐标或插件公开 API。
- 所有 Java 代码兼容 JDK 8，不使用 Java 9+ API。
- 不启动或访问 Kafka、Pulsar、RocketMQ、RabbitMQ、Redis、Mongo、Elasticsearch、InfluxDB、MQTT、S3、Debezium、Milvus、Qdrant、Weaviate。
- 不使用 `spring.main.allow-circular-references=true`、`@Lazy` 或新 BeanPostProcessor 绕过 MQTT 循环。
- 每个新源码修复必须有对应 RED 证据；当前已完成的 AI/classpath 修改复用已保存的启动日志和测试记录，不回滚后重复制造失败。
- 当前工作树已包含 AI 元数据/POM、sample POM 和 classpath 测试的未提交实现；执行各任务前先运行 `git diff -- <任务文件>`，在现有内容上增量修改，禁止用计划代码块整文件覆盖或回退这些改动。
- Task 4 首轮执行已创建未提交的 `application-all-plugins.yaml` 与真实 sample 上下文测试，并保存 Apollo RED；新增 AI 前置修复不得修改、回退或暂存这两个 sample 文件。
- 不提交 `node_modules/`、console `dist/`、`.flattened-pom.xml`、启动日志或临时资源目录。

---

### Task 1: 收敛 AI 自动配置元数据与失效依赖

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/resources/META-INF/spring.factories`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/store/embeding/EmbeddingStoreAutoConfigurationMetadataTest.java`

**Interfaces:**
- Consumes: AI reactor 当前实际存在的自动配置类和 provider/store 模块。
- Produces: 两套 Spring Boot 元数据中均可加载的类清单，以及 sample 可解析的 AI `0.1.1-SNAPSHOT` 本地产物。

- [x] **Step 1: 审查并保留当前未提交实现与已有 RED 证据**

先执行：

```bash
git diff -- \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/resources/META-INF/spring.factories \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/store/embeding/EmbeddingStoreAutoConfigurationMetadataTest.java
```

Expected: diff 已包含三个失效可选依赖删除、无效自动配置类删除/替换和双元数据可加载测试；后续步骤只补缺口，不覆盖这些未提交内容。

确认 `fw-plus-sample/fw-plus-boot-samples/fw-plus-magicapi-spring-boot-starter-sample/logDir_IS_UNDEFINED/report-api-all-plugins.log` 中首次启动失败为：

```text
Unable to read meta-data for class
com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.openapi.Langchain4jOpenApiAutoConfiguration
```

同时确认 cassandra、infinispan、neo4j 三个 `com.xiongx9527.fw.plus` artifact 不在 AI/Core reactor，不能通过补虚假版本修复。

- [x] **Step 2: 校核元数据回归测试**

现有测试必须读取两套资源并逐项加载类；若 diff 尚未包含以下完整测试逻辑，再增量补齐相应方法：

```java
@Test
void shouldRegisterLoadableAutoConfigurationsInBothMetadataFiles() throws Exception {
    String factories = read("META-INF/spring.factories");
    String imports = read("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports");
    String root = "com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jAutoConfiguration";
    String embedding = "com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding.EmbeddingStoreAutoConfiguration";

    assertTrue(factories.contains(root));
    assertTrue(factories.contains(embedding));
    assertTrue(imports.contains(root));
    assertTrue(imports.contains(embedding));
    assertSpringFactoriesClassesAreLoadable();
    assertImportCandidatesAreLoadable(imports);
}

private void assertClassNamesAreLoadable(String[] classNames) throws Exception {
    ClassLoader classLoader = getClass().getClassLoader();
    for (String className : classNames) {
        String candidate = className.trim();
        if (!candidate.isEmpty()) {
            Class.forName(candidate, false, classLoader);
        }
    }
}
```

- [x] **Step 3: 保留最小实现修复**

`spring.factories` 删除不存在的 `Langchain4jOpenApiAutoConfiguration`；`AutoConfiguration.imports` 用现存的 `LangChain4jAutoConfiguration` 替换它。POM 删除三个无 reactor、无受管版本的内部可选依赖，不调整其他 LangChain4j 依赖版本。

- [x] **Step 4: 运行 AI 定向测试**

Run:

```bash
./fw-plus-ai/mvnw -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml \
  -Dtest=EmbeddingStoreAutoConfigurationMetadataTest test
```

Expected: `Tests run: 1, Failures: 0, Errors: 0`，两套清单无 `ClassNotFoundException`。

- [x] **Step 5: 提交 AI 修复**

```bash
git add \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/resources/META-INF/spring.factories \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/store/embeding/EmbeddingStoreAutoConfigurationMetadataTest.java
git commit -m "fix(ai): repair auto-configuration metadata"
```

### Task 2: 修复 MQTT Client resource storage 循环依赖

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/starter/MagicAPIMqttClientConfigurationTest.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/starter/MagicAPIMqttClientConfiguration.java`

**Interfaces:**
- Consumes: Magic-API 2.2.2 `DefaultMagicResourceService(Resource, List<MagicResourceStorage<?>>, ApplicationEventPublisher)` 的统一 `setMagicResourceService(this)` 回填行为。
- Produces: 无参 `mqttClientDataSourceInfoMagicResourceStorage()` Bean；完成服务构造后 storage 持有同一个 `MagicResourceService`。

- [x] **Step 1: 编写真实拓扑失败测试**

```java
class MagicAPIMqttClientConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    MagicAPIMqttClientConfiguration.class,
                    ResourceServiceConfiguration.class);

    @Test
    void createsStorageBeforeServiceAndLetsCoreBackfillService() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(MagicResourceService.class);
            assertThat(context).hasSingleBean(MqttClientDataSourceInfoMagicResourceStorage.class);

            MagicResourceService service = context.getBean(MagicResourceService.class);
            MqttClientDataSourceInfoMagicResourceStorage storage =
                    context.getBean(MqttClientDataSourceInfoMagicResourceStorage.class);
            assertThat(ReflectionTestUtils.getField(storage, "magicResourceService"))
                    .isSameAs(service);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class ResourceServiceConfiguration {

        @Bean
        MagicResourceService magicResourceService(
                ObjectProvider<List<MagicResourceStorage<? extends MagicEntity>>> storagesProvider,
                ApplicationEventPublisher publisher) {
            return new MyDefaultMagicResourceService(
                    mock(Resource.class), storagesProvider.getObject(), publisher);
        }
    }
}
```

使用 JUnit 5、AssertJ、`ReflectionTestUtils` 和 Mockito；不要 mock `MagicResourceService`，否则无法复现真实循环。

- [x] **Step 2: 运行测试确认 RED**

Run:

```bash
./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/pom.xml \
  -Dtest=MagicAPIMqttClientConfigurationTest test
```

Expected: FAIL，根因包含 `BeanCurrentlyInCreationException`，创建环为 `magicResourceService -> mqttClientDataSourceInfoMagicResourceStorage -> magicResourceService`。

- [x] **Step 3: 写入最小生产修复**

```java
@Bean
@ConditionalOnMissingBean
public MqttClientDataSourceInfoMagicResourceStorage mqttClientDataSourceInfoMagicResourceStorage() {
    return new MqttClientDataSourceInfoMagicResourceStorage();
}
```

只删除工厂方法的 `MagicResourceService` 参数和手工 setter；不修改 storage 类型、动态注册器或事件时序。

- [x] **Step 4: 运行 MQTT 定向回归**

重复 Step 2 命令。

Expected: `MagicAPIMqttClientConfigurationTest` PASS，反射断言证明统一回填完成。

- [x] **Step 5: 运行相邻插件配置测试**

```bash
./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/pom.xml \
  -Dtest=MagicAPIMqttClientConfigurationTest,MagicAPIRedisConfigurationTest,MagicAPIMcpConfigurationTest test
```

Expected: 所有测试通过，MQTT 修复未改变 Redis/MCP storage 的先创建行为。

- [x] **Step 6: 提交 MQTT 修复**

```bash
git add \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/starter/MagicAPIMqttClientConfiguration.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/starter/MagicAPIMqttClientConfigurationTest.java
git commit -m "fix(magicapi): remove mqtt client resource cycle"
```

### Task 3: 建立 27 插件聚合清单与 sample classpath 契约

**Files:**
- Modify: `fw-plus-sample/fw-plus-boot-samples/fw-plus-magicapi-spring-boot-starter-sample/pom.xml`
- Modify: `fw-plus-sample/fw-plus-boot-samples/fw-plus-magicapi-spring-boot-starter-sample/src/test/java/com/xiongx9527/fw/plus/boot/magicapi/MagicApiAllPluginsClasspathTest.java`
- Read: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/pom.xml`
- Read: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/pom.xml`

**Interfaces:**
- Consumes: Boot 聚合器 25 个 `<module>` 与 AI 聚合器 2 个 `<module>`。
- Produces: sample 运行时 27 个插件 artifact，以及聚合器新增插件时自动失败的同步契约。

- [x] **Step 1: 审查并保留现有 sample POM、classpath 测试与 RED 证据**

```bash
git diff -- \
  fw-plus-sample/fw-plus-boot-samples/fw-plus-magicapi-spring-boot-starter-sample/pom.xml \
  fw-plus-sample/fw-plus-boot-samples/fw-plus-magicapi-spring-boot-starter-sample/src/test/java/com/xiongx9527/fw/plus/boot/magicapi/MagicApiAllPluginsClasspathTest.java
```

Expected: sample POM 已启用测试并补齐 Admin、Druid、DTP、Influx、Mongo、MQTT、MQTT Client、Task 和 AI 两个插件；测试已有 27 个规范化坐标。保留这些未提交内容，只在其上增加聚合器同步断言。

现有 `MagicApiAllPluginsClasspathTest` 在补 POM 前应报告 9 个缺失 artifact；该历史 RED 已记录，不回滚 POM 重跑。确认 sample POM 已补齐 Admin、Druid、DTP、Influx、Mongo、MQTT、MQTT Client、Task 和 AI 两个插件，并启用测试。

- [x] **Step 2: 新增聚合器同步测试**

在现有 27 坐标清单上增加模块集合比较：

```java
@Test
void shouldMatchBootAndAiPluginAggregators() throws Exception {
    Path root = findRepositoryRoot();
    Set<String> bootModules = readModules(root.resolve(
            "fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/pom.xml"));
    Set<String> aiModules = readModules(root.resolve(
            "fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/pom.xml"));

    assertEquals(25, bootModules.size());
    assertEquals(2, aiModules.size());
    assertEquals(expectedArtifacts("com.xiongx9527.fw.plus.boot"), bootModules);
    assertEquals(expectedArtifacts("com.xiongx9527.fw.plus.ai"), aiModules);
}

private Path findRepositoryRoot() {
    Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
    while (current != null) {
        if (Files.isRegularFile(current.resolve("fw-plus-sample/pom.xml"))
                && Files.isDirectory(current.resolve("fw-plus-boot"))) {
            return current;
        }
        current = current.getParent();
    }
    throw new IllegalStateException("Cannot locate repository root from user.dir");
}

private Set<String> readModules(Path pom) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    NodeList modules = factory.newDocumentBuilder().parse(pom.toFile()).getElementsByTagName("module");
    Set<String> result = new LinkedHashSet<String>();
    for (int i = 0; i < modules.getLength(); i++) {
        result.add(modules.item(i).getTextContent().trim());
    }
    return result;
}

private Set<String> expectedArtifacts(String groupId) {
    Set<String> result = new LinkedHashSet<String>();
    for (String coordinate : PLUGIN_ARTIFACTS) {
        String[] parts = coordinate.split(":", 2);
        if (groupId.equals(parts[0])) {
            result.add(parts[1]);
        }
    }
    return result;
}
```

补充 `DocumentBuilderFactory`、`NodeList`、`Files`、`Path`、`Paths`、`LinkedHashSet`、`Set` 的导入。`readModules` 返回聚合器模块目录名；这些目录名与当前 artifactId 一致，因此直接和 `expectedArtifacts(groupId)` 比较，不复制第二份手写清单。

- [x] **Step 3: 运行 sample 清单测试**

```bash
./fw-plus-core/mvnw \
  -f fw-plus-sample/fw-plus-boot-samples/fw-plus-magicapi-spring-boot-starter-sample/pom.xml \
  -Dtest=MagicApiAllPluginsClasspathTest test
```

Expected: 两个测试 PASS；Boot=25、AI=2、runtime missing list 为空。

- [x] **Step 4: 更新 OpenSpec 任务 1**

在 `openspec/changes/fix-magicapi-plugin-sample-integration/tasks.md` 将任务 1 标记为完成，并在同一行后追加简短证据：27 个 artifact 测试通过、首次 sample 启动 RED 日志路径已记录。

- [x] **Step 5: 提交 sample 清单契约**

```bash
git add \
  fw-plus-sample/fw-plus-boot-samples/fw-plus-magicapi-spring-boot-starter-sample/pom.xml \
  fw-plus-sample/fw-plus-boot-samples/fw-plus-magicapi-spring-boot-starter-sample/src/test/java/com/xiongx9527/fw/plus/boot/magicapi/MagicApiAllPluginsClasspathTest.java \
  openspec/changes/fix-magicapi-plugin-sample-integration/tasks.md
git commit -m "test(magicapi): cover all plugin artifacts in sample"
```

### Task 4: 让可选向量 SDK 缺失时 AI provider 自动配置可安全装配

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/store/embeding/EmbeddingStoreAutoConfiguration.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/store/embeding/EmbeddingStoreAutoConfigurationTest.java`

**Interfaces:**
- Consumes: embedding starter 不传递全部 provider SDK 的既有设计，以及直接链接 Redis、Milvus、Elasticsearch、Qdrant、Weaviate、Azure AI Search SDK 类型的 adapter。
- Produces: 缺少具体 SDK 时仍可创建 `EmbeddingStoreRegistry`，只跳过对应 adapter Bean；Task 5 的真实 sample 不通过 profile 禁用 vector 插件。

- [x] **Step 1: 保留真实 sample 消费侧 RED 证据**

读取 `.superpowers/sdd/2026-07-27-magicapi-plugin-sample-integration/task-5-preflight-report.md`，确认刷新 Boot 旧快照后真实 sample 失败为：

```text
BeanCreationException: milvusEmbeddingStoreProviderAdapter
NoClassDefFoundError: dev/langchain4j/store/embedding/milvus/MilvusEmbeddingStore$Builder
```

确认 sample dependency tree 不包含 `langchain4j-milvus`，且 starter POM 明确不传递所有具体向量 SDK。禁止通过 profile 关闭 embedding store，也不在 sample 逐个补 SDK。

- [x] **Step 2: 编写缺少可选 SDK 的消费侧失败测试**

在 `EmbeddingStoreAutoConfigurationTest` 中使用新的 `ApplicationContextRunner` 和 `FilteredClassLoader` 隐藏以下包：

```text
dev.langchain4j.store.embedding.redis
dev.langchain4j.store.embedding.milvus
dev.langchain4j.store.embedding.elasticsearch
dev.langchain4j.store.embedding.qdrant
dev.langchain4j.store.embedding.weaviate
dev.langchain4j.store.embedding.azure.search
```

测试必须断言上下文无启动失败、`EmbeddingStoreRegistry` 存在、`inMemoryEmbeddingStoreProviderAdapter` 仍存在，并且上述 6 个直接链接型 adapter Bean 均不存在。不得过滤整个 `dev.langchain4j.store.embedding` 包。

- [x] **Step 3: 运行 AI 消费侧测试确认 RED**

```bash
./fw-plus-ai/mvnw -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml \
  -Dtest=EmbeddingStoreAutoConfigurationTest#shouldStartWithoutOptionalTypedProviderSdks test
```

Expected: FAIL，根因为 Milvus 或另一个被过滤的直接链接型 provider 类无法加载，而不是测试装配错误。

- [x] **Step 4: 对直接链接型 adapter 增加最小 classpath 条件**

为 Redis、Milvus、Elasticsearch、Qdrant、Weaviate、Azure AI Search 的 6 个 `@Bean` 方法分别增加：

```java
@ConditionalOnClass(name = "对应 provider SDK 入口类全限定名")
```

入口类使用 Step 2 列出的包内实际 embedding store 类。使用 `name` 字符串避免条件解析提前加载缺失类型。不得给整个 `EmbeddingStoreAutoConfiguration` 加总开关，不修改反射型 provider、registry、factory 或公开 API。

- [x] **Step 5: 运行 AI 自动配置回归**

```bash
./fw-plus-ai/mvnw -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml \
  -Dtest=EmbeddingStoreAutoConfigurationTest,EmbeddingStoreAutoConfigurationMetadataTest test
```

Expected: 消费侧缺 SDK 测试与现有 embedding/元数据测试全部通过；完整测试 classpath 下既有 dedicated adapter 测试仍通过。

- [x] **Step 6: 安装 AI 自动配置快照并提交**

```bash
./fw-plus-ai/mvnw -f fw-plus-ai/fw-plus-ai-project/pom.xml \
  -pl fw-plus-ai-boot-autoconfigure -DskipTests install
git add \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/store/embeding/EmbeddingStoreAutoConfiguration.java \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/store/embeding/EmbeddingStoreAutoConfigurationTest.java
git commit -m "fix(ai): condition optional embedding providers"
```

### Task 5: 让真实 sample 主类完成离线全插件上下文与 HTTP 验证

**Files:**
- Modify: `fw-plus-sample/fw-plus-boot-samples/fw-plus-magicapi-spring-boot-starter-sample/src/main/java/com/xiongx9527/fw/plus/boot/magicapi/MagicapiSampleApplication.java`
- Create: `fw-plus-sample/fw-plus-boot-samples/fw-plus-magicapi-spring-boot-starter-sample/src/main/resources/application-all-plugins.yaml`
- Create: `fw-plus-sample/fw-plus-boot-samples/fw-plus-magicapi-spring-boot-starter-sample/src/test/java/com/xiongx9527/fw/plus/boot/magicapi/MagicApiAllPluginsApplicationContextTest.java`
- Modify: `openspec/changes/fix-magicapi-plugin-sample-integration/tasks.md`

**Interfaces:**
- Consumes: Task 1 的可加载 AI 元数据、Task 2 的无循环 MQTT storage、Task 3 的 27 插件 runtime classpath。
- Produces: `all-plugins` profile，以及真实 `MagicapiSampleApplication` 的离线完整上下文和 HTTP smoke test。

- [x] **Step 1: 新增离线 profile**

```yaml
server:
  port: 0

apollo:
  bootstrap:
    enabled: false

fw:
  plus:
    config:
      event:
        apollo-enabled: false

spring:
  cloud:
    config:
      enabled: false

management:
  endpoints:
    web:
      exposure:
        include: health

magic-api:
  resource:
    type: file
    location: ${java.io.tmpdir}/fw-plus-magicapi-all-plugins
```

profile 不设置任何真实服务地址，也不把 27 个插件从自动配置中排除。

- [x] **Step 2: 编写真实应用失败测试**

```java
@SpringBootTest(
        classes = MagicapiSampleApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("all-plugins")
class MagicApiAllPluginsApplicationContextTest {

    private static final Path RESOURCE_DIRECTORY = createResourceDirectory();

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void resourceProperties(DynamicPropertyRegistry registry) {
        registry.add("magic-api.resource.location", () -> RESOURCE_DIRECTORY.toString());
    }

    @Test
    void startsRealSampleWithoutApolloOrExternalResources() {
        assertThat(applicationContext.getBeansOfType(MagicResourceService.class)).hasSize(1);
        assertThat(applicationContext.containsBean("apolloConfigChangeAdapter")).isFalse();
        assertThat(environment.getProperty("magic-api.resource.location"))
                .isEqualTo(RESOURCE_DIRECTORY.toString());
        assertThat(StreamSupport.stream(environment.getPropertySources().spliterator(), false)
                .map(PropertySource::getName))
                .noneMatch(name -> name.toLowerCase(Locale.ROOT).contains("apollo"));
    }

    @Test
    void exposesHealthAndMagicEditor() {
        ResponseEntity<String> health = restTemplate.getForEntity("/actuator/health", String.class);
        assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(health.getBody()).contains("\"status\":\"UP\"");

        ResponseEntity<String> editor = restTemplate.getForEntity("/magic-api/web/index.html", String.class);
        assertThat(editor.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(editor.getBody()).contains("<title>magic-api</title>");
    }

    private static Path createResourceDirectory() {
        try {
            return Files.createTempDirectory("fw-plus-magicapi-all-plugins-");
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @AfterAll
    static void deleteResourceDirectory() throws IOException {
        Files.deleteIfExists(RESOURCE_DIRECTORY);
    }
}
```

补充 `IOException`、`Files`、`Path`、`Locale`、`StreamSupport`、`@AfterAll`、`@Autowired`、`@SpringBootTest`、`TestRestTemplate`、`@ActiveProfiles`、`@DynamicPropertySource`、`DynamicPropertyRegistry`、`ApplicationContext`、`ConfigurableEnvironment`、`PropertySource`、`HttpStatus`、`ResponseEntity`、`MagicResourceService` 和 AssertJ `assertThat` 的导入。`@AfterAll` 只删除该测试创建的空目录；若目录非空则测试清理失败，以暴露意外资源写入。

- [x] **Step 3: 运行测试确认 RED**

```bash
./fw-plus-core/mvnw \
  -f fw-plus-sample/fw-plus-boot-samples/fw-plus-magicapi-spring-boot-starter-sample/pom.xml \
  -Dtest=MagicApiAllPluginsApplicationContextTest test
```

Expected before sample 主类修复: FAIL，Apollo PropertySource 仍存在或日志出现 Apollo 内网访问；如果 Task 2 尚未完成，也会先复现 MQTT Bean 循环。

- [x] **Step 4: 移除 sample 主类冗余 Apollo 注解**

删除 `MagicapiSampleApplication` 中：

```java
import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
@EnableApolloConfig
```

保留 `@SpringBootApplication` 和 `main` 方法不变。默认 `application.yaml` 的 `apollo.bootstrap.enabled=true` 继续负责正常 Apollo 启用语义。

- [x] **Step 5: 运行完整 sample 测试转 GREEN**

```bash
./fw-plus-core/mvnw \
  -f fw-plus-sample/fw-plus-boot-samples/fw-plus-magicapi-spring-boot-starter-sample/pom.xml test
```

Expected: classpath/聚合清单、完整上下文、健康和 Magic Editor 测试全部 PASS；日志无 Apollo 长轮询和 Bean 循环。

- [x] **Step 6: 更新 OpenSpec 任务 2**

将 `tasks.md` 任务 2 标记为完成，并记录 MQTT RED/GREEN、Apollo 隔离和真实上下文/HTTP 测试命令。

- [x] **Step 7: 提交 sample 离线启动修复**

```bash
git add \
  fw-plus-sample/fw-plus-boot-samples/fw-plus-magicapi-spring-boot-starter-sample/src/main/java/com/xiongx9527/fw/plus/boot/magicapi/MagicapiSampleApplication.java \
  fw-plus-sample/fw-plus-boot-samples/fw-plus-magicapi-spring-boot-starter-sample/src/main/resources/application-all-plugins.yaml \
  fw-plus-sample/fw-plus-boot-samples/fw-plus-magicapi-spring-boot-starter-sample/src/test/java/com/xiongx9527/fw/plus/boot/magicapi/MagicApiAllPluginsApplicationContextTest.java \
  openspec/changes/fix-magicapi-plugin-sample-integration/tasks.md
git commit -m "test(magicapi): start all plugins in sample"
```

### Task 6: 执行分层构建与独立 sample 进程验证

**Files:**
- Modify: `openspec/changes/fix-magicapi-plugin-sample-integration/tasks.md`
- Modify: `openspec/changes/fix-magicapi-plugin-sample-integration/.comet/context.md`（追加最终命令、结果、日志路径，不写主观总结）

**Interfaces:**
- Consumes: Tasks 1-5 的提交与本地 `0.1.1-SNAPSHOT` 依赖。
- Produces: Core -> Boot -> Cloud / AI -> Sample 的完整构建证据，以及独立 sample 进程的健康、Magic-API Web、日志和端口释放证据。

- [x] **Step 1: 运行定向回归总集**

```bash
./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/pom.xml \
  -Dtest=MagicAPIMqttClientConfigurationTest,MagicAPIRedisConfigurationTest,MagicAPIMcpConfigurationTest test
./fw-plus-ai/mvnw -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml \
  -Dtest=EmbeddingStoreAutoConfigurationMetadataTest test
./fw-plus-core/mvnw -f fw-plus-sample/fw-plus-boot-samples/fw-plus-magicapi-spring-boot-starter-sample/pom.xml test
```

Expected: 三组命令均成功，0 failures / 0 errors。

- [x] **Step 2: 按层安装全部本地快照**

```bash
./fw-plus-core/mvnw -f fw-plus-core/pom.xml clean install
./fw-plus-boot/mvnw -f fw-plus-boot/pom.xml clean install
./fw-plus-cloud/mvnw -f fw-plus-cloud/pom.xml clean install
./fw-plus-ai/mvnw -f fw-plus-ai/pom.xml clean install
./fw-plus-core/mvnw -f fw-plus-sample/pom.xml clean verify
```

Expected: 所有 reactor `BUILD SUCCESS`，最终命令不使用 `-DskipTests`。Wrapper 不可用时改用系统 Maven 3.6.3 + JDK 8，并记录该入口差异。

- [x] **Step 3: 启动独立 sample 进程**

选择从 `39089` 起第一个未监听端口，创建 `mktemp -d` 资源目录和 `mktemp` 日志文件：

```bash
SAMPLE_PORT=39089
while lsof -nP -iTCP:${SAMPLE_PORT} -sTCP:LISTEN >/dev/null 2>&1; do
  SAMPLE_PORT=$((SAMPLE_PORT + 1))
done
SAMPLE_RESOURCE_DIR=$(mktemp -d)
SAMPLE_LOG=$(mktemp)
```

然后运行：

```bash
./fw-plus-core/mvnw \
  -f fw-plus-sample/fw-plus-boot-samples/fw-plus-magicapi-spring-boot-starter-sample/pom.xml \
  org.springframework.boot:spring-boot-maven-plugin:2.7.18:run \
  -Dspring-boot.run.profiles=all-plugins \
  -Dspring-boot.run.arguments="--server.port=${SAMPLE_PORT} --magic-api.resource.location=${SAMPLE_RESOURCE_DIR}" \
  > "${SAMPLE_LOG}" 2>&1 &
SAMPLE_PID=$!
```

立即安装退出清理 trap：正常或失败都终止 `${SAMPLE_PID}`，等待进程结束，并删除本次创建的空临时资源目录。

```bash
cleanup_sample() {
  if kill -0 "${SAMPLE_PID}" 2>/dev/null; then
    kill "${SAMPLE_PID}"
    wait "${SAMPLE_PID}" 2>/dev/null || true
  fi
  rmdir "${SAMPLE_RESOURCE_DIR}" 2>/dev/null || true
}
trap cleanup_sample EXIT
```

- [x] **Step 4: 有界轮询健康入口**

每 2 秒检查一次，首轮最多 60 次；若进程提前退出立即失败并输出 `${SAMPLE_LOG}` 末尾。

```bash
curl --fail --silent "http://127.0.0.1:${SAMPLE_PORT}/actuator/health"
```

Expected: 120 秒目标窗口内返回 HTTP 200，body 包含 `"status":"UP"`。如进程仍存活但首次完整 classpath 启动尚未完成，可再轮询 60 次，240 秒为硬上限，并记录实际就绪时间。

- [x] **Step 5: 验证 Magic-API Web 与日志**

```bash
curl --fail --silent "http://127.0.0.1:${SAMPLE_PORT}/magic-api/web/index.html"
```

Expected: HTTP 200，body 包含 `<title>magic-api</title>`。

以下模式必须全部不存在：

```text
Apollo-RemoteConfigLongPollService
172.17.17.185:8080
BeanCurrentlyInCreationException
dependencies of some of the beans in the application context form a cycle
APPLICATION FAILED TO START
```

执行：

```bash
if rg -n "Apollo-RemoteConfigLongPollService|172\\.17\\.17\\.185:8080|BeanCurrentlyInCreationException|dependencies of some of the beans in the application context form a cycle|APPLICATION FAILED TO START" "${SAMPLE_LOG}"; then
  exit 1
fi
```

- [x] **Step 6: 停止进程并确认端口释放**

向 `${SAMPLE_PID}` 发送 `TERM`，有界等待退出；仅在超时后发送 `KILL`。随后执行：

```bash
lsof -nP -iTCP:${SAMPLE_PORT} -sTCP:LISTEN
```

Expected: 无输出，端口已释放。

- [x] **Step 7: 更新最终证据与任务状态**

将各层 Maven 结果、sample PID/端口、健康响应、Magic Editor 响应、负向日志扫描和端口释放写入 `.comet/context.md`。将 `tasks.md` 任务 3 标记为完成。

- [x] **Step 8: 提交验证证据**

```bash
git add \
  openspec/changes/fix-magicapi-plugin-sample-integration/tasks.md \
  openspec/changes/fix-magicapi-plugin-sample-integration/.comet/context.md
git commit -m "test(magicapi): record all-plugin verification"
```

不要提交 `${SAMPLE_LOG}`、临时目录、`.flattened-pom.xml`、`node_modules/` 或 console `dist/`。

---

## Plan Self-Review

- Spec coverage: AI 元数据、MQTT 循环、27 插件同步、Apollo 隔离、真实 sample 上下文、HTTP、分层构建和端口释放均有对应任务。
- Dependency order: Tasks 1-3 可并行；Task 4 依赖 Tasks 1-3；Task 5 依赖 Tasks 1-4；Task 6 依赖 Tasks 1-5。
- Type consistency: `MagicResourceService`、`MqttClientDataSourceInfoMagicResourceStorage`、`magic-api.resource.location`、`fw.plus.config.event.apollo-enabled` 在设计与任务中保持一致。
- Scope: 不包含真实外部中间件读写，不新增 capability 或公共 API。
