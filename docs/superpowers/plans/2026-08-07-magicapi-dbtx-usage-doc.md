# Magic-API DBTX Usage Documentation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 Magic-API DBTX 完整使用说明迁入 `fw-plus-magicapi-spring-boot-starter` 工程，并把根目录旧文档改为稳定跳转入口。

**Architecture:** starter 工程内的 `docs/magicapi-dbtx.md` 是唯一完整内容源，按业务使用顺序覆盖 local、Seata、配置、脚本和排错。根目录 `docs/magicapi-dbtx.md` 不复制正文，只保留迁移说明和相对链接，避免双份文档漂移。

**Tech Stack:** Markdown、Magic-API 2.2.2、Spring Boot 2.7.18、Seata 2.0.0、Magic Script

## Global Constraints

- 配置前缀固定为 `magic-api.dbtx`。
- 默认值固定为 `enabled=true`、`default-coordinator=local`、`local-enabled=true`、`seata-enabled=false`、`timeout-ms=60000`、`name-prefix=magic-api-dbtx`。
- 主 starter 不传递 Seata；业务应用自行引入 `io.seata:seata-spring-boot-starter`，版本由项目 BOM 管理。
- 一 JVM 只连接一个 Seata 逻辑集群，不描述脚本级 Server 切换。
- local 是多本地事务的尽力提交，不描述为原子分布式事务。
- 只修改两份 Markdown 文档，不修改 Java、POM、前端资源或 SQL。
- 保留 README、TinyFlow、Java Entity、MyBatis Plus/Flex 等共享工作区改动，不暂存、不提交。

---

### Task 1: Migrate and Complete the DBTX Usage Guide

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/docs/magicapi-dbtx.md`
- Modify: `docs/magicapi-dbtx.md`

**Interfaces:**
- Consumes: `dbtx.transaction(Collection<String>, Function)` 与 `dbtx.transaction(String, Collection<String>, Function)`。
- Consumes: `magic-api.dbtx.*` 配置和主 starter 资源 `META-INF/sql/seata/mysql-undo-log.sql`。
- Produces: starter 内唯一完整 DBTX 使用文档和根目录兼容入口。

- [ ] **Step 1: 创建 starter 主文档并写入快速开始**

创建 `docs/magicapi-dbtx.md`，标题为 `# Magic-API DBTX 多数据源事务使用说明`。开头明确：

- DBTX 已内置于 `fw-plus-magicapi-spring-boot-starter`，无需单独 DBTX/Seata 插件或 starter。
- 默认使用 `local`；业务可在脚本中显式选择 `local` 或 `seata`。
- `local` 适合单库或允许补偿的多库操作，Seata AT 用于需要全局事务协调的受支持数据库。

给出主 starter 依赖，版本由项目 BOM 管理：

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.boot</groupId>
    <artifactId>fw-plus-magicapi-spring-boot-starter</artifactId>
</dependency>
```

给出两个 Magic-API JDBC 数据源示例，说明可在控制台创建，也可由外部 `resources/datasource/` 加载；key 必须分别为 `order` 和 `account`：

```json
{
  "name": "订单库",
  "key": "order",
  "url": "jdbc:mysql://127.0.0.1:3306/order_db?useSSL=false&serverTimezone=Asia/Shanghai",
  "username": "app",
  "password": "change-me",
  "driverClassName": "com.mysql.cj.jdbc.Driver",
  "type": "com.zaxxer.hikari.HikariDataSource"
}
```

```json
{
  "name": "账户库",
  "key": "account",
  "url": "jdbc:mysql://127.0.0.1:3306/account_db?useSSL=false&serverTimezone=Asia/Shanghai",
  "username": "app",
  "password": "change-me",
  "driverClassName": "com.mysql.cj.jdbc.Driver",
  "type": "com.zaxxer.hikari.HikariDataSource"
}
```

说明示例密码只能放在部署环境的安全配置中，不能提交真实凭据。

- [ ] **Step 2: 写入 local 配置和脚本示例**

写入完整默认配置：

```yaml
magic-api:
  dbtx:
    enabled: true
    default-coordinator: local
    local-enabled: true
    seata-enabled: false
    timeout-ms: 60000
    name-prefix: magic-api-dbtx
```

写入默认协调器脚本：

```javascript
return dbtx.transaction(['order', 'account'], () => {
    db['order'].update(
        'update orders set status = ? where id = ?',
        'PAID', orderId
    );
    db['account'].update(
        'update account set balance = balance - ? where id = ?',
        amount, accountId
    );
    return { success: true };
});
```

写入显式 local 示例，说明显式参数优先于 `default-coordinator`：

```javascript
return dbtx.transaction('local', ['order', 'account'], () => {
    // 同步执行所有数据库操作；抛出异常会触发回滚。
    return true;
});
```

明确数据源 key 非空、不可重复；DBTX 会排序参与 key 以获得稳定开启顺序，并按逆序完成事务。

- [ ] **Step 3: 写入配置表和启停规则**

添加配置表，逐项写明：

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `magic-api.dbtx.enabled` | `true` | DBTX 总开关；关闭后 local 和 Seata 均不注册。 |
| `magic-api.dbtx.default-coordinator` | `local` | 未显式指定协调器时使用的名称。 |
| `magic-api.dbtx.local-enabled` | `true` | 是否注册 local 协调器。 |
| `magic-api.dbtx.seata-enabled` | `false` | 是否尝试注册 Seata 协调器；还要求 Seata 客户端在 classpath。 |
| `magic-api.dbtx.timeout-ms` | `60000` | 事务超时时间，单位毫秒。 |
| `magic-api.dbtx.name-prefix` | `magic-api-dbtx` | 事务名称前缀。 |

说明启动校验：默认协调器不可用时应用启动失败；非默认协调器不可用时应用可以启动，但脚本显式调用会得到 `DBTX_COORDINATOR_UNAVAILABLE`。

- [ ] **Step 4: 写入 Seata AT 接入说明**

给出业务侧依赖：

```xml
<dependency>
    <groupId>io.seata</groupId>
    <artifactId>seata-spring-boot-starter</artifactId>
</dependency>
```

给出启用配置和一个标准事务组到 Server 集群的映射示例：

```yaml
magic-api:
  dbtx:
    seata-enabled: true
    # 可选：将未显式指定的调用默认交给 Seata
    # default-coordinator: seata

seata:
  enabled: true
  application-id: magic-api-service
  tx-service-group: magic-api-dbtx
  service:
    vgroup-mapping:
      magic-api-dbtx: default
    grouplist:
      default: 127.0.0.1:8091
  registry:
    type: file
  config:
    type: file
```

说明生产环境可将 `registry` 和 `config` 改为 Nacos 等受支持实现，但一个 JVM 只使用一个 Seata 客户端和一个逻辑集群。给出显式调用：

```javascript
return dbtx.transaction('seata', ['order', 'account'], () => {
    db['order'].update('update orders set status = ? where id = ?', 'PAID', orderId);
    db['account'].update(
        'update account set balance = balance - ? where id = ?',
        amount, accountId
    );
    return true;
});
```

说明每个参与 MySQL 业务库必须执行 classpath 资源 `META-INF/sql/seata/mysql-undo-log.sql`，并说明 Seata AT 只支持 Seata 客户端支持的数据库类型。

- [ ] **Step 5: 写入事务边界、热更新限制和错误码**

写明以下边界：

- callback 必须同步执行，DBTX 上下文不传播到线程池、异步任务或其他线程。
- 嵌套调用必须使用同一协调器，内层 key 必须是外层 key 的子集；内层异常会将外层标记为 rollback-only。
- local 不允许在已有 Spring 环境事务中再次启动。
- local 多库提交可能产生 `DBTX_HEURISTIC_MIXED`，业务必须补偿或人工核对。
- 正在事务中的数据源替换/删除会被拒绝；Seata 已 prepare 的 `DataSourceProxy` 不能热删除，调整后应重启应用。

添加错误码表，覆盖 `DbTxErrorCode` 的全部枚举值：

`DBTX_VALIDATION_FAILED`、`DBTX_COORDINATOR_DISABLED`、`DBTX_COORDINATOR_UNAVAILABLE`、`DBTX_AMBIENT_TRANSACTION`、`DBTX_BEGIN_FAILED`、`DBTX_NESTED_SCOPE_EXPANSION`、`DBTX_NESTED_MODE_MISMATCH`、`DBTX_UNDECLARED_DATASOURCE`、`DBTX_ROLLBACK_ONLY`、`DBTX_COMMIT_FAILED`、`DBTX_HEURISTIC_MIXED`、`DBTX_ROLLBACK_FAILED`。

每个错误码写一行触发原因和处理建议，不复制 Java 堆栈。

- [ ] **Step 6: 将根目录旧文档改为跳转入口**

将 `docs/magicapi-dbtx.md` 替换为：

```markdown
# Magic-API DBTX 使用说明

完整文档已迁移到 `fw-plus-magicapi-spring-boot-starter` 工程：

[查看 Magic-API DBTX 多数据源事务使用说明](../fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/docs/magicapi-dbtx.md)
```

- [ ] **Step 7: 验证内容、链接和资源路径**

运行：

```bash
test -f fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/docs/magicapi-dbtx.md
test -f fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/main/resources/META-INF/sql/seata/mysql-undo-log.sql
rg -n "dbtx\.transaction|default-coordinator|timeout-ms|DBTX_HEURISTIC_MIXED|mysql-undo-log" \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/docs/magicapi-dbtx.md
rg -n "fw-plus-magicapi-spring-boot-starter/docs/magicapi-dbtx\.md" docs/magicapi-dbtx.md
git diff --check -- \
  docs/magicapi-dbtx.md \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/docs/magicapi-dbtx.md
```

Expected: 两个 `test` exit 0；主文档命中两个 transaction 重载、配置、错误码和 SQL 资源；根文档命中唯一主文档链接；`git diff --check` 无输出。

- [ ] **Step 8: 精确提交两份使用文档**

```bash
git add -- \
  docs/magicapi-dbtx.md \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/docs/magicapi-dbtx.md
git diff --cached --name-status
git diff --cached --check
git commit -m "docs(magicapi): 完善 DBTX 使用说明"
```

Expected: staged/commit 只包含上述两个 Markdown 文件。
