# CLAUDE.md

本文件为 Claude Code（claude.ai/code）在本仓库中工作时提供项目约定与代码上下文。

## Communication

请始终使用简体中文与我对话，并在回答时保持专业、简洁。包括：
- 所有的回答文本
- 所有的选项（如 AskUserQuestion 的选项、菜单选择等）
- 所有的推理过程和中间输出

## 项目概述

fw-plus 是 9527 的 Java 开发框架，基于 Spring Boot 2.7.18，提供 Spring Boot 自动化配置、Spring Cloud 扩展、MyBatis-Plus/Flex 增强、Magic-API 插件、AI（LangChain4j / Agents-Flex / Spring AI / EasyAi）集成、图数据库 JDBC 驱动等能力。四个库层当前版本为 `0.1.1-SNAPSHOT`，通过 `${revision}` + `flatten-maven-plugin` 管理；`fw-plus-sample` 样例聚合器固定使用 `0.1.1-SNAPSHOT`，不使用 `flatten-maven-plugin`。

**Git 仓库结构**：当前工作区只有根目录一个 `.git` 仓库；`fw-plus-core`、`fw-plus-boot`、`fw-plus-cloud`、`fw-plus-ai` 是同一仓库内的顶层 Maven 聚合目录，非 git submodule。当前工作区不包含旧文档曾提到的 `fw-plus-product/` 和独立 `resources/` 仓库。构建产物在各层之间通过 Maven 本地仓库（`~/.m2`）传递。

各层 GroupId：
- Core: `com.xiongx9527.fw.plus`
- Boot: `com.xiongx9527.fw.plus.boot`
- Cloud: `com.xiongx9527.fw.plus.cloud`
- AI: `com.xiongx9527.fw.plus.ai`

## 模块结构

```
fw-plus/                      # 根目录（聚合项目，根 pom.xml 聚合下列 5 个库层，根目录也有 mvnw）
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
│   │   │   └── fw-plus-ai-mcp-server/        # MCP 协议支持
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
│   │   ├── fw-plus-graph/        # 图数据库 JDBC 驱动
│   │   │   ├── fw-plus-nebula-jdbc/   # Nebula Graph JDBC 驱动
│   │   │   └── fw-plus-neo4j-jdbc/    # Neo4j JDBC 驱动
│   │   ├── fw-plus-dify/         # Dify API 客户端
│   │   └── fw-plus-dependencies/ # 统一版本管理（BOM）
│
├── fw-plus-boot/             # Spring Boot 自动配置层
│   ├── fw-plus-boot-project/
│   │   ├── fw-plus-boot/              # 集成自动配置（aggregator）
│   │   ├── fw-plus-boot-autoconfigure/ # 核心自动配置（spring.factories 注册 15 个顶层 AutoConfiguration，含 JimuJsEnterpriseFilter）
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
│   │       │   ├── fw-plus-magicapi-plugin-kafka/         # Kafka 消息队列
│   │       │   ├── fw-plus-magicapi-plugin-mongo/         # MongoDB
│   │       │   ├── fw-plus-magicapi-plugin-mqtt-server/          # MQTT 协议支持
│   │       │   ├── fw-plus-magicapi-plugin-mqtt-client/    # MQTT 客户端
│   │       │   ├── fw-plus-magicapi-plugin-pulsar/        # Pulsar 消息队列
│   │       │   ├── fw-plus-magicapi-plugin-rabbitmq/      # RabbitMQ 消息队列
│   │       │   ├── fw-plus-magicapi-plugin-redis/         # Redis 缓存
│   │       │   ├── fw-plus-magicapi-plugin-rocketmq/      # RocketMQ 消息队列
│   │       │   ├── fw-plus-magicapi-plugin-s3/            # S3 对象存储
│   │       │   ├── fw-plus-magicapi-plugin-satoken/       # Sa-Token 认证鉴权
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
│   │       └── fw-plus-uid-spring-boot-starter/           # 分布式 UID
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
│   │       └── fw-plus-ai-magicapi-plugin/                        # Magic-API AI 插件（4 个）
│   │           ├── fw-plus-ai-magicapi-plugin-langchain4j/ # LangChain4j 插件
│   │           ├── fw-plus-ai-magicapi-plugin-milvus/      # Milvus 向量数据库
│   │           ├── fw-plus-ai-magicapi-plugin-qdrant/      # Qdrant 向量数据库
│   │           └── fw-plus-ai-magicapi-plugin-weaviate/    # Weaviate 向量数据库
│
├── fw-plus-sample/                    # 集中样例模块
│   ├── fw-plus-boot-samples/          # Boot 示例项目（5 个）
│   │   ├── fw-plus-jdbc-spring-boot-starter-sample
│   │   ├── fw-plus-magicapi-spring-boot-starter-sample
│   │   ├── fw-plus-mate-spring-boot-starter-sample
│   │   ├── fw-plus-mp-spring-boot-starter-sample
│   │   └── fw-plus-redis-spring-boot-starter-sample
│   ├── fw-plus-cloud-samples/         # Cloud 示例项目（5 个）
│   │   ├── eureka-gateway-sample
│   │   ├── eureka-server-sample
│   │   ├── eureka-service-sample
│   │   ├── nacos-gateway-sample
│   │   └── nacos-server-sample        # Nacos Server 分发目录（非 Maven 子模块）
│   └── fw-plus-ai-samples/            # AI 示例项目
│       └── fw-plus-ai-magicapi-sample # Magic-API AI 示例
│
├── META-INF/sql/             # 根目录 SQL 辅助资源
├── docs/                     # 项目文档与执行计划
└── openspec/                 # OpenSpec 变更与规格文档
```

## Magic-API 资源目录详解

> **注意**：当前工作区没有 `resources/` 目录。以下内容描述运行环境中可由 Magic-API 引擎动态加载的外部资源仓库/目录结构；只有在该目录实际挂载或单独检出后才可直接修改。

`resources/` 目录下的每个子目录对应一种 Magic-API 资源类型，由对应的插件提供存储和动态注册能力。每个资源类型的文件格式、脚本语言各有不同。

### 配置类（纯 JSON，定义连接信息）

这些目录只存放静态连接配置，不包含可执行逻辑。`*-datasource` 全部属于此类：

**`datasource/`** — JDBC 数据库连接
```json
// datasource/评估.json
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
// 也可异步发送：kafka['kangyang'].sendAsync("topic_name", messageObj)
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
// ws-datasource/xxx.json
{ "name": "xxx", "key": "xxx", ... }
// 脚本中使用：import ws;
// ws['xxx'].broadcastMessageToClient(clientId, msg)
// ws['xxx'].broadcastDistributedMessageToAllClient(msg)
// ws['xxx'].generateToken(secret)
```

**`dtp-datasource/`** — 动态线程池连接
```json
// dtp-datasource/xxx.json
{ "name": "xxx", "key": "xxx", ... }
// 脚本中使用：import dtp;
// dtp.execute(() -> { 异步任务 });
```

**`debezium-datasource/`** — Debezium CDC 连接（纯底层 CDC 数据源，无脚本侧 API，仅用于配置数据变更监听）

### 配置中心（YAML）

**`config/`** — 轻量配置中心，`.ms` 文件中 `================================` 下方是 **YAML 格式**的层级化配置，按 `magic.api.{类型}.{key}.{字段}` 管理各组件的连接参数，值可按 Spring profile 区分：
```yaml
# config/开发环境/redis.ms
================================
magic:
  api:
    redis:
      rediskey:
        hostname:
          环境1(spring.profiles.active): hostname值
          环境2(spring.profiles.active): hostname值
        port:
          环境1(spring.profiles.active): port值
        password:
          环境1(spring.profiles.active): password值
```

### 代码类（`.ms` 脚本，magic-script 语言）

每个 `.ms` 文件由 **JSON 头部** + `================================` + **脚本代码** 组成，包含可执行逻辑：

**`api/`** — REST API 接口，JSON 头部定义 `path`（URL）、`method`（GET/POST）、`options`（权限/校验），脚本处理请求并返回数据：
```magicscript
// api/数据管理/测试生成/保存.ms  path: "/save", method: "POST"
================================
return db.table('t_data_test').primary('id').saveOrUpdate(data)
```

**`function/`** — 可复用函数，可被其他脚本通过 `import` 引用：
```magicscript
// function/公共/获取环境信息.ms  path: "getEnv"
================================
import request;
var tmpEnv = "";
if (request.getHeaders("env")) {
    tmpEnv = "_" + request.getHeaders("env")[0];
}
return tmpEnv;
// API 中调用：import '@/public/getEnv' as getEnv; var env = getEnv();
```

**`kafka-subscribe/`** — Kafka 消息消费，JSON 头部定义 `key`（数据源）、`topic`（主题）、`group`（消费组），收到消息时脚本自动执行：
```magicscript
// kafka-subscribe/.../监听biz_evaluation数据变化.ms
// key: "kangyang", topic: "biz_evaluation", group: "magic-api"
================================
import log; import pulsar;
log.info("biz_evaluation有数据变化了:{}", msg.value());
pulsar['workorder'].send("biz_evaluation_datachange_topic", msg.value());
return null;
```

**`pulsar-subscribe/`** — Pulsar 消息消费，通过 `new String(msg.getValue())` 获取消息内容：
```magicscript
// pulsar-subscribe/评估/iot回调.ms
// key: "evaluation", topic: "iot-callback", group: "magic-api"
================================
import log;
import '@/services/common/iotCallback' as iotCallback;
var msgStr = new String(msg.getValue());
log.info("pulsar消费消息:{}", msgStr);
iotCallback(msgStr)
return null;
```

**`task/`** — 定时任务，JSON 头部定义 `cron` 表达式，按 Cron 周期自动执行：
```magicscript
// task/评估/定时回调评估结果信息.ms  cron: "0 0/10 * * * ?"
================================
import '@/evaluation/evaluationResultCallbak' as evaluationResultCallbak;
var list = db["evaluation"].select("""
    SELECT * FROM evaluation_biz_other_info WHERE delete_flag = 0 AND callback_flag = 0
""");
if (!list) { return; }
// ... 处理逻辑
```

**`ws-subscribe/`** — WebSocket 消息处理，结构同上。

### 特殊代码（非 magic-script 语言）

**`freemarker/`** — FreeMarker 模板语法（FTL），`.ms` 文件中 `================================` 下方是 FTL 代码，由 `template` 模块调用引擎渲染生成 Word、PDF、HTML 等文档：
```ftl
// freemarker/服务/评估/健康评估报告.ms  path: "selfDevRiskReport"
================================
<#include "/services/common/detectBrowser">
<script>
    const deviceType = detectBrowser();
    window.location.href = url;
</script>
```

**`vue3/`** — VUE3 语法（SFC），`.ms` 文件中 `================================` 下方是 `<template>` + `<script setup>` + `<style scoped>`，由 `vue3` 模块在后端编译构建为 HTML 页面返回浏览器显示。

### 分类总览

| 类别 | 目录 | 文件格式 | 脚本语言 |
|------|------|---------|---------|
| 连接配置 | `datasource/` | JSON | — |
| 连接配置 | `kafka-datasource/` | JSON | — |
| 连接配置 | `pulsar-datasource/` | JSON | — |
| 连接配置 | `redis-datasource/` | JSON | — |
| 连接配置 | `s3-datasource/` | JSON | — |
| 连接配置 | `ws-datasource/` | JSON | — |
| 连接配置 | `debezium-datasource/` | JSON | — |
| 连接配置 | `dtp-datasource/` | JSON | — |
| 配置中心 | `config/` | `.ms` | YAML |
| 接口/逻辑 | `api/` | `.ms` | magic-script |
| 接口/逻辑 | `function/` | `.ms` | magic-script |
| 接口/逻辑 | `task/` | `.ms` | magic-script |
| 消息消费 | `kafka-subscribe/` | `.ms` | magic-script |
| 消息消费 | `pulsar-subscribe/` | `.ms` | magic-script |
| 消息消费 | `ws-subscribe/` | `.ms` | magic-script |
| 特殊 | `freemarker/` | `.ms` | FreeMarker 模板语法 |
| 特殊 | `vue3/` | `.ms` | VUE3 语法 |

**规律**：
- `*datasource` → 纯 JSON 连接配置
- `*subscribe` → magic-script 消息消费逻辑
- `api/`、`function/`、`task/` → magic-script 调用/调度逻辑
- `config/` → YAML 配置中心值
- `freemarker/`、`vue3/` → 非 magic-script 语言，各有独立的编译/渲染引擎

## 关键技术栈

| 组件 | 版本 |
|---|---|
| Java | 1.8 |
| Maven | 3.6.3 |
| Spring Boot | 2.7.18 |
| Spring Cloud | 2021.0.9 |
| Spring Cloud Alibaba | 2021.0.5.0 |
| Spring Cloud Tencent | 1.14.0-2023.0.0-RC2 |
| MyBatis-Plus | 3.3.2 |
| MyBatis-Flex | 1.11.4 |
| Magic-API | 2.2.2 |
| JimuReport | 2.3.4 |
| Sa-Token | 1.44.0 |
| LangChain4j | 0.35.0 |
| Agents-Flex | 1.3.4 |
| Spring AI | 1.0.1 |
| EasyAi | 1.4.9 |
| Apache Pulsar | 2.11.2 |
| Apache Kafka | SB managed |
| Apache RocketMQ | 5.1.3 |
| RabbitMQ | SB managed |
| Mica MQTT | 2.3.9 |
| Debezium | 1.9.8.Final |
| Hutool | 5.8.34 |
| EasyPoi | 4.5.0 |
| DynamicTp | 1.2.1（Core BOM）/ 1.2.2（Boot BOM） |
| Apollo | 2.5.0 |
| OpenTelemetry | 1.56.0（Core BOM）；Boot BOM 部分构件覆盖为 1.44.1 |
| 前端控制台 | Vue3 + Vite |

## 构建命令

```bash
# 全量编译（跳过测试）— 可在根目录一把跑（根目录为聚合工程，带 pom.xml 与 mvnw）
# 也可按层增量构建：四个库层各自带有 Maven Wrapper (mvnw)，无需系统安装 mvn
./mvnw clean install -DskipTests

# 按层增量编译（需先进入对应层目录）
cd fw-plus-core && ./mvnw clean install -pl fw-plus-project/fw-plus-common -DskipTests

# 发布到内网 Nexus
./mvnw deploy -DskipTests

# 查看依赖树
./mvnw dependency:tree
```

**构建顺序**：必须先按 Core → Boot → Cloud / AI 顺序准备本地 Maven 依赖，下层依赖上层的 Maven 产物。根目录为聚合工程（`pom.xml` + `mvnw`，模块为 `fw-plus-core`/`fw-plus-boot`/`fw-plus-cloud`/`fw-plus-ai`/`fw-plus-sample`），可在根目录执行 `./mvnw compile` / `./mvnw test`；四个库层各自也带有 `mvnw`，用于按层增量构建（如 `cd fw-plus-core && ./mvnw clean install -DskipTests`）。`fw-plus-sample` 样例聚合目录不带 Wrapper，统一入口为 `./fw-plus-core/mvnw -f fw-plus-sample/pom.xml validate`。

**版本管理**：`fw-plus-core`、`fw-plus-boot`、`fw-plus-cloud`、`fw-plus-ai` 四个库层使用 `${revision}` 属性（当前值 `0.1.1-SNAPSHOT`）+ `flatten-maven-plugin` 解析版本占位符；`fw-plus-sample` 聚合器固定使用 `0.1.1-SNAPSHOT`，不使用 flatten。

依赖 Jeecg 官方仓库（`https://maven.jeecg.org`）。

## 前端控制台构建

修改了 Magic-API 相关模块的前端控制台代码后，必须通过 Maven 构建自动编译打包，**不能手动复制 dist 文件**。

### 构建规则

- **主控制台**（`fw-plus-magicapi-spring-boot-starter`）：构建 `src/console/` → `npm run build` → 产物自动复制到 `target/classes/magic-editor/`
- **Boot 层插件**（`fw-plus-magicapi-plugin/*`，有 `src/console/` 的模块）：构建 `src/console/` → 产物自动复制到 `target/classes/magic-editor/plugins/`
  - 注意：`task`、`swagger` 插件没有 `src/console/`

以上自动化由 `pom.xml` 中的 `exec-maven-plugin`（npm install + npm run build）+ `maven-resources-plugin`（copy-resources）完成。

### 触发阶段

`exec-maven-plugin`（npm install + npm run build）和 `maven-resources-plugin`（copy-resources）均绑定在 `prepare-package` 阶段，`mvn compile` 不会触发前端构建：

| Maven 命令 | 是否编译前端 |
|-----------|------------|
| `mvn compile` | **否** |
| `mvn package` | **是** |
| `mvn install` | **是** |
| `mvn deploy` | **是** |

如果只需编译 Java 代码，直接 `mvn compile -DskipTests` 即可。

## JimuReport 破解体系

核心思想：利用 JVM 类加载顺序（application classes 优先于 JAR 中的 classes），在 `org.jeecg` 包下放置同名类覆盖 JAR 中的加密/受保护类，使积木报表以企业版模式运行。

### 后端破解

位置：`fw-plus-core/fw-plus-project/fw-plus-report/fw-plus-report-jimu/src/main/java/org/jeecg/`

| 类 | 作用 |
|---|---|
| `modules/jmreport/c/a/a.java` | 自定义 ClassLoader，解密 `/META-INF/.templates/` 中的加密类 |
| `modules/jmreport/c/b/a.java` | 核心解密工具，空实现 `d()` 跳过 RSA 签名校验防 `Runtime.halt(0)`，修补 FTL 模板 |
| `modules/jmreport/c/b/c.java` | 方法调度中心，注入企业版标志到参数，递归修补返回值 |
| `common/api/vo/Result.java` | 桩类，满足 `Class.forName` 检测，关闭 HTTP 429 限流 |
| `common/aspect/DictAspect.java` | 桩类，同上 |

### 前端破解

位置：`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/report/webmvc/jimu/JimuJsEnterpriseFilter.java`

Servlet Filter 拦截 JS 响应做字节替换：
- 关闭 `isLimitUse` 标志
- 修补 Yc dispatch（barcode/qrcode/view-setting 空 handler）
- 注入 `addBarcode`/`addQrcode` 方法体
- JS 补丁注入（view-setting 兜底）
- 清除升级提示文字

### 注意事项

- **不要**在项目 `static/` 目录下放置与 JAR 同名的前端文件，会导致字节替换匹配失败
- Filter 拦截的 URL 模式：`/drag/lib/js/*`、`/jmreport/desreport_/js/*`、`/jmreport/desreport_/corelib/*`
- 依赖版本：`jimureport-spring-boot-starter:2.3.4`
- 当前工作区未包含旧 Product 层；若在外部产品仓库中接入 JimuReport，可结合产品侧 Filter 与 `JimuJsEnterpriseFilter` 控制功能开关

## 核心架构模式

- **分层设计**：Core → Boot → Cloud / AI，下层不依赖上层
- **Spring Boot auto-configuration**：通过 `spring.factories` 注册 `@Configuration`，用户通过 `@EnableXxx` 或配置属性控制。Boot 层注册了 15 个顶层 AutoConfiguration 类（Base、Trace、Web、Mate、NoSql、JDBC、MP、Template、UID、Report、Mqtt、ConfigServer/Client/Event/Apollo/Nacos、AdminServer、MagicAPI、WS），AI 层 4 个，Cloud 层 4 个
- **Magic-API 插件体系**：每个插件独立 Starter，扩展 magic-api 的能力（数据源、任务、WebSocket、MCP、消息队列等 20+ 种资源类型）
- **灰度发布**：在 `fw-plus-cloud-autoconfigure` 实现，通过 Ribbon/Feign 拦截器传递灰度标签
- **MyBatis-Plus 增强**：提供 base mapper/entity、字段加密（`@FieldEncrypt`）、数据变更追踪、数据权限过滤
- **Easy-Trans**：字典翻译注解体系，用于实体属性自动翻译
- **Sa-Token**：Core 提供认证基础与 Sa-Token 扩展，Boot 提供 Magic-API Sa-Token 插件

### MQ 插件动态注册器（AbstractMagicDynamicRegistry）模式

MQ（消息队列）插件继承 `AbstractMagicDynamicRegistry<T>` 时，`register(T entity)` 和 `register(MappingNode<T>)` 的职责分工有两种已验证的模式，其他类型资源按需处理：

**规则 A — 资源托管模式（DataSource 类型）：`registerByEntity` 放 `register(MappingNode)`**
```java
public boolean register(T entity) {
    unregisterByEntity(entity);               // 只清理
    return super.register(entity);             // 由父类触发 register(MappingNode)
}
protected boolean register(MappingNode<T> mappingNode) {
    registerByEntity(mappingNode.getEntity()); // 父类确保只在新注册/改 key 时调
    return super.register(mappingNode);
}
```
MagicAPI 同 key 刷新走 DELETE + SAVE 两条事件，`register(MappingNode)` 每次都被触发，且避免 `super.register(entity)` 失败时产生孤立资源。

**规则 B — 业务逻辑注册器（Subscribe 类型）：`registerByEntity` 放 `register(entity)`**
```java
public boolean register(T entity) {
    try {
        boolean result = super.register(entity);
        if (result) {
            registerByEntity(entity);          // 框架存储成功后才创建资源
        }
        return result;
    } catch (Exception e) { return false; }
}
protected boolean register(MappingNode<T> mappingNode) {
    return super.register(mappingNode);         // 空方法
}
```
注册时需检查外部依赖（如数据源是否就绪），有重试机制，在框架存储成功后才创建业务资源。

两种模式不能混用。DataSource 的 `registerByEntity` 不能上移到 `register(entity)`，Subscribe 的 `registerByEntity` 不能下放到 `register(MappingNode)`。非 MQ 插件不强制遵循此模式。

### 更新 magic-api Skill

当需要更新 `.claude/skills/magic-api/SKILL.md` 时，必须结合以下来源：

1. **官方文档**：Magic-API 官网 <https://www.ssssssss.org/magic-api/>，获取最新的语法规则、配置选项和使用说明
2. **上游参考**：Magic-API 官方 AI 插件的 prompt 文件（本地路径 `/Users/xiongxiao/Workspaces/Idea/codes/JuGui/magic-api/magic-api-plugins/magic-api-plugin-ai/src/main/resources/prompt`），获取最新的语法规则和官方示例
3. **项目实际代码**：以下目录中的当前项目代码，确保 skill 反映项目的真实扩展和约定：
   - `fw-plus-core/fw-plus-project/fw-plus-magicapi/` — Magic-API 核心引擎修改
   - `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/` — 自动配置、插件、自定义模块
   - `fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-autoconfigure/` — Cloud 层 Magic-API 自动配置（灰度发布、Eureka、Magic-API 集成）
   - `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/` — AI 层自动配置（LangChain4j、Agents-Flex、Spring AI、EasyAi、向量存储等 @MagicModule）
   - `fw-plus-sample/fw-plus-boot-samples/fw-plus-magicapi-spring-boot-starter-sample/src/main/resources/` — Magic-API 示例项目中的 demo 脚本和资源定义
   - 外部 `resources/` 目录（仅在当前工作区实际挂载或单独检出后）— 实际运行的 API、函数、任务脚本

优先使用 WebFetch 抓取上游 Gitee 页面获取最新 prompt 内容，再探索项目代码对比差异，最后更新 skill 文件。

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
