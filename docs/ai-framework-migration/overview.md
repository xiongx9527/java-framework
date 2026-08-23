# AI 框架破坏性迁移与多 Adapter 接入

## 适用范围

本文是当前 Java 17、Spring Boot 3.5.8 model/store adapter 发布线的迁移指南。LangChain4j 1.18.0 adapter 继续由业务显式安装；本发布线同时新增 `fw-plus-ai-spring-adapter`，以 `spring-ai` 与 `spring-ai-alibaba` 两个 framework 支持同步 CHAT/EMBEDDING。Spring AI 版本为 1.1.2，Spring AI Alibaba 与 extensions 为 1.1.2.2。

AskData 模块保留且不默认引入任何具体 adapter/provider。本次只验证 Spring AI 模型通过中立 `AiModelRegistry` 与 Magic-API `model[...]` 贯通，不宣称 AskData 已切换，也不宣称 Spring AI/Alibaba VectorStore 已实现。

## 1. 必须替换的坐标

| 删除的 groupId:artifactId | 迁移至 groupId:artifactId | 使用场景 |
| --- | --- | --- |
| `com.xiongx9527.fw.plus:fw-plus-ai-langchain4j-model` | `com.xiongx9527.fw.plus:fw-plus-ai-langchain4j-adapter` | 非 Spring 应用直接装配 LangChain4j 中立 SPI |
| `com.xiongx9527.fw.plus:fw-plus-ai-langchain4j-store-embeding` | `com.xiongx9527.fw.plus:fw-plus-ai-langchain4j-adapter` | 同上；旧名称的 `embeding` 是历史拼写 |
| `com.xiongx9527.fw.plus.ai:fw-plus-ai-langchain4j-model-spring-boot-starter` | `com.xiongx9527.fw.plus.ai:fw-plus-ai-model-spring-boot-starter` + `com.xiongx9527.fw.plus:fw-plus-ai-langchain4j-adapter` | Spring Boot 模型接入 |
| `com.xiongx9527.fw.plus.ai:fw-plus-ai-langchain4j-store-embeding-spring-boot-starter` | `com.xiongx9527.fw.plus.ai:fw-plus-ai-store-embedding-spring-boot-starter` + `com.xiongx9527.fw.plus:fw-plus-ai-langchain4j-adapter` | Spring Boot 向量 store 接入 |

统一 adapter 承载模型、embedding 与 vector store 的 LangChain4j 类型转换、provider 构造及中立 SPI 实现。中立 API/starter 保持独立：`fw-plus-ai-model`、`fw-plus-ai-store-embedding`、`fw-plus-ai-model-spring-boot-starter`、`fw-plus-ai-store-embedding-spring-boot-starter` 不传递具体 framework 或 provider SDK。

Spring Boot 应用安装 AI Boot 自动配置与 Core Adapter；按需再安装模型和向量 store 中立 starter：

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-boot-starter</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
<dependency>
    <groupId>com.xiongx9527.fw.plus</groupId>
    <artifactId>fw-plus-ai-langchain4j-adapter</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
```

并按所选 provider 显式加入对应 LangChain4j SDK。adapter starter 不传递全部 provider SDK，缺 SDK 时不会伪造实现或回退到其他 provider。

**禁止混装。** 新版本中不得同时出现统一 adapter 与上表任意旧 artifact，也不得通过手工加入旧 jar 恢复旧路径；这会使 classpath、自动配置和 SPI 候选不再可预测。

## 2. 配置迁移

删除下列旧前缀，不能再依赖“同 key 旧配置优先”或兼容绑定：

- `fw-plus.ai.langchain4j.model`
- `fw-plus.ai.langchain4j.embedding-store`

迁移到以下中立前缀：`fw-plus.ai.model` 和 `fw-plus.ai.embedding-store`。`properties` 是 adapter/provider 专属配置容器；中立 Core 不解释其中的凭证或连接参数。

```yaml
fw-plus:
  ai:
    model:
      enabled: true
      default-model: knowledge-chat
      models:
        knowledge-chat:
          framework: langchain4j
          provider: openai
          type: CHAT
          enabled: true
          properties:
            base-url: http://localhost:11434/v1
            api-key: ${AI_API_KEY}
            model-name: gpt-4o-mini
    embedding-store:
      enabled: true
      default-store: knowledge
      stores:
        knowledge:
          framework: langchain4j
          provider: qdrant
          type: VECTOR
          enabled: true
          properties:
            host: localhost
            port: 6334
```

`framework` 是 adapter 标识，不是 provider 名称。业务选择当前已实现的 LangChain4j Adapter 时填写 `langchain4j`。只安装一个可支持 provider/type 的 adapter 时可省略它；一旦安装多个 adapter，所有会产生歧义的 model/store 定义必须显式填写 framework。

## 3. 自动发现与失败诊断

模型与向量 store 使用相同的确定性算法：

1. 指定 `framework`：只在同名 adapter 中匹配 provider/type；没有匹配项即失败。
2. 未指定 `framework`：按 provider/type 计算候选；恰好一个候选才创建实例。
3. 零候选或多个候选：启动/创建失败，不能使用 Bean 顺序、类名或“LangChain4j 优先级”消除歧义。

错误必须包含 `key`、请求的 `framework`、`provider`、`type` 与候选 framework；不得输出 `api-key`、token、secret、完整 URL 或完整 properties。

| 现象 | 检查与处理 |
| --- | --- |
| 零候选 | 确认 adapter starter 在运行时 classpath，definition 的 framework/provider/type 拼写受支持；如果是 LangChain4j，确认其 provider SDK 已显式引入。 |
| 多候选 | 在该 `models.<key>` 或 `stores.<key>` 下写 `framework`；不要以 `@Primary`、Bean 顺序或删除报错来选择。 |
| provider SDK 缺失 | 检查 dependency tree，加入选定 provider 的 LangChain4j 依赖后重建；不应由 adapter starter 传递全部 SDK。 |
| 仍解析旧 artifact/旧前缀 | 先移除旧 POM 依赖和 `fw-plus.ai.langchain4j.*` 配置，再清理依赖管理、父 POM、样例与部署配置。 |

可执行检查命令（按项目实际选择 `-pl` 范围）：

```bash
# 查出四个已删除坐标的残留来源
./fw-plus-ai/mvnw -f fw-plus-ai/pom.xml dependency:tree \
  -Dincludes=com.xiongx9527.fw.plus:fw-plus-ai-langchain4j-model,com.xiongx9527.fw.plus:fw-plus-ai-langchain4j-store-embeding,com.xiongx9527.fw.plus.ai:fw-plus-ai-langchain4j-model-spring-boot-starter,com.xiongx9527.fw.plus.ai:fw-plus-ai-langchain4j-store-embeding-spring-boot-starter

# 确认统一 adapter 与所选 provider SDK 是否实际解析
./fw-plus-ai/mvnw -f fw-plus-ai/fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-autoconfigure dependency:tree

# 定位旧坐标和旧前缀；输出非空即表示迁移未完成
rg -n 'fw-plus-ai-langchain4j-(model|store-embeding)|fw-plus\.ai\.langchain4j\.(model|embedding-store)' .
```

## 4. Magic-API 边界

`model['key']`、`vector['key']`、模型默认 key 与 store 默认 key 的业务语义保持不变；动态 `model-datasource` / `vector-datasource` 资源也可填写 `framework`，并进入同一选择器。资源缺 framework 且存在零个或多个候选时，必须返回脱敏的明确错误。

稳定的是 key 与中立操作边界，不是原生 Java 对象。MagicScript 不得取得 `LangChain4jModel`、`EmbeddingStore<TextSegment>`、Spring AI `VectorStore` 或未来 SDK 原生对象。需要 SDK 专有 chat、stream、tool 或 store 调用时，改在 Java 应用/adapter 层完成；不要重新把原生类型暴露给脚本。

## 5. 回滚限制

回滚的单位是完整发布版本：将应用的 framework BOM/starter 回退至仍发布四个旧 artifact 的前一发行版本，同时恢复那一版本匹配的旧依赖和配置。**不能**在新版本中混入旧 artifact，也不能只恢复旧配置前缀。

建议顺序：停止新版本部署，锁定前一版本的 BOM 与四个旧坐标，恢复对应配置，重新执行 model/store/Magic-API 回归，再切流。回滚不改变 `model[...]`、`vector[...]` 或资源 key；若脚本已依赖中立 facade 的新行为，应先验证前一插件版本的兼容性。

## 6. 当前发布线的验证命令

执行前固定 Java 17：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

mvn -f fw-plus-core/pom.xml \
  -pl fw-plus-project/fw-plus-ai/fw-plus-ai-spring/fw-plus-ai-spring-adapter \
  -am test

mvn -f fw-plus-ai/pom.xml \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -am test

mvn -f fw-plus-ai/pom.xml \
  -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter,fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-store-embedding-spring-boot-starter \
  -am validate
```

完成代码变更后还应运行 `docs/ai-framework-migration/verify-migration-docs.sh`、`git diff --check`、dependency tree 与相关 LangChain4j/Magic-API 回归。真实远端 provider 测试只在受控凭据下执行，未执行时应明确记录。

## 7. 当前与后续路线

当前实现详情见：

- [Spring AI](spring-ai-java17.md)
- [Spring AI Alibaba](spring-ai-alibaba-java17.md)
- [兼容矩阵](compatibility-matrix.md)

AgentScope 2 Java 仍是独立后续需求；当前不创建任何 `fw-plus-ai-j17` reactor、j17 starter 或 AgentScope artifact。
