## Why

`fw-plus-easytrans` 当前承载注解翻译、DTO 转换、AOP/ResponseBodyAdvice 自动翻译和多种翻译服务，但核心链路缺少最小测试覆盖，且存在空值、反射、集合遍历和失败兜底等稳定性与性能风险。

本次优化聚焦稳定性与明确热点性能，保持现有公开 API、注解、配置 key 和返回结构兼容，用最小改动降低运行时风险。

## What Changes

- 优化 `fw-plus-easytrans` 内部翻译链路的空值、异常兜底和重复处理逻辑。
- 优化批量翻译、反射访问、代理/缓存相关路径中可确认的重复开销。
- 补充覆盖核心翻译路径的最小测试或可运行验证，防止兼容行为被改坏。
- 保持 Boolean、Enum、Dictionary、RPC 等现有翻译输出兼容。
- 不引入新依赖，不新增未被立即使用的抽象，不调整 Boot/sample/product 代码。

## Capabilities

### New Capabilities
- `easytrans-optimization`: 覆盖 `fw-plus-easytrans` 的稳定性与性能优化要求，包括翻译链路兜底、兼容输出和最小验证。

### Modified Capabilities
- 无。

## Impact

- 影响代码范围：
  - `fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-annotation`
  - `fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-common`
  - `fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service`
- 不改变公开 API、注解、配置 key、返回结构。
- 不新增第三方依赖。
- 验证范围限定为 `fw-plus-easytrans` 模块编译与核心翻译链路最小测试。
