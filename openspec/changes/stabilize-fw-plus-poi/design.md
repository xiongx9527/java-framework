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
