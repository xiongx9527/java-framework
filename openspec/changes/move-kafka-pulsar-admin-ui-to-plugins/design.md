## Context

宿主控制台当前直接包含 `magic-kafka.vue`、`magic-pulsar.vue`，并在 `magic-toolbar.vue` 和 `magic-status-bar.vue` 中根据插件元数据维护 Kafka/Pulsar 专属入口。与此同时，两个插件自己的 console 仅注册数据源与订阅编辑器。S3 插件已经证明，管理页面可以由插件 IIFE 自身打包，通过通用 `toolbars` 元数据接入宿主。

宿主加载插件时已经向插件入口传入 i18n、request、constants、Message、bus、modal、JavaClass 和 monaco。迁移后的页面必须使用这些运行时对象，不能继续通过相对路径导入宿主源码。

## Goals / Non-Goals

**Goals:**

- Kafka、Pulsar 管理页面及其业务文案完全归属各自插件 console。
- 两个插件通过现有通用 toolbar 扩展点注册管理员入口。
- 保持现有主题、布局、管理操作、API 路径和 `adminEnabled` 行为。
- 宿主不再维护 Kafka/Pulsar 专属组件、入口或插件存在性判断。
- Kafka、Pulsar、宿主集成采用互不重叠的 Agent 写入分区。

**Non-Goals:**

- 不重写管理页面的交互和视觉设计。
- 不修改 Kafka/Pulsar 后端控制器和管理接口。
- 不修改数据源、订阅资源或脚本模块行为。
- 不新增插件 SDK 或公共 Java API。

## Decisions

### 1. 页面随插件 IIFE 打包

将宿主中的两个管理页面分别迁入 Kafka/Pulsar 插件 `src/console/src/components`，并由插件 `src/index.js` 直接导入和注册。相比保留宿主页面再由插件激活，该方案能在插件未加载时彻底消除业务 UI 依赖，符合 S3 已建立的边界。

### 2. 复用现有插件运行时对象

每个插件增加轻量运行时服务，在入口初始化时保存 `request`、`modal`、`bus`、`Message` 和 i18n format。页面从本插件服务读取这些对象；Magic 表格、图标和 Monaco 编辑器使用宿主已全局注册的通用组件。相比复制宿主基础组件，该方案减少重复打包并保持控制台视觉一致。

### 3. 管理入口使用通用 toolbar 元数据

Kafka、Pulsar 插件各新增一个 `id` 唯一的管理 toolbar，设置 `adminOnly: true`、`fullscreen: true` 和现有标题、图标。宿主既有通用 toolbar 收集逻辑继续根据插件 `adminEnabled` 过滤并追加入口，不再保留两个专属分支。

### 4. 分区实施与集中集成

- Kafka Agent 只修改 Kafka 插件 console 及其契约测试。
- Pulsar Agent 只修改 Pulsar 插件 console 及其契约测试。
- 宿主 Agent 只删除宿主专属页面、注册、文案和入口逻辑，并补充边界契约检查。
- 主 Agent 负责审查三组结果、解决集成问题并运行完整构建验证。

## Risks / Trade-offs

- [迁移页面仍引用宿主内部模块] → 契约测试禁止插件页面出现指向宿主源码的相对导入，并实际执行插件构建。
- [全局组件解析与原页面不同] → 保留原组件标签，使用宿主已注册组件；通过宿主构建和浏览器验证确认渲染。
- [重复工具栏入口] → 插件入口加入后同步删除宿主 Kafka/Pulsar 专属 push 分支和状态栏工具栏注册。
- [大文件迁移产生噪声] → 先按原样迁移，仅做依赖注入和路径适配，不夹带页面重构。

## Migration Plan

1. 分别在 Kafka、Pulsar 插件中迁移页面、文案、运行时服务和 toolbar 元数据。
2. 删除宿主中对应页面及所有专属注册和文案。
3. 构建两个插件和宿主控制台，验证插件产物随 Maven 资源阶段进入 `magic-editor/plugins`。
4. 回滚时可恢复宿主页面和专属入口，同时移除插件 toolbar 元数据；后端无数据迁移。

## Open Questions

无。
