## 1. 事件模型设计

- [x] 1.1 定义 binlog-compatible event 格式、position 模型和持久化策略。
- [x] 1.2 定义 DML、DDL、错误和边界事件的表达方式。

## 2. 事件生成与消费

- [ ] 2.1 在网关写入路径生成 DML 事件。
- [ ] 2.2 在受支持 DDL 路径生成 schema 事件。
- [x] 2.3 实现按 position 读取事件流。

## 3. 验证

- [ ] 3.1 补充写入/DDL 事件生成测试。
- [ ] 3.2 补充 position 消费和绕过网关边界测试。
