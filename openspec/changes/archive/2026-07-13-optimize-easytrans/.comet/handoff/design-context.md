# Comet Design Handoff

- Change: optimize-easytrans
- Phase: design
- Mode: compact
- Context hash: db35ec4ba377626d77779a83ab7377243db4455e54b2cf2cf235e45187e0d10b

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/optimize-easytrans/proposal.md

- Source: openspec/changes/optimize-easytrans/proposal.md
- Lines: 1-31
- SHA256: c7238ffb0e91aed0ca7639002dac50d9a0c2f80368b1433bc86adfcea40b0a13

```md
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

```

## openspec/changes/optimize-easytrans/design.md

- Source: openspec/changes/optimize-easytrans/design.md
- Lines: 1-47
- SHA256: 1cd4eb4bd9deb3bd682de8655acc443e12a6cabfe1cc75b673566f93fdfde578

```md
## Context

`fw-plus-easytrans` 为框架提供 Easy-Trans 扩展，当前主要由注解、通用 DTO、翻译服务、AOP 和 ResponseBodyAdvice 组成。调用链包含反射读取字段、集合批量翻译、代理 VO 创建、Boolean/Enum/Dictionary/RPC 等翻译服务。

约束：
- 仅修改 `fw-plus-easytrans` 三个子模块。
- 保持公开 API、注解、配置 key、返回结构兼容。
- 不新增依赖，不引入新抽象。
- 优先复用现有 Easy-Trans、JDK、Hutool 和项目已有工具。

## Goals / Non-Goals

**Goals:**
- 提高核心翻译链路对 null、空集合、反射异常、翻译失败的容错能力。
- 减少可确认的重复反射、重复集合遍历和无效转换。
- 为改动路径补最小验证，保证兼容输出不被破坏。

**Non-Goals:**
- 不重写 Easy-Trans 架构。
- 不改变 `@Trans`、`MateEasyTransDTO`、AOP 注解语义或配置项。
- 不把优化扩展到 Boot 自动配置、sample 或 product。
- 不做无指标支撑的大规模缓存层、异步化或并发模型改造。

## Decisions

1. **在共享转换链路修根因，而不是在调用点分别打补丁。**
   - 优先检查 `MateEasyTransUtils`、DTO 主键获取、AOP/ResponseBodyAdvice 入口等共享路径。
   - 替代方案：在各翻译服务单独增加 guard。拒绝原因：重复且容易漏 sibling caller。

2. **性能优化只处理明确重复开销。**
   - 允许缓存已存在的反射结果、复用已算出的集合首元素、避免重复 `iterator().next()`、避免无意义转换。
   - 不新增独立缓存框架或配置开关。
   - 替代方案：增加可配置缓存层。拒绝原因：当前没有证据需要，且会增加失效复杂度。

3. **兼容优先，失败兜底保持原返回。**
   - 翻译异常应记录日志并返回原始对象或已有兼容结果。
   - 不把已有宽容行为改成对外抛异常，除非原代码已经明确要求抛出。

4. **验证只补最小闭环。**
   - 优先覆盖被改动的翻译服务或工具方法。
   - 若完整 Spring 容器测试成本过高，使用最小单元测试覆盖稳定性和兼容输出。

## Risks / Trade-offs

- 反射/代理逻辑依赖 Easy-Trans 内部行为，改动过大可能破坏兼容。→ 只改共享路径中的明显问题，并用测试锁住输出。
- 性能热点可能不完整。→ 本 change 只修已确认热点，后续如有真实压测数据再扩展。
- 不修改 Boot/sample 会限制端到端覆盖。→ 本阶段接受该限制，用模块级编译和最小测试兜底。

```

## openspec/changes/optimize-easytrans/tasks.md

- Source: openspec/changes/optimize-easytrans/tasks.md
- Lines: 1-19
- SHA256: edd8734c4ce57820cc4685f7e3f777638c41d0767f783a8bde2c26e4d9b6eb5e

```md
## 1. 定位与验证准备

- [ ] 1.1 梳理 `fw-plus-easytrans` 当前翻译入口、工具方法和翻译服务调用链，确认实际修改点。
- [ ] 1.2 为将要修改的稳定性或性能路径补最小失败用例。

## 2. 稳定性优化

- [ ] 2.1 修复共享翻译链路中的 null、空集合、反射访问或翻译异常兜底问题。
- [ ] 2.2 确认 Boolean、Enum、Dictionary、RPC 翻译输出保持兼容。

## 3. 性能优化

- [ ] 3.1 移除批量翻译中的重复首元素读取、重复遍历或无效转换。
- [ ] 3.2 复用已有缓存或局部变量降低重复反射开销，不新增独立缓存层。

## 4. 验证

- [ ] 4.1 运行 `fw-plus-easytrans` 相关单元测试或最小验证。
- [ ] 4.2 运行模块编译，确认不破坏下游兼容编译。

```

## openspec/changes/optimize-easytrans/specs/easytrans-optimization/spec.md

- Source: openspec/changes/optimize-easytrans/specs/easytrans-optimization/spec.md
- Lines: 1-41
- SHA256: 2e61b634862627beb6f5501519829fc9a89cdd1bdc0ecaf0948e06402272b139

```md
## ADDED Requirements

### Requirement: 兼容的 easytrans 优化
系统 SHALL 在不改变现有公开 API、注解、配置 key 或响应结构的前提下，优化 `fw-plus-easytrans` 的稳定性与性能。

#### Scenario: 现有翻译输出保持兼容
- **WHEN** 通过现有注解和服务执行 Boolean、Enum、Dictionary 或 RPC 翻译
- **THEN** 翻译后的字段值和响应结构 MUST 与当前行为保持兼容

#### Scenario: 公开契约保持不变
- **WHEN** 下游模块基于现有 `fw-plus-easytrans` API、注解和配置 key 编译
- **THEN** 下游模块 MUST 不需要因为本次优化修改源码

### Requirement: 翻译失败被限制在翻译层内
当现有行为期望翻译结果为尽力而为时，系统 SHALL 将翻译失败限制在翻译层内。

#### Scenario: 结果翻译失败
- **WHEN** 方法返回结果的自动翻译因 null 值、反射访问或不支持的字段内容失败
- **THEN** 系统 MUST 仍返回原始结果，并记录失败日志

#### Scenario: 翻译空值和空集合
- **WHEN** 翻译收到 null 值、空集合或首个元素为 null 的集合
- **THEN** 翻译 MUST 完成且不抛出非预期运行时异常

### Requirement: 避免可避免的重复工作
系统 SHALL 在现有翻译流程中安全复用相同结果，避免重复反射或重复集合遍历。

#### Scenario: 批量翻译检查集合
- **WHEN** 批量翻译处理非空集合
- **THEN** 系统 MUST 避免框架扩展代码引入的重复首元素读取和不必要遍历

#### Scenario: 安全复用反射元数据
- **WHEN** 重复翻译操作需要同一类元数据
- **THEN** 系统 SHOULD 在不改变行为的前提下使用已有缓存或 JDK 安全复用方式

### Requirement: 优化具备最小验证
系统 SHALL 为变更后的 easytrans 行为提供最小可运行验证。

#### Scenario: 模块验证运行
- **WHEN** 优化后构建或测试 easytrans 模块
- **THEN** 如果优化后的稳定性或兼容行为回归，验证 MUST 失败

```
