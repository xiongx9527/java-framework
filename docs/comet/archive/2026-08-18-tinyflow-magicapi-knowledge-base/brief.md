# Outcome

在 fw-plus 的 AI 体系中交付可运行、可扩展、可测试的 Tinyflow 工作流能力和统一知识库能力。应用通过 Tinyflow Starter 获得执行引擎和内置 UI；Magic-API 插件通过 `resource` 保存工作流及知识库数据，通过 `toolbars` 提供工作流设计器和本地知识库管理入口。Tinyflow 工作流和 Dify 均可检索 `fw-plus-local` 知识库，Tinyflow 也可通过 `dify` Provider 检索现有 Dify 知识库。

# Scope

- 在 Core AI 层新增 `fw-plus-ai-tinyflow-core`、`fw-plus-ai-tinyflow-local`、`fw-plus-ai-tinyflow-dify` 三个模块及聚合模块，并纳入父 POM 与 BOM。
- 使用 `dev.tinyflow:tinyflow-core:2.0.7`，实现兼容 LangChain4j 0.35.0 的同名覆盖类 `dev.tinyflow.langchain4j.provider.Langchain4jLlm`，并通过现有模型注册表解析模型。
- 新增 Tinyflow Spring Boot Starter，提供执行服务、自动配置、模型/向量/节点/仓库扩展 SPI、运行状态仓库和打包后的 `@tinyflow-ai/ui:1.3.6` 静态资源。
- Tinyflow Starter 的独立 npm 前端工程按 Magic-API 控制台职责组织 `src/assets`、`src/components`、`src/icons`、`src/plugins`、`src/scripts`，保留根级 `package.json`、`vite.config.js` 和单一 `src/index.js` 构建入口。
- 新增 Magic-API Tinyflow 插件，注册 `tinyflow` Magic resource、MagicScript 模块、工作流设计器 `toolbars` 入口和知识库管理 `toolbars` 入口。
- 支持保存、校验、同步执行、异步执行和恢复 Tinyflow 原生工作流 JSON，支持前后端动态节点扩展和使用现有聊天模型、Embedding 模型及向量库。
- 定义统一知识库 Provider SPI，交付 `fw-plus-local` 完整管理实现和依赖现有 `fw-plus-dify` 模块的 `dify` 只读检索实现。
- 本地知识库支持知识库、文档、chunk、索引任务、检索测试、访问密钥和 Dify 连接配置的管理；支持文本、文件、URL 等来源和可替换解析、切片、Rerank、对象存储及可选 GraphRAG 扩展。
- Magic resource 保存知识库定义、文档、chunk、任务、访问密钥和 Dify 连接等六类资源；模型、向量库和 S3 只保存现有资源引用。
- 提供 Dify External Knowledge API `POST /retrieval`，仅检索 `fw-plus-local`，返回 Dify 所需记录及完整来源 metadata。
- 更新 AI Magic-API sample，覆盖工作流设计、保存、真实测试、本地知识库管理、Tinyflow Provider 检索和 Dify External API 检索。

# Non-goals

- 不修改 `springboot2`、`springboot3` 或其他分支，本次只修改 `dev`。
- 不升级 Spring Boot 2.7.18、Magic-API 2.2.2、LangChain4j 0.35.0 等现有技术栈。
- 不依赖或改造官方 `tinyflow-support-langchain4j` JAR，不依赖 classpath 顺序选择适配类。
- 不复制 Tinyflow UI 上游源码，不默认使用外部 CDN；UI 使用固定 npm 包构建并物理打入 Starter。
- 不复制 Tinyflow UI 上游源码；Tinyflow Starter UI 只依赖固定 npm 包和本项目的宿主组件，不引入 pnpm workspace 或额外前端 monorepo。
- 不新增 Tinyflow 私有模型、Embedding、向量库或 S3 连接管理，继续使用现有注册中心和动态数据源。
- 不新增关系型数据库表，不默认引入 Neo4j；GraphRAG 只保留可选 SPI。
- 不把 Dify External Knowledge API 请求转发回 `dify` Provider，避免循环调用。
- 不承诺默认内存运行状态在应用重启后仍可恢复。

# Acceptance examples

- 在 Magic-API 控制台通过 Tinyflow toolbar 新建工作流，选择现有 LangChain4j 模型，保存后输入变量运行，能得到真实结果；含挂起节点的流程能以实例 ID 恢复。
- 工作流资源正文是 Tinyflow 原生 JSON。非法 JSON、未知节点、无起止路径或未保存修改均得到明确提示，且不会覆盖已保存版本。
- Starter JAR 中存在版本化 Tinyflow UI JS/CSS；Magic 插件的 Tinyflow UI 资源只通过 Starter 提供和加载，插件保留知识库对象存储所需的 S3 依赖，卸载页面时正确销毁画布。
- 业务注册自定义 `TinyflowNodeProvider` 及对应前端节点定义后，新节点可保存并执行；缺少后端解析器时明确报错。
- 本地知识库管理 UI 可创建知识库、导入文档、查看和编辑 chunk、重建索引、删除文档并执行检索测试；模型、向量库和对象存储均从现有资源选择。
- 同一 `sourceId` 内容未变化时导入幂等；内容变化时创建新 `documentId` 版本并原子切换，旧向量可补偿删除。
- 每个 chunk 都包含非空 `sourceId`、`sourceType`、`documentId`、`chunkId`，检索结果保持四级来源身份。
- Tinyflow Knowledge 节点使用 `provider=fw-plus-local` 或 `provider=dify` 检索；未知 Provider、知识库不存在、能力不支持和远端失败均返回稳定错误。
- Dify 携带有效访问密钥调用 `POST /retrieval` 可检索本地知识库；响应 metadata 必含 `source_id`、`source_type`、`document_id`、`chunk_id`，无效密钥或租户越权被拒绝。

# Constraints and invariants

- 所有新增 Java 代码兼容 Java 8，不使用 Java 9+ API。
- Core Tinyflow 模块不依赖 Spring、Servlet、Magic-API 或前端构建工具。
- 依赖版本固定为 `tinyflow-core:2.0.7` 和 `@tinyflow-ai/ui:1.3.6`。
- Magic-API 使用项目自行维护的 2.2.2 版本及现有内部仓库配置，不调整其版本。
- 依赖方向固定为 Magic 插件依赖 Starter，Starter 依赖 Core/local/dify；Starter 不反向依赖 Magic-API。
- `fw-plus-local` 是完整管理 Provider，`dify` 是依赖现有 `fw-plus-dify` 的只读 Provider。
- Dify External Knowledge API 只能路由到 `fw-plus-local`。
- 所有知识库操作按租户隔离；访问密钥、连接凭据和资源敏感内容不得进入日志或普通响应。
- UI 列表与执行端使用同一模型、向量和知识库 Provider，确保可选择的 key 必然可解析。
- chunk 四级来源身份在导入、存储、向量 metadata、检索和 Dify 响应的全链路中不可丢失。

# Decisions

- 只在当前 `dev` 分支实施；完成后由用户按既有流程合并到目标分支。
- Tinyflow 使用最新已确认且兼容 Java 8 的 `tinyflow-core:2.0.7`。
- LangChain4j 低版本通过同名覆盖类适配，不引入官方 support JAR。
- Starter 内包含 npm 方式构建的 UI，版本固定为 `@tinyflow-ai/ui:1.3.6`；上游 Bug 优先在包装层或可审计 npm patch 中修复。
- 前端目录采用 Magic-API 单应用方案：`src/index.js` 作为构建入口，Tinyflow 画布宿主放入 `src/components`，知识库页面放入 `src/plugins/knowledge`，公共资源、图标和脚本分别放入 `src/assets`、`src/icons`、`src/scripts`。
- Magic 插件同时提供 `resource` 单数配置和 `toolbars` UI 入口，并依赖 Starter。
- 知识库抽象放在 Tinyflow 模块族，当前 Provider key 固定为 `fw-plus-local` 与 `dify`。
- `dify` Provider 依赖当前已有 `fw-plus-dify` 模块；不把适配职责移入通用客户端。
- 知识库管理 UI 以 `fw-plus-local` 为主，参考 Dify/RAGFlow 的完整工作流，但复用 fw-plus 现有模型、向量和对象存储资源。
- `sourceId` 与 `sourceType` 连同 `documentId`、`chunkId` 均为 chunk 强制字段。
- Tinyflow 同步执行超时只停止调用方等待并返回受控错误，不承诺强制终止底层工作流或已开始的外部副作用；可能长时间运行的流程使用异步执行与恢复接口。

# Open questions

- [blocking] CONFIRM: 将 `fw-plus-ai-tinyflow-spring-boot-starter/src/main/frontend/tinyflow-ui` 整理为 Magic-API 风格独立 npm 单应用：保留根级 package/vite/index 入口，将 Tinyflow 宿主、知识库页面、公共资源、图标和脚本分别归入 `src/components`、`src/plugins/knowledge`、`src/assets`、`src/icons`、`src/scripts`，补齐 npm lockfile，保持版本化 JAR 资源输出和 Magic 插件加载路径不变，并用 npm 测试、npm 构建及 Maven/JAR 检查验证。

# Verification expectations

- 按 TDD 顺序为 Core 契约、LangChain4j 适配、Starter 自动配置、local/dify Provider、Magic resource/API 和前端宿主添加测试。
- 使用系统 Maven 3.6.3 按 Core -> AI -> sample 顺序编译和测试；当前 Wrapper 缺少 `.mvn/wrapper`，不依赖 Wrapper 执行。
- 检查依赖树，确认不存在 `tinyflow-support-langchain4j`、LangChain4j 1.x 或重复 `Langchain4jLlm`。
- 运行 Starter自动配置上下文测试、Magic-API 插件资源与 HTTP 契约测试、Dify `/retrieval` 认证/租户/metadata 测试。
- 执行 npm `ci`、测试和构建，检查 Starter JAR 的版本化 UI 资源及许可证文件。
- 启动 AI Magic-API sample，验证健康检查、工作流保存与执行、知识库导入与检索、Dify External API；对工作流和知识库管理页面进行桌面与移动视口截图及交互检查。
