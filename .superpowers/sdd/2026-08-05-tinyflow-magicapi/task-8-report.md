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
- `TinyflowModule` 与节点 provider registry 位于后置
  `MagicAPITinyflowRuntimeAutoConfiguration`，避免前置配置在 Tinyflow core service
  缺失时创建运行时 Bean。model/vector bridge 保持在前置配置，以便在
  `TinyflowAutoConfiguration` 创建默认 provider 前接管并合并动态数据源。

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
