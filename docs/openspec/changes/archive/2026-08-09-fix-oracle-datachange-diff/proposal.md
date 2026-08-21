## Why

Oracle JDBC 返回的列标签通常为大写，而数据变更监控配置使用业务字段原始大小写。当前 diff 阶段按大小写敏感方式读取快照值，导致 Oracle INSERT/UPDATE 事件可能被错误判定为没有字段变化。

## What Changes

- 让数据变更 diff 按列名大小写不敏感读取快照值。
- 调整跨数据库契约测试，使数值字段断言兼容 Oracle `NUMBER` 返回的 `BigDecimal`。
- 保留并验证 Oracle、MySQL、MariaDB、PostgreSQL、SQL Server 的真实数据库测试。

## Capabilities

### New Capabilities

- `mybatis-datachange-dialect`: 覆盖多数据库数据变更快照比较和方言集成验证。

### Modified Capabilities

### Impact

- 影响 `fw-plus-mybatis-datachange` 的 diff 核心逻辑及方言集成测试。
- 不新增依赖，不改变公开 API，不改变事务可选语义。
