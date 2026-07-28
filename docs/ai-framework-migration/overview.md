# Java 17 AI 框架迁移蓝图

## 交付边界

本目录描述从默认 LangChain4j 路径迁移到 Spring AI、Spring AI Alibaba 或 AgentScope 2 Java 的后续实施方案。本次变更只交付蓝图和可执行的文档完整性校验，不创建 `fw-plus-ai-j17` reactor、Maven Parent、starter、自动配置或真实 adapter，也不证明任何目标 SDK 已与本仓库兼容。

Spring Boot 2.7.18 可以运行在 Java 17。选择 Spring Boot 3.x 不是 Java 17 的强制要求，而是为了隔离目标 SDK 所需的高版本 Spring 生态；现有 Java 8 / Boot 2.7 / LangChain4j 默认路径不因此升级。

## 兼容矩阵

| 线路 | Java / Boot 基线 | 当前可用性 | 依赖边界 |
| --- | --- | --- | --- |
| 中立 Core 契约 | Java 8；不依赖 Spring Boot API | Java 8 中立 model/store API 由当前 change 建设 | 公开签名不得出现任一框架 SDK 类型 |
| 默认 LangChain4j | Java 8 / Boot 2.7.18，也可在 Java 17 运行 | 当前唯一完整 model/store adapter | 不传递引入 Spring AI、Spring AI Alibaba 或 AgentScope 2 Java |
| Spring AI | 计划 Java 17 / Boot 3.x | 仅有本文档蓝图，无可解析 artifact | 未来独立 reactor 只消费已发布的 Java 8 中立 artifact |
| Spring AI Alibaba | 计划 Java 17 / Boot 3.x | 仅有本文档蓝图，无可解析 artifact | SDK/BOM 仅由未来 Java 17 parent 管理 |
| AgentScope 2 Java | 计划 Java 17 / Boot 3.x | 仅有本文档蓝图，无可解析 artifact | 作为进程内 SDK 适配，不以 HTTP/MCP/A2A 桥接代替 adapter |

更详细的现状和隔离验证见 [compatibility-matrix.md](compatibility-matrix.md)。

## 未来模块布局

后续 change 应创建独立 Maven reactor；下列名称是规划坐标，不是当前可解析的 artifact：

```text
fw-plus-ai-j17/
├── pom.xml
├── fw-plus-ai-j17-parent/
├── fw-plus-ai-spring-ai-j17-starter/
├── fw-plus-ai-spring-ai-alibaba-j17-starter/
└── fw-plus-ai-agentscope-j17-starter/
```

建议沿用 AI 层 groupId `com.xiongx9527.fw.plus.ai`。独立 parent 统一设置 Java 17、Boot 3.x、插件版本和三个目标 SDK 的 dependency management；各 starter 只声明自己的 SDK 与 adapter，不进入 Core BOM，也不加入 `fw-plus-core`、`fw-plus-boot` 或现有 `fw-plus-ai` reactor。

依赖方向必须保持单向：

```text
Java 17 starter -> 目标 SDK
                -> Java 8 中立 model/store artifact

Java 8 Core/Boot -X-> Java 17 starter、Boot 3、目标 SDK
```

## 后续实现门槛

每个目标框架都必须单独完成以下门槛后才能从“蓝图”改为“支持”：

1. 固定 JDK 17 与 Boot 3.x 基线，通过 SDK 版本探针验证 BOM、编译器、字节码、`jakarta.*` 及传递依赖兼容性。
2. 让原生模型或 store 只存在于 adapter 内部，通过 `AiModelAdapter`、`AiEmbeddingStoreAdapter` 绑定中立定义。
3. adapter 显式声明经验证的 framework、provider 和能力；不支持的能力启动失败，不返回空实现，也不回退到 LangChain4j。
4. 自动配置同时检查显式 `framework`、目标 SDK class、唯一 adapter 和完整必要配置。任一条件不满足时不得注册 registry 条目或半初始化 Bean。
5. 用本地 fake 或 SDK mock 做契约测试，并为至少一个真实模型端点和一个真实向量后端提供受控集成测试；日志与异常不得泄漏密钥。
6. 验证 Java 8 Core 的 effective POM 和依赖树不包含 Boot 3 或目标 SDK，再验证 Java 17 reactor 的独立构建。

## 稳定迁移契约

- 旧前缀 `fw-plus.ai.langchain4j.model` 与 `fw-plus.ai.langchain4j.embedding-store` 在兼容期继续有效。
- 未来中立配置使用 `fw-plus.ai.model` 与 `fw-plus.ai.embedding-store`，同 key 同时出现时以中立定义优先，迁移提示不得输出 properties 中的凭证。
- 示例统一使用模型 key `knowledge-chat` 和 store key `knowledge`。切换 `framework` 不得改变 registry key、Magic-API 动态资源 key、`model['knowledge-chat']` 或 `vector['knowledge']`。
- Java 业务代码面向 `AiModelRegistry`、`AiEmbeddingStoreRegistry` 和中立数据类型；任何 Spring AI `Document`、LangChain4j `TextSegment` 或目标 SDK 对象都不得跨越 adapter 边界。

## 推荐顺序

1. 保持 LangChain4j 默认路径运行，先完成 Java 8 中立配置与 Magic-API 回归。
2. 为一个目标框架建立独立 Java 17 reactor 和 SDK 探针，锁定版本后再实现 adapter。
3. 先接 model 或 store 中一个完整能力面，通过条件装配、契约测试和真实集成测试后再扩展能力。
4. 以单个业务 key 灰度切换；验证失败时只恢复该 key 的 `framework=langchain4j` 或旧配置，不改 Magic-API key。

三个目标框架的具体计划分别见 [spring-ai-java17.md](spring-ai-java17.md)、[spring-ai-alibaba-java17.md](spring-ai-alibaba-java17.md) 和 [agentscope-java17.md](agentscope-java17.md)。
