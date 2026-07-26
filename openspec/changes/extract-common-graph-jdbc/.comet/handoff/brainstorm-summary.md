# Brainstorm Summary

- Change: extract-common-graph-jdbc
- Date: 2026-07-26

## 确认的技术方案

采用 `fw-plus-graph-jdbc` 作为公共图数据库 JDBC 抽象标准模块，位于 `fw-plus-core/fw-plus-project/fw-plus-graph` 聚合下，并先于 `fw-plus-nebula-jdbc`、`fw-plus-neo4j-jdbc` 构建。公共模块定义 JDBC 标准能力和无协议基础实现，两个具体驱动依赖公共模块并实现协议适配。

公共模块职责包括默认 JDBC 异常/默认值、unwrap/isWrapperFor、资源关闭状态、参数绑定、PreparedStatement 参数元数据、只读 ResultSet/RowSet 构造、DatabaseMetaData 结果集形状，以及连接具体驱动执行器的基础执行/结果抽象。Nebula 与 Neo4j 继续保留连接配置、Driver 注册、协议转换、查询执行器和原生结果映射。

## 关键取舍与风险

主要取舍是使用 `fw-plus-graph-jdbc` 而不是 `fw-plus-graph-jdbc-common`。前者更准确表达“标准层/抽象层”，避免把公共能力弱化为工具类集合。风险是抽象过度会把 nGQL、Cypher、SQL AST 等具体协议逻辑带入公共层；缓解方式是公共层只承载标准 JDBC 行为，所有具体协议实现必须留在具体驱动模块。

## 测试策略

公共模块使用纯单元测试覆盖默认异常、默认值、参数绑定、结果集/元数据形状和资源状态。Neo4j 适配测试覆盖 DriverManager、Statement、PreparedStatement、参数绑定和元数据行为。Nebula 适配测试覆盖已有稳定性与框架集成路径，包括重复 close、ResultSet 边界、wasNull、不支持能力异常、JdbcTemplate/MyBatis 元数据读取。所有测试不依赖真实 NebulaGraph 或 Neo4j 服务。

## Spec Patch

无。当前 delta spec 已表达公共能力是抽象标准，已有 Nebula 能力依赖公共能力。
