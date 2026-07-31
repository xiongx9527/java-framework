# TinyEngine AI Starter 业务依赖对齐设计

## 背景

`fw-plus-tiny-engine-spring-boot-starter` 已提供 TinyEngine Boot 自动配置、Web、MyBatis-Plus 和 Core 能力。`fw-plus-ai-tiny-engine-spring-boot-starter` 已依赖最新中立模型、向量存储和 LangChain4j Adapter Starter，但仍直接依赖 `fw-plus-tiny-engine-core`，导致业务只引入 AI Starter 时不会获得完整 Boot Starter 装配。

本变更将 AI Starter 调整为 TinyEngine 业务聚合入口。AI Starter 只通过 Maven 依赖引用现有 Boot Starter，不复制、不内嵌、不重新实现 Boot Starter 的代码或资源。

## 目标

- 业务只引入 `fw-plus-ai-tiny-engine-spring-boot-starter` 时，同时获得 TinyEngine Boot 基础能力和 AI 能力。
- AI Starter 继续使用 `fw-plus-ai-model-spring-boot-starter`、`fw-plus-ai-store-embedding-spring-boot-starter` 和 `fw-plus-ai-langchain4j-adapter-spring-boot-starter`。
- TinyEngine Chat、RAG 和知识库业务继续面向 `AiModelRegistry`、`AiEmbeddingStoreRegistry` 及中立门面。
- 保持 Java 8、Spring Boot 2.7.18、LangChain4j 0.35.0 和当前 `${revision}` 版本管理方式不变。

## 非目标

- 不新建或复制 `fw-plus-tiny-engine-spring-boot-starter`。
- 不让 Boot Starter 反向依赖 AI 层。
- 不修改 TinyEngine Boot 自动配置、Controller、Mapper、SQL 资源或路由。
- 不升级 AI 框架、Spring 平台、Maven Wrapper 或构建插件。
- 不重新引入旧 LangChain4j model/store 模块或旧 Starter 坐标。

## 依赖设计

目标依赖关系如下：

```text
fw-plus-ai-tiny-engine-spring-boot-starter
|- fw-plus-ai-boot-autoconfigure
|- fw-plus-tiny-engine-spring-boot-starter
|  |- fw-plus-boot-starter
|  |- fw-plus-tiny-engine-core
|  |- mybatis-plus-boot-starter
|  `- spring-boot-starter-web
|- fw-plus-ai-model-spring-boot-starter
|- fw-plus-ai-store-embedding-spring-boot-starter
`- fw-plus-ai-langchain4j-adapter-spring-boot-starter
```

AI Starter POM 将直接 `fw-plus-tiny-engine-core` 依赖替换为 `com.xiongx9527.fw.plus.boot:fw-plus-tiny-engine-spring-boot-starter`。Core 仍由 Boot Starter 传递引入，避免业务聚合层维护重复依赖路径。版本继续由现有 BOM 管理，不在 Starter 中写死版本。

## 运行时边界

- Boot Starter 负责 TinyEngine 基础自动配置和 Web/Mapper/资源装配，代码保持不变。
- AI Starter 负责聚合 Boot 与 AI 依赖；不新增 Boot 代码副本，也不通过源码 `@Import` 复制 Boot 配置。
- TinyEngine AI 自动配置继续从 `AiModelRegistry` 和 `AiEmbeddingStoreRegistry` 获取模型及向量存储。
- LangChain4j 原生类型只允许存在于 `LangChain4jTinyEngineAiProvider` 等 Adapter/Provider 内部，不进入 TinyEngine 业务接口或中立模块公开签名。
- 现有 `fail-fast` 和 `degrade` 缺失依赖策略保持不变。

## 验证设计

1. 添加 Starter POM 结构测试，断言 AI Starter 直接依赖现有 Boot Starter 和三个中立 AI Starter。
2. 断言 AI Starter 不再直接依赖 `fw-plus-tiny-engine-core`，且不包含旧 LangChain4j model/store 坐标。
3. 生成 AI Starter 依赖树，确认 Boot Starter 和 TinyEngine Core 通过预期路径解析，版本由 BOM 管理。
4. 使用仅声明 AI Starter 的测试或样例上下文，验证 Boot 与 AI 自动配置类均可从 classpath 发现。
5. 在 Java 8 下运行 TinyEngine Boot、TinyEngine AI 定向测试和相关 Maven 模块构建。

## 风险与处理

- Boot Starter 会传递引入 Web 与 MyBatis-Plus，这是用户选择的完整 TinyEngine 业务聚合语义；不将依赖标记为 optional。
- 若 AI Starter 所在父 POM 未导入 Boot BOM，必须复用仓库现有 Boot 依赖管理入口，不在子模块写死 `0.1.1-SNAPSHOT`。
- 若上下文测试因数据库基础设施缺失而失败，测试只验证自动配置发现与条件边界，使用既有测试替身，不连接外部数据库。

## 验收标准

- AI Starter POM 直接依赖现有 Boot Starter，不包含 Boot 源码或资源副本。
- AI Starter 同时依赖最新中立模型、向量存储和 LangChain4j Adapter Starter。
- Boot Starter POM 与实现无 AI 反向依赖。
- TinyEngine 业务公开接口不泄漏 LangChain4j 原生类型。
- 定向测试、依赖树检查和 Java 8 Maven 构建通过。
