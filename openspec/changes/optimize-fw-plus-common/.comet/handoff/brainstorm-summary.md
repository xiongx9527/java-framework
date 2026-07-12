# Brainstorm Summary

- Change: optimize-fw-plus-common
- Date: 2026-07-12

## 确认的技术方案

采用“兼容收敛”方案优化 `fw-plus-core/fw-plus-project/fw-plus-common`：

- 先按调用半径筛选 `fw-plus-common` 中高价值公共路径。
- 只处理有明确证据、能验证收益的优化点。
- 优先级为：删除无效代码 > 复用已有实现 > 使用 JDK/已有依赖 > 最小兼容改动。
- 保持既有 public API 的方法签名、返回语义和异常语义兼容。
- 不做广覆盖风格清理，不做性能基准驱动的大改。

## 关键取舍与风险

- 选择兼容收敛，而不是广覆盖清理：降低 common 基础层回归风险。
- 选择证据驱动，而不是主观“看起来更干净”：没有明确收益的改动不做。
- 选择最小测试，而不是完整测试矩阵：只覆盖被改动的分支、状态、异常或校验语义。
- 风险：公共工具存在隐式调用方；通过 CodeGraph 调用半径和最小下游验证降低风险。

## 测试策略

- `fw-plus-common` 内部：对被改动的非平凡逻辑补最小单元测试。
- 下游影响：如触及 `CommonsValidator` / `CommonsValidatorHolder` 等已知公共路径，运行对应 Boot/POI 最小调用方测试或编译验证。
- 验证命令优先使用 Maven wrapper，并限定模块范围，避免无关全量构建。

## Spec Patch

无。
