## Context

宿主状态栏已经能够从 `constants.PLUGINS` 通用收集 `plugin.statusBarItems`，但除 Kafka/Pulsar/S3 外，其余可选插件仍由宿主通过 `hasXxxPlugin` 和 `checkPlugin` 分支控制。Druid、Admin、Swagger、Eureka 目前没有前端 IIFE，LangChain4j 只有预构建 IIFE，没有可维护的 console 源码。

## Goals / Non-Goals

**Goals:**

- 所有可选插件状态栏入口均由插件自身声明。
- 宿主状态栏不包含任何具体插件名、资源类型或跳转地址。
- 保持原入口显示条件、图标、标题和点击行为，并通过 `order` 固化展示顺序。
- 所有现有静态插件 toolbar 通过同一排序语义获得稳定顺序。

**Non-Goals:**

- 不调整 Gitee、GitHub、QQ群、帮助和退出等宿主固定入口。
- 不改变插件业务页面、后端管理端点或权限模型。
- 不新增 toolbar，不改变动态 `ADD_FOOTER_TOOLBAR` 的点击打开行为。
- 不重做状态栏视觉样式。

## Decisions

### 1. IIFE 插件直接注册状态栏项

有 console 源码的插件在入口返回对象中增加 `statusBarItems`，使用插件自己的 SVG symbol 和原有 `displayKey`。外链点击函数由插件闭包持有，宿主只负责调用。

### 2. 无 IIFE 集成补充最小前端插件

Druid、Spring Boot Admin、Swagger、Eureka 分别增加独立前端插件模块和 IIFE。对应 Java `plugin()` 只提供 `globalName`、`javascriptFilename` 和通用运行时属性，不提供状态栏描述。LangChain4j 恢复 console 构建来源，禁止直接维护压缩产物。

### 3. 通用运行时属性传给前端工厂

`Plugin` 增加通用 `attributes`，加载器调用 IIFE 工厂时传入当前 `pluginData`。Admin、Eureka 使用 `attributes.path` 传递配置路径，替代过去复用 `globalName` 存放路径的行为。

### 4. 状态栏和静态插件 toolbar 统一排序

`statusBarItems` 和静态插件 `toolbars` 使用前端 `order`。宿主稳定排序：有效 `order` 升序，缺失或非法值排在最后，同序保持插件加载顺序。内置 toolbar 和动态 toolbar 不参与该排序。

### 5. 宿主只保留通用扩展点

`magic-status-bar.vue` 删除所有 `hasXxxPlugin`、`checkPlugin`、插件专属 `icons` 项和 `openXxx` 函数。插件加载器在 `constants.PLUGINS` 变化后发出通用事件，状态栏据此刷新，按 `config.header[displayKey]` 过滤和排序，并在在线人数之后渲染；不再依赖状态文案或定时轮询。

### 6. 分区实施

按 Boot 现有插件、四个新增插件与后端装配、AI LangChain4j、宿主通用机制和 toolbar 排序五个互不重叠写入分区实施，主任务负责协议审查和集成验证。

## Risks / Trade-offs

- [Admin/Eureka 自定义路径丢失] -> 使用 `Plugin.attributes.path`，覆盖默认路径和自定义路径测试。
- [插件 IIFE 图标不可用] -> 复用插件现有 SVG symbol，构建产物检查 symbol 与 `statusBarItems` 同时存在。
- [加载时序导致入口缺失] -> 使用专用通用事件刷新，并覆盖初始加载、失败隔离和多插件场景。
- [排序改变内置或动态 toolbar] -> 排序范围限定为静态插件 toolbar，并增加边界测试。
- [跨层契约不一致] -> Java 元数据测试、前端契约测试和实际 IIFE/Maven 资源构建共同验证。

## Migration Plan

1. 定稿状态栏、toolbar 排序和 `Plugin.attributes` 契约。
2. 迁移有 IIFE 的插件入口。
3. 新增四个最小前端插件并补齐后端加载元数据。
4. 删除宿主专属状态和行为。
5. 补齐所有现有静态插件 toolbar 的 `order` 并实现通用排序。
6. 运行各插件、宿主、Boot/Cloud/AI 构建与 OpenSpec 验证。

## Open Questions

无。关键方案已在 full Design 阶段确认。
