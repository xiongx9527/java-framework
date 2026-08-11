# TinyEngine 官方 API 全量兼容与现有规范并存设计

## 1. 背景

`java-framework` 已经具备 TinyEngine Core、Boot Starter、打包前端资源以及中立 AI Provider/RAG 基础，但当前运行态只覆盖原前端的一部分接口。对当前源码进行只读比对后，字符串级扫描得到 86 个唯一候选路径，其中包含动态模板展开产生的重复项；现有实现仍有数十个候选路径缺口。已有 AI Web 接口的默认路径、请求 DTO、响应结构和流式协议也不能直接满足官方前端。

本设计以以下本地源码作为事实基线：

- 官方前端：`/Users/xiongxiao/Workspace/Idea/codes/TinyEngine`
- 官方 Java 后端：`/Users/xiongxiao/Workspace/Idea/codes/tiny-engine-backend-java`，版本线为 `v2.10.0`
- 目标工程：`/Users/xiongxiao/Workspace/Idea/codes/java-framework`

现有工作区包含用户未提交改动。实施必须保留这些改动，不得以同步官方源码为由覆盖、回退或重置工作区。

## 2. 目标

1. 审核字符串级扫描得到的 86 个候选路径，固化去重后的有效 API 契约清单并全部补齐，使设计器、应用中心、页面管理、保存、发布、预览、国际化、资源、模型和 AI 工作流可用。
2. 官方路径、HTTP 方法、参数名、请求体、状态码和响应外形以官方 Java 后端为行为基准。
3. 同时保留 `java-framework` 当前已公开的 `/tiny-engine/ai/**`、`TinyEngineAiProviderRegistry`、中立模型/RAG 接口和 `fw.plus.tiny-engine.ai.*` 配置语义。
4. 官方 AI 接口支持用户在 Robot 中配置 OpenAI 兼容服务，覆盖非流式、SSE、工具调用、厂商扩展字段、Key 加密和 RAG。
5. 全部新增 Java 代码兼容 Java 8，MVC 与 WebFlux 能力对等。

## 3. 非目标

- 不迁移官方前端完全不会调用的后台管理接口。
- 不把官方 Java 后端作为第二个独立进程运行，也不通过反向代理依赖它。
- 不在 Starter、样例配置或构建产物中新增 `gpt-5.5`、AI 服务地址或 API Key。
- 不删除官方前端已有的百炼、DeepSeek 空 Key 预设，也不重做 Robot UI。
- 不将 LangChain4j、Spring AI 或具体厂商 SDK 变成中立 TinyEngine AI Starter 的必需依赖。
- 不修改 Magic-API 2.2.2 版本及无关模块。

## 4. 兼容原则

### 4.1 双契约

官方契约与当前框架契约并存：

- `/app-center/api/**`、`/material-center/api/**`、`/platform-center/api/**` 遵循官方 TinyEngine v2.10 契约。
- `/tiny-engine/ai/**` 保留当前 `TinyEngineChatRequest`、`TinyEngineChatResponse`、Provider Registry 和 RAG 语义。

两套入口可以共享加密、HTTP、向量检索等底层组件，但不能为了复用而改变任一入口的公开请求或响应结构。

### 4.2 官方接口范围

实施开始时从官方前端的非生成源码提取 API 候选清单，并逐个定位到真实调用点。清单排除 `dist`、`node_modules`、测试快照和纯文档字符串，合并查询参数、动态路径参数和模板展开产生的重复项。最终有效契约数以提交测试资源中的逐项审计结果为准，不直接把 86 个字符串候选数当作有效路由数。

已确认的现有缺口包括：

- AI：`/app-center/api/ai/chat`、`/app-center/api/chat/completions`、`/app-center/api/encrypt-key`、`/app-center/api/ai/search`
- 应用：删除、更新、保存、发布、预览、Schema、Schema 组件、Canvas 锁、扩展创建/更新/删除
- 页面：删除、复制、历史列表、历史详情、恢复
- 物料：区块历史、区块用户/租户、资源批量创建、资源上传、模型详情兼容入口
- 生成与平台：代码生成、源码模板、任务状态、课程和动态模型数据

最终完成判定以审核后的完整契约清单为准，而不是仅以消除当前浏览器中的若干 404 为准。

### 4.3 响应外形

每个接口独立对齐官方后端。官方返回数组、对象、流或 `Result<T>` 时保持原外形，不增加全局包装，不把异常转换成空数组，也不以 `200` 伪装失败。

## 5. 模块设计

### 5.1 TinyEngine Core

`fw-plus-tiny-engine-core` 继续作为领域事实源，承载：

- 实体、DTO 和 Mapper
- Service 接口及实现
- 数据库业务规则和事务
- 与 Web 框架无关的扩展接口

Boot 层不得复制 Service 内部逻辑。新增写操作的事务边界放在 Service 层，确保 MVC 与 WebFlux 共用相同行为。

### 5.2 Boot 自动配置

`fw-plus-boot-autoconfigure` 按领域拆分薄 Controller：

- `app-center`：应用、页面、Schema、扩展、历史、国际化、数据源和任务状态
- `material-center`：区块、区块组、业务分类、模型和资源
- `platform-center`：用户、租户、课程和动态模型数据

MVC 与 WebFlux Controller 委托同一个 `TinyEngineApiFacade`。Facade 负责跨 Service 组合，不负责持久化细节。两种 Web 栈必须具备相同路由、参数和业务响应。

### 5.3 官方 AI 兼容层

在 `fw-plus-ai-boot-autoconfigure` 中增加官方协议专用边界：

- `TinyEngineOfficialAiController` / 对等 WebFlux Controller：暴露官方四个 AI 路径
- `TinyEngineOpenAiCompatibleGateway`：转发用户选择的 OpenAI 兼容服务
- `TinyEngineAiCredentialCipher`：处理 `EKEY_` Token
- `TinyEngineOfficialRagGateway`：将中立向量检索结果映射为官方搜索结果

官方协议网关不通过现有窄 DTO 重建请求，而使用能够保留未知字段的 JSON 对象。这样可原样支持 `model`、`baseUrl`、`messages`、`tools`、`tool_choice`、`response_format`、`stream_options`、`thinking_budget` 以及厂商扩展字段。

现有 `TinyEngineAiController`、`TinyEngineAiWebFluxController`、`TinyEngineAiProviderRegistry` 和 LangChain4j 插件继续存在，不改变其默认路径和配置语义。后续可以单独增强当前规范的实时流接口，但官方兼容层不能依赖该增强才能工作。

### 5.4 前端资源

继续使用 Starter 内 `src/console` 和 `pnpm` 完成构建，产物复制到 `META-INF/resources/tiny-engine`。只修改经契约测试或浏览器验证确认的兼容问题，不添加新的内置 AI 服务、模型、地址或 Key。

官方前端已有百炼和 DeepSeek 空 Key 预设保持不变。用户可选择预设并填写 Key，或创建自定义服务。

## 6. AI 数据流

### 6.1 Key 加密

1. 当 Robot 配置启用 `encryptServiceApiKey` 时，前端调用 `/app-center/api/encrypt-key`。
2. 服务端通过 SM4 对 Key 加密并返回官方 `EKEY_` Token。
3. SM4 主密钥从外部配置或环境变量获取，不写入源码和构建产物。
4. 官方 Chat 请求中，`Authorization: Bearer ...` 优先于请求体 `apiKey`，与官方 Controller 行为一致。
5. 仅在内存中解密 `EKEY_`，请求完成后不持久化明文。

缺少或无效的 SM4 主密钥必须返回明确配置错误，不能产生不可解密 Token。

### 6.2 Chat 与 Completions

1. 前端将 `baseUrl`、`model`、消息、工具和扩展参数提交到官方入口。
2. 网关校验 URL 为合法的 HTTP/HTTPS 地址，并按官方规则补齐 `/v1/chat/completions`。
3. 网关移除只供本地代理使用的 `apiKey`、`baseUrl` 字段，其余字段原样转发。
4. 非流式响应保留上游状态码、内容类型和 JSON 外形。
5. 流式响应使用 `text/event-stream` 实时透传，保留 `choices.delta`、`reasoning_content`、`tool_calls`、`finish_reason` 和 `[DONE]`，并禁用代理缓冲。

官方入口不得将流式结果先收集为 `List<TinyEngineChatStreamChunk>` 再返回。当前规范入口仍可维持既有返回类型，避免未经设计的破坏性变更。

### 6.3 RAG

`/app-center/api/ai/search` 通过中立向量存储接口检索，并映射为官方前端需要的 `score`、`content`、`doc_name`。RAG 能力关闭或依赖缺失时返回明确不可用错误，不伪造空成功结果。

## 7. 错误与安全

- 官方业务接口沿用官方错误码、HTTP 状态和 `Result<T>` 语义。
- AI 上游非 `2xx` 响应保留其状态码和可安全返回的错误正文。
- 连接失败映射为 `502`，超时映射为 `504`，缺少地址、模型或凭据映射为明确的 `4xx`。
- SSE 建立前的错误使用普通 HTTP 错误；建立后的错误发送兼容错误事件并关闭连接。
- 日志只记录请求 ID、接口、上游主机、耗时和状态码，不记录 Authorization、API Key、完整提示词、工具参数或模型正文。
- 禁止重定向时向不同主机转发 Authorization；连接和读取超时必须可配置。
- 不使用 Java 9+ 的 `java.net.http.HttpClient`、`InputStream.readAllBytes()` 或集合工厂方法。

## 8. 配置兼容

现有 `fw.plus.tiny-engine.*` 和 `fw.plus.tiny-engine.ai.*` 配置保留。新增官方 AI 配置只控制：

- 官方兼容入口是否启用
- SM4 主密钥来源
- 连接、读取和整体流超时
- 最大响应大小及允许的协议

新增配置不能预置模型、AI Base URL 或 API Key。现有样例中已出现的硬编码 AI 地址、模型和 Key 必须移除，改为用户在 Robot 中配置；真实联调只允许使用进程环境变量或临时请求，不写回仓库。

## 9. 测试与验收

### 9.1 路由契约

- 将审核后的官方前端 API 清单固化为测试资源，并保留 86 个候选路径到有效契约的归并依据。
- 对每项记录方法、路径模板、必需参数、请求体和响应类型。
- MVC 与 WebFlux 分别执行完整清单测试。
- 保留现有 `/tiny-engine/ai/**` 测试，证明新增官方入口没有改变当前规范。

### 9.2 业务与数据库

- Service 单元测试覆盖新增业务规则、事务提交和失败回滚。
- 使用隔离数据库验证 Mapper 和 SQL。
- 使用本机 MySQL 完成最终应用、页面、扩展、国际化、历史和资源工作流验证。

### 9.3 AI

使用本地模拟 OpenAI 服务验证：

- 非流式请求和响应
- 未知扩展字段透传
- 工具调用和 `tool_calls`
- 逐块 SSE、`[DONE]` 和客户端取消
- 上游 `4xx/5xx`、连接失败和超时
- SM4 加密、解密、错误 Token 和缺失主密钥
- RAG 正常结果及不可用错误

最后使用用户提供的地址、模型和 Key 做一次临时真实调用。测试输出、日志和报告不得包含凭据。

### 9.4 前端与打包

- 使用 `pnpm` 执行相关前端测试和构建。
- 验证 Starter JAR 中存在 TinyEngine 页面与静态资源，且不包含源码工作区噪声、凭据或本地配置。
- 启动 Java 8 样例后，通过浏览器验证应用创建、页面保存、页面预览、应用预览、扩展加载、国际化删除和 Robot AI。
- 检查浏览器控制台、网络请求和静态资源，确保没有相关 404、未处理 Promise 或空白预览。

## 10. 实施顺序

1. 审核官方前端的 86 个候选路径，固化有效 API 契约清单，并将每项映射到官方后端 Controller/Service。
2. 为当前缺口编写失败的 MVC/WebFlux 契约测试。
3. 按 app、page、material、platform 分组补齐 Core Service、Facade 和 Controller。
4. 为官方 AI 四个接口编写请求、错误、SM4 和 SSE 失败测试。
5. 实现官方 AI 协议网关，同时保留当前 AI 规范测试。
6. 清理样例中的硬编码 AI 配置，补齐运行说明。
7. 执行 Java 8 Maven、前端 pnpm、JAR 内容、MySQL 和浏览器全链路验证。
8. 对照审核后的契约清单逐项关闭缺口，不以局部页面可打开代替全量验收。

## 11. 完成标准

- 86 个候选路径全部完成归并审计，审核后的每项有效 API 契约均有实现和自动化证据。
- 官方路径与当前 `/tiny-engine/ai/**` 同时可用。
- MVC 与 WebFlux 契约对等。
- Robot 支持官方预设和自定义 OpenAI 兼容服务，非流式、SSE、工具调用、Key 加密及 RAG 行为符合官方前端预期。
- 页面预览、应用预览以及已报告的扩展和国际化接口不再报错。
- Java 8 构建、聚焦测试、前端构建、JAR 检查、MySQL 和浏览器验收全部通过。
- 源码、配置、日志、测试报告和构建产物均不包含 API Key。
