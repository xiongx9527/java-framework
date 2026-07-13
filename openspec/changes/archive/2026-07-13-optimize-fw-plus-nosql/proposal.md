## Why

`fw-plus-nosql` 当前 Redis 基础增强模块体量较小，但缺少最小测试覆盖；其中环境属性扫描依赖固定下标解析，遇到边界属性名或重复属性源时缺少显式保护。

本次 tweak 聚焦 Redis 数据源名称解析链路的稳定性和可维护性，保持现有公开 API、注解和配置 key 兼容，用最小测试锁定行为。

## What Changes

- 优化 `fw-plus-nosql/fw-plus-redis` 中 Redis 数据源名称解析逻辑，避免无效属性名导致解析异常或污染结果。
- 提取或复用清晰的前缀判断，减少魔法字符串和重复解析假设。
- 补充 `EnvironmentUtil` 的最小单元测试，覆盖空环境、有效数据源、重复属性源和无效前缀/短属性名。
- 保持 `@RedisDataSource`、`DynamicRedisConstants` 现有常量和对外配置 key 兼容。
- 不扩展到 Boot 自动配置、Magic-API 插件或业务示例。

## Capabilities

### New Capabilities
- `nosql-redis-optimization`: 覆盖 `fw-plus-nosql` Redis 数据源名称解析的稳定性与最小验证要求。

### Modified Capabilities
- 无。

## Impact

- 影响代码范围：
  - `fw-plus-core/fw-plus-project/fw-plus-nosql/fw-plus-redis`
- 不改变公开 API、注解、配置 key 或返回类型。
- 不新增运行时依赖；如需要测试能力，仅使用 test scope 依赖。
- 验证范围限定为 `fw-plus-redis` 单元测试与模块编译。
