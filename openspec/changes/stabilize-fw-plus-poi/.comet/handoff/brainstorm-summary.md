# Brainstorm Summary

- Change: stabilize-fw-plus-poi
- Date: 2026-07-13

## 确认的技术方案

采用最小防御性修复。保持现有公开 API、EasyPoi 版本和主调用链不变，在 FwPlus 扩展入口补齐空参数、普通 EasyPoi 参数、空集合、`needTrans=false`、include/exclude 为空等边界处理。

重点检查并修改：

- `FwPlusExcelExportService`
- `FwPlusExcelBatchExportService`
- `FwPlusExcelTransService`
- `FwPlusExcelImportService`
- `ExcelExportUtil` 的模板导出分支

`TransUtils` 继续作为翻译入口，本 change 不重写 EasyTrans 行为。

## 关键取舍与风险

- 推荐方案优先降低回归风险，不统一重写导入/导出转换模型。
- `TransUtils` 仍作为字段翻译入口；本 change 不重写 EasyTrans 行为。
- `FwPlusTemplateExportParams` 当前未声明独立 `needTrans` 字段，模板翻译能力不是本次稳定性优化的主目标；本次只确保标准 `TemplateExportParams` 不受 FwPlus 逻辑影响。
- 现有工作区存在其他 change 的未提交改动，后续验证以 POI 定向命令为准。

## 测试策略

- 优先补 `FwPlusExcelTransService` 默认方法边界测试，覆盖 null 参数、`needTrans=false`、空集合和 include/exclude 为空。
- 补可轻量执行的导出入口测试，验证标准 `ExportParams` 不触发 FwPlus 参数空引用。
- 视依赖成本补导入服务或模板导出入口测试；若完整 Excel 文件生成成本过高，使用转换层单元测试加 Maven 编译验证兜底。

## Spec Patch

无。当前 delta spec 已覆盖导出、导入、模板导出和验证要求。
