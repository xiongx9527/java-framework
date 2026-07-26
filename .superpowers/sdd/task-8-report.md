# Task 8 Report: Magic Console 插件模块迁移与旧入口删除

## 完成内容

- 新增 `fw-plus-ai-magicapi-plugin-model` Maven 模块及 `magic-model.1.0.0.iife.js` 控制台资源。
- 控制台注册插件 `model` 和资源类型 `model-datasource`，表单包含 `name`、`key`、`provider`、`modelType`、`enabled`、`baseUrl`、`apiKey`、`modelName`、`properties`。
- `apiKey` 使用密码输入控件；列表注册元数据不包含敏感字段。
- 聚合 POM、AI BOM 与 MagicAPI 样例均从旧 LangChain4j 插件切换到 model 插件。
- 删除旧 starter 模块和 `magicapi/plugin/langchain4j` 后端包。
- AskData 原先依赖已删除的 `LangChain4jModelInfo`，已改用公共 `LangChain4jModelRegistry` 并按模型运行时类型提取聊天与向量模型。

## TDD 记录

1. 创建 `src/console/test/magic-model-contract.test.cjs`。
2. 首次执行 `npm test` 失败，原因是新模块尚无 `package.json`，符合任务 brief 的 RED 预期。
3. 实现控制台与模块后，`npm test` 通过。

## 验证结果

- `npm test`：通过，1 个契约测试。
- `npm run build`：通过，生成 12.11 KiB `magic-model.1.0.0.iife.js`。
- `./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model -am -DskipTests install`：通过。
- `./mvnw -pl fw-plus-ai-samples/fw-plus-ai-magicapi-sample -am -DskipTests package`：通过。
- `./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=LangChain4jAskDataModelRegistryTest,MagicAPIModelAutoConfigurationIntegrationTest test`：通过；Surefire 实际执行 Model 集成测试 2 项，旧 JUnit4 AskData 测试已完成 `testCompile`，但未被当前 JUnit Platform 发现。
- 旧模块目录、旧后端包目录与四项旧入口文本的结构检查均通过。
- `git diff --check -- fw-plus-ai`：通过。

## 注意事项

- 全工作树的 `git diff --check` 仍会报告用户已有的 `.superpowers/sdd/task-6-brief.md` 末尾空白；本任务未修改该文件。
