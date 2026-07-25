## Why

Kafka 与 Pulsar 管理页面目前仍由 Magic-API 宿主控制台直接维护，导致宿主依赖可选插件的页面、文案和入口判断。参照已完成的 S3 插件改造，应将消息中间件管理能力归还各自插件，使插件未启用时宿主不携带对应业务 UI。

## What Changes

- 将现有 Kafka 管理页面、运行时依赖和管理工具栏注册迁移到 `fw-plus-magicapi-plugin-kafka/src/console`。
- 将现有 Pulsar 管理页面、运行时依赖和管理工具栏注册迁移到 `fw-plus-magicapi-plugin-pulsar/src/console`。
- Kafka、Pulsar 插件通过通用 `toolbars` 元数据注册管理员管理入口，并沿用各自 `adminEnabled` 门控。
- Kafka、Pulsar 插件通过通用 `statusBarItems` 元数据恢复原状态栏入口及点击行为。
- 从宿主控制台移除 Kafka/Pulsar 专属管理页面、文案、工具栏和状态栏判断，宿主仅保留通用插件扩展机制。
- 增加插件契约测试，覆盖页面归属、工具栏与状态栏元数据、管理权限和宿主边界。

## Capabilities

### New Capabilities

- `magic-messaging-admin-console-ui`: 规范 Kafka、Pulsar 管理页面的插件归属、工具栏注册、权限门控及宿主 UI 边界。

### Modified Capabilities

无。

## Impact

- 影响 Kafka、Pulsar 插件控制台的组件、入口、i18n、构建脚本和测试。
- 影响 Magic-API 宿主控制台的组件注册、工具栏、状态栏和 i18n。
- 不改变 Kafka/Pulsar 后端管理接口、数据源格式、订阅资源格式或脚本侧 API。
- 用户已确认本次跨模块改造继续采用 Tweak 轻量流程，并要求按 Kafka、Pulsar、宿主集成分区使用多个 Agent 处理。
