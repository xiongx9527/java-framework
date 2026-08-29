## 1. 回归证据

- [x] 1.1 运行包结构断言并记录其因目标 `adapter.model` / `adapter.store` 尚不存在而失败的 RED 证据

## 2. 包结构迁移

- [x] 2.1 将 Model 自动配置、Provider Adapter 实现、辅助类和对应测试迁移到 `adapter.model`，并通过文件清单与旧包搜索验证
- [x] 2.2 将 Embedding Store 自动配置、全部 Provider Adapter 实现、辅助类和对应测试迁移到 `adapter.store`，并通过文件清单与旧包搜索验证
- [x] 2.3 更新 LangChain4j 聚合自动配置及仓库内全部旧包引用，并确认 Spring Bean 方法名和接口返回类型保持不变

## 3. 验证

- [x] 3.1 运行同一包结构断言并确认 GREEN，同时确认 `model.provider` 与 `store.embeding.provider` 声明和目录均已消失
- [x] 3.2 运行 Model/Embedding Store Provider Adapter 及自动配置定向测试并确认通过
- [x] 3.3 编译 `fw-plus-ai-boot-autoconfigure` 模块并确认 Java 8 Maven 构建通过
