# LangChain4j Agentic Java 8 回移

## Requirement: 固定且可追溯的上游基线

系统必须将 `dev.langchain4j:langchain4j-agentic:1.19.0-beta29` 的完整生产源码作为本次 Java 8 回移基线。来源必须固定为 LangChain4j tag `1.19.0` 和 commit `09a2aa3861ba468af3507c28f5676048cd5db4c6`，并在模块文档中记录仓库、tag、commit、artifact、许可证、复制路径和文件清单。

### Scenario: 实现期间上游发布新版本

- **WHEN** 上游在本次 Build 或 Verify 期间发布新版本
- **THEN** 当前 change 仍使用已固定的 `1.19.0-beta29` 基线
- **AND** 新版本只作为后续独立升级需求处理

## Requirement: 完整生产源码覆盖

`fw-plus-ai-langchain4j-agentic` 必须包含上游基线 `langchain4j-agentic/src/main/java` 中每个生产 Java 源文件的 Java 8 对应实现。不得因迁移困难删除公开类型、将完整模块缩减为 ReAct 子集，或以统一空实现冒充能力。

### Scenario: 对比生产文件清单

- **WHEN** 验证器比较上游基线与本地模块的生产源码清单
- **THEN** 上游 153 个生产 Java 文件均有对应本地实现
- **AND** 所有实现都位于 `dev.langchain4j.agentic.*` 包空间

## Requirement: Java 源码与反射接口兼容

本地回移版必须保持上游的包名、公开类名、公开嵌套类型名和类层次。除上游 `record` 外，公开类型种类必须保持一致。上游 `record` 是 Java 8 回移唯一允许改变的类型种类，必须表示为普通 `final class`，并保持 canonical constructor、component accessor 的名称/顺序/泛型/注解公开面以及 `equals`、`hashCode`、`toString` 值语义。其他 `public` 与 `protected` 构造器、方法、字段、泛型和注解应保持源码及反射兼容；若 Java 8 类型系统无法表达完全相同的字节码形态，必须记录差异并证明上游主要示例仍可只替换 Maven 坐标后编译。

### Scenario: 使用上游官方示例

- **WHEN** Java 8 示例将依赖从官方 `langchain4j-agentic` 替换为 `fw-plus-ai-langchain4j-agentic`
- **THEN** 示例的 Java import、类名和主要调用代码无需修改即可编译

### Scenario: 比较公开类型

- **WHEN** 验证器生成上游与本地的公开类型/FQCN 清单
- **THEN** 本地没有缺失、改包或擅自重命名的公开类型

## Requirement: Java 8 运行兼容

本地生产源码必须可使用 Java 8 编译与运行。所有 Java 17 语法和 Java 9+ JDK API 必须改写为 Java 8 等价实现，且产物 class major version 不高于 52。

### Scenario: Java 8 编译

- **WHEN** 使用 JDK 8 或等价 `--release 8` 工具链构建模块
- **THEN** 编译成功
- **AND** 不存在 `record`、模式匹配、switch expression、text block、局部变量 `var` 或 Java 9+ API 残留
- **AND** 生成类的 major version 为 52

## Requirement: LangChain4j 0.35 依赖闭包

回移版必须运行在项目既有 LangChain4j `0.35.0` 基线上。Agentic 公开接口或行为依赖但 `0.35.0` 缺失的能力，必须通过明确的兼容实现并由原官方 artifact 对应的 fw-plus 模块承载；不得引入 Java 17 字节码构件，也不得擅自修改 Agentic 包名或类名以绕过依赖。

官方 `langchain4j` aggregate owner 的兼容 API/实现必须由真实 JAR `com.xiongx9527.fw.plus:fw-plus-ai-langchain4j:${revision}` 承载；官方 `langchain4j-core` owner 的兼容 API/实现必须由真实 JAR `com.xiongx9527.fw.plus:fw-plus-ai-langchain4j-core:${revision}` 承载。现有同名聚合 POM 的 parent/aggregator 职责必须迁移到 `fw-plus-ai-langchain4j-parent`，不得使用 classifier、attached JAR 或偶然 classpath 顺序替代普通 Maven JAR 坐标。

### Scenario: 发现现代 Core 缺失类型

- **WHEN** Agentic 生产源码引用 `0.35.0` 不存在或不兼容的 Model、Tool、AiServices、Invocation、Guardrail、Observability、JSON codec 或并发类型
- **THEN** Build 记录其官方 artifact 归属和兼容映射
- **AND** 提供 Java 8 兼容实现或新增对应命名的 fw-plus 回移模块
- **AND** 不删除依赖该类型的 Agentic 公开能力

### Scenario: 检查生产依赖树

- **WHEN** 验证器解析 `fw-plus-ai-langchain4j-agentic` 的生产依赖
- **THEN** 依赖与 LangChain4j `0.35.0` 基线兼容
- **AND** 通过本地 `fw-plus-ai-langchain4j` 与 `fw-plus-ai-langchain4j-core` JAR 获得 aggregate/Core 兼容能力
- **AND** 不包含未计划的 LangChain4j 1.x 或 Java 17 字节码依赖

### Scenario: 检查受控 overlay

- **WHEN** 本地 aggregate/Core JAR 覆盖官方 `0.35.0` 已存在但 ABI 不足的同 FQCN 类型
- **THEN** 每个打包期替换类型都登记在 UTF-8 TSV `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/compat/overlay-allowlist.tsv`
- **AND** 空行忽略，只有行首 `#` 表示注释；其他行严格为 5 个 tab 分隔列 `<binary-fqcn>\t<owner-gav>\t<reason>\t<api-test>\t<behavior-test>`，拒绝 BOM、重复 FQCN、非法注释/列数/空字段/owner/锚点
- **AND** owner GAV 只允许 `dev.langchain4j:langchain4j:${langchain4j.version}` 或 `dev.langchain4j:langchain4j-core:${langchain4j.version}`；测试锚点严格为 `<fully-qualified-test-class>#<method>`
- **AND** `OverlayAllowlistContractTest` 位于 `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-agentic/src/test/java/dev/langchain4j/agentic/compat/OverlayAllowlistContractTest.java`，在 Surefire `test` 阶段反射确认锚点方法存在且带 `@Test`，解析 aggregate/Core POM XML并双向核对 allowlist 与 Shade excludes
- **AND** aggregate/Core 将精确官方 owner 声明为 direct optional compile dependency：aggregate 只声明 `dev.langchain4j:langchain4j:${langchain4j.version}`，Core 只声明 `dev.langchain4j:langchain4j-core:${langchain4j.version}`；`maven-shade-plugin:3.6.1` 在 `package` 阶段的 `artifactSet.includes` 分别只列对应官方 owner，插件自动保留当前项目 main artifact并排除其他 project dependency，`createDependencyReducedPom=false`
- **AND** aggregate 的官方 optional compile 依赖排除官方 Core，并以普通 compile 依赖引用本地 Core；本地 Core 不在 aggregate 的 `artifactSet.includes` 中，因此 aggregate成品不得包含本地 Core或其他依赖 class；Shade 合并 `META-INF/services/*` 并移除无效签名元数据
- **AND** Agentic 的 `maven-dependency-plugin:build-classpath` 在 `pre-integration-test` 以 `includeScope=runtime` 生成 `target/agentic-runtime-classpath.txt`
- **AND** `maven-failsafe-plugin` 显式绑定 `failsafe:integration-test` 到 `integration-test`、`failsafe:verify` 到 `verify`，运行 `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-agentic/src/test/java/dev/langchain4j/agentic/compat/AgenticOverlayPackagingIT.java`
- **AND** Agentic POM 基于 `${main.basedir}` 定义并注入精确路径：aggregate JAR `${main.basedir}/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/target/fw-plus-ai-langchain4j-${revision}.jar`、Core JAR `${main.basedir}/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-core/target/fw-plus-ai-langchain4j-core-${revision}.jar`、对应 `target/classes`、runtime classpath `${project.build.directory}/agentic-runtime-classpath.txt`、Surefire报告 `${project.build.directory}/surefire-reports`；Agentic JAR使用 `${project.build.directory}/${project.build.finalName}.jar`
- **AND** IT以 packaged JAR 与 runtime依赖JAR作为生产来源，只以 aggregate/Core `target/classes` 比较本地替换字节
- **AND** IT 证明 aggregate 成品不含任何仅属于本地 Core 的 class，最终 Core class 只来自本地 Core JAR；allowlist 替换 class 成品字节与本地源码产物一致，所有测试锚点在 Surefire XML中实际执行并通过
- **AND** IT验证固定 SPI 三元组：接口 `dev.langchain4j.service.ParameterNameResolver`、descriptor `META-INF/services/dev.langchain4j.service.ParameterNameResolver`、provider `dev.langchain4j.agentic.internal.AgenticParameterNameResolver`，并用隔离 runtime `URLClassLoader` 通过 `ServiceLoader` 精确发现 provider
- **AND** 最终生产 classpath 任意重复 `dev.langchain4j.*` class、未登记打包替换、allowlist测试锚点未执行通过或官方替换 class 未被排除都会使 Maven `verify` 失败
- **AND** 失败诊断必须输出 binary FQCN、全部来源 JAR、owner 与 allowlist 状态

## Requirement: Agentic 行为完整性

回移版必须保留上游 Agent、AgenticScope、Sequence、Parallel、Parallel Mapper、Loop、Conditional、Supervisor、Planner、Human-in-the-loop、暂停恢复、异步/流式执行和 Observability 等生产能力。允许底层实现为 Java 8 重写，但状态转换、关键事件、异常传播和并发失败语义必须由测试覆盖。

### Scenario: 无外部模型的行为测试

- **WHEN** 测试使用固定假模型、假工具和内存 Scope Store 执行主要 Agentic 模式
- **THEN** 各模式按预期产生结果和状态
- **AND** Tool 调用、异常、取消、并行兄弟失败、暂停与恢复等关键路径具有确定性断言
- **AND** 不需要真实模型凭据或外部服务

## Requirement: Maven 聚合与坐标

Boot 2 / Java 8 线必须以 `com.xiongx9527.fw.plus:fw-plus-ai-langchain4j-agentic:${revision}` 提供回移能力。模块必须加入 `fw-plus-ai-langchain4j-parent` 聚合，并由 Core BOM 管理。LangChain4j `0.35.0` 没有官方 Agentic artifact，因此该模块是新增 Agentic 能力，而不是替换一个不存在的 `0.35.0` JAR。

`fw-plus-ai-langchain4j-parent` 必须是唯一的 LangChain4j Java 8 parent/aggregator POM，不作为运行时依赖进入 BOM。五个目标坐标的 groupId 均为 `com.xiongx9527.fw.plus`、version 均为 `${revision}`，矩阵如下：

| artifactId | packaging | 物理路径 | 父关系/职责 |
| --- | --- | --- | --- |
| `fw-plus-ai-langchain4j-parent` | `pom` | `fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j-parent/pom.xml` | 继承 `fw-plus-ai:${revision}`；唯一 parent/aggregator |
| `fw-plus-ai-langchain4j` | `jar` | `fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/pom.xml` | 继承新 parent；官方 aggregate owner |
| `fw-plus-ai-langchain4j-core` | `jar` | `fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-core/pom.xml` | 继承新 parent；官方 Core owner |
| `fw-plus-ai-langchain4j-spring-boot-starter` | `jar` | `fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-spring-boot-starter/pom.xml` | 继承新 parent；仅承载 10 个 Spring 覆盖类 |
| `fw-plus-ai-langchain4j-agentic` | `jar` | `fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-agentic/pom.xml` | 继承新 parent；完整 Agentic Java 8 回移 |

Reactor 拓扑必须唯一：`fw-plus-ai/pom.xml` 用 `<module>fw-plus-ai-langchain4j-parent</module>` 替换旧 `<module>fw-plus-ai-langchain4j</module>`，不得同时聚合两者；aggregate JAR POM 删除 `<packaging>pom</packaging>` 与全部 `<modules>`。新 parent 到 `fw-plus-ai` 的 relativePath 为 `../pom.xml`，aggregate 到新 parent 为 `../fw-plus-ai-langchain4j-parent/pom.xml`，19 个原先继承旧 aggregate 的子模块与新 starter 到新 parent 为 `../../fw-plus-ai-langchain4j-parent/pom.xml`；adapter 保持既有 `fw-plus-parent` 继承。

新 parent `<modules>` 恰有 22 项：`../fw-plus-ai-langchain4j`、旧 aggregate 的 20 个 module 逐项加前缀 `../fw-plus-ai-langchain4j/`、`../fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-spring-boot-starter`；每个物理路径只出现一次。独立 parent 构建与 Core AI 顶层聚合构建必须解析到同一套 reactor，且现有子模块不在本 change 重命名 artifactId。

所有本地 LangChain4j wrapper 对官方 provider/base artifact 的依赖必须排除传递的 `dev.langchain4j:langchain4j` 与 `dev.langchain4j:langchain4j-core`，并按源码直接使用情况显式依赖本地 aggregate/Core owner JAR。AI Boot 同样不得直接依赖官方 aggregate/Core，而应依赖本地 owner JAR与新 Spring starter。

### Scenario: 使用 Core BOM

- **WHEN** 使用方导入 Core BOM并声明 `fw-plus-ai-langchain4j-agentic`
- **THEN** 无需单独指定模块版本
- **AND** Maven 解析到 fw-plus Java 8 回移模块及其本地 aggregate/Core 依赖
- **AND** parent/aggregator POM 不出现在运行时依赖树

### Scenario: 构建新的 LangChain4j reactor

- **WHEN** 使用 JDK 8 运行 `(cd fw-plus-core && ./mvnw -f fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j-parent/pom.xml clean install)`，随后运行 `(cd fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-autoconfigure -am test)`
- **THEN** `fw-plus-ai/pom.xml` 只聚合新 parent，aggregate POM 不含 modules，新 parent 对 aggregate/20个原子模块/新 starter 的每个相对路径恰好聚合一次
- **AND** 所有现有 LangChain4j 子模块均通过新 parent 解析，adapter 的既有独立 parent 例外保持
- **AND** 独立 parent 命令与 Core AI 顶层聚合命令构建同一模块集合，reactor 中不存在重复 GAV
- **AND** aggregate/Core/Spring starter/Agentic 均生成主 JAR，parent 只生成 POM
- **AND** Core parent reactor 完成全部 unit/IT、产物门禁并安装本地 owner artifacts，AI Boot 模块随后完成条件配置与直接消费者测试
- **AND** 每个本地 LangChain4j wrapper 与 AI Boot 的生产依赖树只保留本地 aggregate/Core owner JAR，官方 provider/base artifact 不再传递官方 aggregate/Core

## Requirement: Spring 覆盖职责迁移

现有 `fw-plus-ai-langchain4j-core` 中的 10 个 `dev.langchain4j.spring`、`dev.langchain4j.service.spring` 与 `dev.langchain4j.rag.spring` 覆盖类必须完整迁入 `fw-plus-ai-langchain4j-spring-boot-starter`。Core JAR 只承载官方 `langchain4j-core` owner 的 Java 8 兼容 API/实现，新 starter 只承载 Spring Boot 覆盖职责。

### Scenario: 检查 JAR 内容和直接消费者

- **WHEN** 验证器检查 Core 与 Spring starter 的产物并启动 AI Boot 条件配置测试
- **THEN** 10 个 Spring 覆盖类只存在于新 starter JAR
- **AND** Core JAR 不包含 `dev/langchain4j/**/spring/**`
- **AND** AI Boot 直接消费者从新 starter 获取 Spring 类型并保持可加载
- **AND** 新 starter 不包含 Agentic、aggregate 或 Core 兼容源码的重复副本

## Requirement: Java 8 兼容覆盖说明

聚合 README 与模块来源说明必须随代码同步维护。文档必须反映上游基线、文件数、依赖闭包、Java 8 改写、许可义务、测试范围和已知差异，且不得继续声称“暂无待新增项”。

### Scenario: 核对文档与实现

- **WHEN** 新模块、依赖映射或复制文件清单发生变化
- **THEN** `fw-plus-ai-langchain4j/README.md` 与模块来源说明同步更新
- **AND** 文档中的 artifact、tag、commit、文件数和依赖映射与实际实现一致

## Requirement: 变更隔离

本 change 必须只修改 Agentic 回移、其必需依赖闭包、parent/aggregate/Core/Spring starter 角色拆分、本地 wrapper 与 AI Boot 的 owner 依赖边、聚合/BOM与直接消费者接入、测试和兼容说明。不得修改并行建设中的 RAG、MCP Server、MCP Client、Agent Platform、Rule 或其他不相关能力。

本 change 不重命名现有 provider artifact，不处理 provider 与官方完整 starter artifactId 的历史命名差异。完成并归档 `fw-plus-ai-langchain4j-agentic` 后必须停止，不自动启动任何后续模块或 change。

### Scenario: 审查 change diff

- **WHEN** 验证器检查最终 diff
- **THEN** 所有实现改动均可追溯到 Agentic 回移、必要 owner 兼容层、Maven角色拆分或直接消费者接入
- **AND** 不包含 provider artifact 重命名、其他会话的未提交修改或其他非目标实现
