---
comet_change: fix-magicapi-plugin-sample-integration
role: technical-design
canonical_spec: openspec
archived-with: 2026-07-28-fix-magicapi-plugin-sample-integration
status: final
---

# Magic-API 全插件与 Sample 整体集成技术设计

## 背景

Magic-API Boot 插件聚合器当前包含 25 个插件，AI 插件聚合器包含 model、vector 2 个插件。现有 Boot sample 只显式依赖其中一部分，AI 插件又由独立 sample 承载，因此单个应用启动不能证明宿主与 27 个插件可以同时完成依赖解析、自动配置和启动。

首次补齐 classpath 后，真实 sample 启动依次暴露了 AI 自动配置元数据引用不存在类、MQTT Client resource storage 与 `MagicResourceService` 循环依赖、Apollo 显式启用绕过关闭属性等问题。默认 sample 还包含内网 Apollo、Redis、Mongo 等地址和硬编码资源路径，不能作为可重复的整体测试入口。

## 目标

- 继续使用 `fw-plus-magicapi-spring-boot-starter-sample` 作为唯一整体测试宿主。
- 同时覆盖 Boot 25 个和 AI 2 个 Magic-API 插件。
- 建立 classpath、自动配置、完整 Spring 上下文、真实 sample 进程和 HTTP 入口的离线严格验证。
- 修复整体启动实际复现的源码缺陷，并为每个缺陷保留最小 RED/GREEN 回归证据。
- 按 Core -> Boot -> Cloud / AI -> Sample 顺序形成可重复的分层构建证据。

## 非目标

- 不启动 Kafka、Pulsar、RocketMQ、RabbitMQ、Redis、Mongo、Elasticsearch、InfluxDB、MQTT、S3、Debezium 或向量数据库服务。
- 不执行依赖真实地址、凭证或业务数据的数据读写测试。
- 不升级 Magic-API 2.2.2，不修改内部 Nexus 坐标。
- 不新增或改变插件公开 API、配置键和运行时业务语义。
- 不通过允许循环依赖、延迟代理或跳过测试来绕过启动失败。

## 设计决策

采用真实 sample 主类的离线整体启动方案：

1. sample 运行时显式依赖全部 27 个插件。
2. MQTT Client storage 改为无参创建，使用 Magic-API 核心已有的统一服务回填机制。
3. sample 主类移除冗余 `@EnableApolloConfig`，Apollo 是否启用只由现有 bootstrap 属性决定。
4. 新增 `all-plugins` profile，关闭 Apollo 两条访问链并把 Magic-API 资源位置指向空的临时目录。
5. 自动测试与真实进程 smoke test 都使用真实 `MagicapiSampleApplication`，不使用绕开主类的测试宿主。
6. embedding starter 继续避免传递全部向量数据库 SDK；对直接链接可选 SDK 的 provider adapter Bean 增加 classpath 条件，缺少 SDK 时跳过对应 adapter，但保留 `EmbeddingStoreRegistry` 与 vector 插件装配。

备选方案未采用：将 Apollo 注解移动到新的条件配置类会引入第二套开关；测试专用宿主无法证明真实 sample 主类可离线启动。

## 组件与数据流

### 插件依赖清单

sample POM 显式引入 Boot 25 个和 AI 2 个插件。测试侧维护同一份规范化坐标清单，并通过结构化 POM 解析读取两个插件聚合器的 `<modules>`，确保：

- 聚合器模块数分别为 25 和 2。
- 聚合器中的每个插件都能映射到 sample 依赖。
- 每个插件 artifact 的 `META-INF/maven/<groupId>/<artifactId>/pom.properties` 可从测试运行时 classpath 读取。

这样既捕获当前依赖缺失，也避免未来新增插件后只更新聚合器而不更新 sample。

### MQTT Client 初始化

修复前的数据流：

```text
MagicAPIBaseAutoConfiguration
  -> 创建 MagicResourceService
  -> 解析全部 MagicResourceStorage
  -> 创建 MqttClientDataSourceInfoMagicResourceStorage
  -> 反向请求正在创建的 MagicResourceService
  -> BeanCurrentlyInCreationException
```

修复后的数据流：

```text
无参创建全部 MagicResourceStorage
  -> 创建 MyDefaultMagicResourceService
  -> DefaultMagicResourceService 构造器逐个执行 setMagicResourceService(this)
  -> 完成动态注册器创建
  -> ApplicationStartedEvent 后读取空资源目录
```

Magic-API 2.2.2 的 `DefaultMagicResourceService` 已同步回填全部 storage。项目的 `MyDefaultMagicResourceService` 直接调用父构造器，因此 MQTT Client 不需要在 Bean 工厂方法中重复注入服务。

### Apollo 隔离

`apollo.bootstrap.enabled=false` 只能关闭 Apollo Boot initializer 和自动配置，不能关闭 `@EnableApolloConfig` 直接导入的 `ApolloConfigRegistrar`。框架的 `ApolloConfigChangeAutoConfiguration` 还会在 `ApplicationReadyEvent` 调用 `ConfigService.getConfig`，需要独立关闭。

sample 主类移除 `@EnableApolloConfig` 后：

- 默认配置中 `apollo.bootstrap.enabled=true`，Apollo 仍按 `apollo.bootstrap.namespaces` 加载，默认 sample 语义保持不变。
- `all-plugins` profile 设置 `apollo.bootstrap.enabled=false` 和 `fw.plus.config.event.apollo-enabled=false`，两条访问链均不创建。
- 不修改生产框架的 Apollo 自动配置条件，避免影响 annotation-only 的现有使用者。

### 离线资源边界

`all-plugins` profile 使用随机端口和空的临时资源目录。现有 sample 中的 `datasource/`、`redis-datasource/`、`kafka-datasource/` 等演示资源不会被加载，因此插件 storage、module、controller 和自动配置可以联合装配，但不会创建动态外部连接。

profile 不全局禁用 27 个插件。若某个第三方客户端在没有资源定义时仍主动连接，优先使用该客户端已有的 lifecycle 属性关闭连接动作，同时通过 Bean 或条件报告断言插件配置仍被评估；不得简单从 classpath 移除插件。

## 自动配置元数据与依赖修复

整体启动已证明 AI 自动配置元数据包含不存在的 `Langchain4jOpenApiAutoConfiguration`。两套 Spring Boot 元数据必须只登记当前存在且可加载的自动配置类，并通过测试逐项执行 `Class.forName(..., false, classLoader)`。

AI 自动配置 POM 中 cassandra、infinispan、neo4j 三个内部 artifact 当前不存在可解析版本且没有对应 reactor 模块。删除这些失效的可选依赖，保留实际存在的 provider/store 依赖；不为不存在模块伪造版本或空 artifact。

真实 sample 消费 `fw-plus-ai-magicapi-plugin-vector` 时，embedding starter 按设计不传递所有具体向量 SDK，但 `EmbeddingStoreAutoConfiguration` 无条件创建 Redis、Milvus、Elasticsearch、Qdrant、Weaviate 和 Azure AI Search 等直接链接 SDK 类型的 adapter，导致 Spring 反射 Bean 时触发 `NoClassDefFoundError`。这些直接链接型 adapter 必须按其 SDK 入口类增加 `@ConditionalOnClass(name = ...)`；基于反射实现且不直接链接 SDK 类型的 adapter 保持现有注册行为。

## 测试策略

### 1. 根因回归测试

新增 MQTT Client 自动配置测试，使用真实 `MyDefaultMagicResourceService` 创建拓扑：

- 修复前因循环依赖失败，形成 RED 证据。
- 修复后上下文成功启动。
- 断言 MQTT Client storage 最终引用同一个 `MagicResourceService`，证明核心统一回填确实发生。

AI 元数据测试同时读取 `spring.factories` 和 `AutoConfiguration.imports`，断言清单中的类全部可加载。

AI embedding 自动配置增加消费侧 classpath 回归：通过 `FilteredClassLoader` 隐藏上述可选 SDK 包，要求上下文无启动失败、`EmbeddingStoreRegistry` 仍创建、缺失 SDK 对应 adapter Bean 不创建。该测试在修复前以 Milvus `Builder` 缺类形成 RED，修复后转 GREEN。

### 2. 插件清单与 classpath 契约

- 结构化解析 Boot、AI 插件聚合 POM，得到 25 + 2 模块事实源。
- 对比 sample 依赖坐标，缺失或多余项直接列出。
- 校验 27 个 `pom.properties` 均在运行时 classpath。

### 3. 完整 ApplicationContext

使用真实 `MagicapiSampleApplication`、`all-plugins` profile 和随机端口启动测试上下文：

- 断言上下文无启动失败，`MagicResourceService` 和宿主关键 Bean 存在。
- 断言全部插件配置被导入；显式条件控制的插件记录并断言条件结果。
- 断言 Apollo 事件适配器与 Apollo PropertySource 不存在。
- 断言 Magic-API 使用测试临时资源目录，不读取默认演示资源。

### 4. 分层 Maven 验证

在仓库根目录依次执行：

```bash
./fw-plus-core/mvnw -f fw-plus-core/pom.xml clean install
./fw-plus-boot/mvnw -f fw-plus-boot/pom.xml clean install
./fw-plus-cloud/mvnw -f fw-plus-cloud/pom.xml clean install
./fw-plus-ai/mvnw -f fw-plus-ai/pom.xml clean install
./fw-plus-core/mvnw -f fw-plus-sample/pom.xml clean verify
```

如 Maven Wrapper 运行时不可用，使用当前已验证的系统 Maven 3.6.3 和 JDK 8 执行等价命令，并在验证报告中记录入口差异。不得使用 `-DskipTests` 作为最终验证结果。

### 5. 真实 sample 进程验证

构建完成后，以 `all-plugins` profile 启动 sample 实际进程：

- 使用动态空闲端口和空的临时资源目录。
- 轮询 `/actuator/health`，要求 HTTP 200 且状态为 `UP`。
- 请求配置的 `/magic-api/web` 入口及其核心静态资源，要求成功响应。
- 检查日志中不存在 Apollo 内网访问、`BeanCurrentlyInCreationException`、循环依赖图和 Apollo 长轮询线程。
- 验证完成后正常终止进程并确认端口释放。

进程就绪使用有上限的轮询，不使用固定长时间 sleep。启动超时、进程提前退出或 HTTP 非预期响应都保存日志并作为失败处理。

## 失败处理

- 每个新发现的源码缺陷先建立最小可重复 RED 测试，再修改生产代码。
- 构建解析失败需区分源码 POM、本地旧 SNAPSHOT 和内部 Nexus 可用性，不把网络错误误判为源码错误。
- 外部服务不可用不是本次失败，因为 `all-plugins` 不加载外部资源；如果仍发生连接，视为启动隔离缺陷并继续定位。
- HTTP smoke test 输出响应码、响应摘要、进程状态和对应日志路径，避免只报告“启动成功”。
- 所有实现保持 JDK 8 兼容，不使用 Java 9+ API。

## 多 Agent 实施边界

实现阶段按无重叠文件域并行：

- Boot Agent：MQTT Client 回归测试与循环依赖修复。
- Sample Agent：插件清单同步测试、Apollo 主类调整、`all-plugins` profile、上下文与 HTTP 验证。
- AI Agent：失效依赖、自动配置元数据和对应回归测试复核。
- 主 Agent：审查各域改动、解决集成冲突、执行分层构建和真实 sample 进程验证。

各 Agent 不得独立放宽验收条件；跨域失败回到主 Agent 统一归因。

## 风险与控制

- 27 个插件联合 classpath 可能继续暴露自动配置顺序、重复 Bean 或第三方版本冲突。控制方式是按最小 RED 测试逐项修复，不批量排除自动配置。
- 默认 sample 配置包含真实地址。控制方式是 `all-plugins` profile 显式覆盖 Apollo 和资源位置，且上下文测试断言最终 Environment。
- 本地 SNAPSHOT 可能陈旧。控制方式是严格按分层顺序安装并记录各层构建结果。
- 全层构建耗时较长。允许先运行定向测试形成快速反馈，但最终验收必须执行完整分层验证。

## 验收标准

- Boot 25 个、AI 2 个插件与 sample 依赖清单一致，27 个 artifact 均在运行时 classpath。
- AI 两套自动配置元数据不再引用不存在的类，清单类全部可加载。
- 缺少具体向量 SDK 的消费侧 classpath 仍能创建 embedding registry，且只跳过对应 provider adapter。
- MQTT Client 最小回归测试证明无循环依赖且统一回填正确。
- 真实 `MagicapiSampleApplication` 的完整上下文在 `all-plugins` profile 下离线启动成功。
- Core、Boot、Cloud、AI、Sample 分层 Maven 测试与构建全部成功。
- 真实 sample 进程健康，Magic-API Web/宿主入口可访问，日志无 Apollo 外连和 Bean 循环，验证后端口释放。

## Spec Patch

无。本变更修复 sample 集成和既有启动缺陷，不新增 capability，不改变公开验收契约。
