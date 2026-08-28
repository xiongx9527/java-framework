## 1. 一致性回归测试

- [x] 1.1 在 AI autoconfigure 模块新增跨 Boot、Cloud、AI 的元数据集合一致性测试，并运行该测试确认修复前因 AI 缺失条目而失败，同时确认 `LangChain4jAutoConfiguration` 被精确排除

## 2. 元数据修复

- [x] 2.1 根据 `spring.factories` 补齐 AI `AutoConfiguration.imports`，运行新增测试确认三层清单转绿且本次未同步 `LangChain4jAutoConfiguration`

## 3. 回归验证

- [x] 3.1 运行 `fw-plus-ai-boot-autoconfigure` 相关 Maven 测试和资源构建检查，确认清单修复未破坏现有自动配置测试且打包资源包含预期内容
