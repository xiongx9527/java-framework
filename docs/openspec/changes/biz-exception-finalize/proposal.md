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
