# 目标

根据 Dify 官方 API 文档（快速开始 <https://docs.dify.ai/zh/api-reference/guides/get-started> 及其直接链接页面），完善 `fw-plus-core/fw-plus-project/fw-plus-dify` 模块的 Dify SDK：按概览目录级补齐缺失接口，并按文档严格对齐现有接口，使 SDK 覆盖文档描述的服务 API 能力。完整行为规格见 `specs/dify-sdk/spec.md`。

# 范围

## Source coverage

来源主链接：<https://docs.dify.ai/zh/api-reference/guides/get-started>（complete）。直接链接的 9 页全部完整读取（complete）：终端用户身份、6 个应用类型概览（chatflow / workflow / agent / chat / completion / knowledge）、知识库列表（OpenAPI）、知识库详情（OpenAPI）；概览所列关键端点详情页（应用侧 14 页 + 知识库侧 26 页 + 会话历史页）由子任务抓取读取（complete），用于固化路径与参数。

| ID | 来源定位 | 读取 | 保留语义 | Spec 位置 | 验收 ID | 覆盖状态 | 理由 |
|----|------|------|-----------|-----------|---------|----------|------|
| S1 | get-started · Warning 仅后端调用密钥 | complete | 密钥只在服务端使用 | §1 | — | background | 安全告示，无 SDK 行为 |
| S2 | get-started · 获取 API 密钥 | complete | 应用密钥/知识库密钥创建入口 | §4.1 | — | background | 控制台操作指引 |
| S3 | get-started · Bearer 认证与基础 URL | complete | `Authorization: Bearer`；默认 `https://api.dify.ai/v1`，自部署可替换 | §1、§2 | A1 | covered | 可执行约束 |
| S4 | get-started · `GET /v1/info` | complete | 请求方式与响应字段 | §3.1 | A2 | covered | 可执行端点 |
| S5 | get-started · 知识库密钥首次调用分流 | complete | 限定→详情；全量→列表 | §4.2 | A3 | covered | 可执行端点 |
| S6 | get-started · 应用类型概览导航（6 链接） | complete | 导航到各概览页 | — | — | non-goal | 导航单元，语义由 G 系列承接 |
| E1 | end-user-identity · `user` 作用范围 | complete | 会话/文件/停止/恢复校验 user 一致 | §3.3 | A4 | covered | 可执行行为约束 |
| E2 | end-user-identity · 停止接口 user 不符静默成功；Workflow 停止不查 user | complete | 静默忽略语义 | §3.2、§3.6 | A4、A11 | covered | 可执行行为约束 |
| E3 | end-user-identity · 恢复事件流 user 不符 404 | complete | 404 语义 | §3.6 | A11 | covered | 可执行行为约束 |
| E4 | end-user-identity · API 与 Web 用户独立 | complete | 两会话空间隔离 | §2 | — | background | 服务端语义 |
| E5 | end-user-identity · 获取终端用户信息 | complete | `GET /end-users/{id}` | §3.3 | A5 | covered | 可执行端点 |
| G1 | 各概览 · 发送消息并接收流式回复 | complete | chat-messages 阻塞/流式、Agent 仅流式 400、stop、suggested | §3.2 | A6 | covered | 可执行端点组 |
| G2 | 各概览 · 管理会话 | complete | 会话 CRUD + 会话变量 list/update | §3.2 | A7 | covered | 可执行端点组 |
| G3 | 各概览 · 处理文件 | complete | 上传、下载 `GET /files/{id}/preview` | §3.3 | A8 | covered | 可执行端点组 |
| G4 | 各概览 · 语音转写与合成 | complete | audio-to-text（30MB、MIME 集合）、text-to-audio（voice、message_id 优先） | §3.4 | A9 | covered | 可执行端点组 |
| G5 | 各概览 · 获取应用信息与配置 | complete | info/parameters/meta/`GET /site` | §3.1 | A2 | covered | 可执行端点组 |
| G6 | chatflow/chat/completion 概览 · 收集反馈 | complete | 提交反馈 + `GET /app/feedbacks` 列表 | §3.5 | A10 | covered | 可执行端点组 |
| G7 | chatflow/chat 概览 · 管理标注 | complete | 标注 CRUD + 回复配置 + 任务状态 | §3.5 | — | covered(现状) | 现有实现已覆盖，保持 |
| G8 | workflow 概览 · 运行与跟踪控制 | complete | run、按 ID 执行、停止任务（`/workflows/tasks/`）、恢复事件流（`/workflow/` 单数）、运行详情、日志 | §3.6 | A11 | covered | 可执行端点组 |
| G9 | chatflow/workflow 概览 · 人工介入暂停 | complete | 表单 GET/POST `/form/human_input/{form_token}`、412 两态 | §3.7 | A12 | covered | 可执行端点组 |
| G10 | agent/chat/chatflow 概览 · 流式事件差异 | complete | 事件类型分派与 error 事件 HTTP 200 | §3.8 | A13 | covered | 可执行行为 |
| K1 | knowledge 指南 · 密钥范围 403 语义 | complete | 路径无 dataset_id 或不在范围 → 403 | §4.1 | A15 | covered | 可执行约束 |
| K2 | knowledge 指南 · 访问 API 权限开关 | complete | 关闭后 403 | §4.1 | A15 | covered | 可执行约束 |
| K3 | knowledge 指南 · 创建和管理知识库 | complete | 6 端点（列表/详情为新增） | §4.2 | A3 | covered | 可执行端点组 |
| K4 | knowledge 指南 · 添加和更新文档（含异步索引 Steps） | complete | 创建/轮询/列表/详情/下载/ZIP/更新（PATCH 对齐）/批量状态/删除 | §4.3 | A14 | covered | 可执行端点组 |
| K5 | knowledge 指南 · 分段与子分段 | complete | 分段 CRUD + 详情新增 + 子分段 CRUD | §4.4 | A15 | covered | 可执行端点组 |
| K6 | knowledge 指南 · 元数据字段 | complete | 7 端点全部新增 | §4.5 | A16 | covered | 可执行端点组 |
| K7 | knowledge 指南 · 标签 | complete | 7 端点全部新增 | §4.6 | A17 | covered | 可执行端点组 |
| K8 | knowledge 指南 · 查询可用模型 | complete | `GET /workspaces/current/models/model-types/{model_type}` | §4.7 | A17 | covered | 可执行端点 |
| K9 | knowledge 指南 · 知识流水线 | complete | 4 端点全部新增 | §4.8 | A18 | covered | 可执行端点组 |
| B1 | 知识库列表页 · OpenAPI `GET /datasets` | complete | 参数 page/limit/keyword/include_all/tag_ids；403；分页结构 | §4.2 | A3 | covered | 端点详情 |
| B2 | 知识库详情页 · OpenAPI `GET /datasets/{dataset_id}` | complete | 403/404；Dataset 结构 | §4.2 | A3 | covered | 端点详情 |
| B3 | 会话历史页 · OpenAPI `GET /messages`（详情页） | complete | conversation_id/first_id/limit 参数，与现有实现一致 | §3.2 | — | covered(现状) | 现有实现与文档一致，保持 |

# 非目标

1. 不升级模块依赖，不新增运行时框架（保持 okhttp/okhttp-sse/fastjson2 技术栈）。
2. 不改动 Console 控制台 API（`ConsoleRoutes` 登录、应用列表）现状。
3. 不接入真实 Dify 实例在线联调；验收以离线单元测试与代码检查为准。
4. 不改动 fw-plus-dify 以外的模块代码。
5. 不实现 Dify 控制台 Web 界面能力、不做服务端部署配置。
6. 逐端点详情页的响应字段级建模（Q1 选 A 而非 C）：新方法以 `String`/`JSONObject` 返回为主，不做全字段类型化。

# 验收示例

- **A1** 认证与基址：所有经 `SimpleHttpClient` 发出的请求携带 `Authorization: Bearer {apiKey}`；`ApiConfig` 支持默认 `https://api.dify.ai/v1` 与自定义自部署基址；非 2xx 响应抛 `DifyException`/`DifyClientException` 且保留响应 `code`（401 unauthorized、403 forbidden、404 not_found、400 bad_request），敏感查询参数脱敏测试不回退。
- **A2** 应用配置四端点：`getAppInfo`/`getAppParameters`/`getAppMetaInfo` 分别对应 `GET /info`、`GET /parameters`、`GET /meta`；新增 `getAppWebAppSettings` 对应 `GET /site`，错误码 403 `forbidden` 透传。
- **A3** 知识库列表与详情：新增 `GET /datasets` 方法（query：page/limit/keyword/include_all/tag_ids，响应 data/has_more/limit/page/total）与 `GET /datasets/{dataset_id}` 详情方法（403 未启用 API/不在范围、404 语义透传），两者均被单元测试断言请求方法与路径。
- **A4** user 显式传递：会话、文件、停止、恢复相关方法（含新增的会话变量、文件下载、事件流恢复）签名携带 `user`（方法参数或参数对象字段），单测断言请求中带 user；Javadoc 注明「停止接口 user 不一致时服务端静默成功、恢复事件流 user 不符返回 404、Workflow 停止不检查 user」。
- **A5** 终端用户信息：新增 `GET /end-users/{end_user_id}`，404 `end_user_not_found` 透传。
- **A6** 消息域行为：Agent 应用阻塞调用返回的 400 `bad_request` 以 `DifyException` 透传（code=bad_request）；`stopResponse` 路径为 `POST /chat-messages/{task_id}/stop`；`suggestedList` 路径为 `GET /messages/{message_id}/suggested`（现状回归）。
- **A7** 会话变量：新增 `GET /conversations/{conversation_id}/variables`（query：user/last_id/limit/variable_name）与 `PUT /conversations/{conversation_id}/variables/{variable_id}`（body value 必填；400 `bad_request` 类型不匹配、404 透传），单测断言方法/路径/参数。
- **A8** 文件下载：新增 `GET /files/{file_id}/preview`（query `as_attachment`），返回文件字节；403 `file_access_denied`、404 `file_not_found` 透传。
- **A9** 语音对齐：`audioToText` 的 Javadoc 与本地校验改为最大 30MB、接受 MIME `audio/mp3|m4a|x-m4a|wav|amr|mpga`（不再宣称 mp4/mpeg/webm）；`textToAudio` 新增 `voice` 参数重载且保留 message_id 优先语义；单元测试覆盖 30MB 边界与 MIME 校验。
- **A10** 反馈列表：新增 `GET /app/feedbacks`（page/limit），与已有 `POST /messages/{id}/feedbacks` 并存。
- **A11** 工作流组：新增按 ID 执行 `POST /workflows/{workflow_id}/run`（阻塞+流式）、运行详情 `GET /workflows/run/{workflow_run_id}`、恢复事件流 `GET /workflow/{workflow_run_id}/events`（query：user 必填、include_state_snapshot、continue_on_pause）；`stopWorkFlow` 路径修正为 `POST /workflows/tasks/{task_id}/stop`（修正现状 `/workflows/task/` 单数错误）；`WorkflowStatus` 补齐 `partial-succeeded`、`paused`；单测断言上述全部路径。
- **A12** 人工介入：新增 `GET /form/human_input/{form_token}` 与 `POST /form/human_input/{form_token}`（body：action/inputs/user 必填），404 与 412 `human_input_form_submitted`/`human_input_form_expired` 作为独立错误码透传，不折叠进 400。
- **A13** 流式事件分派：SSE listener 能按 event 分派 `message`/`message_end`/`agent_message`/`agent_thought`/`tts_message`/`workflow_started`/`node_started`/`node_finished`/`text_chunk`/`reasoning_chunk`/`human_input_required`（含 form_token、workflow_run_id）/`workflow_paused`/`workflow_finished`/`error`/`ping`；HTTP 200 + `error` 事件的业务错误不被当作成功；单测用脚本化事件流驱动。
- **A14** 文档域补齐：新增文档详情（`GET .../documents/{id}`，metadata=all/only/without）、下载（`GET .../download` → 签名 URL）、ZIP 批量下载（`POST .../documents/download-zip`，1–100 个 id）、批量状态（`PATCH .../documents/status/{action}`，action 枚举 4 值）；`updateDocByFile` 新增 `PATCH` 路径方法并把旧 `POST .../update-by-file` 标 `@Deprecated` 保留；索引状态枚举含 waiting/parsing/cleaning/splitting/indexing/completed/error。
- **A15** 分段详情与权限语义：新增 `GET .../segments/{segment_id}`（响应 data+doc_form，404 三态）；403 forbidden（key 范围/未启用 API）与 404 在 dataset 客户端全链路透传，单元测试覆盖。
- **A16** 元数据域：7 个端点全部新增（字段 CRUD、内置字段查询与 enable/disable、批量更新文档元数据），路径、参数与 §4.5 一致，单测断言方法/路径/请求体。
- **A17** 标签域与模型查询：7 个标签端点全部新增（`/datasets/tags` CRUD、binding/unbinding、`GET /datasets/{id}/tags`，unbind 仅暴露 tag_ids 数组）；新增 `GET /workspaces/current/models/model-types/{model_type}`（model_type 枚举 6 值），单测断言方法/路径。
- **A18** 知识流水线：4 个端点全部新增（`POST /datasets/pipeline/file-upload`、`GET .../pipeline/datasource-plugins`、`POST .../pipeline/datasource/nodes/{node_id}/run`（SSE）、`POST .../pipeline/run`（blocking/streaming）），单测断言方法/路径/必填 body 字段。
- **A19** 构建回归：`cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-dify clean test` 全绿；全仓 `./mvnw -o compile`（或等价分层编译）通过；无 Java 9+ API；`fw-plus-dify` 的 pom 不新增运行时依赖；现有公开方法（除 §5 对齐清单外）签名不变。

# 约束与不变量

- JDK 1.8：禁止 Java 9+ API（`List.of`、`Map.copyOf`、`orElseThrow()` 等）。
- 源文件 UTF-8；Tab 缩进；包名 `com.xiongx9527.fw.plus.dify.*` 现有风格。
- 新增能力沿用分层：`routes/*Routes` 定义 `DifyRoute`、`I*Client` + `impl`、参数 `params`、类型 `types`。
- 错误处理沿用 `DifyException`/`DifyClientException`，敏感参数脱敏行为不得回退。
- 路径不对称不得复用错：`GET /workflow/{id}/events`（单数）与 `GET /workflows/run/{id}`（复数）分开定义。
- 现有公开接口除 `specs/dify-sdk/spec.md` §5 对齐清单外保持向后兼容。

# 决策

- D-001：change 隔离方式使用 `current`（当前目录、dev 分支），用户在创建时选择。
- D-002：来源覆盖按「直接链接全读取」执行；get-started 及其 9 个直接链接页面 + 关键端点详情页（40 页）均完整读取。
- D-003（Q1）：范围取 **A——概览目录级补齐**：按 6 个应用类型概览与知识库指南的目录补齐全部缺失接口，实现时以各端点详情页 OpenAPI 核定路径参数；不做 C 的全量字段级建模。
- D-004（Q2）：**严格对齐文档**，允许修改现有实现以符合文档（破坏性调整仅限 `specs/dify-sdk/spec.md` §5 明确列出的 6 项，其余现有方法签名不变）。
- D-005：拆分检测结论——不使用 Supervisor Change，保持单个 Native Change：目标同属一个模块与同一套分层/测试设施，多处共同修改 `routes` 与公共校验，拆分协调成本高于并行收益，且用户未要求并行。
- D-006：Shape 最终确认——用户于 2026-10-08 明确确认目标、范围、关键决定、验收 A1–A19 与非目标（回复「确认，进入 Build」）。

# 待解决问题

（无：Q1/Q2 已由 D-003/D-004 决定，Shape 摘要已于 2026-10-08 获用户明确确认「确认，进入 Build」。）

# 验证预期

- 每轮候选实现由独立只读 Verifier 依据验收矩阵 A1–A19 逐项判断，检查记录需对应当前实现版本。
- 主要检查：`cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-dify clean test`、全仓编译、新增路由与方法的对应关系、JDK 1.8 兼容性、现有测试不回退。
- 需要联网调用真实 Dify 的验证不作为通过条件。
