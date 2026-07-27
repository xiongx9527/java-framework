# Task 6 Report: Magic model-datasource 动态资源与运行态 Registry

## 实现内容

- 新增 `ModelDataSourceInfo`，可转换为 `LangChain4jModelDefinition`，并在 `simple()` 中移除直接敏感字段及 `properties` 中的 `apiKey`、`secretKey`、`accessToken`、`token`、`password`。
- 新增 `MagicDynamicModelDataSource`，运行态模型优先于 Spring Boot registry；删除运行态模型后自动回退 Spring registry。
- 新增 `model-datasource` 资源存储和动态 registry；运行态更新在创建前先销毁同 key 客户端，注销时移除运行态客户端。

## TDD 证据

### RED

先新增两份目标测试。`fw-plus-ai/mvnw` 无法运行，因为 worktree 中缺少 `.mvn/wrapper/maven-wrapper.properties` 与 `maven-wrapper.jar`。使用等价命令：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -Dtest=MagicDynamicModelDataSourceTest,ModelDataSourceMagicDynamicRegistryTest test
```

首次运行在 test compile 阶段失败，原因是 `MagicDynamicModelDataSource`、`ModelDataSourceInfo` 和 registry/storage 类尚不存在。

### GREEN

实现后使用同一条 JDK 8 Maven 命令验证通过：7 tests run，0 failures，0 errors，0 skipped。

## 限制

- 未进行网络调用；全部测试使用 fake factory/registry 或 mock 动态数据源。
- `fw-plus-ai/mvnw` wrapper 文件缺失，无法作为本任务验证入口；已使用指定 JDK 8 的系统 Maven 完成等价聚焦验证。

## 审查修复（2026-07-26）

### 修复内容

- FileEvent 和 GroupEvent 处理失败时记录 warn，仅记录固定上下文与异常类型，不记录异常消息、堆栈或数据源对象，避免凭据泄露。
- 映射节点注册在 runtime client 创建后失败时立即销毁该 key 的 runtime client，避免 Magic 映射与运行态 registry 不一致。
- `toDefinition()` 将直接配置的 `apiKey`、`secretKey`、`accessToken`、`token`、`password` 合并到 definition properties；`simple()` 继续移除直接字段和 properties 中的全部敏感键。
- 同 key 更新测试改为连续两次完整注册，验证每次均为 destroy 后 create。

### 审查修复验证

先新增回滚和凭据传递测试。RED 阶段因测试所需的 `registerMapping` 生命周期边界尚不存在，test compile 失败；实现后执行：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -Dtest=MagicDynamicModelDataSourceTest,ModelDataSourceMagicDynamicRegistryTest test
```

GREEN：9 tests run，0 failures，0 errors，0 skipped。

## 复审修复（2026-07-26，第二轮）

### 修复内容

- `register(ModelDataSourceInfo)` 保持 Magic registry 的 `false` 返回契约，但在捕获异常时以不含实体、异常消息或堆栈的 warn 记录失败信号。
- `unregister(MappingNode)` 的异常处理同样增加不含敏感信息的 warn。
- `ModelDataSourceInfo.copy()` 对显式 `properties: null` 返回空的 `LinkedHashMap`。
- `MagicDynamicModelDataSource.createClient()` 先完成 runtime info copy，再创建并发布模型，避免 copy 失败时残留仅有 model 的运行态条目。

### TDD 与验证

新增 `createClientAcceptsExplicitNullPropertiesWithoutPartialRuntimeState`。RED 阶段该测试在 `ModelDataSourceInfo.copy()` 的空 properties 构造处稳定触发 NPE；实现后执行：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -Dtest=MagicDynamicModelDataSourceTest,ModelDataSourceMagicDynamicRegistryTest test
```

GREEN：10 tests run，0 failures，0 errors，0 skipped。
