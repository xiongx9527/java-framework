---
comet_change: add-langchain4j-model-magicapi
role: technical-design
canonical_spec: openspec
---

# LangChain4j Model Module 与 Magic Model 插件技术设计

## 背景

当前模型动态能力集中在 AI Boot 自动配置层的 Magic-API `langchain4j` 插件包中。该包同时承担模型定义、provider 创建、动态数据源、Magic 资源存储、脚本模块和 controller 职责，导致普通 Spring Boot 应用无法复用模型 registry，也让 Magic 插件成为通用模型能力的事实源。

本设计将模型能力拆成三层：core 定义通用契约，AI Boot 负责 Spring 配置与 provider adapter，Magic model 插件只负责动态资源和脚本访问。旧 Magic LangChain4j 实现仅作为迁移参考，最终删除或替换，不保留旧 `langchain4j-datasource` 与旧 `langchain4j` 脚本入口兼容。

## 设计目标

- 新增 `fw-plus-ai-langchain4j-model`，提供稳定、轻量、可复用的模型抽象。
- 新增 `fw-plus-ai-langchain4j-model-spring-boot-starter`，让业务应用通过配置获得模型 registry。
- 新增 Magic model 插件，资源类型为 `model-datasource`，脚本入口为 model 模块。
- 删除或替换旧 Magic LangChain4j 插件重复实现，避免双入口和双资源契约。
- 保持 JDK 1.8、Spring Boot 2.7.18、LangChain4j 0.35.0 和 Magic-API 2.2.2 约束。

## 模块与边界

### Core: `fw-plus-ai-langchain4j-model`

包名：`com.xiongx9527.fw.plus.ai.langchain4j.model`

核心类型：
- `LangChain4jModelDefinition`：通用模型配置，字段包含 `key`、`provider`、`modelType`、`enabled`、`baseUrl`、`apiKey`、`modelName`、`properties`。
- `LangChain4jModelType`：枚举 `CHAT`、`STREAMING_CHAT`、`EMBEDDING`、`IMAGE`、`LANGUAGE`、`STREAMING_LANGUAGE`、`MODERATION`、`SCORING`。
- `LangChain4jModel`：包装 provider、modelType 和原生 LangChain4j model。
- `LangChain4jModelProviderAdapter`：SPI，按 provider/modelType 判断支持并创建模型。
- `LangChain4jModelFactory` 与 `DefaultLangChain4jModelFactory`：统一 adapter 选择和错误语义。
- `LangChain4jModelRegistry` 与 `DefaultLangChain4jModelRegistry`：按 key 管理模型和默认模型。
- `LangChain4jModelException`：包装配置错误、provider 不支持和创建失败。

core 模块只依赖 LangChain4j 基础模型接口，不引入具体 provider SDK。

### AI Boot 自动配置

新增配置前缀建议为 `fw-plus.ai.langchain4j.model`，结构与 embedding-store 保持相似：

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
            properties: {}
```

自动配置职责：
- 注册 `LangChain4jModelFactory`。
- 注册 provider adapter beans。
- 读取启用的 model definitions 并创建 `LangChain4jModelRegistry`。
- 校验默认模型 key。
- 通过条件装配或运行期类检测隔离缺失 provider SDK。

provider adapter 放在 AI Boot 自动配置层，便于使用已有 `fw-plus-ai-langchain4j-*` provider 模块，也避免 core 模块传递过多第三方依赖。

### Starter

新增 `fw-plus-ai-langchain4j-model-spring-boot-starter`：
- 依赖 `fw-plus-ai-boot-starter`。
- 依赖 `fw-plus-ai-langchain4j-model`。
- 不默认传递 OpenAI、Qwen、Ollama、DashScope、Azure、Anthropic 等具体 provider SDK。

### Magic Model 插件

新增插件建议包：`com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model`

核心组成：
- `ModelDataSourceInfo`：继承 Magic 资源实体，字段映射到 `LangChain4jModelDefinition`。
- `ModelDataSourceInfoMagicResourceStorage`：folder 返回 `model-datasource`。
- `ModelDataSourceMagicDynamicRegistry`：监听 `model-datasource` 文件和组事件，注册/注销动态模型。
- `MagicDynamicModelDataSource`：合并 Spring Boot registry 与 Magic runtime models。
- `ModelModule`：`@MagicModule("model")`，支持 `model['key']` 按 key 获取模型。
- `MagicModelController`：提供测试和列表接口，测试接口必须真实调用 factory 创建模型。
- `MagicAPIModelConfiguration`：注册插件、controller、storage、registry、module。

key 冲突规则建议：Magic runtime 模型优先于 Spring Boot registry；同一个 runtime key 更新时先销毁旧模型再创建新模型。该规则需要通过测试固定。

## 旧实现处理

旧包 `com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j` 中的实现可以作为参考，但最终不作为运行入口保留。实现阶段需要处理：
- 删除或替换旧 `LangChain4jModel`、`LangChain4jModelFactory`、`MagicDynamicLangchain4jDataSource`、`Langchain4jModule`、resource storage、dynamic registry、controller 和 configuration。
- 更新 `MagicApiPluginAutoConfiguration`，导入新的 model 插件配置。
- 更新 Magic 插件聚合和 AI Boot BOM，迁移旧 `fw-plus-ai-magicapi-plugin-langchain4j` artifact 的引用。
- 更新 `fw-plus-ai-magicapi-sample` 等样例依赖。
- 替换前端插件资源中的资源类型和插件名称，使控制台使用 `model-datasource`。

旧 artifact 的处理有两个实现选择：
- 推荐：删除旧 `fw-plus-ai-magicapi-plugin-langchain4j` 模块，并新增 `fw-plus-ai-magicapi-plugin-model`。
- 备选：旧 artifact 改为空壳迁移模块，只依赖新 model 插件并标记废弃。若需要保持 Maven 坐标短期可解析，可采用该方式，但运行态仍只能有新 model 入口。

## 错误处理与安全

- provider 不支持、modelType 不支持、adapter 创建失败时，异常信息包含 key、provider 和 modelType。
- controller 响应和日志不得明文输出 `apiKey`、`secretKey`、`accessToken` 等敏感字段。
- `ModelDataSourceInfo.simple()` 和列表接口返回前应清理敏感字段。
- 解析 `properties` 时保留 JSON/Map 结构错误信息，但不打印敏感值。
- 所有新增代码避免 Java 9+ API。

## 版本兼容说明

- 当前实现以 LangChain4j 0.35.0 为编译目标：Chat 接口使用 `ChatLanguageModel`，流式 Chat 接口使用 `StreamingChatLanguageModel`。
- 高版本 LangChain4j 已将上述接口演进为 `ChatModel` 与 `StreamingChatModel`；README 和迁移说明必须明确该差异，避免使用高版本类名编写 0.35 代码。
- LangChain4j 0.35.0 与高版本均有 `ImageContent`、`AudioContent`、`VideoContent`、`PdfFileContent` 等多模态内容类型；它们通过 Chat 消息输入使用，不代表存在独立 `AudioModel` 或 `VideoModel`。
- 当前完整模型类型覆盖 0.35.0 core 中真实存在的接口：Chat、Streaming Chat、Embedding、Image、Language、Streaming Language、Moderation、Scoring。

## 测试策略

core 测试：
- `DefaultLangChain4jModelFactoryTest` 覆盖匹配 adapter、不支持 provider、adapter 抛异常、adapter 返回 null。
- `DefaultLangChain4jModelRegistryTest` 覆盖 get、getDefault、contains、keys、默认 key 无效。

Boot 测试：
- 配置绑定测试覆盖 enabled、default-model、models map 和 properties。
- 自动配置测试覆盖 registry 创建、默认 key 校验、用户自定义 factory/registry 覆盖。
- provider adapter 测试覆盖缺少 provider SDK 时不会导致无关应用启动失败。

Magic 测试：
- dynamic registry 注册/注销 `model-datasource`。
- `MagicDynamicModelDataSource` 合并 Spring Boot registry 与 runtime models，并固定 runtime 优先规则。
- `ModelModule` 支持 `model['key']` 获取模型。
- `MagicModelController` 测试接口真实创建模型，失败响应不泄露敏感字段。
- 迁移测试或结构检查确保旧 Magic LangChain4j 插件不再作为运行入口。

回归验证：
- `fw-plus-ai-langchain4j-store-embeding` 相关测试继续通过。
- Magic vector 插件测试继续通过。
- 分层 Maven Wrapper 构建 core 和 AI Boot 受影响模块。

## 实施顺序

1. 新建 core model 模块和测试。
2. 注册 core 聚合和 core BOM。
3. 新增 AI Boot properties、adapter、factory、registry 自动配置和测试。
4. 新增 model starter 并注册 AI Boot 聚合/BOM。
5. 新增 Magic model 插件后端实现和测试。
6. 替换前端插件资源、样例依赖和自动配置导入。
7. 删除或空壳化旧 Magic LangChain4j 插件 artifact。
8. 补文档与运行 Maven Wrapper 验证。

## 验收

实现完成后应满足 OpenSpec `langchain4j-model-module` delta spec 的全部场景，尤其是：core 模型模块可复用、starter 不传递所有 provider SDK、Magic 使用 `model-datasource`、旧 Magic LangChain4j 实现被删除或替换、向量存储行为不变。
