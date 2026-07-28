# Task 4 实施报告

## 状态

DONE

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

本轮针对 task review 的三个 Important 分别补充独立 RED：

1. `shouldPreserveDocumentContentOrRejectUnsupportedExplicitId`：无 id 文档的原生
   `Embedding`/`TextSegment` 入参断言通过，但显式 id + text/metadata 未抛
   `AiEmbeddingStoreException`，证明原实现静默丢弃内容。
2. `shouldIncludeProviderAndKeyWhenAdapterCreationFails` 与
   `shouldIncludeProviderAndKeyWhenAdapterSupportCheckFails`：旧
   `EmbeddingStoreException.getCause()` 均为 `null`，证明中立桥接丢失原始异常；外层消息
   的敏感 sentinel 不泄漏断言同时生效。
3. `shouldFailWhenModelKeyIsMissingOrMappedToNull` 与
   `shouldRejectUnknownStoreKeyOrNullStore`：model/store registry 的 `get` 均未抛受控异常，
   证明 null 原生值被包装成了非空中立 wrapper。

## GREEN

同一双模块命令在本轮修复后重新执行，结果：`BUILD SUCCESS`。

- `fw-plus-ai-langchain4j-store-embeding`：12 项测试，0 failure / 0 error / 0 skipped。
- `fw-plus-ai-langchain4j-model`：13 项测试，0 failure / 0 error / 0 skipped。
- 合计 25 项测试全部通过。

本轮三个定向 GREEN 全部通过，随后完整回归保持 store 12/12、model 13/13。显式 id
限制、provider `supports/create` 两条 cause 身份和脱敏断言、两个 registry 的 null
`get/default` 受控异常均包含在这 25 项测试内，未增加或减少测试总数。

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
- LangChain4j 0.35 无 `id + Embedding + TextSegment` 标准 SPI：无 id 文档完整传递
  text/metadata；显式 id 且携带 text 或 metadata 时，在调用原生 store 前受控拒绝，不再静默丢失。
- 旧 store factory 的中立桥接使用单次调用本地的 failure capture 保留原始 cause 身份，外层错误
  仅包含受控 key/framework/provider/type/causeType 上下文，不拼接 cause message。
- 旧 model/store registry 构建中立映射时跳过 null 原生值，`get/default` 继续抛旧异常类型，
  不会返回 null 或非空 wrapper。

## 依赖与 Java 8 证据

`dependency:tree` 结果：

```text
fw-plus-ai-langchain4j-store-embeding
\- com.xiongx9527.fw.plus:fw-plus-ai-store-embedding:0.1.1-SNAPSHOT:compile

fw-plus-ai-langchain4j-model
\- com.xiongx9527.fw.plus:fw-plus-ai-model:0.1.1-SNAPSHOT:compile
```

本轮四个改动类 `LangChain4jAiEmbeddingStore`、`DefaultEmbeddingStoreFactory`、
`DefaultEmbeddingStoreRegistry`、`DefaultLangChain4jModelRegistry` 的 `javap -verbose` 均显示
`major version: 52`。两个 LangChain4j 模块主代码未命中 `List.of`、`Map.of`、`Set.of`、
`copyOf` 或无参 `Optional.orElseThrow()`；中立 model/store 主代码未命中 LangChain4j、
Spring AI 或 AgentScope import。

## 提交

- 实现 SHA：`8a2c4b7957f4863caa4e46803b4580877789b0da`
- 提交标题：`fw-plus-ai: adapt langchain4j modules to neutral core`
- 实现提交仅包含两个 LangChain4j model/store 模块下 12 个文件。
- Important 修复 SHA：`0a0eec6e`
- 修复提交标题：`fix(ai): harden langchain4j neutral bridges`
- 修复提交仅包含两个 LangChain4j model/store 模块下 7 个源码/测试文件。

## 风险

- LangChain4j 0.35 的 `EmbeddingStore` 没有同时接受自定义 id 与 `TextSegment` 的标准方法。
  中立调用方若必须原子保留显式 id、文本和 metadata，需要使用具备专用扩展能力的 provider；
  当前通用桥接会明确拒绝该组合。
- 中立 model 门面当前只承载 key/framework/provider/type 元数据；真实模型调用仍由既有
  LangChain4j 兼容入口处理，AI Boot/Magic-API 改造属于 Task 5。
- 本任务未运行 AI Boot/Magic-API 集成测试，也未修改其装配；这里只证明 Core 两个 adapter
  模块及旧入口回归。
