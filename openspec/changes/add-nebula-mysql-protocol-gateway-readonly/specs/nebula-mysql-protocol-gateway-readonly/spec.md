## ADDED Requirements

### Requirement: MySQL clients can establish read-only connections
系统 SHALL 提供 MySQL wire protocol 只读服务端，使 MySQL CLI 和兼容 MySQL wire protocol 的客户端可连接；GUI 客户端真实验收 SHALL 在具备客户端环境后作为后续人工验收执行。

#### Scenario: 客户端握手
- **WHEN** MySQL CLI 或兼容 MySQL wire protocol 的客户端连接网关
- **THEN** 完成握手和基础认证。

#### Scenario: GUI 客户端验收待后续人工执行
- **WHEN** 当前自动化或 CLI 验收环境未安装 DBeaver、DataGrip、Navicat、TablePlus、Sequel Ace、MySQL Workbench 或等价 GUI 客户端
- **THEN** 本 change SHALL 记录 GUI 验收 blocker 和后续人工验收步骤，不得把未执行的 GUI 验收标记为已真实通过。

#### Scenario: 认证失败
- **WHEN** 客户端使用错误用户名、密码或不允许的默认 database 连接网关
- **THEN** 网关返回 MySQL 客户端可理解的认证错误并关闭连接。

#### Scenario: 非目标协议命令被拒绝
- **WHEN** 客户端发送本 change 不支持的 prepared statement、LOAD DATA、binlog、复制或 routine 相关协议命令
- **THEN** 网关返回明确不支持错误，且不进入 SQL 查询转换或 Nebula 执行链路。

### Requirement: COM_QUERY read statements are executed
系统 SHALL 接收 COM_QUERY 文本查询，将受支持 SQL 查询（包括复杂只读 SQL 的可映射子集）转给 SQL→nGQL 查询能力并返回 MySQL 协议结果集。

#### Scenario: SELECT 查询
- **WHEN** 客户端执行 SELECT 
- **THEN** 网关返回 MySQL 文本协议结果集。

#### Scenario: 复杂只读 SQL 转发
- **WHEN** 客户端通过 COM_QUERY 执行 JOIN、子查询、聚合、GROUP BY、HAVING、UNION 或函数表达式等复杂只读 SQL
- **THEN** 网关将 SQL 交给 SQL→nGQL 查询能力判定和转换，不在协议层直接依赖 parser 或 converter 内部模型。

#### Scenario: SHOW 和 DESCRIBE
- **WHEN** 客户端执行 SHOW TABLES 或 DESCRIBE 
- **THEN** 网关返回结构信息。

#### Scenario: 结果集类型编码
- **WHEN** 查询结果包含 NULL、字符串、数字、布尔、日期时间或其他对象值
- **THEN** 网关使用 MySQL 文本协议返回稳定列定义和行数据编码。

#### Scenario: 查询错误语义
- **WHEN** SQL 语法错误、SQL→nGQL 转换不支持或 Nebula 执行失败
- **THEN** 网关返回包含稳定错误码、SQLState 或明确错误原因的 MySQL ERR 响应。

### Requirement: Read-only gateway rejects writes clearly
只读网关 SHALL 拒绝写入、DDL 和事务语句，并返回 MySQL 客户端可理解的错误。

#### Scenario: 写入被拒绝
- **WHEN** 客户端执行 INSERT、UPDATE、DELETE 或 DDL 
- **THEN** 返回明确只读错误。

#### Scenario: 事务与高级能力被拒绝
- **WHEN** 客户端执行 BEGIN、START TRANSACTION、COMMIT、ROLLBACK、CALL、TRIGGER、binlog 或复制相关语句
- **THEN** 网关返回明确只读或不支持错误，且不调用 Nebula 执行链路。
