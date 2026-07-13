---
comet_change: stabilize-fw-plus-poi
role: technical-design
canonical_spec: openspec
---

# stabilize-fw-plus-poi 技术设计

## 背景

`fw-plus-poi-easypoi` 在 EasyPoi 基础上增加了 FwPlus 参数类型、字段翻译和字段过滤能力。当前扩展链路覆盖普通导出、大批量导出、导入和模板导出，但部分入口默认按 FwPlus 参数处理，遇到标准 EasyPoi 参数、空集合、空 include/exclude 或 `needTrans=false` 时存在空引用或行为漂移风险。

本次优化采用最小防御性修复：保持现有公开 API、EasyPoi/POI/EasyTrans 版本和主调用链不变，仅在 FwPlus 扩展入口补齐稳定性边界。

## 目标

- 标准 EasyPoi 参数与 FwPlus 参数混用时保持兼容。
- `needTrans=false` 时转换层直接返回原集合，不做字段过滤。
- 空参数、空集合、空 include/exclude 配置不触发参数相关异常。
- 标准 `TemplateExportParams` 保持 EasyPoi 原有模板导出行为。
- 为核心边界补充可执行测试或定向编译验证。

## 非目标

- 不升级 EasyPoi、Apache POI 或 EasyTrans 版本。
- 不改变公开方法签名、Maven 坐标或生产依赖版本。
- 不重构整个 EasyPoi 封装层。
- 不重写 `TransUtils` 或 EasyTrans 字段翻译语义。
- 不处理 Word/WPS/View 层，除非发现其直接依赖本次稳定性边界。
- 不触碰当前工作区内 MP、NOSQL、Dify 等无关未提交改动。

## 技术方案

### 1. 扩展入口防御

实现阶段重点检查并修复以下入口：

- `FwPlusExcelExportService`
- `FwPlusExcelBatchExportService`
- `FwPlusExcelTransService`
- `FwPlusExcelImportService`
- `ExcelExportUtil` 的模板导出分支

原则是先识别参数类型，再进入 FwPlus 专属逻辑：

- 参数为 `null` 或非 FwPlus 类型时，跳过 FwPlus 翻译/过滤，回到原 EasyPoi 兼容流程。
- 集合为 `null` 时返回 `null`，空集合返回原集合。
- `needTrans=false` 时返回原集合，不调用 `TransUtils`。
- include/exclude 为 `null` 或空集合时，不因过滤配置缺失抛异常。

### 2. 导出链路

普通导出和大批量导出只在参数确认为 `FwPlusExportParams` 且 `needTrans=true` 时执行 FwPlus 转换。标准 `ExportParams` 必须继续走 EasyPoi 原有导出路径，不能强转为 FwPlus 参数，也不能读取 FwPlus 专属字段。

转换层保持原集合语义：关闭翻译、参数缺失或空集合时直接返回原集合，避免为了稳定性修复引入额外复制或顺序变化。

### 3. 导入链路

导入转换只在参数确认为 `FwPlusImportParams` 且 `needTrans=true` 时执行 FwPlus 转换。参数缺失、翻译关闭或字段过滤为空时返回原集合。

导入实现不改变 EasyPoi 文件解析、校验和对象创建流程，只补齐 FwPlus 转换前置条件，降低导入场景中参数组合触发异常的概率。

### 4. 模板导出链路

模板导出需要保留标准 `TemplateExportParams` 行为。只有参数确认为 `FwPlusTemplateExportParams` 且确实需要 FwPlus 专属处理时，才进入扩展逻辑。

`FwPlusTemplateExportParams` 当前未声明独立 `needTrans` 字段，因此模板翻译能力不是本次稳定性优化的主目标。本次只确保标准模板参数不会被 FwPlus 逻辑误处理。

### 5. 翻译入口

`TransUtils` 继续作为字段翻译/过滤入口。本 change 不重新设计翻译策略，不新增抽象服务，也不改变 EasyTrans 行为。实现只保证调用 `TransUtils` 前的参数、开关和集合状态满足安全前置条件。

## 兼容性要求

- 所有现有 public 方法签名保持不变。
- 标准 EasyPoi 参数调用路径保持兼容。
- FwPlus 参数默认行为保持兼容，仅补齐空值和关闭翻译场景的稳定性。
- Java 代码保持 Java 1.8 兼容，不使用 Java 9+ API。
- 不做无关格式化，不修改无关模块。

## 风险与控制

| 风险 | 控制方式 |
| --- | --- |
| EasyPoi 部分类或方法可见性限制导致完整链路测试成本高 | 优先覆盖转换服务和扩展入口，必要时用最小 POJO 导出测试补充 |
| `TransUtils` 依赖 EasyTrans 上下文 | 测试只验证 FwPlus 参数边界和集合处理契约，不伪造外部翻译能力 |
| 模板导出参数能力与普通导出不完全一致 | 仅保证标准 `TemplateExportParams` 不受 FwPlus 逻辑影响，不扩展模板翻译能力 |
| 工作区已有其他未提交改动 | 验证命令限定 POI 模块范围，并在验证结果中说明外部 dirty worktree 不属于本 change |

## 测试策略

优先补充 `fw-plus-poi-easypoi` 内的定向单元测试，覆盖转换层和低成本入口行为。

必测场景：

1. 标准 `ExportParams` 进入导出入口时不触发 FwPlus 参数空引用。
2. `FwPlusExportParams` 在 `needTrans=false` 时返回原集合。
3. `FwPlusImportParams` 在 `needTrans=false` 时返回原集合。
4. 导入转换参数为 `null` 时返回原集合，不抛参数相关异常。
5. 空集合、空 include/exclude 配置不触发额外异常。
6. 标准 `TemplateExportParams` 进入模板导出时保持 EasyPoi 原流程。

验证命令优先限定 POI 模块：

```bash
cd fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi
cd fw-plus-core && ./mvnw compile -pl fw-plus-project/fw-plus-poi -DskipTests
```

如模块依赖要求完整 reactor，再按 Maven 提示补充 `-am`。若完整 Excel workbook 生成测试成本过高，以转换层单元测试加模块编译验证兜底。

## 实施顺序

1. 用 CodeGraph 梳理导入、普通导出、大批量导出和模板导出的 FwPlus 接入点。
2. 为 `FwPlusExcelTransService` 先补稳定性边界测试。
3. 修复普通导出和大批量导出的 FwPlus 参数识别与空值处理。
4. 修复导入转换的参数缺失、翻译关闭和字段过滤为空场景。
5. 检查模板导出标准参数路径，必要时补防御性分支。
6. 运行 POI 定向测试或编译验证。

## Spec Patch

无。当前 OpenSpec delta spec 已覆盖导出、导入、模板导出和验证要求。
