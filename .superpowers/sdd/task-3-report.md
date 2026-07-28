# Task 3 实施报告

## 状态

DONE

## RED

命令：

```bash
./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding test
```

结果：`BUILD FAILURE`。测试编译阶段报告 `AiEmbeddingStoreDefinition`、
`AiEmbeddingStoreAdapter`、`AiEmbeddingStore`、`DefaultAiEmbeddingStoreRegistry`、
中立 DTO 等类型不存在，符合“先测试、后实现”的预期失败原因。

## GREEN

命令：

```bash
./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding test
```

结果：`BUILD SUCCESS`；执行 9 项契约测试，`Failures: 0, Errors: 0, Skipped: 0`。

覆盖内容：fake adapter 成功解析、重复 enabled key、默认 key 不存在、禁用默认项、
无支持 adapter、adapter 的 supports/create 异常与空返回、properties/metadata/vector/
搜索结果的防御复制，以及异常消息不泄漏 properties/凭证。

审查后追加脱敏回归循环：先将 adapter 异常消息设为测试凭证，并断言完整堆栈不泄漏；
同一测试命令产生预期 `2 failures`，原因是原始 cause 仍被保留。随后 factory 改为仅记录
安全的 `causeType`，不挂载原始 cause；再次运行结果为 9/9 通过。

## 安装与兼容性

命令：

```bash
./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding -am install -DskipTests
```

结果：`BUILD SUCCESS`；reactor 8/8 成功，目标 JAR 与 POM 已安装到项目配置的本地 Maven 仓库。

依赖泄漏检查：

```bash
rg -n 'dev\.langchain4j|org\.springframework\.ai|agentscope' \
  fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding/src/main \
  fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding/pom.xml
```

结果：无输出。Java 9+ API 检查无输出；`javap -verbose AiEmbeddingStore.class` 显示
`major version: 52`，确认产物为 Java 8 字节码。

## 文件与提交

- 聚合注册：`fw-plus-core/fw-plus-project/fw-plus-ai/pom.xml`
- 模块 POM：`fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding/pom.xml`
- 主代码：`fw-plus-ai-store-embedding/src/main/java/com/xiongx9527/fw/plus/ai/store/embedding/` 下 14 个类
- 契约测试：`fw-plus-ai-store-embedding/src/test/java/com/xiongx9527/fw/plus/ai/store/embedding/DefaultAiEmbeddingStoreRegistryTest.java`
- 实现提交：`5e1b692f` (`fw-plus-ai: add neutral embedding store core`)
- 脱敏修复提交：`9d1873e1` (`fw-plus-ai: sanitize embedding adapter failures`)

## 审查

独立审查初次报告 1 个 Important：原始 adapter cause 可能在完整异常链中泄漏配置。
按上述第二轮 TDD 修复后复审，结论为原 Important 已关闭，未发现新增 Critical/Important。

## 风险

- 当前提交只提供中立 Core 契约；LangChain4j 默认 adapter 与 Boot 装配由后续任务接入。
- metadata/properties 采用容器级防御复制；其中自定义可变对象的深复制由调用方负责。
- adapter 的持久化、过滤和评分语义属于 provider 实现，本模块只定义通用边界。
