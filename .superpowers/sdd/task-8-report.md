# Task 8 Report: 综合验证与 OpenSpec 任务收口

## 验证结果

- OpenSpec strict：`openspec validate add-tiny-engine-ai-starter --strict` 通过，输出 `Change 'add-tiny-engine-ai-starter' is valid`。
- AI autoconfigure 聚焦测试：`JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml test '-Dtest=TinyEngine*Test,AiModelAskDataModelRegistryTest,AiEmbeddingStoreAskDataVectorRetrieverTest,AskDataAutoConfigurationTest'` 通过，42 tests，0 failures，0 errors。
- Starter 测试：`JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/pom.xml -pl fw-plus-ai-tiny-engine-spring-boot-starter,fw-plus-ai-askdata-spring-boot-starter test` 通过，2 个 starter reactor 成功，2 tests，0 failures，0 errors。
- 旧 LangChain4j model/store 与 Spring AI/Alibaba/AgentScope 硬依赖过滤后的 dependency tree 为空，两个 starter 构建成功。
- Java 9+ API 扫描为空：未发现 `List.of`、`Map.of`、`Set.of`、`String.isBlank`、`InputStream.readAllBytes` 或 `java.net.http.HttpClient`。

## 边界扫描说明

- 旧 LangChain4j model/store 坐标扫描的命中仅来自 README 的负向边界警告，以及 starter dependency test 中的 `assertFalse` 断言。
- Spring AI、Spring AI Alibaba、AgentScope 扫描命中仅来自 AskData starter dependency test 的负向断言。
- README 中的 `AiEmbeddingStoreRegistry` 是当前中立向量注册表名称，不是旧 LangChain4j 原生 registry。

## 工具差异

本 worktree 的 `fw-plus-core/mvnw` 存在，但缺少 `fw-plus-core/.mvn/wrapper` 元数据，Maven Wrapper 无法启动。综合验证使用同一 JDK 8 下的系统 `mvn` 等价执行，并保留 wrapper 缺失作为环境差异说明。

## 后续补齐

- 追加提交 `62d97284 feat(ai): complete tiny engine chat provider behavior` 实现了 LangChain4j 原生 Chat/Streaming Chat 调用。
- TinyEngine Chat request 补齐 `model`/messages/stream/max_tokens/stream_options/presence_penalty/response_format/enable_thinking/tool_choice/parallel_tool_calls/enable_search/search_options/frequency_penalty 等字段；LangChain4j 0.35 泛型接口可表达的 messages 与 response_format 会传给原生 `ChatRequest`，其余字段进入响应 metadata 供上层诊断，tools 非空时明确 unsupported。
- Chat 与 RAG 的 fail-fast/degrade 行为已统一：fail-fast 在启动阶段报缺失 Provider/model/store，degrade 注册不可用 Bean 且请求时抛出包含不可用原因的异常，不伪造成功。
- 验证命令：`JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml test '-Dtest=TinyEngine*Test,AiModelAskDataModelRegistryTest,AiEmbeddingStoreAskDataVectorRetrieverTest,AskDataAutoConfigurationTest'` 通过，44 tests，0 failures，0 errors。
- Java 9+ API、旧 LangChain4j model/store 坐标、Spring AI/Alibaba/AgentScope 硬依赖扫描为空。

## 未勾选范围

- REST 路由未实现，因此 `/app-center/api/*` 路由隔离项保持未勾。
