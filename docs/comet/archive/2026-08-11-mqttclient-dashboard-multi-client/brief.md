# Outcome

在 `fw-plus-magicapi-plugin-mqtt` 中提供一个内嵌于 Magic-API 控制台工具栏的完整 mica-mqtt Dashboard。Dashboard 从后端读取当前应用实际运行的 MQTT 服务端实例，并允许用户在页面中切换当前服务端。

# Scope

- 删除 MQTT 服务端控制台插件现有的 `statusBarItems` 入口及其旧版简单 MQTT Web 客户端。
- 参照 Kafka 插件注册独立 `toolbars` 管理入口，Dashboard 以可关闭、可全屏的 Magic-API 底部工具栏组件打开，不跳转新窗口。
- 工具栏声明 `adminOnly: true`；后端 `plugin()` 读取 `magic-api.plugin.mqtt.admin.enabled` 并设置 `Plugin.adminEnabled`，默认关闭。
- 以 `https://gitee.com/dreamlu/mica-mqtt-dashboard` 的界面与代码为基础接入 Dashboard，并适配项目固定使用的 mica-mqtt 2.3.9 服务端能力。
- Dashboard 的服务端选择项来自后端实际运行的默认 `MqttServer` 与动态 `mqtt-datasource`，不要求用户重复配置管理地址。
- 切换服务端后，页面后续查询和操作只作用于选中的 `MqttServer`。
- 保留上游仪表盘、客户端管理、客户端详情、状态监控和 MQTT 调试能力；管理请求通过 Magic-API 插件注入的请求能力按服务端标识路由到所选进程内服务端。
- Dashboard 不保留上游独立的 Basic/OAuth 登录页或另存一套管理凭据。
- MQTT 调试页锁定到所选服务端的 WebSocket 监听地址；调试用户名和密码由用户按需输入且只保存在当前页面内存中，后端不下发数据源中已配置的密码。
- 为工具栏元数据、admin 开关、Dashboard 组件、后端接口和多服务端切换行为补充自动化验证。

# Non-goals

- 不修改 Magic-API 自维护版本和 mica-mqtt 2.3.9 依赖版本。
- 不改变现有 MQTT 服务端数据源、订阅脚本和 Magic-Script 模块的既有行为。
- 不修改 `fw-plus-magicapi-plugin-mqttclient`，也不把服务端管理能力挂到 MQTT Client 数据源。
- 不保留或新增 MQTT `statusBarItems` 入口，不从状态栏或新窗口打开 Dashboard。
- 不代理或管理其他进程中的远程 mica-mqtt 服务端。
- 不新增 Dashboard 专用用户体系、Basic/OAuth 登录流程或 MQTT 凭据托管能力。
- 不升级或改造 mica-mqtt 2.3.9 自带的独立 HTTP 管理服务。

# Acceptance examples

- A1：`magic-api.plugin.mqtt.admin.enabled=false` 或未配置时，Magic-API 控制台不显示 MQTT Dashboard 工具栏入口；配置为 `true` 时显示入口。
- A2：MQTT 插件不再声明 `statusBarItems`；点击 MQTT Dashboard 工具栏后在控制台内打开可关闭、可全屏的 Dashboard，不打开新窗口。
- A3：Dashboard 加载时展示当前应用实际运行的默认服务端和动态 `mqtt-datasource` 服务端，摘要不包含密码或认证密钥。
- A4：存在多个服务端时，用户可在页面顶部切换；切换后仪表盘、客户端管理、客户端详情、监控和调试都改用新服务端，进行中的轮询或调试连接被停止，旧服务端状态不会串用。
- A5：选定服务端后，可查看统计、客户端列表与详情和监控数据，并可执行踢出客户端、订阅管理与 MQTT 调试操作。
- A6：没有可用服务端、选中实例被删除或服务端操作失败时，页面展示明确状态且不会静默回退到其他服务端。
- A7：Dashboard 组件及相关资源随 MQTT 插件 IIFE 和 jar 打包，仅依赖 Magic-API 控制台提供的插件运行时，不依赖外部 Dashboard 部署。
- A8：Dashboard 不显示独立登录页，管理请求使用宿主注入的请求能力；无权或会话失效时展示明确失败结果。
- A9：MQTT 调试目标由所选服务端决定，页面不允许改为任意主机；数据源中保存的 MQTT 密码不出现在服务端列表、页面初始化数据、浏览器存储或管理 API 响应中。

# Constraints and invariants

- 目标运行环境为 Spring Boot 2.7.18 和 JDK 8，不使用 Java 9+ API。
- Dashboard 必须随 `fw-plus-magicapi-plugin-mqtt` 的控制台 IIFE 和 jar 构建、打包。
- 工具栏 admin 门控必须复用 Kafka 插件约定：前端 `adminOnly: true`，后端通过 `Plugin.setAdminEnabled(...)` 注入 `magic-api.plugin.mqtt.admin.enabled` 的值。
- 后端响应不得向浏览器泄露 MQTT 服务端密码或其他敏感凭据。
- 服务端选择只能引用后端实际运行的实例，浏览器不能提交任意地址触发后端访问。
- 所有 Dashboard 管理请求必须通过宿主注入的 Magic-API 请求能力，不直连各 MQTT 服务端的 HTTP 管理端口。
- MQTT 调试使用浏览器到所选服务端 WebSocket 监听端口的连接；若该端口不可达或与页面安全协议不兼容，页面显示连接失败，不降级为其他服务端。
- 上游基线为 `mica-mqtt-dashboard` 的 `2ccff8f7dcf9d3de40a4f21b7fc0be9036ff3e80`，接入时保留其 Apache 2.0 许可证声明。

# Decisions

- 使用独立 worktree 和分支 `comet/mqttclient-dashboard-multi-client` 开发，目标分支为 `dev`。
- Dashboard 位于 MQTT 服务端插件 `fw-plus-magicapi-plugin-mqtt`，不位于 `fw-plus-magicapi-plugin-mqttclient`。
- 删除 MQTT 插件现有 `statusBarItems` 和旧简单客户端入口，改为参照 Kafka 的独立 `toolbars` 管理组件。
- 工具栏使用 `adminOnly: true`、`allowClose: true`、`fullscreen: true`。
- 后端 `plugin()` 读取 `magic-api.plugin.mqtt.admin.enabled`，默认值为 `false`，并调用 `plugin.setAdminEnabled(...)`。
- Dashboard 的可选项来自默认 `MqttServer` 和本插件动态创建的多个 `mqtt-datasource` 服务端。
- Dashboard 保留上游完整的 mica-mqtt 服务端管理范围，而不是缩减为仅发布/订阅的客户端操作页。
- Dashboard 后端直接适配进程内 `MqttServer`，不新增外部管理 API 地址或代理配置。
- 页面初始选择优先使用仍然可用的当前会话最近选择；没有有效会话选择时优先默认 `MqttServer`，否则使用稳定排序后的第一个动态服务端。
- MQTT 调试的主机与 WebSocket 端口由所选服务端决定；用户名和密码不从数据源配置回填，由用户在当前页面按需输入。

# Open questions

- 无。

# Verification expectations

- 验证 MQTT 插件不再声明 `statusBarItems`，并注册与 Kafka 约定一致的 admin-only、可关闭、可全屏 `toolbars` 元数据。
- 验证 `magic-api.plugin.mqtt.admin.enabled` 缺省/关闭/开启三种配置下的插件元数据与工具栏可见性。
- 验证后端只返回脱敏的服务端摘要，并拒绝不存在的服务端 key。
- 验证 Dashboard 在零个、一个和多个运行中服务端下的加载与切换行为。
- 验证 Dashboard 不包含独立登录流程，也不会把数据源密码写入响应或浏览器存储。
- 验证切换服务端会停止旧轮询和旧调试连接，调试目标不可被改成任意主机。
- 验证前端生产构建、控制台 IIFE 打包、Java 8 编译及相关单元/契约测试。
