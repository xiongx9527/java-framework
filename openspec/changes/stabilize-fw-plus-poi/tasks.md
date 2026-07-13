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
