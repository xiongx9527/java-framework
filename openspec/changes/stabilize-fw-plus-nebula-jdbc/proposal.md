## Why

`fw-plus-nebula-jdbc` 当前承担 Nebula Graph 的 JDBC 兼容入口，但连接初始化、URL/属性解析、资源关闭、Statement/ResultSet 生命周期和异常语义缺少明确契约与测试保护。优化该模块可以降低调用方在异常输入、连接失败、关闭后访问和空结果读取等场景下遇到不确定行为的风险。

## What Changes

- 稳定 JDBC URL 与连接属性解析，确保非法 URL、缺失必要属性、类型转换异常等输入路径返回清晰的 `SQLException`。
- 改进连接池、会话、Statement、PreparedStatement、ResultSet 的生命周期处理，覆盖初始化失败清理、关闭幂等、关闭后访问等行为。
- 梳理 JDBC 契约边界，对暂不支持的方法保持明确的 `SQLFeatureNotSupportedException`，对支持的方法补齐基础状态校验。
- 增加不依赖真实 Nebula 服务的单元测试，覆盖解析、生命周期、异常路径和元数据边界行为。
- 不引入新的 Nebula 业务能力，不升级 Nebula client 版本，不改变公共包名或 Maven 坐标。

## Capabilities

### New Capabilities

- `nebula-jdbc-stability`: 定义 `fw-plus-nebula-jdbc` 在连接解析、资源生命周期、JDBC 状态校验和异常语义上的稳定性要求。

### Modified Capabilities

- 无。

## Impact

- 影响代码：`fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc`。
- 主要涉及类：`NebulaDriver`、`NebulaConnection`、`NebulaJdbcUrlParser`、`NebulaStatementImpl`、`NebulaPreparedStatementImpl`、`NebulaResultSet`、`NebulaResultSetMetaData`、`ExceptionBuilder`。
- 影响 API：保持现有 JDBC 类型和 Maven 坐标不变；可能使部分异常路径更早、更清晰地失败。
- 依赖与环境：保持 Java 1.8；不要求真实 Nebula 服务参与单元测试。
