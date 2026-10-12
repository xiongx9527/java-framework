# Spring AI 模型适配能力规格

## 目标

系统在 Java 17 / Spring Boot 3.5.8 基线上，通过一个 `fw-plus-ai-spring-adapter` artifact 将 Spring AI 与 Spring AI Alibaba 模型接入现有中立模型体系。业务配置、registry、Magic-API 和其他下游只依赖中立类型，具体 SDK 类型与 provider 生命周期留在 adapter/Boot 装配边界内。

## 模块与依赖

1. Core AI 聚合包含 `fw-plus-ai-spring`，其唯一子模块是 `fw-plus-ai-spring-adapter`。
2. adapter 编译依赖中立 model 契约和 Spring AI 公共模型 API，不传递 OpenAI、DashScope 或其他具体 provider starter。
3. Spring AI `1.1.2`、Spring AI Alibaba BOM 与 extensions `1.1.2.2` 只有在 Boot 3.5.7 官方对照和项目 Boot 3.5.8 探针均通过后才正式锁定；两个独立 reactor 解析到相同版本。
4. 中立 model/store starter 不传递 Spring AI、Spring AI Alibaba 或新 adapter；应用显式安装 adapter 与所需官方 provider starter。

## Framework 与支持能力

1. `spring-ai` 表示使用 Spring AI 通用 provider。
2. `spring-ai-alibaba` 表示使用 Spring AI Alibaba provider，首批 provider 为 `dashscope`。
3. 两个 framework 各由独立 `AiModelAdapter` 实例暴露并由 `DefaultAiModelFactory` 精确匹配；它们共享同一模型 facade、usage、timeout 与异常映射实现。
4. 首版支持同步 `CHAT` 与 `EMBEDDING`。
5. 其他 `AiModelType` 均明确不支持，不被降级或伪装成已支持类型。
6. 首版不提供 Spring AI / Alibaba `AiEmbeddingStoreAdapter`。标准文本型 `VectorStore` 不能丢弃、字符串化或重新计算中立调用方提供的预计算向量来伪装兼容。

## Provider Bean 绑定

1. provider 模型由 Spring 管理，adapter 不反射创建 provider client，不自行读取或持有凭据。
2. definition 包含 `properties.beanName` 时按名称精确选择 Bean，并验证其类型与 `CHAT`/`EMBEDDING` 一致。
3. 未指定 Bean 名时，必须有唯一的显式 binding，binding 至少包含 framework、provider、type、beanName。
4. 不允许从实现类名、包名或“当前只有一个 Bean”推断 provider。
5. `supports` 只检查候选名称与 binding，不实例化 Bean、不调用模型、不进行网络 I/O；`create` 才获取并包装 Spring 管理的 Bean。
6. 零候选、多候选、Bean 缺失、类型不符或 binding 冲突均失败；诊断只包含 key/framework/provider/type、脱敏 Bean 名与候选数量。
7. `spring-ai`、`spring-ai-alibaba`、`langchain4j` 可以同时注册。显式 framework 失败时不得回退其他 adapter。

## Chat 行为

1. 中立 `AiChatRequest.input` 转换为 Spring AI `Prompt`，同步调用 `ChatModel`。
2. 成功结果映射文本、finish reason、usage 与实际中立 model key。
3. usage 字段缺失时返回中立 unknown usage，不因统计缺失使成功响应失败。
4. 首版仅接受已明确白名单并可无损映射的通用 request parameters；其他参数必须在调用前明确拒绝。
5. null/空/畸形 response、结果类型错误和 provider 拒绝必须映射为中立 request rejected。
6. timeout、interrupt、连接或 transport 故障映射为 infrastructure failure，并保留线程中断状态。

## Embedding 行为

1. 批量输入一次交给 Spring AI `EmbeddingModel` 的可返回 metadata API。
2. 输出数量和顺序必须与输入一致；null、空向量或数量不一致均失败。
3. 返回给中立层的每个 `float[]` 都进行防御性复制。
4. usage 映射规则与 Chat 一致，缺失时使用 unknown。
5. 未明确映射的 per-request parameters 必须拒绝；timeout/transport 与 provider 拒绝按 Chat 相同类别处理。

## 自动配置行为

1. 现有 AI Boot autoconfigure 通过 Boot 3 `AutoConfiguration.imports` 注册 Spring adapter 顶层配置。
2. adapter API、通用 Spring AI API、Alibaba SDK、Chat 与 Embedding 的 class 条件相互隔离；缺一个能力不影响其他能力或 framework。
3. 总开关为 `fw.plus.ai.spring.adapter.enabled`；Alibaba 子能力另受 `fw.plus.ai.spring.alibaba.enabled` 控制。
4. 默认 Bean 按名称 back off，不能因已有任意 `AiModelAdapter`（如 LangChain4j）而整体 back off。
5. 错误配置不得留下 registry 条目或半初始化模型。
6. 应用继续使用现有 `fw-plus.ai.model.models`；不新增平行配置前缀或 registry。

## 安全与可观察性

1. 中立 facade 不提供 Spring AI 或 Alibaba native accessor。
2. 异常、日志、actuator 与验证报告不得出现 API key、Authorization/header、完整连接串、properties 值或原始敏感响应体。
3. 错误保留可诊断的 key/framework/provider/type、原因类别和脱敏候选信息。
4. 没有受控凭据时，远端 provider smoke test 可不执行，但最终验收必须明确标记；离线契约、自动配置和依赖边界测试仍必须通过。
