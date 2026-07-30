# AgentScope 2 Java adapter 后续实施蓝图（Java 17 / Boot 3）

## 当前支持状态

本次变更仅交付文档，不创建 Java 17 artifact、starter、自动配置或真实 model/store adapter。AgentScope 2 Java adapter 本次未实现；当前没有 `fw-plus-ai-agentscope-j17-starter`、Boot 3 自动配置或 SDK 兼容性结论。后续实现采用**进程内 SDK adapter**：adapter 在同一 JVM 中包装业务实际使用的模型、工具和 agent/workflow 能力。

它不是外部 HTTP、MCP 或 A2A 桥接，也不是跨进程编排层。这些网络协议可以由业务另行部署，但不能代替中立 model/store SPI adapter，也不能据此声称 AgentScope adapter 已接通。

## 前置条件

Spring Boot 2.7 可以运行在 Java 17；后续仍选独立 Java 17 / Boot 3 reactor，以隔离 AgentScope 2 与 `jakarta.*`。在写入任何坐标前完成 SDK 版本探针，验证 SDK、JDK、Boot 3 与 Spring Framework 6 的兼容性。

## Maven Parent 与 Starter

后续在独立 reactor 创建：

```text
fw-plus-ai-j17/
├── fw-plus-ai-j17-parent/                    # Java 17、Boot 3、插件
├── fw-plus-ai-j17-dependencies/              # AgentScope 2 SDK 版本锁定
├── fw-plus-ai-agentscope-j17-adapter/        # 仅在此处引用 AgentScope 类型
├── fw-plus-ai-agentscope-j17-autoconfigure/  # Boot 3 条件 Bean
└── fw-plus-ai-agentscope-j17-starter/        # 可选应用入口
```

parent/BOM 以 SDK 探针确认 Java 17、Spring Boot 3、Spring Framework 6 与 AgentScope 2 的实际版本组合。Boot 3 使用 `@AutoConfiguration` 和 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`，涉及 Web/validation/servlet 时使用 `jakarta.*`。不得把这些依赖带回 Java 8 Core BOM/reactor，也不得以 Boot 2 `spring.factories` 注册 future starter。

## 同 key 配置迁移

adapter 的 framework 标识为 `agentscope`，按经验证的 provider/type 实现 `AiModelAdapter` 和 `AiEmbeddingStoreAdapter`。AgentScope 原生对象、agent、tool、workflow、memory 或 session 只能驻留 adapter/业务 Java 17 层；中立 Core 和 Magic-API 继续仅使用 key、framework、provider、type 与中立 facade。

```yaml
fw-plus:
  ai:
    model:
      models:
        knowledge-chat:
          framework: agentscope
          provider: verified-provider
          type: CHAT
          properties: {}
```

只安装 AgentScope adapter 时，缺 framework 的定义可按唯一候选规则选择；同时安装多个 adapter 时必须显式 `framework: agentscope`。agent/workflow 是否启用是业务选择，不能隐式由 model/store 注册触发。未验证的工具调用、流式、embedding 或 vector 能力必须明确失败，而不是返回空代理或回退 LangChain4j。

## Java API 迁移

业务代码只依赖 `AiModelRegistry`、`AiEmbeddingStoreRegistry` 与中立 facade。AgentScope 的 model、agent、tool、workflow、memory 与 session 类型留在 Java 17 adapter/业务层，不能泄漏到 Java 8 public API。

## Magic-API 影响

`model[...]` 和 `vector[...]` 的 key 不变，但 MagicScript 仅见中立 facade。缺 SDK class、缺配置、零/多候选或未支持能力必须以脱敏错误失败，不能注册半初始化 Bean、创建空代理或通过协议桥接回退。

## 验证

1. SDK 版本探针：验证实际 Maven 坐标、Java 17 字节码、Boot 3/Jakarta 兼容性、模型 provider 和依赖冲突。
2. 进程内 SDK 契约测试：以 SDK fake/mock 验证 adapter 对模型、工具、agent/workflow 生命周期的调用；不能以 HTTP/MCP/A2A 测试替代。
3. 中立 SPI 测试：显式 framework、唯一/零/多候选、未支持 provider/type 与敏感信息脱敏，model 与 store 分别覆盖。
4. Boot 3 条件装配：SDK class、provider 依赖、配置或唯一 adapter 缺失时，不注册 registry 条目、不创建半初始化 Bean、不启动 agent/workflow。
5. 受控集成测试：至少一个真实模型 provider；若声明 embedding/vector 能力，还必须各自有真实后端验证。日志、诊断和 tracing 不得泄漏 token、secret、完整 endpoint。
6. 隔离检查：Java 8 Core 的 effective POM 和 dependency tree 不得解析 AgentScope、Boot 3 或 Jakarta API。

未来模块存在后才可执行以下模板：

```bash
./fw-plus-core/mvnw -f fw-plus-ai-j17/pom.xml \
  -pl fw-plus-ai-agentscope-j17-adapter,fw-plus-ai-agentscope-j17-autoconfigure -am test
./fw-plus-core/mvnw -f fw-plus-ai-j17/pom.xml \
  -pl fw-plus-ai-agentscope-j17-starter dependency:tree test
```

## 回滚

在上述实现和证据完成前，AgentScope 2 是路线图而非当前可支持 framework。生产回滚必须使用完整的上一发布版本，不能把 Java 17 AgentScope starter 与现有 Java 8 / Boot 2 产物混装。
