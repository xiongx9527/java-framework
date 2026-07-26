# Task 9 验证报告：LangChain4j Model Magic-API 文档、示例与全量验证

## 完成内容

- 在 AI Magic-API 样例的 `application.yaml` 中保留既有 `fw.plus` 配置，并加入独立的 `fw-plus.ai.langchain4j.model` 模型配置和 `magic-api.plugin.model.enabled: true`。
- 在中英文 AI README 中说明模型 Starter 最小依赖、Provider SDK 显式依赖、`model-datasource` JSON、`model` 脚本用法，以及旧 Magic `langchain4j` 插件迁移路径。
- 在核心 LangChain4j Java 8 兼容说明中记录 0.35.0 命名边界与 Chat 多模态内容边界。

## 文档约束

- 固定 LangChain4j `0.35.0`：`CHAT` 对应 `ChatLanguageModel`，`STREAMING_CHAT` 对应 `StreamingChatLanguageModel`。
- 高版本 `ChatModel` / `StreamingChatModel` 仅为升级后的命名，当前版本不能使用。
- `TextContent`、`ImageContent`、`AudioContent`、`VideoContent`、`PdfFileContent` 是 Chat 请求的多模态内容；不新增 `AUDIO`、`VIDEO`、`PDF` modelType，也不存在独立的 `AudioModel` / `VideoModel` / `PdfModel`。
- Magic 资源类型为 `model-datasource`，脚本入口为 `model`。

## 验证结果

### Step 1：样例 YAML 与文档契约

```bash
ruby -e 'require "yaml"; YAML.load_file(ARGV[0]); puts "application.yaml: valid"' fw-plus-ai/fw-plus-ai-samples/fw-plus-ai-magicapi-sample/src/main/resources/application.yaml
rg -n 'fw-plus\\.ai\\.langchain4j\\.model|model-datasource|ChatLanguageModel|StreamingChatLanguageModel|ChatModel|StreamingChatModel|AudioModel|VideoModel|fw-plus-ai-magicapi-plugin-model' fw-plus-ai/README.zh-CN.md fw-plus-ai/README.md fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/README.md
```

结果：通过。YAML 可解析；中英文文档与核心说明均包含模型配置、资源、迁移、版本命名和多模态边界。

### Step 2：Core 定向测试

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model -am test
./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding -am test
```

结果：初次运行时 Maven 在反应堆项目读取阶段退出，因为 `fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/pom.xml` 声明的 `fw-plus-ai-langchain4j-cassandra`、`fw-plus-ai-langchain4j-infinispan`、`fw-plus-ai-langchain4j-neo4j` 子模块在当前工作树中不存在。修复方式：移除这些失效 module 引用，并同步移除 core BOM 与 README 中对这三个当前不存在 artifact 的管理/说明。

使用 Zulu 8 重跑两条 Maven Wrapper 命令后均通过：

- `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home ./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model -am test`：`BUILD SUCCESS`；`fw-plus-ai-langchain4j-model` 测试 `Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`；reactor 中 `fw-plus-common` 测试 `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`。
- `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home ./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding -am test`：`BUILD SUCCESS`；`fw-plus-ai-langchain4j-store-embeding` 测试 `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`；reactor 中 `fw-plus-common` 测试 `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`。

### Step 3：AI Boot 定向测试

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=LangChain4jModelPropertiesTest,LangChain4jModelAutoConfigurationTest,LangChain4jModelProviderAdapterTest,MagicDynamicModelDataSourceTest,ModelDataSourceMagicDynamicRegistryTest,ModelModuleTest,MagicModelControllerTest,MagicAPIModelConfigurationTest,MagicAPIModelAutoConfigurationIntegrationTest,MagicDynamicVectorDataSourceTest,MagicAPIVectorConfigurationTest,MagicAPIVectorAutoConfigurationIntegrationTest,VectorModuleTest,MagicVectorControllerTest test
```

退出状态：`0`。Maven `BUILD SUCCESS`；Surefire 执行 8 个被发现的指定测试类，共 `Tests run: 27, Failures: 0, Errors: 0, Skipped: 0`。输出中有测试预期的 Model 数据源失败日志和既有多 SLF4J binding 警告，均未导致测试失败。

### Step 4：模型 Starter 构建

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-model-spring-boot-starter -am -DskipTests install
```

退出状态：`0`。10 个反应堆模块全部 `SUCCESS`，包括 `fw-plus-ai-langchain4j-model-spring-boot-starter`。

### Step 5：Magic Model 插件构建

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model -am -DskipTests install
```

退出状态：`0`。12 个反应堆模块全部 `SUCCESS`；插件控制台执行 Vite 构建并生成 `magic-model.1.0.0.iife.js`。

### Step 6：样例模块构建

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-samples/fw-plus-ai-magicapi-sample -am -DskipTests package
```

退出状态：`0`。16 个反应堆模块全部 `SUCCESS`，更新后的 `fw-plus-ai-magicapi-sample` 完成资源复制、编译与 JAR 打包。

### Step 7：Java 8、旧入口和敏感数据扫描

```bash
rg -n 'List\\.of\\(|Map\\.of\\(|Map\\.copyOf\\(|Set\\.of\\(|Stream\\.toList\\(|Optional\\.orElseThrow\\(' fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model
rg -n --glob '!README.md' --glob '!README.zh-CN.md' 'langchain4j-datasource|MagicLangchain4j|@MagicModule\\("langchain4j"\\)|fw-plus-ai-magicapi-plugin-langchain4j' fw-plus-ai
rg -n 'apiKey|secretKey|accessToken|password|token' fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model
```

结果：Java 8 API 扫描无匹配；排除迁移说明 README 后旧运行入口扫描无匹配，二者 `rg` 均以预期的状态 `1` 退出。敏感字段仅命中 `ModelDataSourceInfo` 的属性接收、映射与凭据键识别逻辑，未发现控制器列表或测试响应直接返回原始敏感值的证据。

## 最终状态与 Concerns

总体状态：`DONE`。

1. Core 的两项定向测试已在移除失效聚合模块引用后用 Zulu 8 + Maven Wrapper 重跑通过。
2. Maven 构建模型插件会在控制台目录生成未跟踪的 `node_modules/`、`dist/` 和 `package-lock.json`。它们是构建副产物，未纳入本任务提交。
3. 迁移文档必须提及旧入口，故 brief 的全目录旧入口 `rg` 会命中 README；排除 README 后的运行代码扫描无匹配。

## Reviewer 修复（Task 9）

- 恢复样例既有 `fw.plus.uid` 和 `fw.plus.web` 配置块；新增模型配置保留在独立的
  `fw-plus.ai.langchain4j.model` 块中，未重复任何顶层配置块。
- 三份 README 均明确说明：LangChain4j `0.35.0` 使用 `ChatLanguageModel` /
  `StreamingChatLanguageModel`，LangChain4j `1.x` 使用 `ChatModel` /
  `StreamingChatModel`。

### Focused checks

```bash
ruby -e 'require "yaml"; y = YAML.load_file(ARGV[0]); abort("missing fw.plus.uid") unless y.dig("fw", "plus", "uid"); abort("missing fw.plus.web") unless y.dig("fw", "plus", "web"); abort("missing fw-plus.ai.langchain4j.model") unless y.dig("fw-plus", "ai", "langchain4j", "model"); abort("missing magic-api.plugin.model.enabled") unless y.dig("magic-api", "plugin", "model", "enabled") == true; puts "application.yaml: focused structure passed"' fw-plus-ai/fw-plus-ai-samples/fw-plus-ai-magicapi-sample/src/main/resources/application.yaml
rg -n 'fw-plus|uid:|web:|langchain4j:|model:|magic-api:|plugin:|enabled:' fw-plus-ai/fw-plus-ai-samples/fw-plus-ai-magicapi-sample/src/main/resources/application.yaml
rg -n '1\.x|ChatModel|StreamingChatModel' fw-plus-ai/README.md fw-plus-ai/README.zh-CN.md fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/README.md
```

结果：通过。YAML 可解析，四项配置结构均存在，三份 README 均明确包含 LangChain4j
`1.x`、`ChatModel` 与 `StreamingChatModel`。
