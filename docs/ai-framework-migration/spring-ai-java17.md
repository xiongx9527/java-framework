# Spring AI adapter 后续实施蓝图（Java 17 / Boot 3）

## 当前支持状态

本次变更仅交付文档，不创建 Java 17 artifact、starter、自动配置或真实 model/store adapter。当前仓库没有 `fw-plus-ai-spring-ai-j17-starter`、Spring AI adapter、Boot 3 自动配置或已验证的 Spring AI BOM 组合；`framework: spring-ai` 因而不是当前 Java 8 发布线可启用的值。

未来实现不得修改 Java 8 Core 的公开签名或让 `org.springframework.ai.*`、Boot 3、`jakarta.*` 进入 Core BOM/reactor。中立 `AiModelAdapter`、`AiEmbeddingStoreAdapter`、registry 和 definition 是唯一跨线契约。

## 前置条件

Spring Boot 2.7 可以运行在 Java 17；本路线仍选择独立 Boot 3 reactor，以隔离 Spring Framework 6、`jakarta.*` 和目标 SDK，而不是把它们升级进当前 Java 8 发布物。先完成 SDK 版本探针，再确定实现坐标和能力矩阵。

## Maven Parent 与 Starter

建议单独建立以下规划布局，坐标和版本仅在 SDK 探针通过后正式确定：

```text
fw-plus-ai-j17/
├── pom.xml                                  # Java 17 reactor
├── fw-plus-ai-j17-parent/                   # compiler、Boot 3、插件
├── fw-plus-ai-j17-dependencies/             # Spring AI BOM 与版本锁定
├── fw-plus-ai-spring-ai-j17-adapter/        # 仅持有 Spring AI 原生类型
├── fw-plus-ai-spring-ai-j17-autoconfigure/  # Boot 3 条件装配
└── fw-plus-ai-spring-ai-j17-starter/        # 应用入口
```

parent 设定 Java 17，BOM 只管理经探针验证的 Spring AI、Spring Framework 6 与 Spring Boot 3 组合。Boot 3 自动配置放入 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`，使用 Boot 3 的 `@AutoConfiguration` 和 `jakarta.*` API；不得复制 Boot 2 `spring.factories` 注册方式到该线。

## 同 key 配置迁移

adapter 必须以 framework 标识 `spring-ai` 实现 model/store 中立 SPI，并只在 adapter 内部封装 Spring AI 的 `ChatModel`、`EmbeddingModel`、`VectorStore`、`Document` 等对象。应用配置使用既有中立前缀：

```yaml
fw-plus:
  ai:
    model:
      models:
        knowledge-chat:
          framework: spring-ai
          provider: openai
          type: CHAT
          properties: {}
    embedding-store:
      stores:
        knowledge:
          framework: spring-ai
          provider: qdrant
          type: QDRANT
          properties: {}
```

同一个 key 的 Magic-API `model['knowledge-chat']` 和 `vector['knowledge']` 保持不变，但仍只能返回中立 facade。不得在 MagicScript 或 Java 8 public API 返回 Spring AI 类型。

## Java API 迁移

业务 Java API 面向 `AiModelRegistry`、`AiEmbeddingStoreRegistry` 与中立 `AiModel`/`AiEmbeddingStore`。Spring AI `ChatModel`、`EmbeddingModel`、`VectorStore`、`Document` 只留在 Java 17 adapter 内部；新增能力前先扩展中立契约并完成测试。

## Magic-API 影响

`model[...]` 与 `vector[...]` 的 key 不随切换改变，脚本侧不能取得 Spring AI 原生对象。缺 SDK、缺配置、零/多候选和未支持能力必须给出脱敏错误，绝不创建半初始化 Bean 或回退 LangChain4j。

## 验证

1. SDK 探针锁定实际 BOM、Java 17 字节码、Spring Framework 6 与 `jakarta.*` 依赖；生成 dependency tree 作为证据。
2. model/store 各自覆盖显式 `framework=spring-ai` 成功、framework 不存在、零候选、多个候选以及未支持 provider/type；错误包含诊断上下文且脱敏。
3. 使用 `ApplicationContextRunner` 或等效 Boot 3 测试覆盖 SDK class 缺失、配置缺失、provider SDK 缺失时不注册 registry 条目和半初始化 Bean。
4. 对 Chat、Embedding、VectorStore 的每一个承诺能力做 SDK mock/fixture 契约测试；不支持的能力明确失败，不回退 LangChain4j。
5. 在受控凭证下至少验证一个真实模型端点和一个真实向量后端；日志、失败消息、actuator 输出不得泄漏密钥或连接串。
6. 复验当前 Java 8 Core 的 effective POM 与 dependency tree 不包含 Spring AI、Boot 3 或 Jakarta API。

未来 reactor 建立后才可执行的验收模板：

```bash
./fw-plus-core/mvnw -f fw-plus-ai-j17/pom.xml \
  -pl fw-plus-ai-spring-ai-j17-adapter,fw-plus-ai-spring-ai-j17-autoconfigure -am test
./fw-plus-core/mvnw -f fw-plus-ai-j17/pom.xml \
  -pl fw-plus-ai-spring-ai-j17-starter dependency:tree test
```

## 回滚

在上述实现和证据完成前，生产应用应继续使用当前 `framework: langchain4j` 路径。回滚必须回退完整框架版本，不能将未来 Java 17 starter 与 Java 8 Boot 2 应用混装，也不能在新版本混入被删除的旧 artifact。
