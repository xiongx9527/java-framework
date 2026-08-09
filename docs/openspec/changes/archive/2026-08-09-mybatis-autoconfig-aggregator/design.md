## Context

`fw-plus-boot-autoconfigure` 的 `com.xiongx9527.fw.plus.boot.autoconfigure.mybatis` 包下已有 3 个子包：

- `mybatis.mp`：MyBatis-Plus 自动配置（`MpAutoConfiguration` 已 `@Import` 4 个 mp 子配置：MpBase/Join/Encrypt/DataScope）
- `mybatis.mf`：MyBatis-Flex 自动配置（`MfDataChangeAdapterAutoConfiguration`、`MfEncryptAutoConfiguration`）
- `mybatis.datachange`：MyBatis 通用数据变更追踪（`MybatisDataChangeAutoConfiguration`）

当前 `spring.factories` 中 mybatis 相关 5 个类独立注册：

- `mybatis.mp.MpAutoConfiguration`（自身已聚合 4 个 mp 子配置）
- `mybatis.datachange.MybatisDataChangeAutoConfiguration`
- `mybatis.mp.MpDataChangeAdapterAutoConfiguration`
- `mybatis.mf.MfDataChangeAdapterAutoConfiguration`
- `mybatis.mf.MfEncryptAutoConfiguration`

项目主流聚合模式是用 `@Import` 在顶层 AutoConfiguration 聚合同模块子配置（`MpAutoConfiguration`、`MqttAutoConfiguration`、`MateAutoConfiguration`、`WebAutoConfiguration`、`JdbcAutoConfiguration`、`ReportAutoConfiguration`、`TemplateAutoConfiguration`、`NoSqlAutoConfiguration`、`UIDAutoConfiguration` 均如此），仅 `TraceAutoConfiguration` 用 `@ImportAutoConfiguration`。

## Goals / Non-Goals

**Goals:**

- 新建顶层 `MybatisAutoConfiguration` 作为 MyBatis 生态统一聚合入口。
- `spring.factories` / `AutoConfiguration.imports` 中 mybatis 注册从 5 行收敛为 1 行。
- 保持最终注册的 Bean 集合、条件求值、运行时行为与改动前等价。

**Non-Goals:**

- 不重命名、不移动任何现有子配置类。
- 不调整 `MpAutoConfiguration` 内部已有的 `@Import`（MpBase/Join/Encrypt/DataScope）。
- 不改变任何子配置类的条件注解或 Bean 定义。
- 不引入 delta spec（纯注册方式重构，无 spec 级行为变化）。

## Decisions

### D1：用 `@Import` 聚合，与项目主流模式一致

**选择**：`MybatisAutoConfiguration` 用 `@Import` 导入 5 个子配置，而非 `@ImportAutoConfiguration`。

**理由**：

- 项目里 20+ 处聚合 AutoConfiguration 均用 `@Import`，`MpAutoConfiguration` 已是同模块成功先例。
- `@Import` 聚合的子配置，其 `@ConditionalOnClass` / `@ConditionalOnProperty` / `@ConditionalOnSingleCandidate` / `@ConditionalOnMissingBean` / `@ConditionalOnBean` 等条件注解仍正常求值。
- 唯一差异：子配置的 `@AutoConfigureAfter` 在 `@Import` 场景下不再生效（不走 `AutoConfigurationImportSelector`），通过 D3 在 `MybatisAutoConfiguration` 上加 `@AutoConfigureAfter` 统一补偿。

**备选**：`@ImportAutoConfiguration`（仅 `TraceAutoConfiguration` 用）会保留子配置的自动配置排序语义，但与项目主流不一致，且本场景通过 D3 补偿已足够。

### D2：`@ConditionalOnClass(name = "org.mybatis.spring.SqlSessionFactoryBean")` 作为 mybatis-spring 前置条件

**选择**：`MybatisAutoConfiguration` 加 `@ConditionalOnClass(name = "org.mybatis.spring.SqlSessionFactoryBean")`。

**理由**：

- 用户明确要求"mybatis-spring 必须导入了"才启用整个 MyBatis 模块。
- `org.mybatis.spring.SqlSessionFactoryBean` 是 mybatis-spring 库的核心类，仅当项目引入 mybatis-spring（或 mybatis-plus-boot-starter / mybatis-flex-spring-boot-starter 等传递依赖）时才存在。
- 用 `name` 字符串形式避免对 mybatis-spring 的编译期硬依赖（fw-plus-boot-autoconfigure 不直接依赖 mybatis-spring）。
- 该条件是 5 个子配置各自 `@ConditionalOnClass` 的合理前置收敛：若 mybatis-spring 不在 classpath，5 个子配置均不会生效，加此条件不改变有效行为。

### D3：`@AutoConfigureAfter` 补偿子配置排序依赖

**选择**：`MybatisAutoConfiguration` 加 `@AutoConfigureAfter(name = {...})`，声明在 DataSource / Mybatis(Plus/Flex) 自动配置之后处理。

**理由**：

- 改用 `@Import` 后，子配置原有的 `@AutoConfigureAfter` 失效：
  - `MpAutoConfiguration` 原 `@AutoConfigureAfter(DataSourceAutoConfiguration, DynamicDataSourceAutoConfiguration)` + `@ConditionalOnSingleCandidate(DataSource.class)`
  - `MybatisDataChangeAutoConfiguration` 原 `@AutoConfigureAfter(MybatisAutoConfiguration(SpringBoot内置), MybatisPlusAutoConfiguration, MybatisFlexAutoConfiguration)` + `@ConditionalOnClass(SqlSessionFactory.class)`
- 在 `MybatisAutoConfiguration` 上统一加 `@AutoConfigureAfter`，保证整体在 DataSource / Mybatis(Plus/Flex) 之后处理，使 `@Import` 的子配置在求值条件时依赖 bean 已就绪。
- 用 `name` 字符串形式，避免对可能不在 classpath 的 `MybatisPlusAutoConfiguration` / `MybatisFlexAutoConfiguration` 产生编译期硬依赖。

### D4：`@Import` 列表顺序保证 MpEncrypt 先于 MfEncrypt

**选择**：`@Import` 列表顺序为 `MpAutoConfiguration` → `MybatisDataChangeAutoConfiguration` → `MpDataChangeAdapterAutoConfiguration` → `MfDataChangeAdapterAutoConfiguration` → `MfEncryptAutoConfiguration`。

**理由**：

- `MfEncryptAutoConfiguration` 注释提到"加解密配置与默认加密器复用现有 Bean（如 `MpEncryptAutoConfiguration` 注册的 `fw.plus.mp-encrypt` 配置）"。
- `MpAutoConfiguration` 已 `@Import(MpEncryptAutoConfiguration.class)`，放在列表首位保证 `MpEncryptAutoConfiguration` 先于 `MfEncryptAutoConfiguration` 处理。
- `MfEncryptAutoConfiguration` 内部用 `ObjectProvider<EncryptorProperties>` 兜底（`getIfAvailable(EncryptorProperties::new)`），即使顺序异常也能用默认值工作，无功能 break 风险。

## Risks / Trade-offs

- **[风险] 子配置 `@AutoConfigureAfter` 失效导致排序差异** → 通过 D3 在 `MybatisAutoConfiguration` 上加 `@AutoConfigureAfter` 统一补偿；`@ConditionalOnSingleCandidate(DataSource.class)` 等条件注解仍生效，提供二次保护。verify 阶段通过编译 + 现有测试（`MpAutoConfigurationTest`、`MybatisDataChangeAutoConfigurationTest`）验证。
- **[风险] `MpEncryptAutoConfiguration` / `MfEncryptAutoConfiguration` 跨类 Bean 依赖顺序** → D4 保证 `@Import` 顺序；`MfEncryptAutoConfiguration` 用 `ObjectProvider` 兜底，无 break 风险。
- **[取舍] 5 个子配置不再单独出现在 `spring.factories`** → 收敛为 1 行是本次目标；`spring.autoconfigure.exclude` 仍对 `@Import` 的类生效，下游排除能力不受影响。
