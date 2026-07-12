## Why

`fw-plus-common` 是 Core 层公共基础模块，当前承担校验、上下文、异常、Web/Spring 工具等跨模块复用职责。严格优化该模块可以减少重复逻辑、过度实现和潜在运行风险，同时保持下游模块行为兼容。

## What Changes

- 盘点 `fw-plus-common` 现有公共工具代码，优先删除无效代码、复用已有实现、简化低收益复杂逻辑。
- 聚焦高价值优化点：线程上下文清理、校验工具、异常/结果封装、Web/Spring 工具等公共路径。
- 为非平凡优化补充最小可运行测试，确保现有调用语义不变。
- 不引入新依赖，不新增新能力，不做跨模块架构重写。
- 不改变既有 public API 的语义；如必须调整内部实现，保持调用方兼容。

## Capabilities

### New Capabilities
- `common-module-optimization`: 约束 `fw-plus-common` 优化工作必须以兼容、可验证、最小改动为边界。

### Modified Capabilities
- 无。

## Impact

- 主要影响代码：`fw-plus-core/fw-plus-project/fw-plus-common`。
- 可能受影响调用方：Boot/POI 等依赖 `CommonsValidator`、`CommonsValidatorHolder`、Spring/Web 工具类的模块。
- 不新增第三方依赖，不改变 Maven 坐标，不调整公共 API 契约。
