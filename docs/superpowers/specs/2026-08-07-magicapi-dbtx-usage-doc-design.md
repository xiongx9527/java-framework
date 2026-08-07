# Magic-API DBTX 使用文档迁移设计

## 目标

在 `fw-plus-magicapi-spring-boot-starter` 工程内提供一份面向业务开发者的中文使用文档，使使用者无需阅读实现代码即可完成 local 多数据源事务、Seata AT 接入、脚本调用和常见问题排查。

## 方案选择

考虑过三种方案：

1. 只保留根目录 `docs/magicapi-dbtx.md`：无法满足文档随 starter 工程维护的要求。
2. 根目录和 starter 各保留一份完整文档：阅读方便，但后续容易出现内容漂移。
3. starter 内保留完整主文档，根目录文档改为跳转说明：只有一个内容源，同时保留旧路径的发现入口。

采用方案 3。

## 文件布局

- 主文档：`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/docs/magicapi-dbtx.md`
- 旧入口：`docs/magicapi-dbtx.md`，只保留迁移说明和相对链接。

## 文档内容

主文档按使用顺序组织：

1. 能力边界和适用场景。
2. 引入主 starter、配置 Magic-API JDBC 数据源。
3. 默认 local 的最小配置和脚本示例。
4. 显式选择 local/Seata 以及修改默认协调器。
5. `magic-api.dbtx.*` 全部配置项、默认值和启停关系。
6. Seata 客户端依赖、标准 `seata.*` Server/事务组配置、MySQL `undo_log` 初始化。
7. local 尽力提交语义、Seata AT 限制、嵌套事务、线程边界和数据源热更新限制。
8. 常用错误码、启动失败和运行时问题排查。

示例必须使用实际脚本签名：

```javascript
dbtx.transaction(['order', 'account'], callback)
dbtx.transaction('seata', ['order', 'account'], callback)
```

## 一致性约束

- 配置前缀固定为 `magic-api.dbtx`。
- 默认值与 `DbTxProperties` 一致：`enabled=true`、`default-coordinator=local`、`local-enabled=true`、`seata-enabled=false`、`timeout-ms=60000`、`name-prefix=magic-api-dbtx`。
- 主 starter 不传递 Seata；业务应用自行引入 `io.seata:seata-spring-boot-starter`，版本由项目 BOM 管理。
- 一 JVM 只连接一个 Seata 逻辑集群，不描述脚本级 Server 切换。
- 文档不得暗示 local 提供原子分布式提交。
- 文档不新增前端页面、插件或 starter。

## 验证

- 检查主文档与实现中的函数签名、配置默认值、错误码一致。
- 检查根目录跳转链接可解析到 starter 主文档。
- 检查 `undo_log` 资源路径存在。
- 运行 `git diff --check`。
- 文档变更不需要执行 Java 编译；若修改任何代码或 POM，则必须补充对应 Maven 验证。
