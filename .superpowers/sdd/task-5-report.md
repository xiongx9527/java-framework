# Task 5 实施报告：AI Boot 与 Magic-API 中立化

## 状态

DONE

## 实施范围

- 新增 `fw-plus.ai.model` 与 `fw-plus.ai.embedding-store` 中立配置入口。
- 保留 `fw-plus.ai.langchain4j.model` 与 `fw-plus.ai.langchain4j.embedding-store` 兼容映射；同 key 冲突时中立配置优先。
- 冲突 warning 仅包含 `key/framework/provider`，不记录 properties、API key 等凭证。
- 新增 `fw-plus-ai-model-spring-boot-starter` 与 `fw-plus-ai-store-embedding-spring-boot-starter`；LangChain4j starter 依赖对应中立 starter。
- Magic-API model/vector 的自动配置、动态数据源和脚本模块改用中立 registry、factory、adapter 与 facade。
- 保留标记为 `@Deprecated` 的旧构造器源码兼容桥接，但生产 Bean 签名和脚本返回值不再泄漏 LangChain4j 原生模型或原生向量库。

## TDD 证据

### RED

聚焦兼容测试先覆盖以下缺口：

- 中立配置 Bean、旧配置映射与同 key 优先级尚未实现。
- Magic-API model/vector 仍依赖 LangChain4j registry/factory 和原生对象签名。
- 中立 starter 尚不存在，LangChain4j starter 依赖关系不满足预期。

完成生产改造后首次运行完整 `fw-plus-ai-boot-autoconfigure` 测试，97 个测试中仍有 14 个失败。失败均来自 4 个旧测试类继续断言原生 LangChain4j 对象 identity 或旧异常类型；生产契约已切换为中立 facade，因此将这些旧断言最小迁移为中立元数据、默认 key、运行时覆盖和销毁回退断言，没有恢复原生对象泄漏。

首轮只读审查又发现两个 JUnit 4 历史类未被 JUnit Platform 自动发现。将 `VectorModuleTest` 与 `MagicVectorControllerTest` 迁移到 JUnit 5 后，它们分别进入常规 Surefire，旧原生 identity 断言按中立 facade 契约迁移。

审查修复 RED 使用 Zulu JDK 8：

```bash
./fw-plus-ai/mvnw -f fw-plus-ai/fw-plus-ai-project/pom.xml \
  -pl fw-plus-ai-boot-autoconfigure \
  -Dtest=LangChain4jModelAutoConfigurationTest,EmbeddingStoreAutoConfigurationTest,VectorModuleTest test
```

结果：37 tests；2 failures、2 errors。`enabled=false` 的中立同 key 定义被旧配置恢复，且自定义中立 factory 未参与 registry 创建。根因分别是合并前过滤 disabled 定义导致 key 占位丢失，以及 Core 默认 registry 构造器内部创建默认 factory。修复后同一命令 37/37 通过。

controller facade 反射契约 RED 为 11 tests、2 failures；迁移到中立 factory/adapter/definition 后 11/11 通过。Vector factory 异常凭证脱敏另按单测试完成 RED/GREEN，响应与日志不再回显 properties 值。

### GREEN

聚焦兼容与集成测试：

```bash
./fw-plus-ai/mvnw -f fw-plus-ai/fw-plus-ai-project/pom.xml \
  -pl fw-plus-ai-boot-autoconfigure test \
  -Dtest=LangChain4jModelAutoConfigurationTest,EmbeddingStoreAutoConfigurationTest,MagicAPIModelAutoConfigurationIntegrationTest,MagicAPIVectorAutoConfigurationIntegrationTest
```

结果：`BUILD SUCCESS`；29 tests，0 failures，0 errors。

旧模型断言迁移后使用 Zulu JDK 8 验证：

```bash
env JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home \
  PATH=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home/bin:$PATH \
  ./fw-plus-ai/mvnw -f fw-plus-ai/fw-plus-ai-project/pom.xml \
  -pl fw-plus-ai-boot-autoconfigure \
  -Dtest=MagicDynamicModelDataSourceTest,ModelModuleTest test
```

结果：`BUILD SUCCESS`；16 tests，0 failures，0 errors，0 skipped。

旧向量与 starter 断言迁移后使用 Zulu JDK 8 验证：

```bash
env JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home \
  PATH=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home/bin:$PATH \
  ./fw-plus-ai/mvnw -f fw-plus-ai/fw-plus-ai-project/pom.xml \
  -pl fw-plus-ai-boot-autoconfigure \
  -Dtest=MagicDynamicVectorDataSourceTest,LangChain4jAutoConfigurationRegistrationTest test
```

结果：`BUILD SUCCESS`；11 tests，0 failures，0 errors，0 skipped。

## 最终验证

完整自动配置测试使用 Zulu JDK 8：

```bash
env JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home \
  PATH=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home/bin:$PATH \
  ./fw-plus-ai/mvnw -f fw-plus-ai/fw-plus-ai-project/pom.xml \
  -pl fw-plus-ai-boot-autoconfigure test
```

结果：`BUILD SUCCESS`；114 tests，0 failures，0 errors，0 skipped。新增自动发现项包含 `VectorModuleTest` 7 项和 `MagicVectorControllerTest` 5 项。

新增及改造 starter 的 reactor 模型验证：

```bash
env JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home \
  PATH=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home/bin:$PATH \
  ./fw-plus-ai/mvnw -f fw-plus-ai/fw-plus-ai-project/pom.xml \
  -pl fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter,fw-plus-ai-boot-starters/fw-plus-ai-store-embedding-spring-boot-starter,fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-model-spring-boot-starter,fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter \
  -am validate
```

结果：`BUILD SUCCESS`；两个中立 starter 的 10 个 reactor 模块全部编译成功。`NeutralStarterDependencyTest` 另行执行 1 test，0 failures，0 errors。

附加检查：

- Task 5 授权范围 `git diff --check -- <task-5 paths>`：通过，无空白错误；工作树其他 Agent 的既有脏文件不纳入该结论。
- Java 8 隔离检查：本任务生产源码未引入 `List.of`、`Map.copyOf`、`Optional.orElseThrow()`、`record`、`var` 或 `jakarta.*`。
- 范围检查：未创建或修改 Java 17 / Spring Boot 3 实现；未修改 Core、正式 docs、OpenSpec、plan 或 progress。

## 已知警告

- 测试输出存在项目既有的 SLF4J 多 binding、Azure Netty 版本提示和 Java 8 MethodHandles fallback 信息。
- 异常分支测试会记录预期的 Spring Context 初始化 warning。
- 上述 warning 均未导致测试失败；最终结果为 0 failures、0 errors。

## 风险说明

- 本任务只实现 Java 8 / Spring Boot 2.7 侧的中立自动配置、starter 和 Magic-API facade。Java 17 / Spring Boot 3 适配器仍属于独立 reactor 的后续范围，本任务没有宣称其 artifact 或运行兼容性已经验证。
- 旧构造器仅用于源码兼容迁移，后续新增代码应直接依赖中立 registry/factory。
