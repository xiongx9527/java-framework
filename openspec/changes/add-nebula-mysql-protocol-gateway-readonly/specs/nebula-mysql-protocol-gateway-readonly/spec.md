## ADDED Requirements

### Requirement: MySQL clients can establish read-only connections
系统 SHALL 提供 MySQL wire protocol 只读服务端，使常见 MySQL 客户端可连接。

#### Scenario: 客户端握手
- **WHEN** MySQL CLI 或 GUI 客户端连接网关
- **THEN** 完成握手和基础认证。

### Requirement: COM_QUERY read statements are executed
系统 SHALL 接收 COM_QUERY 文本查询，将受支持 SQL 查询（包括复杂只读 SQL 的可映射子集）转给 SQL→nGQL 查询能力并返回 MySQL 协议结果集。

#### Scenario: SELECT 查询
- **WHEN** 客户端执行 SELECT 
- **THEN** 网关返回 MySQL 文本协议结果集。

#### Scenario: SHOW 和 DESCRIBE
- **WHEN** 客户端执行 SHOW TABLES 或 DESCRIBE 
- **THEN** 网关返回结构信息。

### Requirement: Read-only gateway rejects writes clearly
只读网关 SHALL 拒绝写入、DDL 和事务语句，并返回 MySQL 客户端可理解的错误。

#### Scenario: 写入被拒绝
- **WHEN** 客户端执行 INSERT、UPDATE、DELETE 或 DDL 
- **THEN** 返回明确只读错误。

