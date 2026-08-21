# MQTT 服务端 Dashboard 完整规格

## 目标

`fw-plus-magicapi-plugin-mqtt` 必须直接集成 `dreamlu/mica-mqtt-dashboard` 上游模块化源码，并作为 Magic-API 控制台中的 MQTT 服务端管理工具运行。插件必须保留上游基线全部用户功能，同时适配当前 JVM 内默认和动态 MQTT 服务端的安全选择与管理。

## Requirement: 上游源码与功能完整性

实现必须以上游提交 `2ccff8f7dcf9d3de40a4f21b7fc0be9036ff3e80` 为基线，保留其 Vue/TypeScript 页面、store、认证、路由、工具和样式模块。允许的变化仅限 Magic-API 嵌入、多服务端支持、宿主 API、依赖兼容、资源打包和缺陷修复所需的适配。

### Scenario: 检查源码结构

- **GIVEN** 开发者查看 MQTT 控制台源码
- **WHEN** 定位 Dashboard 实现
- **THEN** 可以识别上游的 Dashboard、Clients、ClientDetail、Monitor、Debug、Login/Layout、stores、auth 和工具模块
- **AND** Dashboard 主功能不再由单个大型 `magic-mqtt-dashboard.vue` 手工近似重写
- **AND** 仓库保留上游提交、来源和 Apache 2.0 许可声明

## Requirement: 完整仪表盘

仪表盘必须显示上游提供的连接、消息和节点统计，并提供最近连接客户端列表和刷新/跳转行为。

### Scenario: 查看仪表盘

- **GIVEN** 用户选中运行中的 MQTT 服务端
- **WHEN** 打开仪表盘
- **THEN** 显示客户端节点、总连接、接受/当前/关闭连接及用户数
- **AND** 显示收发包、收发字节及上游定义的消息统计
- **AND** 显示最近客户端并可进入客户端管理或详情

## Requirement: 完整客户端管理

客户端管理必须保留上游的分页、搜索、状态筛选、刷新、详情和踢出能力。

### Scenario: 查询和管理客户端

- **GIVEN** 所选服务端存在客户端
- **WHEN** 用户搜索、筛选或翻页
- **THEN** 列表按客户端 ID、用户名和连接状态返回对应结果
- **AND** 用户可刷新、打开详情并确认踢出指定客户端
- **AND** 操作完成后列表和统计反映所选服务端的最新状态

## Requirement: 完整客户端详情与订阅管理

客户端详情必须展示上游全部可用连接属性，并允许代理订阅与取消订阅。

### Scenario: 查看详情和修改订阅

- **GIVEN** 用户打开一个客户端详情
- **WHEN** 页面加载
- **THEN** 显示客户端 ID、用户名、状态、IP、端口、keepAlive、协议、WebSocket、SSL、队列大小和连接/创建时间等上游字段
- **AND** 显示当前主题过滤器与 QoS
- **AND** 用户可新增 QoS 0/1/2 订阅或取消已有订阅

## Requirement: 完整状态监控

状态监控必须保留上游实时采样、趋势图、数据保留和清理能力。

### Scenario: 实时监控服务端

- **GIVEN** 用户位于状态监控页面
- **WHEN** 启动实时监控
- **THEN** 按上游周期采集连接、消息和字节统计
- **AND** 以图表展示连接数、收发消息和收发字节趋势
- **AND** 用户可停止监控、配置保留数据量并清空数据

## Requirement: 完整 MQTT 调试

MQTT 调试必须保留上游 WebSocket 连接、订阅、发布、消息展示和筛选功能。

### Scenario: 调试 MQTT 消息

- **GIVEN** 所选服务端提供浏览器可达且协议兼容的 WebSocket 监听端口
- **WHEN** 用户输入调试凭据并连接
- **THEN** 可连接和断开，可订阅和取消订阅主题
- **AND** 可按 QoS 0/1/2 及 retain 标志发布消息
- **AND** 可查看入站和出站消息的主题、内容、方向、QoS、retain 和时间
- **AND** 可按主题/方向过滤或搜索、限制保留消息数量并清空消息

### Scenario: 调试目标不可用

- **GIVEN** 所选服务端没有可用 WebSocket、目标不可达或与页面安全协议冲突
- **WHEN** 用户尝试连接
- **THEN** 页面显示明确失败原因
- **AND** 不切换到其他服务端或任意主机端口

## Requirement: 布局、主题与认证

实现必须保留上游侧栏、页面导航、折叠、亮暗主题、登录和退出能力，以及 Basic 与 OAuth/OIDC-PKCE 两种认证实现。作为 Magic-API 插件嵌入时，还必须支持使用宿主控制台会话，避免默认强制重复登录。

### Scenario: 使用宿主会话

- **GIVEN** 用户已通过 Magic-API 控制台认证并打开 admin-only Dashboard
- **WHEN** 插件采用宿主认证模式
- **THEN** 管理请求通过宿主注入的请求能力发出
- **AND** 用户无需再次登录
- **AND** 宿主拒绝请求时显示明确错误，不通过其他凭据绕过

### Scenario: 使用上游认证模式

- **GIVEN** 部署明确配置 Basic 或 OAuth 模式
- **WHEN** 用户访问 Dashboard
- **THEN** Basic 登录、认证检查、退出及凭据注入行为可用
- **OR** OAuth 模式完成 OIDC discovery、PKCE 跳转、回调校验、换取 token、userinfo 和退出流程

## Requirement: 后端驱动的多服务端选择

服务端列表必须来自后端当前运行的默认 `MqttServer` Bean 和动态 `mqtt-datasource` 实例。浏览器不得输入任意管理目标。

### Scenario: 选择初始服务端

- **GIVEN** 存在一个或多个运行中的 MQTT 服务端
- **WHEN** Dashboard 初始化
- **THEN** 优先恢复当前会话仍有效的最近选择
- **AND** 否则优先默认服务端，再选择稳定排序的第一个动态服务端

### Scenario: 切换服务端

- **GIVEN** 用户正在管理一个服务端
- **WHEN** 选择另一个服务端
- **THEN** 仪表盘、客户端列表/分页/详情/订阅、监控和调试都绑定新服务端
- **AND** 旧服务端进行中的请求结果不会覆盖新状态
- **AND** 旧监控定时器和旧 MQTT 调试连接被停止

### Scenario: 服务端消失

- **GIVEN** 当前服务端停止或被删除
- **WHEN** 页面刷新列表或发起管理请求
- **THEN** 显示不可用或空状态并要求选择仍可用服务端
- **AND** 不静默操作其他服务端，不把陈旧数据展示为成功结果

## Requirement: 进程内管理 API 适配

Dashboard 的上游 `/api/v1/*` 调用必须由插件适配层映射到按 `serverId` 解析的进程内 mica-mqtt 2.3.9 服务端能力，不依赖独立 HTTP 管理端口。

### Scenario: 操作有效服务端

- **GIVEN** 请求携带后端列表中的有效 `serverId`
- **WHEN** 查询统计、客户端、详情、订阅或执行删除/订阅操作
- **THEN** 后端直接调用对应进程内 `MqttServer`
- **AND** 返回结构满足上游页面契约

### Scenario: 拒绝未知服务端

- **GIVEN** `serverId` 未知、停止或已删除
- **WHEN** 后端解析目标
- **THEN** 返回明确客户端错误
- **AND** 不回退到默认或其他动态服务端

## Requirement: 敏感信息边界

服务端列表和管理 API 只返回展示和连接所需的非敏感摘要。数据源密码、令牌、认证头和认证处理器配置不得下发。

### Scenario: 返回服务端摘要

- **GIVEN** 服务端数据源含认证信息
- **WHEN** Dashboard 获取列表
- **THEN** 仅返回稳定 ID、名称、监听地址/端口、WebSocket 能力和运行状态等必要字段
- **AND** 序列化结果不含密码、令牌或认证配置

### Scenario: 保存调试凭据

- **GIVEN** 用户输入 MQTT 调试用户名和密码
- **WHEN** 调试会话运行或结束
- **THEN** 凭据仅保留于当前页面内存
- **AND** 不写入 localStorage、sessionStorage 或管理 API

## Requirement: Magic-API 工具栏与插件打包

Dashboard 必须由 `magic-api.plugin.mqtt.admin.enabled` 控制的 admin-only、可关闭、全屏工具栏加载。源码和依赖必须打入控制台 IIFE 与插件 JAR。

### Scenario: 开启管理入口

- **GIVEN** `magic-api.plugin.mqtt.admin.enabled=true`
- **WHEN** Magic-API 加载 MQTT 插件
- **THEN** 显示 MQTT Dashboard 工具栏
- **AND** 在控制台内打开，不跳转外部页面

### Scenario: 关闭管理入口

- **GIVEN** 管理开关未配置或为 `false`
- **WHEN** 插件加载
- **THEN** 不显示 Dashboard 工具栏
- **AND** 不恢复旧状态栏入口

### Scenario: 仅依赖插件 JAR 运行

- **GIVEN** 应用只依赖发布后的 MQTT 插件 JAR
- **WHEN** 打开 Dashboard
- **THEN** 页面、路由、store、图表、MQTT 客户端、样式、图标和图片资源均可加载
- **AND** 不依赖外部 Dashboard 部署或运行时 CDN

## Requirement: 兼容性

- Java 后端必须兼容 JDK 8、Spring Boot 2.7.18 和 Magic-API 2.2.2。
- 后端 mica-mqtt 固定为 2.3.9。
- 现有 MQTT 数据源、订阅脚本、动态注册和 Magic-Script 行为保持兼容。
- `fw-plus-magicapi-plugin-mqttclient` 不受本变更影响。
