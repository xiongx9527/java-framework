---
change: biz-exception-finalize
design-doc: docs/openspec/changes/biz-exception-finalize/design.md
base-ref: 3b29eeb74d9eca4926e91f168b6f009e78e0f93f
---

<!-- comet-task-authority: docs/openspec/changes/biz-exception-finalize/tasks.md -->

# 实施计划：biz-exception-finalize

本计划对已完成的「异常统一继承 BizException」做收尾确认。执行策略：autonomous + standard 审查。本 change 不含任何代码改动，仅做验证与遗漏检查。

## 任务与验收

### T1 — 运行异常相关单元测试 <!-- comet-task-ref:8e8e3878-491f-4d50-a91b-09a04fa71faa -->
- 范围：运行 `BizExceptionFixedCodeTest`、`BootExceptionCodeTest` 等异常相关单元测试，确认全绿。
- 验收：`BizExceptionFixedCodeTest` 5/5、`BootExceptionCodeTest` 2/2 通过；`BizExceptionTest` 为遗留非 JUnit 类跳过。

### T2 — 静态校验异常继承链 <!-- comet-task-ref:0fdc3c0b-c549-4c5d-9bc0-8de7ebb04c62 -->
- 范围：扫描 `fw-plus-core/boot/cloud/ai` 主源码全部 `Throwable` 派生异常类的 `extends` 链。
- 验收：所有异常类均直接或间接继承 `BizException`/`BaseRuntimeException`/`DataChangeException`，无遗漏。

### T3 — 遗漏检查结论 <!-- comet-task-ref:b6aa6ae8-92f1-46f0-a163-6ec14b6dca6d -->
- 范围：汇总校验结果；对发现的未继承异常仅记录为后续待办，不在本 change 修改。
- 验收：输出结论（本次无遗漏）；无代码改动。

### T4 — 留下检查记录 <!-- comet-task-ref:c112aa25-f1b5-4ef3-9391-a4f923443013 -->
- 范围：在 change 目录写入 `VERIFICATION.md`。
- 验收：`VERIFICATION.md` 存在并汇总校验结果与命令输出摘要。

## 实施顺序

T2（静态校验）→ T1（单元测试）→ T3（结论）→ T4（记录）。本 change 为只读验证，无实现写入。

## 风险与回退

- 低风险：若静态校验发现未继承异常，仅记录为后续 change 待办，不在本 change 修改（按确认 scope）。
- 不补齐固定 CODE 常量（按确认，本次不做）。
