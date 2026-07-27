### Task 9: Documentation、Samples 配置与全量验证

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-samples/fw-plus-ai-magicapi-sample/src/main/resources/application.yaml`
- Modify: `fw-plus-ai/README.zh-CN.md`
- Modify: `fw-plus-ai/README.md`
- Modify if needed: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/README.md`

**Interfaces:**
- Consumes: all prior tasks.
- Produces:
  - Sample YAML contains `fw-plus.ai.langchain4j.model` configuration example and `magic-api.plugin.model.enabled=true`.
  - Docs explain model starter minimal dependency, provider SDK explicit dependency rule, `model-datasource` JSON example, and migration from old Magic `langchain4j` plugin.
  - Verification record can be pasted into OpenSpec verification later without changing OpenSpec in this task.

- [ ] **Task 9 Step 1: Add sample configuration**

Add this sample block to `application.yaml` using existing indentation style:

```yaml
fw-plus:
  ai:
    langchain4j:
      model:
        enabled: true
        default-model: chat-default
        models:
          chat-default:
            provider: openai
            model-type: chat
            base-url: http://localhost:11434/v1
            api-key: demo
            model-name: gpt-4o-mini
            enabled: true
            properties: {}

magic-api:
  plugin:
    model:
      enabled: true
```

If `fw-plus` or `magic-api.plugin` already exists, merge keys without duplicating top-level blocks.

- [ ] **Task 9 Step 2: Add documentation examples**

Document this `model-datasource` resource example:

```json
{
  "name": "本地 OpenAI 兼容模型",
  "key": "chat-default",
  "provider": "openai",
  "modelType": "CHAT",
  "enabled": true,
  "baseUrl": "http://localhost:11434/v1",
  "apiKey": "demo",
  "modelName": "gpt-4o-mini",
  "properties": {
    "temperature": 0.2
  }
}
```

Document this Magic script usage:

```javascript
import model;
return model['chat-default'].getModel();
```

Document LangChain4j version compatibility:
- 本模块以 LangChain4j 0.35.0 编译，Chat 类型对应 `ChatLanguageModel`，Streaming Chat 类型对应 `StreamingChatLanguageModel`。
- 高版本 LangChain4j 使用 `ChatModel` / `StreamingChatModel` 命名，当前版本不能引用这些类。
- 0.35.0 已有 `TextContent`、`ImageContent`、`AudioContent`、`VideoContent`、`PdfFileContent` 等多模态内容类型；高版本继续通过 Chat 请求承载多模态输入。
- 不存在独立 `AudioModel` / `VideoModel`，不要在 `modelType` 中新增这两个值；音频、视频、PDF 是 Chat 输入内容能力。

- [ ] **Task 9 Step 3: Run targeted core verification**

Run:

```bash
cd fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model -am test
./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding -am test
```

Expected: both commands PASS.

- [ ] **Task 9 Step 4: Run targeted AI Boot verification**

Run:

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -Dtest=LangChain4jModelPropertiesTest,LangChain4jModelAutoConfigurationTest,LangChain4jModelProviderAdapterTest,MagicDynamicModelDataSourceTest,ModelDataSourceMagicDynamicRegistryTest,ModelModuleTest,MagicModelControllerTest,MagicAPIModelConfigurationTest,MagicAPIModelAutoConfigurationIntegrationTest,MagicDynamicVectorDataSourceTest,MagicAPIVectorConfigurationTest,MagicAPIVectorAutoConfigurationIntegrationTest,VectorModuleTest,MagicVectorControllerTest test
```

Expected: PASS. Vector plugin tests passing confirms model work did not regress vector behavior.

- [ ] **Task 9 Step 5: Run module build verification**

Run:

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-model-spring-boot-starter -am -DskipTests install
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model -am -DskipTests install
./mvnw -pl fw-plus-ai-samples/fw-plus-ai-magicapi-sample -am -DskipTests package
```

Expected: all commands PASS.

- [ ] **Task 9 Step 6: Run source compatibility and migration scans**

Run:

```bash
rg -n "List\\.of\\(|Map\\.of\\(|Map\\.copyOf\\(|Set\\.of\\(|Stream\\.toList\\(|Optional\\.orElseThrow\\(" fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/model fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model
rg -n "langchain4j-datasource|MagicLangchain4j|@MagicModule\\(\"langchain4j\"\\)|fw-plus-ai-magicapi-plugin-langchain4j" fw-plus-ai
rg -n "apiKey|secretKey|accessToken|password|token" fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model
```

Expected:
- First `rg` returns no matches.
- Second `rg` returns no matches.
- Third `rg` only shows masking, filtering, property mapping, or test-safe handling; no controller list/test response returns raw sensitive values.

- [ ] **Task 9 Step 7: Capture verification notes**

Record command outputs in the implementation handoff or PR body. If a dependency cannot resolve from internal Nexus, capture the exact Maven coordinate and repository URL from the failing output, and do not mark the behavior as failed until dependency resolution is addressed.

- [ ] **Task 9 Step 8: Commit this task during execution**

```bash
git add fw-plus-ai/fw-plus-ai-samples/fw-plus-ai-magicapi-sample/src/main/resources/application.yaml \
  fw-plus-ai/README.zh-CN.md \
  fw-plus-ai/README.md \
  fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/README.md
git commit -m "docs: document langchain4j model magicapi plugin"
```

## Coverage Self-Review

- Core reusable model module is covered by Task 1 and Task 2.
- Maven aggregate and BOM registration for core and AI Boot are covered by Task 1 and Task 5.
- Spring Boot properties, factory, provider adapters, registry, and SDK isolation are covered by Task 3 and Task 4.
- Magic `model-datasource`, runtime model priority, script module, controller, sensitive masking, and plugin registration are covered by Task 6 and Task 7.
- Old Magic `langchain4j` implementation deletion and artifact migration are covered by Task 8.
- Sample/docs updates and regression verification are covered by Task 9.
- JDK 1.8, Maven Wrapper usage, no provider SDK in core, and no sensitive output are enforced in Global Constraints and verification scans.
