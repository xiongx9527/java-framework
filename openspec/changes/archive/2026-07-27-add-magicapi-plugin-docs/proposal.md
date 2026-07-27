## Why

`fw-plus-ai-magicapi-plugin-model` 和 `fw-plus-ai-magicapi-plugin-vector` 已提供 Magic-API 模型与向量存储插件能力，但模块目录缺少 README，使用者需要回查源码才能确认依赖、启用条件、资源 JSON、脚本入口和测试接口。

本次热修补齐两个插件模块的说明文档，降低接入和排障成本，并把已实现的多模型名、默认模型、动态资源和版本边界说明沉淀到模块目录。

## What Changes

- 为 `fw-plus-ai-magicapi-plugin-model` 新增 README，说明模块定位、依赖、启用条件、Magic 模块名、`model-datasource` JSON、控制台接口、脚本用法和 LangChain4j 版本边界。
- 为 `fw-plus-ai-magicapi-plugin-vector` 新增 README，说明模块定位、依赖、启用条件、Magic 模块名、`vector-datasource` JSON、控制台接口、脚本用法和向量 store registry 关系。
- 文档补充基础配置、默认模型与多模型配置，并明确 `model-name` 支持多个值、每个模型名可通过 `model-configs.<modelName>.properties` 配置独立参数。
- 不修改 Java API、自动配置行为、Maven 依赖关系或前端运行逻辑。

## Capabilities

### New Capabilities

- `magicapi-ai-plugin-docs`: 约束 AI Magic-API model/vector 插件模块说明文档应覆盖的使用入口、配置示例和注意事项。

### Modified Capabilities

- `langchain4j-model-module`: 文档层补充，不改变已有需求。
- `magicapi-unified-vector-plugin`: 文档层补充，不改变已有需求。

## Impact

- 影响文件：两个插件模块 README 和当前 OpenSpec change 文档。
- 不影响运行时 API、配置绑定、自动配置注册、Magic-API 资源加载、前端构建产物或依赖版本。
- 验证重点：文档文件存在、内容覆盖源码事实、Markdown 基础格式正确，相关前端 console 测试保持通过。
