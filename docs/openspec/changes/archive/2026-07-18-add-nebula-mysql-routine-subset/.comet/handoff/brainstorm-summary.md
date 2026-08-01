# Brainstorm Summary

- Change: add-nebula-mysql-routine-subset
- Date: 2026-07-17
- Language: zh-CN
- Status: 已确认，采用方案 A

## 已确认事实

- 本 change 属于 Comet full workflow，当前 phase 为 design。
- OpenSpec 上游事实源要求支持 CREATE PROCEDURE、DROP PROCEDURE、CALL 的基础路径；保存 routine 定义；支持参数、顺序 SQL、简单变量和简单条件；过程体 SQL 复用 SQL 到 nGQL 查询/写入能力；对游标、handler、动态 SQL、复杂事务等明确报错。
- 现有 Nebula JDBC 稳定路径是 nGQL 直连：`NebulaStatementImpl` 和 `NebulaPreparedStatementImpl` 通过 `NebulaSqlExecutor` 执行，`NebulaStatementClassifier` 只按首词做 query/update 分类。
- 前置设计约定 parser core 输出自有 SQL IR，query/write converter 负责 SQL 到 nGQL，MySQL protocol 网关负责 COM_QUERY/协议/错误包，不应直接依赖 Druid AST 或 raw SQL fallback。

## 确认的技术方案

### 方案 A：在 MySQL 网关侧新增 Routine Service + Interpreter

- 在 MySQL protocol gateway 的 SQL 分发层前置识别 `CREATE PROCEDURE`、`DROP PROCEDURE`、`CALL`。
- parser core 扩展 routine DDL/CALL AST，但只暴露自有 IR；网关 routine 模块消费 IR。
- routine 元数据由可插拔 `RoutineMetadataRepository` 管理，默认提供文件或内存实现，生产建议配置持久化实现。
- `RoutineInterpreter` 解释执行参数绑定、局部变量、`SET`、顺序 SQL、简单 `IF/ELSEIF/ELSE`；过程体 SQL 调用已有 SQL execution port，复用 SQL 到 nGQL 查询/写入能力。
- 默认不支持函数返回值、游标、handler、动态 SQL、临时表、事务控制、多结果集高级语义、递归或循环；递归/嵌套深度、语句数量和耗时均有保护。

已确认采用方案 A。它符合 OpenSpec 的“网关侧解释器”决策，能复用前置 SQL 能力，同时不污染 nGQL 直连 JDBC 路径。

## 已评估但不采用的方案

### 方案 B：把 routine 直接降级为 SQL-to-nGQL 宏展开

- CREATE 保存文本，CALL 时做参数替换和语句展开。
- 实现更短，但容易引入字符串替换安全问题，也难以表达局部变量、条件分支、错误定位和执行保护。

不推荐：不利于测试和安全边界。

### 方案 C：引入完整 MySQL 存储过程执行引擎

- 目标兼容性高，但范围巨大，且 NebulaGraph 不具备完整 MySQL 事务、游标、handler 语义承载。

不推荐：超出本 change 的子集目标。

## 关键取舍与风险

- routine 能力应是 MySQL 网关高级兼容层，不改变 `jdbc:nebula://` nGQL 直连行为。
- 元数据持久化需要定义稳定接口；默认实现可先满足测试和单机运行，但文档必须说明生产持久化/备份责任。
- CALL 可能返回多条 SQL 的多个结果；本 change 建议第一阶段只返回最后一条产生结果集的 SQL 或 OK summary，明确拒绝多结果集高级协议语义。
- 过程体内写入和 DDL 依赖前置 SQL write/DDL support；若前置能力未完成，routine build 应以 fake execution port 或 unsupported error 固定边界。
- 安全风险集中在动态 SQL、递归、超时、元数据污染和权限绕过；设计中必须通过语法白名单、执行预算和权限检查缓解。

## 测试策略

- Parser/IR 测试：CREATE/DROP/CALL、参数、局部变量、IF、顺序 SQL、unsupported 语法。
- Repository 测试：保存、覆盖策略、删除、大小写归一、schema/graphspace 隔离、重启后加载。
- Interpreter 单元测试：参数绑定、变量赋值、条件分支、顺序执行、错误短路、执行预算、递归拒绝。
- Gateway 分发测试：COM_QUERY 中 routine 语句走 routine handler；普通 SQL 不受影响；错误映射到 MySQL ERR。
- 集成契约测试：使用 fake SQL execution port 断言过程体 SQL 按顺序调用；可选真实 Nebula 环境只验证端到端 smoke。

## Spec Patch

- 补充 CREATE PROCEDURE 支持的语法边界：IN 参数、BEGIN/END、顺序 SQL、SET 局部变量、简单 IF/ELSEIF/ELSE。
- 补充 DROP PROCEDURE 删除已保存定义的验收场景。
- 补充 CALL 参数绑定、结果返回策略和未知 routine 错误场景。
- 补充执行保护场景：递归、超时、语句数量/嵌套深度超过限制时明确失败。
- 补充 unsupported 在 CREATE 阶段优先拒绝，若历史定义或兼容路径绕过则在 CALL 阶段拒绝。
