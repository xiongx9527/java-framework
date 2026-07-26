## ADDED Requirements

### Requirement: Nebula JDBC stability depends on common graph JDBC foundation
Nebula JDBC 稳定性能力 SHALL 依赖 `common-graph-jdbc-foundation` 提供的公共 JDBC 抽象标准，并在复用公共层后继续满足既有稳定性要求。

#### Scenario: 资源关闭稳定性通过公共层复用
- **WHEN** Nebula JDBC 的 Connection、Statement 或 ResultSet 接入公共资源状态或关闭辅助逻辑
- **THEN** 重复 close、关闭状态查询和异常语义 SHALL 继续满足 `nebula-jdbc-stability` 既有要求

#### Scenario: ResultSet 边界行为保持稳定
- **WHEN** Nebula JDBC 复用公共只读结果集、默认值或异常辅助逻辑
- **THEN** cursor 边界、wasNull 和列读取错误 SHALL 继续返回明确 JDBC 风格结果或异常，而不是非预期空指针失败

#### Scenario: 本地稳定性测试不依赖真实服务
- **WHEN** Nebula JDBC 稳定性测试验证公共层接入后的行为
- **THEN** 测试 SHALL 可以在没有真实 NebulaGraph 服务的本地环境中运行
