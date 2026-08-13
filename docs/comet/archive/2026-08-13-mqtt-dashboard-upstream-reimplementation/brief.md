# Outcome

在 `fw-plus-magicapi-plugin-mqtt` 中直接引入并适配 `dreamlu/mica-mqtt-dashboard` 上游源码，替换当前手工合并的单文件 Dashboard。最终插件必须在 Magic-API 控制台内提供与上游基线功能等价的完整 MQTT 服务端管理界面，并继续支持当前项目的进程内多 MQTT 服务端选择。

# Scope

- 以上游 Gitee 仓库 `https://gitee.com/dreamlu/mica-mqtt-dashboard` 的提交 `2ccff8f7dcf9d3de40a4f21b7fc0be9036ff3e80` 为源码基线。
- 保留上游 Vue 3 + TypeScript 的页面、状态管理、路由、样式和交互结构，不再把功能重新压缩为单个大型 Vue 组件。
- 完整集成仪表盘、客户端管理、客户端详情、订阅管理、状态监控、MQTT 调试、主题/布局以及 Basic 与 OAuth/OIDC-PKCE 认证能力。
- 将上游 HTTP API 契约适配到 `fw-plus-magicapi-plugin-mqtt` 的进程内服务端管理 API，并按稳定 `serverId` 绑定默认或动态 MQTT 服务端。
- 增加多服务端选择；切换服务端时，所有页面数据、分页、详情、监控采样和 MQTT 调试连接同步隔离与切换。
- 将 Dashboard 及全部运行依赖打入 MQTT 插件控制台 IIFE 与最终 JAR，通过 Magic-API admin-only 工具栏加载。
- 保留并扩展前端、后端和打包测试，验证上游功能清单、多服务端隔离、安全边界及产物完整性。

# Non-goals

- 不修改 `fw-plus-magicapi-plugin-mqttclient`。
- 不升级后端固定的 mica-mqtt 2.3.9、Magic-API 2.2.2、Spring Boot 2.7.18 或 Java 8 基线。
- 不改为外部独立 Dashboard 部署，也不依赖 mica-mqtt 的独立 HTTP 管理端口。
- 不允许浏览器提交任意 MQTT 服务端管理地址绕过后端服务端列表。
- 不在本次变更中重构现有 MQTT 数据源、订阅脚本、动态注册或 Magic-Script API。

# Acceptance examples

- A1：插件源码中保留上游基线的多文件 Vue/TypeScript 应用结构及来源声明，旧的 `magic-mqtt-dashboard.vue` 单文件重写不再承载 Dashboard 主功能。
- A2：用户可使用上游仪表盘查看连接、消息、节点统计和最近客户端，并能刷新数据。
- A3：用户可分页、搜索和筛选客户端，查看完整详情，踢出客户端，并新增或取消客户端订阅。
- A4：用户可启动/停止实时监控，查看连接、消息与字节趋势，配置保留数据量并清空监控数据。
- A5：用户可通过 WebSocket 调试 MQTT：连接/断开、订阅/取消订阅、发布 QoS 0/1/2 与 retain 消息、接收消息、过滤/搜索、限制和清空消息。
- A6：上游的布局、侧栏、页面导航、亮暗主题，以及 Basic、OAuth/OIDC-PKCE 登录与退出能力均保留；嵌入模式同时可使用 Magic-API 宿主会话，不强制重复登录。
- A7：存在多个进程内 MQTT 服务端时，用户可选择管理目标；切换后所有视图、请求、监控和调试状态只属于新服务端，旧轮询和旧连接被停止。
- A8：未知、停止或已删除的 `serverId` 被明确拒绝；界面显示错误或空状态，不静默回退到其他服务端，也不显示陈旧成功数据。
- A9：服务端列表和 Dashboard API 不泄露 MQTT 密码、令牌、认证头或认证处理器配置；调试密码只保留于当前页面内存。
- A10：插件 admin 开关、admin-only 全屏工具栏、IIFE 加载和 JAR 内资源均可验证，运行时不依赖外部 Dashboard 静态部署。
- A11：Java 代码通过 Java 8 编译，后端单元测试、前端功能/构建测试和插件打包检查全部通过。

# Constraints and invariants

- 上游源码基线固定为 `2ccff8f7dcf9d3de40a4f21b7fc0be9036ff3e80`；保留 Apache 2.0 LICENSE/NOTICE 和上游来源信息。
- “直接使用上游源码”指保留其页面与模块化架构；仅允许为 Magic-API 嵌入、宿主请求、路由隔离、多服务端选择、依赖兼容和打包所必需的适配。
- 所有上游用户功能必须可观察地保留；不能以“当前单文件已有近似功能”为完成依据。
- Java 后端严格兼容 JDK 8，不使用 Java 9+ API。
- Dashboard 管理目标只能是当前 JVM 中实际运行且由后端列出的 MQTT 服务端。
- 服务端切换必须终止旧服务端监控轮询和 MQTT 调试连接，防止跨服务端数据污染。
- 调试凭据和管理认证材料不得由服务端数据源摘要下发，不得新增不安全的长期存储。

# Decisions

- 采用新 worktree：`.worktrees/mqtt-dashboard-upstream-reimplementation`，分支 `comet/mqtt-dashboard-upstream-reimplementation`，目标分支 `dev`。
- 直接导入上游源码，不继续维护 1077 行 `magic-mqtt-dashboard.vue` 的手工重写方案。
- 完整功能范围包含上游认证、Dashboard、客户端、详情、订阅、监控、调试、主题和导航；Magic-API 嵌入模式增加宿主会话适配，而不是删除上游认证代码。
- 上游独立 `/api/v1/*` 契约通过插件适配层转向进程内服务端，不请求独立 mica-mqtt HTTP 管理端口。
- 在上游能力之上保留项目特有的多服务端选择和安全边界。

# Open questions

- 无。

# Verification expectations

- 对照上游 README、路由、views、stores 和 auth 模块建立功能清单，逐项映射到自动化测试或可执行检查。
- 运行 MQTT autoconfigure 模块的相关 Maven 单元测试和 Java 8 编译检查。
- 安装锁定的前端依赖，运行类型检查、前端测试和生产 IIFE 构建。
- 检查生成的 IIFE/JAR 包含 Dashboard 运行所需 JS、CSS、图标/图片和 LICENSE/NOTICE。
- 使用独立只读 Verifier 按 A1-A11 逐项验收。
