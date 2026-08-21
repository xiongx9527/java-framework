# Tiny Engine 整体验收规格

## Purpose

定义当前 `dev` 基线下 Tiny Engine 从模块测试、Spring Boot 启动到 H2 HTTP API 和 AI 配置链路的整体验收边界。

## Requirements

### Requirement: Core 与 Boot 组件可测试

系统 SHALL 在 Java 8 下通过 Tiny Engine Core、Boot 自动配置和 Tiny Engine Starter 的编译及测试；测试不得依赖旧分支中的已删除模块或未提交构建产物。

#### Scenario: Core 服务与 Mapper

- **WHEN** 执行 Tiny Engine Core 测试
- **THEN** Mapper 资源、MyBatis-Plus CRUD ResultMap、服务契约、应用角色和 AI 模型持久化测试全部通过
- **AND** 失败项必须给出明确的测试类、方法和根因

#### Scenario: Boot 自动配置

- **WHEN** 执行 Boot Tiny Engine 自动配置与 Starter 测试
- **THEN** MVC/WebFlux Controller、SQL manifest、Mapper 扫描、安全条件和前端资源测试全部通过

### Requirement: AI Provider 与模型配置可验证

系统 SHALL 验证 AI Boot 自动配置、LangChain4j Core Adapter 和 Tiny Engine Provider 注册能力在当前 classpath 下可装配。

#### Scenario: Provider 注册

- **WHEN** classpath 包含 LangChain4j Adapter 且适配器开关开启
- **THEN** Tiny Engine Provider Registry 可获得 `langchain4j` Provider
- **AND** 缺少可选 Provider 或关闭适配器时应用仍能按策略启动

#### Scenario: 应用级模型配置

- **WHEN** 管理员为应用创建 Model Service 和 Model
- **THEN** API Key 只以掩码或解密后的内部配置使用，不以明文返回
- **AND** 模型配置按 tenant、app、service 和 model 边界解析

### Requirement: H2 sample 可启动并提供认证和应用 API

系统 SHALL 支持使用 H2 内存数据库启动 Tiny Engine AI sample，并完成默认租户和管理员初始化。

#### Scenario: 严格租户表结构

- **GIVEN** `t_tenant.enable` 为 `NOT NULL` 且没有默认值
- **WHEN** sample 执行默认租户初始化
- **THEN** 默认系统租户写入 `enable=true`，应用上下文成功启动

#### Scenario: 认证和租户

- **WHEN** 使用管理员账号登录并携带 Bearer Token 请求 `user/me` 与租户列表
- **THEN** 返回管理员角色、当前租户和可用租户
- **AND** 没有租户域名字段时 Host 登录回退到默认系统租户

#### Scenario: 应用接口

- **WHEN** 请求带 `currentPage`、`pageSize` 的应用列表并创建应用
- **THEN** 返回成功结果，应用 tenant、创建人和默认首页关系正确
- **AND** 缺少必填分页参数时返回明确的 400，而不是 500

### Requirement: AI Chat 错误边界可观察

系统 SHALL 区分权限/配置错误和外部 Provider 错误。

#### Scenario: 未配置应用模型

- **WHEN** Chat 请求没有合法应用权限或模型配置
- **THEN** 返回受控权限或配置错误，不泄露内部堆栈

#### Scenario: Provider 不可达

- **WHEN** 应用模型配置指向无效的 OpenAI-compatible URL
- **THEN** 请求通过本地权限和配置解析后返回受控 502
- **AND** 日志只保留脱敏的上游错误信息

### Requirement: Tinyflow 前端和资源打包

Starter SHALL 按 Magic-API Console 风格组织前端，并能在 Maven 构建中完成 npm 安装、测试、生产构建和资源复制。

#### Scenario: Console 目录

- **WHEN** 检查 Tinyflow Starter 前端源码
- **THEN** 根目录为 `src/console`，包含 package、lock、Vite、单一入口和 `src/components`、`src/plugins`、`src/assets`、`src/icons`、`src/scripts`
- **AND** 不保留 `src/main/frontend/tinyflow-ui`

#### Scenario: 构建与打包

- **WHEN** 执行 `npm ci`、`npm test`、`npm run build` 和 Maven package
- **THEN** 产物包含版本化 Tinyflow UI JS、CSS 和许可证资源
- **AND** 不提交 `node_modules`
