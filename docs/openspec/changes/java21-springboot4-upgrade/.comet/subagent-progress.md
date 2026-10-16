# Classic Coordination Checkpoint

Generated from coordination.json; tasks.md remains the completion authority.

Stage: implementing
Tasks: c6a5871d-a930-4de5-aec9-69e72904e095, d0b9710c-a1e0-41d4-8d5d-0476db7c385e, 5516b38f-c873-469f-8f9f-c4479b80d01f, 657fbdf1-8150-4a73-91f0-727b6254f5c0, 81b080a8-966c-4096-9c5e-512a33babaa8
Revision: b75a22c135cca76dbb892850068725a4f70447476ff4879a7f26012f5b198004
Session: ses_ebcbcfd0bffeYfZFtotdGKUeEk
Review rounds: 1

## Evidence
- plan: docs/superpowers/plans/2026-10-16-java21-springboot4-upgrade.md; base-ref 64a27a973763d8ed202b47dbfb671f3210063208
- task 1.1 done: fw-plus-core/pom.xml jdk.version=21; fw-plus-auth-core 移除 maven.compiler.source/target=1.8 与插件内 source/target
- task 1.1 GREEN: JAVA_HOME=zulu-21 ./mvnw clean install -DskipTests in fw-plus-core -&gt; BUILD SUCCESS (32.4s); log /private/var/folders/p7/cg70fqfj1xz7rwplv4n19d_w0000gp/T/opencode/core-build-1.log
- task 1.1 声明型任务无自然 RED（JDK21 下 source/target 8 仍可编译），如实记录
- task 1.2 done: fw-plus-dependencies BOM 升级（Boot 4.1.1 / Cloud 2025.1.3 / Alibaba 2025.1.0.0 / Spring AI 2.0.1 / sa-alibaba+extensions 2.0.0-M1.1 / MP 3.5.17 boot4-starter / MF 1.11.8 / mybatis-spring 4.0.0 / join 1.5.9 / dynamic-datasource 4.6.0 boot4 / druid-spring-boot-4-starter / knife4j fork com.baizhukui 5.7.8 / springdoc 3.0.3 / easy-trans org.dromara 4.0.2 / langchain4j-community 1.22.0-beta32 / tomcat 钉 11.0.27）；easy-trans 两个 Core 模块坐标同步切 org.dromara；Tencent 行留待 3.2
- task 1.2 RED: help:effective-pom 报 easy-trans-anno/service version missing（ProjectBuildingException，BOM 换坐标后消费点未同步）
- task 1.2 GREEN: help:effective-pom BUILD SUCCESS，全部目标版本生效（含 tomcat 11.0.27 五坐标、无旧坐标残留）；输出 /private/var/folders/p7/cg70fqfj1xz7rwplv4n19d_w0000gp/T/opencode/core-bom-effective.xml
- task 1.2 GREEN: cd fw-plus-core &amp;&amp; ./mvnw dependency:resolve -&gt; BUILD SUCCESS (43s); log /private/var/folders/p7/cg70fqfj1xz7rwplv4n19d_w0000gp/T/opencode/core-dependency-resolve.log
- task 1.2 记录: Boot 4.1.1 管理的 Tomcat 11.0.24 受 CVE-2026-76183 影响（affected &lt;= 11.0.25，fix &gt;= 11.0.26），已钉 11.0.27
- task 1.3 complete（comet state task-complete 5516b38f，进度 3/26）。RED-1: Framework 7 HttpHeaders 不再实现 Map，WebFluxRequestContextHolder:204 与 magicapi RequestHandler:497 的 keySet()-&gt;headerNames()（log core-build-red-1.3.log）
- task 1.3 RED-2: mp-join 仓内 fork MPJSqlInjector 引用 join 1.5.9 已移除的 VersionUtils/AbstractMethodV3431；处置=删死 import，getJoinMethod 简化为无条件 SqlMethod 构造（保留仓库自有 methodFilter 移除清单与 MpSqlInjector 基类）对照官方 1.5.9 源码（log core-build-1.3-iter2.log）
- task 1.3 RED-3: easytrans-service 编译失败——easy-trans 4.0.2（org.dromara 坐标）将包从 com.fhs.* 整体迁至 org.dromara.*；处置=全仓 27 文件 com.fhs.-&gt;org.dromara. 机械迁移（Core easytrans 17 + easypoi TransUtils 1 + Boot MateEasyTransAutoConfiguration 1 + mate 样例 5 等）；删除 annotation 模块内 com.fhs 复刻类（Trans/TransType，与上游 FQCN 漂移且 Trans 缺 pkeyClassType 有 NoSuchMethodError 风险）；新增 fw 超集接口 com.xiongx9527.fw.plus.easytrans.annotation.TransType extends 上游 TransType 补 BOOLEAN；annotation pom 增 easy-trans-anno 直接依赖（log core-build-1.3-iter3.log）
- task 1.3 GREEN: cd fw-plus-core &amp;&amp; ./mvnw clean install -DskipTests -&gt; BUILD SUCCESS (37.98s)（log core-build-1.3-green.log）
- BOM 兜底复核（用户确认其余包可升级；先修编译基线再升级、重建回归）：修复 jakarta.servlet-api 重复声明（有效值 6.0.0 -&gt; 6.1.0，与 Boot 4.1.1/Tomcat 11 对齐）；31 项属性升级（commons-*、hutool、fastjson/2、guava 33.7.2-jre、byte-buddy 1.18.11、assertj 3.27.7、json-path 2.10.0、protobuf-java 4.35.1、logstash-logback-encoder 9.0（Jackson 3 线）、kryo 5.7.1、bcprov-jdk18on 1.86、minio 8.6.0 等，明细分录进升级说明）；fw-plus-common bcprov-jdk15on -&gt; bcprov-jdk18on
- BOM 兜底复核 GREEN: clean install -DskipTests -&gt; BUILD SUCCESS (52.9s)（log core-build-1.3-green2.log）；关键解析核对 dependency:tree（servlet-api 6.1.0 / guava 33.7.2-jre / bcprov-jdk18on 1.86 / byte-buddy 1.18.11 / commons-lang3 3.20.0，log core-dep-tree-check.log）
- downstream 备忘: Boot 层消费点待 2.2 切换（MP boot3-&gt;boot4 starter、knife4j jakarta/webflux-&gt;boot4、druid boot-3-&gt;boot-4、dynamic-datasource common、mate autoconfigure 已机械迁移待编译验证）；BOM 兜底升级触达 Boot/AI 消费点（minio/jodd-core/dom4j/x-easypdf/protobuf 4.35.1/logstash 9.0）各层构建验证；AI 基线测试 SpringBoot3DependencyBaselineTest 待 4.3 更新；protobuf 全局 pin 升至 4.35.1（AI A2A 注释与测试作用域覆盖待 4.2/4.3 重验）；mate 样例已机械迁移待 5.1 验证；TransType.BOOLEAN 用户迁移路径=fw 超集接口
- task 1.2/1.3 审查整改（CRITICAL C-1）完成: fw-plus-core/pom.xml 顶层 parent 3.5.8 -&gt; 4.1.1；effective-pom 全量复核 3.5.8 残留 0 处、spring-boot 构件均 4.1.1；安装产物 fw-plus-build-0.3.0-SNAPSHOT.pom 父引用同步（log core-build-post-c1-4.log / core-effective-post-c1.xml）
- C-1 连带适配①: Hibernate Validator 8.0.x -&gt; 9.1.3，CommonsValidator 改经 jakarta.validation.Path 公共接口取 basePath 节点（toString() 与旧 NodeImpl.asString() 字节码等价）；新增回归测试 CommonsValidatorHibernateValidator9Test（3 用例：嵌套属性、#jsonPath 探针、List 索引路径）
- C-1 连带适配②: fw-plus-magicapi 显式引入 com.fasterxml.jackson.core:jackson-databind（不写版本），由 Boot 4.1.1 jackson-2-bom（2.21.5）统一管理，与 Jackson 3 命名空间共存（magic-api 2.2.2 fork 仍为 Jackson 2 源码）
- C-1 修复后全量证据: cd fw-plus-core &amp;&amp; JAVA_HOME=zulu-21 ./mvnw clean install（含测试）-&gt; BUILD SUCCESS，240 tests / 0F / 0E / 3 skipped（1:04 min；log core-build-post-c1-4.log）
- 独立复查轮完成: 结论通过；MINOR「List 用例补索引 jsonPath 探针」已采纳补入并复测 3/3 通过（log common-retest-probe.log）；@Test 注解风格与模块既有约定一致，留待 JUnit Jupiter 迁移时处理
- 修订待办: 用户裁定本仓四层 revision 需改版本号（目标值待确认后执行：四层 pom + sample + 文档基线 + 图 JDBC/CLAUDE.md 0.1.1 残留对齐）
- revision 仓位变更完成（用户裁定）: 四层 revision 与跨层 parent 引用、sample 聚合器、根 pom revision 属性、13 个被跟踪 .flattened-pom.xml、文档基线（AGENTS/README/CLAUDE/升级说明/agentic 指南）、图 JDBC 两处 0.1.1 残留全部对齐 0.4.0-SNAPSHOT；change design.md 与升级说明 1.4 节留裁定备注
- 0.4.0 重建证据: Core clean install（含测试）BUILD SUCCESS，240 tests / 0F / 0E / 3 skipped；.m2 新增 48 个 0.4.0-SNAPSHOT 目录；fw-plus-build:0.4.0-SNAPSHOT.pom 父引用 4.1.1；根 pom project.version=0.4.0-SNAPSHOT（log core-build-0.4.0.log）
- Sample validate（0.4.0）预期性暂红并留证: 报错精确命中 fw-plus-boot-parent:0.4.0-SNAPSHOT 与 fw-plus-boot-dependencies:0.4.0-SNAPSHOT 未安装，证明 revision 联动传播生效；随 Boot/Cloud/AI 重建（2.x/3.x/4.x）消解，5.x 复验（log sample-validate-post-bump.log）

## Unresolved
