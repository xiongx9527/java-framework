# Tinyflow 工作流能力规格

## Requirement: 模块和依赖边界

系统必须在 Core AI 层提供 Tinyflow core、local、dify 模块及聚合模块，在 AI 层提供 Tinyflow Starter 和 Magic-API Tinyflow 插件，并维持 Magic 插件 -> Starter -> Core/Provider 的单向依赖。

#### Scenario: Java 8 兼容依赖

- **GIVEN** 当前项目使用 Java 8、Spring Boot 2.7.18 和 LangChain4j 0.35.0
- **WHEN** 构建 Tinyflow 模块依赖树
- **THEN** 系统使用 `dev.tinyflow:tinyflow-core:2.0.7`
- **AND** 依赖树不包含 `tinyflow-support-langchain4j`、LangChain4j 1.x 或 Java 17 字节码

#### Scenario: 框架无关 Core

- **WHEN** 检查 Core Tinyflow 模块依赖
- **THEN** Core 不依赖 Spring、Servlet、Magic-API 或前端构建工具

## Requirement: LangChain4j 0.35.0 适配

系统必须提供完全限定名为 `dev.tinyflow.langchain4j.provider.Langchain4jLlm` 的 Java 8 兼容实现，将 Tinyflow 消息转换到 LangChain4j 0.35.0 `ChatLanguageModel`，且运行时只能存在一个该类。

#### Scenario: 消息和响应转换

- **GIVEN** Tinyflow LLM 节点包含系统消息、文本内容或图片内容
- **WHEN** 适配器调用 LangChain4j 模型
- **THEN** 适配器使用 0.35.0 的 `SystemMessage`、`UserMessage`、`TextContent`、`ImageContent` 和 `generate` API
- **AND** 将 `Response<AiMessage>.content()` 转换为 Tinyflow 文本响应

#### Scenario: 无效模型响应

- **GIVEN** 模型返回空 Response、空 `AiMessage` 或空文本
- **WHEN** Tinyflow 执行 LLM 节点
- **THEN** 系统抛出稳定的 Tinyflow 执行异常
- **AND** 错误不包含密钥、请求头或敏感变量

#### Scenario: 模型解析

- **GIVEN** 节点声明 `llmId`
- **WHEN** LLM Provider 创建适配器
- **THEN** 系统从现有 `LangChain4jModelRegistry` 按 key 获取 `ChatLanguageModel`
- **AND** 空 `llmId` 使用注册表默认模型
- **AND** 模型缺失或类型错误时返回包含安全 `llmId` 的明确错误

#### Scenario: 逐请求采样参数限制

- **GIVEN** 节点保存 temperature、topP、topK 或其他采样选项
- **WHEN** 底层仅暴露 LangChain4j 0.35.0 通用 `ChatLanguageModel`
- **THEN** 系统保留节点配置但使用已注册模型 Bean 的构建参数
- **AND** 可通过不同模型 key 选择具有不同采样配置的模型实例

## Requirement: Starter 自动配置和执行门面

Starter 必须在 `fw.plus.ai.tinyflow.enabled` 开启时提供解析器、执行器、定义仓库、状态仓库、`TinyflowService`、模型/向量 Provider 和 LLM Provider，并允许业务 Bean 覆盖默认实现。

#### Scenario: 默认启动

- **GIVEN** classpath 包含 Tinyflow 且未定义自有 Bean
- **WHEN** Spring Boot 应用启动
- **THEN** 自动配置创建完整执行所需 Bean
- **AND** 默认同步超时为 5 分钟
- **AND** 默认内存状态仓库仅承诺单进程生命周期内恢复

#### Scenario: 条件和覆盖

- **GIVEN** 应用关闭 Tinyflow 或提供自定义仓库/Provider Bean
- **WHEN** 自动配置生效
- **THEN** 总开关关闭时不创建 Tinyflow Bean
- **AND** 自定义 Bean 替换对应默认 Bean

#### Scenario: 静态 Provider 生命周期

- **WHEN** 应用上下文启动和关闭
- **THEN** 系统注册当前 LangChain4j LLM Provider
- **AND** 关闭时从 Tinyflow `LlmManager` 注销，避免重复注册

## Requirement: 工作流资源存储

Magic-API 插件必须注册 `tinyflow` resource，正文直接保存 `@tinyflow-ai/ui` 导出的原生 JSON，不增加项目自定义外层对象，并以 Magic 资源 ID 作为执行定义 ID。

#### Scenario: 保存有效定义

- **GIVEN** 工作流包含有效 `nodes`、`edges` 和可选 `viewport`
- **WHEN** 用户保存资源
- **THEN** 插件先完成 JSON、节点、连线和起止路径校验
- **AND** 保存成功后立即刷新内存定义

#### Scenario: 拒绝无效定义

- **GIVEN** 工作流 JSON 非法、存在未知节点、缺少节点/连线或无完整起止路径
- **WHEN** 用户保存资源
- **THEN** 系统返回包含资源路径、节点 ID 和节点 type 的可诊断错误
- **AND** 不覆盖当前已保存版本

#### Scenario: 路径解析

- **GIVEN** MagicScript 使用资源分组路径调用工作流
- **WHEN** 路径唯一匹配资源
- **THEN** 系统按资源 ID 注册并执行定义
- **AND** 路径不存在、歧义或资源已删除时明确失败

## Requirement: 工作流执行

系统必须支持同步执行、异步执行和按实例 ID 恢复，并在执行前解析最新已保存资源。

#### Scenario: 同步测试运行

- **GIVEN** 用户已保存工作流并提供输入变量
- **WHEN** 调用同步执行
- **THEN** 系统在有限超时内返回清理内部状态后的结果
- **AND** 超时或线程中断产生受控错误且恢复中断标记
- **AND** 超时只终止调用方等待，不承诺强制终止底层工作流或已开始的外部副作用
- **AND** 可能长时间运行的流程使用异步执行与恢复接口

#### Scenario: 异步和恢复

- **GIVEN** 工作流包含挂起节点
- **WHEN** 调用异步执行后按实例 ID 恢复
- **THEN** 系统返回实例 ID并从默认状态仓库恢复执行
- **AND** API 明确默认内存仓库不支持跨重启恢复

#### Scenario: 未保存修改

- **GIVEN** 设计器存在未保存修改
- **WHEN** 用户发起测试运行、切换资源或关闭设计器
- **THEN** 系统要求先保存或明确放弃修改
- **AND** 测试始终使用后端已保存定义

## Requirement: 动态节点扩展

Core 必须提供框架无关的 `TinyflowNodeProvider` SPI，Starter 收集节点解析器，前端通过 Tinyflow `customNodes` 注册匹配的节点定义。

#### Scenario: 注册自定义节点

- **GIVEN** 业务提供唯一节点 type 的后端 Provider 和匹配的前端节点描述
- **WHEN** 应用启动并打开设计器
- **THEN** 节点可在设计器中创建、保存并由后端执行

#### Scenario: 节点冲突或缺失

- **GIVEN** 多个 Bean 注册同一 type 且无法确定优先级，或工作流引用未安装 type
- **WHEN** 构建解析器或执行工作流
- **THEN** 冲突在启动时失败并列出相关 Bean
- **AND** 未安装节点在执行前明确失败，不得静默忽略

## Requirement: 复用现有模型和向量资源

设计器和执行端必须通过相同的模型、向量和知识库 Provider 暴露及解析现有资源 key，不建设 Tinyflow 私有连接配置。

#### Scenario: 模型和向量选项

- **WHEN** 设计器请求资源选项
- **THEN** LLM 节点只看到现有 CHAT 模型
- **AND** 向量化只看到现有 EMBEDDING 模型
- **AND** 知识库只看到现有向量库与 Provider
- **AND** 响应不包含密钥或完整连接属性

#### Scenario: Magic 动态资源

- **GIVEN** Magic-API 已注册动态模型或向量数据源
- **WHEN** Tinyflow 插件处于应用中
- **THEN** 插件 Provider 同时暴露 Spring 注册资源和 Magic 动态资源
- **AND** Starter 不反向依赖 Magic 动态数据源类

## Requirement: Starter 内置 UI

Starter 必须通过独立 npm 工程固定使用 `@tinyflow-ai/ui:1.3.6`，将构建产物和许可证物理打入版本化 classpath 目录；Magic 插件对 Tinyflow UI 只提供宿主和加载器，知识库对象存储仍可通过独立 S3 依赖复用现有能力。前端工程必须采用 Magic-API 风格的单应用职责目录，不引入 pnpm workspace 或额外 monorepo。

#### Scenario: Magic-API 风格宿主目录

- **WHEN** 开发者进入 Tinyflow Starter 的 `src/main/frontend/tinyflow-ui` 前端工程
- **THEN** 根目录提供 `package.json`、`package-lock.json`、`vite.config.js` 和单一 `src/index.js` 构建入口
- **AND** Tinyflow 画布宿主代码位于 `src/components`
- **AND** 知识库页面和 API 模块位于 `src/plugins/knowledge`
- **AND** `src/assets`、`src/icons`、`src/scripts` 作为公共资源、图标和脚本职责边界存在并可独立扩展
- **AND** 现有版本化 Starter 资源输出和 Magic 插件 Tinyflow UI 加载路径保持不变
- **AND** Magic 插件可继续保留知识库对象存储所需的独立 S3 依赖，不将该依赖视为 Tinyflow UI 资源加载依赖

#### Scenario: 构建和打包

- **WHEN** 执行 Starter 前端和 Maven 构建
- **THEN** 使用精确版本和 lock 文件完成 `npm ci`
- **AND** Starter JAR 包含 `/META-INF/resources/fw-plus/tinyflow-ui/1.3.6/` 下的 JS、CSS 和许可证信息
- **AND** 仓库不提交 `node_modules`

#### Scenario: 页面加载与卸载

- **WHEN** 用户点击 `toolbars` Tinyflow 入口
- **THEN** 插件在全屏宿主中按当前 context path 加载 Starter UI 并挂载画布
- **AND** 页面卸载时调用 `destroy()` 清理实例和监听器
- **AND** 静态资源缺失或版本不匹配时显示诊断信息而非空白页面

## Requirement: Magic-API 集成和权限

Magic 插件必须注册 `resource` 单数配置、工作流及知识库 `toolbars`、MagicScript `tinyflow` 模块和所需管理/测试接口，并沿用 Magic-API 编辑器权限边界。

#### Scenario: MagicScript 调用

- **WHEN** 脚本调用 `tinyflow['group/name'].execute(variables)`、`executeAsync` 或 `resume`
- **THEN** 模块路由到同一 `TinyflowService` 并返回稳定结果或错误

#### Scenario: 管理接口安全

- **WHEN** 未授权用户请求资源、模型列表、保存或测试接口
- **THEN** 系统沿用现有编辑器认证和权限拒绝请求
- **AND** 日志与响应不泄露模型密钥、Authorization、请求头或敏感变量
