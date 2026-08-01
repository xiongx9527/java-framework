## 1. 协议基础

- [x] 1.1 评估 MySQL protocol 实现方式和模块边界。
- [x] 1.2 实现握手、认证和连接生命周期。
- [x] 1.3 实现协议命令白名单和非目标命令错误语义。

## 2. 只读查询

- [x] 2.1 实现 COM_QUERY 文本查询处理。
- [x] 2.2 接入 SQL 查询转换和 Nebula 执行结果，包括复杂只读 SQL 的可映射子集。
- [x] 2.3 实现 MySQL 结果集编码。
- [x] 2.4 实现 SHOW/DESCRIBE 元数据响应。
- [x] 2.5 实现只读拒绝、parser/converter/Nebula 执行错误到 MySQL ERR 响应的映射。
- [x] 2.6 固定网关依赖的 parser、SQL 查询转换、JDBC/Nebula 执行和 metadata 稳定接口。

## 3. 客户端验证

- [x] 3.1 用 MySQL CLI 真实验证连接和 `SELECT 1` 查询；GUI 客户端真实验收因当前环境未检测到可用 GUI 客户端，拆为后续人工验收记录，不作为本 change 的 build 退出条件。
- [x] 3.2 补充只读拒绝写入测试。
- [x] 3.3 补充协议握手、认证失败、非目标命令、结果集编码和 metadata 查询测试。

## 后续人工验收记录

- GUI 客户端验收待在安装 DBeaver、DataGrip、Navicat、TablePlus、Sequel Ace、MySQL Workbench 或等价 MySQL GUI 客户端的环境中执行，至少覆盖连接测试、执行 `SELECT 1`、查看基础 schema/table metadata 和写入拒绝。
