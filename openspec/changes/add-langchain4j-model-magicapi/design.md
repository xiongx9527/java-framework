## Context

`fw-plus-ai-langchain4j-store-embeding` 已经在 core 侧提供了轻量的向量存储抽象，并通过 AI Boot starter 与 Magic-API vector 插件接入运行态动态资源。相比之下，现有 LangChain4j 模型相关类主要位于 `fw-plus-ai-boot-autoconfigure` 的 Magic-API 插件包中，包含模型包装、模型工厂、动态数据源和脚本模块等职责，导致普通 Spring Boot 应用无法在不依赖 Magic 插件实现细节的情况下复用通用模型契约。

本变更面向 Spring Boot 2.7.18、JDK 1.8、LangChain4j 0.35.0 和现有 Magic-API 2.2.2 自维护版本。实现时必须使用各顶层目录的 Maven Wrapper 验证，不升级 `org.ssssssss` 依赖，也不引入 Java 9+ API。

用户已确认旧 Magic LangChain4j 插件实现应删除或替换：它可以作为参考，但最终方案应使用更优雅、合理的一套 model 契约和插件入口。

## Goals / Non-Goals

**Goals:**

- 在 core 侧新增 `fw-plus-ai-langchain4j-model`，提供通用模型定义、模型类型、provider adapter、工厂和 registry 契约。
- 在 AI Boot 层新增 model starter，并通过自动配置把 Spring Boot 配置映射为 model registry。
- 在 Magic-API 中提供新的 `model-datasource` 动态资源与 model 脚本模块，使脚本能按 key 获取 Chat、Streaming Chat、Embedding、Image、Language、Moderation、Scoring 等模型。
- 参考但删除或替换现有 Magic-API LangChain4j 插件实现，避免继续维护两套模型定义、工厂、动态数据源和脚本入口。
- 保持 provider SDK 按需引入，starter 不默认传递所有模型供应商依赖。

**Non-Goals:**

- 不升级 LangChain4j、Spring Boot、Spring AI 或 Magic-API 版本。
- 不新增具体业务对话、RAG、Agent 工作流或 UI 大改版。
- 不改变已有 `fw-plus-ai-langchain4j-store-embeding` 的向量存储行为。
- 不默认内置所有第三方 provider SDK，也不要求应用必须使用 Magic-API 才能使用模型 registry。
- 不保留旧 Magic `langchain4j-datasource` 资源类型和旧 `langchain4j` 脚本入口兼容。

## Decisions

### 1. Core 模块承载通用模型契约

在 `fw-plus-ai-langchain4j` 聚合下新增 `fw-plus-ai-langchain4j-model`，包名建议为 `com.xiongx9527.fw.plus.ai.langchain4j.model`。模块内容仿照 store 模块组织：

- `LangChain4jModelDefinition`：保存 `key`、`provider`、`modelType`、`enabled`、`baseUrl`、`apiKey`、`modelName`、`properties` 等通用配置。
- `LangChain4jModelType`：定义 `CHAT`、`STREAMING_CHAT`、`EMBEDDING`、`IMAGE`、`LANGUAGE`、`STREAMING_LANGUAGE`、`MODERATION`、`SCORING`。
- `LangChain4jModel`：包装原生 LangChain4j model，暴露 `getModelType()`、`getProvider()`、`getModel()`。
- `LangChain4jModelProviderAdapter`：以 provider + modelType 判断是否支持，并创建原生模型或包装模型。
- `LangChain4jModelFactory` / `DefaultLangChain4jModelFactory`：选择 adapter、统一异常语义、禁止返回 null。
- `LangChain4jModelRegistry` / `DefaultLangChain4jModelRegistry`：按 key 获取模型、默认模型、key 集合和包含判断。
- `LangChain4jModelException`：封装 provider 不支持、配置缺失、创建失败等运行期错误。

备选方案是在 AI Boot 自动配置层继续维护模型工厂。该方案改动小，但会让 Magic 插件实现继续成为通用模型能力的事实源，不利于 core 侧复用，因此不采用。

### 2. Provider adapter 放在 Boot 自动配置层，core 只定义 SPI

core 模块只依赖轻量 LangChain4j core 类型，不直接绑定 OpenAI、Qwen、Ollama、DashScope 等 provider SDK。AI Boot 自动配置层提供 provider adapter beans，并通过 `@ConditionalOnClass` 或创建时 class 检查避免缺少 provider jar 时启动失败。

备选方案是让 `fw-plus-ai-langchain4j-model` 内置全部 provider builder。该方案使用方便，但会扩大 core 依赖面，也与 store starter 当前“业务显式引入 provider”的模式不一致，因此不采用。

### 3. Spring Boot starter 只带抽象和自动配置入口

新增 `fw-plus-ai-langchain4j-model-spring-boot-starter`，依赖 `fw-plus-ai-boot-starter` 与 `fw-plus-ai-langchain4j-model`。它负责让业务应用获得自动配置能力，但 provider 具体依赖由业务引入已有 `fw-plus-ai-langchain4j-*` 模块或 LangChain4j provider artifact。

配置结构建议沿用 store 配置风格，例如：

```yaml
fw-plus:
  ai:
    langchain4j:
      model:
        default-model: chat-default
        models:
          chat-default:
            provider: openai
            model-type: chat
            base-url: http://localhost:11434/v1
            api-key: demo
            model-name: gpt-4o-mini
            properties: {}
```

### 4. Magic-API 插件使用新的 model 契约并删除旧实现

Magic 插件侧新增模型动态资源能力，资源类型明确为 `model-datasource`。动态资源创建时转换为 core 的 `LangChain4jModelDefinition`，通过 `LangChain4jModelFactory` 创建模型，并与 Spring Boot registry 一起对外提供查询。

脚本模块建议提供 `model['key']` 访问方式，并根据模型类型暴露原生 LangChain4j model 或受控包装对象。现有 `com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j` 下的重复实现仅作为参考，最终删除或替换为新的 `magicapi.plugin.model` 实现；不维护旧 `langchain4j` façade。

备选方案是保留旧 `langchain4j` 插件作为 façade 并委托新 registry。该方案迁移成本低，但会长期保留两套入口、两套资源命名和两套前端心智模型，不符合本次“通用模型模块 + 优雅统一插件”的目标，因此不采用。

### 5. OpenSpec 单 change 交付

本需求虽然跨 core、AI Boot starter、Magic-API 插件三处代码，但三者共同完成一个“通用 LangChain4j 模型模块可被 Spring 与 Magic 复用”的能力。拆成多个 change 会产生不可单独验收的中间状态，因此保持为一个 OpenSpec change。

## Risks / Trade-offs

- [Risk] 删除旧 Magic-API LangChain4j 插件会破坏旧脚本路径和 `langchain4j-datasource` 资源。→ Mitigation: 在 proposal/spec/tasks 中明确为破坏性替换，更新样例、starter 依赖、前端插件和文档，提供迁移说明。
- [Risk] provider SDK 缺失时 adapter bean 或模型创建可能失败。→ Mitigation: adapter 通过条件装配或运行期类检测隔离，错误信息包含 provider、modelType 和 key。
- [Risk] 将模型类型抽象过宽可能导致 API 难以稳定。→ Mitigation: 第一版覆盖 LangChain4j 0.35.0 core 中实际存在的模型接口：Chat、Streaming Chat、Embedding、Image、Language、Streaming Language、Moderation、Scoring；Audio、Video、PDF 等作为多模态 Chat 内容载体说明和使用，不抽象为不存在的独立模型接口。
- [Risk] starter 不默认传递 provider SDK 会让首次使用多一步依赖配置。→ Mitigation: README 或示例说明最小依赖组合，并与 store starter 的既有边界保持一致。
- [Risk] Magic 动态模型创建可能泄露 apiKey 等敏感字段到日志或前端响应。→ Mitigation: 控制器返回前过滤敏感字段，日志只打印 key、provider 和 modelType。

## Migration Plan

1. 新增 core 模型模块并注册到 `fw-plus-ai-langchain4j` 聚合和 core BOM。
2. 在 AI Boot 自动配置层新增 model properties、factory、registry、provider adapter beans 与测试。
3. 新增 model Spring Boot starter，并注册到 AI Boot starters 聚合和 BOM。
4. 新增 Magic model 插件动态资源存储、动态注册、控制器、脚本模块和必要前端插件资源注册，资源类型使用 `model-datasource`。
5. 删除或替换旧 Magic LangChain4j 插件 Java 实现、starter 依赖、自动配置导入、前端插件资源和示例依赖。
6. 分层运行 Maven Wrapper 验证：先 core 指定模块，再 AI Boot autoconfigure/starter 相关模块。

Rollback 策略：若新 Magic model 插件验证失败，可回退本 change 中对旧 Magic LangChain4j 插件的删除/替换改动；不在同一运行态同时保留新旧双入口。

## Version Notes

- 当前实现以 LangChain4j 0.35.0 为编译目标：Chat 接口使用 `ChatLanguageModel`，流式 Chat 接口使用 `StreamingChatLanguageModel`。
- 高版本 LangChain4j 已将上述接口演进为 `ChatModel` 与 `StreamingChatModel`；本 change 的 README/迁移文档必须说明该版本差异。
- LangChain4j 0.35.0 与高版本均有 `ImageContent`、`AudioContent`、`VideoContent`、`PdfFileContent` 等多模态内容类型；它们通过 Chat 消息输入使用，不代表存在独立 `AudioModel` 或 `VideoModel`。

## Open Questions

- 模型配置字段是否需要统一使用小写 provider/modelType，还是兼容大小写输入并在内部规范化，需要根据现有配置绑定风格确定。
