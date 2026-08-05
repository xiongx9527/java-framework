# Tinyflow Starter 与 Magic-API 插件设计

## 背景

fw-plus 需要在 AI 模块中集成 Tinyflow，并通过 Magic-API 提供工作流资源管理和可视化设计入口。工作流定义由 Magic-API `resource` 保存，编辑器由 `toolbars` 打开，流程执行使用项目已有的 LangChain4j 模型注册体系。

当前 `dev` 分支使用 Java 8、Spring Boot 2.7.18、Magic-API 2.2.2 和 LangChain4j 0.35.0。Tinyflow 最新正式版 `tinyflow-core:2.0.7` 的 POM 使用 Java 8，抽检字节码为 major version 52，可以直接进入 Core；官方 `tinyflow-support-langchain4j:2.0.7` 使用 LangChain4j 1.7.1 和 Java 17，不能进入当前分支。

Tinyflow UI 使用官方 npm 包 `@tinyflow-ai/ui:1.3.6`。Starter 物理包含 UI 前端工程和构建产物，Magic-API Tinyflow 插件单向依赖 Starter。本次只修改并验证 `dev` 分支，不修改 `springboot2` 或 `springboot3` 工作树。

## 目标

- 在 Core AI 聚合层新增独立 Tinyflow 模块。
- 提供适配 LangChain4j 0.35.0 的同名 `Langchain4jLlm` 覆盖类。
- 新增完整的 Tinyflow Spring Boot Starter，提供执行引擎、自动配置、扩展 SPI 和 UI 静态资源。
- 新增 Magic-API Tinyflow 插件，通过 `resource` 保存原生工作流 JSON，通过 `toolbars` 打开全屏设计器。
- 使用现有 `LangChain4jModelRegistry` 解析 Tinyflow LLM 节点的 `llmId`。
- 支持 Tinyflow 节点的后端解析器扩展和前端节点定义扩展。
- 保持 Java 8、Spring Boot 2.7.18 和 Magic-API 2.2.2 兼容。

## 非目标

- 本次不修改 `springboot2` 或 `springboot3` 分支。
- 不引入或改造官方 `tinyflow-support-langchain4j` 制品。
- 不升级当前 LangChain4j、Spring Boot 或 Magic-API 版本。
- 不新增数据库表或独立 Tinyflow 文件存储服务。
- 不复制 Tinyflow UI 上游源码；UI 从固定 npm 版本构建。
- 不保证所有模型提供商都支持 Tinyflow 节点上的逐请求采样参数。
- 不在首版提供持久化流程运行状态仓库，默认运行状态保存在内存中。

## 模块结构

### Core Tinyflow 模块

新增：

```text
fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/
```

该模块进入 Core AI 聚合 POM 和 Core BOM，职责包括：

- 依赖 `dev.tinyflow:tinyflow-core:2.0.7`。
- 依赖现有 LangChain4j adapter/model 契约和 Java 8 兼容的 LangChain4j 0.35.0 核心制品。
- 提供 Tinyflow 执行门面、定义注册契约、节点 Provider SPI 和稳定异常类型。
- 提供 `dev.tinyflow.langchain4j.provider.Langchain4jLlm` 同名兼容类。
- 不依赖 Spring、Magic-API、Servlet 或前端构建工具。

### Tinyflow Starter

新增：

```text
fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/
└── fw-plus-ai-tinyflow-spring-boot-starter/
```

Starter 是应用侧 Tinyflow 的完整入口，职责包括：

- 引入 Core Tinyflow 模块和 AI Boot 自动配置。
- 引入项目现有 LangChain4j adapter starter，使 `LangChain4jModelRegistry` 成为默认模型来源。
- 提供 `ChainParser`、`ChainExecutor`、默认定义仓库、默认状态仓库和 `TinyflowService` Bean。
- 收集业务提供的 Tinyflow 节点 Provider 和可覆盖仓库 Bean。
- 注册并在应用关闭时注销 Tinyflow `LlmManager` 中的 LangChain4j Provider。
- 在 Starter 自身模块中构建并打包 Tinyflow UI。
- 不依赖 Magic-API，避免与插件形成循环依赖。

### Magic-API Tinyflow 插件

新增：

```text
fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/
└── fw-plus-ai-magicapi-plugin/
    └── fw-plus-ai-magicapi-plugin-tinyflow/
```

插件进入 AI Magic-API 插件聚合 POM 和 AI BOM。依赖方向固定为：

```text
fw-plus-ai-magicapi-plugin-tinyflow
    -> fw-plus-ai-tinyflow-spring-boot-starter
        -> fw-plus-ai-tinyflow
```

插件职责包括：

- 注册 Magic-API `tinyflow` 资源类型。
- 注册 MagicScript `tinyflow` 模块。
- 注册 `toolbars` 全屏设计器入口。
- 提供资源读取、保存、校验、测试运行和模型列表接口。
- 加载 Starter JAR 中的 Tinyflow UI JS/CSS，不复制 UI 包。

## Starter 自动配置

配置前缀使用 `fw.plus.ai.tinyflow`，首版定义：

- `enabled`：总开关，默认 `true`。
- `execution-timeout`：同步执行默认超时，默认 `5m`。

自动配置使用 Spring Boot 2.7 的 `spring.factories` 注册方式，并按现有 AI Boot 自动配置布局实现。主要 Bean 为：

- `ChainParser`：包含 Tinyflow 默认节点解析器和业务扩展解析器。
- `ChainDefinitionRepository`：默认使用线程安全内存实现，允许业务 Bean 覆盖。
- `ChainStateRepository`：默认使用 Tinyflow 内存实现，允许覆盖。
- `NodeStateRepository`：默认使用 Tinyflow 内存实现，允许覆盖。
- `ChainExecutor`：由上述仓库构建。
- `TinyflowService`：提供定义校验、注册、删除、同步执行、异步执行和恢复执行。
- `LangChain4jTinyflowLlmProvider`：按 `llmId` 从 `LangChain4jModelRegistry` 解析聊天模型。

自动配置必须使用 `@ConditionalOnClass`、`@ConditionalOnMissingBean` 和总开关保护。Starter 关闭时必须从静态 `LlmManager` 移除当前 Provider，避免测试上下文或应用重启后重复注册。

默认状态仓库只保证单进程生命周期内的异步执行和恢复。需要跨重启恢复时，业务应用通过自定义 Bean 替换流程状态和节点状态仓库；首版不伪装成持久化实现。

## LangChain4j 低版本适配

官方适配类的完全限定名为：

```text
dev.tinyflow.langchain4j.provider.Langchain4jLlm
```

Core Tinyflow 模块提供相同完全限定名的 Java 8 实现，但不引入官方适配器 JAR。依赖管理和依赖边界测试必须确保运行时只有一个该类，不能依赖 classpath 顺序决定加载结果。

适配关系为：

```text
LangChain4j 1.7.1 ChatModel          -> 0.35.0 ChatLanguageModel
chatModel.chat(messages)             -> chatLanguageModel.generate(messages)
ChatResponse                         -> Response<AiMessage>
response.aiMessage()                 -> response.content()
```

系统消息、文本内容、图片内容和用户消息继续使用 0.35.0 已提供的 `SystemMessage`、`TextContent`、`ImageContent` 和 `UserMessage` API。空响应、空 `AiMessage` 或空文本必须转换为稳定的 Tinyflow 执行异常。

`LangChain4jTinyflowLlmProvider` 按以下顺序解析模型：

1. `llmId` 非空时调用 `LangChain4jModelRegistry.get(llmId)`。
2. `llmId` 为空时使用 registry 默认模型。
3. 检查原生模型对象是否为 `ChatLanguageModel`。
4. 创建或获取对应的 `Langchain4jLlm` 适配实例。

找不到模型、默认模型不存在或模型类型错误时立即报错，错误包含 `llmId`，但不输出 API Key、请求头或模型凭证。

Tinyflow `ChatOptions` 包含 temperature、topP、topK、maxTokens、stop 和 seed。LangChain4j 0.35.0 的通用 `ChatLanguageModel` 接口不提供统一逐请求覆盖能力，官方 2.0.7 适配器也没有应用这些选项。首版保留节点配置但以已注册模型 Bean 的构建参数为准。需要不同采样行为时，通过多个模型 key 配置不同模型实例，再由节点选择不同 `llmId`。

## Tinyflow UI 构建与打包

Starter 内新增独立前端工程，例如：

```text
fw-plus-ai-tinyflow-spring-boot-starter/
└── src/main/frontend/tinyflow-ui/
    ├── package.json
    ├── package-lock.json
    ├── vite.config.*
    └── src/
```

前端工程使用精确依赖：

```json
{
  "dependencies": {
    "@tinyflow-ai/ui": "1.3.6"
  }
}
```

不使用 `^` 或 `latest`。Maven 构建通过现有 `exec-maven-plugin` 模式执行 `npm ci` 和 UI 构建。Tinyflow UI 使用独立构建链，不与 Magic-API 插件现有 Vite 2 构建链混用。

构建后的版本化资源写入 Starter JAR：

```text
META-INF/resources/fw-plus/tinyflow-ui/1.3.6/
├── tinyflow-ui.js
└── tinyflow-ui.css
```

仓库提交前端源文件、`package.json` 和 lock 文件，不提交 `node_modules`。是否提交生成的 `dist` 服从现有插件构建约定；JAR 验收必须以最终 classpath 资源为准。

Starter 保留 `@tinyflow-ai/ui` 的 LGPL-3.0-or-later 许可证声明和版本信息。若依赖内部出现阻断性 Bug，优先升级官方版本或在包装层规避；必须修改依赖内部实现时，使用有来源、可审计的 npm patch，并补充对应回归测试，不直接复制整个上游仓库。

Magic-API 插件的前端包只实现资源服务、Toolbar 宿主组件和 Starter UI 资源加载器。资源 URL 根据当前 Magic-API 控制台的 context path 生成，不硬编码 `/`、开发机地址或外部 CDN。

## Magic-API 资源模型

注册资源类型 `tinyflow`。每个资源正文直接保存 `@tinyflow-ai/ui` 导出的原生 Tinyflow JSON，不添加项目自定义外层对象。Magic-API 继续负责资源 ID、名称、分组路径、创建时间、修改时间和备份历史。

资源正文至少包含 Tinyflow 的 `nodes`、`edges` 和可选 `viewport`。读取时执行结构校验；保存前执行 JSON 解析和节点/连线校验。无效内容不得覆盖当前已保存版本。

资源引用优先使用 Magic-API 分组路径，MagicScript 示例为：

```javascript
tinyflow['customer-service/order-agent'].execute(variables)
tinyflow['customer-service/order-agent'].executeAsync(variables)
tinyflow.resume(instanceId, variables)
```

内部执行使用资源 ID 作为流程定义 ID，避免同名资源混淆。按路径解析失败、存在歧义或资源已删除时返回明确错误。资源保存或删除后立即刷新/移除内存定义，不等待应用重启。

## Toolbar 与编辑器数据流

Magic-API 插件通过 `toolbars` 注册 Tinyflow 图标入口。点击后在控制台内打开全屏 Vue 宿主组件，宿主动态加载 Starter 提供的 Tinyflow UI JS/CSS，并通过公开 `Tinyflow` 类挂载画布。

打开资源的数据流：

1. Toolbar 宿主取得当前 Tinyflow 资源 ID。
2. 插件接口通过 `MagicResourceService` 读取资源正文。
3. JSON 校验通过后调用 Tinyflow UI 的 `setData` 或初始化 `data`。
4. `provider.llm` 从后端读取允许选择的 LangChain4j 聊天模型 key 和显示名称。
5. `onDataChange` 更新宿主内存中的待保存 JSON，并标记未保存状态。
6. 保存时调用 Magic-API 资源保存接口，成功后清除未保存状态并刷新运行时定义。
7. 关闭或切换资源前若存在未保存修改，显示确认提示。
8. Vue 宿主卸载时调用 Tinyflow `destroy()`，移除事件监听器和画布实例。

Toolbar 提供保存、测试运行和关闭等必要操作。测试运行使用当前已保存资源；若存在未保存修改，先要求保存，避免 UI JSON 与后端执行定义不一致。

Starter UI 静态资源加载失败、版本不匹配或全局构造器缺失时显示可诊断错误，不展示空白画布。插件不得回退到外部 Tinyflow 网站。

## 动态节点扩展

Core 定义框架无关的 `TinyflowNodeProvider` SPI，至少提供节点 `type` 和对应 `NodeParser`。Starter 收集所有 Provider，构建应用级 `ChainParser`，不修改 Tinyflow 全局 `DefaultNodeParsers` 静态表。

业务 Provider 可以新增节点类型，也可以按相同 `type` 覆盖默认解析器。重复注册且无法确定优先级时启动失败，错误列出冲突的 Bean 和节点类型。

前端使用 `@tinyflow-ai/ui` 的 `customNodes` 扩展点：

- 标准输入、文本域、选择、滑块等表单节点可由可序列化描述生成。
- 需要 JavaScript 回调或自定义渲染的节点在 Starter UI 包装层中显式注册。
- 前端保存的节点 `type` 必须与后端 Provider 完全一致。

执行前扫描所有节点类型。后端缺少解析器时必须报“节点类型未安装”，不能沿用 Tinyflow `ChainParser` 对未知节点返回 `null` 的默认行为而静默丢弃节点。

## 执行流程

同步执行流程：

1. MagicScript 模块按资源路径解析 Magic-API 资源。
2. 校验资源正文并解析为 `ChainDefinition`。
3. 将 Magic-API 资源 ID 写入定义 ID并原子更新定义仓库。
4. `TinyflowService` 调用 `ChainExecutor.execute`，传入变量和有限超时。
5. LLM 节点通过 Tinyflow `LlmManager` 调用项目 LangChain4j Provider。
6. Provider 按 `llmId` 解析 `ChatLanguageModel` 并返回文本。
7. 执行结果清理 Tinyflow 内部状态字段后返回 MagicScript。

异步执行返回 Tinyflow 状态实例 ID。`resume` 使用实例 ID 恢复挂起流程。默认内存状态仓库不承诺应用重启后的恢复能力，接口和文档必须明确该边界。

## 错误处理与安全

- 空资源、非法 JSON、缺少节点或连线、无开始/结束路径时返回资源校验错误。
- 未知节点类型包含资源路径、节点 ID 和节点 type。
- 模型不存在或模型类型不支持时包含 `llmId` 和要求的能力类型。
- 同步执行超时取消等待并返回受控超时错误；长流程引导使用异步接口。
- 线程中断必须恢复中断标记，不吞掉取消信号。
- UI 解析失败和保存失败不得覆盖已保存资源或清除未保存状态。
- Magic-API 资源、模型列表和测试执行接口沿用现有编辑器认证与权限边界，不新增匿名管理接口。
- 错误响应和日志不得输出模型密钥、Authorization、完整请求头或资源中的敏感变量值。
- Tinyflow Code/HTTP 等节点具有脚本执行和外部访问能力，仅面向具有 Magic-API 设计权限的受信用户。

## 测试策略

### Core

- 使用 Java 8 编译新模块，并扫描新增源码不使用 Java 9+ API。
- 验证 `Langchain4jLlm` 的系统消息、文本、图片、空响应和异常转换。
- 验证模型 key、默认模型、错误模型类型和缺失模型行为。
- 验证未知节点预检查和自定义 `TinyflowNodeProvider` 覆盖行为。
- 验证依赖树不包含 `tinyflow-support-langchain4j`。
- 验证运行时只有一个同名 `Langchain4jLlm` 类来源。

### Starter

- 使用 `ApplicationContextRunner` 覆盖默认启用、显式关闭、自定义 Bean 覆盖和缺失依赖条件。
- 验证 Provider 注册、上下文关闭注销和重复上下文不残留。
- 使用无外部依赖的开始、模板和结束节点验证完整同步流程。
- 使用假的 `ChatLanguageModel` 验证 LLM 节点流程，不调用外部模型。
- 覆盖同步超时、异步状态 ID、挂起和恢复。
- 执行 `npm ci` 和前端生产构建，检查 JAR 中存在版本化 JS/CSS 和许可证信息。

### Magic-API 插件

- 覆盖 Tinyflow 资源创建、读取、重命名、保存、删除和运行时定义刷新。
- 验证资源正文保持原生 Tinyflow JSON，没有额外包装。
- 验证路径解析、重名歧义、资源不存在和非法 JSON。
- 验证 `tinyflow[path].execute`、`executeAsync` 和 `resume` MagicScript 接口。
- 验证插件前端元数据同时包含 `resource` 和 `toolbars`，且插件依赖 Starter。
- 验证模型列表只返回可用聊天模型的非敏感元数据。

### 前端与浏览器

- Node 测试覆盖 Starter UI 资源加载、初始化、`setData`、`getData`、`onDataChange` 和 `destroy`。
- 覆盖资源切换、未保存提示、保存失败、非法 JSON、模型列表失败和 UI 资源 404。
- 针对官方 Issue #67 增加节点新增、删除和画布稳定性回归场景。
- 启动 AI Magic-API sample，使用浏览器验证设计器加载、编辑、保存、刷新、重新打开和测试执行。
- 在桌面和移动视口检查全屏画布、Toolbar 和错误提示不重叠；Magic-API 主要工作流以桌面体验为验收重点。

## 构建与验收顺序

1. 构建并安装 Core AI Tinyflow 模块及受影响的 Core BOM。
2. 构建 AI Boot 自动配置和 Tinyflow Starter，验证 UI npm 构建及 JAR 内容。
3. 构建 Magic-API Tinyflow 插件和 AI 插件聚合器。
4. 构建并启动现有 AI Magic-API sample。
5. 运行 Java 单元/集成测试、Node 测试和浏览器端到端验证。
6. 检查 Maven 依赖树、重复类、Java 8 字节码和最终 Git diff。

## 完成标准

只有同时满足以下条件才算完成：`dev` 中三个新增模块边界清晰且进入对应聚合器/BOM；Starter JAR 物理包含由 `@tinyflow-ai/ui:1.3.6` 构建的版本化 UI；Magic-API 插件单向依赖 Starter并注册 `resource` 与 `toolbars`；Tinyflow 原生 JSON可以创建、编辑、保存、重新加载和执行；LangChain4j 0.35.0 同名覆盖类通过假的聊天模型完成真实 Tinyflow LLM 节点流程；未知节点、缺失模型、非法资源和超时均返回明确错误；依赖树不存在官方 LangChain4j 适配器和重复类；Core、AI、插件、sample、Node 与浏览器验证通过；`springboot2` 和 `springboot3` 工作树未被修改。
