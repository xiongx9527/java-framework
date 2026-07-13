## 1. Redis 解析稳定性优化

- [x] 1.1 梳理 `fw-plus-redis` 当前 Redis 数据源名称解析入口和调用边界。
- [x] 1.2 为 `EnvironmentUtil.loadRedisDataSourceName` 补最小单元测试，覆盖有效配置、无效短属性名和重复数据源。
- [x] 1.3 优化数据源名称解析实现，避免固定下标解析带来的异常或空名称污染。

## 2. 验证

- [x] 2.1 运行 `fw-plus-redis` 聚焦测试。
- [x] 2.2 运行 `fw-plus-redis` 模块编译，确认不破坏下游兼容编译。
