# Dify SDK 完整目标规格（capability: dify-sdk）

本规格描述 `fw-plus-core/fw-plus-project/fw-plus-dify` 模块在本 change 完成后的完整行为。来源为 Dify 官方 API 文档（快速开始及其直接链接页面，2026-10 读取），范围按 Q1=A「概览目录级补齐」，现有接口按 Q2=B「严格对齐文档」处理。

## 1. 模块与架构约束

- 包根 `com.xiongx9527.fw.plus.dify.*`，JDK 1.8（禁止 Java 9+ API），UTF-8，Tab 缩进。
- 分层不变：`routes/*Routes` 定义 `DifyRoute` 常量；`I*Client` 接口 + `impl/*Impl` 实现；参数对象放 `params`；响应/枚举类型放 `types`。
- 新增能力默认返回 `String`/`JSONObject`（与现有风格一致），仅在现有类型化模式明确存在时新增类型（如分页 `DifyPage`）。
- HTTP 层沿用 `SimpleHttpClient`（okhttp + okhttp-sse），流式沿用 `EventSource`/`EventSourceListener`；不新增运行时依赖。
- 基础 URL：默认 `https://api.dify.ai/v1`，`ApiConfig` 支持自定义自部署地址；所有请求携带 `Authorization: Bearer {apiKey}`。

## 2. 认证与错误语义

- 401 `unauthorized`、403 `forbidden`、404 `not_found`、400 `bad_request` 等非 2xx 响应抛 `DifyException`/`DifyClientException`，保留响应中的 `code`、`message`、`status`，不吞错、不改写。
- 现有 `SimpleHttpClient` 敏感查询参数脱敏行为不得回退。
- 服务端语义（文档事实，SDK 不得掩盖）：限定范围知识库密钥访问路径不含 `dataset_id` 或不含范围内 ID → 403；知识库关闭「访问 API」→ 403；知识库不存在 → 404；流式接口业务错误 HTTP 恒为 200，错误在 SSE `error` 事件中。

## 3. 应用客户端（app 包）

### 3.1 应用信息与配置

| 能力 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 应用基本信息 | `getAppInfo()`（已有） | `GET /info` | 响应含 name/description/tags/mode/author_name |
| 应用参数 | `getAppParameters()`（已有） | `GET /parameters` | — |
| 应用元数据 | `getAppMetaInfo()`（已有） | `GET /meta` | — |
| Web 应用设置 | `getAppWebAppSettings()`（新增） | `GET /site` | 返回 title/chat_color_theme/default_language 等；403 `forbidden` |

### 3.2 消息与会话

| 能力 | 方法 | 路径 | 关键语义 |
|---|---|---|---|
| 发送对话消息 | `sendMessages`/`sendMessagesAsync`（已有） | `POST /chat-messages` | 阻塞+流式；Agent 应用（`agent` 模式）仅支持流式，阻塞返回 400 `bad_request`，该错误须原样透传为 `DifyException` |
| 停止响应 | `stopResponse`（已有） | `POST /chat-messages/{task_id}/stop` | `user` 与发起者不一致时服务端静默成功——Javadoc 注明 |
| 建议问题 | `suggestedList`（已有） | `GET /messages/{message_id}/suggested` | — |
| 会话历史 | `history`（已有） | `GET /messages?conversation_id=…` | 与文档一致，保持 |
| 会话列表 | `conversations`（已有） | `GET /conversations` | — |
| 重命名/删除会话 | 已有 | `PATCH`/`DELETE /conversations/{id}` | — |
| 获取会话变量 | `listConversationVariables`（新增） | `GET /conversations/{conversation_id}/variables` | query：user/last_id/limit/variable_name；响应 data[]+has_more+limit；400 `not_chat_app`/`invalid_param`、404 `not_found` |
| 更新会话变量 | `updateConversationVariable`（新增） | `PUT /conversations/{conversation_id}/variables/{variable_id}` | body：value（必填，须匹配现有类型）、user；400 `bad_request` 类型不匹配、404 `not_found` |
| completion 发送/停止 | 已有 | `POST /completion-messages`、`POST /completion-messages/{task_id}/stop` | 保持 |

### 3.3 文件与终端用户

| 能力 | 方法 | 路径 | 关键语义 |
|---|---|---|---|
| 上传文件 | `uploadFile`（已有） | `POST /files/upload` | — |
| 下载/预览文件 | `downloadFile`（新增） | `GET /files/{file_id}/preview?as_attachment=` | 返回文件字节流；403 `file_access_denied`、404 `file_not_found` |
| 终端用户信息 | `getEndUser`（新增） | `GET /end-users/{end_user_id}` | 响应含 external_user_id/is_anonymous/type 等；404 `end_user_not_found` |

`user` 约定：会话、文件、停止、恢复相关方法必须显式携带 `user` 参数（方法参数或参数对象字段），由调用方保证同一终端用户取值稳定。

### 3.4 语音（按 Q2=B 对齐）

| 能力 | 方法 | 路径 | 对齐点 |
|---|---|---|---|
| 语音转文字 | `audioToText`（已有） | `POST /audio-to-text` | 限制对齐文档：**最大 30MB**；接受 MIME：`audio/mp3`、`audio/m4a`（含 `audio/x-m4a`）、`audio/wav`、`audio/amr`、`audio/mpga`（`audio/mpeg` 不接受）；Javadoc 与本地校验同步更新；413 `audio_too_large`、415 `unsupported_audio_type` |
| 文字转语音 | `textToAudio*`（已有） | `POST /text-to-audio` | 保留 message_id 优先于 text 的现有语义；新增 `voice` 参数（重载方式，向后兼容）；`streaming` 字段为兼容保留、无实际效果（Javadoc 注明） |

### 3.5 反馈与标注

| 能力 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 提交反馈 | `feedbacks`（已有） | `POST /messages/{message_id}/feedbacks` | rating：like/dislike/null |
| 获取反馈列表 | `queryFeedbacks`（新增） | `GET /app/feedbacks` | query：page（默认1）、limit（1–101，默认20）；响应 data[] 含 rating/from_source 等 |
| 标注 CRUD/回复配置 | 已有 6 个方法 | `/apps/annotations*`、`/apps/annotation-reply/*` | 保持现有路径与行为 |

### 3.6 工作流运行（workflow 模式）

| 能力 | 方法 | 路径 | 关键语义 |
|---|---|---|---|
| 执行工作流 | `runBlocking`/`runStreaming`（已有） | `POST /workflows/run` | — |
| 按 ID 执行工作流 | `runByIdBlocking`/`runByIdStreaming`（新增） | `POST /workflows/{workflow_id}/run` | workflow_id 必须是已发布版本（草稿→400）；body：inputs/user/response_mode/files；400 `not_workflow_app`/`bad_request`、404、429 |
| 停止工作流任务 | `stopWorkFlow`（对齐） | `POST /workflows/tasks/{task_id}/stop` | **修正现有路径错误**（现状拼出 `/workflows/task/{id}/stop`，单数 `task`）；body user 必填但**无需与发起者一致** |
| 运行详情 | `getWorkFlowStatus`（已有，路径正确） | `GET /workflows/run/{workflow_run_id}` | inputs 为 JSON 字符串；状态枚举 running/succeeded/failed/stopped/partial-succeeded/paused（`WorkflowStatus` 补齐 `partial-succeeded`、`paused`） |
| 恢复事件流 | `streamWorkflowEvents`（新增） | `GET /workflow/{workflow_run_id}/events` | query：user（必填，**必须与发起者一致**，否则 404）、include_state_snapshot、continue_on_pause；SSE 以 `ping` 开头 |
| 工作流日志 | `getWorkFlowLog`（已有） | `GET /workflows/logs` | — |

注意：`GET /workflow/{id}/events`（单数）与 `GET /workflows/run/{id}`（复数）路径不对称，路由常量分别定义，不得互相复用。

### 3.7 人工介入

| 能力 | 方法 | 路径 | 关键语义 |
|---|---|---|---|
| 获取表单 | `getHumanInputForm`（新增） | `GET /form/human_input/{form_token}` | 响应 form_content/inputs[]/user_actions[]/expiration_time；404、**412** `human_input_form_submitted`、**412** `human_input_form_expired` |
| 提交表单 | `submitHumanInputForm`（新增） | `POST /form/human_input/{form_token}` | body：action（必填，来自 user_actions[].id）、inputs（按 output_variable_name 键）、user（必填）；200 返回 `{}`；400 `invalid_form_data`、404、412 两态 |

412 状态码必须作为独立错误码透传，不折叠进 400。

### 3.8 流式事件

- SSE listener 须能按 `event` 字段分派（HTTP 200 时也可能是业务 `error` 事件）：`message`、`message_end`、`agent_message`、`agent_thought`、`tts_message`、`workflow_started`、`node_started`、`node_finished`、`text_chunk`、`reasoning_chunk`、`human_input_required`（携带 `form_token` 与 `workflow_run_id`）、`workflow_paused`、`workflow_finished`、`error`、`ping`。
- 现有 `DifyEventSource*Listener` 机制保持兼容；新增事件类型不得破坏既有回调。

## 4. 知识库客户端（dataset 包）

### 4.1 密钥范围与权限

- 限定范围密钥：仅能访问路径含 `dataset_id` 且在范围内的端点；`GET /datasets`、`POST /datasets`、标签 CRUD、`POST /datasets/pipeline/file-upload` 等路径不含 `dataset_id` 的端点返回 403——SDK 透传为 `DifyException` code=forbidden，Javadoc 注明需要全量密钥。

### 4.2 知识库 CRUD

| 能力 | 方法 | 路径 | 状态 |
|---|---|---|---|
| 创建知识库 | `insert`（已有 `DATASETS`） | `POST /datasets` | body name 必填（1–40）等；400 `invalid_param`、403、409 `dataset_name_duplicate` |
| 知识库列表 | `listDatasets`（新增） | `GET /datasets` | query：page/limit/keyword/include_all/tag_ids；响应 data[]/has_more/limit/page/total；403 key-scope |
| 知识库详情 | `get`（已有 `DATASETS_INFO`，`GET /datasets/{id}`） | 同左 | 403（未启用 API / 不在范围）、404 |
| 更新/删除/检索 | 已有 | `PATCH`/`DELETE /datasets/{id}`、`POST /datasets/{id}/retrieve` | 保持 |

### 4.3 文档

| 能力 | 方法 | 路径 | 状态 |
|---|---|---|---|
| 文本/文件创建 | 已有 | `POST /datasets/{id}/document/create-by-text|file` | 返回 `batch` |
| 索引状态轮询 | `queryBatchStatus`（已有） | `GET /datasets/{id}/documents/{batch}/indexing-status` | 状态机 waiting→parsing→cleaning→splitting→indexing→completed/error（`BatchStatus` 枚举核对齐全） |
| 文档列表 | 已有 | `GET /datasets/{id}/documents` | 保持 |
| 文档详情 | `getDocument`（新增） | `GET /datasets/{id}/documents/{document_id}` | query `metadata`：all/only/without；400 `invalid_metadata`、403、404 |
| 下载文档 | `downloadDocument`（新增） | `GET /datasets/{id}/documents/{document_id}/download` | 返回签名 URL（`{url}`）；403 四态、404 |
| 批量下载 ZIP | `downloadDocumentsZip`（新增） | `POST /datasets/{id}/documents/download-zip` | body document_ids（1–100）；响应 `application/zip` 二进制 |
| 上传文件更新 | 已有 `updateDocByFile`（对齐为 `PATCH`） | `PATCH /datasets/{id}/documents/{document_id}` | 现有 `POST .../update-by-file` 为已废弃别名——保留可调用但 Javadoc 标注废弃并指向新方法；multipart：file + data |
| 文本更新 | 已有 | `POST .../update-by-text` | 保持 |
| 批量状态更新 | `updateDocumentsStatus`（新增） | `PATCH /datasets/{id}/documents/status/{action}` | action：enable/disable/archive/un_archive；body document_ids；400 `invalid_action` |
| 删除文档 | 已有 | `DELETE .../documents/{document_id}` | — |
| 文件信息 | 已有 `queryFileInfo` | `GET .../documents/{document_id}/upload-file` | — |

### 4.4 分段与子分段

- 已有：列表/新增/删除/更新分段、子分段增删改查（路径与文档一致）。
- 新增：`getSegment` → `GET /datasets/{id}/documents/{doc_id}/segments/{segment_id}`，响应 `{data, doc_form}`；404 覆盖知识库/文档/分段三态。
- 通过 API 创建/更新的子分段 `type` 恒为 `customized`（Javadoc 注明）。

### 4.5 元数据字段（全部新增，前缀 `/datasets/{dataset_id}/metadata`）

| 方法 | 路径 | 说明 |
|---|---|---|
| `createMetadataField` | `POST /datasets/{id}/metadata` | body name（唯一 ≤255）、type（string/number/time）；201；400 `invalid_param` |
| `listMetadataFields` | `GET /datasets/{id}/metadata` | 返回 built_in_field_enabled + doc_metadata[]（含 count） |
| `updateMetadataField` | `PATCH /datasets/{id}/metadata/{metadata_id}` | body name |
| `deleteMetadataField` | `DELETE /datasets/{id}/metadata/{metadata_id}` | 204；破坏性（文档丢失该值） |
| `getBuiltInMetadataFields` | `GET /datasets/{id}/metadata/built-in` | fields：document_name/uploader/upload_date/last_update_date/source |
| `toggleBuiltInMetadataField` | `POST /datasets/{id}/metadata/built-in/{action}` | action：enable/disable；`{result: success}` |
| `batchUpdateDocumentMetadata` | `POST /datasets/{id}/documents/metadata` | body operation_data[]（document_id + metadata_list[]，partial_update 可选）；400 进行中冲突、404 |

### 4.6 标签（全部新增；路径不含 dataset_id → 需全量密钥）

| 方法 | 路径 | 说明 |
|---|---|---|
| `createTag` | `POST /datasets/tags` | name 唯一 1–50；400 重名 |
| `listTags` | `GET /datasets/tags` | — |
| `updateTag` | `PATCH /datasets/tags` | body tag_id + name；404 |
| `deleteTag` | `DELETE /datasets/tags` | body tag_id；204；不删知识库 |
| `bindTags` | `POST /datasets/tags/binding` | body tag_ids[]（未知静默忽略）+ target_id；204；404 |
| `unbindTags` | `POST /datasets/tags/unbinding` | body target_id + tag_ids[]（`tag_id` 单数为废弃别名，SDK 只暴露 tag_ids）；204 |
| `getDatasetTags` | `GET /datasets/{dataset_id}/tags` | 返回 `{data:[{id,name}], total}`；403/404 |

### 4.7 模型查询

- `getAvailableModels(modelType)` → `GET /workspaces/current/models/model-types/{model_type}`；model_type 枚举：text-embedding/rerank/llm/tts/speech2text/moderation。
- 现有 `MODELS_TEXT_EMBEDDING` 常量改为参数化路由（或保留作默认值），行为对齐上述路径。

### 4.8 知识流水线（全部新增）

| 方法 | 路径 | 说明 |
|---|---|---|
| `uploadPipelineFile` | `POST /datasets/pipeline/file-upload` | multipart file；201 返回 `id`；403（需全量密钥）、413、415 |
| `listDatasourcePlugins` | `GET /datasets/{id}/pipeline/datasource-plugins` | query is_published（默认 true）；400 `Pipeline not found` |
| `runDatasourceNode` | `POST /datasets/{id}/pipeline/datasource/nodes/{node_id}/run` | body inputs/datasource_type/is_published/credential_id；SSE 流式响应 |
| `runPipeline` | `POST /datasets/{id}/pipeline/run` | body 六项必填（inputs/datasource_type/datasource_info_list/start_node_id/is_published/response_mode）；blocking JSON 或 streaming SSE |

## 5. 现有接口对齐清单（Q2=B，允许且仅限以下破坏/调整）

1. `stopWorkFlow` 请求路径 `/workflows/task/{id}/stop` → `/workflows/tasks/{task_id}/stop`（修正服务端路径错误；方法签名不变）。
2. `audioToText` 大小限制与格式：15MB → **30MB**；格式集合改为文档 MIME 集合（mp3/m4a/wav/amr/mpga，不接受 mpeg）；Javadoc 与校验同步。
3. `textToAudio` 增加 `voice` 参数重载（新增重载，不删旧签名）。
4. `updateDocByFile`：新增 `PATCH` 路径方法，旧 `POST .../update-by-file` 标记 `@Deprecated` 保留。
5. `WorkflowStatus` 补齐 `partial-succeeded`、`paused` 两个状态值。
6. 其余现有方法签名与返回结构保持不变；发现文档与实现不一致且不在上述清单时，Build 阶段记录到交接说明，由 Verifier 复核是否越权。

## 6. 非目标

- Console 控制台 API（登录、应用列表）不在来源内，保持现状。
- 不接入真实 Dify 实例在线联调；验收以离线单元测试与代码检查为准。
- 不改动 fw-plus-dify 以外模块；不新增运行时依赖；不做破坏 JDK 1.8 约束的实现。
- 知识库指南中「获取 API 端点和密钥」等控制台操作指引只作 Javadoc 说明，不产生 SDK 行为。
