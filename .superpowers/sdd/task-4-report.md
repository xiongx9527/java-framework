# Task 4 实施报告

## 状态

DONE_WITH_CONCERNS

## RED

先扩展既有四个测试类，覆盖中立 registry 的 provider/model alias、store default key、
完整向量操作桥接，以及中立门面不公开 LangChain4j 原生 accessor。执行：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home \
./fw-plus-core/mvnw -f fw-plus-core/pom.xml \
  -pl fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model,fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding \
  test
```

结果：`BUILD FAILURE`。store 测试编译明确报告
`com.xiongx9527.fw.plus.ai.store.embedding` 不存在于模块依赖图，符合适配层尚未接入
中立 SPI 的预期失败原因；model 因 reactor fail-fast 被跳过。

## GREEN

同一命令最终结果：`BUILD SUCCESS`。

- `fw-plus-ai-langchain4j-store-embeding`：12 项测试，0 failure / 0 error / 0 skipped。
- `fw-plus-ai-langchain4j-model`：13 项测试，0 failure / 0 error / 0 skipped。
- 合计 25 项测试全部通过。

首轮 GREEN 时 store 12/12 通过，model 有 3 个旧异常契约失败：中立 factory 多包装一层
异常，并把旧 `modelType` 文案变成 `type`。随后仅修正兼容异常翻译和旧输入校验，model
13/13 通过，既有 cause 身份、`returned null` 和字段文案保持不变。

## 兼容行为

- 原 artifactId、旧 factory/registry 接口、provider adapter 接口和原生返回类型均保留。
- `DefaultLangChain4jModelFactory` 同时实现 `AiModelAdapter`；旧入口先转换为
  `AiModelDefinition` 并委托中立 factory，provider alias、model key/type 和
  `baseUrl`/`apiKey`/`modelName` 映射保持。
- `DefaultEmbeddingStoreFactory` 同时实现 `AiEmbeddingStoreAdapter`；旧 store 定义映射为
  `framework=langchain4j`、`type=VECTOR`，provider alias、dimension、metadataKeys 和
  properties 保持。
- 两个旧 registry 的 key/default 解析委托中立 registry；旧异常类型与既有 store 错误文案保持。
- 中立模型/store 返回包私有桥接类，公开方法签名只使用中立类型；LangChain4j 原生对象仅能由
  同包兼容入口解包。向量桥接覆盖 add/addAll/remove/removeAll/search 和结果/metadata/vector 转换。

## 依赖与 Java 8 证据

`dependency:tree` 结果：

```text
fw-plus-ai-langchain4j-store-embeding
\- com.xiongx9527.fw.plus:fw-plus-ai-store-embedding:0.1.1-SNAPSHOT:compile

fw-plus-ai-langchain4j-model
\- com.xiongx9527.fw.plus:fw-plus-ai-model:0.1.1-SNAPSHOT:compile
```

两个 `Default*Factory.class` 的 `javap -verbose` 均显示 `major version: 52`。新增主代码未命中
`List.of`、`Map.of`、`Set.of`、`copyOf` 等 Java 9+ API。

## 提交

- 实现 SHA：`8a2c4b7957f4863caa4e46803b4580877789b0da`
- 提交标题：`fw-plus-ai: adapt langchain4j modules to neutral core`
- 实现提交仅包含两个 LangChain4j model/store 模块下 12 个文件。

## 风险

- LangChain4j 0.35 的 `EmbeddingStore` 没有同时接受自定义 id 与 `TextSegment` 的标准方法。
  当前有 id 的中立文档优先保留 id/vector；无 id 文档通过 `add(embedding, segment)` 保留文本和
  metadata。要求二者同时原子持久化的 provider 仍需专用 adapter 扩展。
- 中立 model 门面当前只承载 key/framework/provider/type 元数据；真实模型调用仍由既有
  LangChain4j 兼容入口处理，AI Boot/Magic-API 改造属于 Task 5。
- 本任务未运行 AI Boot/Magic-API 集成测试，也未修改其装配；这里只证明 Core 两个 adapter
  模块及旧入口回归。
