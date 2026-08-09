## Why

`fw-plus-boot-autoconfigure` 中 MyBatis 生态的 5 个自动配置类（`MpAutoConfiguration`、`MybatisDataChangeAutoConfiguration`、`MpDataChangeAdapterAutoConfiguration`、`MfDataChangeAdapterAutoConfiguration`、`MfEncryptAutoConfiguration`）各自独立注册在 `spring.factories` / `AutoConfiguration.imports` 中，缺乏统一的模块入口。这与 fw-plus 中其他模块的聚合模式（`MpAutoConfiguration` 已 `@Import` 4 个 mp 子配置、`MqttAutoConfiguration` 聚合 server/client、`MateAutoConfiguration` 聚合 easytrans/webmvc 等）不一致。

新建顶层 `MybatisAutoConfiguration` 作为 MyBatis 生态的统一聚合入口，把这 5 个子配置收敛到单一注册项，使 `spring.factories` 中 MyBatis 模块从 5 行收敛为 1 行，与项目主流聚合模式对齐。

## What Changes

- 新建 `com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.MybatisAutoConfiguration` 聚合配置类：
  - `@ConditionalOnClass(name = "org.mybatis.spring.SqlSessionFactoryBean")`：仅当 classpath 存在 mybatis-spring 时启用整个 MyBatis 模块
  - `@Import` 聚合 5 个子配置（顺序：`MpAutoConfiguration` → `MybatisDataChangeAutoConfiguration` → `MpDataChangeAdapterAutoConfiguration` → `MfDataChangeAdapterAutoConfiguration` → `MfEncryptAutoConfiguration`）
  - `@AutoConfigureAfter` 声明在 DataSource / Mybatis(Plus/Flex) 自动配置之后处理，补偿子配置改用 `@Import` 后 `@AutoConfigureAfter` 失效的排序依赖
- 修改 `META-INF/spring.factories`：将 mybatis 相关 5 行注册替换为 1 行 `com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.MybatisAutoConfiguration`
- 修改 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`：同步上述 5 行 → 1 行替换
- **BREAKING**：5 个子配置类不再单独注册在 `spring.factories` / `AutoConfiguration.imports` 中，改为通过 `MybatisAutoConfiguration` 的 `@Import` 聚合导入。这些类属于 fw-plus 内部自动配置实现，不对外暴露 public API；通过 Spring Boot autoconfigure 机制使用的项目无感知。`spring.autoconfigure.exclude` 对 `@Import` 的类仍生效，下游排除能力不受影响。

## Capabilities

### New Capabilities

无。本次变更为自动配置注册方式重构，不引入新能力。

### Modified Capabilities

无。本次变更不改变任何 spec 级别的行为或验收场景，已通过 `.openspec.yaml` 设置 `skip_specs: true`。最终注册的 Bean 集合与条件求值结果与改动前等价。

## Impact

- **受影响代码**：`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure` 单一 Maven 模块。
  - 1 个新建 Java 文件（`mybatis/MybatisAutoConfiguration.java`）
  - 2 个资源文件修改（`spring.factories`、`AutoConfiguration.imports`）
- **运行时行为**：Bean 注册顺序、条件注解、Bean 名称保持等价。子配置原有的 `@AutoConfigureAfter` 改由 `MybatisAutoConfiguration` 的 `@AutoConfigureAfter` 统一补偿。
- **依赖与 API**：不涉及 Maven 依赖变化。
- **下游影响**：通过 autoconfigure 机制使用的项目无感知；显式引用子配置类的下游项目行为不变。
