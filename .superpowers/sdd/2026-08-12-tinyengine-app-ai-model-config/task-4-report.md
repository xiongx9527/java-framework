# Task 4 报告：MVC/WebFlux 共享 AI 模型配置 API

## 状态

DONE

## 变更文件

- 修改 `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/api/TinyEngineApiFacade.java`，增加九个应用 AI 模型配置操作。
- 修改 `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/api/DefaultTinyEngineApiFacade.java`，逐项委托 `AppAiModelConfigService`。
- 修改 `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/TinyEngineApiConfiguration.java`，注入配置服务并将其加入宿主服务就绪条件。
- 修改 MVC/WebFlux `TinyEngineApp*Controller`，提供 `/app-center/api/apps/{appId}/ai-model-services` 的九条同构路由。
- 新增 MVC 与 WebFlux 合同测试。

## 红测证据

首次运行指定 Boot 测试时，本地 Maven 快照未包含 Task 3 DTO，测试编译失败；先执行 Core 安装以同步当前工作树的 Task 3 产物：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home \
mvn -f fw-plus-core/pom.xml install \
  -pl fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core -am -DskipTests
```

随后按合同重跑：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home \
mvn -f fw-plus-boot/pom.xml test \
  -pl fw-plus-boot-project/fw-plus-boot-autoconfigure \
  -Dtest=TinyEngineAiModelMvcApiContractTest,TinyEngineAiModelWebFluxApiContractTest
```

结果：`BUILD FAILURE`。两份测试均因 `TinyEngineApiFacade` 缺少 `usableAiModelServices`、管理、服务/模型 CRUD 与导入等九个操作而在测试编译失败，证明合同在实现前不能成立。

## 绿测与回归

实现后执行：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home \
mvn -f fw-plus-boot/pom.xml test \
  -pl fw-plus-boot-project/fw-plus-boot-autoconfigure \
  -Dtest=TinyEngineAiModelMvcApiContractTest,TinyEngineAiModelWebFluxApiContractTest,TinyEngineAppPageMvcApiContractTest,TinyEngineWebFluxApiContractTest,TinyEngineAutoConfigurationTest
```

结果：`BUILD SUCCESS`，`Tests run: 42, Failures: 0, Errors: 0, Skipped: 0`。

- MVC 合同测试覆盖九条路由、统一 `Result.data` 响应、`apiKey` 不序列化及 Facade 到 Core 的逐项委托。
- WebFlux 合同测试覆盖同构路由，且首个 Facade 调用断言在 `tiny-engine-ai-model-test` blocking scheduler 上运行。
- 既有 MVC 应用/成员合同、WebFlux 合同和自动配置测试均通过。

## 自审

- 九条路由严格对应设计文档：可用列表、管理列表、服务 CRUD、模型 CRUD 与一次性导入；`resolveInvocation` 和 `deleteByApp` 是内部调用/应用生命周期操作，不暴露为 HTTP 管理接口。
- Controller 不读取角色、持久化实体或 API Key；权限、租户隔离与安全 DTO 均保留在 Core 服务中。
- `AiModelServiceDto` 不含 `apiKey` 字段，合同通过实际 HTTP 序列化验证。
- WebFlux 每个新增路径均经 `TinyEngineWebFluxSupport.submit`，不复制阻塞线程、超时或错误转换逻辑。
- `git diff --check` 通过；新增代码兼容 Java 8，未引入 Java 9+ API。

## 提交

实现提交：`1b23df6a`（`TinyEngine: 暴露应用 AI 模型配置接口`）。

## 关注点

- 验证前需要将当前 Task 3 Core 模块安装到本地 Maven 仓库；这是四层独立 Maven reactor 的正常依赖准备步骤，不属于代码缺陷。
