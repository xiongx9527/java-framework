---
generated_from_state_version: 8
---

# 验证

## 当前结果

- 结果: **已归档**
- 验证情况: **已完成检查，验证结果已确认**
- 目标周期: 1
- 迭代: 1
- 验证器尝试次数: 1
- 完成时间: 2026-10-08T09:15:20.831Z
- 摘要: 对照 brief A1–A19 与 spec §1–§6 逐项独立复核：路由、接口/实现、Javadoc 语义注记与 12 个测试类断言一致；Runtime 的 56 项模块测试全绿与全仓离线编译通过均核实（报告与当前源码版本对应），pom 仅增 test scope 依赖，接口既有签名无删除。19 项全部通过，建议归档前清理工作区噪音并评估 WORKFLOW_TASK 常量删除的兼容影响。

## 验收

| 编号 | 结果 | 来源 | 验收项 | 原因 |
| --- | --- | --- | --- | --- |
| A1 | passed | brief.md | **A1** 认证与基址：所有经 `SimpleHttpClient` 发出的请求携带 `Authorization: Bearer {apiKey}`；`ApiConfig` 支持默认 `https://api.dify.ai/v1` 与自定义自部署基址；非 2xx 响应抛 `DifyException`/`DifyClientException` 且保留响应 `code`（401 unauthorized、403 forbidden、404 not_found、400 bad_request），敏感查询参数脱敏测试不回退。 | SimpleHttpClient 四类请求均在 apiKey!=null 时加 Authorization: Bearer（requestJson/requestFile/requestSse/requestMultipart）；ApiConfig 默认 https://api.dify.ai/v1 且自定义基址有断言；400/403/404 code 端到端透传有契约测试，DifyExceptionTest 覆盖通用 code 解析（401 同机制）；SimpleHttpClientTest 脱敏 2 项在 Runtime surefire 中通过 |
| A2 | passed | brief.md | **A2** 应用配置四端点：`getAppInfo`/`getAppParameters`/`getAppMetaInfo` 分别对应 `GET /info`、`GET /parameters`、`GET /meta`；新增 `getAppWebAppSettings` 对应 `GET /site`，错误码 403 `forbidden` 透传。 | AppRoutes INFO/PARAMETERS/META_INFO/WEBAPP_SETTINGS 对应 /info /parameters /meta /site；testAppConfigEndpointsCarryBearerAuth 断言四路径与 Bearer，testSiteEndpointForbiddenPassesThrough 断言 403 forbidden 透传 |
| A3 | passed | brief.md | **A3** 知识库列表与详情：新增 `GET /datasets` 方法（query：page/limit/keyword/include_all/tag_ids，响应 data/has_more/limit/page/total）与 `GET /datasets/{dataset_id}` 详情方法（403 未启用 API/不在范围、404 语义透传），两者均被单元测试断言请求方法与路径。 | DATASETS_LIST(GET /datasets) 与 DATASETS_INFO(GET /datasets/{id})；testDatasetListAndDetailEndpoints 断言 GET 方法、路径与 page/limit/keyword/include_all/tag_ids 五参数及 403/404 透传 |
| A4 | passed | brief.md | **A4** user 显式传递：会话、文件、停止、恢复相关方法（含新增的会话变量、文件下载、事件流恢复）签名携带 `user`（方法参数或参数对象字段），单测断言请求中带 user；Javadoc 注明「停止接口 user 不一致时服务端静默成功、恢复事件流 user 不符返回 404、Workflow 停止不检查 user」。 | 会话变量/文件下载 3 参/stopResponse/stopWorkFlow/streamWorkflowEvents 签名均含 user 且单测断言请求带 user；三条 Javadoc 语义注记齐备（停止静默成功、恢复 404、Workflow 停止不检查 user） |
| A5 | passed | brief.md | **A5** 终端用户信息：新增 `GET /end-users/{end_user_id}`，404 `end_user_not_found` 透传。 | GET /end-users/{end_user_id} 路径断言与 404 end_user_not_found 独立错误码透传断言齐全 |
| A6 | passed | brief.md | **A6** 消息域行为：Agent 应用阻塞调用返回的 400 `bad_request` 以 `DifyException` 透传（code=bad_request）；`stopResponse` 路径为 `POST /chat-messages/{task_id}/stop`；`suggestedList` 路径为 `GET /messages/{message_id}/suggested`（现状回归）。 | testMessageDomainRegressionAndAgentBlockingBadRequest 断言阻塞 400 code=bad_request 透传、stopResponse POST /chat-messages/{task_id}/stop、suggestedList GET /messages/{id}/suggested |
| A7 | passed | brief.md | **A7** 会话变量：新增 `GET /conversations/{conversation_id}/variables`（query：user/last_id/limit/variable_name）与 `PUT /conversations/{conversation_id}/variables/{variable_id}`（body value 必填；400 `bad_request` 类型不匹配、404 透传），单测断言方法/路径/参数。 | GET/PUT 会话变量方法/路径及 user/limit/last_id/variable_name query、body value+user 断言，400 bad_request 与 404 独立透传断言齐全 |
| A8 | passed | brief.md | **A8** 文件下载：新增 `GET /files/{file_id}/preview`（query `as_attachment`），返回文件字节；403 `file_access_denied`、404 `file_not_found` 透传。 | GET /files/{file_id}/preview 断言 as_attachment query、返回字节、403 file_access_denied 与 404 file_not_found 透传 |
| A9 | passed | brief.md | **A9** 语音对齐：`audioToText` 的 Javadoc 与本地校验改为最大 30MB、接受 MIME `audio/mp3\|m4a\|x-m4a\|wav\|amr\|mpga`（不再宣称 mp4/mpeg/webm）；`textToAudio` 新增 `voice` 参数重载且保留 message_id 优先语义；单元测试覆盖 30MB 边界与 MIME 校验。 | AudioAlignmentContractTest 覆盖 30MB+1 拒绝、恰好 30MB 接受、mp3/m4a/wav/amr/mpga 与显式 x-m4a 接受、mpeg/mp4/webm/无后缀拒绝；textToAudio voice 重载与 message_id 优先断言，旧签名保留 |
| A10 | passed | brief.md | **A10** 反馈列表：新增 `GET /app/feedbacks`（page/limit），与已有 `POST /messages/{id}/feedbacks` 并存。 | GET /app/feedbacks 方法/路径/page/limit 断言齐全，既有 POST /messages/{id}/feedbacks 并存 |
| A11 | passed | brief.md | **A11** 工作流组：新增按 ID 执行 `POST /workflows/{workflow_id}/run`（阻塞+流式）、运行详情 `GET /workflows/run/{workflow_run_id}`、恢复事件流 `GET /workflow/{workflow_run_id}/events`（query：user 必填、include_state_snapshot、continue_on_pause）；`stopWorkFlow` 路径修正为 `POST /workflows/tasks/{task_id}/stop`（修正现状 `/workflows/task/` 单数错误）；`WorkflowStatus` 补齐 `partial-succeeded`、`paused`；单测断言上述全部路径。 | POST /workflows/{id}/run、GET /workflows/run/{id}、POST /workflows/tasks/{task_id}/stop（复数）、GET /workflows/logs status=partial-succeeded、GET /workflow/{id}/events 三 query 均断言；WorkflowStatus 6 值逐一断言 |
| A12 | passed | brief.md | **A12** 人工介入：新增 `GET /form/human_input/{form_token}` 与 `POST /form/human_input/{form_token}`（body：action/inputs/user 必填），404 与 412 `human_input_form_submitted`/`human_input_form_expired` 作为独立错误码透传，不折叠进 400。 | GET/POST /form/human_input/{form_token} 与 body action/inputs/user 必填断言；404、412 human_input_form_expired、412 human_input_form_submitted 三个独立错误态分别断言，未折叠进 400 |
| A13 | passed | brief.md | **A13** 流式事件分派：SSE listener 能按 event 分派 `message`/`message_end`/`agent_message`/`agent_thought`/`tts_message`/`workflow_started`/`node_started`/`node_finished`/`text_chunk`/`reasoning_chunk`/`human_input_required`（含 form_token、workflow_run_id）/`workflow_paused`/`workflow_finished`/`error`/`ping`；HTTP 200 + `error` 事件的业务错误不被当作成功；单测用脚本化事件流驱动。 | 脚本化 SSE 流驱动 15 种事件全部按 type 分派到达，human_input_required 含 form_token/workflow_run_id，HTTP 200 下 error 作为事件分派；DifyChatEvent 15 常量另有专测断言 |
| A14 | passed | brief.md | **A14** 文档域补齐：新增文档详情（`GET .../documents/{id}`，metadata=all/only/without）、下载（`GET .../download` → 签名 URL）、ZIP 批量下载（`POST .../documents/download-zip`，1–100 个 id）、批量状态（`PATCH .../documents/status/{action}`，action 枚举 4 值）；`updateDocByFile` 新增 `PATCH` 路径方法并把旧 `POST .../update-by-file` 标 `@Deprecated` 保留；索引状态枚举含 waiting/parsing/cleaning/splitting/indexing/completed/error。 | 文档详情 metadata query、下载签名 URL、ZIP document_ids、PATCH status/enable、replaceDocByFile PATCH multipart 且 data 摘除 name/text、旧 POST update-by-file 保留并 @Deprecated、IndexingStatus 7 值断言齐全 |
| A15 | passed | brief.md | **A15** 分段详情与权限语义：新增 `GET .../segments/{segment_id}`（响应 data+doc_form，404 三态）；403 forbidden（key 范围/未启用 API）与 404 在 dataset 客户端全链路透传，单元测试覆盖。 | GET .../segments/{segment_id} 路径与响应 data+doc_form 断言；403 forbidden 与 404 独立透传断言（A3 测试同时覆盖 dataset 403/404） |
| A16 | passed | brief.md | **A16** 元数据域：7 个端点全部新增（字段 CRUD、内置字段查询与 enable/disable、批量更新文档元数据），路径、参数与 §4.5 一致，单测断言方法/路径/请求体。 | testMetadataDomainEndpoints 逐项断言 7 个元数据端点的方法/路径/请求体 |
| A17 | passed | brief.md | **A17** 标签域与模型查询：7 个标签端点全部新增（`/datasets/tags` CRUD、binding/unbinding、`GET /datasets/{id}/tags`，unbind 仅暴露 tag_ids 数组）；新增 `GET /workspaces/current/models/model-types/{model_type}`（model_type 枚举 6 值），单测断言方法/路径。 | testTagDomainAndModelQueryEndpoints 断言 7 个标签端点（unbinding body 无 tag_id 单数字段）与 GET /workspaces/current/models/model-types/llm |
| A18 | passed | brief.md | **A18** 知识流水线：4 个端点全部新增（`POST /datasets/pipeline/file-upload`、`GET .../pipeline/datasource-plugins`、`POST .../pipeline/datasource/nodes/{node_id}/run`（SSE）、`POST .../pipeline/run`（blocking/streaming）），单测断言方法/路径/必填 body 字段。 | 流水线 4 端点断言：file-upload multipart、datasource-plugins+is_published、节点 run SSE 与 body 必填字段、pipeline/run 以 runPipelineBlocking/Streaming 分别强制 response_mode——brief A18 原文即（blocking/streaming），按 brief 判可接受 |
| A19 | passed | brief.md | **A19** 构建回归：`cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-dify clean test` 全绿；全仓 `./mvnw -o compile`（或等价分层编译）通过；无 Java 9+ API；`fw-plus-dify` 的 pom 不新增运行时依赖；现有公开方法（除 §5 对齐清单外）签名不变。 | Runtime 权威证据：dify-module-tests（16:53:23 surefire-reports 12 类 56 项 Failures/Errors 均 0，且 src 无晚于报告的文件）与 full-repo-offline-compile（./mvnw -o compile EXIT 0）；grep 无 Java 9+ API；pom 仅增 spring-context scope=test 非运行时依赖；接口既有方法声明无删除，破坏性调整仅 §5 六项 |

## 检查

| 检查 | 命令 | 工作目录 | 状态 | 退出码 | 耗时 |
| --- | --- | --- | --- | ---: | ---: |
| fw-plus-dify 模块单元测试（POJO 56 项） | -pl fw-plus-project/fw-plus-dify test | fw-plus-core | passed | 0 | 9373 ms |
| 全仓离线编译回归（A19） | -o compile | . | passed | 0 | 64761 ms |

### Builder 报告的证据

以下为 Builder 报告，不等同于 Runtime 检查凭据或独立验收结果。

- 模块单元测试（POJO 56 项，含脱敏回归）: passed — cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-dify test → Tests run: 56, Failures: 0, Errors: 0, BUILD SUCCESS
- 全仓离线编译回归: passed — 根目录 ./mvnw -o compile → BUILD SUCCESS（core/boot/cloud/ai/sample 全部模块）
- JDK 1.8 API 扫描（禁 Java 9+ API）: passed — grep 扫描 fw-plus-dify main+test：无 List.of/Map.copyOf/orElseThrow/isBlank/strip/repeat/Files.readString 等匹配
- 依赖约束检查（不新增运行时依赖）: passed — pom 仅新增 org.springframework:spring-context <scope>test</scope>（ParamMessage.toMap→SpringUtils 需加载 ApplicationContextAware，仅测试期）
- 凭据脱敏回归: passed — SimpleHttpClientTest 断言异常/日志不含 api-key、password、Bearer 值，测试通过
- spec §5 破坏性变更范围检查: passed — 破坏性调整仅限 §5 六项；其余既有公开签名不变（list(5参)→listDatasets、getSegmentDetail→getSegment 为本轮内新增后按 §4.2/§4.4 定名）
- 只读代码复核（独立复核执行）: passed — 对照 brief A1–A19 与 spec §1–§6 逐项核对路由/方法/Bearer 覆盖/错误透传，无阻塞问题
- 已知限制: spec §4.8 的 runPipeline 以 runPipelineBlocking/runPipelineStreaming 两个方法实现（response_mode 分别强制 blocking/streaming），方法名与 §4.8 字面命名不同，行为语义一致
- 已知限制: AppRoutes.WORKFLOW_TASK 公开常量随 §5.1 路径修正移除（全仓 grep 无引用）；未列入 §5 字面清单，属路径修正的载体变更
- 已知限制: getWorkFlowLog 的 status 查询值由 status.name() 改为 status.getValue()（旧三值取值相同，§5.5 枚举补齐的延伸，无行为回退）
- 已知限制: pom 新增 test scope spring-context，非运行时依赖；根因是 fw-plus-common 的 SpringUtils 为 optional 依赖，AbstractTrace.get() 只 catch Exception 不捕获 NoClassDefFoundError
- 已知限制: ApiConfig 未显式设置 server 时默认 https://api.dify.ai/v1（此前为 null 会在 buildUrl 时 NPE），属 §1 默认基址的行为增强
- 已知限制: downloadFile(fileId, asAttachment) 2 参便捷重载不携带 user；携带 user 的 3 参重载 downloadFile(fileId, user, asAttachment) 已新增并有测试断言
- 已知限制: 会话变量 GET 的 user 在官方 OpenAPI 中为非必填参数，Javadoc 按官方标注为可选（null 时省略 query）

## 阻塞项

_无。_

## 风险与跳过的工作

- AppRoutes.WORKFLOW_TASK 公开常量随 §5.1 路径修正被删除，未列入 §5 字面清单（属路径修正载体变更，全仓无引用，已在 known_limits 记录）
- fw-plus-dify pom 新增 spring-context 为 test scope（非运行时，符合 A19 要求），但单测自此依赖 Spring 类加载，根因为 fw-plus-common SpringUtils 为 optional 依赖
- A1 中 401 unauthorized 无端到端用例（400/403/404 均有端到端断言，401 走同一 DifyException 通用 code 解析，DifyExceptionTest 覆盖）
- runPipeline 按 spec §4.8 字面为方法名差异（实现为 runPipelineBlocking/Streaming），brief A18 措辞可容纳，按 brief 判通过
- 工作区 git 索引存在 fw-plus-sample 下 .DS_Store 新增与日志文件删除，疑为环境噪音、非本 change 模块，建议归档前清理
- downloadFile 2 参便捷重载不携带 user（携带 user 的 3 参重载已实现并有断言），A4 括注可满足，但两重载行为不一致易误用

## 之前的迭代

| 目标周期 | 迭代 | 尝试 | 结果 | 未解决项 | 摘要 | 完成时间 |
| ---: | ---: | ---: | --- | --- | --- | --- |
| 1 | 1 | 1 | pass | — | 对照 brief A1–A19 与 spec §1–§6 逐项独立复核：路由、接口/实现、Javadoc 语义注记与 12 个测试类断言一致；Runtime 的 56 项模块测试全绿与全仓离线编译通过均核实（报告与当前源码版本对应），pom 仅增 test scope 依赖，接口既有签名无删除。19 项全部通过，建议归档前清理工作区噪音并评估 WORKFLOW_TASK 常量删除的兼容影响。 | 2026-10-08T09:15:20.831Z |



## 结论

对照 brief A1–A19 与 spec §1–§6 逐项独立复核：路由、接口/实现、Javadoc 语义注记与 12 个测试类断言一致；Runtime 的 56 项模块测试全绿与全仓离线编译通过均核实（报告与当前源码版本对应），pom 仅增 test scope 依赖，接口既有签名无删除。19 项全部通过，建议归档前清理工作区噪音并评估 WORKFLOW_TASK 常量删除的兼容影响。
