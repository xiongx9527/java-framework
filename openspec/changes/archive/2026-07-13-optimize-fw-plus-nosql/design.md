## Context

`fw-plus-nosql` 当前主要提供 Redis 基础增强能力，`fw-plus-redis` 模块包含数据源注解、常量、异常、函数式接口和环境工具。Boot 层多 Redis 注册逻辑会调用 `EnvironmentUtil.loadRedisDataSourceName(AbstractEnvironment)` 从 `spring.redis.datasource.<name>.*` 属性中解析数据源名称。

约束：
- 仅修改 `fw-plus-core/fw-plus-project/fw-plus-nosql/fw-plus-redis`。
- 保持 `spring.redis.datasource.<name>` 配置格式、`@RedisDataSource` 和常量接口兼容。
- 不修改 Boot 自动配置和运行时注册流程。
- 不新增运行时依赖。

## Goals / Non-Goals

**Goals:**
- 提高 Redis 数据源名称解析对空环境、无效属性名和重复属性源的容错能力。
- 减少解析逻辑中的魔法字符串和固定下标假设。
- 为解析行为补最小单元测试。

**Non-Goals:**
- 不重写 Redis 多数据源注册架构。
- 不调整 Redis 连接配置模型或 Boot 自动配置行为。
- 不新增 Redis 客户端能力或脚本执行能力。

## Implementation Notes

- 在 `EnvironmentUtil` 中集中定义 `spring.redis.datasource.` 前缀。
- 解析时只接受前缀后仍包含非空数据源名的属性。
- 对属性源中的重复名称自然去重，继续返回 `Set<String>`。
- 对空 `environment` 或无可枚举属性源保持返回空集合。
- 测试使用 Spring `MockEnvironment` 或 `MapPropertySource` 构造最小环境，不连接 Redis。

## Validation

优先运行：

```bash
cd fw-plus-core && mvn test -pl fw-plus-project/fw-plus-nosql/fw-plus-redis -Dtest=EnvironmentUtilTest
cd fw-plus-core && mvn compile -pl fw-plus-project/fw-plus-nosql/fw-plus-redis -am -DskipTests
```

如果单模块依赖缺失，按 Maven 提示补充 `-am`，但不扩大源码修改范围。
