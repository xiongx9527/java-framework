# Spring Boot 3 升级说明

> **文档定位**：fw-plus 从 Spring Boot 2.7.18 迁移到 Spring Boot 3.x 的完整升级说明。
> 包含代码层改动（`javax` → `jakarta`）、依赖坐标替换、配置项重命名、版本矩阵与分阶段路径。
>
> **当前基线**（`fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`）：
> Spring Boot `2.7.18`、Spring Cloud `2021.0.9`、JDK `1.8`、MyBatis-Plus `3.3.2`、
> Magic-API `2.2.2`（内部维护版）、Sa-Token `1.44.0`、knife4j `4.5.0`、JimuReport `2.3.4`。

---

## 0. 结论速览

| 维度 | 现状 | 目标 | 风险 |
|---|---|---|---|
| JDK | 1.8 | **17** | 低（四层 pom 各改一个属性） |
| Servlet API | `javax.servlet` | `jakarta.servlet` | **中**（32 个主源码文件） |
| 校验 API | `javax.validation` | `jakarta.validation` | **中**（4 个主源码文件 + 自研校验器） |
| Redis 配置 | `spring.redis.*` | `spring.data.redis.*` | **中**（含条件装配注解，改错会静默失效） |
| MyBatis-Plus | 3.3.2 | **≥3.5.5** | **高**（跨 12 个小版本，API 破坏性变更多） |
| Magic-API | 2.2.2 | **3.0.0** | 中（内部维护版，需同步发 jakarta 版） |
| knife4j | 4.5.0（SB2 坐标） | 4.6.0（jakarta 坐标） | 中（上游活跃度低） |
| 自动配置注册 | `spring.factories` + `.imports` 并存 | 仅 `.imports` | 低（`.imports` 已就位） |

**整体判断**：这不是一次「改个版本号」的升级。真正的成本集中在三处：
**① `javax` → `jakarta` 的机械改造；② MyBatis-Plus 3.3.2 → 3.5.x 的 API 断裂；③ Magic-API 内部维护版的 jakarta 化。**

---

## 1. 目标版本矩阵

### 1.1 建议基线

选 **Spring Boot 3.2.x + Spring Cloud 2023.0.x**：既满足 jakarta 要求，
又比 3.4/3.5 更成熟（3.4+ 抬到 Spring Framework 6.2，会连带影响 springdoc、knife4j 等）。

| 组件 | 现状 | 目标 | 备注 |
|---|---|---|---|
| JDK | 1.8 | **17** | Spring Boot 3 硬性要求 |
| Spring Boot | 2.7.18 | **3.2.x** | |
| Spring Cloud | 2021.0.9 | **2023.0.x** | `2022.0.x` → Boot 3.0/3.1；`2024.0.x` → Boot 3.4 |
| Spring Boot Admin | 2.7.16 | **3.x** | `fw-plus-boot-dependencies` |
| MyBatis | 3.5.19 | 3.5.x（随 MP） | |
| mybatis-spring | 2.1.2 | **3.0.3+** | 2.x 不兼容 Spring 6 |
| MyBatis-Plus | 3.3.2 | **3.5.5+** | 3.5.5 起提供 `mybatis-plus-spring-boot3-starter` |
| MyBatis-Flex | 1.11.4 | 保持 | 原生支持 SB3 |
| Magic-API | 2.2.2 | **3.0.0** | 3.0.0 starter 已硬依赖 `magic-api-servlet-jakarta` |
| Sa-Token | 1.44.0 | **保持 1.44.0** | 版本已支持，仅换 artifactId |
| JimuReport | 2.3.4 | **保持 2.3.4** | 同版本已有 SB3 starter |
| knife4j | 4.5.0 | **4.6.0** | 换 jakarta artifactId |
| Druid | 1.2.24 | 1.2.24+ | 换 `druid-spring-boot-3-starter` |
| langchain4j | 0.35.0 | **1.x** | 见附录 A |

### 1.2 关于 Magic-API 的好消息

内部 Nexus 里已存在 **Magic-API 3.0.0**，且其 `magic-api-spring-boot-starter:3.0.0`
的 pom 中 `magic-api-servlet-jakarta` 为**硬依赖**、`magic-api-servlet-javaee` 已被注释掉：

```xml
<artifactId>magic-api-servlet-jakarta</artifactId>
<!-- <artifactId>magic-api-servlet-javaee</artifactId> -->
```

即 3.0.0 已是 jakarta 专用线。**但仍需确认 3.0.0 是否补齐了本仓用到的全部插件**
（redis / mongo / elasticsearch / nebula / cluster / git / task / springdoc / swagger）
的 3.0.0 版本，以及 magic-script 的 breaking change。

---

## 2. 阻塞项清单

### 2.1 `javax.*` → `jakarta.*`

#### 必须迁移的包（Jakarta EE 10 专属）

| 现状包名 | 目标包名 | 主源码文件数 |
|---|---|---|
| `javax.servlet.*` | `jakarta.servlet.*` | **32** |
| `javax.validation.*` | `jakarta.validation.*` | **4** |
| `javax.annotation.Resource` | `jakarta.annotation.Resource` | **1** |

#### 不需要迁移的包（Jakarta EE 10 保留 `javax` 前缀）

`javax.sql.*`、`javax.xml.*`、`javax.tools.*`、`javax.naming.*`、`javax.crypto.*`、
`javax.management.*`、`javax.imageio.*`、`javax.script.*`、`javax.print.*`。

> 本仓 `javax.sql.DataSource`（36 处）、`javax.xml.*`（transform/parsers/sax/dom/stax）、
> `javax.tools.*`（`InMemoryJavaEntityCompiler` 的 javac 调用）、`javax.sql.rowset.*`
> **全部无需改动**。这一步很容易误改，先划清边界。

#### `javax.servlet` 主源码分布（32 个文件）

| 模块 | 文件数 | 代表文件 |
|---|---|---|
| `fw-plus-boot-project` | 15 | `boot-common` 的请求/响应 Wrapper（4 个）、`WebMvcCommonFilter`、`AuthFilter`、`JimuReportSysSwitchFilter`、`JimuJsEnterpriseFilter`、`DsHeaderProcessor`、`DsSessionProcessor`、`StaticResourceFilter`、`Vue3ResourceFilter`、`MagicSwaggerConfiguration`、`ConfigMagicDynamicRegistry`、`MagicAPIRedisMessageListenerContainer`、`Druid*Configuration` |
| `fw-plus-project` | 12 | `WebMvcRequestContextHolder`（fw-plus-common）、EasyPoi fork 的 `cn.afterturn.easypoi.view.*`（9 个）、`MateEasyTransUtils` |
| `fw-plus-ai-project` | 5 | MCP 的 `McpTransportController` / `McpRoutingRegistrar` / `McpStreamableHttpController` / `McpSseController`、`KbConsoleController` |

**特殊注意**：`fw-plus-poi-easypoi` 是**源码内嵌的 EasyPoi fork**（包名 `cn.afterturn.easypoi`），
不是外部依赖，所以 jakarta 改造要**在本仓内直接改**，无法靠换依赖版本绕过。

#### `javax.validation` / `javax.annotation` 主源码分布

- `fw-plus-common/.../validator/CommonsValidator.java`（自研校验器桥接 javax.validation）
- `fw-plus-common/.../validator/CommonsValidatorAnn.java`（自定义约束注解）
- `fw-plus-poi-easypoi/.../util/PoiValidationUtil.java`
- `fw-plus-boot-autoconfigure/.../mate/webmvc/validator/CommonsValidatorRequestBodyAdvice.java`
  （第 34 行**硬编码引用** `javax.validation.Valid.class`，容易漏改）
- `fw-plus-boot-autoconfigure/.../magicapi/plugin/config/service/ConfigMagicDynamicRegistry.java`
  （`@Resource` 注入）
- 样例另有 10 个文件使用 `javax.validation` / `javax.annotation.Resource`

#### 推荐改造手段

优先用 **OpenRewrite** 的 `org.openrewrite.java.spring.boot3.SpringBoot3_0_Migration` 配方做批量改造，
再人工复核。它能同时处理 import 重写、`spring.redis` → `spring.data.redis`、
`spring-boot-starter-parent` 版本、以及一批 API 迁移。

```bash
# 建议先在独立分支 + 干净工作区执行
./mvnw -U org.openrewrite.maven:rewrite-maven-plugin:run \
  -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:spring-boot3-migration:RELEASE
```

> 注意：OpenRewrite 不会碰 `fw-plus-poi-easypoi` 里的 `cn.afterturn.*`（非本项目包名可能漏改），
> 这部分仍需手工核对。

### 2.2 依赖坐标替换（同名不同 artifact，版本不用动）

这类最容易漏——**版本号是对的，但 artifactId 是 SB2 专用坐标，装上就是 javax，运行时炸**。

| 模块 | 现状坐标 | SB3 坐标 | 版本 |
|---|---|---|---|
| 认证 | `cn.dev33:sa-token-spring-boot-starter` | `sa-token-spring-boot3-starter` | 1.44.0 保持 |
| 报表 | `org.jeecgframework.jimureport:jimureport-spring-boot-starter` | `jimureport-spring-boot3-starter` | 2.3.4 保持 |
| 报表(BI) | `jimubi-spring-boot-starter` | `jimubi-spring-boot3-starter` | 保持 |
| 文档 | `com.github.xiaoymin:knife4j-openapi3-spring-boot-starter` | `knife4j-openapi3-jakarta-spring-boot-starter` | 4.5.0 → **4.6.0** |
| 文档(WebFlux) | `knife4j-openapi3-webflux-spring-boot-starter` | `knife4j-openapi3-webflux-jakarta-spring-boot-starter` | 4.6.0 |
| Druid | `com.alibaba:druid-spring-boot-starter` | `druid-spring-boot-3-starter` | 保持 |
| MyBatis-Plus | `com.baomidou:mybatis-plus-boot-starter` | `mybatis-plus-spring-boot3-starter` | 3.3.2 → **≥3.5.5** |

> **knife4j 额外风险**：上游 `xiaoymin/knife4j` 活跃度很低，4.5.0 内嵌
> `springdoc-openapi-starter-webmvc-ui:2.3.0`，在 Spring Boot 3.4+/Framework 6.2 下会抛
> `NoSuchMethodError: ControllerAdviceBean.<init>`。若选 3.2 基线可规避；
> 若必须上 3.4+，需显式覆盖 springdoc 到 2.7.0+，或评估社区 fork
> `com.github.xingfudeshi:knife4j-openapi3-jakarta-spring-boot-starter`。

### 2.3 配置项重命名

#### `spring.redis.*` → `spring.data.redis.*`（**本仓最隐蔽的坑**）

Spring Boot 3 把 Redis 配置移到了 `spring.data.redis` 命名空间。**旧 key 不报错，只是静默失效**，
表现为「配置读不到、回落到默认值」。

| 文件 | 行 | 内容 |
|---|---|---|
| `fw-plus-boot-autoconfigure/.../magicapi/plugin/redis/module/RedisModule.java` | 35–43 | `@Value("${spring.redis.host:}")` / `port` / `username` / `password` / `database` |
| `fw-plus-boot-autoconfigure/.../nosql/RedisDynamicDatasourceAutoConfiguration.java` | 297 | `@ConditionalOnMissingProperty("spring.redis.cluster.nodes")` |
| 同上 | 330 | `@ConditionalOnExistingProperty({"spring.redis.cluster.nodes"})` |
| `fw-plus-boot-autoconfigure/.../nosql/redis/config/properties/RedisDataSourceProperties.java` | 31–38 | javadoc 示例 + `spring.redis.dynamic-database` |
| `fw-plus-redis` 的 `EnvironmentUtilTest` | 26–41 | 测试用例 |
| `fw-plus-magicapi-spring-boot-starter-sample/src/main/resources/application.yaml` | 41–47 | 样例配置 |

⚠️ **第 297 / 330 行是条件装配注解**——key 改错会导致单实例/集群模式的
`RedisTemplate` 分支**完全不走**，且不抛异常、无日志提示。这类问题在集成测试里也未必暴露。

> `spring.redis.datasource.*` 是 fw-plus 自己的自定义命名空间（`RedisMultiSourceRegisterRunner`、
> `RedisMultiDataSourceRegister`），**不在 Spring Boot 的重命名范围内，不要改**。

#### 其他常见重命名（本仓当前未命中，一并记录）

- `server.max-http-header-size` → `server.max-http-request-header-size`
- `management.metrics.export.*` → `management.<exporter>.export.*`
- `spring.mvc.pathmatch.matching-strategy` → Boot 3 默认值改为 `PATH_PATTERN_PARSER`

### 2.4 `spring.factories` 清理

**好消息**：三个 autoconfigure 模块**已经同时存在**
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
和 `META-INF/spring.factories`，所以自动配置在 SB3 下能正常加载。

**但**：`EnableAutoConfiguration` 在 `spring.factories` 中的写法**在 Spring Boot 3.0 已被移除支持**。
现有 4 个 `spring.factories` 里的 `EnableAutoConfiguration` 段落升级后即为死配置，可清理：

- `fw-plus-ai-boot-autoconfigure/src/main/resources/META-INF/spring.factories`
- `fw-plus-boot-autoconfigure/src/main/resources/META-INF/spring.factories`
- `fw-plus-cloud-autoconfigure/src/main/resources/META-INF/spring.factories`
- `fw-plus-cloud/src/main/resources/META-INF/spring.factories`（内容为空）

⚠️ **同文件内的以下两项必须保留**（SB3 仍从 `spring.factories` 读取）：
- `fw-plus-boot-autoconfigure` 的 `org.springframework.boot.autoconfigure.AutoConfigurationImportFilter`
  → `MagicAPIWebFluxImportFilter`
- `fw-plus-boot-autoconfigure` 的 `org.springframework.cloud.bootstrap.BootstrapConfiguration`
  → `ConfigClientBootstrapConfiguration`

清理时**按 key 精确删除**，不要整文件删除。

### 2.5 JDK 8 → 17

四层顶层 pom 各有一处：

| 文件 | 行 | 属性 |
|---|---|---|
| `fw-plus-core/pom.xml` | 29 | `<jdk.version>1.8</jdk.version>` |
| `fw-plus-boot/pom.xml` | 21 | 同上 |
| `fw-plus-cloud/pom.xml` | 21 | 同上 |
| `fw-plus-ai/pom.xml` | 29 | 同上 |

改为 `<jdk.version>17</jdk.version>`。同时建议把
`<source>/<target>` 换成 `<release>${jdk.version}</release>`
（`fw-plus-core/pom.xml:55-56` 等），避免误用高版本 JDK 编译时的 API 泄漏。

#### 连带解除的约束

`AGENTS.md` 中「**JDK 1.8 约束**：不要引入 Java 9+ API（`String.isBlank()`、`List.of()`、
`Map.copyOf()`、`Optional.orElseThrow()`）」这条限制在升级后失效，上述写法均可使用。
升级完成后应同步更新 `AGENTS.md`。

#### 新的限制

JDK 17 下 `--illegal-access` 等已移除；使用 `javax.tools.JavaCompiler` 的
`InMemoryJavaEntityCompiler` 需回归验证（模块化强封装对反射访问内部 JDK 包更严格）。

### 2.6 路径匹配语义变化

`org.springframework.util.AntPathMatcher` 在 Spring 6 中**已废弃**（推荐 `PathPatternParser`），
且 Boot 3 的 MVC 默认路径匹配策略从 `ANT_PATH_MATCHER` 改为 `PATH_PATTERN_PARSER`。
虽然 `AntPathMatcher` 仍可用，但语义有差异（尾部斜杠、`**` 前缀匹配等）。

本仓使用 `AntPathMatcher` 的位置：

- `fw-plus-common/.../util/WebUtils.java:10,31`
- `fw-plus-ai-boot-autoconfigure/.../langchain4j/toolprovider/MagicApiToolRegistry.java:5,43`
- `fw-plus-ai-boot-autoconfigure/.../mcp/tools/MagicApiMcpToolRegistry.java:7,46`

这 3 处应保持**行为一致**——建议统一迁移到 `PathPatternParser`，或在升级说明中
明确记录「保留 `AntPathMatcher` 并关闭新策略」：
`spring.mvc.pathmatch.matching-strategy=ant_path_matcher`。

---

## 3. 推荐升级路径

分四阶段，每阶段可独立编译验证，**不要一次性全改**。

### 阶段 0：建分支 + 冻结基线

```bash
git switch -c springboot3
./mvnw clean install -DskipTests   # 记录当前全绿基线
```

### 阶段 1：纯 jakarta 化（不改业务语义）

1. 用 OpenRewrite 跑 `spring-boot3-migration` 配方。
2. 手工复核 `fw-plus-poi-easypoi`（`cn.afterturn.*`）与
   `CommonsValidatorRequestBodyAdvice` 的硬编码引用。
3. **此时 `spring.redis` 暂不改**——它与 jakarta 无关，留到阶段 3，避免问题耦合。
4. 编译通过即可（此时运行会因 javax/jakarta 混用报错，属预期）。

**验收**：`grep -rl "javax\.\(servlet\|validation\|persistence\|annotation\.\(Resource\|PostConstruct\|PreDestroy\)\)" --include="*.java"` 返回空。

### 阶段 2：依赖坐标与版本

1. 按 §2.2 表格换 artifactId（Sa-Token / JimuReport / knife4j / Druid）。
2. MyBatis-Plus 3.3.2 → 3.5.5+，换 `mybatis-plus-spring-boot3-starter`，
   mybatis-spring → 3.0.3+。
   ⚠️ **MP 3.3.2 → 3.5.x 是本次升级最大的 API 断裂点**，重点回归
   `fw-plus-mp-base` / `fw-plus-mp-datachange` / `fw-plus-mp-datascope` /
   `fw-plus-mp-encrypt` / `fw-plus-mp-join` 与 EasyTrans 扩展。
3. Magic-API 2.2.2 → 3.0.0，核对全部插件 artifact 齐备。
4. Spring Boot / Cloud / Boot Admin 版本对齐（§1.1）。

**验收**：`./mvnw dependency:tree | grep -E "javax.servlet|javax.validation"` 应无本仓期望的 SB2 组件残留。

### 阶段 3：配置与装配

1. `spring.redis.*` → `spring.data.redis.*`（含 §2.3 的条件装配注解）。
2. 清理 `spring.factories` 中的 `EnableAutoConfiguration`（保留 ImportFilter 与 BootstrapConfiguration）。
3. `jdk.version` → 17，`source/target` → `release`。
4. 决定 `AntPathMatcher` 的去留。

**验收**：启动各层样例，确认单实例/集群 Redis 两种模式均装配成功（这一步最容易被静默跳过）。

### 阶段 4：AI 层（langchain4j 0.35.0 → 1.x）

见附录 A。这部分是独立的破坏性升级，与 SB3 无耦合，可最后做。

---

## 4. 验证清单

| 项 | 检查方式 |
|---|---|
| 无 javax EE 残留 | `grep -rl "javax\.\(servlet\|validation\|persistence\)" --include="*.java"` 为空 |
| 自动配置加载正常 | 启动后 `ConditionEvaluationReport` 中 fw-plus 的 AutoConfiguration 均为 `Matched` |
| Redis 单实例/集群 | 分别用 `spring.data.redis.*` 与 `spring.data.redis.cluster.nodes` 启动样例，验证 `RedisTemplate` 分支 |
| Magic-API 控制台 | 访问 `/magic/web`，确认脚本执行与静态资源正常 |
| Swagger 文档 | 访问 `/doc.html` 与 `/v3/api-docs` |
| JimuReport | 打开一张报表，确认数据集与导出正常 |
| MyBatis-Plus | 跑通 `fw-plus-mp-spring-boot-starter-sample`（含数据权限、字段加密、变更追踪） |
| Sa-Token | 登录态读写、`StpUtil` 拦截器链 |
| MQ / 存储插件 | Kafka、Pulsar、RocketMQ、RabbitMQ、MQTT、ES、InfluxDB 各连一次 |
| 云层 | Eureka Server/Client、灰度负载均衡、Gateway |
| AI 层 | chat / embedding / rerank / MCP server / 向量库（Milvus、Qdrant、Weaviate） |

---

## 附录 A：langchain4j 0.35.0 → 1.x

> Spring Boot 3 分支上 langchain4j 必须从 0.35.0 升到 1.x（0.x → 1.x 为破坏性升级）。
> 以下为已识别的调整项。

### A.1 knowledge 元数据 Filter：contains 下推原生支持

- **现状（0.35.0）**：`MetadataFilterBuilder` 无 `containsString`；`KbSearchService` 的
  `contains` / `not contains` 操作符使用自定义 `ContainsStringFilter`（knowledge.service 包内），
  各向量库的 filter 翻译器不识别该类型，下推会失败并自动回落「多取 3 倍 + 内存过滤」
  （语义正确，性能次优）。
- **调整（1.x）**：1.x 原生提供 `ContainsString`（`MetadataFilterBuilder.containsString(...)`），
  将 `contains` / `not contains`（`Filter.not(containsString)`）切换为原生实现，
  contains 即可随 `EmbeddingSearchRequest.filter` 真正下推到向量库；删除自定义 Filter。
- **涉及位置**：`KbSearchService.buildMetadataCondition` 与自定义 Filter 类。
- **is empty / is not empty**：两版均无原生 `IsEmpty`，继续以空串（""）近似表达，行为不变。

### A.2 KbTokenCounter：字符近似 → TokenCountEstimator

- **现状（0.35.0）**：`KbTokenCounter.estimate` 用 CJK/拉丁混合字符近似估 token，不引入 Tokenizer 词表依赖。
- **调整（1.x）**：替换为 `TokenCountEstimator`，只改 `KbTokenCounter` 单一封装点。

### A.3 流式工具调用缺口

- **现状（0.35.0）**：Ollama 流式无 tools 重载；OpenAI 兼容 `/v1` 同帧 `tool_calls` 被丢弃，
  流式场景下工具调用能力受限（`/stream` 报 response cannot be null 的已知问题）。
- **调整（1.x）**：1.x 流式 API 重构后按新版 `ChatModel` / `StreamingChatModel` 的 tools 支持重新接通。

### A.4 Flow.Publisher 流式取消

- **现状（0.35.0）**：无流式取消/底层请求 SPI，订阅取消无法透传到模型请求。
- **调整（1.x）**：langchain4j 1.20.2+ 提供流式取消 SPI 后，在流式链路实现真取消。

### A.5 检索元数据条件按请求动态传入

- **现状（0.35.0）**：AiServices 到 ContentRetriever 的运行期通道只有 Query（text + chatMemoryId），
  没有「每次请求向检索器传参」的内置 API，RAG 助手的 metadataCondition 只能构建期固定——
  每个条件组合（如每个租户）需单独构建一个 assistant 实例缓存复用，同一实例不支持每次对话变更条件
  （ThreadLocal / memoryId 编码租户 / `@V` 均已被否）。
- **调整（1.x）**：确认 1.x 是否提供按请求透传检索参数的 SPI（如 `ChatRequestParameters` 或
  AiServices 的 per-call 扩展）；若有，`SimpleAssistant.chat` 增加 per-call metadataCondition
  通道，工厂改为单实例 + 每请求条件，去掉按条件组合多实例缓存。
- **过渡备选（0.35.0 可用，暂未采用）**：`Query.metadata().chatMemoryId()` 已暴露当前会话
  memoryId，检索器可按 memoryId 查「会话 → 条件」映射动态解析（与 ChatMemoryStore 同构，
  非编码进 memoryId、非 ThreadLocal）；业务侧在会话建立时登记租户条件即可单实例服务全部会话。

---

## 附录 B：待验证项

以下依赖未能从源码静态判定，需在阶段 2 实测：

| 组件 | 现状版本 | 待确认 |
|---|---|---|
| MyBatis-Flex | 1.11.4 | 与 SB3 / mybatis 3.0.x 的兼容性 |
| mybatis-plus-join | 1.5.2 | 是否需要 SB3 专用版本 |
| Apollo Client | 2.5.0 | SB3 下的 bootstrap / 自动配置 |
| OpenTelemetry | 1.56.0 | SB3 下的 javaagent 与 SPI |
| Pulsar Client | 2.11.2 | 是否传递 javax.servlet |
| RocketMQ | 5.1.3 | 同上 |
| Mica MQTT | 2.3.9 | broker 端 servlet 依赖 |
| Debezium | 1.9.8.Final | 与 SB3 无直接关系，但需确认 `javax.*` 传递依赖 |
| Nebula / Neo4j JDBC | 自维护 | `javax.sql.rowset` 属保留包，无需迁移，但需实测 |
| EasyTrans | 1.2.6 | 与 MP 3.5.x 的兼容性 |
| Hutool | 5.8.34 | 若使用 `cn.hutool.extra.servlet` 需换 `JakartaServletUtil`（当前仓库未命中） |

验证命令：

```bash
./mvnw dependency:tree -Dverbose | grep -E "javax\.servlet|javax\.validation|javax\.persistence"
```