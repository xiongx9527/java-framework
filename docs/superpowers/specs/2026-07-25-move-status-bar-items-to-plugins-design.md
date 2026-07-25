---
comet_change: move-status-bar-items-to-plugins
role: technical-design
canonical_spec: openspec
---

# Magic-API 插件状态栏与 Toolbar 排序技术设计

## 1. 背景与目标

宿主 `magic-status-bar.vue` 已能收集 `constants.PLUGINS` 中的 `statusBarItems`，但仍维护 13 组插件专属状态、资源类型检测、图标和点击函数。该实现使宿主知道 Config、Freemarker、DTP、LangChain4j、WebSocket、MQTT、Debezium、Druid、Swagger、Vue3、Sa-Token、Admin、Eureka 的业务细节。

本变更将这些入口完整迁入各自前端插件，并把 S3、Kafka、Pulsar 纳入同一排序契约。同时为所有现有静态插件 toolbar 增加稳定排序。宿主最终只维护通用扩展机制。

不新增 toolbar，不改变业务页面、后端管理接口、权限模型、内置 toolbar 或动态 `ADD_FOOTER_TOOLBAR` 行为。

## 2. 最终边界

### 2.1 前端插件负责

- 声明自身 `statusBarItems`。
- 提供自身 SVG symbol、标题、`displayKey`、`order` 和点击函数。
- 计算自身固定外链或站内管理地址。
- 为已有静态 `toolbars` 声明明确的 `order`。
- 对需要运行时路径的插件，从工厂参数 `pluginData.attributes` 读取配置。

### 2.2 后端插件配置负责

- 返回插件 `name`、IIFE `globalName` 和 `javascriptFilename`。
- 通过通用 `attributes` 传递前端运行所需的非行为数据。
- 确保声明的 IIFE 资源可从 `magic-editor/plugins/` 提供。

后端不得声明状态栏图标、标题、顺序或点击动作。

### 2.3 宿主负责

- 加载 IIFE，调用插件工厂并保存返回对象。
- 发出通用插件集合变更事件。
- 校验、收集、过滤并稳定排序 `statusBarItems`。
- 过滤并稳定排序静态插件 `toolbars`。
- 渲染插件提供的函数和组件，不解析插件名称或资源类型。

## 3. 前端扩展契约

### 3.1 状态栏项

```js
{
  icon: '#magic-plugin-symbol',
  title: 'Plugin title',
  displayKey: 'plugin-key',
  order: 100,
  onClick: () => window.open(url, '_blank')
}
```

字段约束：

- `icon`、`title`、`displayKey` 必须是非空字符串。
- `onClick` 必须是函数。
- `order` 应为有限数值；缺失或非法值按未排序项目处理。
- `displayKey` 继续映射 `config.header[displayKey]`，值为 `false` 时隐藏。

宿主不捕获并改写点击动作。IIFE 是受信任的应用代码，点击逻辑与插件其他组件具有相同信任边界。

### 3.2 静态插件 Toolbar

现有 toolbar 对象增加：

```js
{
  id: 'plugin-toolbar',
  order: 100,
  type,
  icon,
  title,
  component,
  adminOnly,
  fullscreen,
  allowClose
}
```

`order` 只决定静态插件 toolbar 之间的顺序。内置 toolbar 数组保持原样；动态 `ADD_FOOTER_TOOLBAR` 仍在点击时追加并选中。

### 3.3 插件工厂参数

加载器从：

```js
window[plugin.globalName]({ i18n, request, constants, Message, bus, modal, JavaClass, monaco })
```

扩展为：

```js
window[plugin.globalName]({
  i18n,
  request,
  constants,
  Message,
  bus,
  modal,
  JavaClass,
  monaco,
  pluginData: plugin
})
```

这是纯加法扩展，旧插件忽略新增字段即可。

## 4. 后端运行时属性

`org.ssssssss.magicapi.core.model.Plugin` 增加默认空 Map：

```java
private Map<String, Object> attributes = new HashMap<>();
```

使用 Map 而不是插件专属 `path` 字段，原因是 `Plugin` 是公共加载模型，运行时参数不应随每个插件需求持续增加字段。

Admin 和 Eureka 当前把路径放入 `globalName`，与该字段“JS 全局工厂名”的语义冲突。迁移后：

- Admin：`globalName = "MagicAdmin"`，`attributes.path = springbootAdminContextPath`。
- Eureka：`globalName = "MagicEureka"`，`attributes.path = eurekaDashboardPath`。
- 两者的前端插件从 `pluginData.attributes.path` 读取路径，并沿用现有 URL 基址和 `replaceURL` 语义。

Druid 和 Swagger 使用固定站内路径，不需要后端 `attributes.path`。

## 5. 插件模块落点

### 5.1 现有 console 源码

以下模块直接在现有 `src/console/src/index.js` 返回对象中增加状态栏项：

- Config
- Freemarker
- DTP
- WebSocket
- MQTT
- Debezium
- Vue3
- Sa-Token

S3、Kafka、Pulsar 已有状态栏项，只补 `order` 并纳入统一契约。

### 5.2 LangChain4j

在 AI 的 LangChain4j 插件模块恢复完整 `src/console` 构建来源，入口必须保留现有数据源扩展能力，并新增状态栏项。构建生成现有文件名 `magic-langchain4j.1.0.0.iife.js`，不直接手工编辑压缩产物。

### 5.3 Druid、Admin、Swagger、Eureka

在 Boot 的 `fw-plus-magicapi-plugin` 聚合目录中为四个集成提供独立插件资源：

- 新建 `fw-plus-magicapi-plugin-druid`。
- 新建 `fw-plus-magicapi-plugin-admin`。
- 完善已存在但仅含 POM 的 `fw-plus-magicapi-plugin-swagger`。
- 新建 `fw-plus-magicapi-plugin-eureka`。

每个模块使用现有插件 console 构建模式，产出独立 IIFE 和 SVG symbol。对应 Boot/Cloud Java 配置补 `globalName`、`javascriptFilename`；BOM 和聚合 POM 补齐模块坐标。Eureka 的后端配置仍归属 Cloud autoconfigure，前端资源模块可由 Cloud 使用方显式引入。

## 6. 加载与刷新时序

当前状态栏通过状态文案包含“加载完毕”或“Loaded”以及最多十秒轮询来发现插件。这既依赖国际化文案，也会造成延迟和漏刷新。

迁移后的流程：

1. `/plugins` 返回后端插件元数据。
2. 加载器并行加载有 `javascriptFilename` 的 IIFE。
3. 每个 IIFE 成功后，加载器调用工厂并把返回对象加入 `constants.PLUGINS`。
4. 加载器发出通用 `Message.PLUGINS_CHANGED` 事件。
5. 状态栏收到事件后重新收集、校验、过滤和排序。
6. 单个 IIFE 加载失败时不加入集合，其他 Promise 继续完成。

状态栏挂载时仍执行一次同步刷新，用于覆盖组件在插件集合之后挂载的情况。无需轮询。

## 7. 稳定排序

状态栏和静态插件 toolbar 共用相同的比较语义，但分别排序各自集合：

1. 保存扁平化后的原始索引。
2. 有限数值 `order` 优先于缺失或非法 `order`。
3. 有效 `order` 按升序排列。
4. `order` 相同时按原始索引排列。

该规则不依赖 JavaScript 引擎排序稳定性，并兼容未升级的第三方插件。

状态栏默认顺序：S3、Kafka、Pulsar、Config、Freemarker、DTP、Debezium、LangChain4j、WebSocket、MQTT、Druid、Swagger、Vue3、Sa-Token、Admin、Eureka。实现使用预留间隔的数值，后续插件可插入而无需整体重编号。

静态插件 toolbar 的数值只需保证当前全量唯一并符合现有视觉顺序。宿主内置 toolbar 不增加 `order`。

## 8. 宿主清理

`magic-status-bar.vue` 删除：

- 13 个 `hasXxxPlugin` ref。
- `checkPlugin`。
- 13 个插件专属 `icons` 项。
- 所有 `openConfig`、`openFtl` 等插件点击函数。
- 对 `Message.STATUS` 文案的插件加载判断。
- 插件检测轮询和超时器。
- 仅供插件地址计算使用的 import。

保留：

- 宿主固定入口。
- 用户退出入口。
- 通用 `pluginStatusBarItems` 收集和 `config.header` 过滤。
- 挂载刷新与通用事件刷新。

`magic-toolbar.vue` 保留内置数组和动态事件逻辑，只把静态插件 toolbar 的收集改为先携带 `order` 映射、过滤 `adminOnly`、稳定排序后追加。

## 9. 错误与兼容策略

- 插件 IIFE 加载失败：沿用现有加载失败状态，忽略该插件入口。
- 插件工厂返回空值：不加入可渲染集合，其他插件继续。
- 状态栏项字段非法：忽略该项目，不在页面显示错误。
- `order` 非法：项目排到合法顺序项之后，不丢弃。
- `attributes` 缺失：前端插件使用现有默认路径。
- `config.header` 未配置：默认显示，与现状一致。
- 第三方旧插件：工厂调用兼容，已有状态栏或 toolbar 可继续显示并排在显式排序项之后。

## 10. 测试策略

### 10.1 纯前端契约

- 通用排序测试覆盖不同顺序、同序稳定性、缺失值、`NaN` 和字符串值。
- 宿主边界测试确认不存在插件名称、资源类型、`hasXxxPlugin`、`checkPlugin` 和 `openXxx`。
- 通用事件测试覆盖插件成功加载后刷新、挂载时已有插件和单插件失败隔离。
- 状态栏过滤测试覆盖非法项目与 `config.header[displayKey] === false`。
- Toolbar 测试覆盖静态排序、`adminOnly`、内置顺序不变和动态追加行为。

### 10.2 插件契约

- 16 个状态栏插件检查 `icon`、`title`、`displayKey`、`order` 和 `onClick`。
- 检查原有固定 URL 和站内 URL 解析语义。
- 检查 IIFE 同时包含状态栏声明和对应 SVG symbol。
- 14 个现有静态 toolbar 插件检查明确 `order`。
- LangChain4j 检查数据源扩展与状态栏声明同时存在。

### 10.3 Java 与资源装配

- `Plugin.attributes` 默认值和 JSON 序列化测试。
- Druid、Admin、Swagger、Eureka 的 `plugin()` 测试。
- Admin/Eureka 默认路径和自定义路径测试。
- 新增模块 IIFE 在 Maven 包的 `magic-editor/plugins/` 中存在。

### 10.4 集成验证

- 构建所有受影响 console。
- 执行 Boot、Cloud、AI 的目标 Maven 测试和资源打包。
- 浏览器验证在线人数、插件图标、内置图标、静态 toolbar 顺序和点击行为。
- 执行 `openspec validate move-status-bar-items-to-plugins --strict`。
- 执行 `git diff --check`。

## 11. 实施顺序

1. 先增加排序与插件工厂参数的失败测试。
2. 实现公共 `Plugin.attributes`、通用事件和排序工具。
3. 迁移现有 Boot 插件及 S3/Kafka/Pulsar 顺序。
4. 恢复 LangChain4j console 源码。
5. 创建或完善 Druid、Admin、Swagger、Eureka 插件模块及后端配置。
6. 为所有现有静态插件 toolbar 补 `order`。
7. 清理宿主专属逻辑。
8. 运行分层构建、浏览器验证和严格规格校验。
