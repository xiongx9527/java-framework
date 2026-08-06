# Magic-API DBTX 多数据源事务模块设计

日期：2026-08-06
状态：已确认

## 1. 目标

新增独立插件模块 `fw-plus-magicapi-plugin-dbtx`，为 Magic-API 2.2.2 内部 JDBC 数据源提供统一的多数据源事务入口；另新增 `fw-plus-magicapi-plugin-seata`，为 dbtx 提供 Seata AT 全局事务实现。脚本可以在同一应用中按业务选择本地事务协调或 Seata 全局事务，也可以省略模式并使用配置的默认协调器。

本设计只覆盖同步、同线程执行的 Magic-Script 回调。异步任务、线程切换、消息最终一致性、TCC、Saga 和 XA/JTA 不在本次范围内。

## 2. 脚本 API

`dbtx` 只提供回调式事务 API，不提供容易遗漏清理的手动 `begin/commit/rollback` API。

```javascript
import dbtx;

// 使用配置的默认协调器
return dbtx.transaction(['order', 'account'], () => {
    db['order'].update('update orders set status = ? where id = ?', 'PAID', orderId);
    db['account'].update('update account set balance = balance - ? where id = ?', amount, accountId);
});
```

```javascript
// 显式选择协调器，覆盖默认配置
return dbtx.transaction('seata', ['order', 'account'], () => {
    db['order'].update(...);
    db['account'].update(...);
});
```

Java 侧提供两个重载：

```java
Object transaction(Collection<String> dataSourceKeys, Function<?, ?> callback);

Object transaction(String coordinator, Collection<String> dataSourceKeys,
                   Function<?, ?> callback);
```

协调器名称只接受 `local` 和 `seata`，不区分大小写。数据源 key 会去除首尾空白；空字符串表示 Magic-API 默认数据源。空列表、空回调、空元素、重复 key 和不存在的 key 均在事务开始前失败。

## 3. 配置

```yaml
magic-api:
  plugin:
    dbtx:
      enabled: true
      default-coordinator: local
      local-enabled: true
      timeout-ms: 60000
      name-prefix: magic-api-dbtx
    seata:
      enabled: true

seata:
  application-id: ${SEATA_APPLICATION_ID:fw-plus-magicapi}
  tx-service-group: ${SEATA_TX_SERVICE_GROUP:magicapi_tx_group}
  service:
    vgroup-mapping:
      magicapi_tx_group: ${SEATA_CLUSTER:default}
    grouplist:
      default: ${SEATA_SERVER_ADDR:127.0.0.1:8091}
```

- `default-coordinator` 默认是 `local`，只允许 `local` 或 `seata`。
- 显式脚本参数优先于 `default-coordinator`。
- 默认协调器被禁用时，应用启动失败。
- 默认协调器为 `seata` 时，若 Seata 类、客户端配置或协调器不可用，应用启动失败。
- 默认协调器为 `local` 时，缺少 Seata 依赖不影响本地模式启动；显式调用 `seata` 会返回 `DBTX_COORDINATOR_UNAVAILABLE`。
- Seata 插件使用标准 `seata.application-id`、`seata.tx-service-group`、注册中心和配置中心属性，不复制一套 Seata 配置。
- YAML 提供部署默认值，可通过环境变量、启动参数或配置中心覆盖。
- 一个 JVM 只初始化一个 Seata 客户端并连接一个逻辑集群；`grouplist` 可以包含该集群的多个 Server 节点以支持高可用，但脚本不能切换到另一个独立集群。

## 4. 模块与自动配置

两个独立 starter 均采用仓库现有 marker 模式：

- `fw-plus-magicapi-plugin-dbtx`：包含 POM 和 `DbTxPluginMarker`，提供脚本 API、本地协调器和扩展 SPI，不依赖 Seata。
- `fw-plus-magicapi-plugin-seata`：包含 POM 和 `SeataPluginMarker`，依赖 dbtx 插件并引入 `seata-spring-boot-starter`。
- `fw-plus-boot-autoconfigure`：包含 dbtx、Seata 插件实现、属性类和条件自动配置。
- `MagicApiPluginAutoConfiguration`：分别导入 `MagicAPIDbTxConfiguration` 和 `MagicAPISeataConfiguration`。
- Boot BOM：同时管理 `fw-plus-magicapi-plugin-dbtx` 和 `fw-plus-magicapi-plugin-seata`。

`MagicAPIDbTxConfiguration` 装配脚本模块、本地协调器和扩展注册表。`MagicAPISeataConfiguration` 仅在 Seata 插件 marker 与 Seata 客户端类同时存在时装配 Seata 协调器和数据源代理。引入 Seata 插件即可获得客户端依赖，版本沿用现有 BOM 的 2.0.0；不应将 `seata-server` 引入业务应用运行依赖。

`DbTxModule` 使用 `@MagicModule("dbtx")` 注册为 Spring Bean。Magic-API 会自动发现 Spring 容器中的 Magic 模块，无需修改资源加载器。

自动配置模块的 Java 实现固定放在现有 `magicapi.modules` 目录，不放入 `magicapi.plugin`：

```text
com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx
├── coordinator
├── datasource
├── exception
├── model
├── starter
└── DbTxModule

com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.seata
├── coordinator
├── datasource
└── starter
```

独立 starter 工程中的 `DbTxPluginMarker` 和 `SeataPluginMarker` 仍位于各自 starter 包中，仅用于条件装配，不承载实现代码。

## 5. 核心组件

### 5.1 DbTxModule

负责参数规范化、默认协调器解析、嵌套事务判断和协调器路由，不直接操作 JDBC 或 Seata API。

### 5.2 DbTxCoordinator

统一协调接口接收不可变的事务定义和同步回调：

```java
Object execute(DbTxDefinition definition, DbTxCallback callback) throws Throwable;
```

实现由两个插件分别提供：

- `LocalDbTxCoordinator`
- `SeataDbTxCoordinator`（Seata 插件）

### 5.3 DbTxContextHolder

使用 `ThreadLocal` 保存当前模式、允许访问的数据源集合、嵌套深度和 rollback-only 状态。所有成功和异常路径都必须在最外层 `finally` 中清理。

### 5.4 MagicDataSourceDecorator

为 Magic-API 运行时创建的数据源提供有序装饰扩展点。`MyDataSourceMagicDynamicRegistry` 在 `DataSourceBuilder` 创建原始连接池后、调用 `MagicDynamicDataSource.put` 前应用装饰器链。

dbtx 插件的 `DbTxDataSourceDecorator` 将数据源包装为 `DbTxAwareDataSource`，Seata 插件向包装层提供延迟代理工厂。包装层实现 `DataSource` 和 `Closeable`，负责：

- 在活动 dbtx 中拒绝访问未声明的数据源 key。
- 本地模式返回原始数据源连接。
- Seata 模式为参与的数据源延迟创建并复用 `DataSourceProxy`。
- 关闭时释放原始连接池，避免 Magic-API 只识别 `Closeable` 导致泄漏。
- 防止重复 Seata 代理。

延迟创建 Seata 代理可以避免插件启动时尝试代理 Elasticsearch JDBC、Nebula 等不支持 Seata AT 的数据源。参与 Seata 事务的 key 会在全局事务开始前一次性完成代理准备，任何失败都不会执行业务回调。

## 6. 本地协调器

本地模式使用每个 `MagicDynamicDataSource.DataSourceNode` 已有的 `DataSourceTransactionManager`。其 `JdbcTemplate` 与事务管理器持有同一个包装后的 `DataSource`，Spring JDBC 会按 DataSource 将事务连接绑定到当前线程，因此无需修改或代理 Magic-API 的 `SQLModule`。

执行流程：

1. 校验并快照全部参与节点。
2. 拒绝目标数据源上已存在且不属于当前 dbtx 的环境事务。
3. 按规范化 key 排序，依次开启本地事务。
4. 全部开启成功后执行回调。
5. 回调成功且未标记 rollback-only 时，按开启顺序的逆序提交。
6. 回调失败、开启失败或 rollback-only 时，按逆序回滚所有未完成事务。
7. 清理上下文和 Spring 事务资源。

本地模式是 best-effort 1PC，不声称具备跨库原子提交能力。提交阶段发生网络故障时，已经提交的数据源无法由本模块撤销。

提交失败后不得对已提交或状态未知的事务再次回滚。只回滚尚未进入提交阶段的事务，并在异常中分别报告已提交、已回滚、回滚失败和结果未知的数据源。

## 7. Seata 插件与协调器

Seata 插件使用 `GlobalTransactionContext` 和 Magic-API 数据源对应的 `DataSourceProxy`：

1. 校验全部节点并准备每个参与数据源的 Seata 代理。
2. 若当前线程没有 XID，创建 Launcher 并使用配置的超时和名称开启全局事务。
3. 若当前线程已有 XID，则作为 Participant 加入，不获得外层事务的提交所有权。
4. 将允许访问的数据源 key 写入 dbtx 上下文后执行回调。
5. Launcher 在成功时提交，在回调失败或 rollback-only 时回滚。
6. Participant 只传播返回值或异常，不提交、回滚外层全局事务。
7. 提交异常时读取可用的 XID 和全局状态并抛出，不盲目再次回滚。

每个参与数据库必须是 Seata AT 支持的关系数据库并创建 `undo_log`。Seata 插件提供 MySQL `undo_log` 参考 SQL 和运行说明，但不在应用进程中启动 Seata Server。

Seata 2.0 的 TM、RM、`TransactionManagerHolder` 和 XID 上下文均按 JVM 全局管理。插件不尝试通过反射、运行时改地址或多 Spring Context 创建多个客户端，也不允许脚本传入 Server 名称。连接不同独立集群时必须使用不同应用实例；同一逻辑集群内可配置多个 Server 节点并由客户端完成故障转移。

Seata 2.0.0 不完整支持运行时注销已注册的数据源资源。尚未创建 Seata 代理的数据源可正常热更新或删除；已经参与过 Seata 事务的数据源禁止进程内热替换或删除，配置变更需重启应用后生效，避免资源管理器保留指向已关闭连接池的条目。

## 8. 嵌套与访问边界

嵌套采用受限 `REQUIRED`：

- 相同模式且内层 key 集合等于或属于外层集合时，加入外层事务，不重新开启或结束事务。
- 内层新增数据源、使用不相交集合或切换 `local/seata` 模式时直接失败。
- 内层回调抛错时立即标记整个上下文 rollback-only；即使外层脚本捕获该异常，最外层仍执行回滚并抛出 `DBTX_ROLLBACK_ONLY`。
- 活动事务内访问未声明的数据源时，`DbTxAwareDataSource` 在取得连接前拒绝执行。
- 回调必须保持同步同线程。DTP、线程池、异步消息和响应式线程切换不会继承 dbtx 上下文。

## 9. 异常模型

基础设施错误使用 `DbTxException`，至少包含：

- `code`
- `phase`
- `coordinator`
- `failedDataSource`
- `committed`
- `rolledBack`
- `rollbackFailed`
- `unknown`
- `xid` 和 `globalStatus`（Seata 可用时）

主要错误码：

- `DBTX_VALIDATION_FAILED`
- `DBTX_COORDINATOR_DISABLED`
- `DBTX_COORDINATOR_UNAVAILABLE`
- `DBTX_AMBIENT_TRANSACTION`
- `DBTX_BEGIN_FAILED`
- `DBTX_NESTED_SCOPE_EXPANSION`
- `DBTX_NESTED_MODE_MISMATCH`
- `DBTX_UNDECLARED_DATASOURCE`
- `DBTX_ROLLBACK_ONLY`
- `DBTX_COMMIT_FAILED`
- `DBTX_HEURISTIC_MIXED`
- `DBTX_ROLLBACK_FAILED`

业务回调异常保持原异常实例并重新抛出。回滚失败通过 `Throwable.addSuppressed` 附加，不掩盖原始业务异常。提交和基础设施异常才包装为 `DbTxException`。

## 10. 测试设计

### 10.1 本地真实数据库测试

使用两个独立 H2 DataSource 验证：

- 默认 local、显式 local 和返回值透传。
- 两库成功提交。
- 回调异常时两库回滚。
- 开启中途失败时已开启事务回滚。
- 逆序提交及提交故障状态分类。
- 回滚失败不掩盖业务异常。
- 事务期间连接已绑定，结束后 Spring 资源和 ThreadLocal 均清理。

### 10.2 嵌套与边界测试

- 相同集合和子集嵌套只开启一次。
- 内层异常被脚本捕获后，外层仍 rollback-only。
- 范围扩大、不相交集合和跨模式嵌套被拒绝。
- 未声明数据源访问被拒绝且不会产生部分写入。
- 空参数、重复 key、缺失 key 和环境事务在回调前失败。

### 10.3 Seata 单元与条件装配测试

- Launcher 的 begin/commit/rollback 顺序。
- Participant 不结束外层事务。
- rollback 失败作为 suppressed 异常。
- commit 失败不执行盲目回滚，并保留 XID/状态。
- Seata 类缺失、能力禁用、代理失败和默认协调器校验。
- 延迟代理、防双重代理、包装层关闭和热替换保护。

### 10.4 Seata 真实集成测试入口

提供独立的集成测试入口，连接 Seata Server 2.0.0 与两个包含 `undo_log` 的 MySQL 数据库，覆盖双提交、第二库异常双回滚、超时、未声明 key 和并发事务。默认 Maven 单元测试不依赖外部 Seata/MySQL，也不使用 H2 冒充 AT 端到端验证。

## 11. 验收标准

- dbtx starter 可独立引入且不加载 Seata；Seata starter 依赖 dbtx 并增加全局事务能力。
- 省略模式时按可配置的默认协调器执行，默认值为 local。
- local 和 seata 能在同一实例中并存，由每次脚本调用选择。
- 同一 JVM 仅连接一个 Seata 逻辑集群，支持该集群的多节点高可用，不提供脚本级 Server 切换。
- local 真实双库提交/回滚测试通过，提交不确定状态被准确报告。
- Seata 协调器、数据源代理和条件装配测试通过。
- 未声明数据源、跨模式嵌套和不安全热替换被明确拒绝。
- JDK 8 编译通过，不使用 Java 9+ API。
- 现有用户未提交改动不被覆盖，集成 POM 和自动配置仅做最小追加。
