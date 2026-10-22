# Comet Design Handoff

- Change: biz-exception-finalize
- Phase: design
- Mode: compact
- Context hash: 4c9b503ae5c7ee80a861c63c39623f2ee3ecf4ec8800c8abc0bb3993abed713f

Generated-by: comet-handoff.sh
Task hash policy: task-content-v1. Read tasks.md for live completion; excerpts are design-time context.

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## docs/openspec/changes/biz-exception-finalize/proposal.md

- Source: docs/openspec/changes/biz-exception-finalize/proposal.md
- Lines: 1-25
- SHA256: da723dcd1675d46040ac8cb3ced748d610c8f194beeba17e9f47dca4e27c15a5

```md
# Proposal

## Why

先前的提交已将全仓主源码异常类统一改为继承 `BizException`（`d9e63e8ff` 固定状态码、`908600fa2` 剩余 7 个异常类继承），但缺少一次明确的收尾确认：运行相关测试、校验继承链完整性、输出遗漏检查结论。本 change 仅做验证与遗漏检查，不修改任何异常类。

## What Changes

- 运行 `BizException`/`BaseRuntimeException` 相关单元测试（`BizExceptionTest`、`BizExceptionFixedCodeTest`、`*ExceptionI18nTest`、`*ExceptionCodeTest` 等），确认全绿。
- 静态校验全仓主源码异常类的继承链，确认均直接或间接继承 `BizException`。
- 输出遗漏检查结论（若发现异常类未继承，记录为后续 change 的待办，不在本 change 修改）。
- **不**修改异常类继承关系；**不**补齐固定 CODE 常量（按确认，本次不做）。

## Capabilities

### New Capabilities
<!-- 无：本 change 不改变任何 capability 的行为，仅做验证与检查 -->

### Modified Capabilities
<!-- 无 -->

## Impact

- 影响范围：仅测试执行与静态检查，无代码改动。
- 风险：低。若检查发现未继承异常，仅记录不修复。

```

## docs/openspec/changes/biz-exception-finalize/design.md

- Source: docs/openspec/changes/biz-exception-finalize/design.md
- Lines: 1-24
- SHA256: 34923a5452d3563fc281825445abb36d8c5b241131e95087d357a63037128040

```md
---
comet_change: biz-exception-finalize
role: technical-design
canonical_spec: openspec
---

# Design: biz-exception-finalize

## 背景

本 change 不改变任何代码行为，仅对已完成的「异常统一继承 BizException」做收尾确认。继承关系已在先前提交落地（主源码全部异常类直接或间接继承 `BizException`）。

## 验证策略

1. **单元测试**：运行 `fw-plus-common` 的 `BizExceptionTest` / `BizExceptionFixedCodeTest`，以及全仓各模块的 `*ExceptionI18nTest` / `*ExceptionCodeTest`，确认全绿。
2. **静态继承链校验**：对 `fw-plus-core`/`fw-plus-boot`/`fw-plus-cloud`/`fw-plus-ai` 主源码（`src/main/java`，排除测试）扫描 `class *Exception`，确认每个均 `extends BizException` / `BaseRuntimeException`（→BizException）/ `DataChangeException`（→BizException），或为非异常工具类（如 `ExceptionBuilder`）。
3. **遗漏处理**：若发现未继承的异常类，仅记录到 `VERIFICATION.md` 作为后续 change 待办，不在本 change 修改。
4. **产物**：在 change 目录写入 `VERIFICATION.md` 汇总校验结果与命令输出摘要，供归档引用。

## 范围边界

- 不修改异常类继承关系。
- 不补齐固定 CODE 常量（按确认，本次不做）。
- 不做 i18n 或授权相关改动。

```

## docs/openspec/changes/biz-exception-finalize/tasks.md

- Source: docs/openspec/changes/biz-exception-finalize/tasks.md
- Lines: 1-6
- SHA256: 8c219fd739c2f4ff0950d053b3d68d1cde4024493eb168f03ea75a9fc3f2a9f0

```md
# Tasks: biz-exception-finalize

- [ ] 1. 运行 `fw-plus-common` 的 `BizExceptionTest` / `BizExceptionFixedCodeTest` 及全仓 `*ExceptionI18nTest` / `*ExceptionCodeTest`，确认全绿
- [ ] 2. 静态校验 `fw-plus-core/boot/cloud/ai` 主源码全部异常类的继承链，确认均直接或间接继承 `BizException`
- [ ] 3. 输出遗漏检查结论：列出校验结果；对发现的未继承异常仅记录为后续待办，不在本 change 修改
- [ ] 4. 在 change 目录留下检查记录（如 `VERIFICATION.md`），供归档时引用

```

## docs/openspec/changes/biz-exception-finalize/.openspec.yaml

- Source: docs/openspec/changes/biz-exception-finalize/.openspec.yaml
- Lines: 1-3
- SHA256: 9dce7645e8d23214043e04d1f4443f95ea5ca40f4320e09322b23b0ed2bc02ec

```md
schema: spec-driven
created: 2026-10-21
skip_specs: true

```
