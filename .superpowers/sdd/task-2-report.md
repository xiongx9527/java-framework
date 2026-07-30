# Task 2 Report: TinyEngine AI 配置、自动配置入口与服务契约

## Status

DONE

## 实现内容

- 新增 `TinyEngineAiProperties`，绑定 `fw.plus.tiny-engine.ai`，包含总开关、Provider、Chat/RAG/Vector 开关、中立模型 key、中立 store key、缺失依赖策略及 RAG chunk/search 参数。
- 新增 TinyEngine Chat、RAG、知识库服务 marker 契约；真实 Provider、Chat DTO、RAG 和知识库业务实现保留给后续 Task 3 与 Task 6。
- 新增 `TinyEngineAiAutoConfiguration`：总开关关闭时不注册 TinyEngine Bean；仅启用 Chat 时不依赖向量 store；启用 RAG 且默认 fail-fast 策略下缺少 `AiModelRegistry` 或 `AiEmbeddingStoreRegistry` 时，以明确诊断启动失败。
- 在 `spring.factories` 和 `AutoConfiguration.imports` 注册自动配置。

## RED/GREEN 证据

### RED

先新增 `TinyEngineAiAutoConfigurationTest`，再运行：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml test -Dtest=TinyEngineAiAutoConfigurationTest
```

结果：失败，测试编译明确提示缺少 `com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service` 包中的三个服务契约，符合尚未实现的预期。

指定 Maven Wrapper 命令未能执行，因为 worktree 缺少根 `.mvn/wrapper/maven-wrapper.properties` 与 wrapper jar；已按任务 brief 改用同一 JDK 的系统 Maven 等价命令。

### GREEN

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml test -Dtest=TinyEngineAiAutoConfigurationTest
```

结果：`BUILD SUCCESS`，运行 3 个测试，0 failures、0 errors、0 skipped。RAG 缺依赖场景按预期输出 `TinyEngine AI RAG requires both AiModelRegistry and AiEmbeddingStoreRegistry`。

## 变更文件

- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/TinyEngineAiProperties.java`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/TinyEngineAiAutoConfiguration.java`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/service/TinyEngineAiChatService.java`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/service/TinyEngineRagService.java`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/service/TinyEngineKnowledgeBaseService.java`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/TinyEngineAiAutoConfigurationTest.java`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/resources/META-INF/spring.factories`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

## 自审结果

- `git diff --check` 通过。
- 新增 TinyEngine 源码与测试未使用 Java 9+ API，也未引入旧 LangChain4j model/store 业务契约。
- 只使用 `AiModelRegistry` 与 `AiEmbeddingStoreRegistry` 进行 RAG 启动校验。
- 未修改、暂存或提交 `openspec/**`、`docs/superpowers/**`、`.comet/**` 或 AskData 文件。

## 风险信号

- 公共 API 契约或对外接口变更：新增三个 TinyEngine 服务契约与属性前缀。
- 单任务 diff 超过 200 行：新增源码、测试与资源注册合计约 218 行。

## 顾虑

- 系统 Maven 在构建时报告既有 BOM 中 `fw-plus-ai-model` 与 `fw-plus-ai-store-embedding` 的重复 dependencyManagement 声明，以及既有 SLF4J 多绑定警告；目标测试仍成功。这些不是本任务修改引入。
- Task 3 与 Task 6 需要将当前 marker 服务契约扩展为 Provider DTO/业务方法并替换占位实现。
