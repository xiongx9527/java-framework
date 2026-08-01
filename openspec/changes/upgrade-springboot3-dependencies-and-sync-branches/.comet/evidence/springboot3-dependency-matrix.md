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

## Task 10：其他 Spring Boot 3 依赖审计（2026-07-31）

### 固定与排除边界

| 项目 | 精确基线 | 判定 |
| --- | --- | --- |
| Spring Boot / Cloud / Cloud Alibaba | `3.5.8` / `2025.0.0` / `2025.0.0.0` | 固定，不采用 Versions 插件候选。 |
| Magic-API / Magic-Script | `2.2.2` / `1.9.0` | 内部维护坐标，固定。 |
| Spring AI / Spring AI Alibaba | `1.0.1` / `1.0.1` | 固定。 |
| LangChain4j 方案 A | Core `1.18.0`；Redis、Qianfan、DashScope 均为显式 `0.36.2`；AI BOM 仅经 Core BOM 继承 | 固定，不采用 `1.18.1`。 |
| Maven、Wrapper、构建插件、内部 Nexus | 当前声明和仓库配置 | 本任务未修改。 |
| TinyEngine | `springboot3_merge..HEAD` 未命中 `add-tiny-engine-(ai|boot)-starter` | 不引入任何 TinyEngine 变更。 |

候选均来自 `versions:display-property-updates -DallowSnapshots=false -DallowMajorUpdates=true` 的元数据结果。判定缩写：`A` 为“稳定候选、Java 17 对应层 `compile` 通过、无平台冲突，已采用”；`R1` 为“稳定候选但缺少本任务内定向运行时/API 兼容验证，不批量升级”；`R2` 为“重大或基础设施/加密/ORM/协议行为升级，需额外适配或运行时测试”；`R3` 为“预发布或异常元数据版本”；`R4` 为“固定平台/方案边界”；`R5` 为“与固定 Spring Boot/Cloud/Jakarta/Netty 平台边界冲突”。

### Core BOM 候选矩阵

| 属性/坐标 | 当前 | 候选 | 采用 | Java 17 | 固定平台冲突 | API 适配 | 判定/原因 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `aliyun-sdk-oss.version` | 3.18.1 | 3.18.5 | 3.18.5 | 通过（Core） | 无 | 无 | A |
| `assertj-core.version` | 3.26.3 | 4.0.0-M1 | 保留 | 未验证 | 无 | 未做 | R3 |
| `aviator.version` | 5.4.3 | 5.4.4 | 保留 | 未验证 | 无 | 表达式运行时未验收 | R1 |
| `aws-java-sdk-s3.version` | 1.12.794 | 1.12.797 | 1.12.797 | 通过（Core） | 无 | 无 | A |
| `azure-storage-blob.version` | 12.32.0 | 12.36.0-beta.1 | 保留 | 未验证 | 无 | 未做 | R3 |
| `bce-java-sdk.version` | 0.10.401 | 0.10.442 | 0.10.442 | 通过（Core） | 无 | 无 | A |
| `bcmail-jdk14.version` | 1.64 | 1.85 | 保留 | 未验证 | 无 | 加密行为未验收 | R2 |
| `bcprov-jdk14.version` | 1.64 | 1.85.1 | 保留 | 未验证 | 无 | 加密行为未验收 | R2 |
| `bcprov-jdk15on.version` | 1.69 | 1.70 | 保留 | 未验证 | 无 | 历史坐标兼容性未验收 | R2 |
| `bcprov-jdk18on.version` | 1.78.1 | 1.85 | 保留 | 未验证 | 无 | 加密行为未验收 | R2 |
| `byte-buddy.version` | 1.17.6 | 1.18.11-jdk5 | 保留 | 未验证 | 无 | 字节码运行时未验收 | R1 |
| `checker-qual.version` | 3.19.0 | 4.2.1 | 保留 | 未验证 | 无 | 注解传播未验收 | R1 |
| `commons-beanutils.version` | 1.9.2 | 20030211.134440 | 保留 | 未验证 | 无 | 异常元数据 | R3 |
| `commons-collections3.version` | 3.2.2 | 20040616 | 保留 | 未验证 | 无 | 异常元数据 | R3 |
| `commons-collections4.version` | 4.4 | 4.5.0 | 保留 | 未验证 | 无 | 公共工具行为未验收 | R1 |
| `commons-compress.version` | 1.21 | 1.28.0 | 保留 | 未验证 | 无 | 压缩行为未验收 | R1 |
| `commons-io2.version` | 2.18.0 | 20030203.000550 | 保留 | 未验证 | 无 | 异常元数据 | R3 |
| `commons-lang2.version` | 2.6 | 20030203.000129 | 保留 | 未验证 | 无 | 异常元数据 | R3 |
| `commons-lang3.version` | 3.17.0 | 3.20.0 | 保留 | 未验证 | 无 | 公共工具行为未验收 | R1 |
| `commons-logging.version` | 1.3.4 | 1.4.0 | 保留 | 未验证 | 无 | 日志桥接未验收 | R1 |
| `commons-net.version` | 3.11.1 | 20030805.205232 | 保留 | 未验证 | 无 | 异常元数据 | R3 |
| `commons-text.version` | 1.14.0 | 1.15.0 | 保留 | 未验证 | 无 | 文本行为未验收 | R1 |
| `commons-validator.version` | 1.9.0 | 1.10.1 | 保留 | 未验证 | 无 | 校验行为未验收 | R1 |
| `cos_api.version` | 5.6.259 | 5.6.275 | 5.6.275 | 通过（Core） | 无 | 无 | A |
| `dom4j.version` | 2.1.4 | 2.2.0 | 保留 | 未验证 | 无 | XML 行为未验收 | R1 |
| `druid.version` | 1.2.27 | 1.2.28 | 1.2.28 | 通过（Core） | 无 | 无 | A |
| `dynamic-datasource.version` | 4.3.1 | 4.5.0 | 保留 | 未验证 | 无 | 自动配置行为未验收 | R2 |
| `dynamictp.version` | 1.2.1 | 1.2.2-x | 保留 | 未验证 | 无 | 未做 | R3 |
| `error_prone_annotations.version` | 2.36.0 | 2.50.0 | 保留 | 未验证 | 无 | 注解行为未验收 | R1 |
| `esdk-obs-java.version` | 3.25.10 | 3.26.6 | 保留 | 未验证 | 无 | 对象存储运行时未验收 | R1 |
| `fastjson.version` | 2.0.53 | 2.0.63 | 2.0.63 | 通过（Core） | 无 | 无 | A |
| `fastjson2.version` | 2.0.53 | 2.0.63 | 2.0.63 | 通过（Core） | 无 | 无 | A |
| `fontbox.version` | 3.0.3 | 3.0.8 | 保留 | 未验证 | 无 | 文档解析未验收 | R1 |
| `google-cloud-storage.version` | 2.60.0 | 2.71.0 | 保留 | 未验证 | 无 | 对象存储运行时未验收 | R1 |
| `guava.version` | 31.1-jre | 33.6.0-jre | 保留 | 未验证 | 无 | 广泛公共 API 未验收 | R1 |
| `hotswap-agent.version` | 2.0.2 | 2.0.3 | 保留 | 未验证 | 无 | 代理运行时未验收 | R1 |
| `hutool.version` | 5.8.34 | 5.8.47 | 5.8.47 | 通过（Core） | 无 | 无 | A |
| `infinispan.version` | 13.0.22.Final | 16.0.0.Dev03 | 保留 | 未验证 | 无 | 未做 | R3 |
| `jakarta-el.version` | 4.0.2 | 5.0.0-M1 | 保留 | 未验证 | Jakarta 基线 | 未做 | R3/R5 |
| `jakarta.servlet-api.version` | 6.1.0 | 6.2.0-M2 | 保留 | 未验证 | Jakarta 基线 | 未做 | R3/R5 |
| `java-diff-utils.version` | 4.16 | 4.17 | 4.17 | 通过（Core） | 无 | 无 | A |
| `javassist.version` | 3.30.2-GA | 3.32.0-GA | 保留 | 未验证 | 无 | 字节码行为未验收 | R1 |
| `jedis.version` | 5.0.0 | 8.0.0-beta1 | 保留 | 未验证 | 无 | 未做 | R3 |
| `jfreechart.version` | 1.5.4 | 1.5.6 | 1.5.6 | 通过（Core） | 无 | 无 | A |
| `jimubi.version` | 2.3.2 | 2.5.0.1 | 保留 | 未验证 | 内部报表边界 | 报表运行时未验收 | R2 |
| `jimureport.version` | 2.3.4 | 2.5.0 | 保留 | 未验证 | 内部报表边界 | 报表运行时未验收 | R2 |
| `jodd-core.version` | 5.0.13 | 5.3.0 | 保留 | 未验证 | 无 | 公共工具行为未验收 | R1 |
| `json.version` | 20250517 | 20260719 | 保留 | 未验证 | 无 | JSON 行为未验收 | R1 |
| `json-path.version` | 2.9.0 | 3.0.0 | 保留 | 未验证 | 无 | 主版本 API 未适配 | R2 |
| `jsqlparser.version` | 4.6 | 5.3 | 保留 | 未验证 | 无 | 主版本 AST API 未适配 | R2 |
| `kryo.version` | 5.0.3 | 5.6.2 | 保留 | 未验证 | 无 | 序列化行为未验收 | R1 |
| `langchain4j.version` | 1.18.0 | 1.18.1 | 1.18.0 | 固定 | 方案 A | 不适用 | R4 |
| `libphonenumber.carrier.version` | 1.235 | 2.35 | 保留 | 未验证 | 无 | 数据格式行为未验收 | R2 |
| `libphonenumber.geocoder.version` | 2.245 | 3.35 | 保留 | 未验证 | 无 | 数据格式行为未验收 | R2 |
| `libphonenumber.version` | 8.13.51 | 9.0.36 | 保留 | 未验证 | 无 | 主版本 API 未适配 | R2 |
| `logstash-logback-encoder.version` | 8.0 | 9.0 | 保留 | 未验证 | 无 | 主版本日志行为未验收 | R2 |
| `minio.version` | 8.5.17 | 9.0.3 | 保留 | 未验证 | 无 | 主版本对象存储 API 未适配 | R2 |
| `mybatis-flex.version` | 1.11.4 | 1.11.8 | 保留 | 未验证 | 无 | ORM 运行时未验收 | R1 |
| `mybatis-plus-join.version` | 1.5.2 | 1.5.9 | 保留 | 未验证 | 无 | ORM 运行时未验收 | R1 |
| `mybatis-spring.version` | 2.1.2 | 4.1.0 | 保留 | 未验证 | 无 | 主版本 Spring 集成未适配 | R2 |
| `neo4j-java-driver.version` | 4.4.26 | 6.2.0 | 保留 | 未验证 | 无 | 主版本驱动 API 未适配 | R2 |
| `okhttp.version` | 5.3.2 | 5.4.0 | 保留 | 未验证 | 无 | HTTP 运行时未验收 | R1 |
| `okio.version` | 3.16.4 | 3.18.1 | 保留 | 未验证 | 无 | HTTP 运行时未验收 | R1 |
| `opentelemetry.version` | 1.56.0 | 1.64.0 | 保留 | 未验证 | 无 | 观测运行时未验收 | R1 |
| `poi-ooxml.version` | 4.1.2 | 5.5.1 | 保留 | 未验证 | 无 | 主版本文档 API 未适配 | R2 |
| `poi-tl.version` | 1.12.2 | 1.12.3-beta1 | 保留 | 未验证 | 无 | 未做 | R3 |
| `poi.version` | 4.1.2 | 5.5.1 | 保留 | 未验证 | 无 | 主版本文档 API 未适配 | R2 |
| `protobuf-java.version` | 3.25.1 | 4.36.0-RC1 | 保留 | 未验证 | 无 | 未做 | R3 |
| `redis-keeper.version` | 2024.2.0 | 2025.2.1 | 保留 | 未验证 | 无 | Redis 运行时未验收 | R2 |
| `sa-token.version` | 1.44.0 | 1.45.0 | 1.45.0 | 通过（Core） | 无 | 无 | A |
| `sardine.version` | 5.10 | 5.13 | 5.13 | 通过（Core） | 无 | 无 | A |
| `sensitive-word.version` | 0.24.0 | 0.29.5 | 保留 | 未验证 | 无 | 词库行为未验收 | R1 |
| `spring-boot-dependencies.version` | 3.5.8 | 4.1.0 | 3.5.8 | 固定 | Spring Boot | 不适用 | R4 |
| `spring-cloud-alibaba-dependencies.version` | 2025.0.0.0 | 2025.1.0.0 | 2025.0.0.0 | 固定 | Spring Cloud Alibaba | 不适用 | R4 |
| `spring-cloud-dependencies.version` | 2025.0.0 | 2025.1.2 | 2025.0.0 | 固定 | Spring Cloud | 不适用 | R4 |
| `spring-cloud-tencent-dependencies.version` | 1.14.0-2023.0.0-RC2 | 2.1.2.1-2021.0.9 | 保留 | 未验证 | Spring Cloud 基线不匹配 | 不适用 | R5 |
| `spring-plugin.version` | 2.0.0.RELEASE | 3.0.0 | 保留 | 未验证 | 无 | 主版本 API 未适配 | R2 |
| `vesoft-java-client.version` | 3.8.4 | 5.0.0-rc2 | 保留 | 未验证 | 无 | 未做 | R3 |
| `x-file-storage-spring.version` | 2.2.1 | 2.3.0 | 保留 | 未验证 | 无 | 文件存储运行时未验收 | R1 |
| `xmlgraphics.version` | 2.10 | 2.11 | 保留 | 未验证 | 无 | 文档渲染未验收 | R1 |
| `zstd-jni.version` | 1.5.5-5 | 1.5.7-12 | 1.5.7-12 | 通过（Core） | 无 | 无 | A |
| `zxing.core.version` | 3.5.3 | 3.5.4 | 3.5.4 | 通过（Core） | 无 | 无 | A |

### Boot BOM 候选矩阵

| 属性/坐标 | 当前 | 候选 | 采用 | Java 17 | 固定平台冲突 | API 适配 | 判定/原因 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `debezium.version` | 1.9.8.Final | 3.7.0.Alpha1 | 保留 | 未验证 | 无 | 未做 | R3 |
| `dynamic-tp.version` | 1.2.2 | 1.2.2-x | 保留 | 未验证 | 无 | 未做 | R3 |
| `elasticsearch-sdk-java.version` | 8.19.8 | 9.4.4 | 保留 | 未验证 | Spring Boot ES 8 基线 | 主版本 API 未适配 | R5 |
| `grpc.version` | 1.65.1 | 1.83.1 | 保留 | 未验证 | 无 | RPC 运行时未验收 | R1 |
| `influxdb.version` | 6.7.0 | 8.0.0 | 保留 | 未验证 | 无 | 主版本 API 未适配 | R2 |
| `jackson.version` | 2.20.1 | 2.22.1 | 保留 | 未验证 | Spring Boot JSON 基线 | 不适用 | R5 |
| `jakarta.activation.version` | 2.1.4 | 2.2.0-M2 | 保留 | 未验证 | Jakarta 基线 | 未做 | R3/R5 |
| `jakarta.ws.rs.version` | 3.1.0 | 4.0.0 | 保留 | 未验证 | Jakarta 基线 | 不适用 | R5 |
| `jasypt.version` | 3.0.5 | 4.0.4 | 保留 | 未验证 | 无 | 主版本 API 未适配 | R2 |
| `jjwt.version` | 0.12.7 | 0.13.0 | 0.13.0 | 通过（Boot） | 无 | 无 | A |
| `jspecify.version` | 1.0.0 | 1.0.1 | 1.0.1 | 通过（Boot） | 无 | 无 | A |
| `kotlin.version` | 2.2.21 | 2.4.20-Beta2 | 保留 | 未验证 | 无 | 未做 | R3 |
| `mica.version` | 2.7.18.9 | 4.1.0 | 保留 | 未验证 | 无 | 主版本 MQTT API 未适配 | R2 |
| `netty-websocket.version` | 0.12.0 | 0.13.0 | 保留 | 未验证 | Netty 基线 | 不适用 | R5 |
| `netty.version` | 4.1.128.Final | 4.2.16.Final | 保留 | 未验证 | Spring Boot Netty 基线 | 不适用 | R5 |
| `ognl.version` | 3.4.7 | 3.5.0-BETA5 | 保留 | 未验证 | 无 | 未做 | R3 |
| `plumelog.version` | 3.5.2 | 3.5.3 | 3.5.3 | 通过（Boot） | 无 | 无 | A |
| `pulsar-client.version` | 2.11.2 | 5.0.0-M1 | 保留 | 未验证 | 无 | 未做 | R3 |
| `resilience4j.version` | 2.3.0 | 2.4.0 | 保留 | 未验证 | 无 | 熔断运行时未验收 | R1 |
| `rocketmq.version` | 5.1.3 | 5.3.2 | 5.3.2 | 通过（Boot） | 无 | 无 | A |
| `sentinel.version` | 1.8.6 | 2.0.0-alpha | 保留 | 未验证 | 无 | 未做 | R3 |
| `snappy-java.version` | 1.1.8.4 | 1.1.10.8 | 1.1.10.8 | 通过（Boot） | 无 | 无 | A |
| `spring-boot-admin.version` | 3.5.6 | 4.1.2 | 保留 | 未验证 | Spring Boot 4 基线 | 不适用 | R5 |
| `wildfly.version` | 1.7.0.Final | 2.0.1 | 保留 | 未验证 | 无 | 主版本 API 未适配 | R2 |

### Cloud 与 AI BOM 候选矩阵

| 层/属性 | 当前 | 候选 | 采用 | Java 17 | 固定平台冲突 | API 适配 | 判定/原因 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Cloud `servo-core.version` | 0.12.21 | 0.13.2 | 0.13.2 | 通过（Cloud） | 无 | 无 | A |
| Cloud `io.github.openfeign.version` | 13.5 | 13.13 | 保留 | 通过（Cloud） | Spring Cloud OpenFeign 4.3.0 管理 Feign 13.6 | 不适用 | R5 |
| AI `agents-flex.version` | 1.3.4 | 2.2.6 | 保留 | 未验证 | 无 | 主版本 API 未适配 | R2 |
| AI `easyAi.version` | 1.4.9 | 1.6.8 | 保留 | 未验证 | 无 | AI 运行时未验收 | R1 |
| AI `milvus-sdk-java.version` | 2.6.3 | 3.0.5 | 保留 | 未验证 | 无 | 主版本向量 API 未适配 | R2 |
| AI `qdrant-sdk-java.version` | 1.11.0 | 1.18.3 | 保留 | 未验证 | 无 | 向量 API 运行时未验收 | R1 |
| AI `weaviate-sdk-java.version` | 4.6.0 | 5.6.0 | 保留 | 未验证 | 无 | 主版本向量 API 未适配 | R2 |

### Task 10 验证结论

- Java 17 系统 Maven 下的四层 `compile` 均为 `BUILD SUCCESS`：Core 64 模块（44.678 秒）、Boot 49 模块（04:40）、Cloud 14 模块（36.011 秒）、AI 17 模块（24.964 秒）。
- Core 与 Boot `dependency:analyze` 均为 `BUILD SUCCESS`（01:01 / 03:00）；分析器列出的 starter、BOM、内部模块、SPI/反射、自动配置和运行时依赖均未据此删除。
- Spring Boot 3 工作树中的 OptimisticLocker 静态扫描只命中 `MpBaseAutoConfiguration` 的 `OptimisticLockerInnerInterceptor` 注册；保留直接依赖与自动配置，不新增替代实现。
- 风险信号：Maven settings 两处未识别 `repository` 标签、阿里云若干元数据缺失 checksum、Boot 生命周期的 npm audit 报告 18 个既有漏洞和 3 个待批准脚本、以及未 `install` 新 Core SNAPSHOT 时下游仍可能解析本地仓库的旧 Core BOM。
