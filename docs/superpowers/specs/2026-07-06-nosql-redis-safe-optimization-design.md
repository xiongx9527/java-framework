# NoSQL Redis 自动配置安全优化设计

## 背景

`fw-plus-boot-autoconfigure` 的 `nosql` 目录当前主要承载 Redis 动态多数据源能力，包括自动配置、数据源注册、动态 `RedisTemplate`、条件注解、上下文与 helper 工具类。当前目录下没有对应的 `src/test/java/.../nosql` 测试覆盖。

本次优化目标是做“安全小步优化”：不改变外部 API，不新增功能，只提升稳定性、可维护性和可测试性。

## 目标

- 保持现有配置前缀、Bean 名称、公开类和公开方法兼容。
- 增加关键路径的空值保护和参数校验，减少启动期或运行期 NPE。
- 收敛明显重复逻辑和原始类型使用，使代码更易维护。
- 补充最小单元测试，覆盖优化点和已有边界行为。
- 不引入 Java 9+ API，保持 Java 8 兼容。

## 非目标

- 不重构整体 Redis 多数据源架构。
- 不改变 `RedisTemplate` 的序列化策略或动态路由语义。
- 不新增动态刷新、健康检查、连接销毁等新能力。
- 不修改 MagicAPI 插件相关代码。

## 推荐方案

采用小步、可回归的局部优化，优先处理以下区域：

### 1. 条件注解健壮性

目标文件：
- `redis/condition/OnExistingPropertyCondition.java`
- `redis/condition/OnMissingPropertyCondition.java`

优化点：
- 移除依赖 JVM `assert` 的注解属性读取。
- 对注解属性缺失或空数组给出确定的 `ConditionOutcome`。
- 统一 `ConditionMessage` 使用对应注解类，避免日志误导。

测试：
- 验证属性存在/缺失时的 match/noMatch 行为。
- 验证空字符串不被当作有效配置。

### 2. Redis 数据源上下文安全性

目标文件：
- `redis/RedisDataSourceContext.java`
- `redis/client/RedisMultiSourceClient.java`

优化点：
- 对空数据源 key、未注册数据源、空连接工厂做防御性处理。
- 返回值和异常信息更清晰，便于定位配置问题。
- 保持现有调用方式不变。

测试：
- 未注册 key 的行为稳定可断言。
- 注册/获取路径覆盖基础分支。

### 3. 动态模板工厂边界校验

目标文件：
- `redis/config/DynamicRedisTemplateFactory.java`
- `redis/config/RedisConnectionConfiguration.java`

优化点：
- 校验必要依赖和配置对象，避免隐式 NPE。
- 对 Jedis/Lettuce 创建路径保持现有行为，仅补错误信息。
- 不改变默认客户端选择策略。

测试：
- 配置缺失时给出明确异常。
- 默认路径不破坏现有构造逻辑。

### 4. Helper 清理与泛型改进

目标文件：
- `redis/helper/RedisHelper.java`
- `redis/helper/EasyRedisHelper.java`
- `redis/helper/DynamicRedisHelper.java`

优化点：
- 清理原始类型和重复代码。
- 对 key、value、timeout 等明显非法入参做轻量防御。
- 保持方法签名兼容，避免影响下游调用。

测试：
- 优先覆盖纯逻辑和参数校验；需要真实 Redis 的行为不在本次测试范围内。

## 实施顺序

1. 先补 condition 与上下文类的单元测试，建立安全网。
2. 小范围修改 condition 和 context/client 类。
3. 再处理 factory/configuration 的空值校验。
4. 最后做 helper 的低风险泛型和重复逻辑清理。
5. 运行 autoconfigure 模块 nosql/redis 相关定向测试。

## 风险与控制

- 风险：过度校验可能改变启动容错行为。控制：只对原本会 NPE 的路径改为明确异常或 noMatch，不改变正常路径。
- 风险：helper 方法可能被外部依赖。控制：不改公开方法签名，不改返回语义。
- 风险：没有真实 Redis 环境。控制：单元测试使用 mock/fake，只覆盖配置和纯逻辑边界。

## 验收标准

- 新增或更新的单元测试通过。
- `fw-plus-boot-autoconfigure` nosql/redis 相关测试通过。
- `git diff --check` 通过。
- 不出现公开 API 破坏性变更。
