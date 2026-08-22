## Why

`fw-plus-boot-autoconfigure` 模块中，MyBatis-Plus 与 MyBatis-Flex 的自动配置类分散在 `autoconfigure.mp` 和 `autoconfigure.mf` 两个并列子包中，而 MyBatis 通用能力（如数据变更追踪）已经位于 `autoconfigure.mybatis.datachange` 子包下。这种"按 ORM 框架分顶层包、再与通用 mybatis 包并列"的布局，使得 MyBatis 生态的自动配置类分散在三个顶层包中，缺乏统一的命名边界，不利于查找和维护。

将 `mp`、`mf` 两个子包合并到 `mybatis` 包下，可以把整个 MyBatis 生态的自动配置类收敛到单一命名空间，与 fw-plus 中已有的 `mybatis.encrypt`、`mybatis.datachange` 等核心模块包名保持一致，也使后续按功能维度（加密、数据变更适配、连表查询等）做进一步重构更自然。

## What Changes

> **Implementation Divergence（2026-08-09）**：初始方案描述为"扁平合并到 `mybatis` 根包"，实现阶段用户明确要求改为"在 `mybatis` 下创建 `mp` 和 `mf` 两个子包"，以保留按 ORM 框架的细粒度目录边界。实际结果如下（design.md D1 节同步记录了本次决策漂移）。

- 将 `autoconfigure.mp` 包下的 7 个自动配置类（`MpAutoConfiguration`、`MpBaseAutoConfiguration`、`MpDataChangeAdapterAutoConfiguration`、`MpEncryptAutoConfiguration`、`MpDataScopeAutoConfiguration`、`MpJoinAutoConfiguration`、`MpJoinProperties`）移动到 `autoconfigure.mybatis.mp` 子包下，类名保持不变。
- 将 `autoconfigure.mf` 包下的 2 个自动配置类（`MfDataChangeAdapterAutoConfiguration`、`MfEncryptAutoConfiguration`）移动到 `autoconfigure.mybatis.mf` 子包下，类名保持不变。
- 同步更新 `META-INF/spring.factories` 和 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 中 4 处类的全限定名引用（加入 `.mp.` / `.mf.` 段）。
- 移动 `MpAutoConfigurationTest` 测试类到 `autoconfigure.mybatis.mp` 子包下，并更新其 `package` 声明（对生产类 import 保持同包相对引用，无需修改）。
- 更新 `MybatisDataChangeAutoConfigurationTest` 中对 `MfDataChangeAdapterAutoConfiguration`、`MpDataChangeAdapterAutoConfiguration` 的 2 处 import 路径（改为 `.mybatis.mf.` 和 `.mybatis.mp.`）。
- **BREAKING**：`com.xiongx9527.fw.plus.boot.autoconfigure.mp` 和 `com.xiongx9527.fw.plus.boot.autoconfigure.mf` 两个包消失，新包为 `com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.mp` 和 `com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.mf`。这两个包属于 fw-plus 内部自动配置实现，不对外暴露 public API；下游项目通过 Spring Boot autoconfigure 机制触发，无需显式 import 这些类。但若有下游项目显式引用了这些类的全限定名（例如在自定义 `@Import` 或排除配置中），需要同步更新路径。

## Capabilities

### New Capabilities

无。本次变更为纯包路径重构，不引入任何新的能力或行为。

### Modified Capabilities

无。本次变更不改变任何 spec 级别的行为或验收场景，已通过 `.openspec.yaml` 设置 `skip_specs: true`。

## Impact

- **受影响代码**：`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure` 单一 Maven 模块。
  - 9 个 main 源文件移动（mp 7 个 + mf 2 个）
  - 1 个测试文件移动（`MpAutoConfigurationTest`）
  - 1 个测试文件修改 import（`MybatisDataChangeAutoConfigurationTest`）
  - 2 个资源文件修改（`spring.factories`、`AutoConfiguration.imports`）
- **类内部 import**：所有被移动类的内部 import 均为对 fw-plus-core / 第三方依赖的引用，与所在包无关，无需修改。
- **外部引用**：仅 4 处自动配置注册 + 2 处测试 import 需要同步更新；仓库内无其他对 `autoconfigure.mp` / `autoconfigure.mf` 包路径的引用。
- **依赖与 API**：不涉及 Maven 依赖变化，不改变 Spring Bean 注册顺序、条件注解和 Bean 名称，运行时行为完全等价。
- **下游影响**：仅当下游项目显式按全限定名引用这两个包中的类时才会受影响；通过 autoconfigure 机制使用的项目无感知。
