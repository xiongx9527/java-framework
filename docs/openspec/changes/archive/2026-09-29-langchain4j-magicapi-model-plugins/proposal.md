# Proposal

## Why

fw-plus-ai 已把 LangChain4j 以 magic-api `@MagicModule` 形式接入，但模型实例只能在脚本内用 `build*` 编程式自建并缓存，缺少可维护、可视化、可热更新的配置通道；langchain4j 生态的 20+ 模型供应商与 15+ 向量库在 magic-api 侧也没有统一接入面。同时 magic-api 的 `api` / `function` 资源已有元数据（与 MCP 插件共用），却无法作为工具交给大模型本地调用。

本 change 把七类 LangChain4j 模型资源做成 magic-api datasource 插件，补齐脚本只读取用通道，并把 magic-api 资源桥接为 LangChain4j 本地工具，使模型配置从「脚本硬编码」变为「控制台可视化 + 热更新」。

## What Changes

- **新增七类 datasource 型 magic-api 插件**：`langchain4j-chat-model`、`langchain4j-streaming-chat-model`、`langchain4j-language-model`、`langchain4j-streaming-language-model`、`langchain4j-embedding-model`、`langchain4j-tokenizer`、`langchain4j-embedding-store`。每类在控制台「数据源」页签注册，JSON 持久化于 `{type}/*.json`。
- **每类资源补齐标准三件套**：运行时实例持有者 `MagicDynamic*DataSource`（`DisposableBean`，按 key 持有/驱逐/统一关闭）、动态注册器 `*MagicDynamicRegistry`（遵循资源托管规则 A：先驱逐 → `enabled` 检查 → 工厂构建 → 成功才落库）、资源存储 `*MagicResourceStorage`（`key` 唯一与格式、`provider` 短名表、`embedding-store` 必填 `embeddingModelKey`）。
- **装配按资源类型拆为 7 个子 Configuration**：父类 `Langchain4jDatasourceConfiguration` 只保留 `plugin()` / `controllerRegister()` 与共享 bean，用 `@Import` 引入 7 个按类型的子配置类（对齐 `MagicAPIDruidConfiguration` 的 `@Import` 写法）。
- **JSON 结构 = 公共字段 + `options` 差异参数对象**：`key` / `name` / `provider` / `enabled`（`embedding-store` 另加 `embeddingModelKey`）提取到实体基类，其余连接与生成参数放入 `options` 自由 map；不同 provider 的 `options` 天然不一致。
- **provider 覆盖**：每家一个显式工厂（参照 core 层 `AutoConfig` 的 builder 链写法），每类资源有独立短名表，表外值校验失败并列出可用短名。
- **改造 `Langchain4jModule`（**BREAKING**）**：`@MagicModule("uid")` → `@MagicModule("langchain4j")`；删除全部 `build*` 编程式工厂与内部实例 map；`get*` 改为只读委托运行时实例持有者；`remove(type,key)` 改为驱逐语义；`destroy()` 清空全部持有者。旧脚本中的 `langchain4j.buildXxx(...)` 将失效。
- **新增连接测试 Controller**：`POST /langchain4j/{type}/datasource/test`，对未保存的配置即时构建并按类型做强弱不同的最小探测。
- **新增 console 前端子工程** `fw-plus-ai-magicapi-plugin-langchain4j`：7 个数据源组件 + `(资源类型 × provider)` 字段元数据表 + 连接测试 service + 中英文 i18n，构建产物 `magic-langchain4j.1.0.0.iife.js`。
- **新增 Magic-API → LangChain4j 本地工具桥**：把 magic-api 的 `api` / `function` 资源暴露为 LangChain4j `ToolProvider`，过滤范围（分组/路径/HTTP 方法/opt-in）通过 `fw.plus.magicapi.langchain4j.tool.*` 配置。
- **依赖**：AI 层 BOM 统一管理 langchain4j 全生态供应商版本，`fw-plus-ai-boot-autoconfigure` 以 `optional` 引入。
- **硬约束**：core 层 `fw-plus-ai-langchain4j-*` 零修改（含修饰符），只作为 builder 参数写法的参考资料。

## Capabilities

### New Capabilities

- `langchain4j-model-datasource-plugins`: 七类 LangChain4j 模型资源作为 magic-api datasource 的注册、JSON 结构、校验、动态注册与生命周期、连接测试、控制台装配与前端资源。
- `langchain4j-script-module`: 脚本侧 `langchain4j` 模块对七类资源的只读取用语义、未配置时的错误行为与驱逐语义。
- `magicapi-langchain4j-tool-provider`: magic-api `api` / `function` 资源暴露为 LangChain4j 本地 `ToolProvider` 的曝光范围、过滤规则、入参 schema 与变更监听。

### Modified Capabilities

（无。本 change 不改变既有 `magicapi-mcp-plugin` / `mcp-server-starter` 的规格行为；工具桥只复用 MCP 插件既有的 `mcpInputSchema` / `mcpTool` 元数据约定。）

## Impact

- **代码**：`fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure`（插件装配、实体、存储、注册器、工厂、Controller、脚本模块、工具桥）；新增前端子工程 `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-langchain4j`；样例工程 `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-chat-sample` 仅作为「静态配置路径不回归」验收对象。
- **依赖**：`fw-plus-ai-boot-dependencies` 新增 langchain4j 供应商版本管理；`fw-plus-ai-boot-autoconfigure/pom.xml` 以 `optional` 引入模型/向量库 jar。
- **配置**：新增 `fw.plus.magicapi.langchain4j.enabled` 与 `fw.plus.magicapi.langchain4j.tool.*`。
- **破坏性**：脚本侧 `langchain4j.build*` 系列方法移除；`@MagicModule` 名由 `uid` 改为 `langchain4j`。
- **交付物**：前端 `magic-langchain4j.1.0.0.iife.js` 需落在 `target/classes/magic-editor/plugins/`。
- **非目标**：不做 `AiServices` / `Assistant` / `ContentRetriever` / `RetrievalAugmentor` / `ModerationModel` 的 datasource 化；不做敏感参数加密存储；不做集群同步与参数 schema 强校验引擎；不修改 core 层 `fw-plus-ai-langchain4j-*`。
