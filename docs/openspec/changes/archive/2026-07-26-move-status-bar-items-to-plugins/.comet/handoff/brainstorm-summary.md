# Brainstorm Summary

- Change: move-status-bar-items-to-plugins
- Date: 2026-07-25

## 确认的技术方案

- 所有可选插件状态栏入口均由各自前端插件通过 `statusBarItems` 声明，后端不承载状态栏描述。
- Config、Freemarker、DTP、WebSocket、MQTT、Debezium、Vue3、Sa-Token 在现有 console 源码中迁移；LangChain4j 补齐可维护的 console 源码并重新构建 IIFE。
- Druid、Spring Boot Admin、Swagger、Eureka 新增独立的最小前端插件模块；对应后端 `Plugin` 配置只负责提供 `globalName`、`javascriptFilename` 和运行时加载属性。
- `Plugin` 增加通用 `attributes`，插件加载器向前端工厂传入当前 `pluginData`；Admin、Eureka 通过 `attributes.path` 保留动态访问路径。
- 状态栏项目增加前端 `order`，宿主稳定排序并统一插入在线人数之后；S3、Kafka、Pulsar 同步纳入排序。
- 所有现有插件静态 `toolbars` 补充明确的 `order`，宿主只对静态插件 toolbar 稳定排序；不新增 toolbar，不改变动态 `ADD_FOOTER_TOOLBAR` 行为。
- 宿主删除全部 `hasXxxPlugin`、`checkPlugin`、插件专属图标项和 `openXxx`，只保留通用收集、过滤、排序、刷新和渲染。
- 插件加载器发出通用变更事件，状态栏不再依赖“加载完毕/Loaded”文案或定时轮询刷新。

## 关键取舍与风险

- 新增四个前端插件模块会扩大 Boot/Cloud 的构建和资源装配范围，但确保状态栏行为真正归属插件。
- LangChain4j 恢复构建来源，避免直接维护压缩 IIFE；需要验证恢复后的产物仍保留原有数据源功能。
- Admin、Eureka 过去复用 `globalName` 存放路径；迁移为 `attributes.path` 时必须覆盖自定义路径，避免退回默认地址。
- `order` 对旧插件保持可选，缺失或非法值排在已排序项目之后；同序项目保持插件加载顺序。

## 测试策略

- 每个状态栏插件验证 `icon`、`title`、`displayKey`、`order`、`onClick` 和原有跳转语义。
- 宿主验证稳定排序、配置隐藏、通用加载事件、非法项降级、零插件和多插件场景。
- 14 个现有 toolbar 插件验证显式 `order`，并验证 `adminOnly`、内置 toolbar 和动态 toolbar 行为不变。
- Java 测试验证四个新增 IIFE 的加载元数据，以及 Admin/Eureka 的 `attributes.path` 序列化。
- 构建受影响 console，执行 Boot、Cloud、AI 目标 Maven 测试和资源打包、浏览器交互验证、OpenSpec 严格校验及差异检查。

## Spec Patch

- 明确状态栏只允许由前端插件声明，并按 `order` 稳定排序。
- 增加所有现有静态插件 toolbar 声明明确 `order` 的要求。
- 增加 Admin/Eureka 自定义路径通过 `Plugin.attributes` 传入前端插件的场景。
- 增加通用插件变更事件刷新和非法项目静默降级场景。
