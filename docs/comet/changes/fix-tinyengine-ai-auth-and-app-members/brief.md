# Outcome

TinyEngine 已登录用户配置模型后可以在设计器内正常发送消息，模型凭据不会覆盖 TinyEngine 登录凭据，上游模型鉴权失败也不会清除登录状态。应用中心同时提供可发现的成员管理入口，`app_owner` 与 `app_admin` 能按既定规则管理成员，`app_dev` 无入口且无法越权调用接口。

# Scope

- 修复机器人代理请求中 TinyEngine 登录令牌与模型 API Key 共用 `Authorization` 请求头的冲突。
- 启用并验证本机样例的 TinyEngine AI、Web、Chat 与 LangChain4j 模型能力。
- 区分 TinyEngine 登录失效与上游模型鉴权错误，避免发送消息后误跳登录页。
- 保持模型 API Key 在代理请求体中传递；支持现有 `EKEY_` 凭据形式，不把真实 Key 写入仓库、日志或验收产物。
- 在应用中心卡片和表格两种视图的应用操作区增加“成员管理”入口。
- 提供独立成员管理对话框，支持成员列表、添加成员、修改角色、移除成员和失败重试。
- 复用现有 Servlet MVC、WebFlux 和 Core 应用成员 API 及权限边界。
- 构建并安装相关 Core、AI 和 Starter 模块，重启 19080 样例并保留现有 MySQL 数据。

# Non-goals

- 不增加 `app_owner` 转让、分配、修改、删除或多 Owner 能力。
- 不新增应用角色类型，也不改变既有应用列表和非成员操作权限矩阵。
- 不允许浏览器绕过 TinyEngine 后端直接调用上游模型。
- 不在服务端持久化用户输入的真实模型 API Key。
- 不重构机器人插件整体状态管理或应用中心整体布局。
- 不提交构建产物，不提交或回退工作区中与本 change 无关的改动。

# Acceptance examples

- A1：已登录用户发起 Chat Completions 时，`Authorization` 只携带 TinyEngine 登录令牌，模型 API Key 通过请求体独立传递，后端代理调用上游时再使用模型凭据。
- A2：模型 API Key 缺失时，前端显示明确设置提示且不发送 Chat Completions 请求。
- A3：上游模型返回 401/403 或 Key 无效时，页面显示模型调用错误，保留 `engineToken`、当前页面和登录状态。
- A4：TinyEngine 登录令牌缺失、失效或明确返回登录错误码时，前端仍清除登录状态并进入登录页。
- A5：19080 样例启动时 AI Controller 和 Chat Completions 路由已启用；真实登录用户发送“你好”可收到有效模型内容或完整 SSE 结束标记，不跳登录页。
- A6：仓库配置、提交、测试输出和正式验收产物中不保留或新增真实模型 API Key；样例通过运行时外部配置读取凭据。
- A7：应用中心卡片和表格视图中，`app_owner`、`app_admin` 可看到“成员管理”，`app_dev` 不显示该入口。
- A8：成员对话框成功加载并展示成员账号或用户标识、角色；加载失败时保留对话框并可重试，不把业务 403 当作登录失效。
- A9：Owner/Admin 可为当前租户内其他非成员添加 `app_admin` 或 `app_dev`，成功后只刷新当前成员列表。
- A10：同一应用同一用户重复添加、操作者操作自己、分配 `app_owner` 或越权添加时，前后端均阻止操作并显示明确错误，不产生写入。
- A11：Owner/Admin 可把其他非 Owner 成员在 `app_admin` 与 `app_dev` 之间明确变更；不可修改自己或 Owner。
- A12：Owner/Admin 可移除其他 `app_admin`、`app_dev`，不可移除自己或 Owner；成功后只刷新当前成员列表。
- A13：`app_dev`、无应用角色用户和跨租户用户直接调用成员写接口时返回权限不足且不产生部分数据。
- A14：Servlet MVC 与 WebFlux 成员 API 共享 Core 角色管理语义；前端使用现有 API 数据模型，不建立重复权限体系。
- A15：AI、成员入口和既有 TinyEngine 相关定向测试通过，前端生产构建和 Java 8 后端构建成功，部署后完成真实浏览器与 API 验收。

# Constraints and invariants

- 兼容 Java 8、Spring Boot 2.7.18 以及现有 TinyEngine 前端构建链。
- TinyEngine 登录凭据与模型凭据必须保持两个独立安全边界。
- `app_owner` 每应用唯一且不可由成员管理接口操作。
- `app_owner` 与 `app_admin` 可管理成员；操作者不可操作自己；同一应用同一用户只能添加一次。
- 前端可见性与禁用状态只改善交互，Core 服务仍是最终授权边界。
- 当前未提交的 app-role 后端实现是成员入口的前置依赖，必须作为同一交付单元完成验证和提交。
- 当前工作区使用 `dev` 分支和 `current` isolation；必须保留其他用户改动。

# Decisions

- 继续处理 AI 鉴权与应用成员入口，二者放在同一 Native change 中，但实现和提交按两部分拆分。
- 模型 API Key 放入代理请求体，TinyEngine 登录令牌独占浏览器请求的 `Authorization`。
- 上游模型 401/403 属于模型调用失败，不触发 TinyEngine 退出登录；只有 TinyEngine 登录失效才跳登录页。
- 成员管理入口位于应用中心每个应用的操作区，并同时覆盖卡片和表格视图。
- 成员管理使用独立对话框，操作成功后只刷新成员列表。
- Owner/Admin 可管理其他成员；Dev 无入口；Owner、自身、重复成员均受保护。

# Open questions

# Verification expectations

- 后端单元/合同测试覆盖请求凭据分离、AI 路由启用、成员 CRUD、角色权限、Owner/自身/重复保护以及 MVC/WebFlux 一致性。
- 前端组件测试覆盖模型 Key 缺失、上游 401/403 不退出、两种应用视图入口可见性、成员列表及全部成员操作规则。
- 使用 Java 8 构建 Core、AI 与 Starter，执行 TinyEngine 前端生产构建。
- 重新启动 19080，使用真实登录账号、现有 MySQL 数据和已配置模型验证“你好”消息及成员管理界面。
- 验收过程中不得在命令、日志、截图、报告或提交中暴露真实 API Key。
