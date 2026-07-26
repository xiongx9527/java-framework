# Task 3 实施报告

## 状态

DONE

## RED 命令和失败摘要

先确认 `fw-plus-core/.mvn/wrapper/` 缺失，故按任务要求使用 Zulu 8 系统 Maven：

```bash
cd fw-plus-core
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-project/pom.xml -pl fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter -Dtest=EmbeddingStoreAutoConfigurationMetadataTest,EmbeddingStoreAutoConfigurationTest test
```

结果：失败，Maven 报告所选 starter 尚未处于 reactor 中。这符合 RED 预期：聚合 POM 尚未注册 starter，自动配置和 `spring.factories` 也尚未完成。

## GREEN 命令和通过摘要

由于 Task 3 同时变更 core 的 `EmbeddingStoreProperties` 配置绑定注解，聚焦验证使用 `-am` 将 core 变更放入同一 reactor：

```bash
cd fw-plus-core
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-project/pom.xml -pl fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter -am -Dtest=EmbeddingStoreAutoConfigurationMetadataTest,EmbeddingStoreAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：`BUILD SUCCESS`。新增测试共 5 项，`Failures: 0, Errors: 0`，覆盖 `spring.factories`、禁用开关、多 store 默认 key、无效默认 key 和未知 provider。

```bash
cd fw-plus-core
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-project/pom.xml -pl fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding,fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter -am test
```

结果：`BUILD SUCCESS`。core store 模块 10 项测试、starter 模块 8 项测试均为 0 failures、0 errors。

## Java 9+ API 扫描结果

```bash
rg -n --glob '*.java' '\b(List|Set|Map)\.(of|copyOf)\s*\(|Optional\.orElseThrow\s*\(|\.isBlank\s*\(|\.strip\s*\(|\.lines\s*\(|\bvar\s+' \
  fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding/src/main/java \
  fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/src/main/java \
  fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/src/test/java
```

结果：无输出，未发现扫描范围内的 Java 9+ API。另执行 `git diff --check`，通过。

## 修改文件列表

- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding/pom.xml`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/EmbeddingStoreProperties.java`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/pom.xml`
- `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/pom.xml`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/src/main/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/autoconfigure/EmbeddingStoreAutoConfiguration.java`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/src/main/resources/META-INF/spring.factories`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/autoconfigure/EmbeddingStoreAutoConfigurationMetadataTest.java`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/autoconfigure/EmbeddingStoreAutoConfigurationTest.java`

## 提交 hash

- `17b04fd8b8bf42c314c5e7253e8045c8485abb03` - `feat(vector): add embedding store spring boot starter`

## 自检结论和 Concerns

- `EmbeddingStoreProperties` 已按准确 prefix 绑定；自动配置仅通过 `META-INF/spring.factories` 注册，未创建两种错误的 imports 路径。
- starter 注册默认 factory、五个已有 provider adapter 及单一 registry；registry 仅在 `enabled=true` 或缺省时创建，并校验空 provider、重复 key、无效默认 key 和未知 provider。
- `spring-boot-autoconfigure` 自身携带标准 Boot imports 元数据，元数据测试因此限定检查本 starter 自身的 classes 目录，避免将依赖资源误判为本模块新增文件。
- 当前 worktree 缺少 Maven Wrapper，只能通过 `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn` 完成验证。

## Task 3 Fixer：重复 key 回归测试

### RED 命令与摘要

```bash
cd fw-plus-core
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-project/pom.xml -pl fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter -am -Dtest=EmbeddingStoreAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

本次 fixer 针对已存在的重复 key 校验补测试；由于生产代码在测试前已经实现 `Duplicate embedding store key` 校验，未能观察到缺陷状态的失败 RED。新增测试首次运行即按预期捕获启动失败，故没有修改生产代码。

### GREEN 命令与摘要

使用上述命令完成验证：starter 反应堆构建 `BUILD SUCCESS`，`EmbeddingStoreAutoConfigurationTest` 共 5 项，`Failures: 0, Errors: 0`。日志确认两条配置均解析为 `shared` 后，启动失败原因为 `Duplicate embedding store key: shared`。

### 改动文件

- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/langchain4j/store/embeding/autoconfigure/EmbeddingStoreAutoConfigurationTest.java`

新增单一 focused regression test `shouldFailWhenMultipleStoresResolveToTheSameKey`，使用两个 `fake` store 配置并显式设置相同的 `key=shared`。

### 提交 hash

- `f49e83b24f395789b8f068b8ee56b73c6ae85005` - `test(vector): cover duplicate embedding store keys`

### Concerns

- 没有生产代码改动。
- RED 阶段无法按传统方式失败，因为目标校验已经存在；测试用于防止该校验回归。
