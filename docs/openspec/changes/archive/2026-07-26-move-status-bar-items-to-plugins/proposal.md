## Why

`magic-status-bar.vue` 仍通过 13 个 `hasXxxPlugin` 响应式变量、插件名判断和专属点击函数维护可选插件入口，导致宿主持续依赖插件业务细节。Kafka、Pulsar 已证明状态栏项可以由插件通过 `statusBarItems` 自注册，因此其余可选插件也应采用同一边界。

## What Changes

- 将 Config、Freemarker、DTP、LangChain4j、WebSocket、MQTT、Debezium、Vue3、Sa-Token 的状态栏项迁入各自前端插件。
- 为 Druid、Spring Boot Admin、Swagger、Eureka 增加插件自有的最小前端模块和 IIFE，并补齐后端加载元数据。
- 从 `magic-status-bar.vue` 删除全部 `hasXxxPlugin` 状态、插件存在性检查、专属图标项和专属点击函数。
- 保留宿主对 `plugin.statusBarItems` 的通用收集、配置过滤、排序、渲染与事件刷新机制。
- 保持所有入口原有图标、标题、`displayKey`、跳转地址和在线人数后的展示位置。
- 为状态栏项和所有现有静态插件 toolbar 增加明确的前端 `order`，由宿主执行稳定排序。
- 通过通用 `Plugin.attributes` 向前端工厂传递 Admin、Eureka 等运行时路径，不在宿主保留插件名分支。
- 增加插件契约、宿主边界、构建产物与跨层元数据测试。

## Capabilities

### New Capabilities

- `magic-plugin-status-bar-extension`: 规范可选插件状态栏入口的所有权、注册协议、排序、插件 toolbar 排序、宿主边界和兼容行为。

### Modified Capabilities

无。

## Impact

- 影响 Magic-API 宿主控制台的状态栏与插件加载逻辑。
- 影响 Boot 插件 console、AI LangChain4j 插件资源、Cloud Eureka 插件元数据，以及 Druid/Admin/Swagger 等后端插件描述。
- 扩展 `Plugin` 的通用运行时属性和前端工厂参数，但不改变业务接口、数据源格式或脚本侧 API。
- 用户已确认因跨 Boot、Cloud、AI 多模块协调而升级为完整 Comet 流程，并选择在当前 `dev` 分支直接工作。
