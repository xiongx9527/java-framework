# Outcome

将参考项目 `framework/data-permission` 的行级数据权限能力抽象为 fw-plus 的通用 MyBatis 能力，并为 MyBatis-Plus、MyBatis-Flex 提供独立适配器。使用者只需提供当前用户权限 Provider 和表权限配置，即可在支持的 MyBatis 会话工厂中自动对 SQL 注入租户条件与行权限条件。

# Scope

- 新增 `fw-plus-mybatis-datascope` 通用模块，承载行权限 API、`@RowPermission`、状态辅助工具、Provider SPI、XML 表权限配置模型、SpEL 占位符解析、SQL 拦截与异常类型。
- 将参考实现的 SELECT、UPDATE、DELETE 行权限处理抽象到原生 MyBatis `StatementHandler` 链路，覆盖连接查询、子查询、UNION、UPDATE/DELETE 目标表识别和 Join 权限提示；INSERT 不注入过滤条件，明确不支持的 SQL 保持可诊断失败。
- 保留安全默认值：用户未获得表权限且未配置默认 scope 时生成无权限条件；表或 scope 配置缺失、表达式非法、SQL 类型不支持时抛出明确异常。
- 将现有 `fw-plus-mp-datascope` 改造为 MyBatis-Plus 适配模块，依赖通用核心并提供 MP 适配器/自动装配入口。
- 新增 `fw-plus-mf-datascope` 模块，提供 MyBatis-Flex 适配器/自动装配入口。
- 补齐 fw-plus-core 的聚合 POM、BOM、Boot autoconfigure 和 MP starter 依赖/注册，使通用拦截器及 MP/MF 适配器按 classpath 和配置条件装配，并避免重复注册。
- 为通用核心、MP 适配器、MF 适配器和 Boot 自动配置补充单元/上下文测试，至少验证 SQL 改写、状态清理、适配器选择、条件装配和重复注册防护。

# Non-goals

- 不实现具体业务系统的 `UserRowPermissionProvider`，不绑定 Sa-Token、数据库表结构或组织/租户模型。
- 不迁移参考项目中与 MyBatis 无关的业务公共库、Controller 切面之外的 Web 业务逻辑或外部 `framework-common/framework-database` 依赖。
- 不扩展为新的 SQL 解析器能力；继续使用项目已统一管理的 Druid SQL AST，未覆盖的 SQL 形态不以字符串拼接方式猜测改写。
- 不改变现有 `fw-plus-mybatis-datachange`、字段加密和 MyBatis-Flex/Plus 其他适配器的既有行为。

# Acceptance examples

- A1：通用模块可在不引入 MyBatis-Plus 或 MyBatis-Flex 的情况下编译，并能作为 MyBatis `Interceptor` 对 SELECT/UPDATE/DELETE SQL 进行权限改写。
- A2：Provider 可返回租户 ID、表/操作对应的 scope 集合和表配置流；配置支持固定表名、正则表名、表别名、租户列及 scope-clause。
- A3：默认状态为同时启用租户与行权限；`STATUS_NEITHER`、`STATUS_TENANT_ONLY`、`STATUS_BOTH` 和条件表达式覆盖方法/类级 `@RowPermission`。
- A4：scope-clause 支持 `${...}` Provider SpEL 占位符、标量和集合值安全 SQL 字面量替换；空权限默认生成拒绝条件，空 scope-clause 表示不追加行限制。
- A5：SELECT 的普通表、JOIN、子查询和 UNION 能按语义将过滤条件加入 WHERE/ON；UPDATE/DELETE 只对目标表使用写操作权限，引用表按 SELECT 处理。
- A6：Join hint 可分别控制左右表的 `tenant`、`both`、`none` 权限状态，并在改写后移除已消费的权限 hint；不支持的 USING、UPDATE FROM、WITH 等形态有明确异常。
- A7：MP 适配器只在 MP mapper/实体可解析时生效，MF 适配器只在 Flex mapper/实体可解析时生效；二者均不强依赖对方框架。
- A8：Boot 自动配置在 Provider 存在且功能未禁用时为每个 `SqlSessionFactory` 注册一个通用数据权限拦截器；无 Provider、禁用配置或重复注册时不产生错误/重复拦截。
- A9：`RowPermissionHelper` 的显式执行状态在一次 SQL 处理后按 auto-clear 规则清理，异常路径也清理；Controller 请求结束清理切面若保留则位于 Boot 层。
- A10：Core、MP、MF 定向测试及 Boot 上下文测试通过，Java 8 源码约束和 `git diff --check` 通过。

# Constraints and invariants

- 源码和资源使用 UTF-8，Core 目标 JDK 1.8，不使用 Java 9+ API。
- 通用核心不得依赖 MyBatis-Plus 或 MyBatis-Flex；MP/MF 适配器只能依赖通用核心及各自框架。
- 保持现有 groupId、`${revision}`、flatten-maven-plugin 和模块命名风格；新增/调整依赖同步到 Core BOM。
- 兼容现有 `fw-plus-mp-datascope` artifactId 与 `fw.plus.mp-datascope.enabled` 配置入口；通用配置使用明确的 `fw.plus.mybatis-datascope` 前缀，并避免破坏已有空壳自动配置。
- SQL 改写默认 fail-closed；不因为无法识别表、scope 或表达式而放行查询。
- 不修改当前主工作区已有的 Tinyflow 等无关未提交改动；实现只发生在 Native 为本 change 创建的 worktree。

# Decisions

- 采用“通用 MyBatis 核心 + MP 适配器 + MF 适配器 + Boot 条件装配”的分层方式，与现有 `fw-plus-mybatis-datachange` 结构保持一致。
- `fw-plus-mp-datascope` 保留为 MP 适配 artifact；新增同级 `fw-plus-mf-datascope`，不把 Flex 代码塞进 MP 模块。
- 使用 Provider SPI 取得当前用户上下文和配置流；框架核心不内置具体认证、租户和数据权限查询实现。
- 参考项目的安全默认行为作为新能力默认行为：缺少用户 scope 时拒绝访问，空 scope-clause 表示该 scope 不限制行。
- 普通实现选择（代理解包、缓存、异常类型、测试夹具和 POM 细节）由实现阶段按现有代码风格决定。

# Open questions

- [blocking] CONFIRM: 是否按上述目标实现：新增通用 `fw-plus-mybatis-datascope`，保留并改造 `fw-plus-mp-datascope`，新增 `fw-plus-mf-datascope`，同步 Boot 自动装配与测试；默认采用参考项目的 SELECT/UPDATE/DELETE 行权限行为、缺少权限时拒绝、MP/MF 仅提供框架适配，且不纳入当前主工作区无关改动。

# Verification expectations

- 先运行 Core 通用 datascope 模块定向测试，再运行 MP/MF 适配器测试和 Boot autoconfigure 定向测试。
- 至少执行涉及模块的 Maven `test`/`package` 或等价 Reactor 定向构建，并检查最终 POM 模块关系、BOM 依赖和自动配置注册。
- 验收阶段由独立只读 Verifier 逐项核对 A1-A10；未覆盖的真实业务 Provider、外部数据库和生产 Web 请求保留为已知限制，不冒充已验证。
