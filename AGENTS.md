# Repository Guidelines

## Communication

请始终使用简体中文与我对话，并在回答时保持专业、简洁。包括：
- 所有的回答文本
- 所有的选项（如 AskUserQuestion 的选项、菜单选择等）
- 所有的推理过程和中间输出

## 项目概述

fw-plus 是 9527 的 Java 开发框架，基于 Spring Boot 4.1.1（Jakarta EE / JDK 21），提供 Spring Boot 自动化配置、Spring Cloud 扩展、MyBatis-Plus/Flex 增强、Magic-API 插件、AI（LangChain4j / Spring AI / Spring AI Alibaba / AgentScope / LangGraph4j）集成、图数据库 JDBC 驱动等能力。四个库层当前版本为 `0.4.0-SNAPSHOT`，通过 `${revision}` + `flatten-maven-plugin` 管理；`fw-plus-sample` 样例聚合器固定使用 `0.4.0-SNAPSHOT`，不使用 `flatten-maven-plugin`。

**当前分支**：仓库处于 `springboot4` 分支（JDK 17 + Spring Boot 3.5.8 → JDK 21 + Spring Boot 4.1.x 迁移线）。Core 与 Boot 层已完成升级（JDK 21、Boot 4.1.1，测试全绿）；Cloud、AI、Sample 层升级收口中（这两层 `<jdk.version>` 暂为 17）。Jackson 已随 Boot 4 切换到 3.x（`tools.jackson`），Jackson 2 仅在 magic-api fork 等兼容场景保留。迁移详情、版本矩阵与待办见 `docs/springboot4-upgrade-notes.md`。

**Git 仓库结构**：当前工作区只有根目录一个 `.git` 仓库（remote：`xiongx9527/java-framework`）；`fw-plus-core`、`fw-plus-boot`、`fw-plus-cloud`、`fw-plus-ai` 是同一仓库内的顶层 Maven 聚合目录，非 git submodule。根目录 `pom.xml` 聚合全部 5 层，可用根 `./mvnw` 一把构建。当前工作区不包含旧文档曾提到的 `fw-plus-product/` 和独立 `resources/` 仓库。构建产物在各层之间通过 Maven 本地仓库（`~/.m2`）传递。

各层 GroupId：
- Core: `com.xiongx9527.fw.plus`
- Boot: `com.xiongx9527.fw.plus.boot`
- Cloud: `com.xiongx9527.fw.plus.cloud`
- AI: `com.xiongx9527.fw.plus.ai`

## 模块结构

各库层顶层 Maven 目录基本遵循相同布局：
- `fw-plus-{name}-project/` — 实际库代码

所有样例模块集中在根目录 `fw-plus-sample/` 下，按 Boot、Cloud、AI 分组。

所有源码位于 `src/main/java`，资源位于 `src/main/resources`，源码编码统一为 UTF-8。

```
fw-plus/                      # 根目录（聚合工程，根 pom.xml 聚合下列 5 层，含 mvnw + .mvn/wrapper）
├── fw-plus-core/             # 核心库层（JDK 21, Spring Boot 4.1.1）
│   ├── fw-plus-project/
│   │   ├── fw-plus-parent/         # Core 层父 POM
│   │   ├── fw-plus-common/         # 公共工具：加密、UID、异常、序列化、SpringUtils
│   │   ├── fw-plus-annotation/     # 自定义注解：@Algorithm, @FieldEncrypt
│   │   ├── fw-plus-mybatis/        # MyBatis 增强聚合
│   │   │   ├── fw-plus-mybatis-datachange/ # 通用数据变更追踪
│   │   │   ├── fw-plus-mybatis-datascope/  # 通用数据权限过滤
│   │   │   ├── fw-plus-mybatis-encrypt/    # 通用字段加密
│   │   │   ├── fw-plus-mp/         # MyBatis-Plus 增强
│   │   │   │   ├── fw-plus-mp-base/       # MP 基础（base mapper/entity）
│   │   │   │   ├── fw-plus-mp-join/       # MP 连表查询
│   │   │   │   ├── fw-plus-mp-datachange/ # MP 数据变更追踪
│   │   │   │   ├── fw-plus-mp-datascope/  # MP 数据权限过滤
│   │   │   │   └── fw-plus-mp-encrypt/    # MP 字段加密（@FieldEncrypt）
│   │   │   └── fw-plus-mf/         # MyBatis-Flex 增强
│   │   │       ├── fw-plus-mf-base/       # MF 基础增强
│   │   │       ├── fw-plus-mf-datachange/ # MF 数据变更追踪
│   │   │       ├── fw-plus-mf-datascope/  # MF 数据权限过滤
│   │   │       └── fw-plus-mf-encrypt/    # MF 字段加密
│   │   ├── fw-plus-easytrans/      # Easy-Trans 字典翻译扩展
│   │   │   ├── fw-plus-easytrans-annotation/ # 翻译注解
│   │   │   ├── fw-plus-easytrans-common/     # 翻译公共类
│   │   │   └── fw-plus-easytrans-service/    # 翻译服务实现
│   │   ├── fw-plus-poi/            # EasyPoi 扩展
│   │   │   └── fw-plus-poi-easypoi/   # EasyPoi Excel/Word 操作（内嵌 cn.afterturn fork，jakarta 化需在仓内改）
│   │   ├── fw-plus-jdbc/           # JDBC 工具
│   │   ├── fw-plus-nosql/          # 非关系型数据库增强
│   │   │   └── fw-plus-redis/         # Redis 增强
│   │   ├── fw-plus-auth/           # 认证相关
│   │   │   ├── fw-plus-auth-base/     # 认证基础
│   │   │   ├── fw-plus-auth-core/     # 认证核心
│   │   │   └── fw-plus-auth-satoken/  # Sa-Token 认证扩展
│   │   ├── fw-plus-template/       # 模板引擎：Freemarker, docx4j, OpenPDF
│   │   ├── fw-plus-ai/             # AI 基础模块
│   │   │   ├── fw-plus-ai-base/       # AI 基础（向量存储、向量数据库客户端等）
│   │   │   ├── fw-plus-ai-mcp-server/ # MCP 协议支持
│   │   │   ├── fw-plus-ai-langchain4j/# LangChain4j 扩展
│   │   │   │   ├── fw-plus-ai-langchain4j-nebula/  # Nebula 向量库
│   │   │   │   └── fw-plus-ai-langchain4j-neo4j/   # Neo4j 向量库
│   │   │   └── fw-plus-ai-spring/     # Spring AI 扩展
│   │   ├── fw-plus-magicapi/       # Magic-API 扩展（Mongo, Redis, ES, Nebula 等）
│   │   ├── fw-plus-graph/          # 图数据库 JDBC 驱动
│   │   │   ├── fw-plus-graph-jdbc/    # 图数据库 JDBC 基础抽象
│   │   │   ├── fw-plus-nebula-jdbc/   # Nebula Graph JDBC 驱动
│   │   │   └── fw-plus-neo4j-jdbc/    # Neo4j JDBC 驱动
│   │   ├── fw-plus-dify/           # Dify API 客户端
│   │   └── fw-plus-dependencies/   # 统一版本管理（BOM）
│
├── fw-plus-boot/             # Spring Boot 自动配置层
│   ├── fw-plus-boot-project/
│   │   ├── fw-plus-boot/              # 集成自动配置（aggregator）
│   │   ├── fw-plus-boot-autoconfigure/ # 核心自动配置（AutoConfiguration.imports 注册 16 条）
│   │   ├── fw-plus-boot-common/       # Boot 公共类
│   │   ├── fw-plus-boot-dependencies/ # Boot 层 BOM
│   │   ├── fw-plus-boot-parent/       # Boot 层父 POM
│   │   └── fw-plus-boot-starters/     # 各种 Spring Boot Starter（14 个）
│   │       ├── fw-plus-admin-server-spring-boot-starter/  # Spring Boot Admin 监控
│   │       ├── fw-plus-boot-starter/                      # 通用 Boot Starter（aggregator）
│   │       ├── fw-plus-file-storage-spring-boot-starter/  # 文件存储（Minio/S3）
│   │       ├── fw-plus-influxdb-spring-boot-starter/      # InfluxDB 时序数据库
│   │       ├── fw-plus-jdbc-spring-boot-starter/          # JDBC 数据库
│   │       ├── fw-plus-magicapi-spring-boot-starter/      # Magic-API 主引擎
│   │       ├── fw-plus-magicapi-plugin/                   # Magic-API 插件（28 个）
│   │       │   ├── fw-plus-magicapi-plugin-admin/         # 控制台管理
│   │       │   ├── fw-plus-magicapi-plugin-config/        # 配置中心
│   │       │   ├── fw-plus-magicapi-plugin-db/            # 数据库浏览器
│   │       │   ├── fw-plus-magicapi-plugin-debezium/      # Debezium CDC
│   │       │   ├── fw-plus-magicapi-plugin-druid/         # Druid 监控
│   │       │   ├── fw-plus-magicapi-plugin-dtp/           # 动态线程池
│   │       │   ├── fw-plus-magicapi-plugin-elasticsearch/ # Elasticsearch
│   │       │   ├── fw-plus-magicapi-plugin-freemarker/    # Freemarker 模板
│   │       │   ├── fw-plus-magicapi-plugin-influx/        # InfluxDB
│   │       │   ├── fw-plus-magicapi-plugin-java-entity/   # Java 实体生成
│   │       │   ├── fw-plus-magicapi-plugin-kafka/         # Kafka 消息队列
│   │       │   ├── fw-plus-magicapi-plugin-mongo/         # MongoDB
│   │       │   ├── fw-plus-magicapi-plugin-mqtt-client/   # MQTT 客户端
│   │       │   ├── fw-plus-magicapi-plugin-mqtt-server/   # MQTT 服务端
│   │       │   ├── fw-plus-magicapi-plugin-mybatisflex/   # MyBatis-Flex 集成
│   │       │   ├── fw-plus-magicapi-plugin-mybatisplus/   # MyBatis-Plus 集成
│   │       │   ├── fw-plus-magicapi-plugin-pulsar/        # Pulsar 消息队列
│   │       │   ├── fw-plus-magicapi-plugin-rabbitmq/      # RabbitMQ 消息队列
│   │       │   ├── fw-plus-magicapi-plugin-redis/         # Redis 缓存
│   │       │   ├── fw-plus-magicapi-plugin-rocketmq/      # RocketMQ 消息队列
│   │       │   ├── fw-plus-magicapi-plugin-s3/            # S3 对象存储
│   │       │   ├── fw-plus-magicapi-plugin-satoken/       # Sa-Token 认证鉴权
│   │       │   ├── fw-plus-magicapi-plugin-spring-bean/   # Spring Bean 访问
│   │       │   ├── fw-plus-magicapi-plugin-swagger/       # Swagger API 文档
│   │       │   ├── fw-plus-magicapi-plugin-task/          # 定时任务
│   │       │   ├── fw-plus-magicapi-plugin-vue3/          # Vue3 前端资源
│   │       │   ├── fw-plus-magicapi-plugin-word/          # Word 文档操作
│   │       │   └── fw-plus-magicapi-plugin-ws/            # WebSocket
│   │       ├── fw-plus-mate-spring-boot-starter/          # Mate 租户/数据权限
│   │       ├── fw-plus-mp-spring-boot-starter/            # MyBatis-Plus 增强
│   │       ├── fw-plus-mqtt-client-spring-boot-starter/   # MQTT 客户端
│   │       ├── fw-plus-mqtt-server-spring-boot-starter/   # MQTT 服务端
│   │       ├── fw-plus-redis-spring-boot-starter/         # Redis 缓存
│   │       ├── fw-plus-template-spring-boot-starter/      # 模板引擎
│   │       └── fw-plus-ws-spring-boot-starter/            # WebSocket
│
├── fw-plus-cloud/            # Spring Cloud 扩展层
│   ├── fw-plus-cloud-project/
│   │   ├── fw-plus-cloud/              # Cloud 集成（aggregator）
│   │   ├── fw-plus-cloud-autoconfigure/ # 自动配置（Base, eureka server, gray LB, magic-api）
│   │   ├── fw-plus-cloud-common/       # Cloud 公共类
│   │   ├── fw-plus-cloud-dependencies/ # Cloud 层 BOM
│   │   ├── fw-plus-cloud-parent/       # Cloud 层父 POM
│   │   └── fw-plus-cloud-starters/     # Cloud Starter（4 个）
│   │       ├── fw-plus-admin-server-spring-cloud-starter/  # Spring Boot Admin 监控
│   │       ├── fw-plus-cloud-starter/                      # 通用 Cloud Starter（aggregator）
│   │       ├── fw-plus-cloud-magicapi-plugin/              # Magic-API Cloud 插件
│   │       │   └── fw-plus-cloud-magicapi-plugin-eureka/   # Eureka 插件
│   │       └── fw-plus-eureka-server-spring-cloud-starter/ # Eureka 注册中心
│
├── fw-plus-ai/               # AI 集成层
│   ├── fw-plus-ai-project/
│   │   ├── fw-plus-ai-boot/               # AI 集成（aggregator）
│   │   ├── fw-plus-ai-boot-autoconfigure/ # AI 自动配置（LangChain4j, MCP, AgentScope）
│   │   ├── fw-plus-ai-boot-common/        # AI 公共类
│   │   ├── fw-plus-ai-boot-dependencies/  # AI 层 BOM
│   │   ├── fw-plus-ai-boot-parent/        # AI 层父 POM
│   │   └── fw-plus-ai-boot-starters/      # AI Starter
│   │       ├── fw-plus-ai-boot-starter/                    # 通用 AI Starter（aggregator）
│   │       ├── fw-plus-ai-mcp-server-spring-boot-starter/  # MCP Server
│   │       └── fw-plus-ai-magicapi-plugin/                 # Magic-API AI 插件
│   │           ├── fw-plus-ai-magicapi-plugin-mcp-server/  # MCP Server 插件
│   │           └── fw-plus-ai-magicapi-plugin-langchain4j/ # LangChain4j 插件（9 个子模块）
│   │               ├── …-langchain4j-chat-model/               # 对话模型
│   │               ├── …-langchain4j-streaming-chat-model/     # 流式对话模型
│   │               ├── …-langchain4j-language-model/           # 语言模型
│   │               ├── …-langchain4j-streaming-language-model/ # 流式语言模型
│   │               ├── …-langchain4j-embedding-model/          # 向量模型
│   │               ├── …-langchain4j-embedding-store/          # 向量存储
│   │               ├── …-langchain4j-tokenizer/                # 分词器
│   │               ├── …-langchain4j-scoring-model/            # 评分模型
│   │               └── …-langchain4j-skill/                    # Skill
│
├── fw-plus-sample/                    # 集中样例模块
│   ├── fw-plus-boot-samples/          # Boot 示例项目（6 个）
│   │   ├── fw-plus-jdbc-spring-boot-starter-sample
│   │   ├── fw-plus-magicapi-spring-boot-starter-sample
│   │   ├── fw-plus-mate-spring-boot-starter-sample
│   │   ├── fw-plus-mp-spring-boot-starter-sample
│   │   ├── fw-plus-redis-spring-boot-starter-sample
│   │   └── fw-plus-ws-spring-boot-starter-sample
│   ├── fw-plus-cloud-samples/         # Cloud 示例项目（4 个）
│   │   ├── eureka-gateway-sample
│   │   ├── eureka-server-sample
│   │   ├── eureka-service-sample
│   │   └── nacos-gateway-sample
│   └── fw-plus-ai-samples/            # AI 示例项目（6 个）
│       ├── fw-plus-ai-magicapi-sample          # Magic-API AI 示例
│       ├── fw-plus-ai-mcp-server-sample        # MCP Server 示例
│       ├── fw-plus-ai-chat-sample              # AI 对话示例
│       ├── fw-plus-ai-agentscope-sample        # AgentScope 示例
│       ├── fw-plus-ai-nl2sql-sample            # NL2SQL 示例
│       └── fw-plus-ai-structured-output-sample # 结构化输出示例
│
├── docs/                     # 项目文档
│   ├── openspec/             # OpenSpec 变更与规格文档
│   ├── comet/                # Comet 变更与规格文档
│   ├── superpowers/          # superpowers plans / specs / reports
│   ├── sql/                  # SQL 辅助资源（原 META-INF/sql）
│   ├── nl2sql/               # NL2SQL 设计与脚本
│   ├── ai-framework-migration/        # AI 框架迁移脚本
│   ├── springboot4-upgrade-notes.md   # Spring Boot 4 / Java 21 升级说明（当前迁移线）
│   └── springboot3-upgrade-notes.md   # Spring Boot 3 升级说明（存档）
├── CONTEXT.md                # 领域术语表
├── CLAUDE.md                 # AI 助手配置
└── README.md
```

## Magic-API 资源目录详解

> **注意**：当前工作区没有 `resources/` 目录。以下内容描述运行环境中可由 Magic-API 引擎动态加载的外部资源仓库/目录结构；只有在该目录实际挂载或单独检出后才可直接修改。

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

**`ws-subscribe/`** — WebSocket 事件处理，结构同上。

### 特殊代码（非 magic-script 语言）

**`freemarker/`** — FreeMarker 模板语法（FTL），`.ms` 文件中 `================================` 下方是 FTL 代码，由 `template` 模块调用引擎渲染生成 Word、PDF、HTML 等文档。

## 关键技术栈

| 类别 | 技术 |
|------|------|
| 基础框架 | Spring Boot 4.1.1, Spring Cloud 2025.1.3（Oakwood） |
| JDK | 21（Core / Boot 已落地；Cloud / AI 层 `<jdk.version>` 暂为 17，升级收口中） |
| 云生态 | Spring Cloud Alibaba 2025.1.0.0, Spring Cloud Tencent 1.14.0-2023.0.0-RC2（待 Cloud 层实测切 2.1.2.0-2025.1.1） |
| ORM | MyBatis-Plus 3.5.17（boot4 坐标）, MyBatis-Flex 1.11.8（boot4 坐标）, MyBatis 3.5.19, mybatis-spring 4.0.0, mybatis-plus-join 1.5.9（仓内 fork 适配）, dynamic-datasource 4.6.0（boot4 坐标）, Druid 1.2.28（boot4 坐标） |
| 脚本引擎 | Magic-API 2.2.2（仓内维护版本，非中央仓库版本；Spring 7 适配含 vendored Mapping 遮蔽）, magic-script 1.9.0 |
| AI | LangChain4j 1.22.0（community 1.22.0-beta32）, Spring AI 2.0.1, Spring AI Alibaba 2.0.0-M1.1, AgentScope 2.0.4, LangGraph4j 1.9.3 |
| 序列化 | Jackson 3.x（`tools.jackson`，Boot 4 管线）；Jackson 2 仅兼容场景保留（magic-api fork 走 `jackson-2-bom` 2.21.5） |
| 消息队列 | Kafka, Pulsar 2.11.2, RocketMQ 5.3.2, RabbitMQ, Mica MQTT 2.6.13 |
| 数据库/存储 | MySQL, InfluxDB 6.7.0, Redis（Jedis 7.2.1）, Elasticsearch 8.19.8, Nebula Graph（client 3.8.4 / driver 5.3.0）, Neo4j driver 6.0.5, Minio/S3 |
| 工具库 | Hutool 5.8.48, EasyPoi 4.5.0, DynamicTp 1.2.1（Core BOM）/ 1.2.2（Boot BOM）, Debezium 3.7.0.Final, Seata 2.6.0 |
| 模板引擎 | Freemarker, docx4j, OpenPDF, poi-tl 1.12.2 |
| API 文档 | knife4j-next fork（com.baizhukui）5.7.8, springdoc 3.0.3（WebFlux 场景由 springdoc webflux 承载） |
| 可观测性 | OpenTelemetry 1.56.0（AI 层 1.61.0）, Apollo 2.5.0, Spring Boot Admin 4.1.4, Plumelog 3.5.3 |
| 字典翻译 | easy-trans 4.0.2（`org.dromara` 坐标，包名 `com.fhs.*` → `org.dromara.*`） |
| 认证鉴权 | Sa-Token 1.46.0（boot4 坐标）, OAuth2 |
| Servlet 容器 | Tomcat 11.0.27（钉版，Servlet 6.1） |
| 前端 | Vue3, Vite（Magic-API 控制台与插件控制台） |

## 重要提醒

- **Maven 构建**：根目录是聚合工程（`pom.xml` + `mvnw` + `.mvn/wrapper`，模块为 `fw-plus-core`/`fw-plus-boot`/`fw-plus-cloud`/`fw-plus-ai`/`fw-plus-sample`），可用 `./mvnw clean install -DskipTests` 一把跑全仓；四个库层各自也带有 `mvnw`，用于按层增量构建。层间依赖按 Core → Boot → Cloud / AI 顺序准备到本地 Maven 仓库后即可构建。`fw-plus-sample` 聚合器无独立 wrapper，使用根 wrapper：`./mvnw -f fw-plus-sample/pom.xml validate`。发版按版本号统一发，不单独发某个模块。
  - **boot / cloud / ai 三层默认 `maven.test.skip=true`**（编译、打包、安装、deploy 均不含测试）。需要跑测试时必须显式覆盖：`-Dmaven.test.skip=false`。
  - **`fw-plus-ai-langchain4j-*` 各模块需显式声明 `lombok`(provided) 与 `langchain4j-core`**（上游 `langchain4j-core` 是 optional，不传递）。
- **JDK 21**：四层顶层 pom 目标 `<jdk.version>21</jdk.version>`（Core / Boot 已落地；Cloud / AI 层当前仍为 17，升级收口中）。可使用 Java 9+ API（`List.of()`、`Map.copyOf()` 等）。同时代码已全面 **jakarta 化**（`jakarta.servlet.*`、`jakarta.validation.*`），不要再引入 `javax.servlet` / `javax.validation`；`javax.sql` / `javax.xml` / `javax.tools` 等属保留包，无需迁移。注意 Boot 4 管线默认 **Jackson 3**（`tools.jackson`），新代码优先使用 Jackson 3 命名空间；`com.fasterxml.jackson`（Jackson 2）仅用于 magic-api fork 等兼容场景。
- **Magic-API 版本**：使用自行维护的 2.2.2（jakarta 线，依赖 `magic-api-servlet-jakarta`），`org.ssssssss` 组的依赖通过 `<repositories>` 指向内部 Nexus 仓库（jugui-snapshot / jugui-release），请勿修改这些依赖的版本号。
- **源文件编码**：所有 Java 源文件、资源文件统一使用 UTF-8 编码。
- **文件路径**：不构造硬编码的跨平台文件路径，系统路径使用 `System.getProperty()` 或 `Path` API，资源路径使用 Spring `ResourceLoader` 或 `ClassPathResource`。
- **自定义类**：项目中包含大量自定义类和注解（如 `Algorithm`、`FieldEncrypt` 等），修改相关代码时确保充分理解其用途。
- **resources/**：当前工作区未检出该目录；若在运行环境中单独挂载，Magic-API 会动态加载其中资源。修改 api/function/task 等脚本目录后通常即时生效，无需重启服务；修改 datasource/config 等配置目录后需要重启服务或触发配置刷新。
- **迁移参考**：Spring Boot 4 / Java 21 迁移的版本矩阵、各层落地记录与待办清单见 `docs/springboot4-upgrade-notes.md`（Spring Boot 3 迁移记录存档于 `docs/springboot3-upgrade-notes.md`）。
- **领域术语**：`CONTEXT.md` 记录项目领域语言中已精确定义的术语（如宿主栈、栈支线、双栈一致、文档切面、控制台切面）；涉及相关概念时以其为准。

## 构建、测试与开发命令

```bash
# 全量构建（根聚合 5 层，跳过测试）
./mvnw clean install -DskipTests

# 按层增量构建（Core → Boot → Cloud / AI）
cd fw-plus-core && ./mvnw clean install -DskipTests
cd fw-plus-boot && ./mvnw clean install -DskipTests
cd fw-plus-cloud && ./mvnw clean install -DskipTests   # 或
cd fw-plus-ai && ./mvnw clean install -DskipTests

# 运行测试（Cobertura 覆盖率报告在 target/site/cobertura/）
cd fw-plus-core && ./mvnw clean test
cd fw-plus-boot && ./mvnw clean test -Dmaven.test.skip=false   # boot/cloud/ai 默认跳过测试

# 样例聚合器校验（fw-plus-sample 无独立 wrapper，用根 wrapper）
./mvnw -f fw-plus-sample/pom.xml validate

# 发布到内网 Nexus 仓库（jugui-snapshot / jugui-release）
cd fw-plus-core && ./mvnw clean deploy -DskipTests
```

四个库层顶层目录均包含 Maven Wrapper（`mvnw` + `.mvn/wrapper`）；根目录同样带 wrapper。`fw-plus-sample` 不包含 Wrapper，运行样例聚合器时使用根 `./mvnw -f fw-plus-sample/pom.xml validate`。

## 编码风格与命名约定

- **Java**: 目标 JDK 21（Core / Boot 已落地；Cloud / AI 层过渡期为 17），遵循标准 Java 编码规范。
- **缩进**: 使用 Tab（与现有 `pom.xml` 文件一致），保持与周围文件风格统一。
- **模块命名**: kebab-case — `fw-plus-core`、`fw-plus-boot`、`fw-plus-mp`。
- **包命名**: `com.xiongx9527.fw.plus.*` — 如 `com.xiongx9527.fw.plus.mp`、`com.xiongx9527.fw.plus.common`。
- **POM 属性**: 四个库层使用 `${revision}`（当前 `0.4.0-SNAPSHOT`）作为版本属性（flatten-maven-plugin 负责解析）；`fw-plus-sample` 聚合器固定使用 `0.4.0-SNAPSHOT`，不使用 flatten。
- **格式化**: 未找到显式的格式化配置，保持所编辑文件的现有风格。
- **资源文件**: 当前工作区未包含运行态 `resources/`。若单独检出该目录，JSON 数据源定义位于 `resources/datasource/`，共享配置文件位于 `resources/config/`。

## 测试指南

- **框架**: JUnit 6（随 Boot 4 `spring-boot-starter-test`），通过 `maven-surefire-plugin` 和 `maven-failsafe-plugin` 运行。
- **覆盖率**: Cobertura（`cobertura-maven-plugin 2.7`）生成 HTML + XML 报告。
- **测试位置**: 各 Maven 模块的 `src/test/java/` 目录。
- **命名**: 遵循标准 Maven 约定 — 单元测试用 `*Test.java`，集成测试用 `*IT.java`。
- **运行测试**: 从模块根目录执行 `./mvnw clean test`。注意 `fw-plus-boot` / `fw-plus-cloud` / `fw-plus-ai` 默认 `maven.test.skip=true`，需加 `-Dmaven.test.skip=false`；`fw-plus-core` 默认执行测试。
- **AI 层基线**: `fw-plus-ai-boot-autoconfigure` 含较多自动配置用例（含 `SpringBoot3DependencyBaselineTest` 等依赖基线校验），改动 AI 装配时优先回归该模块；该基线在 AI 层 Boot4 升级时同步更新为 Boot4 断言（见 `docs/springboot4-upgrade-notes.md` 附录 B）。

## 提交与 PR 规范

本项目不强制要求严格的提交消息格式。遵循以下实用指南：

- 根据团队习惯使用中文或英文编写简洁、描述性的提交消息。
- 大范围变更在消息前加模块名前缀，如 `fw-plus-core: 修复报表解析器中的 NPE`。
- Pull Request 应包含变更简述、动机以及破坏性变更说明（如有）。
- 尽可能关联相关的 issue 或功能请求。

## 开发环境

- **IDE**: IntelliJ IDEA（`.idea/` 目录已纳入版本控制）。
- **AI 代理配置**: `AGENTS.md`（本文件）和 `CLAUDE.md` 包含项目指南和 AI 助手配置。
- **领域术语**: `CONTEXT.md` 记录项目领域语言与术语。
- **当前分支**: `springboot4`（JDK 21 + Spring Boot 4 迁移线）。
- **许可证**: Apache 2.0。

<comet-ambient-resume>
<!-- Managed by Comet. Edits inside this block may be replaced by comet init/update. -->
<!-- Contract: comet.resume_probe.v2 -->

## Comet Ambient Resume

在这个仓库中，开始处理需要改动或调查的任务前，如果可能存在活跃 Comet workflow，把当前用户请求传入只读探针：`comet resume-probe . --stdin --json`。

- 如果用户通过宿主明确调用任意 Comet Skill（例如 `@comet`、`/comet`、`@comet-native` 或 `/comet-hotfix`），显式调用优先于本恢复协议；不要运行 resume probe，直接进入被调用的 Skill。
- 如果用户通过宿主明确调用的是非 Comet 的 Skill 或斜杠命令，任务意图已由该调用明确：不要运行 resume probe，直接执行该 Skill。
- 如果你正在 Comet 流程内（包括正在等待用户回复你在流程中提出的问题），不要运行 resume probe；把这类回复（例如方案/选项选择）当作当前 change 的继续，直接按用户的选择推进。
- 只信任返回的 `workflow`、`skill` 和 `entrySource`；它们只由项目配置或无配置兼容回退决定。不得扫描或切换另一套 workflow。
- 如果 probe 返回 `auto_resume`，简短说明选中的 active change，并进入 `nextCommand` 指向的永久入口。不要把状态命令当作恢复入口直接推进。
- 如果 probe 返回 `ask_user`，只问一个简短问题并等待用户回复。
- 如果当前请求未明确调用 Comet Skill，且 probe 返回 `out_of_scope` 或 `none`，不要进入 Comet workflow。
- `out_of_scope` 或 `none` 只表示不要因为这个新请求进入 Comet workflow；它绝不表示要暂停或退出一个已在进行的 Comet 流程。
- 如果配置或状态无效且没有 `nextCommand`，停止并报告原因；不要猜测另一个 workflow。
- 不能只因为存在 active change 就把无关任务挂到该 change。Native 的未提交改动由 Native 入口检查，不由探针自动归因。
</comet-ambient-resume>
