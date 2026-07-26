## ADDED Requirements

### Requirement: 可选插件拥有自己的状态栏入口
每个可选插件 SHALL 通过自己的前端插件对象声明状态栏项。状态栏项 MUST 包含图标、标题、`displayKey`、`order` 和点击动作，且插件未加载时对应入口 MUST NOT 出现。后端插件元数据 MUST NOT 承载状态栏描述。

#### Scenario: 加载有前端 IIFE 的插件
- **WHEN** 控制台成功加载一个提供 `statusBarItems` 的插件 IIFE
- **THEN** 状态栏展示该插件声明的入口
- **AND** 点击行为由插件声明而不是宿主插件名分支提供

#### Scenario: 加载新增的最小前端插件
- **WHEN** Druid、Spring Boot Admin、Swagger 或 Eureka 集成启用
- **THEN** 控制台加载该集成自有的 IIFE
- **AND** 状态栏入口由 IIFE 返回的 `statusBarItems` 提供

#### Scenario: 插件没有加载前端 IIFE
- **WHEN** 后端插件元数据没有有效的 `globalName` 或 `javascriptFilename`
- **THEN** 宿主不为该插件生成状态栏入口

### Requirement: 宿主状态栏与具体插件解耦
`magic-status-bar.vue` MUST NOT 包含具体可选插件的存在性状态、资源类型判断、图标项或点击函数。宿主 SHALL 仅通用收集、校验、过滤、排序、刷新和渲染插件状态栏项。

#### Scenario: 构建不含任何可选插件的宿主
- **WHEN** 宿主控制台未加载可选插件
- **THEN** 宿主仍可独立构建和运行
- **AND** 状态栏不显示任何插件入口

#### Scenario: 同时加载多个可选插件
- **WHEN** 多个插件分别声明状态栏项
- **THEN** 宿主通过同一扩展点渲染所有允许显示的入口
- **AND** 不执行任何按插件名或资源类型编写的专属判断

#### Scenario: 插件加载列表发生变化
- **WHEN** 插件加载器成功加入一个插件对象
- **THEN** 加载器发出通用插件变更事件
- **AND** 状态栏重新收集插件项
- **AND** 刷新不依赖状态文案内容或定时轮询

#### Scenario: 插件声明非法状态栏项
- **WHEN** 状态栏项缺少图标、标题、`displayKey` 或点击函数
- **THEN** 宿主忽略该项目
- **AND** 其他插件入口继续正常显示
- **AND** 页面不显示错误内容

### Requirement: 状态栏兼容原有行为
迁移后的插件状态栏入口 MUST 保持原有图标、标题、`displayKey`、跳转目标和配置开关行为，并 SHALL 位于在线人数之后、宿主内置入口之前。

#### Scenario: 配置隐藏插件入口
- **WHEN** `config.header[displayKey]` 为 `false`
- **THEN** 对应插件状态栏项不显示

#### Scenario: 点击迁移后的插件入口
- **WHEN** 用户点击任一迁移后的插件状态栏项
- **THEN** 控制台执行迁移前相同的外链或相对管理地址跳转

#### Scenario: 使用非默认后端基址构建新增插件
- **WHEN** Druid、Spring Boot Admin、Swagger 或 Eureka 使用非默认 `MAGIC_API_BASE_URL` 构建 IIFE
- **THEN** 构建产物中的点击动作打开该后端基址下的目标路径
- **AND** 不回退到当前前端页面的 origin

#### Scenario: 迁移 Druid 和 Swagger 图标
- **WHEN** Druid 或 Swagger 状态栏图标从宿主迁入插件 IIFE
- **THEN** 新 symbol 保持原 SVG 的 `viewBox`、path 和视觉内容
- **AND** 仅允许等价的 symbol 包装或无损压缩

#### Scenario: 按声明顺序渲染插件入口
- **WHEN** 多个有效状态栏项包含不同的 `order`
- **THEN** 宿主按 `order` 数值升序稳定排列

#### Scenario: 状态栏项缺少合法顺序
- **WHEN** 状态栏项没有 `order` 或 `order` 不是合法数值
- **THEN** 该项目排在具有合法 `order` 的项目之后
- **AND** 同序项目保持插件加载顺序

### Requirement: 插件构建产物携带状态栏声明
受影响插件的构建产物 MUST 包含 `statusBarItems`、对应 SVG symbol、`order` 和点击逻辑。Druid、Spring Boot Admin、Swagger、Eureka MUST 提供可由宿主加载的独立 IIFE，LangChain4j MUST 提供可维护的 console 构建来源。

#### Scenario: 构建插件前端
- **WHEN** 执行受影响插件的前端和 Maven 资源构建
- **THEN** 每个 IIFE 产物包含该插件的状态栏声明

#### Scenario: 获取插件元数据
- **WHEN** 宿主请求插件列表
- **THEN** Druid、Spring Boot Admin、Swagger、Eureka 返回有效的 `globalName` 和 `javascriptFilename`
- **AND** 宿主可从 classpath 提供对应 IIFE 资源

#### Scenario: 按框架分层装配 Eureka 插件
- **WHEN** 按 `Core -> Boot -> Cloud` 顺序从干净依赖仓库构建各层
- **THEN** Boot 聚合和 Boot BOM 不依赖 Eureka 插件或任何 Cloud artifact
- **AND** Eureka 插件由 Cloud starters 聚合和 Cloud BOM 管理
- **AND** Cloud 使用方可显式引入 Eureka 插件 artifact

### Requirement: 插件运行时属性传入前端工厂
后端 `Plugin` SHALL 支持通用 `attributes`，插件加载器 SHALL 将当前插件元数据作为 `pluginData` 传入对应前端工厂。状态栏组件 MUST NOT 直接解析这些属性或插件名称。

#### Scenario: 打开自定义 Spring Boot Admin 路径
- **WHEN** Spring Boot Admin 配置了非默认上下文路径
- **THEN** 后端通过 `attributes.path` 返回该路径
- **AND** Admin 前端插件使用 `pluginData.attributes.path` 打开配置地址

#### Scenario: 打开自定义 Eureka 路径
- **WHEN** Eureka 配置了非默认 Dashboard 路径
- **THEN** 后端通过 `attributes.path` 返回该路径
- **AND** Eureka 前端插件使用 `pluginData.attributes.path` 打开配置地址

### Requirement: 静态插件 Toolbar 支持稳定排序
所有现有前端插件声明的静态 `toolbars` 项 MUST 包含明确的 `order`。宿主 SHALL 只对静态插件 toolbar 按 `order` 稳定排序，且 MUST 保持内置 toolbar、动态 `ADD_FOOTER_TOOLBAR` 和 `adminOnly` 行为不变。

#### Scenario: 加载多个静态插件 Toolbar
- **WHEN** 多个插件声明带 `order` 的静态 toolbar
- **THEN** 宿主按 `order` 数值升序稳定追加这些 toolbar
- **AND** 内置 toolbar 的相对顺序不变

#### Scenario: 过滤管理员专属 Toolbar
- **WHEN** 静态插件 toolbar 声明 `adminOnly: true` 且插件未启用管理员模式
- **THEN** 该 toolbar 不显示
- **AND** 其他 toolbar 的排序结果不受影响

#### Scenario: 动态打开 Toolbar
- **WHEN** 插件通过 `ADD_FOOTER_TOOLBAR` 动态打开 toolbar
- **THEN** 宿主保持现有点击后追加并打开的行为
- **AND** 不对该动态项目执行静态插件排序
