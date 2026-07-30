# Task 6 Report: TinyEngine RAG 与知识库服务

## 实现内容

- 新增 `DefaultTinyEngineRagService`，通过中立 `AiEmbeddingStoreRegistry`、`AiEmbeddingStore`、`AiEmbeddingQuery`、`AiEmbeddingSearchResult` 与 `AiEmbeddingMatch` 查询向量库；将 score、content、metadata 映射到 TinyEngine DTO，并在服务层按 `metadata.collection` 过滤。
- 新增 `DefaultTinyEngineKnowledgeBaseService`，支持内联文本、普通文件路径和 `classpath:` 资源的 UTF-8 文本读取；支持 txt/text/md/markdown/csv/json/xml，按 `chunk-size` / `chunk-overlap` 切块并写入中立 `AiEmbeddingDocument`。
- 每个索引块的 metadata 包含 `source`、`format`、`timestamp`、`collection`；索引结果返回 loaded/skipped/failed/chunks 和失败路径。
- 扩展 TinyEngine RAG、知识库服务契约与 DTO，并将自动配置改为构造新服务。RAG 的依赖校验继续使用 `AiModelRegistry` 与 `AiEmbeddingStoreRegistry`，缺失时保留 fail-fast/degrade 行为。

## 测试命令与结果

要求命令：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home ./fw-plus-core/mvnw -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml test -Dtest=TinyEngineRagServiceTest,TinyEngineKnowledgeBaseServiceTest,TinyEngineAiAutoConfigurationTest
```

结果：未启动 Maven。该 worktree 缺少 `.mvn/wrapper/maven-wrapper.properties` 和 `maven-wrapper.jar`，Wrapper 在执行测试前失败。

等价验证命令：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml test -Dtest=TinyEngineRagServiceTest,TinyEngineKnowledgeBaseServiceTest,TinyEngineAiAutoConfigurationTest
```

结果：BUILD SUCCESS，13 tests run，0 failures，0 errors，0 skipped。

## TDD RED/GREEN 证据

- RED：新增测试和契约后执行 Maven Wrapper，因 worktree 缺失 Wrapper 元数据而无法进入编译；首次系统 Maven 编译进入新增测试后，因测试漏导入 `java.util.Arrays` 在 `TinyEngineKnowledgeBaseServiceTest` 编译阶段失败，已立即修正。
- GREEN：最小实现后系统 Maven 聚焦测试通过；补充真实临时文本资源切块测试后再次执行，13 个测试全部通过。

## 修改文件

- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/TinyEngineAiAutoConfiguration.java`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/service/TinyEngineRagService.java`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/service/TinyEngineKnowledgeBaseService.java`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/rag/` 下的 RAG/知识库实现与 DTO。
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/tinyengine/rag/` 下的两组测试。

## 自审结果

- 未修改 AskData、BOM 版本或 Provider 依赖；未将旧 LangChain4j model/store starter 作为本服务的主路径。
- 生产代码使用 Java 8 API，未使用 `List.of`、`Map.of`、`String.isBlank`、`InputStream.readAllBytes` 或 `java.net.http.HttpClient`。
- `git diff --check -- fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure` 无问题。工作树中协调产物 `task-6-brief.md` 的既有末尾空行未修改、未暂存。

## 风险/疑问

- 中立 `AiModel` 契约不暴露 embedding 方法，因此该服务将调用方提供的查询向量传给 Store，知识库切块文档的 vector 为 null，由中立 Store/provider 的写入策略处理。后续若中立模型层增加 embedding 调用契约，可在不引入厂商 SDK 的前提下补齐服务端向量生成。
- POM 有既有重复 dependencyManagement 警告，测试通过但未在本任务中修改。
