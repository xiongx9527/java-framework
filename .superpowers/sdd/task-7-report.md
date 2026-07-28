# Task 7 执行报告：Java 17 / Boot 3.x AI 框架迁移文档

## 交付范围

- 新增 Java 17 AI 框架迁移总览、Spring AI、Spring AI Alibaba、AgentScope 2 Java 三份独立迁移指南。
- 修订兼容矩阵，明确 Task 6/7 只交付后续实施蓝图，当前没有 `fw-plus-ai-j17`、Maven parent、starter、自动配置或真实 adapter。
- 新增可执行文档校验脚本，检查三份框架文档的固定章节、当前发布边界、Java 8 隔离、SDK 探针、条件装配和稳定 Magic-API key。
- 未创建或修改任何 POM、Java 源码、starter、自动配置、Core/Boot/LangChain4j 模块、计划、OpenSpec 规格或进度文件。

## RED / GREEN 证据

先创建 `docs/ai-framework-migration/verify-migration-docs.sh`，在目标文档不存在时运行：

```text
缺失文档: overview.md
缺失文档: spring-ai-java17.md
缺失文档: spring-ai-alibaba-java17.md
缺失文档: agentscope-java17.md
迁移文档校验失败: 4 项
```

命令退出码为 `1`，失败原因与预期一致。补齐文档后再次运行：

```text
迁移文档校验通过: 3 份目标框架文档
```

命令退出码为 `0`。

## 蓝图要点

- Spring Boot 2.7 可以运行在 Java 17；选择 Boot 3.x 只为隔离目标 SDK 的高版本 Spring 生态。
- 未来 `fw-plus-ai-j17` 使用独立 reactor/parent 管理 Java 17、Boot 3.x 和目标 SDK，不进入 Java 8 reactor/Core BOM。
- 目标 adapter 绑定 Java 8 中立 `AiModelAdapter`、`AiEmbeddingStoreAdapter` SPI，SDK 类型不跨越中立 API 边界。
- 自动配置必须同时校验显式 framework、SDK class、唯一 adapter、provider 能力和必要配置；失败时不注册半初始化 Bean，也不回退到 LangChain4j。
- 每个框架先完成 SDK 版本探针，再运行条件装配、契约、真实模型/向量后端、脱敏和 Java 8 依赖隔离测试。
- 模型 key `knowledge-chat`、store key `knowledge`、`model[...]`、`vector[...]` 和动态资源 key 在迁移与回滚时保持不变。
- AgentScope 2 Java 按进程内 SDK 适配；未证明向量 store 能力前不得注册对应 `AiEmbeddingStoreAdapter`，HTTP/MCP/A2A 桥接不能替代 SDK adapter。

## 验证命令

```bash
zsh -n docs/ai-framework-migration/verify-migration-docs.sh
zsh docs/ai-framework-migration/verify-migration-docs.sh
git diff --check -- docs/ai-framework-migration .superpowers/sdd/task-7-report.md
```

Java 17 reactor 构建命令只作为未来验收模板，当前因相关 POM/artifact 不存在而未运行，也未据此声明兼容性。
