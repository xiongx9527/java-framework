# Outcome

以当前 `dev` 基线对 Tiny Engine 进行一次可重复的整体验收，覆盖 Core、Spring Boot 自动配置、AI Provider、H2 示例启动、认证租户、应用配置、AI 模型配置、HTTP API 和前端静态资源。输出每项检查的实际结果、失败根因和剩余外部依赖限制，不以单个模块编译成功替代端到端结论。

# Scope

- 在独立 Native worktree 中以当前 `dev` 提交为测试基线。
- 执行 Tiny Engine Core、Boot Tiny Engine Starter、AI Boot AutoConfigure、LangChain4j Adapter/Tiny Engine Provider 和 Tinyflow Starter 的可运行测试。
- 使用 Java 8 与 H2 内存数据库启动 `fw-plus-ai-tiny-engine-sample`，验证默认租户、管理员登录、当前用户、租户列表、应用分页/创建和 AI Model Service/Model 配置接口。
- 验证 Tiny Engine 前端静态资源、AI Chat 请求的权限/配置解析和外部 Provider 失败转换。
- 记录真实 Maven/npm 命令、测试数量、HTTP 状态和未覆盖的外部系统条件。

# Non-goals

- 不连接真实 MySQL、Redis、S3、Qdrant、Dify 或外部 LLM 服务。
- 不把占位 Provider URL 的 502 判定为框架故障；只验证错误被正确收敛为受控上游错误。
- 不修改业务实现来绕过失败；若发现缺陷，另开修复候选或由当前 Native Runtime 返回 Build。
- 不执行 Git 推送、合并或删除用户已有工作区。

# Acceptance examples

- A1：Tiny Engine Core 全量 JUnit 测试完成，0 failures、0 errors。
- A2：Boot Tiny Engine 自动配置和 Starter 测试完成，自动配置条件、Mapper、SQL manifest 与前端资源检查通过。
- A3：AI Boot AutoConfigure 与 LangChain4j Adapter/Tiny Engine Provider 测试完成，0 failures、0 errors。
- A4：使用 H2 严格 `t_tenant.enable NOT NULL` 表结构启动 sample，默认系统租户和管理员初始化成功，应用上下文保持运行。
- A5：sample 的 `/actuator/health`、Tiny Engine 前端入口、管理员登录、`user/me`、租户列表和带分页参数的应用列表接口返回预期成功状态。
- A6：管理员可创建应用，并可创建 AI Model Service 与 AI Model；API Key 持久化不以明文返回。
- A7：带合法管理员 Token 的 Chat 请求能通过应用权限和模型配置解析；占位 Provider 不可达时返回受控 502，而不是权限初始化异常或未捕获 500。
- A8：Tinyflow Starter 前端完成 `npm ci`、`npm test`、`npm run build`，Maven 资源复制和 Java classpath 资源测试通过。
- A9：所有检查结果、失败堆栈、外部依赖限制和未执行项目均形成 Builder handoff，供 Native Verifier 逐项复核。

# Constraints and invariants

- 使用 Java 8；不得引入 Java 9+ API。
- 测试 worktree 基于 `dev`，不把旧 `springboot2` worktree 的历史状态作为当前代码结论。
- H2 仅作为可重复的本地验证数据库；测试结束后不保留数据库状态。
- 真实凭据不得写入仓库；Provider 使用无效占位地址时不得发送真实业务请求。
- 任何失败必须保留原始命令、退出码和根因，不得用 `-DskipTests` 掩盖测试失败；Tinyflow 前端已知的 npm 执行开关只允许用于明确记录的开发期 Maven 复用检查。

# Decisions

- 测试对象确定为当前 `dev`，不恢复旧的 `tinyflow-magicapi-knowledge-base` 绑定作为测试基线。
- 采用独立 worktree `verify-tiny-engine-overall`，避免影响 `dev` 上正在运行的示例服务。
- 端到端验证优先使用 H2 + sample 自带 JWT 认证；外部 Provider 只验证请求是否到达 Provider 边界及错误映射。
- 本 change 只负责整体验收和证据汇总；发现实现缺陷时记录为失败并回到 Build，不在验证过程中静默修改生产代码。

# Open questions

无阻塞问题。用户已确认以当前 `dev` 为基线执行整体测试。

# Verification expectations

- 先运行各层最小/全量 Maven 测试并保存退出码和测试统计。
- 运行 Tinyflow 前端 npm 测试和构建，再运行 Starter Maven 测试/打包。
- 启动 sample 后使用 curl 验证健康检查、静态页面、登录、租户、应用和 AI 配置接口。
- 对 Chat 使用占位 Provider URL，确认权限与配置链路通过后返回可解释的上游错误。
- 结束前执行 `git diff --check`、工作区状态检查和旧 worktree/旧分支基线核对。
