# Outcome

将 fw-plus 的完整 LangChain4j 调用链迁移到与 LangChain4j 1.18.0 对齐的高版本基线，消除 1.18.x 核心组件与 0.36.2 Provider 混用，并确保模型、向量存储、Tinyflow、AskData 和 Spring Boot 自动配置在可选依赖缺失时仍具有可预测行为。

# Scope

- 保持 `fw-plus-ai-model` 与 `fw-plus-ai-store-embedding` 的框架中立公共 API。
- 修复 `LangChain4jAdapterAutoConfiguration` 及其导入的模型、Embedding Store adapter 自动配置。
- 将生产代码从 `ChatLanguageModel` / `StreamingChatLanguageModel` 迁移到 `ChatModel` / `StreamingChatModel`。
- 迁移 Tinyflow、AskData 以及 LangChain4j Core 相关自动配置和测试。
- 将 DashScope、Qianfan、Zhipu AI、Redis 等旧 Provider 迁移到对应的 `langchain4j-community-*` 坐标和新包名。
- 统一 LangChain4j 依赖基线：稳定核心组件使用 `1.18.0`，同批次 Community/实验性组件使用 `1.18.0-beta28`。
- 增加高版本类路径、可选 Provider 缺失、依赖树一致性和 adapter 注册回归测试。

# Non-goals

- 不改变中立模型和中立向量存储 API 的业务语义。
- 不新增模型供应商或向量数据库能力。
- 不重新设计现有配置文件结构、Provider key 或默认模型/存储选择规则。
- 不修改与 AI/LangChain4j 兼容性无关的模块。
- 不处理当前工作区已有的 `.DS_Store`、`.superpowers/sdd/*`、`.vscode/settings.json` 删除。

# Acceptance examples

- A1：`fw-plus-ai-model` 与 `fw-plus-ai-store-embedding` 的编译依赖和公开签名不包含 `dev.langchain4j` 类型，现有中立 API 测试通过。
- A2：使用 LangChain4j `1.18.0` 与同批次 `1.18.0-beta28` Provider 构建相关 Core、Boot、Tinyflow 和 AskData 模块时编译成功，相关测试全部通过。
- A3：完整依赖树中不存在 `dev.langchain4j:*:0.36.2`，也不存在旧 `langchain4j-dashscope`、`langchain4j-qianfan`、`langchain4j-redis` 坐标。
- A4：`LangChain4jAdapterAutoConfiguration` 在核心 adapter 可用时只注册一个中立模型 adapter 和一个中立 Embedding Store adapter。
- A5：任一可选 Provider SDK 或实现类缺失时，Spring 上下文正常启动，仅对应 Provider bean 不注册。
- A6：Tinyflow 和 AskData 使用 `ChatModel`/`StreamingChatModel` 完成原有聊天调用、响应提取和异常映射，不再引用旧 Chat Language API。
- A7：Qwen、Qianfan、Zhipu 和 Redis 的现有 Provider key 与配置前缀保持不变，但底层实现使用对应 Community 坐标和新包名。
- A8：所有可选 LangChain4j 类型的自动配置条件均不会因类加载期直接解析缺失类型而抛出 `ClassNotFoundException` 或 `NoClassDefFoundError`。

# Constraints and invariants

- 使用 JDK 17 执行 Spring Boot 3.5.8 层验证。
- `fw-plus-ai-model` 和 `fw-plus-ai-store-embedding` 公共签名不得暴露 `dev.langchain4j` 类型。
- 可选 Provider 不得因为未安装 SDK 影响应用启动。
- 保持现有配置 key、配置前缀和已支持 Provider 的用户可见行为。
- 稳定 LangChain4j 核心版本与 Community beta 版本必须属于同一 `1.18.0` 发布批次。
- 不覆盖或提交当前工作区中与本 change 无关的删除。

# Decisions

- 使用当前 `springboot3` 分支和当前工作目录，不创建额外分支或 worktree。
- 采用完整迁移范围，包括 Adapter、Tinyflow、AskData、DashScope、Qianfan、Zhipu AI 和 Redis。
- 目标依赖基线为 LangChain4j 稳定组件 `1.18.0`，Community/实验性组件 `1.18.0-beta28`；不使用没有同批次 Community 发布的 `1.18.1` 作为正式项目基线。
- 继续采用框架中立 core + LangChain4j adapter 的分层结构，不把 LangChain4j 类型泄漏到中立模块。
- 保留既有 Provider key 与配置前缀，通过底层坐标、包名和 API 迁移实现兼容。

# Open questions

- 无。

# Verification expectations

- 使用 Maven reactor 在 `langchain4j.version=1.18.0` 下执行相关模块编译和测试。
- 执行依赖树检查，确认不存在 0.36.2 和旧 Provider 坐标。
- 执行自动配置上下文测试，覆盖完整类路径和缺失可选 Provider 两种场景。
- 执行 Tinyflow、AskData 的聊天模型契约测试。
- 执行公共 API/依赖边界测试，确认中立模块不依赖 LangChain4j。
