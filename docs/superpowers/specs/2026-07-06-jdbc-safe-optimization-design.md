# JDBC 自动配置安全优化设计

## 背景

本次优化范围限定在 `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/jdbc` 目录。现有 JDBC 自动配置主要负责接入 dynamic-datasource、Druid 监控配置，以及扩展 `#header`、`#session` 数据源解析器。

## 目标

- 保持现有公开 API、配置前缀、Bean 名称和默认装配行为兼容。
- 修复 Web 上下文、Header、Session、默认数据源配置的空值边界问题。
- 降低启动阶段因空配置或空白配置导致的 NPE 和误注册风险。
- 为关键边界行为补充自动化测试，避免后续回归。

## 非目标

- 不升级 dynamic-datasource、Druid、Spring Boot 或 Maven 依赖版本。
- 不重构模块结构，不迁移包名，不改变 starter 对外使用方式。
- 不引入新的第三方依赖。
- 不调整业务数据源连接池参数语义。

## 设计

### 1. Header / Session 数据源解析器防御性优化

`DsHeaderProcessor` 和 `DsSessionProcessor` 当前直接对 `key`、`RequestContextHolder.getRequestAttributes()`、Session 属性做链式调用，非 Web 线程或缺失属性时可能抛出 NPE。

优化后：
- `matches(String key)` 对 `null` 和空白字符串返回 `false`。
- 非 Servlet 请求上下文返回 `null`，交由后续 processor 或 dynamic-datasource 默认逻辑处理。
- `#header` 后缀为空时返回 `null`。
- `#session` 后缀为空、无 Session、Session 属性不存在时返回 `null`。
- Session 读取使用 `getSession(false)`，避免仅因解析数据源创建新 Session。

### 2. 默认数据源注入边界优化

`DynamicDataSourceAutoConfiguration` 当前在 `spring.datasource.url` 非空时，将 `DefaultDataSourceProperty` 注册到 `properties.getPrimary()` 对应数据源。风险点是空白 URL 会被当成有效配置，primary 为空时也可能出现不可预期 key。

优化后：
- 仅当 `DefaultDataSourceProperty` 存在且 `url` 非空白时注入默认数据源。
- `properties.getDatasource()` 为空时初始化为 `LinkedHashMap`。
- `primary` 为空白时回退到 dynamic-datasource 默认主库名 `master`。
- 已显式配置同名数据源时不覆盖。

### 3. AOP 属性默认值防御

`DynamicDataSourceAopConfiguration` 当前直接使用 `properties.getAop()`，如果属性对象未初始化可能启动失败。

优化后：
- 增加私有方法统一获取 AOP 属性。
- 当 `properties.getAop()` 为空时使用 `new DynamicDatasourceAopProperties()` 默认值。
- 保持注解切面、事务切面创建逻辑和默认开关不变。

### 4. 测试策略

新增单元测试覆盖：
- `DsHeaderProcessor` 的空 key、非 Web 上下文、空后缀、正常 Header 解析。
- `DsSessionProcessor` 的空 key、非 Web 上下文、无 Session、Session 属性缺失、正常属性解析。
- `DynamicDataSourceAutoConfiguration` 构造阶段默认数据源注入、空白 URL 不注入、显式数据源不覆盖。
- `DynamicDataSourceAopConfiguration` 在 AOP 属性为空时仍可创建 Advisor。

## 风险与兼容性

- 返回 `null` 是 dynamic-datasource processor 链常见兜底语义，比抛出 NPE 更符合配置解析链预期。
- 使用 `getSession(false)` 可能改变此前隐式创建 Session 的副作用，但该副作用不应作为数据源解析功能依赖。
- 默认主库名回退为 `master` 与 dynamic-datasource 约定保持一致。

## 验证

- 定向测试：`mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='*Jdbc*Test,*DataSource*Test,*Ds*Test' test -q`
- 静态检查：`git diff --check`
