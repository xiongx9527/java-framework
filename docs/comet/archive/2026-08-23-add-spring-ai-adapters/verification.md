---
generated_from_state_version: 16
---

# Verification

## Current result

- Result: **Passed**
- Assurance: **skill-coordinated**
- Goal cycle: 1
- Iteration: 3
- Verifier attempt: 2
- Completed: 2026-08-23T01:35:05.630Z
- Summary: 独立复核 brief/spec、当前实现与 diff、Runtime state/logs 后，A1-A51 全部有源码或 Runtime 证据支持；上一轮 A5/A28/A29 的非 eager Bean 探测、FactoryBean 生命周期和脱敏解析诊断路径已通过针对性测试及代码复核。

## Acceptance

| ID | Result | Source | Criterion | Reason |
| --- | --- | --- | --- | --- |
| A1 | passed | brief.md | A1：构建 `fw-plus-ai-spring-adapter` 时，锁定且只解析经兼容探针验证的 Spring AI `1.1.2`、Spring AI Alibaba/Extensions `1.1.2.2` 版本族，并在 Spring Boot `3.5.8` 下通过编译与测试；adapter 依赖树不包含具体 provider starter。 | adapter POM 仅依赖中立 model 与 spring-ai-model；BOM 锁定 Spring AI 1.1.2、Alibaba/Extensions 1.1.2.2，Core 与 Boot 3.5.7/3.5.8 兼容检查通过。 |
| A2 | passed | brief.md | A2：给定 `framework=spring-ai, provider=openai, type=CHAT` 及唯一显式 Bean binding，registry 创建中立 `AiChatModel`，聊天结果正确映射文本、finish reason、usage 和 actual model key，且 facade 不暴露 native accessor。 | SpringAiChatModel 将 input 转为 Prompt，并映射文本、finish reason、usage 与实际 model key；测试同时确认无 native accessor。 |
| A3 | passed | brief.md | A3：给定 `framework=spring-ai-alibaba, provider=dashscope, type=CHAT`，Alibaba adapter 使用与通用 Spring AI 相同的中立 chat facade；其 `getFramework()` 为 `spring-ai-alibaba`，不会被通用 adapter 或 LangChain4j adapter处理。 | Spring AI 与 Alibaba adapter 使用独立 framework 标识；Alibaba 仅绑定 DashScope 类型，自动配置测试验证其 facade 与 provenance。 |
| A4 | passed | brief.md | A4：两个 framework 的 `EMBEDDING` 均支持批量输入，保持输入/输出顺序，校验响应数量与向量有效性，防御性复制向量，并映射 usage；未知请求参数明确失败而不是被忽略。 | Embedding 单次批量调用，按 index 恢复顺序，校验数量和向量，复制数组并映射 usage；Alibaba 路径也有测试覆盖。 |
| A5 | passed | brief.md | A5：`properties.beanName` 精确选择 Spring 管理的模型 Bean；未指定时只接受唯一显式 framework/provider/type binding。零候选、多候选、Bean 缺失或类型不符均给出脱敏、可诊断错误，`supports` 不初始化 Bean、不发网络请求。 | Bean catalog 使用 allowEagerInit=false 非 eager 快照，create 才 getBean；显式 Bean、binding、类型和冲突路径均确定性校验且自动配置测试通过。 |
| A6 | passed | brief.md | A6：Spring AI、Spring AI Alibaba 与 LangChain4j 三个 `AiModelAdapter` 可同时注册，`DefaultAiModelFactory` 按显式 framework 精确选择；任一失败不回退其他框架，也不留下半初始化 registry 条目。 | 三个 framework 可同时登记；DefaultAiModelFactory 按显式 framework 精确选择且不回退，registry 仅在全部 definition 成功后发布。 |
| A7 | passed | brief.md | A7：adapter/API、Alibaba SDK 或某一模型能力缺失时，对应 Boot auto-configuration 独立 back off，不影响其余 framework/能力；用户自定义同名 adapter Bean 可覆盖默认 Bean，`AutoConfiguration.imports` 只登记一次。 | 总 adapter、通用 Chat/Embedding API、Alibaba SDK 与能力条件彼此隔离，默认 Bean 按名称 back off；缺类与覆盖测试通过。 |
| A8 | passed | brief.md | A8：超时、中断与 transport 故障映射为中立 infrastructure failure；非法参数、provider 拒绝与畸形响应映射为 request rejected；测试断言错误、日志与 actuator 信息不泄漏敏感配置值。 | timeout、interrupt、transport 映射 infrastructure failure，参数/provider/畸形响应映射 request rejected；中断状态和脱敏均有测试。 |
| A9 | passed | brief.md | A9：首版不存在 Spring AI / Alibaba `AiEmbeddingStoreAdapter` Bean；向量能力文档明确说明预计算向量与标准文本 `VectorStore` 的语义差异，代码不 stringify、丢弃或重新计算调用方传入的向量来伪装兼容。 | 源码不存在 Spring AI/Alibaba AiEmbeddingStoreAdapter 或 VectorStore bridge，文档明确预计算向量与文本 VectorStore 的语义边界。 |
| A10 | passed | brief.md | A10：中立 model/store starter 的 validate/dependency tree 不传递 `org.springframework.ai:*`、`com.alibaba.cloud.ai:*` 或 `fw-plus-ai-spring-adapter`；业务按文档显式引入 adapter 与 provider starter。 | 两个中立 starter 的 Enforcer 禁止 Spring AI、Alibaba 和 adapter 传递依赖；Runtime 边界检查通过。 |
| A11 | passed | brief.md | A11：迁移文档和模块树反映当前 Java 17 / Boot 3.5.8、新模块、双 framework、版本组合、能力边界与验证命令，不再把独立 `fw-plus-ai-j17` 或 Java 8/Boot 2 作为当前分支实现方案。 | 迁移文档已反映 Java 17/Boot 3.5.8、双 framework、版本和能力边界；文档校验与 diff check 通过。 |
| A12 | passed | brief.md | A12：新 adapter、AI Boot 自动配置、现有中立模型、LangChain4j adapter 与 Magic-API model 相关回归测试全部通过；无凭据时明确记录未执行真实远端 provider smoke test。 | Core 的 LangChain4j 40 项、Spring adapter 17 项及 AI Boot 680 项测试全部通过；无凭据限制已记录。 |
| A13 | passed | specs/spring-ai-model-adapters/spec.md | 系统在 Java 17 / Spring Boot 3.5.8 基线上，通过一个 `fw-plus-ai-spring-adapter` artifact 将 Spring AI 与 Spring AI Alibaba 模型接入现有中立模型体系。业务配置、registry、Magic-API 和其他下游只依赖中立类型，具体 SDK 类型与 provider 生命周期留在 adapter/Boot 装配边界内。 | 单一 spring adapter artifact 接入中立体系，provider 生命周期停留在 Boot Bean catalog/adapter 边界。 |
| A14 | passed | specs/spring-ai-model-adapters/spec.md | Core AI 聚合包含 `fw-plus-ai-spring`，其唯一子模块是 `fw-plus-ai-spring-adapter`。 | Core AI 聚合新增 fw-plus-ai-spring，且其聚合 POM 唯一子模块为 fw-plus-ai-spring-adapter。 |
| A15 | passed | specs/spring-ai-model-adapters/spec.md | adapter 编译依赖中立 model 契约和 Spring AI 公共模型 API，不传递 OpenAI、DashScope 或其他具体 provider starter。 | adapter 仅声明中立 model 和 Spring AI 公共 spring-ai-model，不包含具体 provider starter。 |
| A16 | passed | specs/spring-ai-model-adapters/spec.md | Spring AI `1.1.2`、Spring AI Alibaba BOM 与 extensions `1.1.2.2` 只有在 Boot 3.5.7 官方对照和项目 Boot 3.5.8 探针均通过后才正式锁定；两个独立 reactor 解析到相同版本。 | BOM 版本分别锁定为 1.1.2 与 1.1.2.2，Boot 3.5.7/3.5.8 两组解析和编译探针均成功。 |
| A17 | passed | specs/spring-ai-model-adapters/spec.md | 中立 model/store starter 不传递 Spring AI、Spring AI Alibaba 或新 adapter；应用显式安装 adapter 与所需官方 provider starter。 | 中立 starter 不传递 Spring AI、Alibaba 或新 adapter；安装文档要求业务显式引入 adapter 和 provider starter。 |
| A18 | passed | specs/spring-ai-model-adapters/spec.md | `spring-ai` 表示使用 Spring AI 通用 provider。 | SpringAiModelAdapter 的 framework 常量固定为 spring-ai，并由独立自动配置 Bean 暴露。 |
| A19 | passed | specs/spring-ai-model-adapters/spec.md | `spring-ai-alibaba` 表示使用 Spring AI Alibaba provider，首批 provider 为 `dashscope`。 | Alibaba adapter 的 framework 固定为 spring-ai-alibaba，首批 provider 校验固定识别 dashscope。 |
| A20 | passed | specs/spring-ai-model-adapters/spec.md | 两个 framework 各由独立 `AiModelAdapter` 实例暴露并由 `DefaultAiModelFactory` 精确匹配；它们共享同一模型 facade、usage、timeout 与异常映射实现。 | 两个 adapter 为独立实例并共享 support、facade、usage、timeout 和异常映射实现，精确区分 framework。 |
| A21 | passed | specs/spring-ai-model-adapters/spec.md | 首版支持同步 `CHAT` 与 `EMBEDDING`。 | 共享 support 仅实现同步 CHAT 与 EMBEDDING，两类模型均有离线契约测试。 |
| A22 | passed | specs/spring-ai-model-adapters/spec.md | 其他 `AiModelType` 均明确不支持，不被降级或伪装成已支持类型。 | supportedType 仅接受 CHAT/EMBEDDING，其他类型 supports=false 且 create 明确失败。 |
| A23 | passed | specs/spring-ai-model-adapters/spec.md | 首版不提供 Spring AI / Alibaba `AiEmbeddingStoreAdapter`。标准文本型 `VectorStore` 不能丢弃、字符串化或重新计算中立调用方提供的预计算向量来伪装兼容。 | 不存在 embedding-store adapter，文档明确禁止把文本 VectorStore 伪装为预计算向量接口。 |
| A24 | passed | specs/spring-ai-model-adapters/spec.md | provider 模型由 Spring 管理，adapter 不反射创建 provider client，不自行读取或持有凭据。 | provider 由 Spring BeanFactory 管理且仅在 create 获取；adapter 不构造 client、不读取或持有凭据。 |
| A25 | passed | specs/spring-ai-model-adapters/spec.md | definition 包含 `properties.beanName` 时按名称精确选择 Bean，并验证其类型与 `CHAT`/`EMBEDDING` 一致。 | properties.beanName 被精确读取，并按 CHAT/EMBEDDING 类型验证后通过 getBean(name,type) 获取。 |
| A26 | passed | specs/spring-ai-model-adapters/spec.md | 未指定 Bean 名时，必须有唯一的显式 binding，binding 至少包含 framework、provider、type、beanName。 | binding 强制 framework/provider/type/beanName 完整；未指定 beanName 时只接受唯一匹配 binding。 |
| A27 | passed | specs/spring-ai-model-adapters/spec.md | 不允许从实现类名、包名或“当前只有一个 Bean”推断 provider。 | 解析只依赖显式 framework/provider/type 与 binding 或 beanName，不从类名、包名或单 Bean 推断 provider。 |
| A28 | passed | specs/spring-ai-model-adapters/spec.md | `supports` 只检查候选名称与 binding，不实例化 Bean、不调用模型、不进行网络 I/O；`create` 才获取并包装 Spring 管理的 Bean。 | supports 只读快照和 binding，不 getBean 或调用模型；lazy FactoryBean 测试证明 context/supports 不实例化、create 才实例化。 |
| A29 | passed | specs/spring-ai-model-adapters/spec.md | 零候选、多候选、Bean 缺失、类型不符或 binding 冲突均失败；诊断只包含 key/framework/provider/type、脱敏 Bean 名与候选数量。 | 缺失、类型不符和冲突均失败；诊断仅含 key/framework/provider/type、候选数及排序后的 12 位 SHA-256 beanRef，不含原 Bean 名。 |
| A30 | passed | specs/spring-ai-model-adapters/spec.md | `spring-ai`、`spring-ai-alibaba`、`langchain4j` 可以同时注册。显式 framework 失败时不得回退其他 adapter。 | DefaultAiModelFactory 仅调用显式 framework 对应 adapter，三个 framework 可共存且失败不跨 framework 回退。 |
| A31 | passed | specs/spring-ai-model-adapters/spec.md | 中立 `AiChatRequest.input` 转换为 Spring AI `Prompt`，同步调用 `ChatModel`。 | SpringAiChatModel 使用 new Prompt(request.getInput()) 并同步调用 ChatModel.call。 |
| A32 | passed | specs/spring-ai-model-adapters/spec.md | 成功结果映射文本、finish reason、usage 与实际中立 model key。 | Chat 成功结果映射文本、finish reason、usage 与定义中的实际 model key，测试逐字段断言。 |
| A33 | passed | specs/spring-ai-model-adapters/spec.md | usage 字段缺失时返回中立 unknown usage，不因统计缺失使成功响应失败。 | usage 或 token 字段缺失时返回 AiModelUsage.unknown，不影响成功响应。 |
| A34 | passed | specs/spring-ai-model-adapters/spec.md | 首版仅接受已明确白名单并可无损映射的通用 request parameters；其他参数必须在调用前明确拒绝。 | Chat 与 Embedding 均在 provider 调用前拒绝未白名单 request parameters，测试确认未调用 SDK。 |
| A35 | passed | specs/spring-ai-model-adapters/spec.md | null/空/畸形 response、结果类型错误和 provider 拒绝必须映射为中立 request rejected。 | Chat 和 Embedding 对 null、空、结构错误、数量/index/向量错误均明确映射为 request rejected。 |
| A36 | passed | specs/spring-ai-model-adapters/spec.md | timeout、interrupt、连接或 transport 故障映射为 infrastructure failure，并保留线程中断状态。 | Future timeout 会 cancel，interrupt 会恢复线程中断状态，IOException/transport 映射 infrastructure failure，测试覆盖完整。 |
| A37 | passed | specs/spring-ai-model-adapters/spec.md | 批量输入一次交给 Spring AI `EmbeddingModel` 的可返回 metadata API。 | Embedding 一次构造 EmbeddingRequest 并调用可返回 metadata 的 EmbeddingModel.call，无逐条 provider 调用。 |
| A38 | passed | specs/spring-ai-model-adapters/spec.md | 输出数量和顺序必须与输入一致；null、空向量或数量不一致均失败。 | Embedding 校验数量，按 index 恢复顺序，并拒绝 null、空数组、重复/越界 index 和缺失位置。 |
| A39 | passed | specs/spring-ai-model-adapters/spec.md | 返回给中立层的每个 `float[]` 都进行防御性复制。 | provider 输出数组在模型和中立结果边界均防御性复制；变更原数组不影响返回值。 |
| A40 | passed | specs/spring-ai-model-adapters/spec.md | usage 映射规则与 Chat 一致，缺失时使用 unknown。 | Embedding usage 与 Chat 同规则映射，metadata/usage/token 缺失时返回 unknown。 |
| A41 | passed | specs/spring-ai-model-adapters/spec.md | 未明确映射的 per-request parameters 必须拒绝；timeout/transport 与 provider 拒绝按 Chat 相同类别处理。 | Embedding 在调用前拒绝未映射参数，并复用 Chat 相同的 timeout、transport 和 provider 异常分类。 |
| A42 | passed | specs/spring-ai-model-adapters/spec.md | 现有 AI Boot autoconfigure 通过 Boot 3 `AutoConfiguration.imports` 注册 Spring adapter 顶层配置。 | Boot 3 AutoConfiguration.imports 仅登记一次 SpringAiAdapterAutoConfiguration。 |
| A43 | passed | specs/spring-ai-model-adapters/spec.md | adapter API、通用 Spring AI API、Alibaba SDK、Chat 与 Embedding 的 class 条件相互隔离；缺一个能力不影响其他能力或 framework。 | 通用 API、Alibaba API/SDK、Chat 和 Embedding 使用独立 class 条件；FilteredClassLoader 测试证明能力隔离。 |
| A44 | passed | specs/spring-ai-model-adapters/spec.md | 总开关为 `fw.plus.ai.spring.adapter.enabled`；Alibaba 子能力另受 `fw.plus.ai.spring.alibaba.enabled` 控制。 | 总开关为 fw.plus.ai.spring.adapter.enabled，Alibaba 子开关为 fw.plus.ai.spring.alibaba.enabled。 |
| A45 | passed | specs/spring-ai-model-adapters/spec.md | 默认 Bean 按名称 back off，不能因已有任意 `AiModelAdapter`（如 LangChain4j）而整体 back off。 | 两个默认 adapter Bean 分别按名称 ConditionalOnMissingBean；已有 LangChain4j 或同名自定义 Bean 不会错误整体 back off。 |
| A46 | passed | specs/spring-ai-model-adapters/spec.md | 错误配置不得留下 registry 条目或半初始化模型。 | 全部 definition 创建成功后才发布 DefaultAiModelRegistry，错误配置不会留下部分 registry。 |
| A47 | passed | specs/spring-ai-model-adapters/spec.md | 应用继续使用现有 `fw-plus.ai.model.models`；不新增平行配置前缀或 registry。 | 继续使用 fw.plus.ai.model.models，Spring adapter 未新增平行配置前缀或 registry。 |
| A48 | passed | specs/spring-ai-model-adapters/spec.md | 中立 facade 不提供 Spring AI 或 Alibaba native accessor。 | 公开 facade 仅暴露中立契约，native delegate 为私有字段；反射测试确认无 native accessor。 |
| A49 | passed | specs/spring-ai-model-adapters/spec.md | 异常、日志、actuator 与验证报告不得出现 API key、Authorization/header、完整连接串、properties 值或原始敏感响应体。 | 异常只保留脱敏上下文并替换上游敏感消息；测试确认 API key、Authorization 等不出现在异常链。 |
| A50 | passed | specs/spring-ai-model-adapters/spec.md | 错误保留可诊断的 key/framework/provider/type、原因类别和脱敏候选信息。 | 错误保留 key/framework/provider/type、原因类别、候选数和排序后的脱敏 beanRef，相关缺失与冲突测试通过。 |
| A51 | passed | specs/spring-ai-model-adapters/spec.md | 没有受控凭据时，远端 provider smoke test 可不执行，但最终验收必须明确标记；离线契约、自动配置和依赖边界测试仍必须通过。 | 离线契约、自动配置、依赖边界、兼容探针和回归测试均通过；未提供受控 OpenAI/DashScope 凭据，真实远端 smoke test 未执行，符合允许边界。 |

## Checks

| Check | Command | Working directory | Status | Exit | Duration |
| --- | --- | --- | --- | ---: | ---: |
| Core LangChain4j and Spring adapter tests | JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-adapter,fw-plus-project/fw-plus-ai/fw-plus-ai-spring/fw-plus-ai-spring-adapter -am test | . | passed | 0 | 12708 ms |
| AI Boot autoconfigure full tests | JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am test | . | passed | 0 | 48515 ms |
| Neutral starter Maven Enforcer boundaries | JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter,fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-store-embedding-spring-boot-starter -am validate | . | passed | 0 | 2839 ms |
| Spring AI 3.5.7 and 3.5.8 compatibility probe | JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home zsh docs/ai-framework-migration/probe-spring-ai-compatibility.sh | . | passed | 0 | 10232 ms |
| Migration docs and diff format checks | -lc zsh docs/ai-framework-migration/verify-migration-docs.sh && git diff --check | . | passed | 0 | 1336 ms |

## Blockers

_None._

## Risks and skipped work

- 未提供受控 OpenAI/DashScope 凭据，真实远端 provider smoke test 尚未执行。
- 第三方 SDK 若完全忽略 interrupt，Future.cancel(true) 不能强制终止其内部调用；现有实现限制最多 8 个 worker，生产环境仍需配置 provider 网络超时。
- Magic-API 通用资源权限、configprops 暴露和 key 控制字符治理属于既有插件范围，不是本 change 的阻断项。

## Previous iterations

| Goal cycle | Iteration | Attempt | Outcome | Unresolved | Summary | Completed |
| ---: | ---: | ---: | --- | --- | --- | --- |
| 1 | 1 | 1 | fail | A5, A28, A29 | 独立验收：48 项通过，A5/A28/A29 未通过；需修复非 eager Bean 类型探测与错误诊断后重新提交候选。 | 2026-08-22T10:49:22.351Z |
| 1 | 2 | 1 | recovery | — | Iteration 2 Runtime 检查后补齐了 Verifier 复核发现的 binding 缺失/冲突安全诊断路径，候选快照已变化；保持确认需求不变，返回 Build 提交 iteration 3。 | 2026-08-22T12:49:32.667Z |
| 1 | 3 | 1 | execution-error | — | Verifier 会话在跨会话恢复前中断，旧任务没有可接收的返回通道，无法可信判断其是否完成；候选代码未修改，请由 Runtime 从稳定边界恢复并决定是否重试。 | 2026-08-23T01:20:52.245Z |
| 1 | 3 | 2 | pass | — | 独立复核 brief/spec、当前实现与 diff、Runtime state/logs 后，A1-A51 全部有源码或 Runtime 证据支持；上一轮 A5/A28/A29 的非 eager Bean 探测、FactoryBean 生命周期和脱敏解析诊断路径已通过针对性测试及代码复核。 | 2026-08-23T01:35:05.630Z |

## Conclusion

独立复核 brief/spec、当前实现与 diff、Runtime state/logs 后，A1-A51 全部有源码或 Runtime 证据支持；上一轮 A5/A28/A29 的非 eager Bean 探测、FactoryBean 生命周期和脱敏解析诊断路径已通过针对性测试及代码复核。
