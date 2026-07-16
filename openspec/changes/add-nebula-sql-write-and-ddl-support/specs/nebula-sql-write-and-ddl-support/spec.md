## ADDED Requirements

### Requirement: SQL write statements can be converted to nGQL
系统 SHALL 将受支持 INSERT、UPDATE、DELETE 转换为对应 nGQL 写入语句。

#### Scenario: INSERT 转换
- **WHEN** `INSERT INTO person(id, name) VALUES ('p1', 'Tom')` 可转换为 Nebula 插入语句。
- **THEN** 系统产生符合要求的结果

#### Scenario: UPDATE 和 DELETE 转换
- **WHEN** 带 id 或受控条件的 UPDATE/DELETE 可转换为对应 nGQL，不能安全转换时返回错误。
- **THEN** 系统产生符合要求的结果

### Requirement: SQL DDL maps to Nebula schema operations
系统 SHALL 将受支持基础 DDL 映射到 Nebula tag/edge schema 操作。

#### Scenario: CREATE TABLE 映射
- **WHEN** 受支持 CREATE TABLE 可按映射规则生成 CREATE TAG 或 CREATE EDGE。
- **THEN** 系统产生符合要求的结果

#### Scenario: 不支持 DDL
- **WHEN** 复杂 ALTER、索引或 MySQL 专有选项不可映射时返回明确错误。
- **THEN** 系统产生符合要求的结果

### Requirement: Unsupported transactional semantics are explicit
系统 SHALL 明确 SQL 写入和 DDL 与 Nebula 事务语义的差异，不假装支持不可保证的事务行为。

#### Scenario: 事务语句
- **WHEN** 输入 BEGIN、COMMIT、ROLLBACK 等事务语句
- **THEN** 返回明确不支持或受限支持信息。

