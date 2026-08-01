# Brainstorm Summary

- Change: neutral-ai-adapter-autodiscovery
- Date: 2026-07-29

## 确认的技术方案

- Java 8 / Spring Boot 2.7 基线只保留中立 `fw-plus-ai-model` 与 `fw-plus-ai-store-embedding` 契约；其中立 starter、公共自动配置和 Magic-API 不得出现任何具体 AI framework 痕迹。
- 删除 `fw-plus-ai-langchain4j-model`、`fw-plus-ai-langchain4j-store-embeding` 及两个对应 starter；新增统一 `fw-plus-ai-langchain4j-adapter`，LangChain4j 是仅安装一个 adapter 时的默认实现。
- 选择器规则固定为：显式 framework 精确匹配；未显式 framework 时唯一候选自动选择；零或多候选失败。
- Magic-API 只经由中立 registry 和门面访问 model/vector；旧 `fw-plus.ai.langchain4j.*` 配置不兼容并删除。
- Spring AI、Spring AI Alibaba 和 AgentScope 2.0 Java 本次不实现；未来单独采用 Java 17 / Spring Boot 3.x reactor，并各自提供同一中立 SPI 的 adapter 与可选 Spring Boot starter。AgentScope 2.0 为进程内 Java SDK。

## 方案结论

- 方案 A（已确认）：`fw-plus-ai-langchain4j-adapter` 加独立 adapter Spring Boot starter，框架专属自动配置与 provider 构造只在该入口中存在。中立 starter 无传递 LangChain4j 依赖；starter 是 Spring Boot 集成实现，不是 adapter 的唯一装配形式，未来 Java 17 adapter 可复制该边界。
- 方案 B：adapter 代码独立，但将其自动配置继续放进当前公共 AI Boot 自动配置模块。模块 POM 和条件配置仍会带 LangChain4j 痕迹，不能满足严格中立边界。
- 方案 C：仅发布 core adapter，以 ServiceLoader 手工注册，不提供 Spring Boot 入口。边界最干净，但与现有 Boot 自动配置和使用体验不一致。

已确认：采用方案 A；`fw-plus-ai-langchain4j-adapter-spring-boot-starter` 是 LangChain4j adapter 的一种 Spring Boot 集成实现，并替代旧两个 LangChain4j model/store starter。

## 关键取舍与风险

- 旧 artifact 与旧配置为显式破坏性变更；升级必须替换坐标和配置，不能新旧混装。
- 多 adapter 会使历史缺省 framework 配置失败；这是消除静默框架选择的必要代价，错误必须列候选并指向显式 framework。
- 统一 adapter 迁移可能遗漏 provider 能力；用现有 model/store、Magic-API 与 AskData 回归测试锁定。

## 测试策略

- 先用 RED 测试覆盖模型与 store 的显式匹配、唯一候选、零候选、多候选和异常上下文。
- 对中立模块执行 POM、导入、公开签名和配置前缀静态检查；Magic-API 集成测试只注入中立 registry 假实现。
- 完成后运行 Core 目标 reactor、AI Boot 自动配置、Magic-API/AskData 目标测试、严格 OpenSpec 校验和 Maven 依赖树检查。

## Spec Patch

无。现有 OpenSpec delta specs 已覆盖候选选择、零框架边界、破坏性迁移和 Java 17 后续边界。
