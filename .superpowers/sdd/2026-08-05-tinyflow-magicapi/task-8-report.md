# Task 8 Report: MagicScript 执行与编辑器后端

## 交付内容

- 新增 `TinyflowModule` 与 `TinyflowResourceModule`：MagicScript 可通过
  `tinyflow[path].execute(variables)`、`executeAsync(variables)` 和
  `tinyflow.resume(instanceId, variables)` 调用同一个 `TinyflowService`。
  路径解析只查询已注册的 `TinyflowWorkflowRepository`；不存在、歧义和未知实例
  均返回稳定的 Tinyflow 异常。
- 新增 `MagicTinyflowController`：提供 resource get/save/validate、同步 run、
  run-async、resume、CHAT/EMBEDDING/vector/nodes 描述接口。run 与 run-async
  只接收已保存的 `resourceId`，服务端先由 `MagicResourceService.file(id)` 读取
  `TinyflowResource`，不接受浏览器传入的工作流 JSON。
- 保存前调用 `TinyflowMagicResourceStorage.validate`。没有提前以空 id 调用
  `TinyflowService.validate`，因此 CREATE 可继续由 `MagicResourceService.saveFile`
  生成 id 并触发既有资源动态注册。
- 新增 Magic 动态 model/vector 到 Tinyflow provider 的桥接。桥接复用
  `MagicDynamicModelDataSource` 与 `MagicDynamicVectorDataSource`，两者均包含
  Spring registry 和 Magic 运行时数据源；接口只返回 Tinyflow descriptor，未返回
  `apiKey`、`secret`、`headers` 或内部 Bean。节点 properties 还会过滤这些敏感键。
- `TinyflowModule` 位于后置 `MagicAPITinyflowRuntimeAutoConfiguration`，避免前置配置在
  Tinyflow core service 缺失时创建 Magic 运行时 Bean。节点 provider registry 位于通用
  `TinyflowAutoConfiguration`，确保解析器在校验和执行服务创建前已装配；model/vector
  bridge 保持在前置配置，以便在默认 provider 创建前接管并合并动态数据源。

## Magic-API 2.2.2 实际接口核对

通过本地 `javap` 核对的签名如下：

- `DynamicModule<T>.getDynamicModule(MagicScriptContext)`；为脚本路径访问额外实现
  `DynamicAttribute`，并提供按 path 的重载。
- `MagicControllerRegister.register(Mapping, MagicConfiguration)`；控制器用现有
  `mapping.registerController(new MagicTinyflowController(configuration))` 注册，继承
  `MagicController`，沿用 Magic 编辑器控制器路径。
- `MagicResourceService.saveFile(T)` 与 `file(id)`；计划示例中的通用 save 表述已按
  实际 `saveFile` 实现。
- `JsonBean<T>` 构造 `new JsonBean<T>(data)`；其 `getData()` 来自 map 基类，测试中
  按实际返回类型进行受控转换。

## TDD 证据

先新增 `TinyflowModuleTest`、`MagicTinyflowControllerTest`、
`MagicApiTinyflowProviderTest`，随后执行：

```text
mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -Dtest=TinyflowModuleTest,MagicTinyflowControllerTest,MagicApiTinyflowProviderTest test
```

RED：testCompile 因 `tinyflow.module` 和 `MagicTinyflowController` 未创建失败。

GREEN：同一命令在 Zulu JDK 8 下通过，8 tests，0 failures，0 errors，0 skipped。

受影响回归：

```text
mvn -Dstyle.color=never -f fw-plus-ai/pom.xml \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -Dtest=TinyflowMagicResourceStorageTest,TinyflowResourceDynamicRegistryTest,\
TinyflowAutoConfigurationTest,MagicDynamicVectorDataSourceTest,MagicModelControllerTest test
```

通过，48 tests，0 failures，0 errors，0 skipped。

## 静态检查

- `git diff --check` 通过。
- 新增生产 class 的 `javap -verbose` 均为 `major version: 52`。
- 新增源码未使用 Java 9+ 集合工厂或不可变集合 API。

## Fix Round 1

### RED

新增权限、模型能力和自定义节点回归用例后执行：

```text
mvn -Dstyle.color=never -f fw-plus-ai/pom.xml \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -Dtest=MagicTinyflowControllerTest,MagicApiTinyflowProviderTest,TinyflowAutoConfigurationTest test
```

初始结果为 testCompile 失败：`MagicTinyflowController` 的管理方法尚未接收
`MagicHttpServletRequest`，因此不能在接口调用路径中执行 Magic-API 授权校验。

### 修复内容

- 所有 Tinyflow 管理接口均添加 `@Valid(requireLogin = true, authorization = ...)`，并在
  控制器中显式执行全局权限校验。resource get 在加载资源后使用实体 `VIEW`；save、
  validate 以请求中的 transport resource 使用实体 `SAVE`；run 和 run-async 以已保存
  resource 使用实体 `SAVE`；resume 在无法定位 resource 时至少校验全局 `SAVE`。权限异常
  位于业务失败响应的捕获区之外，避免被伪装为普通执行失败。
- `MagicApiTinyflowModelProvider` 使用同一 native capability predicate 过滤列表与解析：
  CHAT 只接受实际 `ChatLanguageModel`，EMBEDDING 只接受实际 `EmbeddingModel`，普通
  `AiModel` 不会出现在可选列表中。
- `TinyflowNodeProviderRegistry` 前移到通用 `TinyflowAutoConfiguration`，并以
  `ChainParser.builder().addParsers(registry.getParsers()).build()` 构造解析器。Magic
  后置配置不再重复定义 registry，使 provider 选择在校验和执行服务创建前完成。
- 节点描述的 properties 改为递归复制并过滤大小写无关的密码、token、authorization、
  credential、accessKey、privateKey、apiKey、secret、headers 和 bean 等敏感键；嵌套
  Map/List 同样处理，且不修改 provider 原始对象。

### GREEN 与回归

精确回归：

```text
mvn -Dstyle.color=never -f fw-plus-ai/pom.xml \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -Dtest=MagicTinyflowControllerTest,MagicApiTinyflowProviderTest,TinyflowAutoConfigurationTest test
```

通过，19 tests，0 failures，0 errors，0 skipped。

受影响回归：

```text
mvn -Dstyle.color=never -f fw-plus-ai/pom.xml \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -Dtest=TinyflowModuleTest,TinyflowMagicResourceStorageTest,TinyflowResourceDynamicRegistryTest,\
MagicTinyflowControllerTest,MagicApiTinyflowProviderTest,TinyflowAutoConfigurationTest,\
MagicDynamicModelDataSourceTest,MagicDynamicVectorDataSourceTest,MagicModelControllerTest test
```

通过，78 tests，0 failures，0 errors，0 skipped。

`javap -verbose` 已确认本轮四个变更生产 class 均为 `major version: 52`，并执行
`git diff --check`。
