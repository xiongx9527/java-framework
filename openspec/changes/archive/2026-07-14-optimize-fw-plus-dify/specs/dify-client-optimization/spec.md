## ADDED Requirements

### Requirement: Dify 客户端构建配置行为明确
系统 SHALL 为 Dify app、dataset 和 web console 客户端提供明确、一致、可诊断的构建配置行为，包括服务地址、认证凭据、超时、连接池和连接失败重试配置。

#### Scenario: 使用最小必填参数构建客户端
- **WHEN** 调用方使用 server 与 apiKey 构建 app 或 dataset 客户端
- **THEN** 系统 SHALL 创建对应客户端，并应用默认超时、连接池和重试配置

#### Scenario: 必填参数缺失
- **WHEN** 调用方缺少 server、apiKey、用户名或密码等必填参数
- **THEN** 系统 SHALL 抛出包含缺失配置上下文的异常，不得静默创建不可用客户端

#### Scenario: 服务地址边界格式
- **WHEN** 调用方传入末尾带斜杠、包含基础路径或未包含基础路径的 server
- **THEN** 系统 SHALL 使用明确且一致的规则构建最终请求地址，避免重复斜杠或错误拼接 API 路径

### Requirement: Dify 调用异常可诊断
系统 SHALL 在 Dify HTTP 调用失败、Dify 返回错误响应、响应体不是合法 JSON 或网络连接失败时保留足够诊断信息。

#### Scenario: Dify 返回错误响应
- **WHEN** Dify 服务端返回 HTTP 4xx 或 5xx 响应
- **THEN** 系统 SHALL 抛出 Dify 异常并保留 HTTP 状态码、Dify code、message、params 和原始响应体中可获得的信息

#### Scenario: Dify 返回非 JSON 错误响应
- **WHEN** Dify 服务端返回非 JSON 格式错误响应
- **THEN** 系统 SHALL 抛出可诊断异常，并保留 HTTP 状态码与原始响应体

#### Scenario: 网络连接或超时失败
- **WHEN** 请求因连接失败、读取超时或写入超时失败
- **THEN** 系统 SHALL 抛出客户端异常并保留原始异常链，便于调用方定位网络或配置问题

### Requirement: Dify 响应模型解析稳定
系统 SHALL 对 Dify 常见响应模型提供稳定解析行为，尤其是分页响应、布尔操作响应、应用调用响应和动态 JSON 响应。

#### Scenario: 解析分页响应
- **WHEN** Dify 返回包含 page、limit、total、total_pages、has_more 和 data 的分页响应
- **THEN** 系统 SHALL 正确映射分页字段和数据列表

#### Scenario: 解析动态 JSON 响应
- **WHEN** Dify 返回当前客户端尚未建模的 JSON 字段
- **THEN** 系统 SHALL 保留可访问的原始 JSON 信息，不得因未知字段导致调用失败

### Requirement: Magic-API Dify 模块保持兼容
系统 SHALL 保持 Boot 层 Magic-API `dify` 模块的现有入口兼容，并使脚本侧能够继续通过简洁方法创建 Dify 客户端。

#### Scenario: 创建知识库客户端
- **WHEN** Magic-API 脚本调用 `dify.dataset(server, apiKey)`
- **THEN** 系统 SHALL 返回可用于知识库、文档和检索操作的 dataset 客户端

#### Scenario: 创建应用客户端
- **WHEN** Magic-API 脚本调用 `dify.baseApp(server, apiKey)`、`dify.chatApp(server, apiKey)`、`dify.flowApp(server, apiKey)` 或 `dify.completionApp(server, apiKey)`
- **THEN** 系统 SHALL 返回对应类型的 app 客户端，并保持方法名与参数顺序兼容

#### Scenario: 创建 Web 控制台客户端
- **WHEN** Magic-API 脚本调用 `dify.web(server, userName, password)`
- **THEN** 系统 SHALL 返回已完成认证连接或可明确报告认证失败原因的 web console 客户端

### Requirement: Dify 优化具备可验证交付物
系统 SHALL 为 Dify 客户端优化提供足够的验证手段，包括编译验证、核心行为测试、示例或文档中的至少一种可执行或可审查证据。

#### Scenario: 构建验证
- **WHEN** 完成 Dify 客户端优化实现
- **THEN** 系统 SHALL 能在相关 Maven 模块中通过编译验证

#### Scenario: 核心行为验证
- **WHEN** 验证客户端构建、异常处理、分页解析或 Magic-API 模块入口等核心行为
- **THEN** 系统 SHALL 提供测试、示例或文档说明作为验收证据
