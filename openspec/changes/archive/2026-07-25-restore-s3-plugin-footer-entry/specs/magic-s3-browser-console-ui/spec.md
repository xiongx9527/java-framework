## MODIFIED Requirements

### Requirement: S3 文件管理页面遵循 Magic 控制台风格
S3 文件管理页面 SHALL 使用与原 magic-s3 数据源配置页面一致的控制台布局、控件、间距、边框和主题变量。页面 SHALL 避免独立应用式卡片、营销式布局或与宿主控制台不一致的视觉风格。S3 插件 UI SHALL 注册原状态栏 X-file-storage 外链入口，宿主状态栏 SHALL NOT 维护 S3 专属入口逻辑。

#### Scenario: 页面打开后呈现控制台风格
- **WHEN** 管理员通过 S3 插件工具栏打开文件管理页面
- **THEN** 页面顶部操作区、表单控件、列表区域和详情区域使用 Magic 控制台一致的紧凑布局与主题变量

#### Scenario: 保持插件 UI 边界
- **WHEN** S3 文件管理页面被实现或构建
- **THEN** 页面源码和入口保持在 `fw-plus-magicapi-plugin-s3/src/console` 插件 UI 内，不迁移到宿主 UI

#### Scenario: 插件工具栏追加到现有工具栏后面
- **WHEN** Magic 控制台加载 S3 插件工具栏
- **THEN** 插件工具栏追加到现有工具栏之后，不插入到工具栏列表前面

#### Scenario: 控制台前端资源使用同一次构建产物
- **WHEN** Magic 控制台前端资源被 Maven 打包
- **THEN** `magic-editor/index.html` 与其引用的 `assets` 文件来自同一次前端构建，避免 hash 资源引用后出现 404

#### Scenario: S3 外链入口由插件 UI 注册
- **WHEN** Magic 控制台加载 S3 插件 UI
- **THEN** S3 插件 MUST 注册标题为 `X-file-storage` 的外链入口
- **AND** 该入口 MUST 使用 S3 插件 icon
- **AND** 点击该入口 MUST 在新标签页打开 `https://x-file-storage.xuyanwu.cn/`
- **AND** 宿主状态栏 MAY 通过通用插件状态栏入口扩展点渲染该入口
- **AND** 宿主 `magic-status-bar.vue` MUST NOT 维护 `hasS3Plugin`、`openS3()` 或 `s3-datasource` 的 S3 专属状态栏逻辑
