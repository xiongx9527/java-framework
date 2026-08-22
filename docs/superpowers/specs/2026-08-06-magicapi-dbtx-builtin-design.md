# Magic-API DBTX 内置能力设计

## 目标

在现有 `fw-plus-magicapi-spring-boot-starter` 中内置多数据源事务能力，向 Magic-Script 提供统一的 `dbtx.transaction(...)` API，并支持业务按次选择 `local` 或 `seata` 协调器。

本能力是后端运行时模块，不是 Magic-API 控制台插件。实现不创建新的 fw-plus artifact，不提供前端 bundle、`MagicPluginConfiguration`、`Plugin` 元数据或 classpath marker。

## 模块边界

- 删除 `fw-plus-magicapi-plugin-dbtx` 和 `fw-plus-magicapi-plugin-seata` 两个临时 plugin artifact。
- 不创建 `fw-plus-magicapi-dbtx-spring-boot-starter` 或 `fw-plus-magicapi-seata-spring-boot-starter`。
- dbtx 与 Seata 实现继续位于 `fw-plus-boot-autoconfigure`：
  - `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx`
  - `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.seata`
- `fw-plus-magicapi-spring-boot-starter` 继续作为唯一的 fw-plus Magic-API starter，不增加前端构建或资源。
- dbtx/Seata 自动配置由 `MagicAPIBaseAutoConfiguration` 直接导入，不再经过 `MagicApiPluginAutoConfiguration`。

## 依赖模型

local 能力使用现有 Spring JDBC/Spring Transaction 依赖，随 Magic-API 主 starter 可用。

主 starter 不传递 Seata 客户端。业务需要 Seata 时直接引入：

```xml
<dependency>
    <groupId>io.seata</groupId>
    <artifactId>seata-spring-boot-starter</artifactId>
</dependency>
```

版本继续由 fw-plus Boot BOM 管理为 Seata `2.0.0`。一 JVM 使用一个 Seata 客户端和一个逻辑集群；标准 `seata.*` 配置可声明多个 Server 节点实现高可用，不提供脚本级独立 Seata Server 切换。

## 配置

配置不再使用 `magic-api.plugin.*` 前缀：

```yaml
magic-api:
  dbtx:
    enabled: true
    default-coordinator: local
    local-enabled: true
    seata-enabled: false
```

- `enabled` 默认 `true`，控制整个 dbtx 后端模块。
- `default-coordinator` 默认 `local`，供无 mode 重载使用。
- `local-enabled` 默认 `true`。
- `seata-enabled` 默认 `false`；启用后仍要求业务 classpath 存在 Seata 客户端。
- 配置的默认协调器不可用时应用启动失败，不静默降级。
- 非默认协调器不可用时允许应用启动，脚本显式选择该模式时返回清晰的 `DBTX_COORDINATOR_UNAVAILABLE`。

Seata 客户端和 Server 连接继续使用 Seata 官方配置，例如：

```yaml
seata:
  application-id: fw-plus-magicapi
  tx-service-group: magicapi_tx_group
  service:
    vgroup-mapping:
      magicapi_tx_group: default
    grouplist:
      default: 127.0.0.1:8091
```

## 脚本 API

```javascript
dbtx.transaction(['order', 'account'], callback);
dbtx.transaction('local', ['order', 'account'], callback);
dbtx.transaction('seata', ['order', 'account'], callback);
```

- 第一种重载使用 `magic-api.dbtx.default-coordinator`。
- 显式 mode 覆盖默认值。
- local 与 Seata 可在同一应用中共存。
- 相同模式的子集嵌套加入外层事务；扩大数据源范围或跨模式嵌套失败。
- callback 仅支持同步、同线程执行。

## local 语义

local 是 best-effort 1PC，不宣称跨库原子 2PC：

- 按规范化后的 key 顺序开启本地事务，反序提交或回滚。
- callback 失败时回滚所有已开启事务，回滚异常附加为 suppressed。
- commit 失败时报告 `committed`、`unknown`、`rolledBack`、`rollbackFailed`。
- 已有参与库完成提交时使用 `DBTX_HEURISTIC_MIXED`，不得把已提交库误报为已回滚。
- 拒绝加入线程中已有的环境 Spring 事务，避免破坏外层资源。

## Seata 语义

- 每个参与数据源首次执行 Seata 事务时延迟创建 `DataSourceProxy`。
- 已有 `SeataDataSourceProxy` 在数据库 allow-list 校验后原样复用，不二次代理。
- 不支持的 JDBC 类型在 transaction lookup、global begin 和 callback 前失败。
- Launcher 拥有 begin/commit/rollback；Participant 只加入现有 XID，不结束外层事务。
- commit 失败携带 XID 和最佳可得 `GlobalStatus`，不得盲目 rollback。
- 已使用的 Seata proxy 因 2.0.0 缺少安全 unregister 能力，运行期数据源热替换或删除必须被拒绝。
- 每个业务库必须创建 Seata AT 所需的 `undo_log` 表。

## 数据源生命周期

Magic-API 动态 JDBC 数据源由 `DbTxAwareDataSource` 包装：

- 无事务上下文和 local 模式使用 raw DataSource。
- 未声明的数据源 key 在访问 raw connection 前失败。
- adapter 按 coordinator 延迟准备并缓存。
- raw pool 由 wrapper 独占物理关闭；adapter 不得重复关闭 raw。
- 动态数据源替换/删除先执行全部无副作用 veto，再由成功的节点移除流程执行 cleanup。
- 装饰阶段失败按逆序回滚每个成功 stage，raw 最后关闭一次，清理异常聚合为 suppressed。

## 自动配置

- `MagicAPIBaseAutoConfiguration` 直接导入 dbtx 与 Seata 配置类。
- dbtx 配置只依赖 Magic-API 主运行时和 `magic-api.dbtx.enabled`。
- Seata 配置同时要求 `magic-api.dbtx.seata-enabled=true` 与 `GlobalTransactionContext` 在 classpath。
- 默认协调器校验使用独立启动生命周期 bean，不能因用户自定义 `DbTxModule` 而跳过。
- 两份配置不实现 `MagicPluginConfiguration`，不返回 `Plugin`，不注册任何 console JavaScript。

## 迁移与清理

从当前开发中间状态迁移时：

- 删除两个 plugin 目录、marker 类和 marker 测试。
- 从 Magic-API plugin 聚合 POM 删除两个 module。
- 从 Boot BOM 删除两个临时 plugin artifact 坐标。
- 从 `MagicApiPluginAutoConfiguration` 删除 dbtx/Seata import 和 `@Import` 项。
- 将条件测试从 marker/classpath 模式改为主 starter 内置配置与 property/class 条件模式。
- 将配置键从 `magic-api.plugin.dbtx.*` / `magic-api.plugin.seata.*` 迁移为 `magic-api.dbtx.*`；本次功能尚未发布，不保留旧键兼容别名。

## 验证

- dbtx 路由、嵌套、rollback-only 与 ThreadLocal 清理单元测试。
- 动态数据源声明边界、装饰失败回滚和生命周期测试。
- 双 H2 local 提交/回滚及 begin/commit/rollback 故障注入测试。
- 无 live Server 的 Seata Launcher/Participant、proxy、XID/status 测试。
- 自动配置测试覆盖 dbtx disabled、默认 local、local disabled、Seata client 缺失、Seata disabled/enabled、自定义 module 不能绕过默认校验。
- 主 Magic-API starter 聚合验证、Java 8 字节码检查、`git diff --check`。
- 外部 Seata Server/MySQL 不可用时，不宣称真实 AT 端到端通过；该项明确标为环境门禁。

## 非目标

- 不提供 Magic-API 控制台页面或前端 bundle。
- 不新增 fw-plus dbtx/Seata plugin 或 starter artifact。
- 不实现一 JVM 多个独立 Seata 客户端或脚本级 Server 切换。
- 不支持异步 callback 跨线程传播事务上下文。
- 不把 local best-effort 1PC 描述为强一致分布式事务。
