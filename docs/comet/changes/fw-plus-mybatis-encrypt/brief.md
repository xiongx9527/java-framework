# Outcome

参照 `fw-plus-mybatis-datachange` 的三层模式，为字段加解密建立框架无关的通用核心 `fw-plus-mybatis-encrypt`，并把现有 `fw-plus-mp-encrypt` 瘦身为 MyBatis-Plus 适配层，保留现有字段加解密能力与 Boot 自动配置集成。

# Scope

- 新建 `fw-plus-mybatis-encrypt`（框架无关，仅依赖 MyBatis + common，不依赖 MyBatis-Plus/Flex/Spring）。
- `fw-plus-mp-encrypt` 改为 MP 适配层（只保留 MP 特有元数据/列名映射）。
- **新增 `fw-plus-mf-encrypt`**（MyBatis-Flex 适配层，含 Flex 字段元数据解析 + 适配器 + 测试），与 datachange 完全对称。
- 能力边界：**写入加密**（INSERT/UPDATE 参数加密，`Executor.update` 拦截）+ **查询解密**（SELECT 结果集解密，`ResultSetHandler.handleResultSets` 拦截），双向都保留，MP 与 Flex 各自端到端验证。
- Boot 自动配置（`MpEncryptAutoConfiguration`）与 `@FieldEncrypt` 注解、`IEncryptor` 契约保持不变。
- 迁移/补充测试与文档（含查询解密验收）。

# Non-goals

- 不改变 `@FieldEncrypt` 注解、`IEncryptor` 接口、`DefaultEncryptor` 的语义与契约。
- 不涉及 HTTP 层加解密（`boot-common` 的 `MyHttpEncryptorProperties` 为独立能力）。
- 不新增加密算法，不改变 `fw.plus.mp-encrypt` 配置语义。

# Acceptance examples

- `fw-plus-mybatis-encrypt` 的 pom 不含 MP/Flex/Spring 依赖（仅 mybatis + common/annotation）。
- MP 端到端：INSERT 落库为密文，SELECT 返回明文（H2）。
- MF 端到端：Flex 实体 INSERT 落库为密文，SELECT 返回明文（H2）。
- 现有 `fw-plus-mp-encrypt` 的 25 个测试迁移后全部通过。
- `MpAutoConfigurationTest`（boot）继续通过；MP 自动配置仍注册加密/解密拦截器。

# Constraints and invariants

- Java 8 兼容（fw-plus-core 约束）。
- 公共核心不得引用 MyBatis-Plus、MyBatis-Flex、Spring。
- 现有 `fw.plus.mp-encrypt` 配置前缀与公开类保持可用。

# Decisions

- D1: 模块拓扑与 datachange 完全对称——通用核心 `fw-plus-mybatis-encrypt` + MP 适配 `fw-plus-mp-encrypt` + **MF 适配 `fw-plus-mf-encrypt`**（用户确认，即使当前 MF 无加密使用者）。
- D2: 能力边界保留写入加密 + 查询解密双向（用户确认）。
- D3（设计决定）: 按 datachange 模式重写，拦截器/处理器上移通用核心；`fw-plus-mp-encrypt` 只保留 MP 元数据适配与兼容重导出；boot 自动配置同步调整。
- D4（设计决定）: 列名映射通过 SPI（FieldNameResolver 类契约）由 MP/Flex 适配器提供；通用核心反射解析 `@FieldEncrypt` 与类型约束，不依赖 `@TableField`/Flex 注解。

# Open questions


# Verification expectations

- `fw-plus-core` 全量 `mvn test`（至少 encrypt 相关模块）通过。
- MP 与 MF 各自的 H2 端到端测试通过。
- boot `MpAutoConfigurationTest` 通过。
