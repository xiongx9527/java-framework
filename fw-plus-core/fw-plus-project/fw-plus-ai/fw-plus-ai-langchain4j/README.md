# fw-plus-ai-langchain4j Java 8 兼容覆盖说明

## 背景

fw-plus-core 的基线是 Java 8。对 LangChain4j 来说，`0.35.x` 通常是兼顾功能和 Java 8 兼容性的较高可用版本；但本目录下部分扩展模块的 `0.35` 版本源码已经使用了 Java 8 不支持的类或方法。

为保持项目整体 Java 8 兼容性，本目录中除 `fw-plus-ai-langchain4j-store-embeding` 之外的模块，采用与 LangChain4j 原模块相同的包名和类名重写关键类，用本工程内的源码覆盖 `0.35` 中不兼容 Java 8 的实现。

## 范围

以下模块属于 LangChain4j 兼容覆盖模块：

- `fw-plus-ai-langchain4j-anthropic`
- `fw-plus-ai-langchain4j-azure-ai-search`
- `fw-plus-ai-langchain4j-azure-open-ai`
- `fw-plus-ai-langchain4j-core`
- `fw-plus-ai-langchain4j-dashscope`
- `fw-plus-ai-langchain4j-elasticsearch`
- `fw-plus-ai-langchain4j-github-models`
- `fw-plus-ai-langchain4j-milvus`
- `fw-plus-ai-langchain4j-ollama`
- `fw-plus-ai-langchain4j-openai`
- `fw-plus-ai-langchain4j-qianfan`
- `fw-plus-ai-langchain4j-reactor`
- `fw-plus-ai-langchain4j-redis`
- `fw-plus-ai-langchain4j-vertex-ai-gemini`
- `fw-plus-ai-langchain4j-voyage-ai`

`fw-plus-ai-langchain4j-store-embeding` 是本工程自有的向量存储抽象与适配扩展，不属于上游 LangChain4j 类覆盖范围。

## 覆盖方式

这些模块中的源码保留上游 LangChain4j 的原始包名，例如：

- `dev.langchain4j.spring.*`
- `dev.langchain4j.openai.spring.*`
- `dev.langchain4j.store.embedding.*.spring.*`

构建时，本工程会产出同坐标或配套坐标的 Java 8 兼容类，供上层 `fw-plus-ai-boot-autoconfigure` 和 starter 依赖使用。这样可以避免运行时加载到使用高版本 Java API 的上游实现。

## 维护规则

1. 修改这些模块时，必须继续保持 Java 8 兼容，不要使用 Java 9+ API，例如 `List.of()`、`Map.of()`、`Optional.or()`、`Stream.toList()` 等。
2. 同步 LangChain4j `0.35` 或后续版本时，应先对比上游同名类的行为差异，再只迁移必要逻辑，避免直接复制会破坏 Java 8 兼容的代码。
3. 包名、类名、Spring Bean 名称和配置属性前缀应尽量与上游保持一致，确保自动配置和 starter 行为可替换。
4. 若新增 LangChain4j 扩展覆盖模块，应同步更新本说明和上层 `LangChain4jAutoConfiguration` 的导入及启动条件。
5. 若某个上游模块重新恢复 Java 8 兼容，也需要经过编译、自动配置测试和依赖冲突检查后，才能移除本地覆盖。

## 验证建议

修改本目录的兼容覆盖类后，至少验证：

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core
./mvnw clean install -pl fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j -DskipTests

cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-ai
./mvnw test -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure
```

当前工作区的 Maven Wrapper 若不可用，可按项目实际环境使用 Java 8 的 Maven 命令替代，但最终应确认 Java 8 下编译通过。
