# Comet Design Handoff

- Change: stabilize-fw-plus-poi
- Phase: design
- Mode: compact
- Context hash: 4e0eb64f0db3025a823bb4b8ff304f2b36526624d73e80cc01453f4d3359c689

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/stabilize-fw-plus-poi/proposal.md

- Source: openspec/changes/stabilize-fw-plus-poi/proposal.md
- Lines: 1-26
- SHA256: b7f091d56aa4349506ce434005d56e54a4b827181ba29cfa8f551ba706c333d3

```md
## Why

`fw-plus-poi` 当前对 EasyPoi 的导入、普通导出、大批量导出和模板导出做了 FwPlus 扩展，但稳定性边界缺少明确约束和测试覆盖。空参数、普通 EasyPoi 参数、空集合、翻译开关和字段过滤组合如果处理不一致，容易在业务导入导出场景中触发非预期异常或行为漂移。

## What Changes

- 增强 `fw-plus-poi-easypoi` 在导入/导出链路中的防御性处理，优先覆盖空参数、空集合、非 FwPlus 参数和翻译配置边界。
- 保持现有公开 API、依赖版本和默认行为不变，不升级 EasyPoi/POI。
- 为核心稳定性场景补充可执行测试，允许新增必要测试依赖和测试辅助类。
- 若实现过程中发现需要跨模块修改或扩大到 EasyTrans/EasyPoi 基础能力，暂停并重新确认范围。

## Capabilities

### New Capabilities

- `poi-easypoi-stability`: 约束 FwPlus EasyPoi 扩展在导入、普通导出、大批量导出和模板导出边界场景下的稳定性行为。

### Modified Capabilities

- 无。

## Impact

- 主要影响 `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi` 下的 FwPlus EasyPoi 扩展类和测试代码。
- 可能涉及测试 POM 依赖，但不改变生产依赖版本。
- 不改变公开 API、不引入数据库 schema 变更、不触碰 `fw-plus-mp`、`fw-plus-nosql` 或其他当前未提交改动。

```

## openspec/changes/stabilize-fw-plus-poi/design.md

- Source: openspec/changes/stabilize-fw-plus-poi/design.md
- Lines: 1-64
- SHA256: 402f209c2f7635bef90ebede884dce7a3f358f9a78bf4c9299042eff1bc4371c

```md
## Context

`fw-plus-poi-easypoi` 在 EasyPoi 基础上增加了 FwPlus 参数类型、字段翻译/过滤和导入导出适配。当前主要扩展点包括：

- `FwPlusExportParams`、`FwPlusImportParams`、`FwPlusTemplateExportParams`：承载 `needTrans`、`includeFields`、`excludeFields`。
- `FwPlusExcelTransService`：在导入/导出集合进入 EasyPoi 处理前执行转换。
- `FwPlusExcelExportService`、`FwPlusExcelBatchExportService`、`FwPlusExcelImportService`、`ExcelExportUtil`：接入普通导出、大批量导出、导入和模板导出链路。

本次优化面向稳定性，不引入新功能，不升级 EasyPoi/POI，不改变公开 API。当前工作区存在其他 change 的未提交改动，本 change 只触碰 POI 相关产物和实现。

## Goals / Non-Goals

**Goals:**

- 在普通 EasyPoi 参数和 FwPlus 参数混用时保持兼容，不因可选 FwPlus 参数为空而触发异常。
- 明确 `needTrans=false`、参数为空、集合为空、include/exclude 为空等边界行为。
- 将导入、普通导出、大批量导出、模板导出的关键稳定性场景纳入测试。
- 保持 Java 1.8 兼容，遵循现有模块风格。

**Non-Goals:**

- 不调整 EasyPoi、Apache POI 或 EasyTrans 的版本。
- 不新增业务功能或新的导入导出模式。
- 不重构整个 EasyPoi 封装层，不改变现有公开方法签名。
- 不处理 Word/WPS/View 层，除非实现中发现其直接依赖本次稳定性边界。

## Decisions

### 决策 1：以防御性边界处理为主，不扩大为行为重构

优先在 FwPlus 扩展类入口处理空参数、普通 EasyPoi 参数和空集合，避免对 EasyPoi 原始流程产生额外行为变化。

替代方案是统一重写所有导入导出入口的转换策略。该方案一致性更强，但会扩大回归风险，不适合作为本次稳定性优化的默认路径。

### 决策 2：翻译/过滤转换继续复用现有 `TransUtils`

`TransUtils` 已是当前模块的字段转换入口。本次只保证调用前置条件和参数边界稳定，不重新实现字段过滤逻辑。

替代方案是抽取新的转换服务或策略类。当前重复点较少，且需求聚焦稳定性，新增抽象会增加维护成本。

### 决策 3：测试优先覆盖可独立验证的边界

新增测试应覆盖：

- 非 FwPlus `ExportParams` 走普通导出入口时不触发 FwPlus 参数空引用。
- `FwPlusExportParams` / `FwPlusImportParams` 在 `needTrans=false` 时返回原集合。
- 空集合、空 include/exclude 配置不会触发额外异常。
- 模板导出在非 FwPlus 模板参数下保持原 EasyPoi 行为。

如完整 Excel 文件生成测试依赖过重，可先测试转换服务和工具入口的稳定性，再用模块编译验证兜底。

## Risks / Trade-offs

- [Risk] EasyPoi 部分类或方法可见性限制导致测试难以直接覆盖完整链路 → 优先覆盖 FwPlus 扩展入口和转换服务，必要时通过最小 POJO 导出测试补充。
- [Risk] `TransUtils` 的内部行为可能依赖 EasyTrans 上下文 → 测试不强行验证外部翻译服务，只验证 FwPlus 参数边界和集合处理契约。
- [Risk] 现有未提交改动会干扰全量工作区状态 → 仅运行 POI 定向 Maven 验证，并在结果中说明未处理的外部 dirty worktree。

## Migration Plan

无需数据迁移或配置迁移。实现完成后运行 `fw-plus-poi` 或 `fw-plus-poi-easypoi` 定向测试/编译；若失败，停留在 build 阶段修复，不进入 verify。

## Open Questions

- 是否需要在 build 阶段补充真实 Excel workbook 生成测试，取决于现有测试依赖和 EasyPoi 初始化成本。

```

## openspec/changes/stabilize-fw-plus-poi/tasks.md

- Source: openspec/changes/stabilize-fw-plus-poi/tasks.md
- Lines: 1-16
- SHA256: ec701f1ab376a5983975b1e15519dd76aa3e14b879a5fb16d73ab90c0a9c918e

```md
## 1. 边界调查

- [ ] 1.1 梳理 `fw-plus-poi-easypoi` 导入、普通导出、大批量导出和模板导出的 FwPlus 接入点。
- [ ] 1.2 识别空参数、普通 EasyPoi 参数、空集合、`needTrans=false` 和 include/exclude 为空时的实际风险点。

## 2. 稳定性实现

- [ ] 2.1 修复普通导出和大批量导出中 FwPlus 参数为空或非 FwPlus 参数时的防御性处理。
- [ ] 2.2 修复导入转换中 FwPlus 参数为空、翻译关闭或字段过滤为空时的边界行为。
- [ ] 2.3 检查模板导出在标准 `TemplateExportParams` 下是否保持原 EasyPoi 行为，必要时补充防御性处理。

## 3. 测试与验证

- [ ] 3.1 为导入/导出转换服务补充稳定性边界单元测试。
- [ ] 3.2 为可轻量执行的导出入口补充普通参数或 FwPlus 参数边界测试。
- [ ] 3.3 运行 `fw-plus-poi` 定向 Maven 测试或编译验证，并记录受现有 dirty worktree 影响的外部范围。

```

## openspec/changes/stabilize-fw-plus-poi/specs/poi-easypoi-stability/spec.md

- Source: openspec/changes/stabilize-fw-plus-poi/specs/poi-easypoi-stability/spec.md
- Lines: 1-37
- SHA256: 75c0e1c5d615c63a9c29420dcfee72c42c3d30b1a34f28af05e29b2e5e0b8d04

```md
## ADDED Requirements

### Requirement: FwPlus EasyPoi export SHALL tolerate non-FwPlus export parameters
The system SHALL preserve EasyPoi-compatible export behavior when callers use standard `ExportParams` instead of `FwPlusExportParams`.

#### Scenario: Standard export params are used
- **WHEN** an export path receives a standard `ExportParams` instance
- **THEN** the FwPlus extension SHALL avoid dereferencing missing FwPlus-specific parameters

#### Scenario: FwPlus export translation is disabled
- **WHEN** export parameters are `FwPlusExportParams` and `needTrans` is `false`
- **THEN** the export conversion step SHALL return the original collection without applying field filtering

### Requirement: FwPlus EasyPoi import SHALL tolerate empty or disabled translation settings
The system SHALL handle FwPlus import conversion safely when import parameters are absent, translation is disabled, or field filters are empty.

#### Scenario: Import params are absent
- **WHEN** an import conversion path receives no FwPlus import parameters
- **THEN** the conversion step SHALL return the original collection without throwing a parameter-related exception

#### Scenario: Import translation is disabled
- **WHEN** import parameters are `FwPlusImportParams` and `needTrans` is `false`
- **THEN** the import conversion step SHALL return the original collection without applying field filtering

### Requirement: FwPlus EasyPoi template export SHALL preserve standard template behavior
The system SHALL keep standard EasyPoi template export behavior when callers use standard `TemplateExportParams`.

#### Scenario: Standard template params are used
- **WHEN** template export receives a standard `TemplateExportParams` instance
- **THEN** the FwPlus extension SHALL skip FwPlus-specific translation handling and continue with the standard EasyPoi template export flow

### Requirement: POI stability changes SHALL be covered by targeted verification
The system SHALL include targeted verification for the FwPlus EasyPoi stability boundaries introduced by this change.

#### Scenario: Boundary tests run
- **WHEN** the POI module verification command is executed
- **THEN** tests or compilation SHALL validate the relevant import/export boundary behavior without requiring unrelated modules to change

```
