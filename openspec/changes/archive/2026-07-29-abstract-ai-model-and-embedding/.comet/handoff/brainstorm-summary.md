# Brainstorm Summary

- Change: abstract-ai-model-and-embedding
- Date: 2026-07-27

## 已确认的事实

- `fw-plus-ai-langchain4j-model` 与 `fw-plus-ai-langchain4j-store-embeding` 必须迁出 LangChain4j 聚合，成为框架无关能力；LangChain4j 保持默认完整实现。
- Spring AI、Spring AI Alibaba 与 AgentScope 2 Java 均需通过统一 adapter 接入；AgentScope 2 Java 按进程内 SDK 而非外部运行时桥接。
- 必须保留旧 LangChain4j 配置、资源 key 与 Magic-API `model[...]`/`vector[...]` 访问兼容，并提供清晰迁移文档和测试。
- 当前项目的 Java 8、Spring Boot 2.7.18 基线与目标 SDK 的实际兼容性尚未通过有效 POM、依赖树和编译探针验证。

## 确认的技术决策

- 中立核心和默认 LangChain4j 路径继续保持 Java 8 / Spring Boot 2.7。
- 不兼容的 Spring AI、Spring AI Alibaba、AgentScope 2 Java adapter 采用独立 Java 17+ / Spring Boot 3+ 适配线。
- Java 17+ 适配线使用同一仓库中的独立 Maven artifact、Java 17 / Spring Boot 3+ parent 与 starter；它们共享中立 API，但不进入 Java 8 默认依赖图。
- Java 17+ 首个版本中，Spring AI、Spring AI Alibaba 与 AgentScope 2 Java 都只交付模块骨架、starter 边界、迁移文档和可验证条件装配；不承诺原生 model/store adapter 实现。
- 迁移文档必须清楚说明不兼容原因、Java 17 对接依赖、配置、API、测试和回滚方式。

## 候选方案与取舍

- 方案 A（采用）：Java 8 中立核心与 LangChain4j 完整 adapter 保持主发布线；同仓 Java 17+ parent 下提供三个独立 adapter artifact 骨架。优点是默认用户零升级影响，未来实现不需重做模块边界；代价是首版不能切换到真实 Java 17 SDK。
- 方案 B：立即实现三个 Java 17 adapter。优点是一次性交付运行能力；代价是 SDK 兼容性、测试矩阵和实现范围显著扩大，且与已确认首版范围不符。
- 方案 C：仅保留中立 SPI，不创建 Java 17 artifact。优点是改动最小；代价是 Java 17 接入方式仍不清楚，也无法验证 starter/BOM/文档对接。

## Spec Patch

- 已确认回写 `ai-framework-adapter-integration`：将 Java 17 首版的三个目标框架从“可运行 model/store adapter”调整为“独立 artifact、可验证 starter 骨架和明确不支持诊断”；LangChain4j 保持唯一完整默认 adapter。
- 已确认回写 `ai-abstraction-contract-testing`：将 Java 17 首版测试限定为模块编译、条件装配、依赖隔离和迁移文档示例校验；真实 SDK adapter 契约测试移至后续实现 change。

## 文档交付要求

- 提供兼容矩阵，明确 Java 8 / Boot 2.7 的中立核心和 LangChain4j 默认实现，与 Java 17+ / Boot 3+ 三个目标框架骨架之间的边界。
- 按 Spring AI、Spring AI Alibaba、AgentScope 2 Java 分别说明 Maven artifact/BOM、Java 17 parent、Boot 3 starter、配置、Java API、Magic-API、验证命令和回滚。
- 明确首版 Java 17 artifact 仅完成模块与装配骨架，不能被描述为已提供可运行 model/store adapter；对未来真实 adapter 的接入步骤给出占位但可执行的说明。
