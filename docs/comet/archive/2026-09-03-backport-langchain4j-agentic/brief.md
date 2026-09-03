# Outcome

在 Spring Boot 2 / Java 8 的 `dev` 技术线上，为 LangChain4j `0.35.0` 提供一个完整的 `langchain4j-agentic` Java 8 回移模块。该模块以上游最新发布 `1.19.0-beta29` 为冻结基线，保持上游包名、类名和公开使用方式，纳入 `fw-plus-ai-langchain4j-parent` reactor，并通过本地 aggregate/Core owner JAR 获得 Java 8 兼容依赖。

# Scope

- 新增 Maven 模块 `fw-plus-ai-langchain4j-agentic`，模块名称遵循 `fw-plus-ai-` + 官方 artifactId 规则。
- 以上游 LangChain4j tag `1.19.0`、commit `09a2aa3861ba468af3507c28f5676048cd5db4c6`、artifact `dev.langchain4j:langchain4j-agentic:1.19.0-beta29` 为本次固定来源。
- 完整纳入上游 `langchain4j-agentic/src/main/java` 的 153 个生产 Java 源文件，并保留 `dev.langchain4j.agentic.*` 包名、公开类名和嵌套类型名。
- 将上游 Java 17 语法和 Java 9+ JDK API 改写为 Java 8 等价实现。
- 处理 Agentic `1.19.0-beta29` 与 LangChain4j `0.35.0` 在 Model、Tool、AiServices、Invocation、Guardrail、Observability、JSON codec、并发执行等方面的依赖差异；公开能力所需但 `0.35.0` 不存在的依赖能力必须形成明确兼容实现，并由所属官方 artifact 对应的 fw-plus 模块承载。
- 新建 `fw-plus-ai-langchain4j-parent`，独立承担现有 LangChain4j Java 8 模块的 Maven parent 与 aggregator 职责；现有子模块改用该 parent，物理目录可保持不动。
- 将 `fw-plus-ai-langchain4j` 从聚合 POM 改为官方 `dev.langchain4j:langchain4j` 对应的真实 JAR，承载 aggregate-owned Java 8 兼容 API/实现，并依赖项目既有 LangChain4j `0.35.0` 基线。
- 将 `fw-plus-ai-langchain4j-core` 收敛为官方 `dev.langchain4j:langchain4j-core` 对应的 Core 兼容 JAR；原有 10 个 `dev.langchain4j.*.spring` 覆盖类迁入新增的 `fw-plus-ai-langchain4j-spring-boot-starter`。
- 允许本地 aggregate/Core JAR 对官方 `0.35.0` 中 ABI 不足的同 FQCN 类型进行受控 overlay。overlay 必须采用打包期确定性合并：本地 owner JAR 合并对应官方 `0.35.0` JAR 的非替换 class，排除 allowlist 中由本地源码提供的 class；发布 POM 与本地包装模块排除官方同 owner JAR，使运行时只存在一份 owner class。
- overlay allowlist 固定为 UTF-8 TSV `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/compat/overlay-allowlist.tsv`。空行忽略，只有行首 `#` 表示注释；其他行必须严格为 5 个 tab 分隔列：`<binary-fqcn>\t<owner-gav>\t<reason>\t<api-test>\t<behavior-test>`。owner GAV 只允许 `dev.langchain4j:langchain4j:${langchain4j.version}` 或 `dev.langchain4j:langchain4j-core:${langchain4j.version}`；reason 非空；测试锚点语法固定为 `<fully-qualified-test-class>#<method>`。
- `OverlayAllowlistContractTest` 固定路径为 `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-agentic/src/test/java/dev/langchain4j/agentic/compat/OverlayAllowlistContractTest.java`，在 Surefire `test` 阶段使用严格 UTF-8 解析器拒绝 BOM、重复 FQCN、非法注释/列数/空字段/owner/锚点；反射确认两个锚点方法存在并带 `@Test`，解析 aggregate/Core POM XML并双向核对 Shade excludes。Failsafe IT再读取 Surefire XML报告，证明每个锚点实际执行且通过。
- Maven `verify` 必须生成最终生产依赖 JAR 清单，按 class entry 聚合来源；未登记打包替换、最终依赖树任意重复 `dev.langchain4j.*` class、allowlist 测试锚点未执行通过或本地替换 class 未从官方合并输入排除时失败，并输出 class FQCN、全部来源 JAR和 allowlist状态。
- 更新 AI Boot 自动配置等直接消费者，使 Spring 覆盖类从新 starter 获取；AI Boot 不再直接依赖官方 `langchain4j`/`langchain4j-core`，改用本地 aggregate/Core owner JAR。
- 所有本地 LangChain4j wrapper 对官方 provider/base artifact 的依赖必须排除传递的 `dev.langchain4j:langchain4j` 与 `dev.langchain4j:langchain4j-core`，并按源码直接使用情况显式依赖本地 owner JAR；provider artifactId 与源码职责保持不变，本 change 不扩展到 provider 命名清理。
- 目标坐标与路径固定如下，五个坐标的 groupId 均为 `com.xiongx9527.fw.plus`、version 均为 `${revision}`：

  | artifactId | packaging | 物理路径 | 版本/父关系 |
  | --- | --- | --- | --- |
  | `fw-plus-ai-langchain4j-parent` | `pom` | `fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j-parent/pom.xml` | 继承 `fw-plus-ai:${revision}`，聚合 aggregate JAR、现有 20 个子模块与新 Spring starter |
  | `fw-plus-ai-langchain4j` | `jar` | `fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/pom.xml` | 继承新 parent，官方 aggregate owner |
  | `fw-plus-ai-langchain4j-core` | `jar` | `fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-core/pom.xml` | 继承新 parent，官方 Core owner |
  | `fw-plus-ai-langchain4j-spring-boot-starter` | `jar` | `fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-spring-boot-starter/pom.xml` | 继承新 parent，只承载 10 个 Spring 覆盖类 |
  | `fw-plus-ai-langchain4j-agentic` | `jar` | `fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-agentic/pom.xml` | 继承新 parent，依赖本地 aggregate/Core |

- Reactor 拓扑固定为：`fw-plus-ai/pom.xml` 用 `<module>fw-plus-ai-langchain4j-parent</module>` **替换**旧 `<module>fw-plus-ai-langchain4j</module>`；aggregate JAR POM 删除 `<packaging>pom</packaging>` 与全部 `<modules>`。新 parent 的 relativePath 到 `fw-plus-ai` 为 `../pom.xml`；aggregate 到新 parent 为 `../fw-plus-ai-langchain4j-parent/pom.xml`；19 个原先继承旧 aggregate 的子模块和新 starter 到新 parent 为 `../../fw-plus-ai-langchain4j-parent/pom.xml`；adapter 保持既有 `fw-plus-parent` 继承。
- 新 parent 的 `<modules>` 恰有 22 项：`../fw-plus-ai-langchain4j`、旧 aggregate 的 20 个 module 逐项加前缀 `../fw-plus-ai-langchain4j/`、`../fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-spring-boot-starter`；每个物理路径只出现一次，adapter 虽保持独立 parent 仍由新 parent 聚合。
- 本地 aggregate/Core 使用 `maven-shade-plugin:3.6.1` 在 `package` 阶段合并精确官方 owner。官方 owner 是 direct optional compile dependency：aggregate 只声明 `dev.langchain4j:langchain4j:${langchain4j.version}`，Core 只声明 `dev.langchain4j:langchain4j-core:${langchain4j.version}`。两个 Shade execution 的 `artifactSet.includes` 分别只列对应官方 owner；Shade `ArtifactSelector` 自动保留当前项目 main artifact，其余 project dependency 全部不选中；`createDependencyReducedPom=false`。
- aggregate 对官方 `langchain4j` 的 optional compile 依赖显式排除官方 Core，并以普通 compile 依赖引用本地 `fw-plus-ai-langchain4j-core`；由于本地 Core 不在 aggregate 的 `artifactSet.includes` 中，不能进入 aggregate 成品。POM 的 class-entry excludes 是 allowlist 的静态执行映射，`OverlayAllowlistContractTest` 在 Agentic `test` 阶段双向校验 allowlist 与两个 owner POM excludes 无遗漏或多余条目；Shade 合并 SPI service descriptors并移除无效签名元数据。
- Agentic 使用 `maven-dependency-plugin:build-classpath` 在 `pre-integration-test` 以 `includeScope=runtime` 生成 `target/agentic-runtime-classpath.txt`。`maven-failsafe-plugin` 必须显式配置两个 execution：`failsafe:integration-test` 绑定 `integration-test`，`failsafe:verify` 绑定 `verify`；IT 源码固定为 `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-agentic/src/test/java/dev/langchain4j/agentic/compat/AgenticOverlayPackagingIT.java`。
- Agentic POM 基于已有 `${main.basedir}` 定义并注入 Failsafe 的精确路径属性：`agentic.aggregate.jar=${main.basedir}/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/target/fw-plus-ai-langchain4j-${revision}.jar`、`agentic.core.jar=${main.basedir}/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-core/target/fw-plus-ai-langchain4j-core-${revision}.jar`、对应两个目录的 `target/classes`、`agentic.runtime.classpath=${project.build.directory}/agentic-runtime-classpath.txt`、`agentic.surefire.reports=${project.build.directory}/surefire-reports`；Agentic 自身成品使用 `${project.build.directory}/${project.build.finalName}.jar`。
- IT 以三个 packaged JAR 和 runtime依赖JAR作为生产来源，仅以 aggregate/Core `target/classes` 比较 allowlist 替换字节；断言 aggregate 成品不包含任何仅属于本地 Core 的 class，最终 Core class 只来自本地 Core JAR。
- Agentic 必须保留并验证固定 SPI 三元组：接口 `dev.langchain4j.service.ParameterNameResolver`；descriptor `META-INF/services/dev.langchain4j.service.ParameterNameResolver`；provider `dev.langchain4j.agentic.internal.AgenticParameterNameResolver`。IT 从 Agentic packaged JAR读取 descriptor，并使用只含最终 runtime classpath 的隔离 `URLClassLoader` 验证 `ServiceLoader` 精确发现该 provider。
- 迁移与 Java 8 回移目标有关、且不依赖真实外部模型的上游测试；增加固定假模型、工具、并发、暂停恢复、Scope、Planner、Workflow、Supervisor 与公开接口兼容测试。
- 同步维护 `fw-plus-ai-langchain4j/README.md` Java 8 兼容覆盖说明，并为新模块记录上游来源、许可、复制清单、依赖映射和本地修改。
- 将模块纳入 Core AI 聚合和 BOM 依赖管理，使上层可显式依赖该模块。

# Non-goals

- 本次不实现 fw-plus 统一 Agent Platform、Agent Runtime Adapter、Rule/Policy、Skill、Memory 或 Harness。
- 本次不实现 `fw-plus-ai-mcp` Client；该能力已作为低优先级独立待办记录。
- 本次不修改 RAG 或 Magic-API MCP Server 的并行建设成果。
- 本次不在 `springboot3` 分支实现同坐标桥接模块；Boot 3 最终使用相同 fw-plus 坐标传递官方 Agentic，但应由后续独立变更完成。
- 本次不重命名现有 LangChain4j provider artifact，不处理 provider 坐标与官方完整 starter artifactId 的历史差异；只完成 Agentic 所必需的 parent/aggregate/Core/Spring starter 角色拆分。
- 本次不继续实现或启动 Agentic 之外的后续模块/change；`fw-plus-ai-langchain4j-agentic` 完成并归档后立即停止。
- 本次不承诺与 Java 17 上游 JAR 的字节码二进制兼容，也不承诺跨 JVM 的序列化对象可直接互换。

# Acceptance examples

- A1：在 Java 8 编译配置下构建 `fw-plus-ai-langchain4j-agentic`，153 个上游生产源文件均有对应 Java 8 源文件并成功编译，不包含 `record`、模式匹配、switch expression、text block、`var` 或 Java 9+ 集合/Stream/Optional/String API。
- A2：回移模块中的生产类型保持上游 `dev.langchain4j.agentic.*` FQCN；自动生成的公开类型清单与上游基线相比没有缺失或擅自重命名的公开类、接口、枚举、注解和嵌套类型。上游 `record` 是唯一允许改变的类型种类：Java 8 中必须表示为 `final class`，并保持 canonical constructor、component accessor、泛型/注解公开面、`equals`、`hashCode` 与 `toString` 契约。
- A3：上游公开示例覆盖的 Agent、AgenticScope、Sequence、Parallel、Loop、Conditional、Supervisor、Planner、Human-in-the-loop、异步/流式执行和 Observability 能力，在 Java 8 兼容测试中可创建并运行，不以统一空实现或 `UnsupportedOperationException` 代替。
- A4：固定假模型和工具测试证明 Tool 调用、Agent 输出、Scope 状态、异常传播、取消/失败、并行兄弟失败、暂停与恢复等关键行为符合回移基线预期；不要求真实模型凭据。
- A5：`fw-plus-ai-langchain4j-parent` 是唯一 parent/aggregator；`fw-plus-ai-langchain4j` 与 `fw-plus-ai-langchain4j-core` 均产出 Java 8 JAR，Agentic 通过这两个本地 owner 模块获得兼容依赖，不解析到 LangChain4j 1.x 或 Java 17 字节码构件。
- A6：原 `fw-plus-ai-langchain4j-core` 的 10 个 Spring 覆盖类完整迁入 `fw-plus-ai-langchain4j-spring-boot-starter`，AI Boot 直接消费者切换到新 starter；Core JAR 不再包含 `dev.langchain4j.*.spring` 类，Spring starter 不承载 Agentic/Core/aggregate API。
- A7：兼容说明明确记录 `0.35.0` 不存在官方 Agentic artifact、本地模块基于 `1.19.0-beta29` 完整回移、上游 tag/commit、153 文件清单、依赖替换、Java 8 修改、受控 overlay allowlist 和已知行为差异；模块包含 Apache 2.0 来源及修改声明所需资料。
- A8：Core AI LangChain4j parent reactor、aggregate/Core/Spring starter 和 Agentic 模块的 Java 8 构建与测试通过；构建检查验证全部本地产物主版本不高于 52，Agentic JAR 无重复或错误来源的 `dev.langchain4j.agentic.*` 类，最终生产 classpath 无任何重复 `dev.langchain4j.*` class。`AgenticOverlayPackagingIT` 在 Maven `verify` 阶段验证 allowlist 格式/测试锚点、打包替换来源、成品字节、本地源码优先和失败诊断。
- A9：所有本地 LangChain4j wrapper 与 AI Boot 的生产依赖树只保留本地 aggregate/Core owner JAR；官方 provider/base artifacts 不再传递官方 `langchain4j`/`langchain4j-core`，provider artifactId 不变。
- A10：本次 diff 不修改 Agent Platform、MCP Client、RAG 或其他会话正在建设的实现文件；除 Agentic 必需的 parent/aggregate/Core/Spring starter 拆分、wrapper/AI Boot 依赖边、聚合/BOM/文档接入外，不重命名 provider artifact，不包含其他会话的未提交修改。

# Constraints and invariants

- Core 技术基线保持 Java 8、Spring Boot 2.7.18、LangChain4j `0.35.0`。
- 上游冻结基线固定为 tag `1.19.0` / commit `09a2aa3861ba468af3507c28f5676048cd5db4c6` / Agentic `1.19.0-beta29`；本次实现期间不追踪后续发布。
- 包名、公开类名和嵌套类型名与上游一致；不得新增 `compat`、`jdk8` 或 fw-plus 专属后缀替代上游类型。
- 新增回移 artifact 均按 `fw-plus-ai-` + 官方 artifactId 命名；Maven-only parent 例外使用 `fw-plus-ai-langchain4j-parent`，Spring 覆盖模块使用官方 `langchain4j-spring-boot-starter` 对应的 `fw-plus-ai-langchain4j-spring-boot-starter`。
- `fw-plus-ai-langchain4j-parent` 只作为 parent/aggregator，不进入运行时 BOM 依赖；`fw-plus-ai-langchain4j`、`fw-plus-ai-langchain4j-core` 和 `fw-plus-ai-langchain4j-agentic` 为普通 JAR 坐标。
- 允许改写私有实现与替换底层依赖，但不得静默删除公开能力；无法等价实现时必须在 Build 中暴露为明确阻塞，而不是降低验收标准。
- 0.35 同 FQCN overlay 必须最小化并登记在 `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/compat/overlay-allowlist.tsv`；official owner optional compile 依赖、精确 Shade `artifactSet.includes`、静态 class-entry excludes、`OverlayAllowlistContractTest` 与 `AgenticOverlayPackagingIT` 共同形成确定性门禁，最终生产 classpath 不允许保留任何重复 `dev.langchain4j.*` class。
- Apache License 2.0 许可和修改声明必须随回移来源记录维护。
- 现有其他会话的未提交改动不得进入本 change；实现只在独立 worktree 中进行。

# Decisions

- 代码复制优先级高于 Agent Platform、Rule 与 MCP Client 的进一步规划。
- 采用最新已核实发布版本，而不是 Agentic 首版或较早稳定点。
- 完整复制上游生产源码，不只抽取 ReAct 或部分 Workflow 能力。
- Java 包名和类名保持上游一致。
- Boot 2 / Boot 3 最终向用户提供相同坐标 `com.xiongx9527.fw.plus:fw-plus-ai-langchain4j-agentic`；本 change 只实现 Boot 2 / Java 8 内容。
- `0.35.0` 没有官方 `langchain4j-agentic` artifact；本地模块是新增能力，不描述为替换一个不存在的 `0.35.0` JAR。
- 为解决 `fw-plus-ai-langchain4j` 聚合 POM 与官方 aggregate owner 运行时 JAR 的 GAV 冲突，完整拆分 LangChain4j 聚合器：新建 `fw-plus-ai-langchain4j-parent`，释放 `fw-plus-ai-langchain4j` 为 JAR，重建纯 Core owner JAR，并将 Spring 覆盖类迁入独立 starter。
- 本次只做 Agentic 所需的角色拆分，不重命名现有 provider artifact；provider 命名历史差异留给后续独立需求。
- 保持单一 Native change，不拆 Supervisor/child：Agentic 依赖闭包和模块角色迁移共享同一组 POM/API，拆分后冲突与集成成本高于并行收益。
- Agentic change 完成并归档后立即停止，不自动启动后续模块。
- MCP Client 选择完整协议范围，但优先级较低，另行规划。

# Open questions

无。

# Verification expectations

- 使用 JDK 8 或 Maven `--release 8` 等价工具链完成编译验证，并检查生成 class major version 为 52。
- 运行 Agentic 回移模块的全部本地单元与契约测试。
- 使用 JDK 8 运行 `(cd fw-plus-core && ./mvnw -f fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j-parent/pom.xml clean install)`，执行 owner/Agentic 全部 unit + IT、验证 aggregate/Core/Spring starter/Agentic 产物 packaging/JAR 内容并安装本地 artifacts；随后运行 `(cd fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-autoconfigure -am test)`，验证 AI Boot 直接消费者、条件配置和 owner 依赖边。
- 验证所有现有 LangChain4j 子模块均解析到新 parent，reactor 中不存在重复 GAV；`fw-plus-ai-langchain4j-parent` 不进入运行时依赖树。
- 对每个本地 LangChain4j wrapper 与 AI Boot 解析生产依赖树，验证官方 provider/base artifacts 已排除传递的官方 aggregate/Core，且所需 API 来自本地 owner JAR。
- 对比上游与回移版生产文件清单、FQCN 清单和公开类型清单。
- 扫描 Java 17 语法和 Java 9+ API 残留。
- 检查生产依赖树不存在 Java 17 字节码依赖及未计划的现代 LangChain4j 构件。
- 核对受控 overlay allowlist、重复 class 扫描、类加载来源和 ABI/行为测试；任何未登记重复类均失败。
- 核对旧 core 的 10 个 Spring 覆盖类只存在于新 Spring starter，AI Boot 条件类仍可加载。
- 核对 README/UPSTREAM 来源说明与实际模块、文件和依赖映射一致。
