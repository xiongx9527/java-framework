## 1. Trigger 子集设计

- [x] 1.1 定义支持的 CREATE/DROP TRIGGER 语法和 BEFORE/AFTER 范围。
- [x] 1.2 定义触发器元数据、执行顺序、失败策略和递归限制。

## 2. Trigger 实现

- [x] 2.1 实现 trigger 定义保存和删除。
- [x] 2.2 在 INSERT/UPDATE/DELETE 网关写入路径触发 BEFORE/AFTER 逻辑。
- [ ] 2.3 实现递归保护和不支持语法错误。

## 3. 验证

- [x] 3.1 补充 INSERT/UPDATE/DELETE 触发测试。
- [ ] 3.2 补充绕过网关边界、递归限制和失败策略测试。
