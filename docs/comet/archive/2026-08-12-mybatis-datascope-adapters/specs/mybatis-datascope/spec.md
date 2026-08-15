# MyBatis DataScope

## Capability

fw-plus 提供一个不绑定 MyBatis-Plus/MyBatis-Flex 的通用 MyBatis 行级数据权限能力。它通过 MyBatis `StatementHandler.prepare` 拦截 SQL，根据当前请求状态、表权限配置和 `UserRowPermissionProvider` 输出租户过滤条件与行权限过滤条件。

## Public API

通用模块提供以下能力：

- `RowPermission` 注解可标注 mapper 方法或 mapper 类型。
- `RowOperations` 定义 INSERT、SELECT、UPDATE、DELETE 操作常量。
- `UserRowPermissionProvider` 提供：
  - 是否启用行权限；
  - 加载表权限配置的输入流；
  - 当前用户 tenantId；
  - 当前用户对表和操作可用的 scope 集合；
  - SpEL 表达式所需的 Provider 属性/方法。
- `TableRowPermissionConfigConsumer` 接收配置文件名和 UTF-8 输入流。
- `RowPermissionHelper` 提供设置、读取、清理权限状态，以及带 finally 清理的同步执行方法。
- `RowPermissionInterceptor` 实现 MyBatis `Interceptor`，不暴露 MP/Flex 类型。

`RowPermission` 的状态值如下：

- `STATUS_NEITHER = 0`：不追加租户和行权限；
- `STATUS_TENANT_ONLY = 1`：只追加租户条件；
- `STATUS_BOTH = 3`：追加租户和行权限；
- `STATUS_ON_CONDITION = -1`：用 Provider 作为 root object 的 SpEL 条件计算上述状态。

未标注 `@RowPermission` 时默认使用 `STATUS_BOTH`。方法级注解优先于类型级注解；条件状态的计算结果必须是上述合法状态，否则抛出处理异常。

## Configuration

表配置使用 XML `table` 根节点，包含：

- `name`：表名或大小写不敏感的正则表名；
- `db-type`：Druid 支持的数据库类型；
- `table-alias`：scope-clause 中使用的逻辑别名；
- `tenant-id/column`：租户列，可选；
- `row-permissions/row-permission`：scope 数字与 scope-clause 的映射。

scope-clause 中的 `{{TABLE_NAME}}` 在加载时替换为配置表名；`${expression}` 在加载时预编译为 SpEL，在执行时以 Provider 为 root object 求值。值支持 null、String、Integer、Long、Boolean 以及这些类型组成的 Collection；字符串必须 SQL 转义，Collection 生成 IN 字面量，null 生成 `NULL`。非法类型、表达式或重复 scope 配置抛出配置/处理异常。

配置前缀如下：

- 通用开关：`fw.plus.mybatis-datascope.enabled`，默认启用；
- 缺少用户 scope 时的默认 scope：`fw.plus.mybatis-datascope.scope-when-missing-config`，未设置时 fail-closed；
- 为兼容现有 MP 空壳入口，`fw.plus.mp-datascope.enabled` 仍可关闭 MP 侧装配；
- 参考实现的 `fw.data-permission.enabled` 和 `fw.data-permission.scopeWhenMissingConfig` 作为兼容别名时，其值不得覆盖显式的 `fw.plus.mybatis-datascope` 配置。

## SQL Rewriting

当 Provider 未启用、当前状态为 `STATUS_NEITHER`、或不存在可用数据权限拦截条件时，SQL 原样继续执行。其他情况按以下规则处理：

1. 多语句逐条解析并重建，不能通过字符串前缀判断语句类型。
2. SELECT 处理普通表、JOIN、子查询和 UNION；外层 WHERE 过滤使用 WHERE，外连接右/左侧过滤进入对应 JOIN ON，避免改变外连接语义。
3. UPDATE/DELETE 识别目标表；目标表使用 UPDATE/DELETE 权限，引用表使用 SELECT 权限。UPDATE FROM、WITH、DELETE WITH、JOIN USING 等未实现形态抛出明确异常。
4. INSERT 不添加行过滤条件，保留原 SQL。
5. Replace、Call 和无法识别的 SQL 形态抛出明确的不支持异常。
6. 表名匹配固定表配置或正则表配置；表未配置时默认拒绝并抛出可定位异常，不静默放行。
7. JOIN 条件中的 `/*+ RP(left=tenant,right=none) */` 权限 hint 可独立降低左右表状态。hint 被成功消费后从输出 SQL 中移除；无效 hint 保留原条件并记录诊断。

租户条件使用实际 SQL 别名和配置列名。scope 条件先替换配置别名为实际别名，再追加到正确的 WHERE/ON 节点。多个 scope 条件取 OR；其中任一 scope-clause 为空表示该 scope 不限制行权限。

## Framework Adapters

通用模块定义可排序的框架适配器 SPI，使拦截器不依赖具体 ORM：

- `MybatisPlusDataScopeAdapter` 位于 `fw-plus-mp-datascope`，只识别 MyBatis-Plus mapper/实体和 MP 参数包装，使用 MP 的代理解包/元数据能力；未识别的原生 mapper 不由它声明支持。
- `MybatisFlexDataScopeAdapter` 位于 `fw-plus-mf-datascope`，只识别 MyBatis-Flex `BaseMapper`/实体和 Flex 参数包装/表元数据；未识别的原生 mapper 不由它声明支持。
- 适配器按顺序选择，MP 与 MF 依赖互相隔离；核心模块即使 classpath 没有两者也可使用。
- 适配器只负责框架差异（目标解包、mapper/实体识别和注册所需元数据），SQL AST 改写、权限状态和 Provider 合约只存在于通用核心。

## Spring Boot Integration

Boot autoconfigure 提供：

- 通用 datascope 配置属性、Provider 条件和 `RowPermissionInterceptor` Bean；
- 每个 `SqlSessionFactory` 至多注册一次通用拦截器的 BeanPostProcessor/registrar；
- MP、MF 适配器的 `@ConditionalOnClass`、`@ConditionalOnMissingBean` 自动配置；
- `MybatisAutoConfiguration` 聚合入口和 `spring.factories`/`AutoConfiguration.imports` 注册。

没有 `UserRowPermissionProvider` 时不得创建会导致启动失败的拦截器；禁用开关时不得改写 SQL。用户显式提供同类型拦截器或适配器 Bean 时，自动配置不覆盖它。MP/MF 不在 classpath 时对应自动配置必须安全跳过。

## State Cleanup

`RowPermissionHelper.execUseStatus` 在正常和异常路径都清理 ThreadLocal。自动清理状态由拦截器在一次 prepare 调用结束后清理；若保留 Web MVC 请求切面，则它只负责请求结束兜底清理，不承载 SQL 改写逻辑。

## Compatibility

现有 `fw-plus-mp-datascope` artifactId、MP starter 依赖位置和 MP 开关属性保持可用。由于该模块当前无已发布实现和公开 Java API，不要求迁移旧实现类名；新增 Java API 使用 `com.xiongx9527.fw.plus.mybatis.datascope`、`com.xiongx9527.fw.plus.mp.datascope`、`com.xiongx9527.fw.plus.mf.datascope` 命名空间。

## Verification

必须提供：

- 通用核心的配置解析、SpEL/值替换、权限状态、SELECT/JOIN/子查询/UNION、UPDATE/DELETE、fail-closed 和异常路径测试；
- MP 适配器的 mapper/实体/参数包装识别测试；
- MF 适配器的 mapper/实体/参数包装识别测试；
- Boot 自动配置的有/无 Provider、禁用开关、MP/MF classpath、重复注册测试；
- Maven 模块聚合、BOM 依赖、Java 8 编译和 `git diff --check` 验证。
