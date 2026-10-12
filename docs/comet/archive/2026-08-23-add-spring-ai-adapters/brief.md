# Outcome

在现有 Java 17 / Spring Boot 3.5.8 代码线上新增 `fw-plus-ai-spring` 聚合及 `fw-plus-ai-spring-adapter`，让业务通过现有中立 `AiModelRegistry`、配置前缀和 Magic-API `model[...]` 使用 Spring AI 与 Spring AI Alibaba 的同步聊天及嵌入模型，而不暴露具体 SDK 类型。

# Scope

- 新增 `fw-plus-ai-spring` 聚合与唯一子模块 `fw-plus-ai-spring-adapter`。
- 同一 adapter artifact 提供两个精确 framework：`spring-ai` 与 `spring-ai-alibaba`。
- 首版适配上游 `ChatModel` 与 `EmbeddingModel`，支持中立 `CHAT`、`EMBEDDING`。
- 通用 Spring AI provider 与 Spring AI Alibaba DashScope 共用中立 facade、usage、超时及异常映射。
- 在现有 AI Boot autoconfigure 中发现和绑定 Spring 管理的模型 Bean；业务显式引入 adapter 及官方 provider starter。
- 统一并验证 Spring AI / Spring AI Alibaba BOM，修正相关迁移文档与依赖边界测试。

# Non-goals

- 不创建 `fw-plus-ai-j17`、`fw-plus-ai-spring-alibaba` 或第二个 Alibaba adapter 模块。
- 不修改中立 `AiModel`、`AiModelAdapter`、`DefaultAiModelFactory`、registry 的公开契约。
- 不实现 `STREAMING_CHAT`、IMAGE、AUDIO、工具调用、Agent/Graph 等中立契约尚未表达的能力。
- 首版不注册 `AiEmbeddingStoreAdapter`，不把标准文本型 Spring AI `VectorStore` 伪装为预计算向量查询接口。
- 不在 adapter 或 fw-plus 中立 starter 中传递 OpenAI、DashScope 等具体 provider starter。
- 不静默回退到 LangChain4j，不把 API key、header、完整连接串或原始敏感响应写入错误。

# Acceptance examples

- A1：构建 `fw-plus-ai-spring-adapter` 时，锁定且只解析经兼容探针验证的 Spring AI `1.1.2`、Spring AI Alibaba/Extensions `1.1.2.2` 版本族，并在 Spring Boot `3.5.8` 下通过编译与测试；adapter 依赖树不包含具体 provider starter。
- A2：给定 `framework=spring-ai, provider=openai, type=CHAT` 及唯一显式 Bean binding，registry 创建中立 `AiChatModel`，聊天结果正确映射文本、finish reason、usage 和 actual model key，且 facade 不暴露 native accessor。
- A3：给定 `framework=spring-ai-alibaba, provider=dashscope, type=CHAT`，Alibaba adapter 使用与通用 Spring AI 相同的中立 chat facade；其 `getFramework()` 为 `spring-ai-alibaba`，不会被通用 adapter 或 LangChain4j adapter处理。
- A4：两个 framework 的 `EMBEDDING` 均支持批量输入，保持输入/输出顺序，校验响应数量与向量有效性，防御性复制向量，并映射 usage；未知请求参数明确失败而不是被忽略。
- A5：`properties.beanName` 精确选择 Spring 管理的模型 Bean；未指定时只接受唯一显式 framework/provider/type binding。零候选、多候选、Bean 缺失或类型不符均给出脱敏、可诊断错误，`supports` 不初始化 Bean、不发网络请求。
- A6：Spring AI、Spring AI Alibaba 与 LangChain4j 三个 `AiModelAdapter` 可同时注册，`DefaultAiModelFactory` 按显式 framework 精确选择；任一失败不回退其他框架，也不留下半初始化 registry 条目。
- A7：adapter/API、Alibaba SDK 或某一模型能力缺失时，对应 Boot auto-configuration 独立 back off，不影响其余 framework/能力；用户自定义同名 adapter Bean 可覆盖默认 Bean，`AutoConfiguration.imports` 只登记一次。
- A8：超时、中断与 transport 故障映射为中立 infrastructure failure；非法参数、provider 拒绝与畸形响应映射为 request rejected；测试断言错误、日志与 actuator 信息不泄漏敏感配置值。
- A9：首版不存在 Spring AI / Alibaba `AiEmbeddingStoreAdapter` Bean；向量能力文档明确说明预计算向量与标准文本 `VectorStore` 的语义差异，代码不 stringify、丢弃或重新计算调用方传入的向量来伪装兼容。
- A10：中立 model/store starter 的 validate/dependency tree 不传递 `org.springframework.ai:*`、`com.alibaba.cloud.ai:*` 或 `fw-plus-ai-spring-adapter`；业务按文档显式引入 adapter 与 provider starter。
- A11：迁移文档和模块树反映当前 Java 17 / Boot 3.5.8、新模块、双 framework、版本组合、能力边界与验证命令，不再把独立 `fw-plus-ai-j17` 或 Java 8/Boot 2 作为当前分支实现方案。
- A12：新 adapter、AI Boot 自动配置、现有中立模型、LangChain4j adapter 与 Magic-API model 相关回归测试全部通过；无凭据时明确记录未执行真实远端 provider smoke test。

# Constraints and invariants

- 当前实现目标分支是 `springboot3`，Java 17、Spring Boot 3.5.8。
- 使用现有中立 `fw-plus.ai.model.models`、`AiModelAutoConfiguration`、`DefaultAiModelFactory` 与 `AiModelRegistry`。
- Spring AI / Alibaba 原生类型只允许出现在 `fw-plus-ai-spring-adapter`、条件自动配置和相关测试中。
- provider client 由官方 starter 或应用配置交给 Spring 管理；adapter 不反射构造 client，不自行持有凭据。
- Bean 解析必须确定、无 I/O，真实 Bean 只在 `create` 阶段取得。
- 具体 provider starter 始终由应用显式安装，不能从中立 starter 或 adapter 传递。
- VectorStore 后续只有在 provider bridge 能完整保留预计算向量、ID、metadata、score/filter/delete 语义时才能另行启用。

# Decisions

- 使用单一聚合 `fw-plus-ai-spring` 与单一 artifact `fw-plus-ai-spring-adapter`，同时覆盖 Spring AI 和 Spring AI Alibaba。
- framework 标识分别为 `spring-ai` 和 `spring-ai-alibaba`；同一共享 support/facade 由两个独立 `AiModelAdapter` 实例暴露。
- 版本探针首选 Spring AI `1.1.2`、Spring AI Alibaba 与 extensions `1.1.2.2`，需在官方 Boot 3.5.7 对照及项目 Boot 3.5.8 中均验证后锁定。
- 首版能力限定为同步 CHAT 与 EMBEDDING，不实现 VECTOR。
- 通过显式 `properties.beanName` 或 `SpringAiProviderBinding` 绑定 Spring Bean，不从类名或“唯一 Bean”猜 provider。
- 沿用“中立 starter + 显式 adapter + 官方 provider starter”的安装方式，不新增额外 fw-plus Spring starter。
- 当前需求目标紧耦合于同一 adapter、BOM 与 Boot 装配区域，保持单一 Native change，不拆 Supervisor Change。

# Open questions

- 无。

# Verification expectations

- 先运行 BOM/SDK 兼容探针，保存 effective POM 与 dependency tree 证据，再实现正式 POM 与 API 映射。
- 新 adapter 使用 Jupiter fake `ChatModel` / `EmbeddingModel` 做双 framework 参数化契约测试。
- AI Boot 使用 `ApplicationContextRunner`、`FilteredClassLoader` 验证条件装配、Bean 绑定、三 framework 共存及失败原子性。
- 运行 Maven Enforcer、dependency tree 与 API/源码边界检查，证明中立 artifact 不传递具体框架。
- 按 Core → AI Boot 顺序运行目标模块测试及相关 LangChain4j/Magic-API 回归。
- 真实 OpenAI/DashScope smoke test 仅在存在受控凭据时执行；缺凭据不阻塞离线验收，但必须在最终报告中明确标记未执行。
