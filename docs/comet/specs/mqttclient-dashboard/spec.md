# MQTT 服务端 Dashboard 能力规格

## 目标

`fw-plus-magicapi-plugin-mqtt` 必须内置基于 `mica-mqtt-dashboard` 的完整服务端管理界面。该界面作为 Magic-API 控制台的 admin-only 工具栏组件打开，从插件后端读取当前应用实际运行的 MQTT 服务端，并允许用户切换当前管理目标。

## Requirement: Admin-only 工具栏入口

MQTT 服务端控制台插件必须参照 Kafka 插件注册独立 `toolbars` 管理入口。工具栏必须声明 `adminOnly: true`、`allowClose: true` 和 `fullscreen: true`，并由 `magic-api.plugin.mqtt.admin.enabled` 控制是否可见；该配置默认关闭。

#### Scenario: 开启 MQTT 管理工具栏

- **GIVEN** `magic-api.plugin.mqtt.admin.enabled=true`
- **WHEN** Magic-API 控制台加载 MQTT 服务端插件
- **THEN** 控制台显示 MQTT Dashboard 工具栏入口
- **AND** 点击入口后在控制台内打开可关闭、可全屏的 Dashboard 组件
- **AND** 不打开新窗口

#### Scenario: 未开启 MQTT 管理工具栏

- **GIVEN** `magic-api.plugin.mqtt.admin.enabled` 未配置或为 `false`
- **WHEN** Magic-API 控制台加载 MQTT 服务端插件
- **THEN** 控制台不显示 MQTT Dashboard 工具栏入口

#### Scenario: 插件传递 admin 开关

- **GIVEN** 应用启动并创建 MQTT 插件元数据
- **WHEN** `MagicAPIMqttConfiguration.plugin()` 读取配置
- **THEN** 它读取 `magic-api.plugin.mqtt.admin.enabled`，缺省值为 `false`
- **AND** 它通过 `Plugin.setAdminEnabled(...)` 把解析后的布尔值提供给控制台

## Requirement: 删除状态栏入口

MQTT 服务端控制台插件不得再声明 `statusBarItems`，也不得保留从状态栏打开旧简单 MQTT Web 客户端或 Dashboard 的行为。

#### Scenario: 加载 MQTT 插件扩展

- **GIVEN** Magic-API 控制台加载 MQTT 服务端插件
- **WHEN** 插件返回扩展声明
- **THEN** 返回值包含 MQTT Dashboard 的 `toolbars` 声明
- **AND** 返回值不包含 `statusBarItems`

## Requirement: 完整 Dashboard 组件

Dashboard 必须保留上游的仪表盘、客户端管理、客户端详情、状态监控和 MQTT 调试能力，并在 Magic-API 工具栏可用空间内针对当前选中的进程内 mica-mqtt 服务端提供对应功能。

#### Scenario: 使用完整管理能力

- **GIVEN** 用户选中了运行中的 MQTT 服务端
- **WHEN** 用户访问 Dashboard 各管理视图
- **THEN** 可以查看服务端统计、客户端列表与详情和监控数据
- **AND** 可以执行踢出客户端、订阅管理和 MQTT 调试操作

## Requirement: 使用宿主插件运行时

Dashboard 不得保留上游独立的 Basic/OAuth 登录页或维护另一套 Dashboard 管理凭据。其管理请求必须使用 Magic-API 控制台注入给 MQTT 插件的请求能力。

#### Scenario: 工具栏组件请求管理数据

- **GIVEN** MQTT Dashboard 已在 Magic-API 控制台中打开
- **WHEN** Dashboard 请求服务端列表或管理能力
- **THEN** 请求通过插件运行时注入的宿主请求能力发出
- **AND** Dashboard 不要求再次登录，也不自行维护 Basic/OAuth 凭据

#### Scenario: 宿主请求被拒绝

- **GIVEN** 宿主会话失效或请求没有访问权限
- **WHEN** Dashboard 发出管理请求
- **THEN** Dashboard 展示宿主返回的明确失败结果
- **AND** 不以独立凭据绕过该结果

## Requirement: 后端驱动的多服务端选择

Dashboard 的可选目标必须来自后端当前实际运行的服务端集合，包括可选的默认 `MqttServer` Bean 和动态 `mqtt-datasource` 创建的服务端。浏览器不得提供任意目标地址绕过该集合。

#### Scenario: 切换服务端

- **GIVEN** 后端存在多个运行中的 MQTT 服务端
- **WHEN** 用户从 Dashboard 顶部选择另一个服务端
- **THEN** 仪表盘、客户端管理、客户端详情、监控和调试均切换到新服务端
- **AND** 旧服务端的详情、分页、监控采样和调试状态不会显示为新服务端的数据
- **AND** 旧服务端的进行中监控轮询和 MQTT 调试连接被停止

#### Scenario: 选择初始服务端

- **GIVEN** Dashboard 加载时存在一个或多个运行中的 MQTT 服务端
- **WHEN** 页面确定初始管理目标
- **THEN** 优先恢复当前浏览器会话中仍然可用的最近选择
- **AND** 没有有效会话选择时优先选择默认 `MqttServer`，否则选择稳定排序后的第一个动态服务端

#### Scenario: 服务端集合发生变化

- **GIVEN** 当前选中的动态服务端已被删除或停止
- **WHEN** Dashboard 重新加载服务端列表或发起请求
- **THEN** 页面要求用户选择仍然可用的服务端或展示空状态
- **AND** 不会静默操作其他服务端

## Requirement: 进程内服务端管理 API

插件后端必须以 Dashboard 所需的稳定 HTTP 契约适配项目固定的 mica-mqtt 2.3.9 进程内服务端能力。每次请求按服务端标识解析默认或动态 `MqttServer`，不依赖外部 mica-mqtt HTTP 管理服务，也不接受浏览器提交上游 URL。

#### Scenario: 拒绝未知服务端

- **GIVEN** 请求携带不存在、已删除或未运行的服务端标识
- **WHEN** 后端解析管理目标
- **THEN** 请求以明确的客户端错误结束
- **AND** 不操作默认服务端或其他动态服务端

#### Scenario: 操作进程内服务端

- **GIVEN** 请求携带当前列表中有效的服务端标识
- **WHEN** Dashboard 查询统计、客户端信息或执行管理操作
- **THEN** 插件后端直接调用对应的进程内 `MqttServer` 能力
- **AND** 不访问该实例的独立 HTTP 管理端口

## Requirement: 敏感信息边界

提供给 Dashboard 的服务端摘要不得包含 MQTT 密码、认证处理器配置、令牌或其他认证密钥。

#### Scenario: 获取服务端列表

- **GIVEN** MQTT 服务端数据源包含用户名、密码和扩展配置
- **WHEN** Dashboard 获取可选服务端列表
- **THEN** 响应只包含选择和展示所需的稳定标识、名称、监听信息及运行状态
- **AND** 响应序列化结果不包含密码、令牌或认证头

## Requirement: 绑定所选服务端的 MQTT 调试

MQTT 调试必须使用当前所选服务端的 WebSocket 监听地址，且不得把数据源中保存的 MQTT 密码下发给浏览器。调试凭据由用户按需输入并仅保留在当前页面内存中。

#### Scenario: 连接当前服务端进行调试

- **GIVEN** 所选服务端提供浏览器可达且协议兼容的 MQTT WebSocket 监听端口
- **WHEN** 用户输入必要的调试用户名和密码并发起连接
- **THEN** 调试客户端连接到所选服务端并支持订阅、取消订阅、发布和接收消息
- **AND** 主机及端口不可被改为列表外的任意目标
- **AND** 用户输入的密码不写入 localStorage、sessionStorage 或管理 API

#### Scenario: 调试端口不可达

- **GIVEN** 所选服务端的 MQTT WebSocket 端口对浏览器不可达或与当前页面安全协议不兼容
- **WHEN** 用户发起调试连接
- **THEN** 页面显示该服务端的连接失败状态
- **AND** 不静默切换到其他服务端或回退为任意地址

## Requirement: 插件打包

Dashboard 组件及其依赖资源必须随 `fw-plus-magicapi-plugin-mqtt` 的控制台 IIFE 和 jar 打包，并通过 Magic-API 插件运行时加载。

#### Scenario: 从插件产物加载

- **GIVEN** 应用只依赖已发布的 MQTT 服务端插件 jar
- **WHEN** 管理开关开启且用户打开 MQTT Dashboard 工具栏
- **THEN** Dashboard 组件和资源正常加载
- **AND** 不依赖外部 Dashboard 部署或独立静态页面

## Requirement: 失败与空状态

没有可用服务端、选中实例被删除、服务端停止或管理操作失败时，Dashboard 必须显示可区分的错误或空状态，并保留切换到其他运行中服务端的能力。

#### Scenario: 当前服务端不可用

- **GIVEN** 当前服务端已停止或被删除
- **WHEN** 页面加载管理数据或执行管理操作
- **THEN** 页面显示当前服务端的失败状态
- **AND** 不展示其他服务端的陈旧数据作为成功结果

## 兼容性约束

- Java 后端保持 JDK 8 兼容，并运行于 Spring Boot 2.7.18。
- 不升级项目固定的 mica-mqtt 2.3.9 或 Magic-API 2.2.2 依赖。
- 现有 MQTT 服务端数据源、订阅脚本、动态注册与 Magic-Script 接口保持兼容。
- `fw-plus-magicapi-plugin-mqttclient` 及其数据源行为不在本能力范围内。
- 不新增 Dashboard 独立用户体系、Basic/OAuth 登录流程或 MQTT 凭据托管能力。
- 上游基线固定为提交 `2ccff8f7dcf9d3de40a4f21b7fc0be9036ff3e80`，分发时保留 Apache 2.0 许可证声明。
