# Spring Boot 4 / Java 21 升级说明

> **文档定位**：fw-plus 从 JDK 17 + Spring Boot 3.5.8 升级到 JDK 21 + Spring Boot 4.1.x 的完整升级说明。
> 结构对齐 `docs/springboot3-upgrade-notes.md`：版本矩阵、破坏性清单、代码层适配、生态组件处置、待办。
>
> **当前基线**（升级后）：JDK `21`、Spring Boot `4.1.1`、Spring Cloud `2025.1.3`（Oakwood）、
> Spring Cloud Alibaba `2025.1.0.0`、Spring AI `2.0.1`、LangChain4j `1.22.0`、
> MyBatis-Plus `3.5.17`（boot4 坐标）、MyBatis-Flex `1.11.8`、Magic-API `2.2.2`（内部维护版）、
> Sa-Token `1.46.0`、knife4j-next fork `5.7.8`、springdoc `3.0.3`、easy-trans `org.dromara:4.0.2`。
>
> **推进方式**：分层闭环 Core → Boot → Cloud / AI → Sample → 根聚合收口
> （设计见 `docs/openspec/changes/java21-springboot4-upgrade/design.md` D3）。
> 各层章节随该层完成后补齐；本文档在 Core 层完成后创建。

---

## 0. 结论速览

| 维度 | 现状（升级前） | 目标 | 状态 |
|---|---|---|---|
| JDK | 17 | **21** | ✅ Core 已落地（四层 `jdk.version`，随各层补齐） |
| Spring Boot | 3.5.8 | **4.1.1** | ✅ Core BOM 已落地 |
| Spring Cloud | 2025.0.0 | **2025.1.3**（Oakwood） | ✅ Core BOM 已落地（Cloud 层实测待 3.x） |
| Servlet/Tomcat | Tomcat 10.1 线 | **Tomcat 11.0.27**（Servlet 6.1） | ✅ 已钉（含 CVE-2026-76183 修复，Boot 管理值 11.0.24 受影响） |
| Jackson | 2.x | **3.x**（`tools.jackson`） | ⏳ Boot 层起按编译驱动适配 |
| MyBatis-Plus | 3.5.16（boot2 坐标） | **3.5.17（boot4 坐标）** | ⏳ Core BOM 已切；消费点坐标随 Boot 层（2.2）切换 |
| MyBatis-Flex | 1.11.4 | **1.11.8** | ⏳ Core BOM 已切；消费点坐标随 Boot 层切换 |
| easy-trans | 3.0.6（`com.fhs` 包） | **4.0.2（`org.dromara` 包）** | ✅ Core 已适配（包迁移 + 适配点见 2.1） |
| Sa-Token / Druid | 1.46.0 / 1.2.28 | **1.46.0 / 1.2.28（boot4 坐标）** | ⏳ 坐标随 Boot 层（2.2）切换 |
| 文档组件 | knife4j 4.5.0 / springdoc 2.8.17 | **knife4j-next fork 5.7.8 / springdoc 3.0.3** | ⏳ Core BOM 已切；消费点随 Boot 层切换 |
| mica-mqtt | 2.3.9 | **2.5.9+** | ⏳ Boot 层（2.1） |
| Magic-API / EasyPoi | 2.2.2 / fork | **不变版本号** | ✅（仓内自维护，兼容性以实测为准） |
| Core 层构建 | — | `clean install -DskipTests` | ✅ BUILD SUCCESS（37.98s → 兜底升级后 52.9s） |
| Core 层测试 | — | `clean test`（Core 默认跑测试） | ✅ 237 tests / 0 failures / 3 skipped |

---

## 1. 目标版本矩阵

### 1.1 核心矩阵（设计 D1，Core BOM 已落地）

| 组件 | 升级前 | 升级后 | 说明 |
|---|---|---|---|
| JDK | 17 | 21 | 四层顶层 pom `jdk.version`；auth-core 的 1.8 遗留已清理（Core） |
| Spring Boot | 3.5.8 | **4.1.1** | `spring-boot-dependencies` |
| Spring Cloud | 2025.0.0 | **2025.1.3** | Oakwood 线 |
| Spring Cloud Alibaba | 2025.0.0.0 | **2025.1.0.0** | 适配 Boot 4.1 |
| Spring Cloud Tencent | 1.14.0-2023.0.0-RC2 | 2.1.2.0-2025.1.1 | 留待 Cloud 层（3.2）实测 |
| Spring AI | 1.1.8 | **2.0.1** | 2.0 线面向 Boot 4.x |
| Spring AI Alibaba / extensions | 1.1.2.4-security-fix | **2.0.0-M1.1** | 适配线里程碑 |
| LangChain4j | 1.22.0 | **1.22.0**（community `1.22.0-beta32`） | 对 Boot 无强绑定，保持最新稳定线 |
| MyBatis-Plus | 3.5.16 | **3.5.17** | BOM 移除 boot2 坐标条目，强制切 `mybatis-plus-spring-boot4-starter` |
| MyBatis-Flex | 1.11.4 | **1.11.8** | 切 `mybatis-flex-spring-boot4-starter`；Boot4 autoconfigure 不再默认含 jdbc（消费点补 starter 见 2.2 备忘） |
| mybatis-spring | 3.x | **4.0.0** | 随 MP/Flex boot4 线 |
| mybatis-plus-join | 1.5.2 | **1.5.9** | Core `fw-plus-mp-join` 仓内 fork 已适配（见 2.1.2） |
| Sa-Token | 1.46.0 | **1.46.0** | 版本不变，坐标切 `sa-token-spring-boot4-starter`（BOM 已纳管） |
| Druid | 1.2.28 | **1.2.28** | 版本不变，坐标切 `druid-spring-boot-4-starter` |
| dynamic-datasource | 4.3.1 | **4.6.0** | 切 `dynamic-datasource-spring-boot4-starter` |
| easy-trans | 3.0.6 | **4.0.2** | 4.0.x=Boot4 线；groupId 切 `org.dromara`，包名 `com.fhs.*` → `org.dromara.*` |
| knife4j / springdoc | 4.5.0 / 2.8.17 | **fork `com.baizhukui` 5.7.8 / 3.0.3** | 上游无 Boot4 支持，切社区 fork（仅改 groupId；无 webflux boot4 构件 → WebFlux 场景由 springdoc webflux-ui 3.0.3 承载，见 2.2） |
| mica-mqtt | 2.3.9 | **2.5.9+** | Boot 4 / Jackson 3 支持线（Boot 层 2.1 落地） |
| Tomcat | Boot 管理 10.1 线 | **11.0.27**（显式钉版） | Boot 4.1.1 管理值 11.0.24 受 CVE-2026-76183 影响（≤11.0.25），11.0.27 为 2026-10-06 发布修复版 |
| Magic-API / EasyPoi | 2.2.2 / fork | **不变** | 仓内自维护/内嵌 fork，版本号不动 |

### 1.2 Core BOM 其余组件兜底升级记录（2026-10-16）

> 口径：用户确认「其余包版本可在 Boot 4.1 兼容线内升级」。
> 执行方式：先修完编译基线，再对 Core BOM 显式钉版做一轮兜底复核（对 Maven Central 元数据比对），
> 之后 `clean install -DskipTests` + `clean test` 回归。基础库与 Boot 4.1.1 管理口径对齐优先。

以下 31 项属性就地升级（均在原大版本线内或对齐 Boot 管理值）：

| 属性 | 升级前 | 升级后 | 备注 |
|---|---|---|---|
| `commons-lang3.version` | 3.17.0 | 3.20.0 | 对齐 Boot 4.1.1 管理值 |
| `commons-io2.version` | 2.18.0 | 2.22.0 | `commons-io:commons-io` |
| `commons-collections4.version` | 4.4 | 4.6.0 | |
| `commons-compress.version` | 1.21 | 1.28.0 | 含多项 CVE 修复面 |
| `commons-text.version` | 1.14.0 | 1.15.0 | |
| `commons-net.version` | 3.11.1 | 3.13.0 | |
| `commons-validator.version` | 1.9.0 | 1.11.0 | |
| `commons-beanutils.version` | 1.9.2 | 1.11.0 | 含 CVE 修复面 |
| `commons-logging.version` | 1.3.4 | 1.4.0 | |
| `hutool.version` | 5.8.47 | 5.8.48 | |
| `fastjson.version` | 2.0.63 | 2.0.65 | `com.alibaba:fastjson`（v1 兼容坐标） |
| `fastjson2.version` | 2.0.63 | 2.0.65 | |
| `guava.version` | 31.1-jre | 33.7.2-jre | 消费点仅用 `Throwables`、`Cache/CacheBuilder` |
| `byte-buddy.version` | 1.17.6 | 1.18.11 | 对齐 Boot 4.1.1 管理值（byte-buddy / byte-buddy-agent） |
| `javassist.version` | 3.30.2-GA | 3.33.0-GA | |
| `error_prone_annotations.version` | 2.36.0 | 2.50.0 | 注解库 |
| `checker-qual.version` | 3.19.0 | 4.3.1 | 注解库 |
| `dom4j.version` | 2.1.4 | 2.2.0 | Boot 层消费点随 2.x 验证 |
| `assertj-core.version` | 3.26.3 | 3.27.7 | 对齐 Boot 4.1.1 管理值 |
| `logstash-logback-encoder.version` | 8.0 | 9.0 | 9.0 为 Jackson 3 线，与 Boot 4 基线一致 |
| `json.version` | 20250517 | 20260814 | `org.json:json` |
| `json-path.version` | 2.9.0 | 2.10.0 | 对齐 Boot 4.1.1 管理值 |
| `libphonenumber.version` | 8.13.51 | 8.13.55 | 同 8.13 线（9.x 为跨大版本，未取） |
| `protobuf-java.version` | 3.25.1 | 4.35.1 | 对齐 Boot 4.1.1；AI 层 A2A 注释与 test-scope 覆盖待 4.x 复核 |
| `kryo.version` | 5.0.3 | 5.7.1 | 同大版本线 |
| `zstd-jni.version` | 1.5.7-12 | 1.5.7-23 | |
| `sensitive-word.version` | 0.24.0 | 0.29.5 | |
| `aviator.version` | 5.4.3 | 5.4.4 | |
| `jodd-core.version` | 5.0.13 | 5.3.0 | Boot 层消费点随 2.x 验证 |
| `bcprov-jdk18on.version` | 1.78.1 | 1.86 | 同时将 `fw-plus-common` 坐标由 `bcprov-jdk15on` 切到 `bcprov-jdk18on`（API 同包，JDK 21 基线下代际正确） |
| `minio.version` | 8.5.17 | 8.6.0 | 同 8.x 线（9.0.3 为跨大版本，未取）；Boot 层 file-storage 消费点随 2.x 验证 |

另修复一项 BOM 缺陷：

- **`jakarta.servlet-api` 重复声明**：BOM 内曾同时存在 `${jakarta.servlet-api.version}`（6.1.0）与 `${jakarta.version}`（6.0.0，Boot3.0 时代遗留属性）两个条目，有效值被后者拉低到 6.0.0。已删除旧条目与孤儿属性 `jakarta.version`，有效值恢复 6.1.0（与 Boot 4.1.1 管理值、Tomcat 11 线一致）。

### 1.3 已复核但维持不动的钉版（避免盲目升级）

- 已是最新版本：`x-easypdf-fop 3.2.1`、`zxing 3.5.4`、`jfreechart 1.5.6`、`xercesImpl 2.12.2`、`protostuff 1.8.0`、`transmittable-thread-local 2.14.5`、`sharding-jdbc-core 4.1.1`、`datastax-java-driver 4.17.0`。
- 停更 / 遗留代际保留：`orika 1.5.4`、`pinyin4j`、`jsch 0.1.55`、`commons-io 1.3.2`（旧 group）、`commons-lang2 / collections3`、`javax.servlet-api 4.0.1`、`aws-java-sdk-s3 1.12.x`（v1 已 EOL，终端版本线）。
- 联动保留：`poi` 系版本与仓内 EasyPoi fork / poi-tl 联动，不在本轮调整；`jsqlparser 4.6`（datachange SQL 解析行为依赖，不跨大版本）。
- 待各层处置：`spring-plugin-core 2.0.0.RELEASE`（暂未确认 Boot 层 knife4j fork 的传递依赖需求，留待 2.2 核对）；`x-file-storage 2.2.1`（2.3.0 可用，留待 Boot 层 file-storage 消费点验证时一并评估）。

### 1.4 仓位（revision）变更记录（2026-10-16，用户裁定）

四个库层 `<revision>` 与 `fw-plus-sample` 聚合器由 `0.3.0-SNAPSHOT` 升为 `0.4.0-SNAPSHOT`（覆盖原「不调版本号」约束；
裁定留痕见 change 的 `design.md` 既定约束节与协调检查点）。

联动对齐：`fw-plus-boot` / `fw-plus-cloud` / `fw-plus-ai` 对上层 parent（`fw-plus-parent` / `fw-plus-boot-parent`）的硬编码版本引用；
根 `pom.xml` 补齐 `<revision>` 属性（原为未定义，会以字面量 `${revision}` 作为自身版本安装，现 `project.version` 解析为 `0.4.0-SNAPSHOT`）；
被跟踪的 13 个 `.flattened-pom.xml`（Cloud 8 + Sample 5）；文档基线（`AGENTS.md` / `README.md` / `CLAUDE.md` / 本说明 / `docs/langchain4j-agentic-guide.md`）；
图 JDBC 两处 `0.1.1-SNAPSHOT` 残留（Neo4j / Nebula 驱动版本串）。Sample 子 pom 的 `${revision}` 经 `fw-plus-boot-build` 解析，随 Boot 层天然联动，无需逐文件修改。

重建验证：Core `clean install`（含测试）在 `0.4.0-SNAPSHOT` 下 BUILD SUCCESS，**240 tests / 0F / 0E / 3 skipped**（log `core-build-0.4.0.log`）；
安装 `fw-plus-build:0.4.0-SNAPSHOT.pom` 父引用仍为 4.1.1。Sample 聚合器 validate 现为**预期性暂红**——报错精确命中
`fw-plus-boot-parent:0.4.0-SNAPSHOT` 与 `fw-plus-boot-dependencies:0.4.0-SNAPSHOT` 未安装（证明 `${revision}` 联动已生效），
随 Boot / Cloud / AI 层重建（2.x / 3.x / 4.x）消解；5.x 复验聚合器校验、6.x 根收口。

---

## 2. 破坏性适配清单（编译 / 测试驱动，逐点记录）

### 2.1 Core 层（✅ 已完成）

#### 2.1.1 Spring Framework 7：`HttpHeaders` 不再实现 `Map`

`org.springframework.http.HttpHeaders` 在 Framework 7 中移除了 `Map` 接口实现，
`keySet()` / `entrySet()` / `containsKey()` 等方法不再可用，改用 `headerNames()`（返回 `Set<String>`）。

| 文件 | 适配 |
|---|---|
| `fw-plus-common/.../common/web/WebFluxRequestContextHolder.java` | `headers.keySet()` → `headers.headerNames()` |
| `fw-plus-magicapi/.../magicapi/core/web/RequestHandler.java` | `getHeaders().keySet()` → `getHeaders().headerNames()` |

#### 2.1.2 mybatis-plus-join 1.5.9：版本适配层移除

join 1.5.9 删除了 `adapter.base.tookit.VersionUtils` 与 `adapter.v3431.AbstractMethodV3431`
（版本分支适配层终结），仓内 fork `MPJSqlInjector` 无法再编译。

- 处置：删除两个死 import，`getJoinMethod` 简化为无条件 `SqlMethod` 构造；
  **保留**仓库自有行为——`methodFilter` 方法移除清单与 `MpSqlInjector` 基类、对 JoinMapper 的注入范围
  （对照官方 1.5.9 源码，官方仅对 `JoinMapper` 注入 join 方法）。

#### 2.1.3 easy-trans 3.0.6 → 4.0.2：`com.fhs.*` → `org.dromara.*` 全量包迁移

easy-trans 4.0.x（Boot4 线，groupId `org.dromara`）把包名从 `com.fhs.*` 整体迁到 `org.dromara.*`
（子路径一一对应）。这导致：

1. **源码 import 全量迁移**（共 25 个文件）：Core easytrans 17 个 + Poi fork `TransUtils` 1 个 +
   Boot `MateEasyTransAutoConfiguration` 1 个 + mate 样例 6 个；另有 2 个复刻类直接删除（见下条）；
2. **删除 annotation 模块内的复刻类** `com.fhs.core.trans.anno.Trans` 与 `com.fhs.core.trans.constant.TransType`：
   原设计用同 FQCN 遮蔽上游注解，但 4.0.2 后上游 FQCN 变为 `org.dromara.*`，复刻类既失去遮蔽意义，
   又因缺少上游新增的 `pkeyClassType()` 属性存在 `NoSuchMethodError` 风险（上游 `SimpleTrans`/`SimpleTransService` 会调用）；
3. **新增 fw 超集接口** `com.xiongx9527.fw.plus.easytrans.annotation.TransType`（extends 上游 `TransType`）：
   收纳 fw-plus 扩展常量 `BOOLEAN`（布尔翻译，对应 `MateEasyTransBooleanTransService`），
   不遮蔽上游 FQCN；fw-plus 代码与样例统一引用该超集接口，普通项目亦可继续直接用上游 `TransType`；
4. `fw-plus-easytrans-annotation` 增加 `org.dromara:easy-trans-anno` 直接依赖（供超集接口 extends）。

> **用户迁移提示**：引用 fw-plus easytrans 注解的项目需同步把 `com.fhs.*` import 改为 `org.dromara.*`；
> 使用 `TransType.BOOLEAN` 的代码将 import 切换为
> `com.xiongx9527.fw.plus.easytrans.annotation.TransType` 即可保持原写法不变。

#### 2.1.4 Boot4 迁移连带项（本层已改、后续层验收）

- Boot 层 `MateEasyTransAutoConfiguration` 与 mate 样例文件的 `com.fhs.*` → `org.dromara.*` 机械迁移已随本层完成，
  编译验证分别在 Boot 层（2.x）与 Sample 层（5.x）执行。

#### 2.1.5 Boot 4.1.1 父链对齐（独立审查修复 C-1）与连带适配

审查发现 `fw-plus-core/pom.xml` 顶层 parent 仍为 `spring-boot-starter-parent:3.5.8`，而依赖管理经 BOM import 已升 4.1.1。
Maven 语义下**父链继承的 dependencyManagement 先于 import 展开且优先**，导致 73 项 `org.springframework.boot:*`
（含全部 `spring-boot-starter-*` 与 `spring-boot-maven-plugin`）仍解析到 3.5.8。处置：parent 直接对齐 4.1.1
（版本单点原则：父与 BOM 必须同线）。修复后 `help:effective-pom` 全量复查：`3.5.8` 残留 **0 处**，
`spring-boot` 等构件统一 4.1.1；安装产物 `fw-plus-build:0.4.0-SNAPSHOT.pom` 父引用同步为 4.1.1（跨层传播生效）。

父链对齐后连带暴露并修复的两处兼容问题：

| 问题 | 处置 |
|---|---|
| Hibernate Validator 8.0.x → **9.1.3.Final**：`org.hibernate.validator.internal.engine.path.PathImpl` / `NodeImpl` 被移除，`CommonsValidator` 无法编译 | 不再引用 HV 内部路径类：`ConstraintValidatorContextImpl.basePath` 反射值按 `jakarta.validation.Path` 公共接口迭代（HV9 实现类为 `MutablePath`/`MutableNode`，实测其 `toString()` 与旧 `NodeImpl.asString()` 字节码等价）；新增回归测试 `CommonsValidatorHibernateValidator9Test`（3 用例：嵌套属性校验结果、`#jsonPath` 变量值、List 索引路径——含 `$.nested.items[0].pathProbe` jsonPath 探针与 `nested.items[0].email` 违规路径断言） |
| Jackson 2/3 共存：`magic-api 2.2.2` fork 源码基于 Jackson 2（`com.fasterxml.jackson`），而 Boot 4 的 starter-web 不再传递 Jackson 2，`fw-plus-magicapi` 编译失败 | `fw-plus-magicapi` 显式声明 `com.fasterxml.jackson.core:jackson-databind`（**不写版本**），版本由 Boot 4.1.1 的 `jackson-2-bom`（2.21.5）统一管理；与 Jackson 3（`tools.jackson`）命名空间共存 |

> 钉版坑位：Jackson 2.20 起 `jackson-annotations` 不再发布补丁版本（只有 `2.21`，没有 `2.21.1`），
> 若自行按 `${x}.${y}.1` 钉 annotations 会解析失败；统一走 Boot 的 `jackson-2-bom` 即可规避。

修复后 Core 全量证据：`clean install`（含测试）BUILD SUCCESS，全 reactor **240 tests / 0 failures / 0 errors / 3 skipped**（1:04 min）；
wrapper 固定 Maven 3.6.3 与 Boot 4.1.1 库层构建链实测兼容（sample 层 `spring-boot-maven-plugin` 打包待 5.x 冒烟复核，见附录 B）。

### 2.2 Boot 层（✅ 已完成，2026-10-17）

范围：starter / 模块坐标切换（MP boot3→boot4、knife4j jakarta→boot4 线、druid boot-3→boot-4、
sa-token boot4、dynamic-datasource boot4、nacos config 2025.1.0.0 钉版）、Jackson 3 适配、
JUnit 6 测试 API、mica-mqtt 2.5.9+、SBA 4.1.x、Spring 7 装配适配（`RequestMappingInfo` 尾斜杠 API 移除、
`WsHandlerMapping`/WebFlux 装配、_refresh scope 依赖补齐）等。

关键处置记录：

- **knife4j webflux**：knife4j-next fork（`com.baizhukui`）无 Boot4/webflux 变体构件（条目 -2 见 1.1），
  WebFlux 场景改由 **springdoc `springdoc-openapi-starter-webflux-ui 3.0.3`**（Core BOM 管理段）承载；
  autoconfigure 补 optional 依赖，`MagicAPIWebFluxImportFilter` 常量适配 Spring 7。
- **nacos config 钉版**：旧锁 2021.0.5.0 的元凶是 bov BOM import 的 `dynamic-tp-dependencies`（boot 层 1.2.2）；
  boot BOM 同 pom 自身 management 优先于 parent 继承，故在 boot `fw-plus-boot-dependencies` 的
  dynamic-tp import **之后**显式钉 `spring-cloud-starter-alibaba-nacos-config = 2025.1.0.0`。
- **vendored Mapping（Core 侧）**：Spring 7 移除 `setTrailingSlashMatch`，仓内自维护的 magic-api 2.2.2 按既有
  同 FQN 遮蔽惯例补 `org.ssssssss.magicapi.utils.Mapping`（fw-plus-magicapi classpath 序先于 magic-api），
  删尾斜杠守卫、恒走 patternParser 分支。
- **前端资源链路（clean 断链修复）**：magicapi 插件父 pom 的 `clean-console-dist`（clean 阶段删
  `src/console/dist`，出自 cd6ff8818「npm 动态生成 dist」的旧设计）与 dist 纳入 git 跟踪（bbb770222
  preserve plugin resources，支撑 `mvn clean test` 经 `copy-console-dist-on-compile` 补齐 target/classes）
  互相矛盾——全量 `clean test` 时 dist 先被删，所有 `xxxFrontendClasspathTest` 必挂（jdbc-starter 首先爆出，
  admin/swagger 因 reactor 在 jdbc 处 fail-fast 被 SKIPPED 而未暴露）。处置：**移除 `clean-console-dist`**，
  保留 `clean-stale-console-assets`（initialize 清 target/classes/magic-editor）+ process-resources 拷贝配对。
- **admin 前端测试归位**：`MagicApiAdminFrontendClasspathTest` 原在 `fw-plus-admin-server-spring-boot-starter`，
  但 0b608fc29 已将其对 plugin-admin 的依赖移除（plugin-admin 反向依赖 admin-server-starter，补回会成
  reactor 循环）——该测试自 0b608fc29 起即前提失效。按 swagger 同型样板（测试放回插件模块自身）迁移：
  删 starter 内孤儿测试，在 `fw-plus-magicapi-plugin-admin` 内补等效测试（其 pom 补 test-scope
  `spring-boot-starter-test`，与 plugin-swagger 一致）。
- **测试基建**：autoconfigure 补 minio/aliyun-sdk-oss test 依赖（S3 平台选项用例）、
  `WsEndpointHttpUpgradeTest` 加 `spring.cloud.nacos.config.import-check.enabled=false`（新版 SCA
  ImportException 关闭）、`WsWebFluxAutoConfigurationTest` 补 RefreshAutoConfiguration runner。

Boot 全量证据：`cd fw-plus-boot && ./mvnw clean test -Dmaven.test.skip=false` → **BUILD SUCCESS**，
50/50 模块 SUCCESS：autoconfigure **651 tests / 0 failures / 0 errors / 0 skipped** + 各 starter 冒烟
（jdbc/druid-admin 插件资源、magicapi DbTx、admin 插件资源、eureka 等）合计 660 tests / 0 failures。

### 2.3 Cloud 层（⏳ 待 3.x 完成）

预留：Spring Cloud Tencent 2.1.2.0-2025.1.1 实测、eureka/gateway 装配回归等。

### 2.4 AI 层（⏳ 待 4.x 完成）

预留：Spring AI 2.0.1 / Spring AI Alibaba 2.0.0-M1.1 / AgentScope / LangGraph4j 装配适配、
`SpringBoot3DependencyBaselineTest` 旧版本断言更新、protobuf 4.35.1 与 A2A 的复核等。

### 2.5 Sample 层（⏳ 待 5.x 完成）

预留：15 个样例编译适配（含 MP-join boot-starter 传递 boot3 starter 的排除处置、
MF 样例补 `spring-boot-starter-jdbc`、mate 样例迁移验证等）。

---

## 3. 验证清单

| 层 | 命令 | 状态 |
|---|---|---|
| Core 构建 | `cd fw-plus-core && JAVA_HOME=<zulu-21> ./mvnw clean install -DskipTests` | ✅ BUILD SUCCESS（矩阵升级后 37.98s；兜底升级后 52.9s） |
| Core 测试 | `cd fw-plus-core && ./mvnw clean test` | ✅ 237 tests / 0 failures / 0 errors / 3 skipped（17 个模块） |
| Core 构建+测试（C-1 修复后） | `cd fw-plus-core && JAVA_HOME=<zulu-21> ./mvnw clean install`（含测试） | ✅ BUILD SUCCESS，240 tests / 0 failures / 0 errors / 3 skipped（1:04 min） |
| Core 构建+测试（revision 0.4.0 重建） | `cd fw-plus-core && JAVA_HOME=<zulu-21> ./mvnw clean install`（含测试） | ✅ BUILD SUCCESS，240 tests / 0 failures / 0 errors / 3 skipped |
| 根 pom 版本解析（新增 `<revision>` 属性） | `./mvnw -N help:evaluate -Dexpression=project.version -DforceStdout` | ✅ `0.4.0-SNAPSHOT` |
| Sample 聚合器（0.4.0） | `./mvnw -f fw-plus-sample/pom.xml validate` | ⏳ 预期性暂红：命中 `fw-plus-boot-*:0.4.0-SNAPSHOT` 未安装，随 2.x/3.x/4.x 重建消解 |
| Core 父链残差核对 | `help:effective-pom` 全量 dump 复查 | ✅ `3.5.8` 残留 0 处，关键构件统一 4.1.1 |
| Core 关键解析核对 | `dependency:tree`（servlet-api / guava / bcprov / byte-buddy / commons-lang3） | ✅ 6.1.0 / 33.7.2-jre / 1.86 / 1.18.11 / 3.20.0 |
| Boot 构建 | `cd fw-plus-boot && ./mvnw clean install -DskipTests` | ✅ BUILD SUCCESS（BOM 钉版/依赖调整后） |
| Boot 测试 | `cd fw-plus-boot && ./mvnw clean test -Dmaven.test.skip=false` | ✅ BUILD SUCCESS：50/50 模块，autoconfigure 651 + starter 冒烟合计 660 tests / 0 failures（2026-10-17） |
| Boot / Cloud / AI / Sample | 见各层章节 | Boot ✅；Cloud / AI / Sample ⏳ |
| 根聚合收口 | `./mvnw clean install -DskipTests`（根目录） | ⏳ |

---

## 附录 A：版本比对方法（本轮兜底复核所用）

对 Core BOM 中每一项显式钉版属性，取 Maven Central 元数据（`maven-metadata.xml` 的 `<release>`
或同线版本列表）与当前值对比，按「Boot 4.1.1 管理口径优先 → 同大版本线内取新 → 跨大版本线不盲目取」
的顺序决策；跨大版本或联动敏感的（poi 系、jsqlparser、storage SDK、EOL 组件）保持不动并记录理由。

## 附录 B：待办 / 观察项

- [ ] Boot 层（2.2 必修）：`MpJoinAutoConfiguration` 仍引用 mybatis-plus-join 1.5.9 已删除的 `VersionUtils`，切换消费点（boot3→boot4 starter）前先移除。
- [ ] Boot 层：核对 `spring-plugin-core` 传递依赖需求（knife4j fork / springdoc 3.0.3 实际引用情况）。
- [ ] Boot 层：file-storage 消费点评估 `x-file-storage 2.3.0` 与 `minio 8.6.0` 组合。
- [ ] AI 层：复核 protobuf 全局提升到 4.35.1 后 A2A 场景与 `fw-plus-ai-boot-autoconfigure` 的 test-scope 覆盖；同步更新 A2A 注释版本口径（3.25.1 → 4.35.x）。
- [ ] AI 层：`SpringBoot3DependencyBaselineTest` 的旧版本断言更新为 Boot4 基线。
- [ ] Sample 层：mate 样例 `TransType` / `Trans` 迁移与新 import 的编译验证。
- [ ] Sample 层：wrapper Maven 3.6.3 + `spring-boot-maven-plugin` 4.1.x 打包冒烟（库层构建链已实测兼容）。
- [ ] Core 备注：`bcprov-jdk15on` 为已知死配置（保留以兼容旧消费方，后续可清理）；easytrans-service 的 `spring-boot-starter-test` 重复声明无害，待顺手清理。
