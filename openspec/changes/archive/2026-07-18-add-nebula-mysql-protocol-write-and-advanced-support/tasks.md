## 1. 高级协议设计

- [x] 1.1 定义写入、DDL、prepared statement、权限、SSL 的支持边界。
- [x] 1.2 定义客户端兼容矩阵和元数据响应策略。

## 2. 协议扩展实现

- [x] 2.1 实现 MySQL 协议写入和 DDL 执行路径。
- [x] 2.2 实现 prepared statement 常见路径。
- [x] 2.3 实现认证/权限/SSL 配置策略。
- [x] 2.4 增强 metadata 查询兼容。

## 3. 兼容验证

- [x] 3.1 使用 Java MySQL JDBC Driver 验证查询和写入。
- [x] 3.2 使用 Java MySQL JDBC Driver、DBeaver/DataGrip/BI metadata fixtures、protocol/security 自动测试验证元数据浏览、查询、写入和失败路径；真实 GUI/BI 客户端验收不作为本 change build exit criteria。
- [x] 3.3 补充安全失败和不支持协议路径测试。
- [x] 3.4 记录后续 GUI/BI 手动验收事项：在安装了至少两个真实 GUI/BI 客户端的环境中补充连接、metadata 浏览和查询验收。
