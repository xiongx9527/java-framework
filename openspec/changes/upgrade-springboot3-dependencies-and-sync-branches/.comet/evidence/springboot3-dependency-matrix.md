# Spring Boot 3 LangChain4j 依赖与适配证据

## 验证环境

- 执行日期：`2026-07-31`
- 工作树：`.worktrees/springboot3-upgrade`
- 分支：`springboot3`
- 验证起点：`afa7a50f1f9062894c2aa14231c707a34d9722bd`
- JDK：Zulu OpenJDK `17.0.13`，`JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home`
- Maven：系统 Maven `3.6.3`

## 依赖管理矩阵

| 坐标 | Core BOM 源声明 | Core effective POM | AI BOM | autoconfigure 实际依赖树 | 消费模块直接版本 |
| --- | --- | --- | --- | --- | --- |
| `dev.langchain4j:langchain4j-bom` | `${langchain4j.version}` = `1.18.0` | import 被有效模型展开；属性为 `1.18.0` | 源 POM 不直接导入；通过 `fw-plus-dependencies` 继承，effective POM 属性为 `1.18.0` | 主线组件解析为 `1.18.0`，BOM beta 组件解析为 `1.18.0-beta28` | 不适用 |
| `dev.langchain4j:langchain4j-redis` | `0.36.2` 显式覆盖 | `0.36.2` | 继承 Core 管理；effective POM 为 `0.36.2` | `0.36.2` | 无 |
| `dev.langchain4j:langchain4j-qianfan` | `0.36.2` 显式覆盖 | `0.36.2` | 继承 Core 管理；effective POM 为 `0.36.2` | `0.36.2` | 无 |
| `dev.langchain4j:langchain4j-dashscope` | `0.36.2` 显式覆盖 | `0.36.2` | 继承 Core 管理；effective POM 为 `0.36.2` | `0.36.2` | 无 |

AI BOM 源 POM 只 import `com.xiongx9527.fw.plus:fw-plus-dependencies:${project.version}`，没有直接声明 `langchain4j.version`、`langchain4j-bom` 或 `dev.langchain4j` 依赖。`help:effective-pom` 会展开继承，因此 AI effective POM 出现 `langchain4j.version=1.18.0` 和 LangChain4j 管理项是继承结果，不是 AI BOM 的重复源声明。

## Maven 解析证据

以下命令均先设置 Java 17；Maven 命令退出码均为 `0`。

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f .worktrees/springboot3-upgrade/fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml help:effective-pom -Doutput=target/effective-pom.xml
mvn -f .worktrees/springboot3-upgrade/fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml help:effective-pom -Doutput=target/effective-pom.xml
mvn -f .worktrees/springboot3-upgrade/fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml dependency:tree -Dincludes=dev.langchain4j -DoutputFile=target/langchain4j-tree.txt
```

- Core effective POM：`langchain4j.version=1.18.0`；Redis、Qianfan、DashScope 均为 `0.36.2`。
- AI effective POM：继承后的 `langchain4j.version=1.18.0`；三项例外均为 `0.36.2`。
- autoconfigure 实际树：`langchain4j-open-ai`、`anthropic`、`ollama`、`langchain4j`、`langchain4j-core` 为 `1.18.0`；BOM 中仍为 beta 的组件解析为 `1.18.0-beta28`；Redis、Qianfan、DashScope 均实际解析为 `0.36.2`。
- brief 指定的 autoconfigure 树本身已包含三项例外，因此无需改选其他消费模块补树；精确扫描三项均命中并退出 `0`。

消费 POM 扫描命令及结果：

```bash
rg -n '<artifactId>langchain4j-(redis|qianfan|dashscope)</artifactId>' \
  .worktrees/springboot3-upgrade --glob 'pom.xml'
```

`rg` 退出码为 `0`。使用 `xmllint` 对命中的 dependency 节点做结构化计数：Core BOM 的 3 个管理节点有 3 个直接 `<version>`；autoconfigure、Redis、Qianfan、DashScope 四个消费 POM 各命中 1 个依赖且直接 `<version>` 计数均为 `0`。

## 定向测试与 RED/GREEN

命令：

```bash
mvn -f .worktrees/springboot3-upgrade/fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml \
  -Dtest=LangChain4jAutoConfigurationRegistrationTest,LangChain4jAdapterAutoConfigurationTest test
```

- RED：首次运行退出码 `1`，共 13 项，`Failures: 1, Errors: 0, Skipped: 0`。`LangChain4jAdapterAutoConfigurationTest` 为 6/6 通过；`LangChain4jAutoConfigurationRegistrationTest` 的 7 项中 1 项失败。失败精确显示测试仍期望旧包 `dev.langchain4j.model.vertexai.*`，生产 `@ConditionalOnClass` 已使用 1.18.0 的 `dev.langchain4j.model.vertexai.gemini.*`。
- 根因：提交 `d3303707e046bf00e4a354e6eeb5f3e12b1ac3b8` 已正确迁移生产 import，但相邻注册测试的期望字符串遗漏同步；实际依赖类、生产注解和自维护 Vertex AI 自动配置三者均使用新包。
- 最小修复：`c26099f2`（`test(ai): 对齐 Vertex AI Gemini 自动配置断言`）只更新测试中的两条包名期望，没有修改生产代码或 POM。
- GREEN：相同命令重跑退出码 `0`，共 13 项，`Failures: 0, Errors: 0, Skipped: 0`；注册测试 7/7、适配器测试 6/6 均通过。结果同时由两个最新 Surefire XML 的 `tests/failures/errors/skipped` 属性核对。

## API 适配提交审计

`d3303707`（`chore(deps): 适配 LangChain4j 1.18.0`）已在当前 `springboot3` 历史中，Task 9 未创建重复适配提交。只读审计确认的具体差异包括：

- `EmbeddingStore#findRelevant(...)` 改为 `search(EmbeddingSearchRequest).matches()`；相邻 retriever 测试和多个 `EmbeddingStore` 测试桩随新 `search(...)` 契约适配。
- Vertex AI Gemini 模型包从 `dev.langchain4j.model.vertexai` 移至 `dev.langchain4j.model.vertexai.gemini`；Task 9 的 RED/GREEN 注册测试直接验证 `@ConditionalOnClass` 边界。
- Weaviate embedding store 构造参数新增 `metadataFieldName`，适配器从定义中透传该字段；相邻 provider adapter 测试覆盖构造与异常边界。
- `EmbeddingModel` 测试实现改用 `embed(String)` 返回单个 `Response<Embedding>`；消息文本访问按 `SystemMessage#text()`、`UserMessage#singleText()`、`AiMessage#text()` 分派。
- 自维护兼容层同步了 Chat/Streaming Chat、Azure OpenAI、DashScope、Ollama、Cassandra/AstraDB、Neo4j、Infinispan、Reactor 等 1.18.0 签名变化；这些属于既有提交审计结果，本任务未再改生产实现。

## 声明结构基线

命令：

```bash
mvn -f .worktrees/springboot3-upgrade/fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml \
  -Dtest=SpringBoot3DependencyBaselineTest test
```

退出码 `0`，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。该 XML 基线测试锁定 Core BOM `1.18.0`、AI BOM 通过 Core BOM 继承、AI 无直接 LangChain4j 管理以及三项 `0.36.2` 覆盖的源声明结构；它不能证明 Maven 最终解析结果，最终版本解析仍以上述 effective POM 和真实 dependency tree 为准。

## 已提交与祖先证据

```bash
git -C .worktrees/springboot3-upgrade merge-base --is-ancestor 0c44f893 HEAD
git -C .worktrees/springboot3-upgrade merge-base --is-ancestor d3303707 HEAD
```

两条命令退出码均为 `0`。`0c44f8938d5a784876124b32670bc981554a574f` 提供 BOM/基线结构，`d3303707e046bf00e4a354e6eeb5f3e12b1ac3b8` 提供 1.18.0 API 适配；Task 9 不创建空提交或重复提交。

## 警告与证据边界

- 所有 Maven 命令均警告系统级与用户级 `settings.xml` 第 213 行存在无法识别的 `repository` 标签；本任务不修改机器级 Maven 配置，解析与测试仍正常完成。
- GREEN 测试编译提示相邻测试使用 deprecated API；运行时提示 Commons Logging 与 `spring-jcl` 潜在冲突、classpath 存在多个 SLF4J provider。测试汇总为零失败，但这些告警没有被视为已修复。
- 首次生成 Core effective POM 时，Maven 本身 `BUILD SUCCESS`，但外层 zsh 记录脚本误用只读变量名 `status`，导致包装进程退出 `1`；根因确认后仅将局部变量名改为 `cmd_rc` 并原样重跑，完整命令退出 `0`。矩阵中的解析结论只采用重跑后的成功证据。
- effective POM 与 dependency tree 位于各模块 `target/`，属于本地生成证据，不纳入 Git；可由本文命令重建。
- 本任务未输出仓库凭据，未 push、rebase、merge 或勾选 OpenSpec 任务。
