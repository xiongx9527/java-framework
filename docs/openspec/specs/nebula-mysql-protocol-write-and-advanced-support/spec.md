# nebula-mysql-protocol-write-and-advanced-support Specification

## Purpose
TBD - created by archiving change add-nebula-mysql-protocol-write-and-advanced-support. Update Purpose after archive.
## Requirements
### Requirement: MySQL protocol gateway supports writes and DDL
系统 SHALL 在 MySQL 协议网关中支持受控 SQL 写入和 DDL，并通过 SQL→nGQL 写入能力执行。

#### Scenario: 协议写入
- **WHEN** 客户端通过 MySQL 协议执行 INSERT/UPDATE/DELETE 
- **THEN** 网关完成转换并返回执行结果。

#### Scenario: 协议 DDL
- **WHEN** 客户端通过 MySQL 协议执行受支持 DDL 
- **THEN** 网关返回 MySQL 风格成功或错误响应。

### Requirement: Prepared statement common paths are supported
系统 SHALL 支持 Java MySQL JDBC Driver 常见 prepared statement 路径。

#### Scenario: Prepared SELECT
- **WHEN** Java MySQL JDBC Driver 使用 PreparedStatement 执行 SELECT 
- **THEN** 参数绑定后可查询。

#### Scenario: Prepared write
- **WHEN** PreparedStatement 执行受支持写入
- **THEN** 可完成参数绑定、转换和执行。

### Requirement: Advanced client compatibility is configurable
系统 SHALL 提供权限、SSL 或安全策略配置，增强 Java MySQL JDBC Driver 与 GUI/BI metadata fixture 的元数据探测兼容，并记录真实 GUI/BI 客户端的后续人工验收事项。

#### Scenario: 权限或认证失败
- **WHEN** 凭据或权限不满足
- **THEN** 客户端收到明确 MySQL 协议错误。

#### Scenario: 元数据探测
- **WHEN** BI 或 GUI 工具发起常见 metadata 查询
- **THEN** 网关返回兼容响应或明确不支持错误。

#### Scenario: 自动 metadata fixture 验证
- **WHEN** 自动测试执行 Java MySQL JDBC Driver 路径以及 DBeaver/DataGrip/BI 常见 metadata SQL fixture
- **THEN** 查询、写入、元数据响应和安全失败路径均可通过可重复测试证明。

#### Scenario: GUI/BI 手动验收跟进
- **WHEN** 当前环境缺少至少两个可运行的真实 GUI/BI 客户端
- **THEN** 本 change SHALL 记录后续 Manual Acceptance Follow-up，列出客户端、连接、metadata 浏览和查询验收步骤，且不得声明真实 GUI/BI 验收已完成。

