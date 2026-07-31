# 验证报告：add-tiny-engine-ai-starter

## 摘要

| 维度 | 状态 |
|------|------|
| 完整性 | OpenSpec 47/47 项任务已完成；8 个 delta requirement 已核对 |
| 正确性 | 已验证 TinyEngine AI REST、Provider SPI、RAG、AskData 中立模型/向量和依赖边界 |
| 一致性 | 实现遵循设计中“TinyEngine 与 AskData 都走中立 AI registry 主路径”的决策 |

最终结论：PASS。当前没有剩余 CRITICAL 或 IMPORTANT 问题；进入 archive 前仍需要按流程做归档确认。

## 验证范围

- Change：`add-tiny-engine-ai-starter`
- 验证模式：`full`
- 审查模式：`off`；按已选择的构建配置跳过自动代码审查。
- Plan base ref：`0c0a12fa996bbdb4f327b77faa28c55fe9aeda72`

## Requirement 映射

- TinyEngine AI starter 模块接入：通过 starter POM、聚合模块、BOM 条目和 `TinyEngineAiStarterDependencyTest` 验证。
- AskData 使用当前中立 AI 模块：通过 `AiModelAskDataModelRegistry`、`AiEmbeddingStoreAskDataVectorRetriever`、`AskDataAutoConfigurationTest` 和 starter 依赖测试验证。
- AI 能力独立启停：通过 `TinyEngineAiAutoConfigurationTest` 验证。
- 默认 LangChain4j Provider：通过 `LangChain4jTinyEngineAiProviderTest` 和 provider registry 测试验证；TinyEngine 对外 DTO/SPI 不暴露 LangChain4j 类型。
- Provider SPI 多运行时能力边界：第一次完整验证发现非 Chat 能力枚举缺失；已在 `8d115b3e` 补齐 `RAG_RETRIEVAL`、`KNOWLEDGE_INDEXING`、`EMBEDDING`、`VECTOR_STORE` 和 `AGENT_RUNTIME`，并添加回归测试。
- RAG 与知识库服务：通过 `TinyEngineRagServiceTest` 和 `TinyEngineKnowledgeBaseServiceTest` 验证。
- 缺失依赖失败与降级：通过 `TinyEngineAiAutoConfigurationTest` 验证。
- REST 兼容入口：通过 `TinyEngineAiWebAutoConfigurationTest` 和 `TinyEngineAiControllerTest` 验证；Web 路由默认关闭，并通过 `fw.plus.tiny-engine.ai.web.*` 配置 base path 和各端点 path。

## 执行命令

| 命令 | 结果 |
|------|------|
| `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml test -Dtest=TinyEngineAiProviderRegistryTest` | PASS，5 个测试 |
| `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml test -Dtest='TinyEngine*Test'` | PASS，26 个测试 |
| `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml test -Dtest=AiModelAskDataModelRegistryTest,AiEmbeddingStoreAskDataVectorRetrieverTest,AskDataAutoConfigurationTest` | PASS，25 个测试 |
| `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/pom.xml -pl fw-plus-ai-tiny-engine-spring-boot-starter,fw-plus-ai-askdata-spring-boot-starter test` | PASS，2 个测试 |
| `openspec validate add-tiny-engine-ai-starter --strict` | PASS |
| `rg -n "/app-center/api" .../tinyengine .../fw-plus-ai-tiny-engine-spring-boot-starter/README.md` | PASS，零命中 |
| `rg -n "List\\.of\\(|Map\\.of\\(|Set\\.of\\(|String\\.isBlank\\(|readAllBytes\\(|java\\.net\\.http\\.HttpClient" .../tinyengine` | PASS，零命中 |

## 静态边界检查

- 旧 LangChain4j model/store 坐标扫描只命中 README 迁移提示和依赖测试负向断言，没有生产主路径依赖命中。
- Spring AI / Spring AI Alibaba / AgentScope2 扫描只命中 AskData starter 的负向断言，没有必需依赖命中。
- REST 路由扫描确认 TinyEngine AI 生产代码和 TinyEngine AI README 没有硬编码 `/app-center/api/*`。

## 备注

- Maven 仍输出既有的 `fw-plus-ai-model`、`fw-plus-ai-store-embedding` dependencyManagement 重复声明警告，以及测试中的 SLF4J multiple binding 警告。这些警告不是本次 REST/capability follow-up 引入，也没有导致验证命令失败。
- 当前 worktree 缺少 Maven Wrapper 元数据，因此按本 change 已采用的方式使用系统 Maven，并显式指定 JDK 8。
