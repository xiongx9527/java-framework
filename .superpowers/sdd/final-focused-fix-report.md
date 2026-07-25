# Final Focused Fix Report

## RED

命令：

```bash
cd fw-plus-core && JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-project/pom.xml -pl fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter -Dtest=EmbeddingStoreProviderAdapterTest test
```

新增的四个 focused tests 中，Milvus 旧 `token` 和 Qdrant 旧 `utls` 用例按预期失败：builder 中 Milvus `token` 为 `null`，Qdrant `useTls` 为 `false`。结果为 `Tests run: 10, Failures: 2, Errors: 0`。

## GREEN

使用同一命令复跑后通过：`Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`，Maven `BUILD SUCCESS`。

## 修改文件

- `MilvusEmbeddingStoreProviderAdapter.java`：读取顺序为 `apiKey`，缺失时回退到旧 `token`。
- `QdrantEmbeddingStoreProviderAdapter.java`：读取顺序为 `useTls`，缺失时回退到旧 `utls`。
- `EmbeddingStoreProviderAdapterTest.java`：新增四个 focused tests，覆盖两组别名回退和新字段优先级。
- `docs/vector-magicapi-plugin-migration.md`：补充 `token -> apiKey`、`utls -> useTls` 映射、兼容说明及 `apiKey` 示例。

## Java 9+ API Scan

在相关 starter 的 `src/main/java` 和 `src/test/java` 执行 Java 9+ API 模式扫描，未发现 `List.of`、`Map.of`、`copyOf`、`String.isBlank`、`Files.readString`、`Stream.toList` 等用法。`git diff --check` 通过。

## 提交

`742f65ceb1f94921f823644750dd353f8a51d007`

## Concerns

无本次功能范围内的 concerns。Maven 输出了现有代码的 deprecation/unchecked 编译提示，不属于本次修改引入的问题。
