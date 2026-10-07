# 目标

接管当前全部未提交改动（45 个文件），交付一套**可用的** Magic-API 控制台增强，解决已报障的 3 类问题：

- KB 知识库管理端接口（`KbConsoleController` 全套 `/kb/**`）在知识库装配时真实注册并可访问，不再 404。
- 前端控制台 i18n 的 `kb.*` key 中英齐备，且**重建后的构建产物**实际包含，不再显示未翻译原始 key。
- MCP 控制台可用：`/mcp/server/list` 不再抛 `NoSuchBeanDefinitionException`；新增 `/mcp/datasource/test` 连接测试。
- 前端产物按新架构重建：content-retriever 单一入口（datasources + 知识库 toolbars），mcp-server 新 datasource service 与图标。
- auth-core（optional 依赖）的 classpath 条件加固：缺失时各配置类整体降级不装配，杜绝 `NoClassDefFoundError`。
- 全仓编译与样例启动无回归。

# 范围

本次接管的范围即当前未提交改动的全部内容，按主题分组：

1. **KB 知识库控制台（后端）**
   - `KbConsoleController`（接口路径/响应契约微调，全套 `/kb/**`）
   - `KnowledgeConfiguration`（隐身插件、auth-core 条件、Controller 注册守卫）
2. **前端整合（content-retriever 插件）**
   - `src/console/src/index.js`：datasources + toolbars 单入口
   - 删除 `index-kb.js`、`vite.kb.config.js`；`package.json` build 脚本合并为 `vite build`
   - i18n `en.js` / `zh-cn.js`（补 `metadata-filter-mode` 及全部 `kb.*` 使用键）
   - `metadata/providers.js`（knowledge 类型新增 `metadata-filter-mode` select）
3. **MCP 控制台（后端 + 前端）**
   - `MagicDynamicMcpServer.testConnection()`、`MagicMcpServerController /mcp/datasource/test`
   - `MagicAPIMcpServerConfiguration` / `McpAutoConfiguration`（auth-core 条件）
   - mcp-server console：`index.js`（新 datasource service + 手动 SVG 图标注入）、`magic-mcp-server.js`（doTest）、`vite.config.js`（去 vite-plugin-svg-icons）、`icons/server.svg`
4. **依赖与启动加固**
   - `fw-plus-dependencies` BOM 增补 `fw-plus-auth-core`；`fw-plus-ai-boot-autoconfigure`、两个插件模块、`ai-magicapi-sample` 的 pom 与 `application.yaml`（H2/schema/插件配置）
   - `MyDefaultMagicResourceService`（group null 守卫）、`WsAutoConfiguration`（auth-core 条件）
5. **Build 阶段必须补全的已知缺口**
   - 根因定位并修复 `KbConsoleController` 未注册（404）
   - 处理 `MagicMcpServerController.list()` 的 `NoSuchBeanDefinitionException`
   - 重建两个插件的前端 dist 产物（`magic-langchain4j-content-retriever.*.iife.js`、`magic-mcp-server.*.iife.js`）

# 非目标

- 不恢复独立的 KB 插件产物（`magic-kb-browser` iife）；维持「并入 content-retriever 单入口 + 后端隐身插件」架构。
- 不升级 Magic-API / Spring Boot / langchain4j 版本；`org.ssssssss` 依赖仍指向内部 Nexus 的 2.2.2 自维护版。
- 不实现新的知识库业务能力（检索、对外 API、Dify 兼容已在先前 change 交付）；本次仅修复控制台可用性与前端整合。
- 不引入 Java 9+ API（JDK 1.8 约束）。
- **不交付构建噪音**：`.flattened-pom.xml`（flatten 插件生成物）、`.DS_Store`、eureka 样例 `.gitignore` 调整均不进入交付 commit（构建时会重新生成）。

# 验收示例

- **A1** 样例启动（知识库装配，H2 + schema-h2.sql）后，`GET /magic-api/web/kb/dataset`、`GET /magic-api/web/kb/settings`、`GET /magic-api/web/kb/openapi-auth-providers` 返回 `{"code":1,...}` 而非 404；`POST /kb/dataset`（键为合法表单）创建后 `GET /kb/dataset` 可列出。（Build 阶段根因修正：控制台 `request.js` 以 `data.code === 1` 为成功判定，`ok()` 必须返回 code=1，旧值 200 会让控制台全部请求走全局异常分支。）
- **A2** `KbConsoleController` 其余路径按能力规格 §3.1 全量可访问：hitTest、preview-split、import/export、document（upload/reindex/enabled/archived/metadata/segment/move/download/delete）、segment（enabled/update/delete）、settings 等；任意存在路径不再返回 404，无效 id 返回业务错误而非 404。（Build 阶段补全：`DELETE|POST /kb/document/{id}` 删除文档端点，前端删除按钮原调该路径但后端缺失。）（Build iteration-2 根因修复：规格 §3.1 规定数据集/分段的更新与删除同为 `POST /{kb|...}/dataset|segment/{id}`、以 body 是否为空对象 `{}` 区分，原实现拆成 update `{PUT,POST}` 与 delete `{DELETE,POST}` 两个 handler，POST 歧义触发 Spring "Ambiguous handler methods" 一律 500，控制台编辑/删除数据集与分段不可用；现改为单 handler `{PUT,DELETE,POST}` 内按 method + body 分发——DELETE 或 POST+空 body 走删除，否则走更新。）
- **A3** `zh-cn.js`/`en.js` 含前端实际使用的全部 `kb.*` 键（含 `kb.dataset.title`、`kb.empty`），且 `kb` 段位于字典**顶层**（与前端 `i18n.format('kb.*')` 引用同层，历史缺陷是嵌在 `langchain4j` 命名空间下导致查不到而显示原始 key）；重建后的 `dist/magic-langchain4j-content-retriever.*.iife.js` 中可 grep 到对应文案（esbuild 默认 charset 将中文转义为大写 `\uXXXX` 形式，如 `知识库` = `\u77E5\u8BC6\u5E93`），不再显示原始 key。
- **A4** 重建后的 content-retriever iife 被控制台加载（`/magic-api/web/plugins` 中 `langchain4j-content-retriever` 指向该产物），toolbars 出现「知识库」入口并可打开（数据集/文档/分段/召回测试/设置视图）。
- **A5** MCP 控制台接口不再抛 `NoSuchBeanDefinitionException`：`GET /magic-api/web/mcp/server/list` 在 MCP 插件装配时返回运行实例列表；未装配时返回空列表或明确提示。
- **A6** `POST /magic-api/web/mcp/datasource/test` 按控制台表单真实装配并经传输探测后返回 `"ok"` 或错误描述；不落库、不注册、不影响运行中实例。
- **A7** 重建后的 `dist/magic-mcp-server.*.iife.js` 包含新 datasource service（doTest 调 `/mcp/datasource/test`）与 `magic-mcp-server` 图标 symbol；控制台 `mcp-server` 类型 datasource 可打开并执行「测试连接」。
- **A8** auth-core optional 场景：`KnowledgeConfiguration`、`MagicAPIMcpServerConfiguration`、`McpAutoConfiguration`、`WsAutoConfiguration` 均以 classpath 条件纳入 `InstanceAuthRegistry`；条件评估匹配（样例含 auth-core），缺失时整类降级不装配、不抛 `NoClassDefFoundError`。
- **A9** 全仓 `./mvnw compile`（或分层增量）通过；`ai-magicapi-sample` 可启动，`/magic-api/web/plugins` 正常返回 4 个插件（s3/mcpserver/langchain4j-content-retriever/langchain4j-knowledge）。
- **A10** 交付 commit 不含 `.flattened-pom.xml`、`.DS_Store` 等生成噪音与 eureka 样例无关调整。

# 约束与不变量

- JDK 1.8 语法（禁 Java 9+ API）；Spring Boot 2.7.18；Magic-API 2.2.2 自维护版（`org.ssssssss` 依赖指向内部 Nexus，不得改版本号）。
- Java 源码 UTF-8；缩进与周围文件一致（Java/XML 用 Tab）。
- Agent 只编辑 brief、完整目标规格与 `children.yaml`；`comet-state.yaml`、`verification.md` 由 Runtime 管理。
- 与既有能力规格对齐：`ai-knowledge-rag`（§3.1 管理端、§4 控制台前端）、`magicapi-mcp-plugin`（连接测试/认证条件）；冲突时重读最新规格并按用户意图改写。

# 决策

- **D1** 隔离方式：`current`（沿用当前分支与目录，保留全部未提交 WIP）。
- **D2** 范围：接管当前全部未提交改动（45 个文件），按上方 # 范围 分组交付。
- **D3** 架构：KB 管理端 UI 并入 content-retriever 单入口产物；后端 `langchain4j-knowledge` 作为隐身插件（`javascriptFilename` 为空）注册，`plugin()` 不得返回 null。
- **D4** 交付物包含重建后的前端 dist 产物。两个 console 的 `dist/` 均被各自 `.gitignore` 忽略、由构建期 `prepare-package`（exec-maven-plugin：npm install + npm run build）从 `src/console` 重新生成——与同层 14 个 langchain4j console 及 boot 层各 plugin console 的惯例一致（仓库仅 admin/druid/swagger 三个历史 console 提交了 dist）。本次交付以「前端源码修复 + 工作区 dist 已重建（供 A3/A7 静态 grep 验收）」为准，不将 dist 强行纳入 commit。
- **D5** 非受控噪音（`.flattened-pom.xml`、`.DS_Store`、eureka 样例调整）不进入交付 commit。
- **D6** KB 控制台入口可见性：恢复 `adminOnly: true`（仅管理员可见，符合资规格 §4.1）。后端 `AbstractLangchain4jPluginConfiguration` 已反射调用 `Plugin#setAdminEnabled` 且运行样例 `/magic-api/web/plugins` 已验证 `adminEnabled:true` 生效，因此清除 WIP 注释里“中央版 jar 强制 false”的过时绕过（`adminOnly: false`）。
- **D7** MCP 控制台容错：`MagicMcpServerController.list()` / `datasourceTest` 在 `MagicDynamicMcpServer` Bean 缺失时返回空列表 / 明确错误描述，不抛 `NoSuchBeanDefinitionException`。

# 待解决问题

- 用户已确认 Q1（KB 入口 `adminOnly: true`）与 Q2（MCP 容错空列表），见决策 D6/D7。无剩余阻塞问题。

# 验证预期

- **构建**：`cd fw-plus-ai && ./mvnw compile -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure` 与全仓聚合编译通过；两个前端 `src/console` 各自 `npm build`（或 `vite build`）产出更新后的 `dist` iife。
- **运行**：启动 `ai-magicapi-sample`（端口 3089），`curl` 验证 A1/A2/A5/A6 各接口；`/magic-api/web/plugins` 验证 A4/A9。
- **静态检查**：`grep` 重建后的 iife 验证 A3/A7 的文案与接口路径；`git diff --stat` 验证 A10（无生成噪音）。
- **回归**：`./mvnw -o compile`（样例聚合入口）通过；既有已归档能力不因本次改动失效。