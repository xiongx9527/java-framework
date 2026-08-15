# Tinyflow Dify External 请求限额与审计重构设计

## 背景

当前 `TinyflowDifyExternalController` 直接接收 `@RequestBody String`。Spring MVC 在进入 Controller 前已经读取完整请求体，因此 Controller 内的长度判断无法在分配完整 body 前阻止超限请求。

`TinyflowDifyExternalKnowledgeService` 已限制 TopK、超时、频率和执行队列，但 query 长度、metadata 条件数量及嵌套深度仍需在 Service 边界统一限制，避免 direct `IKnowledgeService` 调用绕过 HTTP 校验。

外部检索审计需要覆盖 HTTP 前置失败和 direct Service 调用，同时不能因 Controller 与 Service 各自记录而重复。审计事件不得包含 Authorization、API Key、完整 query、metadata 值或异常消息。

本设计对应当前 Native change 的以下验收项：

- A37：资源和请求限额；
- A38：审计和脱敏。

## 目标

1. 在 Spring MVC 反序列化前限制外部 `/retrieval` 的原始请求体字节数。
2. 在 Service 边界统一限制 query 长度、metadata 条件数量和嵌套深度。
3. HTTP 和 direct Service 入口均记录一次且仅一次 `external:retrieval` 审计事件。
4. 保持现有认证、scope 和 JSON 错误优先级以及稳定的 Dify External 错误响应。
5. 保持 Java 8、Spring Boot 2.7.18 和现有 `IKnowledgeService` 契约兼容。

## 非目标

- 不迁移或替换现有 Core `TinyflowKnowledgeAuditSink` 模型。
- 不把审计改为异步队列或批量持久化。
- 不重构通用 Magic-API 管理端审计。
- 不改变 Dify External API 的成功响应、错误码、Bearer Key 格式或 Provider 路由。
- 不处理真实 Dify、真实浏览器或最终部署 JAR E2E；这些仍属于已确认的后续独立任务。

## 架构

### 1. 原始请求体预处理层

新增 `TinyflowDifyExternalRequestBodyFilter`，继承 `OncePerRequestFilter`。

职责：

- 只处理当前 context path 下由 `TinyflowDifyExternalProperties.basePath + "/retrieval"` 定义的 POST 路径；
- 在进入 Spring MVC 消息转换前检查 `Content-Length`；
- 使用请求包装器限制实际从 `ServletInputStream` 读取的字节数，覆盖 chunked 或未知长度请求；
- 非目标路径和非 POST 请求直接透传；
- 超限时返回稳定的 HTTP 400、`error_code=1003`、`error_msg="Invalid request"`，不回显请求内容；
- 通过审计协调器记录一次失败事件，tenantId、resourceId 均可为 null。

请求包装器必须按原始字节计数，而不是 Java `String.length()`，因此多字节 UTF-8 内容也受正确限制。包装器不得预读或缓存完整 body；Spring 的 String converter 仍负责正常请求的读取和解码。

Controller 继续使用 `@RequestBody String`，不自行读取 `HttpServletRequest`。这样可以保留现有方法签名和以下顺序：

1. Bearer header 校验；
2. Access Key 认证；
3. 从原始 JSON 预取 `knowledge_id` 并进行 scope 校验；
4. 完整 JSON 解析及业务校验。

### 2. Service 语义限额层

在 `TinyflowDifyExternalProperties` 增加：

- `maxRequestBodyBytes`，默认 65536；
- `maxQueryLength`，默认 4096；
- `maxMetadataConditions`，默认 20；
- `maxMetadataDepth`，默认 8。

所有值必须为正数，沿用 `InitializingBean.afterPropertiesSet()` 启动校验。

`TinyflowDifyExternalKnowledgeService.validate(...)` 是 query 和 metadata 语义限制的唯一归属：

- trim 后的 query 不能为空，且长度不得超过 `maxQueryLength`；
- metadata 条件数量不得超过 `maxMetadataConditions`；
- 每个 metadata value 的容器嵌套深度不得超过 `maxMetadataDepth`；
- 深度遍历对 `Map`、`Collection` 和数组生效，并在超限时立即短路；
- 校验必须发生在 Provider 查找和调用前。

Controller 只校验 JSON 节点的基本形状和数值类型，不重复 query、条件数量或深度规则。这样 direct `IKnowledgeService` 调用与 HTTP 调用使用同一语义边界。

### 3. 审计协调层

新增 `TinyflowDifyExternalAuditCoordinator`。

该组件依赖现有 `MagicTinyflowKnowledgeAuditSink`，集中负责：

- 固定 action 为 `external:retrieval`；
- 创建只包含 tenantId、resourceId、success 和 errorType 的 `AuditEvent`；
- 不接收或保存 Authorization、Access Key、query、metadata value、请求正文或异常消息；
- 实现统一的审计持久化失败策略。

审计失败策略沿用现有 `MagicTinyflowKnowledgeController`：

- 业务已成功但审计写入失败：fail-closed，抛出专用 `AuditPersistenceException`；
- 业务已失败且审计写入失败：保留原始业务异常，将审计异常加入 suppressed；
- 不对 `AuditPersistenceException` 再发起失败审计，避免一次请求产生成功和失败两条事件。

`TinyflowDifyExternalAutoConfiguration` 仅在 `MagicTinyflowKnowledgeAuditSink` 可用时创建外部审计协调器及完整 external endpoint Bean 图。这样不会发布无法满足 A38 的未审计外部接口。现有 Magic Tinyflow 配置继续提供默认的 `MagicResourceTinyflowKnowledgeAuditSink`，业务可用自定义 Bean 覆盖。

## 审计数据流

### HTTP 请求体超限

1. Filter 在 MVC 前发现超限；
2. Coordinator 记录 `success=false`，tenantId/resourceId 为 null，errorType 为安全的限额异常类名；
3. Filter 返回 400/1003；
4. Controller 和 Service 不执行。

### HTTP 前置失败

Controller 保存当前可安全取得的 `accessKey`、`knowledgeId` 和 `delegated` 状态：

- 缺失或非法 Authorization：Controller 通过 Coordinator 记录失败；
- 无效 Key：Controller 通过 Coordinator 记录失败，tenantId 为 null；
- scope 越权或 JSON 解析失败：Controller 使用已认证 AccessKey 和已提取 knowledgeId 记录失败；
- 在调用 Service 前 `delegated=false`；调用 Service 前切换为 true；
- delegated 后的成功、超时、内部错误等由 Service 负责，Controller 不重复记录。

### Direct Service 调用

- `retrieval(String apiKey, KnowledgeArgs args)` 包围认证过程；无效 Key 由 Service 通过 Coordinator 记录一次失败；
- 认证成功后委托 `retrieval(AccessKey, KnowledgeArgs)`；
- 后者包围 validate、限流、资源查找、Provider 调用、超时和结果映射；
- 成功记录一次成功事件；任一业务异常记录一次失败事件并重新抛出；
- `args == null` 时仍审计，resourceId 为 null。

## 错误契约

保持现有响应：

- 缺失或格式非法的 Authorization：403/1001；
- 无效、吊销或越权 Access Key：401/1002；
- 非法或超限请求：400/1003；
- 知识库不存在：404/2001；
- 非本地 Provider：409/2002；
- Provider 不可用：503/2003；
- 频率限制：429/3001；
- 超时：504/3002；
- 队列繁忙或服务关闭：503/3003；
- 未知内部错误：500/5000。

Filter 的超限响应使用与非法请求相同的 400/1003，避免扩大外部错误契约。

错误响应和普通日志不得包含 Authorization、Access Key、query、metadata value、完整正文或底层敏感异常消息。

## 组件与依赖

### `TinyflowDifyExternalRequestBodyFilter`

依赖：

- `TinyflowDifyExternalProperties`；
- `TinyflowDifyExternalAuditCoordinator`。

不依赖 Service 或知识库仓库。

### `TinyflowDifyExternalAuditCoordinator`

依赖：

- `MagicTinyflowKnowledgeAuditSink`。

不依赖 HTTP、Service、Provider 或资源仓库。

### `TinyflowDifyExternalKnowledgeService`

新增依赖：

- `TinyflowDifyExternalAuditCoordinator`。

继续依赖 Provider Registry、知识库仓库、Access Key Service 和 Properties。

### `TinyflowDifyExternalController`

新增依赖：

- `TinyflowDifyExternalAuditCoordinator`。

继续依赖 External Knowledge Service。

### `TinyflowDifyExternalAutoConfiguration`

负责创建和装配：

1. `TinyflowDifyExternalAuditCoordinator`；
2. `TinyflowDifyExternalKnowledgeService`；
3. `TinyflowDifyExternalController`；
4. `TinyflowDifyExternalRequestBodyFilter` 或对应 `FilterRegistrationBean`。

所有 Bean 保持 `@ConditionalOnMissingBean` 覆盖能力。Filter 只映射配置后的 external retrieval 路径，不影响其他接口。

## 测试设计

### Filter 测试

- `Content-Length` 已知且大于限制时，在下游执行前返回 400/1003；
- chunked/未知长度输入超过限制时，在读到第 `limit + 1` 个字节时终止；
- 正好等于限制的 body 可透传；
- 多字节 UTF-8 内容按字节而不是字符计数；
- 非目标路径和非 POST 请求不受影响；
- 超限请求记录一次失败审计，事件不包含正文。

### Service 限额测试

直接调用 `retrieval(AccessKey, KnowledgeArgs)`：

- query 超长时在 Provider 调用前返回 400/1003；
- metadata 条件数超限时在 Provider 调用前返回 400/1003；
- metadata value 嵌套深度超限时在 Provider 调用前返回 400/1003；
- 边界值合法；
- Provider 调用次数保持为零。

### 审计测试

验证以下场景各产生一次且仅一次事件：

- HTTP 成功；
- 缺失或格式非法 Authorization；
- 无效或吊销 Key；
- scope 越权；
- malformed JSON；
- 请求体超限；
- direct Service 无效 Key；
- direct Service 空请求；
- 知识库不存在；
- 超时；
- Provider 内部错误。

每项断言 tenantId、resourceId、action、success 和 errorType，并断言事件字符串及持久化 payload 不包含 token、`Bearer`、query 或敏感异常消息。

审计 sink 故障测试：

- 成功检索后的 sink 失败产生一次 `AuditPersistenceException`，不再记录失败事件；
- 已有业务失败后的 sink 失败保留原业务异常和状态码，并附加一个 suppressed 异常。

### 自动配置测试

- enabled 且所需仓库、Provider、审计 Sink 存在时创建 Coordinator、Service、Controller 和 Filter；
- 缺少审计 Sink 时不发布 external endpoint Bean 图；
- 自定义 Coordinator、Service 或 Filter Bean 可覆盖默认实现；
- basePath 自定义后 Filter 只匹配对应 retrieval 路径。

### 回归检查

至少运行：

- `TinyflowDifyExternalControllerTest`；
- `TinyflowDifyExternalKnowledgeServiceTest`；
- 新增 Filter、Coordinator 和自动配置测试；
- 既有 Tinyflow AI 自动配置定向测试；
- `git diff --check`。

## 完成标准

- A37：body、query、metadata 数量和深度限制均在危险或无界工作前生效，HTTP 与 direct Service 不可绕过；
- A38：所有外部检索结果和主要前置失败均恰好审计一次，事件与错误响应不含凭证和完整敏感正文；
- 现有认证、scope、错误码和 Provider 路由测试继续通过；
- Runtime 检查通过，并由新的只读 Verifier 逐项接受当前 Native 验收列表。
