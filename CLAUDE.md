# CLAUDE.md

本文件为 Claude Code（claude.ai/code）在本仓库中工作时提供项目约定与代码上下文。

## Communication

请始终使用简体中文与我对话，并在回答时保持专业、简洁。包括：
- 所有的回答文本
- 所有的选项（如 AskUserQuestion 的选项、菜单选择等）
- 所有的推理过程和中间输出

## 项目概述

fw-plus 是 9527 的 Java 开发框架，基于 Spring Boot 4.1.1（Jakarta EE / JDK 21），提供 Spring Boot 自动化配置、Spring Cloud 扩展、MyBatis-Plus/Flex 增强、Magic-API 插件、AI（LangChain4j / Spring AI / Spring AI Alibaba / AgentScope / LangGraph4j）集成、图数据库 JDBC 驱动等能力。四个库层当前版本为 `0.4.0-SNAPSHOT`，通过 `${revision}` + `flatten-maven-plugin` 管理；`fw-plus-sample` 样例聚合器固定使用 `0.4.0-SNAPSHOT`，不使用 `flatten-maven-plugin`。

**当前分支**：仓库处于 `springboot4` 分支（JDK 17 + Spring Boot 3.5.8 → JDK 21 + Spring Boot 4.1.x 迁移线）。Core 与 Boot 层已完成升级（JDK 21、Boot 4.1.1，测试全绿）；Cloud、AI、Sample 层升级收口中（这两层 `<jdk.version>` 暂为 17）。迁移版本矩阵、各层落地记录与待办见 `docs/springboot4-upgrade-notes.md`。

**Git 仓库结构**：当前工作区只有根目录一个 `.git` 仓库（remote：`xiongx9527/java-framework`）；`fw-plus-core`、`fw-plus-boot`、`fw-plus-cloud`、`fw-plus-ai` 是同一仓库内的顶层 Maven 聚合目录，非 git submodule。当前工作区不包含旧文档曾提到的 `fw-plus-product/`、`fw-plus-report/`（JimuReport 模块已移除）和独立 `resources/` 仓库。构建产物在各层之间通过 Maven 本地仓库（`~/.m2`）传递。

各层 GroupId：
- Core: `com.xiongx9527.fw.plus`
- Boot: `com.xiongx9527.fw.plus.boot`
- Cloud: `com.xiongx9527.fw.plus.cloud`
- AI: `com.xiongx9527.fw.plus.ai`

## 模块结构与 Magic-API 资源目录

完整的模块树（5 个聚合层、14 个 Boot Starter、28 个 Magic-API 插件、样例清单）、Magic-API 外部 `resources/` 资源目录详解（datasource / config / api / function / subscribe / task / freemarker 等）与分类总览，见 `AGENTS.md`，以该文件为准。要点：

- 自动配置注册：Boot 层 `fw-plus-boot-autoconfigure` 通过 `META-INF/spring/...AutoConfiguration.imports` 注册 16 条顶层自动配置（Boot 4 起不再使用 `spring.factories`）。
- Magic-API 版本：仓内自维护 2.2.2（jakarta 线，依赖 `magic-api-servlet-jakarta`），`org.ssssssss` 组依赖经 `<repositories>` 指向内部 Nexus 仓库（jugui-snapshot / jugui-release），请勿修改版本号。
- Spring 7 适配：`setTrailingSlashMatch` 等尾斜杠匹配 API 被移除，由仓内 vendored `org.ssssssss.magicapi.utils.Mapping`（classpath 序先于 magic-api）遮蔽解决。

## 关键技术栈

| 组件 | 版本 |
|---|---|
| Java | 21（Core / Boot 已落地；Cloud / AI 层 `<jdk.version>` 暂为 17，升级收口中） |
| Maven（wrapper 固定） | 3.6.3 |
| Spring Boot | 4.1.1 |
| Spring Cloud | 2025.1.3（Oakwood） |
| Spring Cloud Alibaba | 2025.1.0.0 |
| Spring Cloud Tencent | 1.14.0-2023.0.0-RC2（待 Cloud 层实测切 2.1.2.0-2025.1.1） |
| Servlet 容器 | Tomcat 11.0.27（钉版，Servlet 6.1） |
| MyBatis-Plus | 3.5.17（boot4 坐标） |
| MyBatis-Flex | 1.11.8（boot4 坐标） |
| MyBatis / mybatis-spring | 3.5.19 / 4.0.0 |
| mybatis-plus-join | 1.5.9（仓内 fork 适配） |
| Magic-API / magic-script | 2.2.2（仓内维护）/ 1.9.0 |
| Sa-Token | 1.46.0（boot4 坐标） |
| Druid / dynamic-datasource | 1.2.28（boot4 坐标）/ 4.6.0（boot4 坐标） |
| easy-trans | 4.0.2（`org.dromara` 坐标，包名 `com.fhs.*` → `org.dromara.*`） |
| LangChain4j | 1.22.0（community 1.22.0-beta32） |
| Spring AI / Spring AI Alibaba | 2.0.1 / 2.0.0-M1.1 |
| AgentScope / LangGraph4j | 2.0.4 / 1.9.3 |
| Jackson | 3.x（`tools.jackson`，Boot 4 管线）；Jackson 2 仅兼容场景保留（magic-api fork 走 `jackson-2-bom` 2.21.5） |
| Pulsar / RocketMQ / Mica MQTT | 2.11.2 / 5.3.2 / 2.6.13 |
| Debezium / Seata | 3.7.0.Final / 2.6.0 |
| Hutool / EasyPoi / poi-tl | 5.8.48 / 4.5.0 / 1.12.2 |
| DynamicTp | 1.2.1（Core BOM）/ 1.2.2（Boot BOM） |
| knife4j（com.baizhukui fork）/ springdoc | 5.7.8 / 3.0.3（WebFlux 由 springdoc webflux 承载） |
| OpenTelemetry | 1.56.0（AI 层 1.61.0） |
| Spring Boot Admin | 4.1.4 |
| InfluxDB / Elasticsearch / Redis（Jedis） | 6.7.0 / 8.19.8 / 7.2.1 |
| Nebula Graph / Neo4j | client 3.8.4、driver 5.3.0 / Java driver 6.0.5 |
| 测试框架 | JUnit 6（随 Boot 4 `spring-boot-starter-test`） |
| 前端控制台 | Vue3 + Vite |

## 构建命令

```bash
# 全量编译（跳过测试）— 根目录聚合一把跑
./mvnw clean install -DskipTests

# 按层增量构建（Core → Boot → Cloud / AI；Cloud / AI 两层升级收口中）
cd fw-plus-core && ./mvnw clean install -DskipTests

# 运行测试（boot / cloud / ai 默认 maven.test.skip=true，需显式开启；core 默认跑测试）
cd fw-plus-boot && ./mvnw clean test -Dmaven.test.skip=false

# 发布到内网 Nexus（jugui-snapshot / jugui-release）
cd fw-plus-core && ./mvnw clean deploy -DskipTests

# 查看依赖树
./mvnw dependency:tree
```

**构建顺序**：必须先按 Core → Boot → Cloud / AI 顺序准备本地 Maven 依赖，下层依赖上层的 Maven 产物。根目录为聚合工程（`pom.xml` + `mvnw` + `.mvn/wrapper`，Maven Wrapper 固定 3.6.3），可在根目录执行 `./mvnw compile` / `./mvnw test`；四个库层各自也带有 `mvnw`，用于按层增量构建。`fw-plus-sample` 样例聚合目录不带 Wrapper，统一入口为根 wrapper：`./mvnw -f fw-plus-sample/pom.xml validate`。

**版本管理**：四个库层使用 `${revision}`（当前值 `0.4.0-SNAPSHOT`）+ `flatten-maven-plugin` 解析；`fw-plus-sample` 聚合器固定使用 `0.4.0-SNAPSHOT`，不使用 flatten。

## 前端控制台构建

修改了 Magic-API 相关模块的前端控制台代码后，必须通过 Maven 构建自动编译打包，**不能手动复制 dist 文件**。

- **主控制台**（`fw-plus-magicapi-spring-boot-starter`）：构建 `src/console/` → 产物进入 `target/classes/magic-editor/`
- **Boot 层插件**（有 `src/console/` 的 Magic-API 插件）：产物进入 `target/classes/magic-editor/plugins/`

当前构建链路（旧文档中「`mvn compile` 不触发前端构建」的 prepare-package 时代描述已过时）：

1. `initialize`：`clean-stale-console-assets` 清空 `target/classes/magic-editor`（vite 每次生成新 hash 文件名，避免旧产物在 target 与最终 jar 中堆积）
2. `generate-resources`：`exec-maven-plugin` 在 `src/console/` 执行 `npm install` + `npm run build`
3. `process-resources`：默认 `resources:resources` 把 `src/console/dist` 复制到 `target/classes/magic-editor`

| Maven 命令 | 是否编译前端 |
|-----------|------------|
| `mvn compile` | 是 |
| `mvn package` | 是 |
| `mvn install` | 是 |

## 核心架构模式

- **分层设计**：Core → Boot → Cloud / AI，下层不依赖上层
- **Spring Boot 4 auto-configuration**：通过 `AutoConfiguration.imports` 注册 `@AutoConfiguration`/`@Configuration`，用户通过 `@EnableXxx` 或配置属性控制。Boot 层注册 16 条顶层 AutoConfiguration（Base、Trace、Web、Mate、NoSql、JDBC、Mybatis、Template、UID、Mqtt、Config、AdminServer、MagicAPI、Swagger、ConfigPlugin、WS），AI 层与 Cloud 层各自独立注册
- **Magic-API 插件体系**：每个插件独立模块，扩展 magic-api 的能力（数据源、任务、WebSocket、消息队列、CDC 等 20+ 种资源类型）
- **Spring 7 / Boot 4 关键适配**：尾斜杠匹配 API 移除（vendored Mapping 遮蔽）、Jackson 2/3 双命名空间共存、Spring Cloud Alibaba nacos config 钉版等，落地记录见 `docs/springboot4-upgrade-notes.md`
- **灰度发布**：在 `fw-plus-cloud-autoconfigure` 实现灰度负载均衡与标签传递
- **MyBatis-Plus 增强**：base mapper/entity、字段加密（`@FieldEncrypt`）、数据变更追踪、数据权限过滤
- **Easy-Trans**：easy-trans 4.0.2（`org.dromara` 包）字典翻译注解体系；引用方需把 `com.fhs.*` import 改为 `org.dromara.*`
- **Sa-Token**：Core 提供认证基础与 Sa-Token 扩展，Boot 提供 Magic-API Sa-Token 插件
- **注意事项**：旧文档中的 JimuReport 破解体系已随 `fw-plus-report` 模块移除而不再适用

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
   - `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/` — AI 层自动配置（LangChain4j、Spring AI、AgentScope、向量存储等 @MagicModule）
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
