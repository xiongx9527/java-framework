# Repository Guidelines

## Communication

请始终使用简体中文与我对话，并在回答时保持专业、简洁。包括：
- 所有的回答文本
- 所有的选项（如 AskUserQuestion 的选项、菜单选择等）
- 所有的推理过程和中间输出

## 项目概述

fw-plus 是 9527 的 Java 开发框架，基于 Spring Boot 2.7.18，提供 Spring Boot 自动化配置、Spring Cloud 扩展、MyBatis-Plus/Flex 增强、Magic-API 插件、AI（LangChain4j / Agents-Flex / Spring AI / EasyAi）集成等能力。使用 Maven 构建，当前版本 `0.1.1-SNAPSHOT`（通过 `${revision}` + `flatten-maven-plugin` 管理）。

**Git 仓库结构**：根目录和各模块（`fw-plus-core`、`fw-plus-boot`、`fw-plus-cloud`、`fw-plus-ai`、`fw-plus-product`、`resources`）各自拥有独立的 `.git` 仓库，互为平级目录，非 git submodule。构建产物在各仓库间通过 Maven 本地仓库（`~/.m2`）传递，不依赖 SNAPSHOT 的自动更新机制。

各层 GroupId：
- Core: `com.xx9527.fw.plus`
- Boot: `com.xx9527.fw.plus.boot`
- Cloud: `com.xx9527.fw.plus.cloud`
- AI: `com.xx9527.fw.plus.ai`
- Product: `com.xx9527.fw.plus.product`

## Module Structure

Each module follows the same internal layout:
- `fw-plus-{name}-project/` — the actual library code
- `fw-plus-{name}-samples/` — sample/example usage

All source code is under `src/main/java`; resources under `src/main/resources`. Source encoding is UTF-8.

```
fw-plus/                      # 根目录（聚合项目，无统一父 POM）
├── fw-plus-core/             # 核心库层（Java 1.8, Spring Boot 2.7.18）
│   ├── fw-plus-project/
│   │   ├── fw-plus-parent/       # Core 层父 POM
│   │   ├── fw-plus-common/       # 公共工具：加密、UID、异常、序列化、SpringUtils
│   │   ├── fw-plus-annotation/   # 自定义注解：@Algorithm, @FieldEncrypt
│   │   ├── fw-plus-mp/           # MyBatis-Plus 增强
│   │   │   ├── fw-plus-mp-base/       # MP 基础（base mapper/entity）
│   │   │   ├── fw-plus-mp-datachange/ # 数据变更追踪
│   │   │   ├── fw-plus-mp-datascope/  # 数据权限过滤
│   │   │   ├── fw-plus-mp-encrypt/    # 字段加密（@FieldEncrypt）
│   │   │   └── fw-plus-mp-join/       # MP 连表查询
│   │   ├── fw-plus-mf/           # MyBatis-Flex 增强
│   │   │   └── fw-plus-mf-base/       # MF 基础增强
│   │   ├── fw-plus-easytrans/    # Easy-Trans 字典翻译扩展
│   │   │   ├── fw-plus-easytrans-annotation/ # 翻译注解
│   │   │   ├── fw-plus-easytrans-common/     # 翻译公共类
│   │   │   └── fw-plus-easytrans-service/    # 翻译服务实现
│   │   ├── fw-plus-magicapi/     # Magic-API 扩展（Mongo, Redis, ES, Nebula 等）
│   │   ├── fw-plus-ai/           # AI 基础模块
│   │   │   ├── fw-plus-ai-base/       # AI 基础（向量存储、向量数据库客户端等）
│   │   │   └── fw-plus-ai-mcp/        # MCP 协议支持
│   │   ├── fw-plus-javaparser/   # 动态类加载器、JavaParser
│   │   ├── fw-plus-template/     # 模板引擎：Freemarker, docx4j, OpenPDF, FO
│   │   ├── fw-plus-poi/          # EasyPoi 扩展
│   │   │   └── fw-plus-poi-easypoi/   # EasyPoi Excel/Word 操作
│   │   ├── fw-plus-report/       # JimuReport 扩展
│   │   │   ├── fw-plus-report-base/   # 报表基础抽象
│   │   │   └── fw-plus-report-jimu/   # JimuReport 集成 + org.jeecg 破解类
│   │   ├── fw-plus-jdbc/         # JDBC 工具
│   │   ├── fw-plus-nosql/        # 非关系型数据库增强
│   │   │   └── fw-plus-redis/         # Redis 增强
│   │   ├── fw-plus-auth/         # 认证相关
│   │   │   ├── fw-plus-auth-base/     # 认证基础
│   │   │   └── fw-plus-auth-satoken/  # Sa-Token 认证扩展
│   │   ├── fw-plus-nebula/       # Nebula Graph
│   │   │   └── fw-plus-nebula-jdbc/   # Nebula Graph JDBC 驱动
│   │   ├── fw-plus-dify/         # Dify API 客户端
│   │   └── fw-plus-dependencies/ # 统一版本管理（BOM）
│   └── fw-plus-samples/          # Core 层示例
│
├── fw-plus-boot/             # Spring Boot 自动配置层
│   ├── fw-plus-boot-project/
│   │   ├── fw-plus-boot/              # 集成自动配置（aggregator）
│   │   ├── fw-plus-boot-autoconfigure/ # 核心自动配置（19 个 AutoConfiguration，含 JimuJsEnterpriseFilter）
│   │   ├── fw-plus-boot-common/       # Boot 公共类
│   │   ├── fw-plus-boot-dependencies/ # Boot 层 BOM
│   │   ├── fw-plus-boot-parent/       # Boot 层父 POM
│   │   └── fw-plus-boot-starters/     # 各种 Spring Boot Starter（15 个）
│   │       ├── fw-plus-admin-server-spring-boot-starter/  # Spring Boot Admin 监控
│   │       ├── fw-plus-boot-starter/                      # 通用 Boot Starter（aggregator）
│   │       ├── fw-plus-file-storage-spring-boot-starter/  # 文件存储（Minio/S3）
│   │       ├── fw-plus-influxdb-spring-boot-starter/      # InfluxDB 时序数据库
│   │       ├── fw-plus-jdbc-spring-boot-starter/          # JDBC 数据库
│   │       ├── fw-plus-jimu-spring-boot-starter/          # JimuReport 积木报表
│   │       ├── fw-plus-magicapi-spring-boot-starter/      # Magic-API 主引擎
│   │       ├── fw-plus-magicapi-plugin/                   # Magic-API 插件（22 个）
│   │       │   ├── fw-plus-magicapi-plugin-config/        # 配置中心
│   │       │   ├── fw-plus-magicapi-plugin-debezium/      # Debezium CDC
│   │       │   ├── fw-plus-magicapi-plugin-dtp/           # 动态线程池
│   │       │   ├── fw-plus-magicapi-plugin-elasticsearch/ # Elasticsearch
│   │       │   ├── fw-plus-magicapi-plugin-freemarker/    # Freemarker 模板
│   │       │   ├── fw-plus-magicapi-plugin-influx/        # InfluxDB
│   │       │   ├── fw-plus-magicapi-plugin-word/          # Word 文档操作
│   │       │   ├── fw-plus-magicapi-plugin-kafka/         # Kafka 消息队列
│   │       │   ├── fw-plus-magicapi-plugin-mongo/         # MongoDB
│   │       │   ├── fw-plus-magicapi-plugin-mqtt/          # MQTT 协议支持
│   │       │   ├── fw-plus-magicapi-plugin-mqttclient/    # MQTT 客户端
│   │       │   ├── fw-plus-magicapi-plugin-pulsar/        # Pulsar 消息队列
│   │       │   ├── fw-plus-magicapi-plugin-rabbitmq/      # RabbitMQ 消息队列
│   │       │   ├── fw-plus-magicapi-plugin-redis/         # Redis 缓存
│   │       │   ├── fw-plus-magicapi-plugin-rocketmq/      # RocketMQ 消息队列
│   │       │   ├── fw-plus-magicapi-plugin-s3/            # S3 对象存储
│   │       │   ├── fw-plus-magicapi-plugin-satoken/       # Sa-Token 认证鉴权
│   │       │   ├── fw-plus-magicapi-plugin-sse/           # Server-Sent Events
│   │       │   ├── fw-plus-magicapi-plugin-swagger/       # Swagger API 文档
│   │       │   ├── fw-plus-magicapi-plugin-task/          # 定时任务
│   │       │   ├── fw-plus-magicapi-plugin-vue3/          # Vue3 前端资源
│   │       │   └── fw-plus-magicapi-plugin-ws/            # WebSocket
│   │       ├── fw-plus-mate-spring-boot-starter/          # Mate 租户/数据权限
│   │       ├── fw-plus-mp-spring-boot-starter/            # MyBatis-Plus 增强
│   │       ├── fw-plus-mqtt-client-spring-boot-starter/   # MQTT 客户端
│   │       ├── fw-plus-mqtt-server-spring-boot-starter/   # MQTT 服务端
│   │       ├── fw-plus-redis-spring-boot-starter/         # Redis 缓存
│   │       ├── fw-plus-template-spring-boot-starter/      # 模板引擎
│   │       └── fw-plus-uid-spring-boot-starter/           # 分布式 UID
│   └── fw-plus-boot-samples/  # Boot 示例项目（5 个）
│       ├── fw-plus-jdbc-spring-boot-starter-sample
│       ├── fw-plus-magicapi-spring-boot-starter-sample
│       ├── fw-plus-mate-spring-boot-starter-sample
│       ├── fw-plus-mp-spring-boot-starter-sample
│       └── fw-plus-redis-spring-boot-starter-sample
│
├── fw-plus-cloud/            # Spring Cloud 扩展层
│   ├── fw-plus-cloud-project/
│   │   ├── fw-plus-cloud/              # Cloud 集成（aggregator）
│   │   ├── fw-plus-cloud-autoconfigure/ # 自动配置（gray release, eureka, magic-api）
│   │   ├── fw-plus-cloud-common/       # Cloud 公共类
│   │   ├── fw-plus-cloud-dependencies/ # Cloud 层 BOM
│   │   ├── fw-plus-cloud-parent/       # Cloud 层父 POM
│   │   └── fw-plus-cloud-starters/     # Cloud Starter（4 个）
│   │       ├── fw-plus-admin-server-spring-cloud-starter/  # Spring Boot Admin 监控
│   │       ├── fw-plus-cloud-starter/                      # 通用 Cloud Starter（aggregator）
│   │       ├── fw-plus-eureka-server-spring-cloud-starter/ # Eureka 注册中心
│   │       └── fw-plus-magicapi-spring-cloud-starter/      # Magic-API Cloud 集成
│   └── fw-plus-cloud-samples/        # Cloud 示例项目（5 个）
│       ├── eureka-gateway-sample
│       ├── eureka-server-sample
│       ├── eureka-service-sample
│       ├── nacos-gateway-sample
│       └── nacos-server-sample
│
├── fw-plus-ai/               # AI 集成层
│   ├── fw-plus-ai-project/
│   │   ├── fw-plus-ai-boot/               # AI 集成（aggregator）
│   │   ├── fw-plus-ai-boot-autoconfigure/ # AI 自动配置（LangChain4j, Agents-Flex, Spring AI, EasyAi）
│   │   ├── fw-plus-ai-boot-common/        # AI 公共类
│   │   ├── fw-plus-ai-boot-dependencies/  # AI 层 BOM
│   │   ├── fw-plus-ai-boot-parent/        # AI 层父 POM
│   │   └── fw-plus-ai-boot-starters/      # AI Starter
│   │       ├── fw-plus-ai-boot-starter/                           # 通用 AI Starter（aggregator）
│   │       ├── fw-plus-ai-langchain4j-spring-boot-starter/        # LangChain4j 集成
│   │       ├── fw-plus-ai-langchain4j-open-ai-spring-boot-starter/ # LangChain4j OpenAI
│   │       └── fw-plus-ai-magicapi-plugin/                        # Magic-API AI 插件（4 个）
│   │           ├── fw-plus-ai-magicapi-plugin-langchain4j/ # LangChain4j 插件
│   │           ├── fw-plus-ai-magicapi-plugin-milvus/      # Milvus 向量数据库
│   │           ├── fw-plus-ai-magicapi-plugin-qdrant/      # Qdrant 向量数据库
│   │           └── fw-plus-ai-magicapi-plugin-weaviate/    # Weaviate 向量数据库
│   └── fw-plus-ai-samples/              # AI 示例项目
│       └── fw-plus-ai-magicapi-sample    # Magic-API AI 示例
│
├── fw-plus-product/          # 产品层（主要开发工作区）
│   ├── fw-plus-product-common/             # 产品公共代码（含 SaTokenConfig 等）
│   ├── fw-plus-product-dependencies/       # 产品依赖版本 BOM
│   ├── fw-plus-product-parent/             # 产品 POM 父工程
│   └── fw-plus-product-report/             # 报表产品（含 3 个独立 Spring Boot 应用）
│       ├── fw-plus-product-report-api      # 报表 API 服务（端口 3081，MagicAPI + 拦截器 + SQL 初始化）
│       ├── fw-plus-product-report-gateway  # 报表网关服务（端口 3080，Sa-Token OAuth2 认证）
│       ├── fw-plus-product-report-jimu     # 积木报表 Web 服务（端口 3082，报表入口 + 业务配置）
│       └── web/                            # 前端控制台
│           └── magic-boot-naive/           # NaiveUI 前端（Vue3 + Vite）
```

## Magic-API 资源目录详解

`resources/` 目录下的每个子目录对应一种 Magic-API 资源类型，由对应的插件提供存储和动态注册能力。每个资源类型的文件格式、脚本语言各有不同。

### 配置类（纯 JSON，定义连接信息）

这些目录只存放静态连接配置，不包含可执行逻辑。`*-datasource` 全部属于此类：

**`datasource/`** — JDBC 数据库连接
```json
{
  "name": "评估", "key": "evaluation",
  "url": "jdbc:mysql://172.16.1.89:3306/biz_evaluation?allowMultiQueries=true&useSSL=false&...",
  "username": "em", "password": "...",
  "driverClassName": "com.mysql.cj.jdbc.Driver",
  "type": "com.zaxxer.hikari.HikariDataSource"
}
// 脚本中使用：db['evaluation'].select('select * from user')
```

**`kafka-datasource/`** — Kafka 集群连接
```json
// kafka-datasource/kangyang.json
{ "name": "kangyang", "key": "kangyang", "servers": "172.16.2.192:9092" }
// 脚本中使用：import kafka; kafka['kangyang'].send("topic_name", messageObj)
```

**`pulsar-datasource/`** — Pulsar 集群连接
```json
// pulsar-datasource/评估.json
{ "name": "评估", "key": "evaluation", "servers": "172.16.1.89:6650",
  "tenant": "juyouqi", "namespace": "operation" }
// 脚本中使用：import pulsar; pulsar['evaluation'].send("topic_name", messageBody)
```

**`redis-datasource/`** — Redis 连接
```json
// redis-datasource/curaos.json
{ "name": "curaos", "key": "curaos", "hostname": "172.16.1.89", "port": 6379,
  "password": "...", "database": 3, "clientType": "lettuce", "mode": "standalone" }
// 脚本中使用：import redis; redis.setex('user:token:123', 3600, token); redis.get('key')
```

**`s3-datasource/`** — 对象存储连接
```json
// s3-datasource/评估.json
{ "name": "评估", "key": "evaluation", "platform": "aliyun-oss",
  "accessKey": "...", "secretKey": "...",
  "endPoint": "oss-cn-beijing.aliyuncs.com", "bucketName": "jbptest", "basePath": "dev/" }
// 脚本中使用：import s3;
// var fileStorageService = s3['evaluation'].getFileStorageService();
// fileStorageService.upload(...) / fileStorageService.generatePresignedUrlByUrl(url) / fileStorageService.delete(url)
```

**`sse-datasource/`** — SSE 服务端连接
```json
{ "name": "xxx", "key": "xxx" }
// import sse;  sse['xxx'].broadcastMessageToClient(clientId, eventName, msg)
```

**`ws-datasource/`** — WebSocket 服务端连接
```json
{ "name": "xxx", "key": "xxx" }
// import ws;  ws['xxx'].broadcastMessageToClient(clientId, msg)
```

**`dtp-datasource/`** — 动态线程池连接
```json
{ "name": "xxx", "key": "xxx" }
// import dtp;  dtp.execute(() -> { 异步任务 });
```

**`debezium-datasource/`** — Debezium CDC 连接（纯底层 CDC 数据源，无脚本侧 API，仅用于配置数据变更监听）

### 配置中心（YAML）

**`config/`** — 轻量配置中心，`.ms` 文件中 `================================` 下方是 **YAML 格式**的层级化配置，按 `magic.api.{类型}.{key}.{字段}` 管理各组件的连接参数，值可按 Spring profile 区分：

### 代码类（`.ms` 脚本，magic-script 语言）

每个 `.ms` 文件由 **JSON 头部** + `================================` + **脚本代码** 组成，包含可执行逻辑：

**`api/`** — REST API 接口，JSON 头部定义 `path`（URL）、`method`（GET/POST）、`options`（权限/校验），脚本处理请求并返回数据。

**`function/`** — 可复用函数，可被其他脚本通过 `import` 引用。

**`kafka-subscribe/`** — Kafka 消息消费，JSON 头部定义 `key`（数据源）、`topic`（主题）、`group`（消费组），收到消息时脚本自动执行。

**`pulsar-subscribe/`** — Pulsar 消息消费，通过 `new String(msg.getValue())` 获取消息内容。

**`task/`** — 定时任务，JSON 头部定义 `cron` 表达式，按 Cron 周期自动执行。

**`sse-subscribe/`** 和 **`ws-subscribe/`** — SSE / WebSocket 事件处理，结构同上。

### 特殊代码（非 magic-script 语言）

**`freemarker/`** — FreeMarker 模板语法（FTL），`.ms` 文件中 `================================` 下方是 FTL 代码，由 `template` 模块调用引擎渲染生成 Word、PDF、HTML 等文档。

## 关键技术栈

| 类别 | 技术 |
|------|------|
| 基础框架 | Spring Boot 2.7.18, Spring Cloud 2021.0.9 |
| ORM | MyBatis-Plus 3.3.2, MyBatis-Flex 1.11.4 |
| 脚本引擎 | Magic-API 2.2.2（自行维护版本，非中央仓库版本） |
| AI | LangChain4j 0.35.0, Agents-Flex 1.3.4, Spring AI 1.0.1, EasyAi 1.4.9 |
| 消息队列 | Kafka, Pulsar, RocketMQ, RabbitMQ, MQTT |
| 数据库/存储 | MySQL, InfluxDB, Redis, Elasticsearch, Nebula Graph, Minio/S3 |
| 模板引擎 | Freemarker, docx4j, OpenPDF |
| 认证鉴权 | Sa-Token 1.40.0, OAuth2 |
| 前端 | Vue3, Vite, NaiveUI |

## 重要提醒

- **Maven 构建**：使用各模块下的 `mvnw`（Maven Wrapper）而非系统 `mvn`。按依赖顺序构建：fw-plus-core → fw-plus-boot → fw-plus-cloud → fw-plus-ai → fw-plus-product。fw-plus-core 是底层依赖，各层级均有独立的父 POM。发版按版本号统一发，不单独发某个模块。
- **Magic-API 版本**：使用自行维护的 2.2.2 版本，`org.ssssssss` 组的依赖通过 `<repositories>` 指向内部 Nexus 仓库（jugui-snapshot / jugui-release），请勿修改这些依赖的版本号。
- **JDK 1.8 约束**：fw-plus-core 严格约束为 Java 1.8（compiler source/target = 1.8），不要引入 Java 9+ 的 API（如 `List.of()`、`Map.copyOf()`、`Optional.orElseThrow()` 等）。
- **源文件编码**：所有 Java 源文件、资源文件统一使用 UTF-8 编码。
- **文件路径**：不构造硬编码的跨平台文件路径，系统路径使用 `System.getProperty()` 或 `Path` API，资源路径使用 Spring `ResourceLoader` 或 `ClassPathResource`。
- **自定义类**：项目中包含大量自定义类和注解（如 `Algorithm`、`FieldEncrypt` 等），修改相关代码时确保充分理解其用途。
- **resources/**：该目录由 Magic-API 引擎动态加载（独立 git 仓库，不在当前工作区），修改 api/function/task 等脚本目录后即时生效，无需重启服务；修改 datasource/config 等配置目录后需要重启服务或触发配置刷新。

## Build, Test, and Development Commands

```bash
# Build the entire project (skip tests for speed during development)
./mvnw clean install -DskipTests

# Build a specific module
./mvnw clean install -pl fw-plus-core -DskipTests

# Run tests (Cobertura coverage report in target/site/cobertura/)
./mvnw clean test

# Generate site report including coverage
./mvnw clean site

# Deploy to the internal Nexus repository (jugui-snapshot / jugui-release)
./mvnw clean deploy -DskipTests
```

The Maven wrapper (`mvnw`) is included in each top-level module — use it instead of a system `mvn` for reproducible builds.

## Coding Style & Naming Conventions

- **Java**: Target JDK 1.8. Code style follows standard Java conventions.
- **Indentation**: Tabs (shown in existing `pom.xml` files). Be consistent with the surrounding file.
- **Module naming**: kebab-case — `fw-plus-core`, `fw-plus-boot`, `fw-plus-mp`.
- **Package naming**: `com.xx9527.fw.plus.*` — e.g., `com.xx9527.fw.plus.mp`, `com.xx9527.fw.plus.common`.
- **POM properties**: Use `${revision}` as the version property (flatten-maven-plugin resolves it).
- **Formatting**: No explicit formatter configuration found; maintain the style of the file you are editing.
- **Resources**: JSON datasource definitions live under `resources/datasource/`. Shared config files sit in `resources/config/`.

## Testing Guidelines

- **Framework**: JUnit via the `maven-surefire-plugin` and `maven-failsafe-plugin` (inherited from Spring Boot parent).
- **Coverage**: Cobertura (`cobertura-maven-plugin 2.7`) generates HTML + XML reports.
- **Test location**: `src/test/java/` in each Maven module.
- **Naming**: Follow standard Maven conventions — `*Test.java` for unit tests, `*IT.java` for integration tests.
- **Run tests**: `./mvnw clean test` from the module root.

## Commit & Pull Request Guidelines

This project does not enforce a strict commit message convention. Follow these practical guidelines:

- Write concise, descriptive commit messages in Chinese or English as appropriate for the team.
- Prefix large-scope changes with the module name, e.g., `fw-plus-core: fix NPE in report parser`.
- Pull requests should include a short description of the change, motivation, and any breaking changes.
- Link related issues or feature requests when possible.

## Development Environment

- **IDE**: IntelliJ IDEA (`.idea/` directory checked in).
- **AI agent config**: `AGENTS.md` (this file) and `CLAUDE.md` contain project guidelines and AI assistant configuration.
- **Internal Nexus**: `http://172.17.1.6:28081/` — repositories `jugui-snapshot` and `jugui-release`.
- **License**: Apache 2.0.

</INSTRUCTIONS>
