# Comet Design Handoff

- Change: optimize-fw-plus-common
- Phase: design
- Mode: compact
- Context hash: d69d804fd73f85ff25248d6fb47b1471fc98e7feea9add035fec04784a6f42e7

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/optimize-fw-plus-common/proposal.md

- Source: openspec/changes/optimize-fw-plus-common/proposal.md
- Lines: 1-25
- SHA256: 0b49c71920ecb3881f930d5bf8503622983526bdf76c0dfda8038f1494b9105c

```md
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

```

## openspec/changes/optimize-fw-plus-common/design.md

- Source: openspec/changes/optimize-fw-plus-common/design.md
- Lines: 1-47
- SHA256: 5a8ff253d88d50b68213128ff31507b362b30571c2735908b003dac0bc63c661

```md
## Context

`fw-plus-common` 位于 Core 层公共基础模块，被 Boot、POI 等模块复用。初步探索显示，`CommonsValidator`、`CommonsValidatorHolder` 等公共校验与线程上下文代码已有跨模块调用，优化时需要先看调用半径，再选择最小可验证改动。

本次 change 的重点不是新增能力，而是把 common 模块中已经存在的公共代码做严格收敛：能删则删，能复用则复用，能用 JDK / 已有依赖完成则不引入新东西。

## Goals / Non-Goals

**Goals:**
- 降低 `fw-plus-common` 中重复、低收益复杂度和潜在运行风险。
- 保持既有 public API 语义兼容，避免影响下游模块。
- 对非平凡逻辑改动补充最小测试或复用现有测试验证。
- 优先覆盖公共调用路径：校验工具、线程上下文、异常/结果封装、Web/Spring 工具。

**Non-Goals:**
- 不新增业务能力。
- 不引入新依赖。
- 不做跨模块架构重写。
- 不批量格式化或纯风格化修改。

## Decisions

1. **先定证据，再改代码。**
   - 做法：每个优化点先确认调用方、现有行为和可验证收益。
   - 替代方案：直接全模块扫描式重构。
   - 取舍：扫描式重构改动大、回归面宽，不符合严格优化的最小改动原则。

2. **公共 API 保持兼容。**
   - 做法：方法签名、返回语义和异常语义默认不变；内部实现可简化。
   - 替代方案：顺手清理 API 设计。
   - 取舍：common 是基础层，API 清理需要单独 change 和迁移计划。

3. **测试只补关键路径。**
   - 做法：涉及分支、线程上下文、异常转换、解析逻辑时补最小测试。
   - 替代方案：为整个 common 模块补完整测试矩阵。
   - 取舍：完整矩阵成本高，本次只保证优化不破坏核心行为。

4. **删除优先于新增抽象。**
   - 做法：移除无效代码、重复包装和未使用逻辑；不新增单实现接口、工厂或配置项。
   - 替代方案：建立新的公共优化框架。
   - 取舍：框架会增加维护面，不解决当前问题。

## Risks / Trade-offs

- 公共工具被多个模块间接依赖 → 通过 CodeGraph 调用半径、现有测试和最小新增测试降低回归风险。
- 部分优化收益可能不明显 → 没有清晰收益的改动不做。
- 保持 API 兼容会限制重构深度 → 深层 API 清理留给后续独立 change。

```

## openspec/changes/optimize-fw-plus-common/tasks.md

- Source: openspec/changes/optimize-fw-plus-common/tasks.md
- Lines: 1-14
- SHA256: e282c60b2f544b52c82193195cd68f8133b9a09de81358ad281b32d75b88de9b

```md
## 1. 优化点筛选

- [ ] 1.1 盘点 `fw-plus-common` 关键公共类及调用半径，筛掉无明确收益的改动。
- [ ] 1.2 确定本次只处理的最小优化清单，并记录不处理项。

## 2. 最小实现

- [ ] 2.1 对选定公共工具代码执行兼容性优化，优先删除、复用和简化。
- [ ] 2.2 为涉及分支、状态或异常语义的改动补充最小测试。

## 3. 验证

- [ ] 3.1 运行 `fw-plus-common` 相关 Maven 测试或最小可行测试命令。
- [ ] 3.2 如影响下游公共调用方，运行对应最小下游验证。

```

## openspec/changes/optimize-fw-plus-common/specs/common-module-optimization/spec.md

- Source: openspec/changes/optimize-fw-plus-common/specs/common-module-optimization/spec.md
- Lines: 1-22
- SHA256: cbff6a67cb004747cf0c347ab72717ef48ca55e9cde96e448833e9d15726ea6d

```md
## ADDED Requirements

### Requirement: Common module optimizations remain compatible
The system SHALL optimize `fw-plus-common` without changing the observable semantics of existing public APIs.

#### Scenario: Existing common API callers continue to work
- **WHEN** downstream modules call existing `fw-plus-common` public methods with previously supported inputs
- **THEN** the methods SHALL preserve their documented or existing observable behavior

### Requirement: Optimizations are evidence-driven and minimal
The system SHALL only apply common-module optimizations with a clear code-level reason, such as removing unused code, reusing existing helpers, simplifying duplicated logic, or reducing a demonstrated risk.

#### Scenario: Candidate optimization has no clear benefit
- **WHEN** a candidate change has no clear compatibility, maintainability, performance, or risk-reduction benefit
- **THEN** the change SHALL NOT be included in this work

### Requirement: Non-trivial logic changes are verified
The system SHALL include the smallest practical runnable verification for non-trivial logic changes in `fw-plus-common`.

#### Scenario: Optimization changes branching or stateful behavior
- **WHEN** an optimization changes branching, parsing, exception handling, thread-local state, or validation behavior
- **THEN** a targeted test or equivalent runnable check SHALL verify the affected behavior

```
