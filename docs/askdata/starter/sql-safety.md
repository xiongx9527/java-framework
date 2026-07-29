# SQL 安全

AskData 将 SQL 视为受控命令而非模型文本。模型、业务插件或远程运行时只能产生候选 SQL、受控片段和具名参数；最终命令必须经过统一验证器后才可能进入只读执行器。

## 插件链

建议按以下确定顺序组合插件：意图/模板片段、具名参数绑定、租户与数据范围补充、安全校验前方言改写、执行前准备、执行后结果映射、审计补充。每个插件仅接收受控命令和上下文，不能取得 JDBC 连接、数据库凭据或跳过后续阶段。

插件产出的 SQL 即使标记为可信，也必须回到最终校验器。插件链审计记录仅保存插件 key 和处理阶段，不保存 SQL 参数、连接与其他敏感对象。

## 命名参数与范围约束

动态值使用具名参数绑定，例如 `:tenantId`、`:operatorId`、`:startDate`；禁止以字符串拼接将用户输入、模型输出或租户值写入 SQL 文本。租户和数据范围插件应从经认证的 `AskDataProjectContext` 读取范围，将其转换为参数和授权对象约束，不能相信远程运行时自行声明的范围。

对象授权至少应验证项目、数据源逻辑 key、表/视图、列、行级范围和语义版本。未授权对象、跨项目对象或无法解析的方言对象直接拒绝并记录稳定错误码。

## 治理授权桥接

`AskDataGovernanceProvider` 是业务 IAM 到 AskData 的唯一数据访问桥接。业务实现根据 `AskDataAuthorizationRequest` 中已认证的 `AskDataProjectContext`（`projectId + operatorId`）查询自己的成员、角色、数据域与策略，再返回 `AskDataAuthorizationSummary`。Core 不依赖 Spring Security、Sa-Token、JustAuth 或任何企业 IAM 类型。

- `AskDataAuthorizedObject` 对应 IAM 判定后的一个数据源逻辑 key、表和列；列名可用 `*` 表示该表的全部列。业务 IAM 必须先完成项目归属判断，不能把调用方声称的数据源或角色直接当作授权事实。
- `AskDataAuthorizationSummary` 是一次请求的不可变授权快照。只有 `isAllowed()` 为 `true` 且存在当前数据源的已授权对象时，流程才继续；业务插件只能收窄该摘要，不能扩大、清空后替换项目/操作人/参数/授权对象或行级谓词。
- `AskDataRowPredicate` 对应 IAM 给某个已授权对象下发的行级范围。其 `sqlFragment` 必须是审核过的受控 SQL 片段，动态值必须使用 `AskDataSqlParameter.of("name", value)` 以 `:name` 具名参数传递，禁止拼接用户、模型、租户或角色值。

例如，业务 IAM 可以把“项目 A 的 `sales` 数据源中 `orders` 表仅可读 `id`、`amount`，且只看本人区域”映射为两个 `AskDataAuthorizedObject` 与一个 `AskDataRowPredicate`；谓词可写为 `region_id = :regionId`，参数为 `AskDataSqlParameter.of("regionId", regionId)`。当前 Starter 对授权摘要中尚未能落实到受控最终 SQL 的行级谓词采取 fail-closed；由业务治理层或后续增强完整注入谓词时，仍必须经过最终 SQL 校验，不能以“已由 IAM 判定”为由跳过它。

元数据会在 Prompt 构建之前按授权摘要过滤表和列，未授权列不会进入 Prompt。最终 SQL 校验仍会重查授权对象和行级约束，因此元数据过滤不是 SQL 放行依据。

## 最终校验与只读执行

最终校验至少执行以下检查：

- 单语句：拒绝分号拼接、多语句和批量写入语义。
- 只读：拒绝 `INSERT`、`UPDATE`、`DELETE`、`MERGE`、DDL、事务控制、存储过程和方言等价写操作。
- 授权对象：只允许项目、数据源和数据范围授权的对象。
- 物理限行：强制添加或收紧 `LIMIT`/方言等价限制，默认最大 100 行。
- 超时与资源：执行器使用只读账号、查询超时、连接池上限和结果集大小限制。
- 方言：仅对已验证方言启用相应改写，未知方言拒绝执行而非猜测。

`sql.execute-enabled` 默认是 `false`。开启后仍要同时满足上述校验；配置开关不是执行授权。

## 审计与脱敏

SQL 审计应记录项目、操作人、会话、请求、数据源 key、语义版本、插件链、校验结论、执行时长、行数、稳定错误码和结果摘要。SQL 文本、具名参数、敏感列值、连接串、账号、token 与模型原始响应按最小化原则保存；展示和导出前进行脱敏、访问控制及保留期清理。

管理员查看审计同样受项目范围约束，跨项目读取必须由业务授权。审计写入失败应采取显式失败策略，不得悄然放行未完成安全记录的生产查询。

管理员监控日志查询还同时需要 `AskDataMonitoringLogAccessAuthorizer` 和 `AskDataMonitoringLogAccessPolicy`：前者向宿主 IAM 确认请求人是否为目标项目管理员，后者再约束可读日志范围。两者默认拒绝；不能信任调用方传入的管理员角色。成功查询会追加仅含项目范围和是否存在 trace 过滤条件的脱敏审计记录，不保存 trace 或日志正文。

## 业务扩展接口

业务可以新增 SQL 插件、方言注册、对象授权解析器、审计落库和执行器，但必须通过契约测试验证：恶意插件改写为写 SQL 时被最终校验拒绝；参数没有文本拼接；限行和超时生效；未授权对象被拒绝；审计不泄露敏感信息。
