## Context

当前 DB 管理 UI 已经放在 `fw-plus-magicapi-plugin-db/src/console`，但后端代码仍位于 `fw-plus-boot-autoconfigure/src/main/java/.../magicapi/dbbrowser`。需要把后端移动到 autoconfigure 的 Magic-API 插件包目录，使其与 S3、Debezium 等插件后端目录结构一致。

## Goals / Non-Goals

**Goals:**

- DB 管理后端源码和测试迁移到 `fw-plus-boot-autoconfigure` 的 `magicapi/plugin/db` 目录。
- 保持已有 `/db/browser/**` 接口兼容。
- 控制台 toolbar 名称显示为 `db管理`。

**Non-Goals:**

- 不重做 DB 管理 UI。
- 不改变 JDBC datasource 保存、注册和 Magic 脚本侧 `db` 使用方式。
- 不新增数据库写操作或 SQL 客户端能力。

## Decisions

- 使用 `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db` 作为新包名前缀，和已有 Magic-API 插件包命名保持一致。
- 由 `MagicApiPluginAutoConfiguration` 导入 DB 管理配置，保持 Magic-API 插件后端的集中注册方式。
- `fw-plus-magicapi-plugin-db` 只维护控制台插件 UI，不维护 Java 后端源码或 Spring Boot 自动配置声明。

## Risks / Trade-offs

- [Risk] 应用如果未引入 `fw-plus-magicapi-plugin-db`，将没有 DB 管理控制台前端资源。
  Mitigation: 文档中保留插件依赖说明，后端配置仍由 autoconfigure 集中导入。
- [Risk] 包名迁移可能漏改测试或文档引用。
  Mitigation: 使用全局检索和模块测试覆盖旧包名残留。
