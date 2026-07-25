## 1. 通用扩展契约

- [x] 1.1 实现 `statusBarItems` 与静态插件 `toolbars` 的稳定 `order` 语义、通用插件变更事件和边界测试。
- [x] 1.2 为 `Plugin` 增加通用 `attributes`，并向 IIFE 工厂传入当前 `pluginData`。

## 2. 前端插件迁移

- [ ] 2.1 将 Config、Freemarker、DTP、WebSocket、MQTT、Debezium、Vue3、Sa-Token 的状态栏入口迁入各自 Boot 插件 console，并验证构建产物。
- [ ] 2.2 将 LangChain4j 状态栏入口迁入 AI 插件前端资源，恢复可维护的构建来源并验证产物。

## 3. 新增前端插件

- [ ] 3.1 为 Druid、Spring Boot Admin、Swagger、Eureka 创建独立最小前端插件、IIFE 资源和后端加载配置，覆盖 Admin/Eureka 自定义路径。

## 4. 宿主解耦

- [ ] 4.1 删除 `magic-status-bar.vue` 中全部 `hasXxxPlugin`、`checkPlugin`、专属图标项、点击函数、文案判断和轮询，只保留通用插件状态栏机制及边界测试。

## 5. Toolbar 排序

- [ ] 5.1 为所有现有静态插件 toolbar 补充明确 `order`，宿主稳定排序且不改变内置、动态 toolbar 和 `adminOnly` 行为。

## 6. 集成验证

- [ ] 6.1 运行受影响插件、宿主、Boot/Cloud/AI 构建，检查 IIFE 与 Maven 资源产物，完成浏览器交互、OpenSpec 严格验证和差异检查。
