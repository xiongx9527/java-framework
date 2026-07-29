# Task 2 报告：向量 Store 选择器

## RED 证据

原定命令：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" ./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding -am -Dtest=DefaultAiEmbeddingStoreFactoryTest test
```

该命令首先在上游 `fw-plus-annotation` 失败，原因是 Surefire 对 `-Dtest` 施加到整个反应器且该模块没有匹配测试：`No tests were executed!`。

为使反应器抵达目标模块，保留相同命令并追加 `-DfailIfNoTests=false`。目标模块在 `DefaultAiEmbeddingStoreFactoryTest.java:102` 的 `getFramework()` `@Override` 处测试编译失败，证明生产接口尚未声明所需契约。

## GREEN 证据

目标工厂测试：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" ./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding -am -Dtest=DefaultAiEmbeddingStoreFactoryTest -DfailIfNoTests=false test
```

结果：`DefaultAiEmbeddingStoreFactoryTest` 4 个测试全部通过。

模块回归测试：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" ./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding test
```

结果：13 个测试全部通过，失败 0、错误 0、跳过 0。

## 提交

`4db8b7e8 feat(ai): select neutral embedding adapter deterministically`

## 修改文件

- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding/src/main/java/com/xiongx9527/fw/plus/ai/store/embedding/AiEmbeddingStoreAdapter.java`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding/src/main/java/com/xiongx9527/fw/plus/ai/store/embedding/DefaultAiEmbeddingStoreFactory.java`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding/src/test/java/com/xiongx9527/fw/plus/ai/store/embedding/DefaultAiEmbeddingStoreFactoryTest.java`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding/src/test/java/com/xiongx9527/fw/plus/ai/store/embedding/DefaultAiEmbeddingStoreRegistryTest.java`

## 风险信号与顾虑

- `AiEmbeddingStoreAdapter` 增加抽象方法，既有 registry 测试中的两个测试适配器已同步补齐 `getFramework()`；外部适配器实现方也需要实现该方法。
- brief 的原始反应器命令会被上游无匹配测试阻断，验证时需追加 `-DfailIfNoTests=false`，或使用模块级回归命令。
- 工厂异常只输出 key、framework、provider、type、候选 framework 与异常类型，不包含 `properties` 或原始异常信息。

## 审查修复（后续提交）

### RED 证据

在 `DefaultAiEmbeddingStoreFactoryTest` 中先新增以下回归场景：

- 空 framework 且零 adapter 时，错误诊断包含 key、framework、provider、type、candidateFrameworks，且不包含 `properties` 或密码值；
- 同一 adapter 对象在列表中出现两次时，空 framework 仍只选择该唯一对象。

执行：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" ./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding -am -Dtest=DefaultAiEmbeddingStoreFactoryTest -DfailIfNoTests=false test
```

结果：失败。`shouldUseDuplicatedStoreAdapterInstanceOnlyOnce` 抛出 `Multiple adapters support store`，诊断中的候选为重复的 `[framework-a, framework-a]`，证明旧实现按列表条目而不是 adapter 对象身份计数。

### GREEN 与编译证据

- 工厂目标测试使用上述命令通过：6 个测试，0 failure、0 error；
- store 模块完整回归：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" ./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding test
```

结果：15 个测试，0 failure、0 error；

- Core LangChain4j store 生产实现编译：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" ./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding -am -DskipTests compile
```

结果：10 个 reactor 项目全部成功；

- 为让 AI Boot 使用当前 Core SPI，先以 JDK 8 安装 store 反应器，再编译 auto-configuration：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" ./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding -am -DskipTests install
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" ./fw-plus-ai/mvnw -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am -DskipTests compile
```

结果：Core install 的 8 个 reactor 项目成功；AI Boot compile 的 7 个 reactor 项目成功。

### 修复内容与风险

- `DefaultAiEmbeddingStoreFactory` 使用 `IdentityHashMap` 驱动的集合按 adapter 对象身份去重；不同 adapter 即使返回相同 framework 仍保留为真实歧义，显式 framework 的严格匹配逻辑不变；
- `DefaultEmbeddingStoreFactory` 与 `LegacyEmbeddingStoreAiAdapter` 均显式返回稳定的 `langchain4j` framework，因此所有当前生产 `AiEmbeddingStoreAdapter` 实现均满足 SPI；
- 中立 Core 未引入 LangChain4j 类型或依赖；
- `AiEmbeddingStoreAdapter#getFramework()` 是新增抽象 SPI，外部实现者仍需按迁移契约补充该方法，旧二进制在调用时仍存在 `AbstractMethodError` 风险；本修复仅覆盖当前仓库生产实现，未启动整体 adapter 迁移。

## 最终复审修复：身份去重优先于 framework 读取

### RED 证据

先在 `DefaultAiEmbeddingStoreFactoryTest` 新增
`shouldReadDuplicatedStoreAdapterFrameworkOnlyOnce`，对同一个 adapter 对象重复输入时断言
`getFramework()` 仅调用一次。

执行：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" ./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding -am -Dtest=DefaultAiEmbeddingStoreFactoryTest -DfailIfNoTests=false test
```

结果：失败，新增测试断言 `expected: <1> but was: <2>`。这证明旧候选扫描在身份去重前读取了重复 adapter 的 framework。

### GREEN 证据

将 `DefaultAiEmbeddingStoreFactory.candidates(...)` 中的 `discovered.add(adapter)` 前移到
`getFramework()` 调用之前，仅对首次发现的对象读取 framework 并创建候选；严格 framework 匹配和异常脱敏逻辑未变。

使用上述工厂测试命令验证：7 个测试全部通过，失败 0、错误 0。

完整模块回归：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" ./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding test
```

结果：16 个测试全部通过，失败 0、错误 0、跳过 0。

### 修改文件

- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding/src/main/java/com/xiongx9527/fw/plus/ai/store/embedding/DefaultAiEmbeddingStoreFactory.java`
- `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding/src/test/java/com/xiongx9527/fw/plus/ai/store/embedding/DefaultAiEmbeddingStoreFactoryTest.java`
- `.superpowers/sdd/task-2-report.md`

### 风险

- 身份去重刻意使用对象身份而非 `equals()`：不同 adapter 实例即使逻辑相等或 framework 相同，仍保留为真实多候选，符合既有歧义处理契约。
- `getFramework()` 仍可能在首次发现 adapter 时抛出异常，工厂继续以不含 properties 的安全上下文包装该异常。
