## Why

`fw-plus-mp` 当前包含 base、datachange、datascope、encrypt、join 等 MyBatis-Plus 增强模块，部分核心工具链缺少最小测试覆盖，且存在配置默认值、格式化兜底和条件判断等边界稳定性风险。

本次 tweak 聚焦 `fw-plus-mp` 内部可独立收敛的稳定性与可维护性优化，保持现有公开 API、配置 key 和下游编译兼容。

## What Changes

- 优化 `fw-plus-mp` 内部配置默认值和数据变更格式化路径中的明显边界问题。
- 补充核心工具方法或配置类的最小测试，锁定兼容行为。
- 优先处理不改变公开契约的拼写/默认值/空值兜底/重复判断等问题。
- 保持 MyBatis-Plus、MP Join、字段加密、数据变更和数据权限的公开 API 与配置 key 兼容。
- 不扩展到 Boot 自动配置、sample 或 product 层。

## Capabilities

### New Capabilities
- `mp-core-optimization`: 覆盖 `fw-plus-mp` 核心增强模块的稳定性、兼容性与最小验证要求。

### Modified Capabilities
- 无。

## Impact

- 影响代码范围：
  - `fw-plus-core/fw-plus-project/fw-plus-mp/fw-plus-mp-base`
  - `fw-plus-core/fw-plus-project/fw-plus-mp/fw-plus-mp-datachange`
  - `fw-plus-core/fw-plus-project/fw-plus-mp/fw-plus-mp-datascope`
  - `fw-plus-core/fw-plus-project/fw-plus-mp/fw-plus-mp-encrypt`
  - `fw-plus-core/fw-plus-project/fw-plus-mp/fw-plus-mp-join`
- 不改变公开 API、配置 key、注解语义或返回结构。
- 不新增运行时依赖；如需要测试能力，仅使用 test scope 依赖。
- 验证范围限定为 `fw-plus-mp` 相关单元测试与模块编译。
