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
