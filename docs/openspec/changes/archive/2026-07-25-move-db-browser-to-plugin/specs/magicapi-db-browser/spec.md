## MODIFIED Requirements

### Requirement: Console plugin provides a DB browsing workflow
系统 SHALL 通过 Magic-API DB 插件提供 DB 管理入口，使用户可以选择数据源、浏览对象树、查看对象详情并预览数据；DB 管理前端 UI MUST 归属于 `fw-plus-magicapi-plugin-db/src/console`，后端自动配置、Controller、Service 和模型 MUST 位于 `fw-plus-boot-autoconfigure` 的 `magicapi/plugin/db` 插件包下，宿主 `fw-plus-magicapi-spring-boot-starter/src/console` MUST NOT 维护 DB 管理业务实现。

#### Scenario: User browses from datasource to data preview
- **WHEN** 用户进入 DB 管理模块
- **THEN** 前端 MUST 展示可选择的数据源入口
- **AND** 用户选择数据源后 MUST 能浏览对象列表
- **AND** 用户选择表或视图后 MUST 能查看结构详情和分页数据预览

#### Scenario: Backend request fails
- **WHEN** 后端元数据或数据预览请求失败
- **THEN** 前端 MUST 展示可理解的错误状态
- **AND** 前端 MUST 保持当前数据源选择和已加载的上下文，便于用户重试或切换数据源

#### Scenario: DB browser UI is packaged as plugin UI
- **WHEN** 构建 DB 浏览控制台前端
- **THEN** DB 浏览 UI MUST 位于独立 Magic-API 插件 `src/console` 中
- **AND** 宿主控制台 MUST NOT 新增 DB 浏览业务页面、DB 专属工具栏逻辑或 DB 专属 i18n 文案

#### Scenario: DB management backend is organized under the Magic-API plugin package
- **WHEN** 构建 `fw-plus-boot-autoconfigure`
- **THEN** DB 管理后端源码 MUST 位于 `fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/db`
- **AND** `MagicApiPluginAutoConfiguration` MUST 导入 DB 管理配置类
- **AND** `fw-plus-magicapi-plugin-db` MUST NOT 维护 DB 管理 Java 后端源码或 Spring Boot 自动配置声明

#### Scenario: DB management toolbar uses the expected name
- **WHEN** DB 插件注册控制台 toolbar
- **THEN** 中文控制台入口名称 MUST 显示为 `db管理`
