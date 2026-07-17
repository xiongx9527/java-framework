## ADDED Requirements

### Requirement: Simple routines can be created and called
系统 SHALL 支持有限 MySQL 存储过程/函数子集的创建、删除和调用。

#### Scenario: 创建过程
- **WHEN** 客户端执行受支持 CREATE PROCEDURE 
- **THEN** 系统保存过程定义。

#### Scenario: 删除过程
- **WHEN** 客户端执行 DROP PROCEDURE 删除已保存过程
- **THEN** 系统删除该过程定义，后续 CALL 返回过程不存在错误。

#### Scenario: 调用过程
- **WHEN** 客户端执行 CALL 
- **THEN** 系统按定义执行受支持过程体。

#### Scenario: 调用未知过程
- **WHEN** 客户端执行 CALL 调用不存在的过程
- **THEN** 系统返回明确的过程不存在错误。

### Requirement: Routine bodies execute supported SQL sequentially
系统 SHALL 支持参数、简单变量、简单条件和顺序 SQL 执行，过程体 SQL 复用现有 SQL→nGQL 能力。

#### Scenario: 顺序执行
- **WHEN** 过程体包含多条受支持 SQL 
- **THEN** 系统按顺序执行并返回结果。

#### Scenario: 参数和简单变量
- **WHEN** 过程体使用 IN 参数、局部变量和 SET 赋值
- **THEN** 系统按调用参数和变量当前值执行过程体 SQL。

#### Scenario: 简单条件
- **WHEN** 过程体包含受支持 IF/ELSEIF/ELSE 条件
- **THEN** 系统只执行命中的分支。

#### Scenario: 执行保护
- **WHEN** 过程调用超过配置的超时、语句数量、嵌套深度或递归限制
- **THEN** 系统中止调用并返回明确的执行保护错误。

### Requirement: Unsupported routine language features fail explicitly
系统 SHALL 对复杂游标、handler、动态 SQL、临时表或复杂事务语义返回明确不支持错误。

#### Scenario: 复杂过程体
- **WHEN** 过程体包含游标或 handler 
- **THEN** 系统拒绝创建或执行并说明原因。

#### Scenario: 非目标 routine 语义
- **WHEN** 客户端使用 CREATE FUNCTION、OUT/INOUT 参数、动态 SQL、临时表、事务控制或循环语句
- **THEN** 系统返回明确不支持错误。
