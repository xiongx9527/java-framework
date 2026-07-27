# Task 7 Report: Magic model 模块、Controller 与插件配置

## 状态

完成。提交为 `feat: add magic model plugin backend`。

## 实现内容

- 新增 `@MagicModule("model")` 的 `ModelModule`，支持显式模型 key 和 `Options.DEFAULT_DATA_SOURCE` 默认模型解析，输出统一 `LangChain4jModel` 包装对象或原生模型对象。
- 新增 `MagicModelController`：模型数据源测试端点使用 `LangChain4jModelFactory.create(definition, adapters)` 验证；失败响应固定为通用信息，日志仅记录数据源 key；列表端点逐项调用 `simple()`。
- 新增 `MagicAPIModelConfig` 与条件化的 `MagicAPIModelConfiguration`，注册动态数据源、资源存储、动态注册表和模块 Bean；插件元数据为 `model` / `MagicModel` / `magic-model.1.0.0.iife.js`。
- `MagicApiPluginAutoConfiguration` 已导入新配置，未导入旧 `MagicAPILangchain4jConfiguration`。

## TDD 证据

- RED：先新增四个 Task 7 测试文件，运行指定 Maven 命令在 testCompile 阶段失败，原因是 `module`、`web`、`starter` 目标包和类尚不存在。
- GREEN：实现后运行六个指定测试类，最终共 19 个测试通过，0 failures，0 errors，0 skipped。

## 验证命令

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -Dtest=MagicDynamicModelDataSourceTest,ModelDataSourceMagicDynamicRegistryTest,ModelModuleTest,MagicModelControllerTest,MagicAPIModelConfigurationTest,MagicAPIModelAutoConfigurationIntegrationTest test
```

Maven Wrapper 在本工作树可正常执行，因此未使用 `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn` 回退命令。

## 注意事项

- 测试运行存在仓库既有的多 SLF4J binding 和过时/未检查 API 警告，不影响本次 19 个聚焦测试结果。
- 全仓 `git diff --check` 命中已有 `.superpowers/sdd/task-6-brief.md` 的 EOF 空白行；本任务范围文件单独检查通过，未修改该用户已有文件。
