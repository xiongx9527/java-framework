# 项目长期备忘 — java-framework3 (fw-plus)

## LangChain4j 层 owner 结构
- `fw-plus-ai-langchain4j` 是**唯一** parent/aggregator（`packaging=pom`，聚合 21 个子模块），**不产出 JAR**。历史上的 `fw-plus-ai-langchain4j-parent` 已删除，不要再引入。
- `fw-plus-ai-langchain4j-core` 是**唯一 owner JAR**：同时承载上游 `dev.langchain4j:langchain4j`（aggregate）与 `dev.langchain4j:langchain4j-core` 两组 owner 的 shade 内容，并用本地源码覆盖同 FQCN 的 Java 8 兼容类。
- overlay 替换清单唯一来源：`fw-plus-ai-langchain4j/compat/overlay-allowlist.tsv`，core 的 pom 里对应两个 shade `<filter>`，两边必须严格一致；由 `Aggregate/Core/OverlayAllowlistContractTest` 与 `*PackagingIT` 校验，丢了会红 4 个单测。
- 子模块 parent 一律 `fw-plus-ai-langchain4j`，不写 `<relativePath>`；**唯一例外** `fw-plus-ai-langchain4j-adapter` 继承 `fw-plus-parent`，必须写 `../../../fw-plus-parent`。
- 要 owner 依赖时声明 `-core`，不要声明 aggregate。

## POM 书写约定（2026-09-19 全仓精简后：160 POM，12294 → 11261，-1033 行）
四类内容已清零（残留复查 `grep -rl` 均应为 0），**新写 POM 不要再引入**：
1. `<main.basedir>` 属性 —— 死属性、零引用。要父目录用 `${project.basedir}/..` 或 `${project.parent.basedir}`（后者仅限普通元素，见下）。
2. `<scope>compile</scope>` —— Maven 默认值。删前须确认该 GA 未被父链 dependencyManagement 钉成 provided/test（本仓有 14 条非 compile 条目）；用 `dependency:list -DincludeScope=test` 前后 diff 判定，不要静态猜。
3. `<relativePath>../pom.xml</relativePath>` —— 默认值。非默认路径（adapter、boot 系列 `../fw-plus-boot-parent`）必须保留。
4. 被 BOM 管理的依赖 `<version>` —— `dev.langchain4j:*` 由 `langchain4j-bom`、`com.xiongx9527.fw.plus:*` 由 `fw-plus-dependencies` 管理。**例外：`<type>test-jar</type>` 依赖必须写版本**。
- `fw-plus-ai-langchain4j-infinispan` 的 `infinispan-bom` import **必须保留**（钉住传递依赖 `infinispan-commons` 13.0.22，删掉会掉到 13.0.20，已实测）；本地重复的 `<infinispan.version>` 属性已删。
- BOM 中引用聚合器坐标必须显式 `<type>pom</type>`。

## agentic 模块 POM（已极简，90 行）
只剩 `dependencies` + `dependency-plugin(build-classpath)` + `failsafe`；「可验证性基础设施」已搬进测试代码：
- `agentic.*` 属性与 surefire `systemPropertyVariables` 全部删除，**不要再加**。文件位置由 `src/test/java/dev/langchain4j/agentic/AgenticTestPaths.java` 从 `user.dir`/`user.home` 推导，`resolve(name)` 保留原属性名作 key。
- `dependency-plugin` `build-classpath`（pre-integration-test）**保留**（必须靠 Maven 解析依赖才能得到）；**antrun 已删除**，IT 侧 `AgenticOverlayPackagingIT.runtimeJars` 就地把本地仓库 jar 换成 worktree 产物。故 `clean` 后 core 必须先 `package`/`install`，否则 IT 报 `runtime classpath entry is not a file`。
- failsafe **只写一个 `<execution><id>default</id>` + goals，不写 `<phase>`**：靠官方「同 id 覆盖」语义顶掉 `spring-boot-starter-parent` 注入的 default execution。**不要用 `combine.self="override"` + 三个 execution**，否则 IT 跑两遍。形态由 `OverlayAllowlistContractTest#failsafeLifecycleOverridesInheritedDefaultExecutionBySameId` 锁定。
- `<configuration>` 一律放 **plugin 级**（Maven 3.3.1+ 起对所有 execution 生效）。
- core 的 `pluginManagement > shade > <executions combine.self="override"/>` 已删（effective-pom 逐字节比对证明纯冗余）。真正起作用的是本模块 `<id>default</id><phase>none</phase>` + `artifactSet include __disabled__` 的空 execution —— 按同 id 顶掉 Spring Boot 专用 transformers。
- **`combine.self="override"` 放本模块 `<executions>` 上无效**：继承来的 execution 会以 package 阶段复活并把全部依赖打进产物。
- **插件 `<configuration>` 里 `${project.parent.basedir}` 不被解析**（字面量落盘）；`${project.basedir}` / `${project.build.directory}` / `${settings.*}` / `${revision}` 正常。
- 判断插件块是否冗余：**只能 `help:effective-pom` 前后 diff**，读源码猜会得出相反结论。

## 回移基线与 record→Java 8 铁律
- 冻结基线 `dev.langchain4j:langchain4j-agentic:1.20.0-beta30`，来源 tag `1.20.0` / commit `3a2f4dca6fb447e4d191624b3d588952ed9f4ce9`（无同名 beta30 tag）；上游 152 个生产文件，92 字节一致 / 60 修改 / 3 本地 helper。
- **升级上游必须同步**：`UPSTREAM-MANIFEST.txt`、`JAVA8-MODIFIED-MANIFEST.txt`、`AgenticTestPaths.UPSTREAM_VERSION`、`AgenticPublicApiParityTest`（jar 名 + 两个计数断言）、`AgenticOverlayPackagingIT`（清单计数）、`UPSTREAM.md`、聚合器 `README.md`、`META-INF/NOTICE`。
- **record 组件 `@Json*` 注解要手工复刻分布**：javac 传播到「字段 + 规范构造器参数 + 隐式访问器」，**显式覆写的访问器不传播**（如 `AgentInvocation.output()`），否则 parity 的 ASM 注解集合比对失败。
- **不要给 Java 8 等价类加 `@JsonCreator`**：上游 record 靠「唯一非默认构造器 + 参数 `@JsonProperty`」被 Jackson 隐式识别，加了就与上游注解集合不等（JSON 行为不变）。例外：上游本就在类构造器写了 `@JsonCreator` 的（`DefaultAgenticScope(Object, Kind)`）保留。
- **`AgenticRecordComponentMetadata` 是生产代码**：任一注册类型的组件增删，必须同改生产源码 `component(...)` 列表与 `Task2a3MetadataContractTest.EXPECTED`，否则运行期 `validate()` 抛 drift。
- **`DefaultAgenticScopeJsonCodec` 必须保持 `@Internal class`**（包私有、非 final）；final 会让 parity 的 class access 比对失败。
- 孤立 harness（`Task2a1/2/3RuntimeContractTest`）编译**继承 surefire 的 `java.class.path`**，故生产源码新增 Jackson 注解不会让它失败——缺 import 才会（合成包装类按大括号截取 region，import 不在其中，要手动补）。

## 构建与既有失败
- **必须 JDK 8**：`export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home`。JAVA_HOME 为空时 `/usr/libexec/java_home` 给 zulu-21，导致 3 个 agentic runtime harness 假失败。
- 本机 Maven：`/Users/xiongxiao/Workspaces/Maven/apache-maven-3.6.3/bin/mvn`；langchain4j reactor 22 模块（21 子 + 1 聚合器）。
- 该 reactor **不能只跑 `test-compile`**：core 的官方 langchain4j 依赖是 `<optional>true</optional>`，官方类只存在于 shade 后的 JAR；未打包时用 `target/classes` 会报找不到 `dev.langchain4j.model.chat.*`。必须 `install`（至少 `package`）。
- 分层验证顺序（`/tmp/verify_all.sh`）：Core(`-Dmaven.test.skip=true install`) → langchain4j(`install` 含测试) → Boot/Cloud/AI/Sample(`-DskipTests`)。**当前 Core / langchain4j / Cloud / AI 四层 SUCCESS**（langchain4j 22 模块，agentic 70 unit + 8 IT 全绿）。
- **2 处既有失败**（已用 `git checkout HEAD --` 对应 POM 复现，与精简无关）：
  1. `fw-plus-boot-autoconfigure` 主源码 `magicapi/plugin/s3/file/MyFileStorageService.java:391`：hutool `HttpResponse#getFileNameFromDisposition()` 现需 String 参数，源码仍按无参调用 → Boot 构建失败，**Sample 层随之级联失败**。
  2. `fw-plus-ai-tinyflow-dify` 测试编译：测试用 JUnit4 `org.junit.Test` 但 pom 未引 junit，报 `找不到符号: 类 Test` → Core 层全量验证只能带 `-Dmaven.test.skip=true`。
- **Boot 既有失败会阻塞 tinyflow 解耦 change**：其 Task 5.3 的边界测试与 6.2 的 `fw-plus-ai` verify 都依赖 `fw-plus-ai-boot-autoconfigure` 能编译，所以修 `MyFileStorageService` 是该 change 收尾的前置条件。
- zsh 下 `grep "a\|b"` 假阴性，必须 `grep -E`。

## 提交边界（2026-09-20 已整理完毕）
堆积的三批改动已按边界切成 6 个提交落到 `dev`（未 push）：`b26cfe23` comet 工具链 → `930cbd69` langchain4j owner 重构（含该层 22 个 pom）→ `6a7358b8` beta30 回移 → `37a485dd` tinyflow 5.2 删除 → `3c22982f` 其余 147 个 pom 精简 → `2b6f3b0d` openspec 产物。
- **中间提交不保证各自可独立构建**（langchain4j 层的 pom 结构性改动与精简 delta 无法按 hunk 拆开，整文件归到 owner 重构那个提交），只有 HEAD 是完整状态。
- 工作区现只剩 `.claude/`、`.workbuddy/` 两个未跟踪目录（未加进 .gitignore）。

## 归档文档
`docs/comet/archive/**` 是历史 change 记录（含旧 `-parent` 拓扑描述），不要回改。
