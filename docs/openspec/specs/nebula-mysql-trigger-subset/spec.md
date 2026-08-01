# nebula-mysql-trigger-subset Specification

## Purpose
TBD - created by archiving change add-nebula-mysql-trigger-subset. Update Purpose after archive.
## Requirements
### Requirement: Gateway writes can fire supported triggers
系统 SHALL 对经网关执行的受支持 INSERT、UPDATE、DELETE 触发已定义的 BEFORE/AFTER trigger。

#### Scenario: INSERT 触发
- **WHEN** 经网关执行 INSERT 
- **THEN** 匹配 trigger 被执行。

#### Scenario: UPDATE/DELETE 触发
- **WHEN** 经网关执行 UPDATE 或 DELETE 
- **THEN** 匹配 trigger 被执行。

#### Scenario: BEFORE trigger 失败
- **WHEN** BEFORE trigger 执行失败
- **THEN** 系统不执行主写入并返回明确错误。

#### Scenario: AFTER trigger 执行条件
- **WHEN** 主写入执行失败
- **THEN** 系统不执行 AFTER trigger。

### Requirement: Trigger definitions are managed as gateway metadata
系统 SHALL 支持创建和删除 trigger 定义，并将其作为网关元数据管理。

#### Scenario: 创建触发器
- **WHEN** 客户端执行受支持 CREATE TRIGGER 
- **THEN** 系统保存 trigger 定义。

#### Scenario: 删除触发器
- **WHEN** 客户端执行 DROP TRIGGER 
- **THEN** 系统移除 trigger 定义。

#### Scenario: 删除不存在的触发器
- **WHEN** 客户端执行 DROP TRIGGER 删除不存在的 trigger
- **THEN** 系统按明确错误或 IF EXISTS 幂等语义响应。

### Requirement: Trigger boundaries and recursion protection are explicit
系统 SHALL 明确 trigger 只对经网关写入生效，并限制递归触发。

#### Scenario: 绕过网关写入
- **WHEN** 原生 Nebula 写入不会触发网关 trigger，系统文档明确该边界。
- **THEN** 系统产生符合要求的结果

#### Scenario: 递归触发
- **WHEN** trigger body 导致递归触发超过限制
- **THEN** 系统中止并返回明确错误。

#### Scenario: 不支持的 trigger body
- **WHEN** trigger body 包含不支持语法
- **THEN** 系统拒绝创建或执行并返回明确错误。

