## Design

保持 `fw-plus-mp-encrypt` 现有模块结构与公开 API 不变，只补测试、文档与 spec 验收场景：

### 测试策略

- 依赖：`pom.xml` 增加 test scope 依赖 `junit-jupiter`、`assertj-core`、`h2`（版本由 `fw-plus-dependencies` BOM 管理，与 `fw-plus-mp-datachange` 一致）。
- 单元测试（JUnit 5 + AssertJ）：
  - `FieldProcessorTest`：反射字段解析、`@FieldEncrypt` 类型约束（非 String 抛 `BaseRuntimeException`）、`@TableField(value)` 列名映射、继承字段、`HashMap` 跳过、解析缓存。
  - `FieldEncryptorHandlerTest`：`handleEncrypt` 仅处理 INSERT/UPDATE、Map/非 Map 参数遍历、`getEncryptor` 默认与自定义加密器缓存复用；`handleDecrypt` 结果集解密。
  - `FieldEncryptInterceptorTest` / `FieldDecryptInterceptorTest`：MyBatis `@Signature` 与 `plugin` 包装行为（Executor / ResultSetHandler）。
  - `EncryptorPropertiesTest`：全局 salt/algorithm 与注解值的优先级（沿用 `DefaultEncryptorProperties` 语义）。
- 端到端测试（H2）：构造真实 `SqlSessionFactory`，注册两个拦截器，定义带 `@FieldEncrypt` 的实体与 Mapper，验证 INSERT 落库为密文、SELECT 返回明文。

### 文档

- 新增 `fw-plus-mp-encrypt/README.md`：模块用途、配置前缀 `fw.plus.mp-encrypt`（enabled/密钥/IV/盐/算法）、`@FieldEncrypt` 注解属性、支持的算法、自定义 `IEncryptor` 扩展方式。
