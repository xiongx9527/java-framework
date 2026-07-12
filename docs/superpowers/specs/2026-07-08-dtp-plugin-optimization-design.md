# MagicAPI DTP 插件优化设计

## 目标

标准化并严格优化 `magicapi/plugin/dtp`，使 DTP 插件在 Spring Boot 自动配置、MagicAPI 动态资源注册、线程池生命周期和脚本侧使用上保持一致、可诊断、可测试。

本次优化保留现有能力边界：

- 保留配置前缀 `magic-api.plugin.dtp`。
- 保留资源目录 `dtp-datasource` 和 `.json` 格式。
- 保留脚本侧 `import dtp`、`dtp.execute(...)`、`dtp['key']` 的使用方式。
- 不引入新依赖，不升级 DynamicTp、MagicAPI 或 Spring Boot 版本。
- 目标代码保持 Java 8 兼容。

## 当前问题

`MagicAPIDtpConfiguration.controllerRegister()` 直接调用 `magicDynamicDtpDataSource()` 方法创建对象，在 `@Configuration` 未启用 `proxyBeanMethods = true` 语义时容易绕开 Spring 容器，导致控制台测试和运行时注册使用不同的数据源管理器。

DTP 默认 bean 覆盖点不完整，`DtpModule`、`DtpDataSourceInfoMagicResourceStorage` 等 bean 缺少完整的 `@ConditionalOnMissingBean`，不利于业务侧替换。

动态线程池配置校验分散且不足。`corePoolSize`、`maximumPoolSize`、`queueCapacity`、`queueType`、`rejectedPolicy`、`other` 等字段缺少统一约束，错误配置可能在运行期以不清晰异常暴露。

数据源更新流程先销毁旧线程池再创建新线程池。新配置构建失败时，旧线程池已经被移除，导致原本可用的数据源被破坏。

注册器吞掉异常，文件事件、分组事件、注册和注销失败时缺少足够日志，不利于定位资源加载失败原因。

脚本侧指定不存在的数据源时会回退默认线程池。这个行为容易隐藏配置错误，使任务跑到错误线程池。

模块方法对空任务列表返回 `null`，调用方容易出现 NPE。

当前没有 DTP 插件测试，缺少自动配置、资源校验、生命周期和脚本模块行为的回归保护。

## 推荐方案

采用标准化重构方案，保留外部资源格式和脚本 API，修正不合理默认行为，补齐测试。

### 自动配置

`MagicAPIDtpConfiguration` 使用 `@Configuration(proxyBeanMethods = false)`，并通过字段或构造参数持有容器内的 `MagicDynamicDtpDataSource`，确保控制器注册使用同一个数据源管理器。

所有默认 bean 增加覆盖点：

- `MagicDynamicDtpDataSource`
- `DtpExecutor`
- `DtpModule`
- `DtpDataSourceInfoMagicResourceStorage`
- `DtpDataSourceMagicDynamicRegistry`

默认 `DtpExecutor` 保持从 `MagicAPIDtpConfig` 读取参数，兼容现有配置结构。配置读取逻辑可以保留在 `BaseService`，但需要通过校验统一约束。

### 动态数据源

在 `MagicDynamicDtpDataSource` 中集中处理线程池构建：

- `key` 必须非空。
- `corePoolSize` 必须大于 0。
- `maximumPoolSize` 必须不小于 `corePoolSize`。
- `keepAliveTime` 必须不小于 0。
- `queueCapacity` 对有界队列必须大于 0。
- `queueType` 仅允许支持的类型。
- `rejectedPolicy` 仅允许支持的策略。
- `other` 非空时必须是 JSON Object。

更新数据源时先构建新 `DtpExecutor`，构建成功后再替换 `clientMap` 和 `infoMap`，最后销毁旧 executor。这样新配置错误不会破坏旧线程池。

销毁方法保持幂等，单个 executor 销毁失败只记录日志，不阻断后续清理。

`datasourceNodes()` 返回动态数据源和默认数据源基础信息，避免空 key 和重复 key。

### 资源注册

`DtpDataSourceMagicDynamicRegistry` 保持监听 `dtp-datasource` 的 `FileEvent` 和 `GroupEvent`。

事件处理、注册和注销不再静默吞异常：

- 事件处理失败记录事件类型和异常。
- 注册失败记录数据源 key 和异常。
- 注销失败记录数据源 key 和异常。

`register(entity)` 和 `register(mappingNode)` 的数据源生命周期顺序统一：先构建并替换线程池，再注册映射；失败时不污染已有 `clientMap/infoMap`。

### 资源校验

`DtpDataSourceInfoMagicResourceStorage.validate()` 继续负责资源级校验：

- `name` 必填。
- `key` 必填且符合文件名约束。
- `key` 不可重复。
- 已存在资源的 `key` 不可修改。
- `other` 必须是 JSON Object。
- 线程池核心参数合法。

资源 storage 的校验和动态数据源构建校验可以复用同一套规则，避免保存成功但运行失败。

### 脚本模块

`DtpModule` 保持现有公开方法：

- `execute(Runnable command)`
- `allOfWithAllResults(List<Supplier<T>> tasks)`
- `anyOfWithFirstResult(List<Supplier<T>> tasks)`
- `anyOfWithoutResult(List<Runnable> tasks)`
- `getDynamicModule(MagicScriptContext context)`
- `getDynamicAttribute(String key)`

行为调整：

- 指定的数据源 key 不存在时抛出清晰异常，不再回退默认线程池。
- 空任务列表返回已完成 Future 或空集合结果，不返回 `null`。
- 默认数据源未显式指定时仍使用默认 `DtpExecutor`。

`MagicApiFuncTask` 和 `MagicApiScriptTask` 失败时继续返回空 Map，以兼容现有脚本；日志补充函数名或脚本片段上下文。

## 测试方案

新增测试目录：

`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/dtp`

测试覆盖：

- `MagicAPIDtpConfigurationTest`
  - 插件禁用时不创建 DTP bean。
  - 插件启用时创建默认数据源、默认 executor、module、storage、registry。
  - 用户自定义 bean 时自动配置 backoff。
  - `controllerRegister()` 使用容器内同一个 `MagicDynamicDtpDataSource`。

- `MagicDynamicDtpDataSourceTest`
  - 合法配置可构建 executor。
  - 非法 `corePoolSize`、`maximumPoolSize`、`queueCapacity`、`queueType`、`rejectedPolicy` 被拒绝。
  - 更新失败不破坏旧 executor。
  - `destroy()` 会清空动态数据源。

- `DtpModuleTest`
  - 未指定 key 时使用默认 executor。
  - `dtp['key']` 命中动态 executor。
  - 不存在 key 抛出清晰异常。
  - 空任务列表返回非 null 完成结果。

- `DtpDataSourceInfoMagicResourceStorageTest`
  - 必填字段校验。
  - 重复 key 校验。
  - key 不可修改。
  - 非法 JSON 和线程池参数校验。

验证命令：

```bash
cd fw-plus-boot
mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='*Dtp*Test' test
mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='*Dtp*Test,*MagicAPI*Test' test
```

## 风险和兼容性

指定不存在数据源时由回退默认线程池改为抛异常，这是有意的行为修正。它能避免生产任务静默跑错线程池，但如果已有脚本依赖错误 key 自动回退，需要修正脚本配置。

资源格式不变，已有 `dtp-datasource/*.json` 不需要迁移。

默认配置前缀不变，已有 `magic-api.plugin.dtp.*` 配置继续生效。

不新增依赖，不改变 Maven 依赖版本。

## 自检

- 没有跨出 DTP 插件及其直接测试范围。
- 没有引入 Java 9+ API。
- 没有改变 MagicAPI 资源目录和脚本导入方式。
- 行为变化只有一项：指定不存在 DTP 数据源时不再回退默认线程池。
- 每个风险点都有对应测试计划。
