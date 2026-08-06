# Magic-API dbtx 与 Seata 配置

`fw-plus-magicapi-spring-boot-starter` 内置 `dbtx` 脚本模块。默认启用本地协调器；它不引入 Seata 运行时依赖，也不创建额外的 Magic-API 插件或 starter。

## 默认配置

以下是本地事务的默认配置，可按需显式写入应用配置：

```yaml
magic-api:
  dbtx:
    enabled: true
    default-coordinator: local
    local-enabled: true
    seata-enabled: false
```

`default-coordinator` 默认为 `local`。`enabled: false` 会关闭 dbtx 与 Seata 后端；`local-enabled: false` 时，默认协调器必须改为已启用的其他协调器，否则应用启动失败。

## 脚本用法

`dbtx` 提供两个 `transaction` 重载。第一个使用 `default-coordinator`：

```javascript
return dbtx.transaction(['order', 'account'], () => {
    db['order'].update('update orders set status = 1 where id = ?', orderId);
    db['account'].update('update account set balance = balance - ? where id = ?', amount, accountId);
    return true;
});
```

第二个重载显式选择协调器，优先于 `default-coordinator`：

```javascript
return dbtx.transaction('seata', ['order', 'account'], () => {
    db['order'].update('update orders set status = 1 where id = ?', orderId);
    db['account'].update('update account set balance = balance - ? where id = ?', amount, accountId);
    return true;
});
```

数据源 key 必须非空且不重复。回调必须在调用 `transaction` 的同一线程内同步执行；dbtx 上下文不会传播到异步任务、线程池或其他线程。不要把后续异步工作当作本次事务的一部分。

嵌套调用不会开启新事务：内层必须使用和外层相同的协调器，且内层数据源 key 必须是外层声明 key 的子集。内层回调抛出异常会将外层事务标记为仅回滚。

## 本地协调器

`local` 为每个声明的数据源创建 Spring 本地事务。它适合单库事务，或能接受跨库尽力提交结果的场景；它不是分布式二阶段提交。多个数据源提交过程中若已有参与者提交、随后某个参与者提交失败，模块会抛出 `DBTX_HEURISTIC_MIXED`。此时业务数据可能处于已提交和已回滚并存的状态，必须按业务规则补偿或核对。

不要在已有 Spring 事务中再启动 `dbtx.transaction(...)`；本地协调器会拒绝这种环境事务嵌套。

## Seata AT 协调器

需要 Seata 时，由业务应用直接添加依赖，主 starter 不传递该依赖：

```xml
<dependency>
    <groupId>io.seata</groupId>
    <artifactId>seata-spring-boot-starter</artifactId>
</dependency>
```

然后开启 dbtx Seata 协调器，并通过标准 `seata.*` 配置一个逻辑事务集群。例如，所有参与同一全局事务的服务使用相同的事务组，并将该事务组映射到同一个集群名：

```yaml
magic-api:
  dbtx:
    seata-enabled: true

seata:
  tx-service-group: magic-api-dbtx
  service:
    vgroup-mapping:
      magic-api-dbtx: default
  registry:
    type: file
  config:
    type: file
```

按实际 Seata Server 的注册与配置中心替换 `registry`、`config` 的 `file` 配置；保持一个 `tx-service-group` 到一个逻辑集群的映射。Seata AT 会为参与的数据源准备 `DataSourceProxy`，仅支持 Seata AT 支持的数据库类型。

在每个参与 Seata AT 事务的 MySQL 业务库执行主 starter 随附的 SQL：`META-INF/sql/seata/mysql-undo-log.sql`。该 `undo_log` 表保存 AT 回滚信息；未创建时不要启用 Seata AT 事务。

已被 Seata 准备过的 `DataSourceProxy` 不能通过 Magic-API 热删除。停止应用并重启后再调整该数据源，避免留下无法安全注销的代理。
