# Final Review Fix Report

## 状态

已完成全部 Critical / Important 阻断项修复。

## 修复摘要

- 为 model core 自动配置和 AskData model registry 桥接增加 classpath 条件，缺少 optional model core 时不再解析相关 Bean。
- AskData 同时合并直接注入的 Spring `ChatLanguageModel` / `EmbeddingModel` Bean 与新 `LangChain4jModelRegistry` 模型，默认 key 仍来自 `AskDataProperties`。
- 反射模型 adapter 现在应用 `properties`，支持 String、Integer/int、Long/long、Double/double、Float/float、Boolean/boolean 和 Duration 转换；不支持的扩展属性会明确失败。
- Azure OpenAI adapter 单独配置 `baseUrl -> endpoint` alias，不影响 Qianfan 等 provider；真实 0.35.0 Azure builder 构建测试通过。
- Magic runtime 模型覆盖 Spring 默认 key 时，`getDefault()` 返回 runtime 模型。
- `enabled=false` 会清理同 key runtime 模型且不创建替代模型。
- `ModelDataSourceInfo.simple()` 递归清理 Map、List/Collection、array 中的敏感 key，同时保留非敏感值。

## 验证

- 指定定向测试：23 tests，0 failures，0 errors。
- `fw-plus-ai-boot-autoconfigure` 全模块测试：59 tests，0 failures，0 errors。
- Java 9+ API 扫描：无命中。
- Magic 入口/资源类型扫描：保持 `model` / `model-datasource`，未发现旧 `langchain4j` 入口或 AUDIO/VIDEO/PDF modelType。
- 本次变更范围 `git diff --check`：通过。

## 未处理项

- 未处理 Minor console lockfile / `npm ci`，未修改 console 构建，也未提交 `node_modules` 或 `dist`。
- 工作区原有且按要求禁止修改的 `.superpowers/sdd/task-6-brief.md` 存在 EOF 空行告警，因此全工作区 `git diff --check` 仍会报告该既有文件；本次变更范围检查通过。
