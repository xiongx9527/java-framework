# Brainstorm Summary

- Change: token-names-fallback
- Date: 2026-07-01

## 确认的技术方案

提取 `getTokenValueByTokenName(String)` 私有辅助方法封装请求体/Header/Cookie 三来源读取逻辑，在 `getTokenValueNotCut()` 中先用 `tokenName` 调用，无值后遍历 `tokenNames` 数组（跳过与 `tokenName` 重复的条目），返回第一个非空值。

## 关键取舍与风险

- 提取辅助方法消除代码重复 vs 内联循环：选择提取，代码更清晰
- 遍历时跳过与 tokenName 重复的 tokenNames 条目：避免默认配置下的无意义重复查询
- 风险极低：仅一个方法改动，向后兼容，性能影响可忽略

## 测试策略

- `mvn compile` 编译验证
- 无现有单元测试，依赖集成测试 + 手动验证

## Spec Patch

无
