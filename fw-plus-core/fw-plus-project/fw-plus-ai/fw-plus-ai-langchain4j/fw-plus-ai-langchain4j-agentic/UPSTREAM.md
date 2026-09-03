# Agentic 上游来源与 Java 8 修改说明

## 固定来源

- 仓库：<https://github.com/langchain4j/langchain4j>
- Tag：`1.19.0`
- Commit：`09a2aa3861ba468af3507c28f5676048cd5db4c6`
- 官方 artifact：`dev.langchain4j:langchain4j-agentic:1.19.0-beta29`
- 官方复制路径：`langchain4j-agentic/src/main/java`
- 本地生产源码路径：`src/main/java`
- 官方生产 Java 文件数：153
- 许可证：Apache License 2.0；本仓库的许可证副本位于 `fw-plus-core/LICENSE`

`UPSTREAM-MANIFEST.txt` 是稳定排序、可机器比较的官方生产源码清单，每行一个相对于 `src/main/java` 的路径。固定版本的官方 `sources` JAR与该清单均包含153项，路径集合完全一致。

## 本地模块与依赖归属

Boot 2 / Java 8 技术线以
`com.xiongx9527.fw.plus:fw-plus-ai-langchain4j-agentic:${revision}` 提供完整Agentic回移。
LangChain4j `0.35.0` 没有官方Agentic artifact，因此这是新增能力，不是替换一个不存在的
`0.35.0` JAR。

本地生产源码继续使用上游 `dev.langchain4j.agentic.*` FQCN。Agentic所需但`0.35.0`
缺失或ABI不足的依赖按官方artifact owner承载：

| 上游职责 | 本地 Java 8 owner |
| --- | --- |
| `dev.langchain4j:langchain4j` | `fw-plus-ai-langchain4j` JAR |
| `dev.langchain4j:langchain4j-core` | `fw-plus-ai-langchain4j-core` JAR |
| Spring Boot覆盖类 | `fw-plus-ai-langchain4j-spring-boot-starter` JAR |
| Agentic生产实现 | `fw-plus-ai-langchain4j-agentic` JAR |

`fw-plus-ai-langchain4j-parent` 是唯一的 Maven parent/aggregator，只产出POM，不进入运行时依赖树。Agentic不复制aggregate/Core/Spring实现，也不默认依赖官方Java 17 Agentic JAR。

## Java 8 回移修改

与固定官方 `sources` JAR逐文件比较；59个修改文件的稳定排序路径见
`JAVA8-MODIFIED-MANIFEST.txt`：

- 94个官方文件保持字节一致；
- 59个官方文件为Java 8兼容或已验证的行为修复而修改；
- 另有3个仅属于本地Java 8实现的helper：
  - `dev/langchain4j/agentic/internal/AgenticRecordComponentMetadata.java`
  - `dev/langchain4j/agentic/internal/CollectionHelper.java`
  - `dev/langchain4j/agentic/internal/StringHelper.java`

修改范围包括：

- 23个上游`record`改写为保持canonical constructor、component accessor、泛型/注解公开面以及值语义的`final class`；
- pattern matching、switch expression/arrow switch、text block和局部变量`var`改写为Java 8语法；
- `List.of`/`Set.of`/`Map.of`、`copyOf`、`Stream.toList`、`Optional.ifPresentOrElse`、`Predicate.not`、`Files.writeString`、`String.isBlank`和`Class.isRecord`等Java 9+ API改为Java 8等价实现；
- `AgenticRecordComponentMetadata`显式保存上游record component信息，供Supervisor描述和兼容门禁使用；
- 兼容LangChain4j `0.35.0`的Model、Tool、AiServices、Invocation、Guardrail、Observability、JSON codec与并发执行API，并保留旧ABI所需入口；
- JDK 8的`java.lang.Deprecated`不包含Java 9新增的`since`/`forRemoval`元素，因此`AgentBuilder.maxSequentialToolsInvocations(int)`保留`@Deprecated`，但无法表达上游`since = "1.15.0"`。API parity仅对这一精确类型、方法和注解元素做Java 8归一化，其他注解描述和值仍严格比较。

这些修改不承诺与官方Java 17 JAR字节码二进制兼容，也不承诺跨JVM直接交换序列化对象；目标是Java源码公开面、主要调用方式、状态转换和可观察行为兼容。

## 受控 overlay

aggregate/Core对官方`0.35.0`同FQCN的必要替换登记在
`../compat/overlay-allowlist.tsv`。两个owner JAR在打包期只合并各自官方owner，排除由本地源码提供的allowlist class；最终生产classpath只保留一份`dev.langchain4j.*` class。

`OverlayAllowlistContractTest`与`AgenticOverlayPackagingIT`共同验证allowlist格式、测试锚点、Shade excludes、owner边界、成品字节、Java 8 class major、重复class、SPI发现和生产classpath来源。

## 测试与验证范围

默认测试不需要真实模型凭据、网络或外部服务。验证范围包括：

- 153项来源清单、3项helper allowlist与Agentic包边界；
- 官方/本地命名FQCN和public/protected API对照；
- 23个record替代类型的constructor/accessor/泛型/注解与`equals`/`hashCode`/`toString`契约；
- Agent、Scope、Sequence、Parallel、Parallel Mapper、Loop、Conditional、Planner、Supervisor、Human-in-the-loop、暂停恢复、异步/流式与Observability离线行为；
- Tool调用、参数/error handler、补偿、异常、流式取消边界、并行兄弟失败与持久化恢复；
- 真实public root proxy覆盖Sequence/Conditional/Loop/Parallel、Planner、Supervisor/listener继承、HITL挂起后恢复且不重跑、JSON store reload、AsyncResponse与StreamingResponse；
- Agentic Surefire 69项与Failsafe packaging IT 8项；IT在`integration-test`执行一次，独立`verify` execution只校验结果；
- aggregate/Core/Spring starter/Agentic成品、依赖树与AI Boot直接消费者。

已知运行边界：LangChain4j `0.35.0`的字符串式streaming callback不提供可取消handle，兼容层通过`PartialResponseContext`暴露一个明确拒绝取消的handle；`cancel()`抛`UnsupportedFeatureException`且`isCancelled()`保持`false`。Parallel root沿固定上游`CompletableFuture.allOf`语义等待所有兄弟结束后传播失败，不主动取消仍在运行的兄弟；测试对此做确定性断言，不把“未取消”伪报为成功取消。

Maven Wrapper运行文件未随当前worktree提供时，使用同版本Maven 3.6.3与JDK 8执行等价命令；这不改变项目的Java 8产物要求。

## 许可与修改声明

本模块包含来自Apache License 2.0项目LangChain4j的衍生源码。分发时必须保留Apache 2.0许可证副本、本文的固定来源信息和上述显著修改说明。`UPSTREAM-MANIFEST.txt`只记录153个官方生产Java文件；POM、资源、测试、本文和3个本地helper不计入该数量。

## 隔离边界

- Agentic生产源码只位于`dev.langchain4j.agentic.*`；
- 依赖闭包由对应aggregate/Core owner JAR承载，不并入Agentic JAR；
- A2A与MCP使用上游可选SPI边界，本模块不默认加入其实现artifact；
- 本次不实现Agent Platform、Rule、RAG、MCP Client或provider artifact重命名。
