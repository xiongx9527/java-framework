## ADDED Requirements

### Requirement: Simple routines can be created and called
系统 SHALL 支持有限 MySQL 存储过程/函数子集的创建、删除和调用。

#### Scenario: 创建过程
- **WHEN** 客户端执行受支持 CREATE PROCEDURE 
- **THEN** 系统保存过程定义。

#### Scenario: 调用过程
- **WHEN** 客户端执行 CALL 
- **THEN** 系统按定义执行受支持过程体。

### Requirement: Routine bodies execute supported SQL sequentially
系统 SHALL 支持参数、简单变量、简单条件和顺序 SQL 执行，过程体 SQL 复用现有 SQL→nGQL 能力。

#### Scenario: 顺序执行
- **WHEN** 过程体包含多条受支持 SQL 
- **THEN** 系统按顺序执行并返回结果。

### Requirement: Unsupported routine language features fail explicitly
系统 SHALL 对复杂游标、handler、动态 SQL、临时表或复杂事务语义返回明确不支持错误。

#### Scenario: 复杂过程体
- **WHEN** 过程体包含游标或 handler 
- **THEN** 系统拒绝创建或执行并说明原因。

