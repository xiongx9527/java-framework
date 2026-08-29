## 1. 包结构迁移

- [x] 1.1 记录 adapter 根包未拆分的 RED 结构证据。
- [x] 1.2 将 12 个模型相关主类与 3 个测试类迁移到 `adapter.model`。
- [x] 1.3 将 9 个 Embedding Store 相关主类与 3 个测试类迁移到 `adapter.store`。

## 2. 引用迁移与验证

- [x] 2.1 更新仓库内生产代码和测试的旧包 import，且不引入逻辑变更。
- [x] 2.2 验证根包清空、类型数量正确且旧包引用归零。
- [x] 2.3 运行 Core adapter 模块与 Boot AI 自动配置相关测试/编译。
