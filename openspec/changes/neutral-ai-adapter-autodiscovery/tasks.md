## 1. 基线与回归测试准备

- [ ] 1.1 盘点四个旧 artifact 在 Core/AI BOM、聚合 POM、starter、Magic-API、AskData、样例和文档中的引用，建立删除清单。
- [x] 1.2 为模型和向量 store adapter 选择器编写 RED 单元测试：显式 framework 匹配、唯一候选、零候选和多候选。
- [ ] 1.3 为中立 starter/公共自动配置/Magic-API 的零具体框架依赖、导入、公开签名和配置前缀建立静态回归测试。

## 2. 中立 SPI 与选择器

- [x] 2.1 扩展 `AiModelAdapter` 与 `AiEmbeddingStoreAdapter` 的 framework/能力声明，并保持 Java 8 API。
- [x] 2.2 实现中立模型 adapter 选择与受控异常上下文，令缺省 framework 仅接受唯一候选。
- [x] 2.3 实现中立向量 store adapter 选择与受控异常上下文，令缺省 framework 仅接受唯一候选。
- [ ] 2.4 使中立 properties、factory、registry 与默认 key 校验不依赖任何具体 AI framework，并将第 1 组 RED 测试转为 GREEN。

## 3. LangChain4j 统一适配层

- [ ] 3.1 新建 `fw-plus-ai-langchain4j-adapter`，迁移旧 model/store 的 LangChain4j 对象转换、provider 构造和中立 SPI 实现。
- [ ] 3.2 新建 LangChain4j adapter starter/隔离自动配置，使 LangChain4j SDK 与 adapter 均在 classpath 时才注册实现。
- [ ] 3.3 保持现有 LangChain4j provider、多模型别名、多个 store、默认 key 与错误上下文行为，并为 adapter 编写目标测试。

## 4. 中立 Boot 与 Magic-API 边界

- [ ] 4.1 将中立 model/store properties 与公共自动配置迁出 LangChain4j 包和依赖路径。
- [ ] 4.2 重构 Magic-API model/vector 动态资源、控制器和 legacy support，使其只使用中立 registry/门面并复用 adapter 选择规则。
- [ ] 4.3 执行并补齐 Magic-API 与自动配置集成测试：唯一 LangChain4j adapter、多个 adapter 歧义、显式 framework 和无原生类型暴露。
- [ ] 4.4 在其他 adapter、Boot 与 Magic-API 工作完成，并获得用户再次明确确认后，最后更新 AskData starter 与相关测试，移除对已删除 model/store artifact 的编译依赖并保留 Java 8 行为。

## 5. 构建坐标与破坏性清理

- [ ] 5.1 从 Core/AI 聚合、Core/AI Boot BOM、starter 聚合、Magic-API 与样例 POM 中移除四个旧 artifact，注册统一 adapter 与 starter。
- [ ] 5.2 删除 `fw-plus-ai-langchain4j-model`、`fw-plus-ai-langchain4j-store-embeding` 及两个旧 starter 目录和所有旧自动配置/兼容桥接代码。
- [ ] 5.3 验证仓库不再包含旧 artifact、`fw-plus.ai.langchain4j.model`、`fw-plus.ai.langchain4j.embedding-store` 或中立模块中的具体框架痕迹。

## 6. 迁移文档与验证

- [ ] 6.1 编写中文破坏性迁移指南：旧坐标/配置映射、LangChain4j adapter 安装、自动发现错误诊断、Magic-API key 边界、回滚限制和测试命令。
- [ ] 6.2 编写 Java 17 / Spring Boot 3.x 后续实施蓝图：独立 reactor/BOM、Jakarta 迁移、Spring AI、Spring AI Alibaba、AgentScope 2 进程内 SDK adapter SPI 与测试要求。
- [ ] 6.3 运行 Core 目标 reactor、AI Boot 自动配置、Magic-API 与 AskData 目标测试、OpenSpec 严格校验和 Maven 依赖/源码边界检查，记录结果。
