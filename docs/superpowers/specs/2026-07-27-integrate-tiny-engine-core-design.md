---
comet_change: integrate-tiny-engine-core
role: technical-design
canonical_spec: openspec
archived-with: 2026-07-28-integrate-tiny-engine-core
status: final
---

# TinyEngine Core 深度技术设计

## 目标与边界

本设计将 `tiny-engine-backend-java` 的低代码后端基础能力提炼到 `fw-plus-core`，形成可被 `fw-plus-boot` starter 和后续 `fw-plus-ai` 扩展复用的底层库能力。目标不是把原 Spring Boot 应用整体搬入仓库，而是把其领域模型、DTO、Mapper/XML、SQL 资源、核心服务、动态模型和安全边界改造成 Java 8 / Spring Boot 2.7.18 兼容的 core artifact。

本 change 明确不包含 Controller、Spring Boot 自动配置、starter、启动类、OpenAPI、Servlet Filter、profile 配置、Docker 部署、AI Chat、RAG、VectorStorage 和 LangChain4j 1.x。Web 暴露由 `add-tiny-engine-boot-starter` 处理，AI 能力由 `add-tiny-engine-ai-starter` 处理。

## 模块方案

采用中间方案：在 `fw-plus-core/fw-plus-project` 下新增聚合模块 `fw-plus-tiny-engine`，首版只包含 `fw-plus-tiny-engine-core` 子模块。

```text
fw-plus-core/fw-plus-project/
└── fw-plus-tiny-engine/
    ├── pom.xml
    └── fw-plus-tiny-engine-core/
        ├── pom.xml
        └── src/main/
            ├── java/com/xiongx9527/fw/plus/tinyengine/...
            └── resources/
```

这样做有三个好处：第一版对 Boot/AI 的依赖面只有一个稳定 artifact；未来如动态模型、SQL 资源或领域服务需要拆成独立 artifact，可以在同一聚合模块下扩展；同时不会把 TinyEngine 领域代码塞进现有 `fw-plus-mp`、`fw-plus-jdbc` 或 `fw-plus-common`，避免污染通用模块。

`fw-plus-project/pom.xml` 增加 `fw-plus-tiny-engine` 模块。`fw-plus-dependencies` 托管 `fw-plus-tiny-engine-core`，版本沿用 `${revision}`。`fw-plus-tiny-engine-core` 父 POM 使用 `fw-plus-parent`，依赖优先复用 `fw-plus-common`、`fw-plus-mp`、MyBatis/MyBatis-Plus、Hutool、Fastjson 等 core BOM 已有版本。

## 包结构

包名统一迁移到 `com.xiongx9527.fw.plus.tinyengine`，内部按职责分层：

```text
common        Result、异常、枚举、上下文、TypeHandler、基础工具
model.entity 低代码领域实体
model.dto    REST/service 入参出参 DTO，但不包含 Web 注解职责
mapper       MyBatis/MyBatis-Plus Mapper 接口
service      核心领域服务接口
service.impl 核心领域服务实现
dynamic      动态模型 DTO、DAO、SQL 构建、服务和校验
support      资源清单、迁移清单、上下文 SPI、Hook SPI
```

迁移时不保留 `com.tinyengine.it` 包名。Mapper XML 的 namespace、resultMap、typeHandler 和实体全限定名必须同步改成新包名。

## 迁移分层

### 公共基础层

迁移 `Result`、`BaseEntity`、`HistoryEntity`、`PageQueryVo`、`ExceptionEnum`、`ServiceException`、`IBaseError`、`LoginUserContext`、TypeHandler、JSON/Schema/Hash/文件安全等工具。进入 core 后要去掉 Web/OpenAPI 强依赖，例如 `@ResponseStatus`、Swagger/OpenAPI 注解、Servlet 相关类型。HTTP 状态、统一异常响应和 Controller advice 由 Boot 层适配。

`LoginUserContext` 不直接绑定参考项目登录实现，改成可替换上下文 SPI，例如 `TinyEngineUserContext`。默认实现可以返回空用户或系统用户，Boot starter 后续再接入 Sa-Token、Security 或宿主上下文。

### 领域模型与持久化层

实体、DTO、Mapper 接口和 XML 按领域迁移，覆盖 app、page、page history、page template、canvas、block、block group、material、material history、component、component library、resource、resource group、business category、i18n、platform、tenant、user、extension、task record、model、datasource 和关联关系。

Mapper XML 放在稳定 classpath 路径，例如：

```text
classpath*:mapper/tinyengine/**/*.xml
```

SQL 初始化资源作为可发现资源分发，不在 core 自动执行。推荐路径：

```text
META-INF/tiny-engine/sql/mysql/
META-INF/tiny-engine/sql/h2/
META-INF/tiny-engine/sql/postgresql/
META-INF/tiny-engine/sql/manifest.properties
```

manifest 记录脚本顺序和数据库类型，Boot starter 后续按配置决定是否加载或执行。

### 服务层

迁移 app/page/material/resource/i18n/platform/tenant/user/task/model/datasource 等核心服务接口与实现。服务实现可以使用 Spring `@Service` 和 Spring 事务注解，但不得依赖 Controller、OpenAPI、Servlet Filter、启动类或 profile 配置。

保存后 Hook、上下文读取、文件资源处理等可变点抽象成 SPI，默认实现保持最小能力。业务方或 Boot starter 可通过 Bean 覆盖扩展，不需要改 core 代码。

## 动态模型安全设计

动态模型是本 change 的最高风险区域。参考实现中 `DynamicSqlProvider` 对 table、field、orderBy、condition key 等 SQL 标识符存在拼接路径，迁移时必须先建立集中式安全边界，再迁移动态服务。

新增 `TinyEngineSqlIdentifierValidator`，统一校验：

- 模型英文名、动态表名、普通表名
- 字段名、条件 key、排序字段
- 排序方向，仅允许 `ASC` / `DESC`
- DDL 列名、批量写入字段、查询字段列表

标识符只允许单段或受控多段白名单形式，默认使用 Java 8 正则 `^[A-Za-z_][A-Za-z0-9_]*$`。不允许分号、注释、空白拼接、函数调用、表达式、引号、括号、关键字拼接或多语句片段。数据值必须通过 MyBatis 参数绑定传入，不能把用户值拼接到 SQL 字符串。

动态查询、插入、更新、删除、建表、改表、删表、初始化、结构读取和批量插入都必须经过同一校验器。`COUNT(*) as count` 这类聚合字段不应作为用户字段透传，而应由受控枚举或内部常量生成。

## Java 8 / Spring Boot 2.7 兼容

迁移时必须替换或排除不兼容 API：

- `jakarta.*` 改为 Spring Boot 2.7 可用的 `javax.*` 或 Spring 注解，事务使用 `org.springframework.transaction.annotation.Transactional`
- 禁止 `List.of()`、`Map.of()`、`String.isBlank()`、`InputStream.readAllBytes()`、`java.net.http.HttpClient` 等 Java 9+ API
- 不引入 Spring Boot 3、Jakarta EE 9+、LangChain4j 1.x、Springdoc OpenAPI 2.x
- MyBatis-Plus 版本收敛到 core BOM 当前 3.3.2 体系

对参考项目里的 Lombok、MapStruct、Druid、Fastjson、Hutool、Bouncycastle 等依赖逐项评估，优先复用 core BOM 已有版本；无法证明必要的依赖不进入首版 core。

## 实施顺序

1. 建立 Maven 模块和 BOM 注册，保证空模块可编译。
2. 迁移公共基础类型和 Java 8 兼容工具，先去掉 Web/OpenAPI 强依赖。
3. 迁移领域实体、DTO、Mapper 接口、Mapper XML 和 SQL 资源 manifest。
4. 迁移核心服务接口与实现，剥离 Controller、启动应用和 profile 配置。
5. 建立动态 SQL 校验器，再迁移动态模型服务和 SQL provider。
6. 补测试和范围排除检查，最后跑 core 聚合验证。

## 测试策略

单元测试覆盖公共基础类型、TypeHandler、JSON/Schema/Hash/文件安全工具和 `TinyEngineSqlIdentifierValidator`。

动态 SQL 安全测试覆盖分号、注释、空白注入、函数调用、表达式、非法排序方向、非法 DDL 列名、非法批量字段和非法 condition key，确认 SQL 执行前被拒绝。合法用例覆盖动态分页查询、插入、更新、删除、结构读取和表生命周期操作。

持久化资源测试验证 Mapper XML 能被 MyBatis 加载，namespace 与新包名一致，SQL 资源 manifest 可发现且按数据库类型返回有序脚本。

构建验证至少运行新增模块编译和必要 core 聚合验证。若完整 core 测试耗时或依赖外部环境，可先运行新增模块测试，再记录未运行范围和原因。

## 回滚与兼容

本 change 以新增模块为主。回滚时从 `fw-plus-project/pom.xml`、`fw-plus-dependencies` 和 `fw-plus-tiny-engine` 聚合中移除新增模块即可，不影响现有 core 模块。

对上层模块的兼容契约是 `fw-plus-tiny-engine-core` artifact、`com.xiongx9527.fw.plus.tinyengine.*` 包名、Mapper XML 路径和 SQL manifest 路径。后续 Boot/AI change 不应引用 `tiny-engine-backend-java` 原仓库路径。

## 仍需实现阶段确认

- 最终 artifactId 是否采用 `fw-plus-tiny-engine-core`。
- `TinyEngineUserContext` 默认行为使用空用户、系统用户还是强制宿主提供。
- SQL manifest 具体格式使用 properties、JSON 还是 YAML。
- 参考项目中加密、文件安全、Schema 工具的依赖是否全部必要。
