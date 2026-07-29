## 1. Core governance contracts

- [x] 1.1 在 `fw-plus-ai-askdata` 新增治理授权桥接 SPI、授权摘要、授权对象、列级授权、行级谓词、管理员查询范围和权限拒绝错误模型，保持 Java 8 与无 Spring 依赖。
- [x] 1.2 保留并兼容现有 `AskDataPermissionEvaluator`，实现旧接口到粗粒度能力判定的适配说明，避免把旧接口误认为完整行列权限来源。
- [x] 1.3 为治理 SPI、默认拒绝语义、跨项目/跨操作人拒绝和授权摘要不可扩大规则补充 Core 单元测试。

## 2. Starter autoconfiguration bridge

- [x] 2.1 在 AskData Starter 自动配置中装配默认拒绝治理桥接、业务 `@ConditionalOnMissingBean` 覆盖入口和开发测试桩示例。
- [x] 2.2 将 `CoreAskDataServiceAdapter`、默认 Flow、元数据提供器和请求上下文接入治理授权摘要，未授权时在模型调用前返回受控失败。
- [x] 2.3 为自动配置覆盖、默认拒绝、业务 Bean 优先和旧服务适配边界补充 Spring Boot 测试。

## 3. SQL authorization pipeline

- [x] 3.1 将授权摘要传入元数据过滤、Prompt 构建、SQL 插件链和最终 SQL 校验，确保插件只能收窄不能扩大授权对象或行级条件。
- [x] 3.2 使用受控 SQL 片段和具名参数表达行级谓词，不允许业务插件通过字符串拼接动态值。
- [x] 3.3 覆盖未授权数据源、未授权表/列、插件清空授权对象、插件移除行级谓词、跨项目 resume/token 复用和最终 SQL 重校验测试。

## 4. Observability and admin access

- [x] 4.1 将治理拒绝、授权版本、授权来源和 SQL 权限失败写入脱敏监控事件，并与 SQL 审计使用同一请求/会话关联标识。
- [x] 4.2 将管理员监控日志查询接入治理授权桥接，区分本项目管理员、平台管理员允许范围和普通操作人拒绝。
- [x] 4.3 覆盖项目管理员查询、平台管理员跨项目受限查询、普通操作人拒绝和查询审计测试。

## 5. Documentation and matrix updates

- [x] 5.1 更新 `docs/askdata/starter` 中治理权限、SQL 安全、配置、存储和外部系统边界文档，说明业务 IAM 如何映射到 Core 治理桥接。
- [x] 5.2 更新 SQLBot/WrenAI 功能矩阵中工作空间/成员/权限/数据源/对象授权/管理员日志相关状态，不宣称企业微信、钉钉、飞书、DataEase、Dify、Coze、n8n 已真实联通。
- [x] 5.3 补充可编译业务治理桥接示例或测试用例，展示数据源授权、列过滤和行级谓词参数绑定（`AskDataGovernanceBridgeDocumentationExample`）。

## 6. Verification

- [x] 6.1 运行 `openspec validate add-askdata-governance-permission-bridge --strict` 并修复所有 OpenSpec 问题。
- [x] 6.2 运行 Core AskData 相关 Maven 测试，确认 Java 8 Core 契约不引入框架类型。
- [x] 6.3 运行 AI AskData 自动配置定向测试，确认默认拒绝、业务覆盖、SQL 权限和管理员日志查询行为通过。
- [x] 6.4 运行文档/矩阵状态扫描和 `git diff --check`，记录验证证据。
