# Brainstorm Summary

- Change: add-magicapi-db-browser
- Date: 2026-07-24

## 确认的技术方案

已确认采用“后端通用 JDBC 元数据服务 + 受控只读预览 SQL Builder + 独立 magic-db 控制台插件 + S3 UI 迁回插件”的方案。后端复用现有 MagicDynamicDataSource 和 datasource 注册体系，前端参照 magic-s3 组织 DB 插件入口，并修正 magic-s3 自身 UI 归属。UI 必须写到插件 UI 里面，不写到宿主 UI；现有 magic-s3 文件管理 UI 必须迁移到 `fw-plus-magicapi-plugin-s3/src/console` 插件 UI，宿主控制台中已有的 S3 页面、状态栏/工具栏入口和 S3 专属文案需要清理。

## 关键取舍与风险

不开放任意 SQL；以 DatabaseMetaData 为主路径，必要时预留方言扩展；数据预览必须做 identifier 白名单、quote 处理、PreparedStatement 参数绑定和最大 pageSize 限制。S3 UI 迁移风险是原宿主工具栏/状态栏入口变化，需在 S3 插件 IIFE 内保留等价入口和页面能力。

## 测试策略

后端以 H2 或可控 DataSource 做单元测试，覆盖元数据、预览分页、安全过滤和权限；前端至少覆盖 DB 插件构建/smoke、S3 插件构建/smoke，以及错误态、空态、当前选择保持。

## Spec Patch

已回写：DB 浏览 UI 必须位于插件 UI；S3 文件管理 UI 必须迁移到 `fw-plus-magicapi-plugin-s3/src/console`，宿主控制台不得保留 S3 业务页面和专属入口。
