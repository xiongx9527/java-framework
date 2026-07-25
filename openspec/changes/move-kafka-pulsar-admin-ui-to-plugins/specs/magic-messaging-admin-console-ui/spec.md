## ADDED Requirements

### Requirement: Kafka 管理页面归属 Kafka 插件
Kafka 管理页面、业务文案和运行时适配代码 SHALL 位于 `fw-plus-magicapi-plugin-kafka/src/console`。Kafka 插件 SHALL 通过通用 `toolbars` 元数据注册管理入口，且入口 MUST 受 `adminEnabled` 管理权限门控。

#### Scenario: 管理员加载 Kafka 插件
- **WHEN** 控制台加载 `adminEnabled=true` 的 Kafka 插件
- **THEN** Kafka 插件注册一个使用 Kafka 图标和标题的管理工具栏
- **AND** 该工具栏支持全屏展示 Kafka 管理页面

#### Scenario: 非管理员加载 Kafka 插件
- **WHEN** 控制台加载 `adminEnabled=false` 的 Kafka 插件
- **THEN** 通用工具栏机制不展示 Kafka 管理入口

#### Scenario: Kafka 页面保持原管理能力
- **WHEN** 管理员打开 Kafka 管理页面
- **THEN** 页面继续支持选择数据源、查看 Topic、查看详情、创建和删除 Topic 以及消费者组偏移量操作
- **AND** 页面继续调用现有 Kafka 管理接口

### Requirement: Pulsar 管理页面归属 Pulsar 插件
Pulsar 管理页面、业务文案和运行时适配代码 SHALL 位于 `fw-plus-magicapi-plugin-pulsar/src/console`。Pulsar 插件 SHALL 通过通用 `toolbars` 元数据注册管理入口，且入口 MUST 受 `adminEnabled` 管理权限门控。

#### Scenario: 管理员加载 Pulsar 插件
- **WHEN** 控制台加载 `adminEnabled=true` 的 Pulsar 插件
- **THEN** Pulsar 插件注册一个使用 Pulsar 图标和标题的管理工具栏
- **AND** 该工具栏支持全屏展示 Pulsar 管理页面

#### Scenario: 非管理员加载 Pulsar 插件
- **WHEN** 控制台加载 `adminEnabled=false` 的 Pulsar 插件
- **THEN** 通用工具栏机制不展示 Pulsar 管理入口

#### Scenario: Pulsar 页面保持原管理能力
- **WHEN** 管理员打开 Pulsar 管理页面
- **THEN** 页面继续支持选择数据源、管理 Tenant、Namespace 和 Topic、查看 Topic 详情以及消费者组偏移量操作
- **AND** 页面继续调用现有 Pulsar 管理接口

### Requirement: 消息插件保留状态栏入口
Kafka、Pulsar 插件 SHALL 分别通过通用 `statusBarItems` 扩展点注册状态栏入口。Kafka 状态栏项 SHALL 保持打开 Kafka 官网的点击行为；Pulsar 状态栏项 SHALL 保持打开 Pulsar 管理工具栏的点击行为。

#### Scenario: 点击 Kafka 状态栏项
- **WHEN** 用户点击 Kafka 插件注册的状态栏项
- **THEN** 控制台打开 `https://kafka.apache.org/`

#### Scenario: 点击 Pulsar 状态栏项
- **WHEN** 用户点击 Pulsar 插件注册的状态栏项
- **THEN** 插件通过宿主注入的事件总线打开 Pulsar 管理工具栏
- **AND** 管理工具栏继续使用插件提供的组件和元数据

### Requirement: 宿主控制台保持消息插件无关
Magic-API 宿主控制台 MUST NOT 包含 Kafka/Pulsar 管理页面、业务文案、专属工具栏注册或专属状态栏判断。宿主 SHALL 仅通过通用插件 toolbar 和 status bar 扩展点渲染两个插件提供的入口，并 SHALL 将插件工具栏追加在现有工具栏之后。

#### Scenario: 构建不含消息插件的宿主控制台
- **WHEN** 宿主控制台未加载 Kafka 或 Pulsar 插件
- **THEN** 宿主仍可独立构建和运行
- **AND** 宿主源码中不存在 Kafka/Pulsar 管理页面和专属入口分支

#### Scenario: 同时加载两个消息插件
- **WHEN** Kafka 与 Pulsar 插件均启用管理入口
- **THEN** 宿主通过同一通用扩展点追加两个插件工具栏
- **AND** 两个插件页面使用各自运行时依赖和业务文案，互不覆盖

### Requirement: 插件构建产物包含管理页面
Kafka、Pulsar 插件的前端构建产物 MUST 分别包含其管理页面、toolbar 和 status bar 注册元数据，Maven 资源处理 SHALL 将对应 IIFE 文件复制到 `magic-editor/plugins`。

#### Scenario: 构建消息插件前端
- **WHEN** 分别执行 Kafka、Pulsar 插件 console 构建
- **THEN** 两个 IIFE 产物均包含各自管理 toolbar 的唯一 id、`adminOnly` 和 `fullscreen` 元数据
- **AND** 两个 IIFE 产物均包含各自的 `statusBarItems` 元数据

#### Scenario: 执行 Maven 资源处理
- **WHEN** Maven 对 Kafka、Pulsar 插件执行资源处理
- **THEN** 对应 IIFE 文件被复制到插件资源输出目录供宿主动态加载
