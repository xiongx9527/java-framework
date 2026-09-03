# fw-plus-ai-langchain4j Java 8 兼容覆盖说明

## 背景

fw-plus-core 的基线是 Java 8。对 LangChain4j 来说，`0.35.x` 通常是兼顾功能和 Java 8 兼容性的较高可用版本；但本目录下部分扩展模块的 `0.35` 版本源码已经使用了 Java 8 不支持的类或方法。

为保持项目整体 Java 8 兼容性，本目录中的 provider 模块采用与 LangChain4j 原模块相同的包名和类名重写关键类，用本工程内的源码覆盖 `0.35` 中不兼容 Java 8 的实现。模型与向量能力统一由 `fw-plus-ai-langchain4j-adapter` 接入中立 SPI。

下表中的 `0.35` provider覆盖模块只面向Java 8编译或运行环境；Java 17及以上环境应优先使用对应LangChain4j `0.35`原始artifact。新增的Agentic完整回移不是provider覆盖，适用边界单独见下节。

## 当前已覆盖的类包

以下模块属于 LangChain4j `0.35` Java 8 兼容覆盖模块。覆盖方式是保留上游相同的 `dev.langchain4j.*` 包名和类名，在本工程重新编译出 Java 8 兼容版本。

| 本地模块 | 覆盖的上游 artifact | 覆盖包 | 覆盖类 |
| --- | --- | --- | --- |
| `fw-plus-ai-langchain4j-anthropic` | `langchain4j-anthropic-spring-boot-starter` | `dev.langchain4j.anthropic.spring` | `AutoConfig`, `ChatModelProperties`, `Properties` |
| `fw-plus-ai-langchain4j-azure-ai-search` | `langchain4j-azure-ai-search-spring-boot-starter` | `dev.langchain4j.azure.aisearch.spring` | `AutoConfig`, `Properties` |
| `fw-plus-ai-langchain4j-azure-open-ai` | `langchain4j-azure-open-ai-spring-boot-starter` | `dev.langchain4j.azure.openai.spring` | `AutoConfig`, `ChatModelProperties`, `EmbeddingModelProperties`, `ImageModelProperties`, `Properties` |
| `fw-plus-ai-langchain4j-core` | `langchain4j-core` | `dev.langchain4j.*` Core owner兼容包 | Agentic所需的Model、Tool、Invocation、Guardrail、Observability、JSON与并发兼容API |
| `fw-plus-ai-langchain4j-spring-boot-starter` | `langchain4j-spring-boot-starter` | `dev.langchain4j.spring`, `dev.langchain4j.service.spring`, `dev.langchain4j.rag.spring` | `LangChain4jAutoConfig`, `AiService`, `AiServiceFactory`, `AiServiceScannerProcessor`, `AiServiceWiringMode`, `AiServicesAutoConfig`, `ClassPathAiServiceScanner`, `RagAutoConfig`, `RagProperties`, `RetrievalProperties` |
| `fw-plus-ai-langchain4j-dashscope` | `langchain4j-dashscope-spring-boot-starter` | `dev.langchain4j.dashscope.spring` | `AutoConfig`, `ChatModelProperties`, `EmbeddingModelProperties`, `LanguageModelProperties`, `Properties`, `TokenizerProperties` |
| `fw-plus-ai-langchain4j-elasticsearch` | `langchain4j-elasticsearch-spring-boot-starter` | `dev.langchain4j.store.embedding.elasticsearch.spring` | `ElasticsearchClientHelper`, `ElasticsearchEmbeddingStoreAutoConfiguration`, `ElasticsearchEmbeddingStoreProperties` |
| `fw-plus-ai-langchain4j-github-models` | `langchain4j-github-models-spring-boot-starter` | `dev.langchain4j.model.githubmodels.spring` | `AutoConfig`, `ChatModelProperties`, `EmbeddingModelProperties`, `Properties` |
| `fw-plus-ai-langchain4j-milvus` | `langchain4j-milvus-spring-boot-starter` | `dev.langchain4j.store.embedding.milvus.spring` | `MilvusEmbeddingStoreAutoConfiguration`, `MilvusEmbeddingStoreProperties` |
| `fw-plus-ai-langchain4j-ollama` | `langchain4j-ollama-spring-boot-starter` | `dev.langchain4j.ollama.spring` | `AutoConfig`, `ChatModelProperties`, `EmbeddingModelProperties`, `LanguageModelProperties`, `Properties` |
| `fw-plus-ai-langchain4j-openai` | `langchain4j-open-ai-spring-boot-starter` | `dev.langchain4j.openai.spring` | `AutoConfig`, `ChatModelProperties`, `EmbeddingModelProperties`, `ImageModelProperties`, `LanguageModelProperties`, `ModerationModelProperties`, `Properties`, `ProxyProperties` |
| `fw-plus-ai-langchain4j-qianfan` | `langchain4j-qianfan-spring-boot-starter` | `dev.langchain4j.qianfan.spring` | `AutoConfig`, `ChatModelProperties`, `EmbeddingModelProperties`, `LanguageModelProperties`, `Properties` |
| `fw-plus-ai-langchain4j-reactor` | `langchain4j-reactor` | `dev.langchain4j.reactor` | `TokenStreamToFluxAdapter` |
| `fw-plus-ai-langchain4j-redis` | `langchain4j-redis-spring-boot-starter` | `dev.langchain4j.store.embedding.redis.spring` | `RedisEmbeddingStoreAutoConfiguration`, `RedisEmbeddingStoreProperties` |
| `fw-plus-ai-langchain4j-vertex-ai-gemini` | `langchain4j-vertex-ai-gemini` | `dev.langchain4j.vertexai.spring` | `AutoConfig`, `ChatModelProperties`, `Properties` |
| `fw-plus-ai-langchain4j-voyage-ai` | `langchain4j-voyage-ai-spring-boot-starter` | `dev.langchain4j.voyageai.spring` | `AutoConfig`, `EmbeddingModelProperties`, `Properties`, `ScoringModelProperties` |

## LangChain4j Agentic Java 8 回移

`fw-plus-ai-langchain4j-agentic` 固定回移 LangChain4j tag `1.19.0`、commit
`09a2aa3861ba468af3507c28f5676048cd5db4c6` 的 `langchain4j-agentic:1.19.0-beta29`
生产源码。官方清单含153个Java文件，本地另有3个Java 8 helper；94个官方文件保持字节一致，
59个因record/现代语法与JDK API回移而修改，包名保持`dev.langchain4j.agentic.*`。
由于LangChain4j `0.35.0`没有官方Agentic artifact，该模块是新增Java 8能力，不是替换
`0.35.0` JAR。

LangChain4j角色由唯一的`fw-plus-ai-langchain4j-parent`聚合器管理：
`fw-plus-ai-langchain4j`是aggregate owner JAR，`fw-plus-ai-langchain4j-core`是Core owner
JAR，`fw-plus-ai-langchain4j-spring-boot-starter`仅承载10个Spring覆盖类，Agentic通过普通本地
owner坐标取得兼容依赖。

aggregate/Core对官方`0.35.0`同FQCN的必要替换登记在
`compat/overlay-allowlist.tsv`，由unit/packaging IT验证Shade excludes、成品字节、Java 8 class major、
重复class、SPI和最终生产classpath。API parity还比较public/protected类型层次、泛型、访问标志、
构造器/方法/字段、throws、常量和注解值；离线行为测试通过真实public root覆盖workflow、Planner、
Supervisor/listener、HITL持久化恢复、并行失败及异步/流式路径。完整来源、修改类别、许可和Java 8
不可表达差异见`fw-plus-ai-langchain4j-agentic/UPSTREAM.md`。

## Java 8 / Java 17 依赖版本对照

以下版本只针对本目录新增的 Java 8 覆盖模块。Java 8 环境使用本工程覆盖模块及其降级依赖；Java 17 及以上环境应恢复使用 LangChain4j `0.35.0` 原始 artifact 及上游依赖版本。

| 覆盖模块 | Java 8 覆盖依赖 | Java 17 及以上原始依赖 |
| --- | --- | --- |
`fw-plus-ai-langchain4j-adapter` 是本工程自有的模型与向量适配扩展，不属于上游 LangChain4j 类覆盖范围。

## 覆盖方式

这些模块中的源码保留上游 LangChain4j 的原始包名，例如：

- `dev.langchain4j.spring.*`
- `dev.langchain4j.openai.spring.*`
- `dev.langchain4j.store.embedding.*.spring.*`

构建时，本工程会产出配套坐标的 Java 8 兼容类，供上层 `fw-plus-ai-boot-autoconfigure` 和 starter 依赖使用。这样可以避免 Java 8 环境加载到使用高版本 Java API 或高版本字节码的上游实现。Java 17 及以上环境不需要这些覆盖模块，应使用 LangChain4j 原始 artifact。

## 维护规则

1. 修改这些模块时，必须继续保持 Java 8 兼容，不要使用 Java 9+ API，例如 `List.of()`、`Map.of()`、`Optional.or()`、`Stream.toList()` 等。
2. 同步 LangChain4j `0.35` 或后续版本时，应先对比上游同名类的行为差异，再只迁移必要逻辑，避免直接复制会破坏 Java 8 兼容的代码。
3. 包名、类名、Spring Bean 名称和配置属性前缀应尽量与上游保持一致，确保自动配置和 starter 行为可替换。
4. 若新增 LangChain4j 扩展覆盖模块，应同步更新本说明和上层 `LangChain4jAutoConfiguration` 的导入及启动条件。
5. 若某个上游模块重新恢复 Java 8 兼容，也需要经过编译、自动配置测试和依赖冲突检查后，才能移除本地覆盖。

## 模型 API 版本边界

本工程的模型扩展锁定 LangChain4j `0.35.0`。`CHAT` 使用
`ChatLanguageModel`，`STREAMING_CHAT` 使用 `StreamingChatLanguageModel`；LangChain4j
`1.x` 使用 `ChatModel` 和 `StreamingChatModel` 命名，当前版本不能引用这些类。

`TextContent`、`ImageContent`、`AudioContent`、`VideoContent` 和
`PdfFileContent` 均表示 Chat 请求可携带的多模态内容，而不是独立模型接口。
`modelType` 保留 `AUDIO`、`VIDEO`、`PDF` 三类 fw-plus 扩展类型，便于 Magic
资源和业务适配器统一登记；但 LangChain4j `0.35.0` 和当前 `1.x` 文档没有
独立的 `AudioModel`、`VideoModel` 或 `PdfModel` 统一接口，默认自动配置不会
为这三类创建内置 provider adapter。需要使用时通过自定义
`LangChain4jModelProviderAdapter` 接入厂商 SDK 或业务封装。

## 验证建议

修改本目录的兼容覆盖类后，至少验证：

```bash
cd fw-plus-core
./mvnw -f fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j-parent/pom.xml clean install

cd ../fw-plus-ai
./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-autoconfigure -am test
```

命令必须使用JDK 8。当前worktree的Maven Wrapper运行文件若缺失，可用Maven 3.6.3执行等价命令，但不能跳过unit、Failsafe IT、owner成品或生产classpath门禁。
