# Brainstorm Summary

- Change: add-nebula-mysql-protocol-gateway-readonly
- Date: 2026-07-17

## 确认的技术方案

采用独立 MySQL wire protocol 网关模块，协议层与查询执行层分离。

- 协议层负责 MySQL handshake、基础认证、能力协商、packet 编解码、连接生命周期、COM_QUERY 分发、OK/ERR/EOF 或 5.7 兼容 EOF 语义，以及文本结果集编码。
- 查询执行层不直接解析复杂 SQL，不依赖 Druid AST 或 converter 内部类；它消费前置 change 暴露的 `NebulaSqlExecutor` 或等价稳定查询服务，将原始 SQL、只读执行意图、连接上下文和参数空绑定传入 SQL parser / SQL-to-nGQL 转换链路。
- Nebula 执行继续复用 `NebulaConnection.execute(nGql)` / `NebulaClientFacade.executeNgql(nGql)`，结果集列描述优先来自 SQL 执行服务或 converter 输出；缺失时保守映射 Nebula `ResultSet` 的列名和值类型。
- SHOW DATABASES、SHOW TABLES、DESCRIBE 走网关 metadata handler，读取 Nebula graphspace、tag、edge 和属性信息，并编码为 MySQL 文本协议结果集；复杂 metadata 探测 SQL 可交给 parser/converter 白名单处理或返回明确不支持。
- 只读边界在协议入口和查询服务入口双层执行：COM_QUERY 收到 INSERT、UPDATE、DELETE、DDL、事务、CALL、TRIGGER、routine、binlog、复制等语句时，在调用 converter 前拒绝并返回 MySQL 客户端可理解的 ERR packet。

## 候选技术方案

已确认推荐方案。以下备选不采用，仅作为设计取舍记录。

备选方案 A：直接复用标准 JDBC 驱动并在 MySQL 协议层把 COM_QUERY 转成 JDBC `Statement.executeQuery(sql)`。实现较少，但会把 MySQL 协议错误、metadata 兼容和 JDBC 框架集成 change 的边界耦合在一起，不利于后续 write/binlog 扩展。

备选方案 B：引入完整 MySQL Server 协议框架并一次性实现更大协议子集。客户端兼容潜力更高，但范围会自然扩张到 prepared statement、事务、权限、binlog 和 server status，不符合本 change 的只读基础网关边界。

## 关键取舍与风险

- 推荐采用独立协议适配层，保持 MySQL wire protocol、SQL 转换和 Nebula JDBC 执行三段边界清晰。
- 本 change 只处理 MySQL protocol 连接、认证、COM_QUERY、文本结果集、metadata 查询和只读拒绝；不实现 prepared statement 二进制协议、LOAD DATA、LOCAL INFILE、事务、写入、binlog、routine、trigger 或复制协议。
- 协议实现方式优先评估 Java 8 兼容、许可证清晰、可单元测试的 MySQL packet 编解码库；若第三方库风险较高，则以 Netty + 最小 packet 编解码实现本 change 所需子集。
- 认证边界采用可配置简单认证或桥接 Nebula 连接配置，不在本 change 实现完整 MySQL 权限体系；认证成功仅表示可建立只读网关会话。
- 错误语义需要统一映射：只读拒绝返回稳定 SQLState/错误码；parser 语法错误、converter 不支持、Nebula 执行失败分别转成 MySQL ERR packet，并保留服务端日志中的诊断信息。

## 测试策略

- 协议单元测试覆盖 packet 序列号、握手、认证成功/失败、COM_QUERY 分发、OK/ERR/resultset 编码。
- 查询处理测试使用 fake SQL executor / fake Nebula result，不依赖真实 NebulaGraph，验证 SELECT、复杂只读 SQL 转发、SHOW/DESCRIBE metadata 响应和写入拒绝。
- 结果集编码测试覆盖列定义、NULL、字符串、整数、浮点、布尔、时间、对象 fallback、空结果集和错误结果。
- 客户端验收测试至少覆盖 MySQL CLI 和一个 GUI 客户端的连接、SELECT、SHOW TABLES、DESCRIBE、写入拒绝；可作为手动或可选集成测试记录。

## Spec Patch

补充 delta spec 验收场景，使协议错误语义、复杂只读 SQL 转发边界、metadata 查询、结果集类型编码和非目标协议拒绝更明确。
