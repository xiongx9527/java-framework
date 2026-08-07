# Magic-API DBTX 多数据源事务使用说明

`fw-plus-magicapi-spring-boot-starter` 已内置 `dbtx` 脚本模块，不需要再引入独立的 DBTX/Seata 插件或 starter。

DBTX 支持两种事务协调器：

| 协调器 | 默认启用 | 适用场景 |
| --- | --- | --- |
| `local` | 是 | 单库事务，或允许失败后补偿、核对的多库操作 |
| `seata` | 否 | 需要 Seata AT 全局事务协调，且数据库类型受 Seata 支持 |

默认协调器是 `local`。业务可以修改默认值，也可以在每次脚本调用时显式选择 `local` 或 `seata`。

> `local` 会协调多个 Spring 本地事务，但不是分布式二阶段提交。跨库提交过程中仍可能出现部分库已提交、部分库回滚的结果。

## 1. 引入 starter

项目使用 fw-plus BOM 管理版本时，只需引入主 starter：

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.boot</groupId>
    <artifactId>fw-plus-magicapi-spring-boot-starter</artifactId>
</dependency>
```

local 所需能力已经包含在主 starter 中。只有使用 Seata 时，才需要业务应用另外引入 Seata 客户端。

## 2. 准备 Magic-API JDBC 数据源

事务中使用的数据源必须先注册到 Magic-API 动态数据源中。可以在 Magic-API 控制台创建，也可以由运行环境挂载的 `resources/datasource/` 目录加载。

以下示例准备两个数据源，key 分别为 `order` 和 `account`。

`resources/datasource/order.json`：

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

`resources/datasource/account.json`：

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

示例中的地址、账号和密码仅用于说明。真实凭据应由部署环境或配置中心提供，不要提交到代码仓库。

DBTX 会装饰通过 Magic-API 注册的数据源，以提供事务作用域、Seata 代理和数据源生命周期保护。不要把未经过 DBTX 装饰的外部 `DataSource` 节点直接加入事务。

## 3. 默认 local 配置

DBTX 默认已经启用。以下配置展示了全部默认值，可以按需显式写入 `application.yml`：

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

### 配置项

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `magic-api.dbtx.enabled` | `true` | DBTX 总开关；关闭后 local 与 Seata 后端均不注册 |
| `magic-api.dbtx.default-coordinator` | `local` | 未在脚本中显式指定协调器时使用的名称 |
| `magic-api.dbtx.local-enabled` | `true` | 是否注册 local 协调器 |
| `magic-api.dbtx.seata-enabled` | `false` | 是否启用 Seata 协调器；还要求 Seata 客户端存在于 classpath |
| `magic-api.dbtx.timeout-ms` | `60000` | 事务超时时间，单位毫秒 |
| `magic-api.dbtx.name-prefix` | `magic-api-dbtx` | 事务名称前缀 |

应用启动时会校验默认协调器：

- 默认协调器可用：正常启动。
- 默认协调器不可用：应用启动失败，避免接口运行后才暴露配置错误。
- 非默认协调器不可用：应用仍可启动；脚本显式选择该协调器时返回 `DBTX_COORDINATOR_UNAVAILABLE`。

例如关闭 local 时，必须同时把默认协调器改为已启用且可用的协调器：

```yaml
magic-api:
  dbtx:
    default-coordinator: seata
    local-enabled: false
    seata-enabled: true
```

## 4. local 脚本用法

### 使用默认协调器

未传协调器名称时，使用 `magic-api.dbtx.default-coordinator`：

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

### 显式选择 local

显式传入的协调器名称优先于默认配置：

```javascript
return dbtx.transaction('local', ['order', 'account'], () => {
    db['order'].update(
        'update orders set status = ? where id = ?',
        'CANCELLED', orderId
    );
    db['account'].update(
        'update account set balance = balance + ? where id = ?',
        amount, accountId
    );
    return true;
});
```

callback 抛出异常时，local 会回滚已经开启的参与事务：

```javascript
return dbtx.transaction(['order', 'account'], () => {
    db['order'].update('update orders set status = ? where id = ?', 'PAID', orderId);

    var affected = db['account'].update(
        'update account set balance = balance - ? where id = ? and balance >= ?',
        amount, accountId, amount
    );
    if (affected != 1) {
        throw '账户余额不足';
    }
    return true;
});
```

使用规则：

- 数据源 key 集合不能为空。
- key 不能为空或重复。
- callback 中使用的 JDBC 数据源必须包含在声明的 key 集合中。
- DBTX 会按 key 排序后开启参与事务，并按逆序完成提交或回滚。
- callback 必须同步执行；不要在 callback 中启动线程池或异步任务并把它们当作本次事务的一部分。

### local 的多库提交语义

local 会分别提交各数据源的 Spring 本地事务。如果某个参与者提交失败：

- 尚无参与者确认提交时，抛出 `DBTX_COMMIT_FAILED`。
- 已有参与者提交成功时，抛出 `DBTX_HEURISTIC_MIXED`。

`DBTX_HEURISTIC_MIXED` 表示业务数据可能同时存在“已提交”和“已回滚”的库。应用必须根据订单号、业务流水号等幂等标识执行补偿或人工核对，不能把它当成全部回滚。

## 5. Seata AT 接入

### 5.1 引入 Seata 客户端

主 starter 不传递 Seata。业务应用需要自行引入：

```xml
<dependency>
    <groupId>io.seata</groupId>
    <artifactId>seata-spring-boot-starter</artifactId>
</dependency>
```

Seata 版本由 fw-plus 项目 BOM 管理，当前框架基线为 Seata `2.0.0`。

### 5.2 配置 Seata Server

下面示例使用 file 模式直接连接 `127.0.0.1:8091`，并把事务组 `magic-api-dbtx` 映射到逻辑集群 `default`：

```yaml
magic-api:
  dbtx:
    seata-enabled: true
    # 可选：让未显式指定协调器的脚本默认使用 Seata
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

生产环境可以按 Seata 官方方式把 `registry`、`config` 改为 Nacos 等实现。所有参与同一全局事务的应用必须使用一致的事务组映射，并连接同一个逻辑 Seata 集群。

当前设计在一个 JVM 中只使用一个 Seata 客户端和一个逻辑集群，不支持在 Magic Script 中按请求切换 Seata Server。

### 5.3 初始化 undo_log

每个参与 Seata AT 事务的 MySQL 业务库都必须创建 `undo_log` 表。主 starter 已随包提供 SQL：

```text
META-INF/sql/seata/mysql-undo-log.sql
```

在所有参与库执行该 SQL 后再启用 Seata AT。缺少 `undo_log` 时，Seata 无法正确记录和回滚分支事务。

Seata AT 仅支持 Seata 客户端支持的数据库类型。DBTX 会在数据源首次参加 Seata 事务时准备 `DataSourceProxy`。

### 5.4 显式使用 Seata

```javascript
return dbtx.transaction('seata', ['order', 'account'], () => {
    db['order'].update(
        'update orders set status = ? where id = ?',
        'PAID', orderId
    );
    db['account'].update(
        'update account set balance = balance - ? where id = ?',
        amount, accountId
    );
    return true;
});
```

也可以将 `default-coordinator` 配置为 `seata`，然后使用不带协调器参数的重载。

## 6. 嵌套事务与线程边界

嵌套 `dbtx.transaction(...)` 不会开启第二个物理事务，必须满足：

- 内层与外层使用相同协调器。
- 内层数据源 key 必须是外层声明 key 的子集，不能扩大事务范围。
- 内层 callback 抛出异常后，外层事务会被标记为 rollback-only。即使外层脚本捕获了内层异常，最终也不能提交。

示例：

```javascript
return dbtx.transaction('local', ['order', 'account'], () => {
    db['order'].update('update orders set status = ? where id = ?', 'PAID', orderId);

    dbtx.transaction('local', ['account'], () => {
        db['account'].update(
            'update account set balance = balance - ? where id = ?',
            amount, accountId
        );
    });
    return true;
});
```

DBTX 上下文绑定当前线程，不会自动传播到：

- 自建线程。
- 线程池任务。
- 异步回调。
- 消息发送后的消费者线程。

需要异步执行时，应在当前事务提交后发送包含业务幂等键的消息，由消费者建立自己的事务边界。

local 也不允许在已有 Spring 环境事务中再次启动 `dbtx.transaction(...)`，否则抛出 `DBTX_AMBIENT_TRANSACTION`。

## 7. 动态数据源热更新限制

DBTX 会保护参与事务的数据源生命周期：

- local 事务持有数据源租约期间，替换或删除该节点会被拒绝。
- 数据源替换、删除和 DBTX 装饰元数据更新按同一生命周期边界执行，避免事务使用到错误节点。
- 已为 Seata 准备过的 `DataSourceProxy` 不能通过 Magic-API 热删除。

需要调整已参加 Seata 事务的数据源时，建议停止应用、修改数据源配置并重新启动。不要在流量处理中强制替换连接池或代理。

## 8. 错误码与处理建议

DBTX 业务异常类型为 `DbTxException`，可通过 `getDbTxCode()` 获取错误码。Magic-API 的统一异常响应如何呈现该字段，取决于应用自身的异常处理配置。

| 错误码 | 含义 | 处理建议 |
| --- | --- | --- |
| `DBTX_VALIDATION_FAILED` | 参数校验失败，例如 key 为空、重复或 callback 为空 | 修正脚本参数，不要重试相同请求 |
| `DBTX_COORDINATOR_DISABLED` | 协调器被配置为禁用；当前作为协调器禁用语义的保留错误码 | 检查 `local-enabled`、`seata-enabled` 和总开关 |
| `DBTX_COORDINATOR_UNAVAILABLE` | 请求的协调器未注册或 classpath 条件不满足 | 检查协调器名称、开关和 Seata 客户端依赖 |
| `DBTX_AMBIENT_TRANSACTION` | local 检测到当前线程已有 Spring 环境事务 | 统一事务入口，不要在现有 `@Transactional` 中再开启 DBTX |
| `DBTX_BEGIN_FAILED` | 获取数据源、租约、连接或开启 local/Seata 事务失败 | 检查数据源是否通过 Magic-API 注册、连接池状态和 Seata Server |
| `DBTX_NESTED_SCOPE_EXPANSION` | 嵌套调用声明了外层没有包含的数据源 | 把完整 key 集合放到最外层事务 |
| `DBTX_NESTED_MODE_MISMATCH` | 嵌套调用切换了协调器 | 内外层使用相同的 `local` 或 `seata` |
| `DBTX_UNDECLARED_DATASOURCE` | 使用了事务声明范围外的数据源；当前保留为声明范围错误语义 | 把实际使用的数据源 key 加入最外层声明，并避免动态拼接未知 key |
| `DBTX_ROLLBACK_ONLY` | 嵌套异常等原因已将事务标记为只能回滚 | 不要吞掉内层异常后继续提交，按失败处理请求 |
| `DBTX_COMMIT_FAILED` | 提交失败，且没有已确认提交的 local 参与者；Seata 提交失败也使用该码 | 查询日志和 Seata XID/status，确认最终状态后决定重试或补偿 |
| `DBTX_HEURISTIC_MIXED` | local 多库提交发生部分成功、部分失败 | 立即按业务流水核对并执行幂等补偿 |
| `DBTX_ROLLBACK_FAILED` | local 回滚过程中出现失败 | 检查连接状态并人工核对受影响数据；保留原异常及 suppressed 信息 |

## 9. 常见问题

### 应用启动时报默认协调器不可用

检查 `default-coordinator` 对应的协调器是否启用。默认配置使用 local；若改为 Seata，需要同时满足：

- `magic-api.dbtx.seata-enabled=true`。
- 已引入 `seata-spring-boot-starter`。
- Seata 配置能够创建客户端。

### local 开始事务时报数据源不是 dbtx-aware

确认数据源是通过 Magic-API 动态数据源注册流程创建的。不要绕过 registry 直接放入一个未经过 DBTX decorator 的 `DataSource` 节点。

### Seata 调用时报 `DBTX_BEGIN_FAILED`

依次检查：

1. Seata Server 地址和注册中心是否可达。
2. `tx-service-group` 与 `vgroup-mapping` 是否一致。
3. 参与库是否已创建 `undo_log`。
4. 数据库类型是否受 Seata AT 支持。
5. 数据源账号是否具备业务表与 `undo_log` 所需权限。

### 为什么 local 不能保证两个库同时提交

local 使用多个独立的 Spring 本地事务，没有全局事务协调器。需要原子分布式协调时使用 Seata AT；不能使用 Seata 时，应设计业务流水、幂等重试和补偿任务。

## 10. 完整示例配置

local 与 Seata 可以同时启用，由默认配置或脚本调用选择：

```yaml
magic-api:
  dbtx:
    enabled: true
    default-coordinator: local
    local-enabled: true
    seata-enabled: true
    timeout-ms: 60000
    name-prefix: magic-api-dbtx

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

此时：

- `dbtx.transaction(keys, callback)` 使用 local。
- `dbtx.transaction('local', keys, callback)` 显式使用 local。
- `dbtx.transaction('seata', keys, callback)` 显式使用 Seata。
