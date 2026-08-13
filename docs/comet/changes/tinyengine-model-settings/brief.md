# Outcome

修复 `fw-plus-tiny-engine-spring-boot-starter` 的模型设置，使 AI 模型服务和模型配置按 TinyEngine 应用共享并持久化到 MySQL。浏览器不再保存、回填或发送模型 API Key；服务端使用 `fw-plus-mybatis-encrypt` 的 `@FieldEncrypt` 透明加解密，并在每次 AI 调用时按应用、服务和模型解析凭据。

# Scope

- 新增应用级模型服务、模型表、MySQL/H2 幂等迁移、实体、Mapper、安全 DTO 和 Core 配置服务。
- `app_owner`、`app_admin` 可管理模型服务和模型；`app_dev` 只能读取并使用已启用模型。
- Servlet MVC 与 WebFlux 提供同构管理/使用 API，并委托同一 Facade 和 Core 服务。
- AI Chat 请求改为提交 `appId`、`serviceId`、`modelName`；服务端解析内部凭据并由真实 LangChain4j provider 调用上游。
- Robot 模型设置改为远端应用配置；旧 `tiny-engine-robot-settings` 模型字段仅首次迁移，成功后清理，失败时保留。
- 删除应用时在同一事务中删除对应模型服务和模型。
- 补齐 Core、Boot、AI、前端和样例端到端测试，并在本机 MySQL 验证密文字段。

# Non-goals

- 不实现租户级或用户级模型配置。
- 不把聊天记录迁移到服务端。
- 不修改业务建模使用的 `t_model` 表。
- 不新增加密算法或改变 `fw.plus.mp-encrypt` 的既有配置契约。
- 不提供 Owner 转让、多个 Owner 或向浏览器返回可逆密钥的能力。
- 不使用用户曾提供的真实 API Key；测试只使用占位凭据。

# Acceptance examples

- A1：同一应用的 Owner 创建模型服务和模型后，Admin 可管理，Dev 可读取并调用；无角色、跨租户用户不可读取或调用。
- A2：Owner/Admin 可新增、修改、删除服务和模型；Dev 的管理请求返回权限不足且不写入数据。
- A3：管理响应只返回 `hasApiKey` 和固定掩码，不包含明文、密文或可逆令牌；可用列表也不包含 API Key。
- A4：模型服务实体的 `apiKey` 使用公共 `@FieldEncrypt`；数据库原始 JDBC 查询为密文，Mapper 业务读取为明文，业务代码不手动加解密。
- A5：API Key 留空表示保留，非空表示替换，`clearApiKey=true` 表示清除；替换和清除同时出现时拒绝请求。
- A6：所有模型配置 SQL 同时约束 `tenant_id` 和 `app_id`，服务 ID 或模型 ID 不能绕过应用隔离。
- A7：MVC 与 WebFlux 暴露同构的可用列表、管理列表、服务/模型增删改和本地导入 API，均委托同一 Facade/Core 服务；WebFlux 使用既有阻塞调度器。
- A8：聊天请求只包含 `appId`、`serviceId`、`modelName` 和生成参数，不包含 `baseUrl`、`apiKey`；旧客户端提交的凭据不得成为正常调用来源。
- A9：服务端调用 `resolveInvocation` 后，真实 LangChain4j provider 使用解析出的 endpoint、API Key 和模型名请求上游；同步和流式路径均覆盖。
- A10：上游 401/403 被转换为脱敏模型调用错误，不清除 TinyEngine `engineToken`，不跳转登录；真正的 TinyEngine 登录失效仍按既有流程处理。
- A11：Owner/Admin 的设置界面显示管理功能；Dev 只显示可用模型选择，不显示密钥输入、创建、编辑或删除动作。
- A12：前端状态、localStorage、聊天请求和普通日志中不保留服务端 API Key；编辑时只显示是否已配置及固定掩码，并支持留空保留和显式清除。
- A13：旧 localStorage 在数据库无配置时只导入一次；数据库已有配置时不覆盖；导入成功才删除模型相关字段，失败时完整保留；聊天设置及未知键不丢失。
- A14：并发旧配置导入最多成功一次，不产生重复服务/模型，也不会用旧快照覆盖已存在的数据库配置。
- A15：删除应用时模型、服务、角色关系和应用删除位于一致事务边界；失败时回滚，成功后两张模型配置表无该应用数据。
- A16：MySQL/H2 迁移可重复执行；服务键、模型名等应用内唯一性成立，主键类型与 BIGINT 一致。
- A17：Java 8 下 Core、Boot、AI 定向与相关回归通过；Robot 定向 Vitest、可收集全量测试和生产 build 通过。
- A18：本机 MySQL 验证配置能跨浏览器/用户按应用共享、原始 API Key 列为密文，应用删除后关联数据清理。

# Constraints and invariants

- Java 实现兼容 Java 8，不使用 Java 9 及以上 API。
- API Key 只在服务端解密和使用，不返回浏览器，不写普通日志、错误消息、测试快照或提交信息。
- 实体字段使用公共 `@FieldEncrypt`，业务逻辑不得手动加解密或二次加密。
- 权限和租户隔离在 Core 服务边界执行，Controller 与前端限制不能替代后端校验。
- 写操作采用明确事务；应用锁与子资源锁顺序保持一致，避免并发导入、更新和删除产生竞态。
- 保持 TinyEngine 登录令牌与模型服务凭据严格分离；非 AI 请求和既有登录行为不回归。
- 保留工作区用户无关改动；所有实现和测试仅在独立 worktree 中完成。

# Decisions

- 使用规范化的 `t_ai_model_service`、`t_ai_model` 两表，而不是在应用表保存 JSON。
- 配置共享边界为应用，不是租户或用户。
- `app_owner`、`app_admin` 管理，`app_dev` 只使用已启用模型。
- API Key 使用 `fw-plus-mybatis-encrypt` 的 `@FieldEncrypt` 持久化。
- 浏览器不再作为凭据真相来源；正常聊天只提交应用、服务、模型标识。
- 复用仓库已有的 2026-08-12 设计与实施计划作为实现依据，并以本 Native 规格和验收项作为最终验收边界。
- 用户于 2026-08-13 明确确认上述目标和范围。

# Open questions

- 无。

# Verification expectations

- 先补失败测试覆盖数据迁移、密文、权限、双栈 API、真实 provider、前端迁移与无凭据请求，再实现修复。
- 使用 Zulu JDK 8 运行 Java 测试；按 Core → Boot → AI → Sample 顺序准备本地 Maven 依赖。
- 准备前端 workspace 依赖后运行 Robot 定向 Vitest、可收集全量测试及生产 build；不得以静态检查替代可运行测试。
- 使用占位 API Key 和本地模拟上游验证 Authorization、endpoint、同步/流式 401/403；不得使用或输出真实 Key。
- 最终在本机 MySQL 3306 的测试应用上验证迁移、密文、共享读取与删除清理，并部署 19080 供用户验收。
