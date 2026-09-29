---
comet_change: langchain4j-magicapi-model-plugins
role: technical-design
canonical_spec: openspec
archived-with: 2026-09-29-langchain4j-magicapi-model-plugins
status: final
---

# Design

## Context

动机与范围见 `proposal.md`；行为契约见 `specs/`。本节只记录影响实现方式的现状与约束。

**当前状态**：`fw-plus-ai` 已把 LangChain4j 以 magic-api `@MagicModule` 形式接入，但 `modules/langchain4j/core/Langchain4jModule` 在 `0e8af325e` 中被清空为占位类，工作区里另有一份尚未定案的 261 行版本（仍为 `@MagicModule("uid")` + 全套 `build*`）。七类 datasource 插件的后端骨架已提交（`2322ef973`、`5240c9f5b`），落在 `plugin/langchain4j/**`，但装配是一个 239 行的单类配置、运行时实例由一个跨七类的共享 holder 持有，且前端子工程、测试与 Module 改造尚未完成。`modules/langchain4j/tool/**` 下的工具桥为未提交新增，无既有设计覆盖。

**已确定的约束**：

- core 层 `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j-*` 只作参考，零修改（含修饰符）。
- Java 8：不得使用 `var`、`record`、`List.of()`、`Map.copyOf()` 等 Java 9+ API；注释与异常信息用简体中文。
- 供应商模型类来自 langchain4j 官方 jar，由 `fw-plus-ai-boot-autoconfigure` 以 `optional` 引入；版本由 AI 层 BOM 统一管理。
- 既有插件惯例（`MagicAPIMongoConfiguration`、`MagicAPIKafkaConfiguration`、`MagicAPIDruidConfiguration`）决定了目录分层、装配写法与运行时持有者的形态。

## Goals / Non-Goals

**Goals:**

- 七类模型资源以 magic-api datasource 形式可配置、可热更新、可停用，生命周期与数据源一致。
- 运行时实例持有层与仓库既有插件同构，便于后续维护与增量接入新供应商。
- 装配入口可读、可分层：单类型改动不影响其它类型。
- 脚本侧只有一条只读取用通道，实例来源唯一。
- magic-api 已有资源可作为本地工具交给模型调用，且曝光范围可控。

**Non-Goals:**

- 不做 `AiServices` / `Assistant` / `ContentRetriever` / `RetrievalAugmentor` / `ModerationModel` 的 datasource 化（本次仅七类）。
- 不做敏感参数加密存储（`api-key` 等与 magic-api 内建 datasource 密码字段行为一致）。
- 不做集群同步、参数 schema 强校验引擎。
- 不做 embedding-store 对 embedding-model 的级联删除。
- 不修改 core 层；不在本次处理 chat-sample 的会话级能力（另建 change）。

## Decisions

### D1. datasource 形态与 JSON 结构

每类资源在控制台「数据源」页签注册为一个数据源类型，JSON 经 magic-api 通用 `/resource/file/{type}/save` 接口持久化到 `{type}/*.json`，与官方 `mongo-datasource`、`redis-datasource` 同构。

JSON = 公共字段 + `options` 差异参数对象。公共字段 `key` / `name` / `provider` / `enabled` 提取到实体基类（`embedding-store` 扩展 `embeddingModelKey`）；其余一律入 `options` 自由 map，因为不同 provider 的参数天然不一致（qwen 有 `enable-search`，openai 没有）。

*替代方案*：把所有参数拍平成实体字段 → 字段数不可控且每加一个供应商都要改实体，放弃。

### D2. 每类资源的运行时持有者与注册器

七类资源各自拥有：

| 角色 | 形态 | 职责 |
| --- | --- | --- |
| 运行时持有者 | `MagicDynamic<Type>DataSource`，`implements DisposableBean` | 按 `key` 持有/驱逐/列举实例，`destroy()` 统一关闭；只依赖本类型实例 |
| 动态注册器 | `<Type>MagicDynamicRegistry`（`AbstractMagicDynamicRegistry<T>`） | 遵循 CLAUDE.md「MQ 插件动态注册器」规则 A：先驱逐 → `enabled` 检查 → 工厂构建 → 成功才落库 |
| 资源存储 | `<Type>MagicResourceStorage`（`MagicResourceStorage<T>`） | `folder()` = 该类型 datasource type；`key` 必填/格式/同类唯一；`provider` 命中本类型短名表；`embedding-store` 另校验 `embeddingModelKey` |

*决策依据*：与 `MagicDynamicKafkaDataSource`、`MagicDynamicMongoDataSource` 等 12 个既有插件同构（同样的持有者+注册器+存储三件套、同样的 `DisposableBean` 释放语义）。

*替代方案*：单一跨类型 `Langchain4jModelHolder`（当前已提交实现）→ 与仓库既有插件不同构，一个类承担七类职责，类型安全靠 `EnumMap` + 强转维持；已被否。

### D3. 装配拆为七个按类型的子 Configuration

父类 `Langchain4jDatasourceConfiguration implements MagicPluginConfiguration` 只保留跨类型共享内容（插件标识 `plugin()`、`controllerRegister()`、工厂注册表）与共享 bean，通过 `@Import` 引入七个按资源类型的顶层子配置类；每个子配置只声明本类型的持有者、存储与注册器 bean。

*决策依据*：与 `MagicAPIDruidConfiguration`（`@Import({DruidSpringAopConfiguration, ...})`）一致；单类型 bean 定义集中在一处，改动一个类型不触及其它六个。

*替代方案*：
- 内部 `static class` 嵌套子配置 → 写法与仓库既有高层级插件不一致，放弃。
- 七个子配置全部由自动配置入口直接注册 → 装配入口分散、`plugin()` / `controllerRegister()` 仍需一个实现类，放弃。

*补充要求*：单类型装配或构建失败不得阻止其它类型可用（已写入 `specs/langchain4j-model-datasource-plugins`）。

*实施补充*：与类型无关的共享内容（工厂注册表）单独放进 `Langchain4jModelFactoryConfiguration`；父装配构造注入该注册表与 embedding-model 持有者，供 `controllerRegister()` 直接构造连接测试 Controller——避免在 `@Configuration` 构造器里注入自身 `@Bean` 造成的循环依赖，同时去掉 Controller 的 `SpringUtils` 服务定位。选项读取器的错误路径使用传入的完整 datasource type，即 `langchain4j-chat-model[qwen-main].options.api-key`（类型串本身已含前缀，读取器不再二次拼接）。

### D4. provider 工厂 SPI 与短名表单一事实源

每家供应商一个显式工厂类（参照 core 层 `AutoConfig` 的 builder 链写法）。工厂实现 `ModelFactory` SPI：`provider()` 返回短名，七类 `build*` 方法有默认实现抛「不支持」。短名表不单独维护常量，而是**由已注册工厂的能力探测生成**，存储校验用的短名集合与工厂注册表同源，避免「表里有短名但无工厂」的漂移。

工厂按类资源只注册其真实支持的方法；`chat-model` 与 `tokenizer` 的可用短名因此天然不同（已写入规格的场景）。

*替代方案*：短名表与工厂分别维护 → 两处不同步只能靠测试兜底，放弃。

### D5. embedding-store 与 embedding-model 的搭配

- `embedding-store` 必填 `embeddingModelKey`，注册时从 embedding-model 的持有者取实例，取不到则注册失败并提示「请先配置并启用 embedding-model: {key}」。
- 维度决策链：`options.dimension` → 被引用模型维度 → 报错。
- 不级联删除：停用/删除 embedding-model 不联动删除 store 实例；store 后续注册会按上述规则失败，由使用方显式修复依赖。
- 加载顺序兜底：启动完成钩子（`SmartLifecycle` / `ApplicationRunner`）对启动期失败的 store 统一重试一次注册，覆盖「store 事件先于 model 到达」的情形。

*替代方案*：级联删除 → 引入跨资源隐式依赖与顺序敏感的删除语义，超出当前收益，放弃。

### D6. Langchain4jModule 只读化（BREAKING）

按设计 §7 改造位于 `modules/langchain4j/core` 的脚本模块：

- `@MagicModule("uid")` → `@MagicModule("langchain4j")`；
- 删除全部 `build*` 与内部实例 map；
- `get*` 只读委托对应类型的运行时持有者，未配置时抛中文异常并列出该类型已配置 `key`；
- `remove(type, key)` 改为持有者驱逐语义（不删除已保存的数据源配置）；
- `destroy()` 清空全部持有者并关闭可关闭实例；
- `chat(...)` / `Assistant` 保持原样，模型入参由调用方从取用方法获得。

*取舍*：旧脚本中的 `langchain4j.buildXxx(...)` 会失效。选择统一实例来源而非保留双通道，避免「脚本自建 + 数据源注册」两套实例并存导致的配置不可见与内存泄漏。

### D7. 包与目录结构（相对设计文档初稿的修正）

设计初稿把所有内容放在 `modules.langchain4j.*`。按仓库既有约定修正为：

```
com.xiongx9527.fw.plus.ai.autoconfigure.magicapi
├── plugin/langchain4j/            # datasource 插件（与 mongo/kafka/redis 插件同级）
│   ├── model/                     # 7 实体 + 基类 + 7 个 MagicDynamic*DataSource
│   ├── resource/                  # 7 个 MagicResourceStorage
│   ├── registry/                  # 7 个 MagicDynamicRegistry + 抽象基类
│   ├── factory/                   # 工厂 SPI + 注册表 + 每家供应商一个工厂
│   ├── web/                       # 连接测试 Controller（构造注入依赖）、启动重试
│   ├── 7 个按类型的子 Configuration
│   ├── Langchain4jModelFactoryConfiguration（共享工厂注册表，供父装配构造注入）
│   └── Langchain4jDatasourceConfiguration（父装配，implements MagicPluginConfiguration）
└── modules/langchain4j/           # 脚本模块与工具桥
    ├── core/Langchain4jModule      # @MagicModule("langchain4j")，只读取用
    ├── tool/                       # api/function → LangChain4j ToolProvider
    └── Langchain4jConfiguration    # 模块与工具装配入口
```

依据：`plugin/**` 是 magic-api 数据源插件的既定位置（`plugin/mongo`、`plugin/kafka`、`plugin/druid`…），`modules/**` 是 `@MagicModule` 脚本模块的既定位置。两棵树分别由 `MagicAPIBaseAutoConfiguration` 的 `@Import` 装配。

### D8. 工具桥：api / function → 本地 ToolProvider

- **工具名**：`api` 为 `「HTTP 方法 + 空格 + 以斜杠开头的路径」`，`function` 为函数名；同名冲突时函数优先（函数先登记）。
- **过滤优先级**：排除分组/排除路径优先，其次包含分组/包含路径；`api` 额外有方法白名单与 opt-in。分组模式不带通配符时同时覆盖该分组与其子分组，减少写 `/**` 的负担。
- **opt-in 与入参 schema 复用 MCP 元数据**：默认选项名 `mcpTool`、默认属性名 `mcpInputSchema`，使同一处元数据同时驱动 MCP 与 LangChain4j 两侧，避免维护两份声明。
- **快照缓存 + 事件失效**：首次读取构建快照并缓存，资源变更事件到达时失效重建；刻意不用 TTL，否则会出现「刚保存的内容在 N 秒内读不到」。
- **执行语义**：参数 JSON 解析后原样透传脚本；映射/集合/数组按 JSON 序列化返回，其它按字符串；工具不存在或脚本异常返回错误文本，不把异常抛给模型调用循环，让模型自行决定重试或放弃。

### D9. 依赖与版本

- AI 层 BOM 统一管理 langchain4j 全生态供应商版本；`fw-plus-ai-boot-autoconfigure` 以 `optional` 引入全部供应商、Tokenizer 与向量库 jar（含 `bge-small` / `all-minilm` 内置向量模型）。
- 个别社区版构件若以更高 JDK 编译（如某个 1.0.0-beta 版为 Java 17），改用与 0.35.0 同代的可用实现，不因此提升本项目 Java 基线。
- core 层不新增/删除任何依赖。

### D10. 本 change 不拆分工具桥

工具桥与 datasource 插件同属「LangChain4j ↔ magic-api 集成通道」，共享同一装配入口（`MagicAPIBaseAutoConfiguration` 的 `@Import`）与同一批 MCP 元数据约定；拆成两个 change 会造成装配入口与元数据约定被两个 change 分别修改。用户已确认纳入本 change，因此不拆分。

## Risks / Trade-offs

- `options` 为自由 map，键名拼写错误只在构建期暴露 → 强类型读取工具缺失报错带完整路径（`langchain4j-chat-model[qwen-main].options.api-key`）；前端按 provider 元数据渲染表单；连接测试即时回显。
- 把共享 holder 重构为七个持有者，改动面覆盖七个注册器与脚本取用路径 → 分类型原子提交、每个类型有对应注册器测试；`Langchain4jModule` 改造与持有者重命名放在同一批提交内，避免中间态出现两套实例来源。
- 显式工厂数量大（每家一个类） → 模板化写法（参照 core 层 `AutoConfig`）+ 每家一个代表单测；短名表与工厂一一对应由单测保证。
- 全量 `optional` 依赖增大依赖树与构建时间 → 版本统一由 BOM 管理；`optional` 不向下游传递。
- embedding-store 依赖加载顺序 → 启动完成钩子统一重试（D5）。
- 工具桥错误被静默为文本可能掩盖脚本缺陷 → 错误文本包含异常摘要，便于在应用日志与模型侧同时定位。
- `@MagicModule` 更名与 `build*` 删除是破坏性变更 → 见迁移计划。

## Migration Plan

1. **脚本迁移**：把脚本中的 `langchain4j.buildXxx(key, builder)` 改为「控制台新增对应 datasource → 脚本 `langchain4j.getXxx(key)`」。受影响脚本需在升级前盘点；模块名由 `uid` 改为 `langchain4j`。
2. **配置迁移**：模型连接参数从脚本内联移到 `{type}/*.json`；样例工程 `fw-plus-ai-chat-sample` 现有静态配置路径保持不变，作为不回归验收项。
3. **部署顺序**：AI 层 BOM 与 `fw-plus-ai-boot-autoconfigure` 先发布；依赖方按 Core → Boot → Cloud / AI 顺序刷新本地依赖后升级。前端产物随 `fw-plus-ai-magicapi-plugin-langchain4j` 打包进 `target/classes/magic-editor/plugins/`。
4. **灰度与回滚**：总开关 `fw.plus.magicapi.langchain4j.enabled=false` 可整体停用插件（工具桥另有 `fw.plus.magicapi.langchain4j.tool.enabled`）；回滚为回退到升级前版本并保留 `{type}/*.json`（新一代版本可继续读取）。
5. **验收**：`fw-plus-ai-boot-autoconfigure` 单测与集成测试全绿；前端子工程 `mvn package` 产物就位；控制台七类数据源的新建/保存/停用/删除/测试全链路可用；core 层 `git diff` 为空。

## Open Questions

- 首批供应商工厂覆盖范围（当前 22 家）是否需要在本次补齐到 langchain4j 全生态？剩余供应商可在后续增量中按 D4 的 SPI 追加，不影响本设计的装配与存储结构。

## 结构变更记录（Build 期，已实施）

以下三项在 Build 期按用户确认调整，落地后与本设计其余部分一致：

1. **前端按资源类型拆为七个子工程**。`fw-plus-ai-magicapi-plugin-langchain4j` 改为 `packaging=pom` 聚合器，下挂 `fw-plus-ai-magicapi-plugin-langchain4j-{type}` 七个子工程，各自带 console 与独立前端产物 `magic-langchain4j-{type}.1.0.0.iife.js`（对齐仓库「一插件一工程」惯例，如 `fw-plus-magicapi-plugin-kafka`）。前端构建插件声明集中到聚合器（父工程默认跳过，子工程以一行属性打开）；字段元数据表按类型拆分，由后端工厂方法体解析生成，并在各子工程内做交叉校验。
2. **七类各自一个插件标识**。新增公共类 `AbstractLangchain4jPluginConfiguration`（统一插件标识、前端产物声明与「产物缺失即降级」判定、默认空 registrar），七个类型子配置继承它并声明各自的插件标识与前端产物名；连接测试 Controller（路径含 `{type}`）由 chat-model 配置统一承载，避免同一路径按类型重复注册。父配置只保留共享 bean（工厂注册表与启动重试）。
3. **依赖策略修正**。`fw-plus-ai-boot-autoconfigure` 中的 langchain4j provider/store 依赖原为 `compile + optional`（不向下游传递），按类型引入 starter 会拿不到实现类；七个子工程改为显式声明 `fw-plus-ai-langchain4j-core` 与该类型涉及的各 provider 官方模块 / core 层 wrapper。
4. **按类型独立条件与开关**。每个类型子配置各自声明 `@ConditionalOnClass`（该类型接口）与 `@ConditionalOnProperty`（`fw.plus.magicapi.langchain4j.{type}.enabled`，缺省开启）；父配置的 langchain4j 类条件收窄为只判 magic-api starter。这样「只引入部分 SDK」或「按类型停用」都只影响对应类型，与 spec 中「单类型装配失败/停用不得阻止其它类型」一致。

**8.5 验收边界**：控制台浏览器交互无法在无头环境自动化。已自动覆盖的等价链路为——后端 storage 落盘/读回/删除与校验、资源变更后的实例注册与注销（走 magic-api 事件入口 `processEvent`）、连接测试接口、各类型表单元数据与组件绑定；magic-api 的分组树管理属于引擎通用能力（由 `MagicResourceService` 及其定制实现承担），不计入本插件的验收范围。
