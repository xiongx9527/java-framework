# fw-plus

**统一依赖、自动配置与可组合扩展，为 Spring Boot 4.1.x 项目提供完整的工程能力底座。**

<a href="#项目概览">项目概览</a> ·
<a href="#快速开始">快速开始</a> ·
<a href="#分层架构">分层架构</a> ·
<a href="#能力与模块">能力与模块</a> ·
<a href="#magic-api-资源模型">Magic-API</a> ·
<a href="#样例项目">样例项目</a> ·
<a href="#开发与维护">开发与维护</a> ·
<a href="#兼容性与限制">兼容性</a>

## 项目概览

**fw-plus** 是一个面向 Java 21 与 Spring Boot 4.1.x 的模块化 Java 开发框架。它将公共基础能力、ORM 增强、Spring Boot 自动配置、Spring Cloud 扩展、Magic-API 插件、AI 适配层、图数据库 JDBC 与样例项目组织为四个可独立构建的库层。

项目强调按需组合，而不是要求应用一次性引入所有能力：

- **统一版本**：每层提供 BOM 与 Parent，四个库层使用同一 `0.4.0-SNAPSHOT` 版本线。
- **自动配置**：Boot、Cloud 与 AI 层通过 AutoConfiguration 和 Starter 收敛集成成本。
- **插件化扩展**：Magic-API 将数据源、消息、任务、认证、文档、MCP 与低代码能力拆分为独立插件。
- **中立 AI 边界**：模型与向量存储使用中立接口，LangChain4j / Spring AI 作为显式 Adapter 接入。
- **真实样例验证**：Boot、Cloud、AI 样例集中在 `fw-plus-sample/`，通过统一 Maven 聚合入口校验。

| 层级 | Maven GroupId | 主要职责 |
| --- | --- | --- |
| [Core](./fw-plus-core/) | `com.xiongx9527.fw.plus` | 公共能力、ORM、认证、模板、图数据库、AI 基础与 BOM |
| [Boot](./fw-plus-boot/) | `com.xiongx9527.fw.plus.boot` | Spring Boot 自动配置、Starter 与 Magic-API 插件 |
| [Cloud](./fw-plus-cloud/) | `com.xiongx9527.fw.plus.cloud` | Spring Cloud 自动配置、注册中心、网关与 Cloud 插件 |
| [AI](./fw-plus-ai/) | `com.xiongx9527.fw.plus.ai` | 中立 AI Starter、Adapter、MCP 与 Magic-API AI 插件 |
| [Samples](./fw-plus-sample/) | `com.xiongx9527.fw.plus` | Boot、Cloud、AI 的集中样例与整体验证入口 |

## 快速开始

### 环境要求

- JDK 21（Core / Boot 层已落地；Cloud / AI 层升级收口中，过渡期 `<jdk.version>` 仍为 17）
- Git
- macOS、Linux 或 Windows Shell
- Apache Maven 3.8+，或直接使用仓库自带的 Maven Wrapper（根目录与四个库层均含 `mvnw` 与 `.mvn/wrapper`）

### 从源码准备本地依赖

根目录提供统一聚合 POM，可一次性构建全部层；构建产物通过 Maven 本地仓库在层间传递。如需按层构建，请按 Core -> Boot -> Cloud / AI 的顺序执行：

```bash
# 全量构建（根聚合 5 层，跳过测试）
./mvnw clean install -DskipTests

# 等价的按层构建
./mvnw -f fw-plus-core/pom.xml clean install -DskipTests
./mvnw -f fw-plus-boot/pom.xml clean install -DskipTests
./mvnw -f fw-plus-cloud/pom.xml clean install -DskipTests   # 按需，或
./mvnw -f fw-plus-ai/pom.xml clean install -DskipTests      # 按需，可与 Cloud 独立执行

# 校验集中样例聚合器
./mvnw -f fw-plus-sample/pom.xml validate
```

`Cloud` 和 `AI` 都继承 Boot 层父 POM，因此必须先完成 Core 与 Boot 的本地安装；完成 Boot 后，两层可按实际需要独立构建。

## 分层架构

### Core

Core 是全部上层的基础，负责公共抽象、领域增强和依赖版本管理。

- `fw-plus-dependencies`：Core 层 BOM。
- `fw-plus-parent`：Core 层父 POM。
- `fw-plus-common`、`fw-plus-annotation`：公共工具、异常、序列化、自定义注解。
- `fw-plus-mybatis`、`fw-plus-jdbc`、`fw-plus-nosql`：数据访问与存储增强。
- `fw-plus-easytrans`、`fw-plus-poi`、`fw-plus-template`：翻译、Office 与模板。
- `fw-plus-auth`、`fw-plus-graph`：认证扩展与图数据库 JDBC。
- `fw-plus-ai`、`fw-plus-dify`：AI 中立接口、LangChain4j / Spring AI、MCP 与 Dify 客户端。
- `fw-plus-magicapi`：Magic-API Core 扩展。

### Boot

Boot 在 Core 之上提供 Spring Boot 4.1.x 自动配置。`fw-plus-boot-autoconfigure` 汇集核心自动配置，`fw-plus-boot-starters` 提供可按需引入的 Starter 和 Magic-API 插件。

### Cloud

Cloud 在 Boot 之上提供 Spring Cloud 2025.1（Oakwood 线）扩展，包括 Cloud 通用能力、自动配置、Eureka 服务端、灰度负载均衡与 Magic-API Cloud 插件。

### AI

AI 层将模型、向量存储与具体框架实现解耦，基础能力在 Core 的 `fw-plus-ai`（Base、MCP Server、LangChain4j、Spring AI），自动配置与 Starter 位于 AI 层。当前适配以 LangChain4j 与 Spring AI 为主，并提供 MCP Server 与 LangChain4j 两组 Magic-API AI 插件，样例中另有 AgentScope 集成示例。

## 能力与模块

### Core 模块

| 模块 | 子模块或能力 |
| --- | --- |
| `fw-plus-common` | 加密、UID、异常、序列化、Spring 工具等公共能力 |
| `fw-plus-annotation` | `@Algorithm`、`@FieldEncrypt` 等注解 |
| `fw-plus-mybatis` | 聚合 MyBatis-Plus / MyBatis-Flex 增强与通用 DataChange、DataScope、Encrypt |
| `fw-plus-mp` | Base、Join、DataChange、DataScope、Encrypt |
| `fw-plus-mf` | MyBatis-Flex Base、DataChange、DataScope、Encrypt |
| `fw-plus-easytrans` | Annotation、Common、Service 字典翻译扩展 |
| `fw-plus-poi` | EasyPoi Excel / Word 增强（内嵌 `cn.afterturn` fork） |
| `fw-plus-jdbc` | JDBC 工具与数据库扩展基础 |
| `fw-plus-nosql` | Redis 增强 |
| `fw-plus-template` | FreeMarker、docx4j、OpenPDF 等模板能力 |
| `fw-plus-auth` | Auth Base、Auth Core 与 Sa-Token 扩展 |
| `fw-plus-graph` | Graph JDBC 公共层、Nebula JDBC、Neo4j JDBC |
| `fw-plus-ai` | Base、MCP Server、LangChain4j（Nebula / Neo4j）、Spring AI |
| `fw-plus-dify` | Dify API 客户端 |
| `fw-plus-magicapi` | Magic-API Core 扩展 |

### Boot Starter

| 分类 | Starter |
| --- | --- |
| 聚合 | `fw-plus-boot-starter` |
| 数据访问 | `fw-plus-mate-spring-boot-starter`、`fw-plus-mp-spring-boot-starter`、`fw-plus-jdbc-spring-boot-starter`、`fw-plus-redis-spring-boot-starter`、`fw-plus-influxdb-spring-boot-starter` |
| 平台 | `fw-plus-magicapi-spring-boot-starter` |
| 基础设施 | `fw-plus-file-storage-spring-boot-starter`、`fw-plus-mqtt-server-spring-boot-starter`、`fw-plus-mqtt-client-spring-boot-starter`、`fw-plus-admin-server-spring-boot-starter` |
| 实时通信 | `fw-plus-ws-spring-boot-starter` |
| 文档与模板 | `fw-plus-template-spring-boot-starter` |

<details>
<summary><strong>Boot Magic-API 插件完整清单（28 个）</strong></summary>

| 分类 | 插件模块 |
| --- | --- |
| 平台与控制台 | `config`、`vue3`、`admin`、`swagger`、`spring-bean` |
| 数据访问与存储 | `druid`、`db`、`redis`、`mongo`、`s3`、`influx`、`elasticsearch`、`mybatisplus`、`mybatisflex` |
| 消息与实时通信 | `rocketmq`、`rabbitmq`、`kafka`、`pulsar`、`mqtt-server`、`mqtt-client`、`ws` |
| 运行时扩展 | `freemarker`、`task`、`dtp`、`debezium`、`satoken`、`word`、`java-entity` |

完整 artifactId 使用 `fw-plus-magicapi-plugin-` 前缀，例如 `fw-plus-magicapi-plugin-kafka`。

</details>

### Cloud Starter 与插件

| 类型 | 模块 |
| --- | --- |
| 聚合 | `fw-plus-cloud-starter` |
| 运维 | `fw-plus-admin-server-spring-cloud-starter` |
| 注册中心 | `fw-plus-eureka-server-spring-cloud-starter` |
| Cloud 插件 | `fw-plus-cloud-magicapi-plugin`（`fw-plus-cloud-magicapi-plugin-eureka`） |

### AI Starter 与插件

| 类型 | 模块 |
| --- | --- |
| 聚合 | `fw-plus-ai-boot-starter` |
| MCP | `fw-plus-ai-mcp-server-spring-boot-starter` |
| Magic-API 插件 | `fw-plus-ai-magicapi-plugin-mcp-server`、`fw-plus-ai-magicapi-plugin-langchain4j`（9 个子模块：chat / streaming-chat / language / streaming-language、embedding-model / embedding-store、tokenizer、scoring-model、skill） |

## Magic-API 资源模型

运行环境可从外部 `resources/` 仓库或挂载目录动态加载资源。**当前工作区没有根目录 `resources/`，因此这里只描述运行时约定，不提供可直接修改的业务资源。**

| 资源类型 | 目录 | 格式与用途 | 变更生效方式 |
| --- | --- | --- | --- |
| 数据源 | `datasource/`、`kafka-datasource/`、`pulsar-datasource/`、`redis-datasource/`、`s3-datasource/`、`ws-datasource/`、`dtp-datasource/`、`debezium-datasource/` | JSON 连接定义 | 通常需要重启或配置刷新 |
| 配置中心 | `config/` | `.ms` 头部之后保存分层 YAML | 通常需要重启或配置刷新 |
| 接口与函数 | `api/`、`function/` | JSON 头部 + Magic Script | 运行时动态加载 |
| 订阅与任务 | `kafka-subscribe/`、`pulsar-subscribe/`、`task/`、`ws-subscribe/` | JSON 头部 + Magic Script | 运行时动态加载 |
| 模板 | `freemarker/` | JSON 头部 + FreeMarker FTL | 由模板模块加载与渲染 |

`.ms` 资源统一使用分隔线区分元数据和内容：

```text
{ JSON 元数据 }
================================
Magic Script、YAML 或 FreeMarker 内容
```

连接配置应通过环境变量或受控配置系统注入敏感值，不要在资源文件、README 或日志中提交真实密码、Token、Access Key 或 Secret Key。

## 样例项目

### Boot Samples

| 样例 | 目的 |
| --- | --- |
| [`fw-plus-jdbc-spring-boot-starter-sample`](./fw-plus-sample/fw-plus-boot-samples/fw-plus-jdbc-spring-boot-starter-sample/) | JDBC Starter |
| [`fw-plus-magicapi-spring-boot-starter-sample`](./fw-plus-sample/fw-plus-boot-samples/fw-plus-magicapi-spring-boot-starter-sample/) | Magic-API 主引擎与插件 |
| [`fw-plus-mate-spring-boot-starter-sample`](./fw-plus-sample/fw-plus-boot-samples/fw-plus-mate-spring-boot-starter-sample/) | Mate 租户与数据权限 |
| [`fw-plus-mp-spring-boot-starter-sample`](./fw-plus-sample/fw-plus-boot-samples/fw-plus-mp-spring-boot-starter-sample/) | MyBatis-Plus 增强 |
| [`fw-plus-redis-spring-boot-starter-sample`](./fw-plus-sample/fw-plus-boot-samples/fw-plus-redis-spring-boot-starter-sample/) | Redis Starter |
| [`fw-plus-ws-spring-boot-starter-sample`](./fw-plus-sample/fw-plus-boot-samples/fw-plus-ws-spring-boot-starter-sample/) | WebSocket Starter |

### Cloud Samples

| 样例 | 目的 |
| --- | --- |
| [`eureka-server-sample`](./fw-plus-sample/fw-plus-cloud-samples/eureka-server-sample/) | Eureka 注册中心 |
| [`eureka-gateway-sample`](./fw-plus-sample/fw-plus-cloud-samples/eureka-gateway-sample/) | Eureka 网关 |
| [`eureka-service-sample`](./fw-plus-sample/fw-plus-cloud-samples/eureka-service-sample/) | Eureka 服务实例 |
| [`nacos-gateway-sample`](./fw-plus-sample/fw-plus-cloud-samples/nacos-gateway-sample/) | Nacos 网关 |

### AI Samples

| 样例 | 目的 |
| --- | --- |
| [`fw-plus-ai-magicapi-sample`](./fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-magicapi-sample/) | AI Starter 与 Magic-API AI 插件集成 |
| [`fw-plus-ai-mcp-server-sample`](./fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-mcp-server-sample/) | MCP Server 集成 |
| [`fw-plus-ai-chat-sample`](./fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-chat-sample/) | AI 对话 |
| [`fw-plus-ai-agentscope-sample`](./fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-agentscope-sample/) | AgentScope 集成 |
| [`fw-plus-ai-nl2sql-sample`](./fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-nl2sql-sample/) | NL2SQL |
| [`fw-plus-ai-structured-output-sample`](./fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-structured-output-sample/) | 结构化输出 |

## 开发与维护

### 仓库结构

```text
java-framework/
├── fw-plus-core/       # Core 库层，基础能力与 BOM
├── fw-plus-boot/       # Spring Boot 自动配置与 Starter
├── fw-plus-cloud/      # Spring Cloud 扩展
├── fw-plus-ai/         # AI 集成层
├── fw-plus-sample/     # 集中样例，无独立 Maven Wrapper
├── docs/               # 设计、计划、迁移与验证文档（含 sql/、openspec/、comet/、superpowers/）
├── CONTEXT.md          # 领域术语表
└── README.md
```

根目录 `pom.xml` 聚合这五个层，可用根 `./mvnw` 一次构建。四个库层是同一 Git 仓库中的顶层 Maven 聚合目录，不是 Git Submodule。

### 构建与测试

```bash
# 全量构建（跳过测试）
./mvnw clean install -DskipTests

# Core 全量测试
./mvnw -f fw-plus-core/pom.xml clean test

# 构建 Core 中的指定模块及其依赖
./mvnw -f fw-plus-core/pom.xml \
  clean install -pl fw-plus-project/fw-plus-common -am -DskipTests

# Boot / Cloud / AI 默认跳过测试，需显式开启
./mvnw -f fw-plus-boot/pom.xml clean test -Dmaven.test.skip=false
```

- 单元测试位于 `src/test/java/`，命名为 `*Test.java`。
- 集成测试命名为 `*IT.java`。
- Cobertura HTML/XML 报告位于对应模块的 `target/site/cobertura/`。
- Java 源码和资源统一使用 UTF-8。
- 编译目标为 Java 21（Core / Boot 已落地；Cloud / AI 层过渡期仍为 17），源码已全面 jakarta 化（`jakarta.servlet.*`、`jakarta.validation.*`）。

### 版本与发布模型

| 范围 | 版本方式 | Flatten |
| --- | --- | --- |
| Core / Boot / Cloud / AI | `${revision}`，当前为 `0.4.0-SNAPSHOT` | 使用 `flatten-maven-plugin` |
| Sample | 固定 `0.4.0-SNAPSHOT` | 不使用 Flatten |

四个库层按同一版本号发布，不单独发布某个层的不同版本。层间构建产物通过本地 Maven 仓库或制品仓库传递。

## 兼容性与限制

| 项目 | 当前基线 |
| --- | --- |
| Java | 21（Core / Boot 已落地；Cloud / AI 过渡期 17） |
| Spring Boot | 4.1.1 |
| Spring Cloud | 2025.1.3（Oakwood） |
| Spring Cloud Alibaba | 2025.1.0.0 |
| Magic-API | 2.2.2，自行维护版本（jakarta 线） |
| Magic Script | 1.9.0 |
| MyBatis-Plus | 3.5.17（boot4 坐标） |
| MyBatis-Flex | 1.11.8（boot4 坐标） |
| LangChain4j | 1.22.0 |
| Spring AI | 2.0.1 |
| AgentScope | 2.0.4 |
| Sa-Token | 1.46.0（boot4 坐标） |

使用前请注意：

- 仓库当前处于 `springboot4` 分支（JDK 17 + Spring Boot 3.5.8 → JDK 21 + Spring Boot 4.1.x 迁移线），Core 与 Boot 层已完成升级，Cloud / AI / Sample 层收口中。迁移版本矩阵与待办见 [Spring Boot 4 升级说明](./docs/springboot4-upgrade-notes.md)。
- Boot 4 管线默认使用 **Jackson 3**（`tools.jackson`）；`com.fasterxml.jackson`（Jackson 2）仅在 magic-api fork 等兼容场景保留，新代码优先使用 Jackson 3 命名空间。
- 代码已切换到 `jakarta.*` 命名空间，不要再引入 `javax.servlet` / `javax.validation`；`javax.sql` / `javax.xml` / `javax.tools` 等属保留包，无需迁移。
- Magic-API 使用自行维护的 2.2.2（jakarta）版本，请勿自行替换为 Maven Central 中的其他版本。
- 部分依赖通过项目 POM 声明的非中央仓库解析，外部环境需要具备对应网络和仓库访问能力。
- 运行样例可能需要 MySQL、Redis、Kafka、Pulsar、MQTT、Elasticsearch、对象存储或向量数据库等外部中间件；请按样例实际依赖准备。
- AI 模块中不同 Provider SDK 可能有额外的运行环境要求，选择实现前应核对具体 artifact，而不是只依据聚合层的编译目标。
- 当前工作区没有运行态 `resources/`，Magic-API 业务资源需要单独挂载或检出。

## 贡献

提交变更前请保持模块边界、Java 21 兼容性（Cloud / AI 层过渡期仍为 Java 17）、jakarta 命名空间约束和现有代码风格，并运行受影响层的测试。

- [提交 Issue](https://github.com/xiongx9527/java-framework/issues)
- [创建 Pull Request](https://github.com/xiongx9527/java-framework/pulls)

## 许可证

四个库层均声明 [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)：

- [Core License](./fw-plus-core/LICENSE)
- [Boot License](./fw-plus-boot/LICENSE)
- [Cloud License](./fw-plus-cloud/LICENSE)
- [AI License](./fw-plus-ai/LICENSE)
