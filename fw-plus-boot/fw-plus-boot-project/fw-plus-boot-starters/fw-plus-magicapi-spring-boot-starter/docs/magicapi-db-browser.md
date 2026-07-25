# Magic-API DB 管理使用说明

本文说明 Magic-API 控制台 DB 管理的使用方式、只读边界和插件归属。

## 功能范围

DB 管理复用已注册的 JDBC 数据源，不新增第二套数据源配置。控制台用户可以在插件入口中：

- 选择已注册 JDBC 数据源。
- 浏览 catalog、schema、表、视图、函数和存储过程元数据。
- 查看表/视图字段、主键和索引信息。
- 分页预览表/视图数据。
- 对预览数据使用基础排序、LIKE 过滤和分页。

不同 JDBC 驱动返回的 `DatabaseMetaData` 字段可能不完整。字段、索引、备注等可选信息为空时属于兼容行为。

## 只读边界

DB 管理不是 SQL 客户端。后端只接受结构化浏览参数，不接收任意 SQL 文本。

明确不支持：

- INSERT、UPDATE、DELETE。
- DDL，例如 CREATE、ALTER、DROP。
- 任意 SQL 执行。
- 函数调用或存储过程调用。
- 数据库迁移、结构变更或数据维护操作。

表/视图预览由后端根据 JDBC 元数据构建白名单，只允许访问元数据中存在的表、视图和字段。过滤值通过 `PreparedStatement` 参数绑定，排序字段必须来自字段白名单。

## 控制台入口

DB 管理前端 UI 位于独立插件模块：

```text
fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-db/src/console
```

构建产物：

```text
magic-db.1.0.0.iife.js
```

插件资源存在时，注册名与后端配置保持一致：

```java
new Plugin("db", "MagicDB", "magic-db.1.0.0.iife.js")
```

中文控制台入口展示为 `db管理`。

使用应用需要引入 DB 插件 JAR 才会提供前端资源：

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.boot</groupId>
    <artifactId>fw-plus-magicapi-plugin-db</artifactId>
</dependency>
```

DB 管理后端位于 `fw-plus-boot-autoconfigure` 的 Magic-API 插件包目录：

```text
fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/db
```

`MagicApiPluginAutoConfiguration` 负责导入 DB 管理配置类。宿主控制台不维护 DB 浏览业务页面、DB 专属工具栏逻辑或 DB 专属 i18n 文案；宿主只负责通用插件加载和挂载。

## 后端接口

所有 DB 浏览接口都要求 Magic-API 控制台登录，并校验 `Authorization.VIEW` 权限。

| 接口 | 用途 |
| --- | --- |
| `GET /db/browser/datasources` | 列出已注册 JDBC 数据源 |
| `GET /db/browser/namespaces` | 列出 catalog/schema |
| `GET /db/browser/objects` | 列出表/视图 |
| `GET /db/browser/routines` | 列出函数/存储过程元数据 |
| `GET /db/browser/object/detail` | 查看表/视图字段、主键和索引 |
| `POST /db/browser/preview` | 分页预览表/视图数据 |

## 数据预览参数

预览请求使用结构化 JSON 参数：

```json
{
  "datasourceKey": "default",
  "catalog": "catalog_name",
  "schema": "schema_name",
  "objectName": "sys_user",
  "objectType": "TABLE",
  "page": 1,
  "pageSize": 20,
  "sortColumn": "id",
  "sortDirection": "DESC",
  "filters": [
    { "column": "name", "operator": "like", "value": "%admin%" }
  ]
}
```

说明：

- `objectType` 只允许 `TABLE` 或 `VIEW`。
- `pageSize` 有后端上限，超过上限会被限制。
- `sortColumn` 必须是目标对象的真实字段。
- `filters` 只允许后端支持的简单操作符。

## S3 插件 UI 边界

S3 文件管理 UI 已迁移到 S3 插件模块：

```text
fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console
```

宿主控制台已移除 S3 文件管理业务页面、S3 专属 toolbar/status 入口、S3 专属图标和 S3 业务 i18n 文案。S3 插件通过自己的 IIFE 资源注册文件管理入口，宿主继续保留通用插件 `toolbars`、`resource`、`datasources` 加载能力。

## 构建验证

常用验证命令：

```bash
(cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure && mvn -Dtest=MagicDbBrowserControllerTest,DefaultDbBrowserServiceTest,DbBrowserSqlBuilderTest test)
(cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-db/src/console && npm test && npm run build)
(cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console && npm test && npm run build)
(cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console && npm run build)
```
